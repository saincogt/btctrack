package com.zeal.btctrack.ui

import com.zeal.btctrack.domain.model.AddressEntry

// Existing group paths plus all their parent paths, filtered by query.
fun groupSuggestions(entries: List<AddressEntry>, query: String): List<String> {
    val paths = entries
        .map { it.groupPath.trim() }
        .filter { it.isNotEmpty() }
        .flatMap { path ->
            val segments = path.split("/")
            segments.indices.map { i -> segments.take(i + 1).joinToString("/") }
        }
        .distinct()
        .sorted()
    return filterByQuery(paths, query)
}

// Existing labels filtered by query; labels from the current group come first.
fun labelSuggestions(entries: List<AddressEntry>, group: String, query: String): List<String> {
    val currentGroup = group.trim()
    val (inGroup, others) = entries
        .filter { it.label.isNotBlank() }
        .partition { currentGroup.isNotEmpty() && it.groupPath.trim() == currentGroup }
    val first = inGroup.map { it.label.trim() }.distinct().sorted()
    val rest = others.map { it.label.trim() }.distinct().sorted().filter { it !in first }
    return filterByQuery(first + rest, query)
}

private fun filterByQuery(candidates: List<String>, query: String): List<String> {
    val q = query.trim()
    return candidates.filter { it.contains(q, ignoreCase = true) && !it.equals(q, ignoreCase = true) }
}
