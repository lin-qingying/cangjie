package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.argumentTextAt
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.findAnnotations
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.session.CfirApiLevelProvider
import org.cangnova.cangjie.cfir.session.apiLevelProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.name.Name

/**
 * APILevel 引用检查器。
 *
 * 对齐 C++:
 * - sema_apilevel_ref_higher (PluginCustomAnnoChecker.cpp): 调用 `@APILevel(N)` 声明
 *   时当前项目 APILevel < N 报错。
 * - sema_apilevel_syscap_error / _warning (PluginCustomAnnoChecker.cpp:555/566):
 *   调用 `@Syscap(s)` 声明时 s ∉ union 报 error, s ∉ intersection 报 warning。
 *
 * 通过 [CfirApiLevelProvider] 读项目级配置;未启用时跳过,避免非 ohos 场景误报。
 */
object CfirApiLevelRefHigherChecker : CfirFunctionCallChecker() {
    private val API_LEVEL = Name.identifier("APILevel")
    private val SYSCAP = Name.identifier("Syscap")

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirFunctionCall) {
        val provider = context.session.apiLevelProvider
        val projectLevel = provider.projectApiLevel
        val syscapActive = provider.syscapEnabled

        if (projectLevel == CfirApiLevelProvider.DISABLED && !syscapActive) return

        val ref = expression.calleeReference as? CfirResolvedNamedReference ?: return
        val target = (ref.resolvedSymbol as? CfirCallableSymbol<*>)?.cfir ?: return

        if (projectLevel != CfirApiLevelProvider.DISABLED) {
            val targetLevel = extractApiLevel(target)
            if (targetLevel != null && targetLevel > projectLevel) {
                reporter.reportOn(
                    source = expression.source,
                    factory = CfirErrors.APILEVEL_REF_HIGHER,
                    a = nameOf(target),
                    b = targetLevel,
                    c = projectLevel,
                )
            }
        }

        if (syscapActive) {
            val targetSyscap = extractSyscap(target)
            if (targetSyscap != null) {
                val nameArg = Name.identifier(targetSyscap)
                if (targetSyscap !in provider.syscapUnion) {
                    reporter.reportOn(
                        source = expression.source,
                        factory = CfirErrors.APILEVEL_SYSCAP_ERROR,
                        a = nameArg,
                    )
                } else if (targetSyscap !in provider.syscapIntersection) {
                    reporter.reportOn(
                        source = expression.source,
                        factory = CfirErrors.APILEVEL_SYSCAP_WARNING,
                        a = nameArg,
                    )
                }
            }
        }
    }

    private fun nameOf(decl: CfirDeclaration): Name = when (decl) {
        is CfirNamedFunction -> decl.name
        is CfirProperty -> decl.name
        is CfirClassLikeDeclaration -> decl.symbol.classId.shortClassName
        else -> Name.identifier("<unknown>")
    }

    private fun extractApiLevel(decl: CfirCallableDeclaration): Int? {
        val entry = findAnnotation(decl, API_LEVEL) ?: return null
        return entry.argumentTextAt(0)?.toIntOrNull()
    }

    private fun extractSyscap(decl: CfirCallableDeclaration): String? {
        val entry = findAnnotation(decl, SYSCAP) ?: return null
        return entry.argumentTextAt(0)
    }

    private fun findAnnotation(decl: CfirCallableDeclaration, annName: Name): CfirAnnotationCall? =
        decl.findAnnotations(annName).firstOrNull() as? CfirAnnotationCall
}
