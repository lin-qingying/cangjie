/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.generators.tree.printer

import org.cangnova.cangjie.generators.tree.*
import org.cangnova.cangjie.generators.tree.imports.ImportCollecting
import org.cangnova.cangjie.generators.tree.imports.ImportCollector
import org.cangnova.cangjie.generators.util.GeneratorsFileUtil
import org.cangnova.cangjie.utils.IndentingPrinter
import org.cangnova.cangjie.utils.SmartPrinter
import java.io.File

/**
 * 生成文件统一使用的版权头文本。
 */
private val COPYRIGHT by lazy {
    val relativePath = "license/COPYRIGHT_HEADER.txt"
    val start = File(".").canonicalFile
    val headerFile = generateSequence(start) { it.parentFile }
        .map { File(it, relativePath) }
        .firstOrNull { it.exists() }
        ?: error("Cannot find $relativePath from ${start.path} and its parent directories")
    headerFile.readText()
}

/**
 * 单个待写入生成文件的路径和内容。
 */
class GeneratedFile(
    /**
     * 目标生成文件路径。
     */
    val file: File,
    /**
     * 需要写入目标文件的新文本。
     */
    val newText: String,
)

/**
 * 根据输出根目录、包名和类型名计算 Kotlin 文件路径。
 */
private fun getPathForFile(generationPath: File, packageName: String, typeName: String): File {
    val dir = generationPath.resolve(packageName.replace(".", "/"))
    return File(dir, "$typeName.kt")
}

/**
 * 同时实现缩进打印和导入收集的打印上下文。
 */
private data class PrinterAndImportCollector(
    /**
     * 实际承载缩进和文本追加的打印器。
     */
    val printer: SmartPrinter,
    /**
     * 当前生成文件的导入收集器。
     */
    val importCollector: ImportCollecting,
) : IndentingPrinter by printer,
    ImportCollecting by importCollector,
    ImportCollectingPrinter

/**
 * 使用新的底层 [SmartPrinter] 临时打印一段内容，并复用当前导入收集器。
 */
fun ImportCollectingPrinter.withNewPrinter(printer: SmartPrinter, body: ImportCollectingPrinter.() -> Unit) {
    PrinterAndImportCollector(printer, this).apply(body)
}

/**
 * 将多个生成类型打印到同一个 Kotlin 文件中。
 */
fun <GeneratedType, TypePrinter> printGeneratedTypesIntoSingleFile(
    generatedTypes: List<GeneratedType>,
    generationPath: File,
    treeGeneratorReadMe: String,
    packageName: String,
    fileNameWithoutExtension: String,
    fileSuppressions: List<String> = emptyList(),
    makeTypePrinter: (ImportCollectingPrinter) -> TypePrinter,
    printType: TypePrinter.(GeneratedType) -> Unit,
): GeneratedFile {
    val stringBuilder = StringBuilder()
    val file = getPathForFile(generationPath, packageName, fileNameWithoutExtension)
    val importCollector = ImportCollector(packageName)
    val printer = PrinterAndImportCollector(SmartPrinter(stringBuilder), importCollector)

    val typePrinter = makeTypePrinter(printer)
    var isFirst = true
    for (generatedType in generatedTypes) {
        if (isFirst) {
            isFirst = false
        } else {
            printer.println()
        }
        typePrinter.printType(generatedType)
    }

    return GeneratedFile(
        file,
        buildString {
            appendLine(COPYRIGHT)
            appendLine()
            append(GeneratorsFileUtil.GENERATED_MESSAGE_PREFIX)
            append(treeGeneratorReadMe)
            appendLine(".")
            appendLine(GeneratorsFileUtil.GENERATED_MESSAGE_SUFFIX)
            appendLine()
            if (fileSuppressions.isNotEmpty()) {
                fileSuppressions.joinTo(this, prefix = "@file:Suppress(", postfix = ")\n\n") { "\"$it\"" }
            }
            appendLine("package $packageName")
            appendLine()
            if (importCollector.printAllImports(this)) {
                appendLine()
            }
            append(stringBuilder)
        }
    )
}

/**
 * 将单个生成类型打印到一个 Kotlin 文件中。
 */
fun printGeneratedType(
    generationPath: File,
    treeGeneratorReadMe: String,
    packageName: String,
    typeName: String,
    fileSuppressions: List<String> = emptyList(),
    body: ImportCollectingPrinter.() -> Unit,
): GeneratedFile =
    printGeneratedTypesIntoSingleFile(
        listOf(null),
        generationPath,
        treeGeneratorReadMe,
        packageName,
        typeName,
        fileSuppressions,
        makeTypePrinter = { it },
        printType = { body() }
    )

