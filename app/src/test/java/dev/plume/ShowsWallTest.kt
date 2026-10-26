package dev.plume

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShowsWallTest {
    @Test fun wideArtIsA16By9ThreeByTwoOrFourByThreePosterOrAnyThumbnail() {
        assertTrue(ShowsWall.wide(false, 0))
        assertTrue(ShowsWall.wide(false, 1))
        assertTrue(ShowsWall.wide(false, 2))
        assertFalse(ShowsWall.wide(false, 3)) // square
        assertFalse(ShowsWall.wide(false, 4)) // 2:3
        assertFalse(ShowsWall.wide(false, 5)) // movie poster
        assertTrue(ShowsWall.wide(true, 4)) // a thumbnail is wide whatever the poster is
    }

    @Test fun aPictureFitsWhenItIsWideAndBigEnough() {
        assertTrue(ShowsWall.fits(1920, 1080))
        assertTrue(ShowsWall.fits(1600, 900))
        assertTrue(ShowsWall.fits(640, 360))
        assertTrue(ShowsWall.fits(800, 600)) // 4:3
        assertTrue(ShowsWall.fits(2560, 1080)) // 21:9 is under 2.5:1
        assertFalse(ShowsWall.fits(1000, 1500)) // a poster
        assertFalse(ShowsWall.fits(600, 600)) // a square
        assertFalse(ShowsWall.fits(320, 180)) // too small
        assertFalse(ShowsWall.fits(1920, 0))
        assertFalse(ShowsWall.fits(3000, 1000)) // a strip
    }

    @Test fun theNextOneComesAfterTheCurrentAndTheCurrentIsLast() {
        val keys = listOf("a", "b", "c", "d")
        assertEquals(listOf("c", "d", "a", "b"), ShowsWall.order(keys, "b"))
        assertEquals(listOf("a", "b", "c", "d"), ShowsWall.order(keys, "d"))
        assertEquals(keys, ShowsWall.order(keys, "gone")) // no longer published: start over
        assertEquals(keys, ShowsWall.order(keys, null))
        assertEquals(emptyList<String>(), ShowsWall.order(emptyList(), "a"))
    }

    @Test fun autoDarkeningLeavesADarkPictureAloneAndTamesABrightOne() {
        assertEquals(0f, ShowsWall.meanLuma(IntArray(0)), 0f)
        assertEquals(1f, ShowsWall.meanLuma(intArrayOf(0xFFFFFFFF.toInt())), 0.001f)
        assertEquals(0f, ShowsWall.meanLuma(intArrayOf(0xFF000000.toInt())), 0.001f)
        assertEquals(0, ShowsWall.autoDim(0.1f)) // already darker than the target
        assertEquals(0, ShowsWall.autoDim(0.3f))
        assertTrue(ShowsWall.autoDim(0.5f) in 90..110) // 40 % of black
        assertEquals(153, ShowsWall.autoDim(1f)) // never more than 60 %
        assertTrue(ShowsWall.autoDim(0.6f) > ShowsWall.autoDim(0.5f)) // brighter, more black
    }

    @Test fun aVeryDarkPictureIsLiftedAndOthersAreNot() {
        assertEquals(0, ShowsWall.autoLift(0.22f))
        assertEquals(0, ShowsWall.autoLift(0.4f))
        assertEquals(51, ShowsWall.autoLift(0.0f)) // the most: 20 % grey
        assertTrue(ShowsWall.autoLift(0.1f) in 1..50)
        assertTrue(ShowsWall.autoLift(0.05f) > ShowsWall.autoLift(0.1f)) // darker, more lift
        for (m in listOf(0f, 0.05f, 0.1f, 0.3f, 0.6f, 1f)) assertTrue(ShowsWall.autoDim(m) == 0 || ShowsWall.autoLift(m) == 0) // never both
    }
}
