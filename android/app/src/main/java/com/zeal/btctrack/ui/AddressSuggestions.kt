package com.zeal.btctrack.ui

import com.zeal.btctrack.domain.model.AddressEntry
import com.zeal.btctrack.domain.normalizeGroupPath

// Existing group paths plus all their parent paths, filtered by query.
fun groupSuggestions(entries: List<AddressEntry>, query: String): List<String> {
    val paths = entries
        .map { pathSegments(it.groupPath) }
        .filter { it.isNotEmpty() }
        .flatMap { segments -> segments.indices.map { i -> segments.take(i + 1).joinToString("/") } }
    return filterByQuery(distinctIgnoreCase(paths.sorted()), query)
}

// Existing labels filtered by query; labels from the current group come first.
fun labelSuggestions(entries: List<AddressEntry>, group: String, query: String): List<String> {
    val currentGroup = pathSegments(group)
    val (inGroup, others) = entries
        .filter { it.label.isNotBlank() }
        .partition { currentGroup.isNotEmpty() && pathSegments(it.groupPath) == currentGroup }
    val first = inGroup.map { it.label.trim() }.sorted()
    val rest = others.map { it.label.trim() }.sorted()
    return filterByQuery(distinctIgnoreCase(first + rest), query)
}

private fun pathSegments(path: String): List<String> =
    normalizeGroupPath(path).let { if (it.isEmpty()) emptyList() else it.split("/") }

private fun distinctIgnoreCase(values: List<String>): List<String> = values.distinctBy { it.lowercase() }

private fun filterByQuery(candidates: List<String>, query: String): List<String> {
    val q = query.trim()
    return candidates.filter { it.contains(q, ignoreCase = true) && !it.equals(q, ignoreCase = true) }
}