/**
 * 用于清理 [generationPath] 中不再由本次生成流程产出的旧文件。
 *
 * @param generationPath 生成文件输出目录。
 * @param treeGeneratorReadme 树生成器 README 的相对路径，会写入每个生成文件的自动生成提示中。
 */
class TreeGenerator(
    /**
     * 生成文件输出目录。
     */
    private val generationPath: File,
    /**
     * 树生成器 README 的相对路径。
     */
    val treeGeneratorReadme: String,
) {
    /**
     * 本次生成流程产出的文件列表。
     */
    val generatedFiles = mutableListOf<GeneratedFile>()

    /**
     * 执行一次完整生成流程并同步目标目录。
     */
    fun run(body: TreeGenerator.() -> Unit) {
        body(this)
        val previouslyGeneratedFiles = GeneratorsFileUtil.collectPreviouslyGeneratedFiles(generationPath)
        generatedFiles.forEach { GeneratorsFileUtil.writeFileIfContentChanged(it.file, it.newText, logNotChanged = false) }
        GeneratorsFileUtil.removeExtraFilesFromPreviousGeneration(previouslyGeneratedFiles, generatedFiles.map { it.file })
    }

    /**
     * 为模型中的可打印元素生成元素接口/抽象类文件。
     */
    fun <Element> printElements(
        model: Model<Element>,
        createElementPrinter: (ImportCollectingPrinter) -> AbstractElementPrinter<Element, *>,
    ) where Element : AbstractElement<Element, *, *> {
        val elementsToPrint = model.elements.filter { it.doPrint }
        elementsToPrint.mapTo(generatedFiles) { element ->
            printGeneratedType(
                generationPath,
                treeGeneratorReadme,
                element.packageName,
                element.typeName,
            ) { createElementPrinter(this).printElement(element) }
        }
    }

    /**
     * 为实现类模型生成具体实现文件。
     */
    fun <Element, ElementField, Implementation> printElementImplementations(
        implementations: List<Implementation>,
        createImplementationPrinter: (ImportCollectingPrinter) -> AbstractImplementationPrinter<Implementation, Element, ElementField>,
    ) where Element : AbstractElement<Element, ElementField, Implementation>,
            ElementField : AbstractField<ElementField>,
            Implementation : AbstractImplementation<Implementation, Element, ElementField> {
        val implementationsToPrint = implementations.filter { it.doPrint }
        implementationsToPrint.mapTo(generatedFiles) { implementation ->
            printGeneratedType(
                generationPath,
                treeGeneratorReadme,
                implementation.packageName,
                implementation.typeName,
                fileSuppressions = listOf("DuplicatedCode"),
            ) { createImplementationPrinter(this).printImplementation(implementation) }
        }
    }

    /**
     * 为 Builder 模型生成 Builder DSL 文件。
     */
    fun <Element, ElementField> printElementBuilders(
        builders: List<Builder<ElementField, Element>>,
        createBuilderPrinter: ((ImportCollectingPrinter) -> AbstractBuilderPrinter<Element, *, ElementField>),
    ) where Element : AbstractElement<Element, ElementField, *>,
            ElementField : AbstractField<ElementField> {
        builders.mapTo(generatedFiles) { builder ->
            printGeneratedType(
                generationPath,
                treeGeneratorReadme,
                builder.packageName,
                builder.typeName,
                fileSuppressions = listOf("DuplicatedCode", "unused"),
            ) {
                createBuilderPrinter(this).printBuilder(builder)
            }
        }
    }

    /**
     * 为模型生成 visitor/transformer 文件。
     */
    fun <Element, ElementField> printVisitors(
        model: Model<Element>,
        createVisitorPrinters: List<Pair<ClassRef<*>, (ImportCollectingPrinter, ClassRef<*>) -> AbstractVisitorPrinter<Element, ElementField>>>,
    ) where Element : AbstractElement<Element, ElementField, *>,
            ElementField : AbstractField<ElementField> {
        createVisitorPrinters.mapTo(generatedFiles) { (visitorClass, createVisitorPrinter) ->
            printGeneratedType(generationPath, treeGeneratorReadme, visitorClass.packageName, visitorClass.simpleName) {
                createVisitorPrinter(this, visitorClass).printVisitor(model.elements)
            }
        }
    }
}
