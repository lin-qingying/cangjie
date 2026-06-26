package org.cangnova.cangjie.frontend.arguments.generator

import org.cangnova.cangjie.arguments.description.CompilerArgumentsLevelNames
import org.cangnova.cangjie.arguments.description.cangjieCompilerArguments
import org.cangnova.cangjie.arguments.dsl.base.*
import java.io.File

/**
 * 生成文件使用的版权头模板。
 */
private const val COPYRIGHT = """/*
 * Copyright 2010-2025 Cangjie Compiler Project.
 */"""

/**
 * 参数 DSL 源文件目录，用于写入生成文件的来源提示。
 */
const val ORIGIN_FILE_PATH = "compiler/arguments/src/org/cangnova/cangjie/arguments/description"

/**
 * 前端参数类生成器命令行入口。
 *
 * 第一个参数是生成目录，后续参数是需要生成的参数层级名称。
 */
fun main(args: Array<String>) {
    val genDir = File(args[0])
    for (level in args.drop(1)) {
        generateLevel(genDir, level)
    }
}

/**
 * 查找指定参数层级并生成对应参数类。
 */
private fun generateLevel(genDir: File, levelName: String) {
    val (level, parent) = findLevelWithParent(levelName)
    generateArgumentsClass(genDir, level, parent)
}

/**
 * 从参数 DSL 树中查找指定层级及其直接父层级。
 */
private fun findLevelWithParent(name: String): Pair<CangJieCompilerArgumentsLevel, CangJieCompilerArgumentsLevel?> {
    fun find(
        level: CangJieCompilerArgumentsLevel,
        parent: CangJieCompilerArgumentsLevel?,
    ): Pair<CangJieCompilerArgumentsLevel, CangJieCompilerArgumentsLevel?>? {
        if (level.name == name) return level to parent
        return level.nestedLevels.firstNotNullOfOrNull { find(it, level) }
    }
    return find(cangjieCompilerArguments.topLevel, null) ?: error("Level with name $name not found")
}

/**
 * 参数层级到生成类的映射信息。
 *
 * @property levelName DSL 中的参数层级名称。
 * @property className 生成出的 Kotlin 类名。
 * @property classPackage 生成类所在包名。
 * @property levelIsFinal 生成类是否为 final 类。
 * @property originFileName 源 DSL 文件名。
 * @property additionalSyntheticArguments 额外注入的合成属性名。
 */
class ArgumentsInfo(
    /**
     * DSL 中的参数层级名称。
     */
    val levelName: String,
    /**
     * 生成出的 Kotlin 类名。
     */
    val className: String,
    /**
     * 生成类所在包名。
     */
    val classPackage: String = "org.cangnova.cangjie.frontend.arguments.",
    /**
     * 生成类是否为 final 类。
     */
    val levelIsFinal: Boolean,
    /**
     * 源 DSL 文件名。
     */
    val originFileName: String = className,
    /**
     * 额外注入的合成属性名。
     */
    val additionalSyntheticArguments: List<String> = emptyList(),
)

/**
 * 当前映射是否表示通用工具参数层级。
 */
val ArgumentsInfo.isCommonToolsArgs: Boolean
    get() = levelName == CompilerArgumentsLevelNames.commonToolArguments

/**
 * 当前映射是否表示通用编译器参数层级。
 */
val ArgumentsInfo.isCommonCompilerArgs: Boolean
    get() = levelName == CompilerArgumentsLevelNames.commonCompilerArguments

/**
 * 参数层级名称到生成类信息的映射表。
 */
val levelToClassNameMap = listOf(
    ArgumentsInfo(
        levelName = CompilerArgumentsLevelNames.commonToolArguments,
        className = "CommonToolArguments",
        levelIsFinal = false,
    ),
    ArgumentsInfo(
        levelName = CompilerArgumentsLevelNames.commonCompilerArguments,
        className = "CommonCompilerArguments",
        levelIsFinal = false,
        additionalSyntheticArguments = listOf("autoAdvanceLanguageVersion", "autoAdvanceApiVersion"),
    ),
).associateBy { it.levelName }
