@file:OptIn(org.cangnova.cangjie.arguments.dsl.base.ExperimentalArgumentApi::class)

/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cangnova.cangjie.cli.common.arguments

import org.cangnova.cangjie.arguments.dsl.base.CangJieCompilerArgument
import org.cangnova.cangjie.arguments.dsl.base.CangJieCompilerArguments
import org.cangnova.cangjie.arguments.dsl.base.CangJieCompilerArgumentsLevel
import org.cangnova.cangjie.arguments.dsl.base.CangJieReleaseVersion
import org.cangnova.cangjie.arguments.dsl.base.generatedPropertyName
import org.cangnova.cangjie.arguments.description.cangjieCompilerArguments
import org.cangnova.cangjie.arguments.dsl.types.BooleanType
import org.cangnova.cangjie.arguments.dsl.types.CangJieArgumentValueType
import org.cangnova.cangjie.arguments.dsl.types.LiteralPathType
import org.cangnova.cangjie.arguments.dsl.types.StringArrayType
import org.cangnova.cangjie.arguments.dsl.types.StringListType
import org.cangnova.cangjie.arguments.dsl.types.StringType
import org.cangnova.cangjie.arguments.dsl.types.SystemPathType
import java.io.File
import java.lang.reflect.Method

/** 单条文本参数解析诊断。 */
public data class CangJieArgumentParseIssue(
    /** 用户可直接读取的诊断内容。 */
    val message: String,
    /** 错误会阻止 frontend 执行；警告只提示参数迁移。 */
    val isError: Boolean,
)

/** 一次文本参数解析的完整结果。 */
public data class CangJieArgumentParseResult<A : Any>(
    /** 已按 schema 更新的参数对象。 */
    val arguments: A,
    /** `--` 后及未识别为选项的源码路径参数。 */
    val freeArguments: List<String>,
    /** 显式出现的公开参数名称到输入值的映射。 */
    val explicitArguments: Map<String, List<String>>,
    /** 参数解析错误与警告。 */
    val issues: List<CangJieArgumentParseIssue>,
) {
    /** 当前参数文本是否存在错误。 */
    public val hasErrors: Boolean get() = issues.any { it.isError }
}

/**
 * 将 compiler argument DSL 声明的参数文本解析到生成的参数对象。
 *
 * 解析名、属性类型、默认值、分隔符和生命周期均来自同一 schema；不维护另一份 CJMP 专用选项表。
 *
 * @param overrideArguments 为 true 时，数组选项从空值开始收集，适合覆盖环境变量参数；
 *   默认行为会把重复数组选项追加到 result 当前值之后。
 */
