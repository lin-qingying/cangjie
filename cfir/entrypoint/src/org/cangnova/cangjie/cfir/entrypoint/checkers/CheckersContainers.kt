package org.cangnova.cangjie.cfir.entrypoint.checkers

import org.cangnova.cangjie.cfir.analysis.checkers.CommonDeclarationCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.CommonExpressionCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.CommonLanguageVersionSettingsCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.CommonTypeCheckers
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
