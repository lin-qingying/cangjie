/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.generators.util

import org.cangnova.cangjie.utils.IndentingPrinter
import org.cangnova.cangjie.utils.SmartPrinter
import org.cangnova.cangjie.utils.withIndent
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * 生成器打印辅助工具集合：
 * 包括导入输出、版权与自动生成说明输出，以及通用代码块打印。
 */
// -------------------------------- 导入相关 --------------------------------

/**
 * 打印 Kotlin import 列表。
 */
fun SmartPrinter.printImports(
    packageName: String,
    importableTypes: Collection<KType>,
    simpleImports: Collection<String>,
    starImports: Collection<String>,
) {
    val imports = collectImports(packageName, importableTypes, simpleImports, starImports)
    if (imports.isEmpty()) return
    printImports(imports)
    println()
}

/**
 * 收集需要打印的 import 字符串。
 */
private fun collectImports(
    packageName: String,
    importableTypes: Collection<KType>,
    simpleImports: Collection<String>,
    starImports: Collection<String>,
): Collection<String> {
    return buildSet {
        for (starImport in starImports) {
            add("$starImport.*")
        }

        importableTypes.forEach { type ->
            type.collectClassNamesTo(this)
        }
        addAll(simpleImports)
    }.filterNot { importString ->
        importString.dropLastWhile { it != '.' } == "$packageName."
    }
}

/**
 * 递归收集 KType 中出现的类名。
 */
private fun KType.collectClassNamesTo(set: MutableSet<String>) {
    (classifier as? KClass<*>)?.qualifiedName?.let(set::add)
    for (argument in arguments) {
        argument.type?.collectClassNamesTo(set)
    }
}

/**
 * 打印已经收集完成的 import 字符串集合。
 */
private fun SmartPrinter.printImports(imports: Collection<String>) {
    val importsToPrint = imports.filterNot { it.isDefaultImport() }.distinct().sorted()
    for (import in importsToPrint) {
        println("import $import")
    }
}

/**
 * 判断 import 是否属于 Kotlin 默认导入包。
 */
private fun String.isDefaultImport(): Boolean {
    return substringBeforeLast('.') in defaultImportedPackages
}

/**
 * Kotlin 默认导入包集合。
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

// -------------------------------- 说明头相关 --------------------------------

/**
 * 仓库版权头文本。
 */
private val COPYRIGHT = File("license/COPYRIGHT_HEADER.txt").readText()

/**
 * 打印版权头。
 */
fun SmartPrinter.printCopyright() {
    println(COPYRIGHT)
    println()
}

/**
 * 打印自动生成文件提示。
 */
fun SmartPrinter.printGeneratedMessage() {
    println(GeneratorsFileUtil.GENERATED_MESSAGE)
    println()
}

// -------------------------------- 其他工具 --------------------------------

/**
 * 根据根目录和包名计算生成输出目录，并确保目录存在。
 */
fun getGenerationPath(rootPath: File, packageName: String): File {
    return packageName
        .split(".")
        .fold(rootPath, File::resolve)
        .apply { mkdirs() }
}

/**
 * 打印带缩进的代码块。
 */
inline fun IndentingPrinter.printBlock(header: String = "", body: () -> Unit) {
    println("$header {")
    withIndent(body)
    println("}")
}
