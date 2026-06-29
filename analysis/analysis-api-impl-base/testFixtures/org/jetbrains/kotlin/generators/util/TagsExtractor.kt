package org.jetbrains.kotlin.generators.util

import java.io.File

/**
 * 测试数据目录中存放标签列表的文件名。
 */
private const val TAGS_FILE_NAME = "_tags.txt"

/**
 * JUnit5 标签中禁止出现的表达式控制字符。
 */
private val PROHIBITED_SYMBOLS = listOf(' ', ',', '(', ')', '&', '|', '!')

/**
 * 从测试数据目录的 `_tags.txt` 中读取 JUnit5 标签。
 *
 * 标签文件不存在时返回空列表；存在时逐行读取非空标签并校验标签字符合法性。
 */
fun extractTagsFromDirectory(dir: File): List<String> {
    if (!dir.exists()) return emptyList()
    require(dir.isDirectory) {
        "${dir.absolutePath} is not a directory"
    }
    val tagsFile = dir.resolve(TAGS_FILE_NAME)
    if (!tagsFile.exists()) return emptyList()
    return tagsFile.readLines().filter { it.isNotBlank() }.onEach(::validateTag)
}

/**
 * 从单个测试数据文件读取标签。
 *
 * 当前仓颉测试生成器尚未定义文件级标签格式，因此该函数固定返回空列表。
 */
fun extractTagsFromTestFile(@Suppress("UNUSED_PARAMETER") file: File): List<String> = emptyList()

/**
 * 校验标签不包含 JUnit 标签表达式中的保留符号。
 */
private fun validateTag(tag: String) {
    require(PROHIBITED_SYMBOLS.none { it in tag }) {
        "Tag \"tag\" contains one of prohibited symbols: $PROHIBITED_SYMBOLS"
    }
}
