package dev.plume

import org.junit.Assert.assertEquals
import org.junit.Test

class TopBarTest {
    @Test fun noSavedOrderMeansTheDefaultOne() {
        assertEquals(TOP_ICONS, topOrder(null))
        assertEquals(TOP_ICONS, topOrder(""))
    }

    @Test fun aSavedOrderIsKeptAndAnIconThatIsNewGoesLast() {
        assertEquals(listOf("plume", "apps", "wifi", "sources", "android", "power"), topOrder("plume,apps,wifi"))
    }

    @Test fun unknownAndRepeatedNamesAreDropped() {
        assertEquals(listOf("apps", "wifi", "sources", "android", "plume", "power"), topOrder("apps,gone,apps,wifi"))
    }

    @Test fun anIconMovesOnePlaceAndStopsAtTheEnds() {
        val order = TOP_ICONS
        assertEquals(listOf("sources", "wifi", "apps", "android", "plume", "power"), topMoved(order, "wifi", 1))
        assertEquals(order, topMoved(order, "wifi", -1))
        assertEquals(order, topMoved(order, "power", 1))
        assertEquals(listOf("wifi", "sources", "apps", "android", "power", "plume"), topMoved(order, "plume", 1))
    }
}
