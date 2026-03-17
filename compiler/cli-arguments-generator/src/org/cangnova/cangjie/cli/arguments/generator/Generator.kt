package org.cangnova.cangjie.cli.arguments.generator

import org.cangnova.cangjie.arguments.dsl.base.CangJieCompilerArgument
import org.cangnova.cangjie.arguments.dsl.base.CangJieCompilerArgumentsLevel
import org.cangnova.cangjie.arguments.dsl.base.ExperimentalArgumentApi
import org.cangnova.cangjie.arguments.dsl.types.*
import java.io.File

fun generateArgumentsClass(
    genDir: File,
    level: CangJieCompilerArgumentsLevel,
    parent: CangJieCompilerArgumentsLevel?,
) {
    val info = levelToClassNameMap.getValue(level.name)
    val packagePath = info.classPackage.dropLastWhile { it == '.' }.split(".")
    var dir = genDir
    for (packagePart in packagePath) {
        dir = dir.resolve(packagePart)
    }
    dir.mkdirs()
    val file = dir.resolve(info.className + ".kt")
    val newText = buildString { generateArgumentsClassContent(level, parent, info) }
    file.writeText(newText)
}

private fun StringBuilder.generateArgumentsClassContent(
    level: CangJieCompilerArgumentsLevel,
    parent: CangJieCompilerArgumentsLevel?,
    info: ArgumentsInfo,
) {
//    appendLine(COPYRIGHT)
    appendLine("package org.cangnova.cangjie.cli.common.arguments")
    appendLine()

    val imports = collectImports(info)
    if (imports.isNotEmpty()) {
        imports.forEach { appendLine(it) }
        appendLine()
    }

    appendLine("// This file was generated automatically. See generator in :compiler:cli:cli-arguments-generator")
    appendLine("// Please declare arguments in $ORIGIN_FILE_PATH/${info.originFileName}.kt")
    appendLine("// DO NOT MODIFY IT MANUALLY.")
    appendLine()

    if (!info.levelIsFinal) {
        append("abstract ")
    }
    append("class ${info.className}")
    val supertypes = when (parent) {
        null -> "Freezable(), Serializable"
        else -> "${levelToClassNameMap.getValue(parent.name).className}()"
    }
    appendLine(" : $supertypes {")

    generateAdditionalSyntheticArguments(info)

    for (argument in level.arguments) {
        generateProperty(argument)
        appendLine()
    }

    appendLine("}")
}

private fun collectImports(info: ArgumentsInfo): List<String> {
    val imports = mutableSetOf<String>()
    if (info.isCommonToolsArgs) {
        imports.add("import java.io.Serializable")
    }
    return imports.sorted()
}

private fun StringBuilder.generateAdditionalSyntheticArguments(info: ArgumentsInfo) {
    for (argument in info.additionalSyntheticArguments) {
        appendLine("    var $argument: Boolean = true")
        appendLine("        set(value) {")
        appendLine("            checkFrozen()")
        appendLine("            field = value")
        appendLine("        }")
        appendLine()
    }
}

@OptIn(ExperimentalArgumentApi::class)
private fun StringBuilder.generateProperty(argument: CangJieCompilerArgument) {
    val name = argument.calculateName()
    val type = when (val argType = argument.argumentType) {
        is BooleanType -> if (argType.isNullable.current) "Boolean?" else "Boolean"
        is StringArrayType -> "Array<String>"
        is StringListType -> "Array<String>"
        is SystemPathType -> "String?"
        is LiteralPathType -> "Array<String>"
        else -> if (argType.isNullable.current) "String?" else "String"
    }

    appendLine("    var $name: $type = ${argument.defaultValueInArgs}")
    appendLine("        set(value) {")
    appendLine("            checkFrozen()")
    if (type == "String?") {
        appendLine("            field = if (value.isNullOrEmpty()) ${argument.defaultValueInArgs} else value")
    } else {
        appendLine("            field = value")
    }
    appendLine("        }")
}

fun CangJieCompilerArgument.calculateName(): String = compilerName ?: name
    .removePrefix("X").removePrefix("X")
    .split("-").joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
    .replaceFirstChar(Char::lowercaseChar)

@OptIn(ExperimentalArgumentApi::class)
private val CangJieCompilerArgument.defaultValueInArgs: String
    get() {
        @Suppress("UNCHECKED_CAST")
        val valueType = argumentType as CangJieArgumentValueType<Any>
        return when (valueType) {
            is StringArrayType -> "emptyArray()"
            is StringListType -> if (valueType.defaultValue.current.isNullOrEmpty()) "emptyArray()"
                else "arrayOf(${valueType.stringRepresentation(valueType.defaultValue.current)})"
            is LiteralPathType -> if (valueType.defaultValue.current.isNullOrEmpty()) "emptyArray()"
                else "arrayOf(${valueType.stringRepresentation(valueType.defaultValue.current)})"
            else -> valueType.stringRepresentation(valueType.defaultValue.current) ?: "null"
        }
    }
