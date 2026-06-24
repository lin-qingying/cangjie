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
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.entrypoint.configuration

import org.cangnova.cangjie.cfir.diagnostics.CjRegisteredDiagnosticFactoriesStorage
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.CompilerConfigurationKey

/**
 * 前端阶段配置键集合。
 *
 * 对齐 Kotlin 声明：`org.jetbrains.kotlin.cli.FrontendConfigurationKeys`。
 */
object CfirFrontendConfigurationKeys {
    /** 诊断工厂存储键。对齐 Kotlin 键：`DIAGNOSTIC_FACTORIES_STORAGE`。 */
    @JvmField
    val DIAGNOSTIC_FACTORIES_STORAGE =
        CompilerConfigurationKey.create<CjRegisteredDiagnosticFactoriesStorage>("DIAGNOSTIC_FACTORIES_STORAGE")

    /** 是否关闭 prelude。 */
    @JvmField
    val NO_PRELUDE =
        CompilerConfigurationKey.create<Boolean>("NO_PRELUDE")

    /** 测试/命令行注入的项目 API level。 */
    @JvmField
    val API_LEVEL =
        CompilerConfigurationKey.create<Int>("API_LEVEL")

    /** 测试/命令行注入的 syscap 配置文件路径。 */
    @JvmField
    val API_LEVEL_SYSCAP_CONFIG_PATH =
        CompilerConfigurationKey.create<String>("API_LEVEL_SYSCAP_CONFIG_PATH")

    /** 是否按可执行编译目标检查程序入口。 */
    @JvmField
    val CHECK_PROGRAM_ENTRY =
        CompilerConfigurationKey.create<Boolean>("CHECK_PROGRAM_ENTRY")
}

/**
 * 诊断工厂存储扩展属性。
 *
 * 对齐 Kotlin 声明：`CompilerConfiguration.diagnosticFactoriesStorage`。
 */
var CompilerConfiguration.diagnosticFactoriesStorage: CjRegisteredDiagnosticFactoriesStorage?
    get() = get(CfirFrontendConfigurationKeys.DIAGNOSTIC_FACTORIES_STORAGE)
    set(value) {
        put(
            CfirFrontendConfigurationKeys.DIAGNOSTIC_FACTORIES_STORAGE,
            requireNotNull(value) { "nullable values are not allowed" },
        )
    }

/**
 * 是否关闭默认 prelude 注入。
 *
 * 该开关影响源码 session 初始化时注册的 [CfirPreludeSettingsComponent]，用于测试或特殊 CLI
 * 场景显式屏蔽标准 prelude 声明。
 */
var CompilerConfiguration.noPrelude: Boolean
    get() = getBoolean(CfirFrontendConfigurationKeys.NO_PRELUDE)
    set(value) {
        put(CfirFrontendConfigurationKeys.NO_PRELUDE, value)
    }

/**
 * 当前项目声明的 API level。
 *
 * 返回 `null` 表示配置未显式提供 API level，后续由 syscap 配置或
 * [CfirApiLevelProvider.DISABLED] 决定最终行为。
 */
var CompilerConfiguration.apiLevel: Int?
    get() = get(CfirFrontendConfigurationKeys.API_LEVEL)
    set(value) {
        if (value == null) return
        put(CfirFrontendConfigurationKeys.API_LEVEL, value)
    }

/**
 * API level 与 syscap 配置文件路径。
 *
 * 该路径由前端 session factory 解析为 [CfirApiLevelProvider]，用于统一生产入口与测试 facade
 * 的 API/syscap 能力判断。
 */
var CompilerConfiguration.apiLevelSyscapConfigPath: String?
    get() = get(CfirFrontendConfigurationKeys.API_LEVEL_SYSCAP_CONFIG_PATH)
    set(value) {
        if (value == null) return
        put(CfirFrontendConfigurationKeys.API_LEVEL_SYSCAP_CONFIG_PATH, value)
    }

/**
 * 是否按可执行目标检查程序入口。
 *
 * 该配置最终进入 [CfirProgramEntrySettingsComponent]，由声明 checker 在完成 resolve 后决定是否
 * 上报缺失入口诊断。
 */
var CompilerConfiguration.checkProgramEntry: Boolean
    get() = getBoolean(CfirFrontendConfigurationKeys.CHECK_PROGRAM_ENTRY)
    set(value) {
        put(CfirFrontendConfigurationKeys.CHECK_PROGRAM_ENTRY, value)
    }
