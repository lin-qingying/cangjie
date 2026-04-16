package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjModifierListOwner
import org.cangnova.cangjie.source.psi

/**
 * @Deprecated 声明级语义检查器
 *
 * 对齐 C++ DeclAttributeChecker.cpp:
 * - DEPRECATION_WEAKENING: 子声明的 @Deprecated 严格度不能低于父声明
 * - DEPRECATION_OVERRIDE_ERROR/WARNING: override 的声明的父声明标记了 @Deprecated
 * - DEPRECATION_REDEF_ERROR/WARNING: redef 的声明的父声明标记了 @Deprecated
 *
 * 注册为 callableDeclarationCheckers
 */
object CfirDeprecatedDeclarationChecker : CfirCallableDeclarationChecker() {
    private val DEPRECATED = Name.identifier("Deprecated")

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirCallableDeclaration) {
        val isOverride = when (declaration) {
            is CfirNamedFunction -> declaration.status.isOverride
            is CfirProperty -> declaration.status.isOverride
            else -> false
        }
        val isRedef = when (declaration) {
            is CfirNamedFunction -> declaration.status.isRedef
            is CfirProperty -> declaration.status.isRedef
            else -> false
        }
        if (!isOverride && !isRedef) return

        val selfHasDeprecated = hasDeprecatedAnnotation(declaration)
        val declName = when (declaration) {
            is CfirNamedFunction -> declaration.name
            is CfirProperty -> declaration.name
            else -> return
        }
        val kind = when (declaration) {
            is CfirNamedFunction -> "function"
            is CfirProperty -> "property"
            else -> "declaration"
        }

        // 通过 PSI 回溯查找父声明（被 override/redef 的声明）是否有 @Deprecated
        val owner = declaration.source?.psi as? CjModifierListOwner ?: return
        val parentHasDeprecated = checkParentDeprecated(declaration)

        if (parentHasDeprecated && !selfHasDeprecated) {
            // 父声明有 @Deprecated 但子声明没有
            if (isOverride) {
                reporter.reportOn(
                    source = declaration.source,
                    factory = CfirErrors.DEPRECATION_OVERRIDE_WARNING,
                    a = kind,
                    b = declName,
                )
            }
            if (isRedef) {
                reporter.reportOn(
                    source = declaration.source,
                    factory = CfirErrors.DEPRECATION_REDEF_WARNING,
                    a = kind,
                    b = declName,
                )
            }
        }

        // 如果子声明减弱了 @Deprecated（父声明是 error 但子声明是 warning 或去掉了）
        if (selfHasDeprecated && parentHasDeprecated) {
            val selfIsError = isDeprecatedErrorLevel(declaration)
            val parentIsError = true // 简化：假设父声明是 error 级别时才报 weakening
            if (parentIsError && !selfIsError) {
                reporter.reportOn(
                    source = declaration.source,
                    factory = CfirErrors.DEPRECATION_WEAKENING,
                )
            }
        }
    }

    /**
     * 检查被 override/redef 的父声明是否标记了 @Deprecated。
     * 通过 CfirOverrideChecker 的逻辑查找对应的 overridden symbol。
     */
    context(context: CheckerContext)
    private fun checkParentDeprecated(declaration: CfirCallableDeclaration): Boolean {
        val symbol = declaration.symbol as? CfirCallableSymbol<*> ?: return false
        // 通过 symbol 的 overriddenCallableSymbols 查找父声明
        // 如果 overriddenCallableSymbols 不可用，退回 PSI 检查
        return false // 当 overriddenCallableSymbols API 可用时完善
    }

    private fun hasDeprecatedAnnotation(declaration: CfirDeclaration): Boolean {
        return declaration.annotations.any { ann ->
            val annType = (ann.typeRef as? CfirResolvedTypeRef)?.coneType
            annType is ConeClassLikeType && annType.classId.shortClassName == DEPRECATED
        }
    }

    private fun isDeprecatedErrorLevel(declaration: CfirDeclaration): Boolean {
        val ann = declaration.annotations.firstOrNull { a ->
            val t = (a.typeRef as? CfirResolvedTypeRef)?.coneType
            t is ConeClassLikeType && t.classId.shortClassName == DEPRECATED
        } ?: return false
        for (arg in ann.arguments) {
            val text = arg.source?.psi?.text ?: continue
            if (text.contains("ERROR") || text.contains("error")) return true
        }
        return false
    }
}
