package org.cangjie.cfir.checkers.generator

import org.cangnova.cangjie.utils.SmartPrinter

private object ImportPrinter {
    fun SmartPrinter.printImports(imports: Collection<String>) {
        val importsToPrint = imports.filterNot { it.isDefaultImport() }.distinct().sorted()
        for (import in importsToPrint) {
            println("import $import")
        }
    }

    private fun String.isDefaultImport() = substringBeforeLast('.') in defaultImportedPackages

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

fun SmartPrinter.printImports(imports: Collection<String>) {
    with(ImportPrinter) { printImports(imports) }
}




