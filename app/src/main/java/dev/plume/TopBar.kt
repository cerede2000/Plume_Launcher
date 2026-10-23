package dev.plume

/** The icons of the top bar, in their default order. "plume" (Plume settings) is always shown, so settings can always be reached. */
internal val TOP_ICONS = listOf("wifi", "sources", "apps", "android", "plume", "power")

/** The icons hidden until the user shows them. */
internal val TOP_HIDDEN_AT_FIRST = setOf("power")

/** The saved order (comma separated), kept to the icons that exist, with any new icon added at the end. */
internal fun topOrder(stored: String?): List<String> {
    val kept = stored.orEmpty().split(',').filter { it in TOP_ICONS }.distinct()
    return kept + TOP_ICONS.filter { it !in kept }
}

/** [id] moved [dir] places (-1 earlier, +1 later), stopping at the ends. */
internal fun topMoved(order: List<String>, id: String, dir: Int): List<String> {
    val at = order.indexOf(id)
    val to = (at + dir).coerceIn(0, order.size - 1)
    if (at < 0 || to == at) return order
    return order.toMutableList().apply { removeAt(at); add(to, id) }
}
