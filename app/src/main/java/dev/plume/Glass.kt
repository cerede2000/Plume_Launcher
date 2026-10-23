package dev.plume

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.os.SystemClock
import android.view.View
import java.lang.ref.WeakReference

/** The blurred wallpaper is this many pixels wide, and as tall as a 16:9 screen: 120x68 is ~32 KB. */
internal const val GLASS_W = 120
internal const val GLASS_H = 68

/** A blur of [px] (opaque ARGB, [w]x[h]): [passes] box blurs of radius [radius], horizontal then vertical each time. */
internal fun boxBlur(px: IntArray, w: Int, h: Int, radius: Int, passes: Int = 2): IntArray {
    var src = px.copyOf()
    var dst = IntArray(px.size)
    repeat(passes) {
        for (horizontal in booleanArrayOf(true, false)) {
            val n = if (horizontal) w else h // pixels along a line
            val lines = if (horizontal) h else w
            val step = if (horizontal) 1 else w // from one pixel of a line to the next
            val lineStep = if (horizontal) w else 1 // from one line to the next
            for (l in 0 until lines) {
                val base = l * lineStep
                for (i in 0 until n) {
                    var r = 0
                    var g = 0
                    var b = 0
                    for (k in -radius..radius) {
                        val p = src[base + (i + k).coerceIn(0, n - 1) * step]
                        r += p shr 16 and 0xFF
                        g += p shr 8 and 0xFF
                        b += p and 0xFF
                    }
                    val c = 2 * radius + 1
                    dst[base + i * step] = (0xFF shl 24) or (r / c shl 16) or (g / c shl 8) or (b / c)
                }
            }
            val t = src
            src = dst
            dst = t
        }
    }
    return src
}

/**
 * The glass keeps the wallpaper's colours but not its brightness: dark mode keeps [darkPct] percent of it, light mode
 * moves [lightPct] percent of the way to white.
 */
internal fun glassTone(px: IntArray, dark: Boolean, darkPct: Int = 100, lightPct: Int = 20): IntArray = IntArray(px.size) {
    val p = px[it]
    val c = IntArray(3) { k -> p shr (16 - 8 * k) and 0xFF }
    for (k in 0..2) c[k] = if (dark) c[k] * darkPct / 100 else c[k] + (255 - c[k]) * lightPct / 100
    (0xFF shl 24) or (c[0] shl 16) or (c[1] shl 8) or c[2]
}

/**
 * The wallpaper as it shows on the screen, blurred, as [GLASS_W]x[GLASS_H] pixels. [ground] fills what the picture
 * does not cover (the bundled ones stop at 70% of the height). Done once when a wallpaper loads, off the UI thread.
 */
internal fun glassPixels(wallpaper: Bitmap, ground: Int, dark: Boolean, darkPct: Int = 100, lightPct: Int = 20): IntArray {
    // In two steps, or a 1920 px picture shrunk to 120 px in one would skip most of its pixels.
    val mid = Bitmap.createScaledBitmap(wallpaper, wallpaper.width / 4, wallpaper.height / 4, true)
    val screen = Bitmap.createBitmap(GLASS_W, GLASS_H, Bitmap.Config.ARGB_8888)
    val c = Canvas(screen)
    c.drawColor(ground or (0xFF shl 24))
    val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    c.drawBitmap(mid, Rect(0, 0, mid.width, mid.height), RectF(0f, 0f, GLASS_W.toFloat(), mid.height * GLASS_W.toFloat() / mid.width), paint)
    mid.recycle()
    val px = IntArray(GLASS_W * GLASS_H)
    screen.getPixels(px, 0, GLASS_W, 0, 0, GLASS_W, GLASS_H)
    screen.recycle()
    return glassTone(boxBlur(px, GLASS_W, GLASS_H, 2), dark, darkPct, lightPct)
}

/**
 * Sits between the wallpaper and the home screen and paints the blurred wallpaper under each glass tile, at the
 * tile's own place, so the tile's see-through colour sits over frosted glass. One shader and a few rounded
 * rectangles: no library, no live blur, nothing for the GPU to redo on each frame.
 */
internal class GlassLayer(context: Context) : View(context) {
    private class Tile(val view: WeakReference<View>, val radius: Float)

    private val tiles = ArrayList<Tile>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val matrix = Matrix()
    private val visible = Rect()
    private val rect = RectF()
    private val at = IntArray(2)
    private var shader: BitmapShader? = null
    var blur: Bitmap? = null // the blurred wallpaper, for the surfaces that paint it themselves (GlassBox)
        private set
    private var until = 0L // keeps redrawing for a moment after a scroll or a focus change, which move the tiles

