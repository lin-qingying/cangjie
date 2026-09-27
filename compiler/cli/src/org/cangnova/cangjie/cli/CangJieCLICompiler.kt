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

package org.cangnova.cangjie.cli

import org.cangnova.cangjie.arguments.dsl.base.CangJieCompilerArguments
import org.cangnova.cangjie.arguments.dsl.base.CangJieReleaseVersion
import org.cangnova.cangjie.arguments.description.cangjieCompilerArguments
import org.cangnova.cangjie.cli.common.CangJieCLICompiler
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.frontend.arguments.CommonCompilerArguments
import org.cangnova.cangjie.frontend.pipeline.AbstractFrontendPipeline

/**
 * 将已解析的命令行参数接入已有 frontend 管线的 CLI 基类。
 *
 * 具体命令负责把自由参数（通常是源文件路径）装配到配置；CJMP 标志和其它结构化参数由同一 parser 写入参数对象。
 */
public abstract class CangJieFrontendCLICompiler<A : CommonCompilerArguments>(
    private val frontendPipeline: AbstractFrontendPipeline<A>,
    schema: CangJieCompilerArguments = cangjieCompilerArguments,
    compilerRelease: CangJieReleaseVersion = CangJieReleaseVersion.V_1_1_3,
) : CangJieCLICompiler<A>(schema, compilerRelease) {
    /** 把命令行自由参数装配为当前 frontend 实例的输入。 */
    protected abstract fun configureSourceArguments(
        freeArguments: List<String>,
        configuration: CompilerConfiguration,
    ): Boolean

    final override fun executeParsedArguments(
        arguments: A,
        freeArguments: List<String>,
        configuration: CompilerConfiguration,
    ): Boolean {
        if (!configureSourceArguments(freeArguments, configuration)) return false
        return frontendPipeline.execute(arguments, configuration)
    }
}
