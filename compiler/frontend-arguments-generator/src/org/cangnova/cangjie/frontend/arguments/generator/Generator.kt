package org.cangnova.cangjie.frontend.arguments.generator

import org.cangnova.cangjie.arguments.dsl.base.CangJieCompilerArgument
import org.cangnova.cangjie.arguments.dsl.base.CangJieCompilerArgumentsLevel
import org.cangnova.cangjie.arguments.dsl.base.ExperimentalArgumentApi
import org.cangnova.cangjie.arguments.dsl.types.*
import java.io.File

/**
 * 为指定编译器参数层级生成前端使用的参数类源码。
 *
 * @param genDir 生成源码根目录。
 * @param level 需要生成的参数层级。
 * @param parent 当前层级的父参数层级；为空时生成根参数类。
 */
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

/**
 * 生成单个参数类文件内容。
 */
private fun StringBuilder.generateArgumentsClassContent(
    level: CangJieCompilerArgumentsLevel,
    parent: CangJieCompilerArgumentsLevel?,
    info: ArgumentsInfo,
) {
//    appendLine(COPYRIGHT)
    appendLine("package org.cangnova.cangjie.frontend.arguments")
    appendLine()

    val imports = collectImports(info)
    if (imports.isNotEmpty()) {
        imports.forEach { appendLine(it) }
        appendLine()
    }

    appendLine("// This file was generated automatically. See generator in :compiler:frontend-arguments-generator")
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

/**
 * 收集生成参数类所需的 import 列表。
 */
private fun collectImports(info: ArgumentsInfo): List<String> {
    val imports = mutableSetOf<String>()
    if (info.isCommonToolsArgs) {
        imports.add("import java.io.Serializable")
    }
    return imports.sorted()
}

/**
 * 生成不是 DSL 参数声明、但前端参数类需要持有的合成属性。
 */
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
/**
 * 根据参数 DSL 描述生成一个可冻结检查的可变属性。
 */
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

/**
 * 计算参数在生成类中的 Kotlin 属性名。
 */
fun CangJieCompilerArgument.calculateName(): String = compilerName ?: name
    .removePrefix("X").removePrefix("X")
    .split("-").joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
    .replaceFirstChar(Char::lowercaseChar)

@OptIn(ExperimentalArgumentApi::class)
/**
 * 计算参数在生成类中的默认值源码表达式。
 */
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
