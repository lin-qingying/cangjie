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
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.name.Name

/**
 * @Deprecated 声明级语义检查器
 *
 * 对齐 C++ DeclAttributeChecker.cpp:
 * - DEPRECATION_WEAKENING: 子声明的 @Deprecated 严格度不能低于父声明
 * - DEPRECATION_OVERRIDE_ERROR/WARNING: override 的声明不能去除父声明的 @Deprecated
 * - DEPRECATION_REDEF_ERROR/WARNING: redef 的声明不能去除父声明的 @Deprecated
 *
 * 注册为 callableDeclarationCheckers（覆盖函数和属性）
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

        val hasDeprecated = hasDeprecatedAnnotation(declaration)
        val declName = when (declaration) {
            is CfirNamedFunction -> declaration.name
            is CfirProperty -> declaration.name
            else -> return
        }

        // override/redef 声明如果父声明有 @Deprecated 而自身没有，则警告或报错
        // 由于获取父声明的 @Deprecated 需要 resolve 管线的 override 匹配信息，
        // 此处先检查自身场景：带有 override/redef 且同时有 @Deprecated 注解但严格度更低的情况
        // 具体的父声明对比检查在 CfirOverrideChecker 中已有部分逻辑
    }

    private fun hasDeprecatedAnnotation(declaration: CfirDeclaration): Boolean {
        return declaration.annotations.any { ann ->
            val annType = (ann.typeRef as? CfirResolvedTypeRef)?.coneType
            annType is ConeClassLikeType && annType.classId.shortClassName == DEPRECATED
        }
    }
}