    fun add(v: View, radius: Float) {
        tiles.removeAll { it.view.get() == null }
        if (tiles.none { it.view.get() === v }) tiles += Tile(WeakReference(v), radius)
    }

    /** The blurred wallpaper, or null for none (a plain background has nothing to blur). */
    fun setBlur(px: IntArray?) {
        blur = px?.let { Bitmap.createBitmap(it, GLASS_W, GLASS_H, Bitmap.Config.ARGB_8888) }
        shader = blur?.let { BitmapShader(it, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) }
        invalidate()
    }

    /** Tiles move on a scroll or a focus lift: draw again for the next [ms]. */
    fun kick(ms: Long) {
        until = maxOf(until, SystemClock.uptimeMillis() + ms)
        postInvalidateOnAnimation()
    }

    override fun onDraw(c: Canvas) {
        val s = shader ?: return
        matrix.setScale(width.toFloat() / GLASS_W, height.toFloat() / GLASS_H)
        s.setLocalMatrix(matrix)
        paint.shader = s
        getLocationInWindow(at)
        val ox = at[0]
        val oy = at[1]
        for (t in tiles) {
            val v = t.view.get() ?: continue
            if (!v.isShown || !v.getGlobalVisibleRect(visible)) continue
            v.getLocationInWindow(at)
            // Where the tile is drawn, lift included; what a scroll view cuts off of it stays cut off.
            rect.set(at[0] - ox.toFloat(), at[1] - oy.toFloat(), at[0] - ox + v.width * v.scaleX, at[1] - oy + v.height * v.scaleY)
            c.save()
            c.clipRect(visible.left - ox.toFloat(), visible.top - oy.toFloat(), visible.right - ox.toFloat(), visible.bottom - oy.toFloat())
            c.drawRoundRect(rect, t.radius * v.scaleX, t.radius * v.scaleY, paint)
            c.restore()
        }
        if (SystemClock.uptimeMillis() < until) postInvalidateOnAnimation()
    }
}

/** Where a view at ([x], [y]) on a [screenW]x[screenH] screen finds itself in the blurred wallpaper: scale x, scale y, shift x, shift y. */
internal fun glassMapping(screenW: Int, screenH: Int, x: Int, y: Int) =
    floatArrayOf(screenW.toFloat() / GLASS_W, screenH.toFloat() / GLASS_H, -x.toFloat(), -y.toFloat())

/**
 * A menu, a card or a panel in frosted glass: it paints the blurred wallpaper that is under it, at its own place, then
 * a tint (dark or white) and a hairline rim, with the children on top. The blur is the one the tiles use, so
 * nothing is blurred again, and what is behind the surface (tiles, cards) does not show through the text.
 * [round] is the corner radius, [flatRight]: the right corners run off the screen (a panel along the right edge).
 */
internal class GlassBox @JvmOverloads constructor(context: Context, attrs: android.util.AttributeSet? = null) : android.widget.LinearLayout(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val tintPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val rimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 1f }
    private val matrix = Matrix()
    private val rect = RectF()
    private val at = IntArray(2)
    private var shader: BitmapShader? = null
    private var gloss: android.graphics.LinearGradient? = null
    var round = 0f
    var flatRight = false
    var tint = 0
    var rim = 0
    var highlight = 0 // white at the top edge, fading out: the light of the glass

    init {
        setWillNotDraw(false)
        clipChildren = false
    }

    /** The blur to paint, or null for a plain [tint] (a plain background has nothing to blur). */
    fun setBlur(b: Bitmap?) {
        shader = b?.let { BitmapShader(it, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP) }
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        gloss = android.graphics.LinearGradient(0f, 0f, 0f, minOf(h.toFloat(), 120f * resources.displayMetrics.density), highlight, 0, Shader.TileMode.CLAMP)
    }

    override fun onDraw(c: Canvas) {
        rect.set(0f, 0f, width.toFloat(), height.toFloat())
        if (flatRight) rect.right += round
        shader?.let { s ->
            getLocationOnScreen(at)
            val m = glassMapping(resources.displayMetrics.widthPixels, resources.displayMetrics.heightPixels, at[0], at[1])
            matrix.setScale(m[0], m[1])
            matrix.postTranslate(m[2], m[3])
            s.setLocalMatrix(matrix)
            paint.shader = s
            c.drawRoundRect(rect, round, round, paint)
        }
        tintPaint.color = tint
        c.drawRoundRect(rect, round, round, tintPaint)
        if (highlight != 0) {
            tintPaint.shader = gloss
            c.drawRoundRect(rect, round, round, tintPaint)
            tintPaint.shader = null
        }
        rimPaint.color = rim
        rect.inset(0.5f, 0.5f)
        c.drawRoundRect(rect, round, round, rimPaint)
    }
}
