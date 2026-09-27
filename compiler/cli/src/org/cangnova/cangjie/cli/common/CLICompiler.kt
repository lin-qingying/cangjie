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

package org.cangnova.cangjie.cli.common

import org.cangnova.cangjie.arguments.description.cangjieCompilerArguments
import org.cangnova.cangjie.arguments.dsl.base.CangJieCompilerArguments
import org.cangnova.cangjie.arguments.dsl.base.CangJieReleaseVersion
import org.cangnova.cangjie.cli.common.arguments.CangJieArgumentParseResult
import org.cangnova.cangjie.cli.common.arguments.parseCommandLineArguments
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.frontend.arguments.CommonCompilerArguments
import org.cangnova.cangjie.messages.CompilerMessageSeverity

/** 命令行解析与执行的返回状态。 */
public data class CangJieCLIExecutionResult<A : CommonCompilerArguments>(
    /** 解析后的参数、自由源码路径和参数诊断。 */
    val parsing: CangJieArgumentParseResult<A>,
    /** 参数错误时为 null，否则是 frontend 管线的返回值。 */
    val frontendSucceeded: Boolean?,
)

/**
 * 仓颉 compiler CLI 的公共执行层。
 *
 * 具体编译器提供参数对象与执行实现；参数文本使用共享 schema 解析，错误在进入编译管线前统一上报。
 */
public abstract class CangJieCLICompiler<A : CommonCompilerArguments>(
    private val schema: CangJieCompilerArguments = cangjieCompilerArguments,
    private val compilerRelease: CangJieReleaseVersion = CangJieReleaseVersion.V_1_1_3,
) {
    /** 创建当前 compiler 对应的参数对象。 */
    public abstract fun createArguments(): A

    /** 解析文本参数后由具体 compiler 执行。 */
    protected abstract fun executeParsedArguments(
        arguments: A,
        freeArguments: List<String>,
        configuration: CompilerConfiguration,
    ): Boolean

    /**
     * 从原始命令行 token 开始执行；参数错误经 compiler message collector 报告且不会进入 frontend。
     */
    public fun exec(
        args: List<String>,
        configuration: CompilerConfiguration,
    ): CangJieCLIExecutionResult<A> {
        val parsing = parseCommandLineArguments(
            args = args,
            result = createArguments(),
            schema = schema,
            compilerRelease = compilerRelease,
        )
        for (issue in parsing.issues) {
            configuration.messageCollector.report(
                if (issue.isError) CompilerMessageSeverity.ERROR else CompilerMessageSeverity.WARNING,
                issue.message,
            )
        }
        if (parsing.hasErrors) return CangJieCLIExecutionResult(parsing, frontendSucceeded = null)
        return CangJieCLIExecutionResult(
            parsing = parsing,
            frontendSucceeded = executeParsedArguments(parsing.arguments, parsing.freeArguments, configuration),
        )
    }

    /** 从 JVM/C++ host 常用的字符串数组命令行入口执行。 */
    public fun exec(
        args: Array<out String>,
        configuration: CompilerConfiguration,
    ): CangJieCLIExecutionResult<A> = exec(args.asList(), configuration)
}
