package dev.plume

import org.junit.Assert.assertEquals
import org.junit.Test

class AppHeroTest {
    @Test fun theRectGrowsFromTheTileToTheScreenAndBack() {
        assertEquals(100f, AppHero.lerp(100f, 0f, 0f), 0.001f)
        assertEquals(50f, AppHero.lerp(100f, 0f, 0.5f), 0.001f)
        assertEquals(0f, AppHero.lerp(100f, 0f, 1f), 0.001f)
    }

    @Test fun theTilePictureFadesOutBeforeThePanelIsHalfOverTheScreen() {
        assertEquals(1f, AppHero.pictureAlpha(0f), 0.001f)
        assertEquals(0.5f, AppHero.pictureAlpha(0.25f), 0.001f)
        assertEquals(0f, AppHero.pictureAlpha(0.5f), 0.001f)
        assertEquals(0f, AppHero.pictureAlpha(1f), 0.001f)
    }
}
