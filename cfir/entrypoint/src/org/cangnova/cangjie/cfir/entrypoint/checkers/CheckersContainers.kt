package org.cangnova.cangjie.cfir.entrypoint.checkers

import org.cangnova.cangjie.cfir.analysis.checkers.CommonDeclarationCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.CommonExpressionCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.CommonLanguageVersionSettingsCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.CommonTypeCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.ExtraDeclarationCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.ExtraExpressionCheckers
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.entrypoint.session.CfirSessionConfigurator

/**
 * 通用检查器注册（对齐 K2 的 CheckersContainers.kt）。
 *
 * 将基础的、跨平台共享的检查器注册到 session 中。
 * 平台特有的检查器由各平台 SessionFactory 的 registerPlatformCheckers() 注册。
 */
fun CfirSessionConfigurator.registerCommonCheckers() {
    useCheckers(CommonDeclarationCheckers)
    useCheckers(CommonExpressionCheckers)
    useCheckers(CommonTypeCheckers)
    useCheckers(CommonLanguageVersionSettingsCheckers)
    registerDiagnosticContainers(CfirErrors )
}

/**
 * 额外检查器注册（对齐 K2 的 Extra*Checkers）。
 *
 * unused/DCE 与 effects 等附加 warning 不属于主干 type-check 集合，
 * 仅在测试或 IDE 诊断过滤显式请求 extra checker 时挂载。
 */
fun CfirSessionConfigurator.registerExtraCommonCheckers() {
    useCheckers(ExtraDeclarationCheckers)
    useCheckers(ExtraExpressionCheckers)
}

/**
 * Hook that mirrors Kotlin FIR test infrastructure.
 * Cangjie currently has no dedicated experimental checker set.
 */
fun CfirSessionConfigurator.registerExperimentalCheckers() {
}
