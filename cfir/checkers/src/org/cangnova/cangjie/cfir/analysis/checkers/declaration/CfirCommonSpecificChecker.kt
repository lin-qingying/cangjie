package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn

/**
 * common/specific 跨平台检查器（CommonSpecific 分组）
 *
 * 对齐 C++ CJMP/ 目录:
 * - common 包中不能有 main 函数
 *
 * 注册为 classLikeCheckers
 */
object CfirCommonSpecificChecker : CfirClassLikeChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        if (declaration !is CfirClass) return
        if (!declaration.status.isCommon) return
        // common/specific 深层检查需要跨模块信息，当前框架先实现基本约束
    }
}

/**
 * common 包 main 函数检查器
 *
 * 对齐 C++ DiagKind::sema_common_package_has_main:
 * common 包中不允许定义 main 函数。
 *
 * 注册为 fileCheckers
 */
object CfirCommonPackageMainChecker : CfirFileChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: org.cangnova.cangjie.cfir.declarations.CfirFile) {
        // 检查文件中是否有 common main 函数
        for (decl in declaration.declarations) {
            if (decl is org.cangnova.cangjie.cfir.declarations.CfirMainFunction && decl.status.isCommon) {
                reporter.reportOn(
                    source = decl.source,
                    factory = CfirErrors.COMMON_PACKAGE_HAS_MAIN,
                )
            }
        }
    }
}

/**
 * Mock 语义检查器（Mock 分组）
 *
 * 注册为 classLikeCheckers
 * Mock 功能检查依赖编译选项 API，暂留空
 */
object CfirMockSemanticsChecker : CfirClassLikeChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        // TODO: 待编译选项 API 就绪后实现 MOCK_DISABLED / MOCK_NOT_IN_TEST_MODE 等检查
    }
}
