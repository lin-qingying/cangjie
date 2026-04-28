package org.jetbrains.kotlin.generators.util

import java.io.File

private const val TAGS_FILE_NAME = "_tags.txt"
private val PROHIBITED_SYMBOLS = listOf(' ', ',', '(', ')', '&', '|', '!')

fun extractTagsFromDirectory(dir: File): List<String> {
    if (!dir.exists()) return emptyList()
    require(dir.isDirectory) {
        "${dir.absolutePath} is not a directory"
    }
    val tagsFile = dir.resolve(TAGS_FILE_NAME)
    if (!tagsFile.exists()) return emptyList()
    return tagsFile.readLines().filter { it.isNotBlank() }.onEach(::validateTag)
}

fun extractTagsFromTestFile(@Suppress("UNUSED_PARAMETER") file: File): List<String> = emptyList()

private fun validateTag(tag: String) {
    require(PROHIBITED_SYMBOLS.none { it in tag }) {
        "Tag \"tag\" contains one of prohibited symbols: $PROHIBITED_SYMBOLS"
    }
}