public fun <A : Any> parseCommandLineArguments(
        args: List<String>,
        result: A,
        schema: CangJieCompilerArguments = cangjieCompilerArguments,
        compilerRelease: CangJieReleaseVersion = CangJieReleaseVersion.V_1_1_3,
        overrideArguments: Boolean = false,
    ): CangJieArgumentParseResult<A> {
        val bindings = createBindings(result.javaClass, schema, compilerRelease)
        val freeArguments = mutableListOf<String>()
        val explicitArguments = linkedMapOf<String, MutableList<String>>()
        val arrayValues = mutableMapOf<ArgumentBinding, MutableList<String>>()
        val issues = mutableListOf<CangJieArgumentParseIssue>()
        var freeArgumentsStarted = false
        var index = 0

        while (index < args.size) {
            val rawArgument = args[index++]
            if (freeArgumentsStarted) {
                freeArguments += rawArgument
                continue
            }
            if (rawArgument == FREE_ARGUMENTS_DELIMITER) {
                freeArgumentsStarted = true
                continue
            }

            val valueSeparatorIndex = rawArgument.indexOf(ARGUMENT_VALUE_SEPARATOR)
            val optionName = if (valueSeparatorIndex >= 0) {
                rawArgument.substring(0, valueSeparatorIndex)
            } else {
                rawArgument
            }
            val option = bindings[optionName]
            if (option == null) {
                if (rawArgument.startsWith("-X")) {
                    issues += CangJieArgumentParseIssue("Unknown advanced compiler argument: $rawArgument", isError = true)
                } else if (rawArgument.startsWith('-')) {
                    issues += CangJieArgumentParseIssue("Invalid argument: $rawArgument", isError = true)
                } else {
                    freeArguments += rawArgument
                }
                continue
            }

            val binding = option.binding
            val argument = binding.argument
            if (option.kind == OptionNameKind.SHORT && valueSeparatorIndex >= 0) {
                issues += CangJieArgumentParseIssue("Invalid argument: $rawArgument", isError = true)
                continue
            }
            if (option.kind == OptionNameKind.DEPRECATED) {
                issues += CangJieArgumentParseIssue(
                    "Argument '$optionName' is deprecated; use '${binding.primaryName}'.",
                    isError = false,
                )
            }
            if (argument.isObsolete) {
                issues += CangJieArgumentParseIssue("Obsolete compiler argument: $optionName", isError = true)
            }

            val hasExplicitValue = valueSeparatorIndex >= 0
            val explicitValue = if (hasExplicitValue) rawArgument.substring(valueSeparatorIndex + 1) else null
            when (val valueType = argument.valueType) {
                is BooleanType -> {
                    val parsedValue = when (explicitValue) {
                        null, "true" -> true
                        "false" -> false
                        else -> {
                            issues += CangJieArgumentParseIssue(
                                "Incorrect value for boolean argument '$optionName'. Only 'true' and 'false' are allowed.",
                                isError = true,
                            )
                            true
                        }
                    }
                    binding.setter.invoke(result, parsedValue)
                    explicitArguments.getOrPut(argument.name, ::mutableListOf).add(parsedValue.toString())
                }

                is StringType, is SystemPathType, is StringArrayType, is StringListType, is LiteralPathType -> {
                    val value = if (explicitValue != null) {
                        explicitValue
                    } else {
                        if (optionName.startsWith("-X")) {
                            issues += CangJieArgumentParseIssue(
                                "Advanced argument '$optionName' should use '$optionName=<value>'.",
                                isError = false,
                            )
                        }
                        if (index >= args.size) {
                            issues += CangJieArgumentParseIssue("No value passed for argument $rawArgument", isError = true)
                            continue
                        }
                        args[index++]
                    }
                    explicitArguments.getOrPut(argument.name, ::mutableListOf).add(value)
                    when (valueType) {
                        is StringArrayType, is StringListType, is LiteralPathType -> {
                            val elements = splitArrayValue(value, argument.delimiter)
                            val accumulated = arrayValues.getOrPut(binding) {
                                if (overrideArguments) {
                                    mutableListOf()
                                } else {
                                    (binding.getter.invoke(result) as? Array<*>)
                                        .asStringList()
                                }
                            }
                            accumulated += elements
                            binding.setter.invoke(result, accumulated.toTypedArray())
                        }

                        is StringType, is SystemPathType -> binding.setter.invoke(result, value)
                        else -> error("Unsupported compiler argument value type: $valueType")
                    }
                }

                else -> error("Unsupported compiler argument value type: $valueType")
            }
        }

        return CangJieArgumentParseResult(
            arguments = result,
            freeArguments = freeArguments,
            explicitArguments = explicitArguments.mapValues { (_, values) -> values.toList() },
            issues = issues,
        )
}

    private fun createBindings(
        argumentClass: Class<*>,
        schema: CangJieCompilerArguments,
        compilerRelease: CangJieReleaseVersion,
    ): Map<String, ArgumentOption> {
        val result = linkedMapOf<String, ArgumentOption>()
        for (argument in schema.topLevel.allArguments()) {
            if (!argument.isAvailableIn(compilerRelease)) continue
            val propertyName = argument.generatedPropertyName
            val propertySuffix = propertyName.replaceFirstChar(Char::uppercaseChar)
            val setter = argumentClass.methods.singleOrNull { method ->
                method.name == "set$propertySuffix" && method.parameterCount == 1
            } ?: error("No generated setter for compiler argument property '$propertyName' on ${argumentClass.name}")
            val getter = argumentClass.methods.singleOrNull { method ->
                method.name == "get$propertySuffix" && method.parameterCount == 0
            } ?: error("No generated getter for compiler argument property '$propertyName' on ${argumentClass.name}")
            validatePropertyType(argument, setter)
            val binding = ArgumentBinding(argument, longOptionName(argument.name), getter, setter)
            addOption(result, binding.primaryName, binding, OptionNameKind.PRIMARY)
            argument.shortName?.takeIf(String::isNotBlank)?.let { shortName ->
                addOption(result, optionName(shortName), binding, OptionNameKind.SHORT)
            }
            argument.deprecatedName?.takeIf(String::isNotBlank)?.let { oldName ->
                addOption(result, optionName(oldName), binding, OptionNameKind.DEPRECATED)
            }
        }
        return result
    }

    private fun addOption(
        options: MutableMap<String, ArgumentOption>,
        name: String,
        binding: ArgumentBinding,
        kind: OptionNameKind,
    ) {
        val existing = options.putIfAbsent(name, ArgumentOption(binding, kind))
        require(existing == null || existing.binding == binding) {
            "Compiler argument schema declares '$name' more than once"
        }
    }

    private fun validatePropertyType(argument: CangJieCompilerArgument, setter: Method) {
        val propertyType = setter.parameterTypes.single()
        val supported = when (argument.argumentType) {
            is BooleanType -> propertyType == Boolean::class.javaPrimitiveType || propertyType == Boolean::class.javaObjectType
            is StringType, is SystemPathType -> propertyType == String::class.java
            is StringArrayType, is StringListType, is LiteralPathType -> propertyType == Array<String>::class.java
            else -> false
        }
        require(supported) {
            "Compiler argument '${argument.name}' generates unsupported property type ${propertyType.name}"
        }
    }

    private fun splitArrayValue(value: String, delimiter: CangJieCompilerArgument.Delimiter?): List<String> {
        val separator = when (delimiter ?: CangJieCompilerArgument.Delimiter.Default) {
            CangJieCompilerArgument.Delimiter.Default -> DEFAULT_ARRAY_SEPARATOR
            CangJieCompilerArgument.Delimiter.None -> null
            CangJieCompilerArgument.Delimiter.PathSeparator -> File.pathSeparator
            CangJieCompilerArgument.Delimiter.Space -> " "
            CangJieCompilerArgument.Delimiter.Semicolon -> ";"
        }
        return if (separator == null) listOf(value) else value.split(separator)
    }

    private fun Array<*>?.asStringList(): MutableList<String> =
        this?.map { value -> value as? String ?: error("Expected a string compiler argument value") }?.toMutableList()
            ?: mutableListOf()

    private fun CangJieCompilerArgumentsLevel.allArguments(): Sequence<CangJieCompilerArgument> = sequence {
        yieldAll(arguments)
        for (nestedLevel in nestedLevels) yieldAll(nestedLevel.allArguments())
    }

    private fun CangJieCompilerArgument.isAvailableIn(release: CangJieReleaseVersion): Boolean {
        if (compareVersions(release, releaseVersionsMetadata.introducedVersion) < 0) return false
        val removedVersion = releaseVersionsMetadata.removedVersion ?: return true
        return compareVersions(release, removedVersion) < 0
    }

    private fun compareVersions(first: CangJieReleaseVersion, second: CangJieReleaseVersion): Int =
        compareValuesBy(first, second, CangJieReleaseVersion::major, CangJieReleaseVersion::minor, CangJieReleaseVersion::patch)

    private fun optionName(name: String): String = when {
        name.startsWith('-') -> name
        name.startsWith('X') -> "-$name"
        name.length == 1 -> "-$name"
        else -> "--$name"
    }

    private fun longOptionName(name: String): String = optionName(name)

    private data class ArgumentBinding(
        val argument: CangJieCompilerArgument,
        val primaryName: String,
        val getter: Method,
        val setter: Method,
    )

    private data class ArgumentOption(
        val binding: ArgumentBinding,
        val kind: OptionNameKind,
    )

    private enum class OptionNameKind {
        PRIMARY,
        SHORT,
        DEPRECATED,
    }

private const val FREE_ARGUMENTS_DELIMITER = "--"
private const val ARGUMENT_VALUE_SEPARATOR = '='
private const val DEFAULT_ARRAY_SEPARATOR = ","
