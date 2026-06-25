package org.cangnova.cangjie.cfir.checkers.generator

import org.cangnova.cangjie.utils.SmartPrinter

/**
 * 生成源码 import 列表的工具对象。
 */
private object ImportPrinter {
    /**
     * 打印过滤默认导入后的 import 集合。
     */
    fun SmartPrinter.printImports(imports: Collection<String>) {
        val importsToPrint = imports.filterNot { it.isDefaultImport() }.distinct().sorted()
        for (import in importsToPrint) {
            println("import $import")
        }
    }

    /**
     * 判断全限定名是否位于 Kotlin 默认导入包。
     */
    private fun String.isDefaultImport() = substringBeforeLast('.') in defaultImportedPackages

    /**
     * 生成源码时不需要显式打印的默认导入包。
     */
    private val defaultImportedPackages = setOf(
        "kotlin",
        "kotlin.annotation",
        "kotlin.collections",
        "kotlin.ranges",
        "kotlin.sequences",
        "kotlin.text",
        "kotlin.io",
    )
}

/**
 * 在 [SmartPrinter] 上打印生成文件需要的 import 列表。
 */
fun SmartPrinter.printImports(imports: Collection<String>) {
    with(ImportPrinter) { printImports(imports) }
}




