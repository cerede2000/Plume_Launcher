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
}
