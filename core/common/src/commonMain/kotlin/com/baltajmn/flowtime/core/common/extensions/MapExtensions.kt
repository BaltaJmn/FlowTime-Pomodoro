package com.baltajmn.flowtime.core.common.extensions

fun mapToString(map: Map<String, Long>): String {
    return map.entries.joinToString(separator = "\n") { "${it.key}: ${it.value}" }
}
