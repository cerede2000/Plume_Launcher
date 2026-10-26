package dev.plume

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View

/**
 * The panel of an app that opens or closes: it starts as the tile the app was opened from, with the tile's own picture
 * on it, and grows to cover the screen in the home screen's own ground colour, so that nothing flashes light. Going back, it shrinks into the tile. Plume draws all of
 * it, in its own window, above every row: so nothing is cut at the edge of a scroller, and the opening and the return
 * are the same movement run one way and the other.
 *
 * [progress] is 0 on the tile and 1 over the whole screen; the tile's picture fades out as it grows.
 */
internal class AppHero(
    context: Context,
    private val picture: Bitmap?,
    private val color: Int,
    private var tile: RectF,
    private val screen: RectF,
    private val radius: Float,
) : View(context) {
    var progress = 0f
        set(value) {
            field = value
            invalidate()
        }

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = this@AppHero.color }
    private val art = Paint(Paint.FILTER_BITMAP_FLAG)

    /** Where the panel shrinks to: the tile as it is now. */
    fun retarget(rect: RectF) {
        tile = rect
    }

    override fun onDraw(c: Canvas) {
        val p = progress
        val r = RectF(lerp(tile.left, screen.left, p), lerp(tile.top, screen.top, p), lerp(tile.right, screen.right, p), lerp(tile.bottom, screen.bottom, p))
        val round = radius * (1f - p)
        c.drawRoundRect(r, round, round, fill)
        picture?.let {
            art.alpha = (255 * pictureAlpha(p)).toInt()
            if (art.alpha > 0) c.drawBitmap(it, r.centerX() - it.width / 2f, r.centerY() - it.height / 2f, art)
        }
    }

    companion object {
        fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

        /** The tile's picture is on show at the start and gone by the time the panel is half over the screen. */
        fun pictureAlpha(progress: Float) = (1f - progress * 2f).coerceIn(0f, 1f)

    }
}
