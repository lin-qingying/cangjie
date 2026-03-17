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
