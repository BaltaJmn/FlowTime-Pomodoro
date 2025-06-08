package com.baltajmn.flowtime.core.common.extensions

fun mapToString(map: Map<String, Long>): String {
    return map.entries.joinToString(separator = "\n") { "${it.key}: ${it.value}" }
}

fun stringToMap(data: String): Map<String, Long> {
    val regex = Regex("""^(.+): (\d+)$""")
    return data.lines().mapNotNull { line ->
        regex.matchEntire(line)?.destructured?.let { (key, value) ->
            key to value.toLong()
        }
    }.toMap()
}