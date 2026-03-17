package org.cangnova.cangjie.cli.arguments.generator

import org.cangnova.cangjie.arguments.description.CompilerArgumentsLevelNames
import org.cangnova.cangjie.arguments.description.cangjieCompilerArguments
import org.cangnova.cangjie.arguments.dsl.base.*
import java.io.File

private const val COPYRIGHT = """/*
 * Copyright 2010-2025 Cangjie Compiler Project.
 */"""

  const val ORIGIN_FILE_PATH = "compiler/arguments/src/org/cangnova/cangjie/arguments/description"

fun main(args: Array<String>) {
    val genDir = File(args[0])
    for (level in args.drop(1)) {
        generateLevel(genDir, level)
    }
}

private fun generateLevel(genDir: File, levelName: String) {
    val (level, parent) = findLevelWithParent(levelName)
    generateArgumentsClass(genDir, level, parent)
}

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

class ArgumentsInfo(
    val levelName: String,
    val className: String,
    val classPackage: String = "org.cangnova.cangjie.cli.common.arguments.",
    val levelIsFinal: Boolean,
    val originFileName: String = className,
    val additionalSyntheticArguments: List<String> = emptyList(),
)

val ArgumentsInfo.isCommonToolsArgs: Boolean
    get() = levelName == CompilerArgumentsLevelNames.commonToolArguments

val ArgumentsInfo.isCommonCompilerArgs: Boolean
    get() = levelName == CompilerArgumentsLevelNames.commonCompilerArguments

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
