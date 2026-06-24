package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.name.Name

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
    /**
     * Deprecated 注解名。
     */
    private val DEPRECATED = Name.identifier("Deprecated")

    /**
     * 检查 override/redef 声明与父声明之间的 Deprecated 严格级别兼容性。
     */
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

        val parentDecl = findOverriddenDeclaration(declaration)
        val parentHasDeprecated = parentDecl?.let { hasDeprecatedAnnotation(it) } ?: false
        val parentIsError = parentDecl?.let { isDeprecatedErrorLevel(it) } ?: false

        if (parentHasDeprecated && !selfHasDeprecated) {
            if (isOverride) {
                val factory = if (parentIsError)
                    CfirErrors.DEPRECATION_OVERRIDE_ERROR
                else
                    CfirErrors.DEPRECATION_OVERRIDE_WARNING
                reporter.reportOn(
                    source = declaration.source,
                    factory = factory,
                    a = kind,
                    b = declName,
                )
            }
            if (isRedef) {
                val factory = if (parentIsError)
                    CfirErrors.DEPRECATION_REDEF_ERROR
                else
                    CfirErrors.DEPRECATION_REDEF_WARNING
                reporter.reportOn(
                    source = declaration.source,
                    factory = factory,
                    a = kind,
                    b = declName,
                )
            }
        }

        if (selfHasDeprecated && parentHasDeprecated) {
            val selfIsError = isDeprecatedErrorLevel(declaration)
            if (parentIsError && !selfIsError) {
                reporter.reportOn(
                    source = declaration.source,
                    factory = CfirErrors.DEPRECATION_WEAKENING,
                )
            }
        }
    }

    /**
     * 在父类型中查找被 override/redef 的对应声明（同名同 kind）。
     */
    context(context: CheckerContext)
    private fun findOverriddenDeclaration(declaration: CfirCallableDeclaration): CfirDeclaration? {
        val ownerClassId = (declaration.symbol as? CfirCallableSymbol<*>)?.callableId?.classId ?: return null
        val ownerSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId) ?: return null
        val ownerDecl = ownerSymbol.cfir as? org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration ?: return null
        val declName = when (declaration) {
            is CfirNamedFunction -> declaration.name
            is CfirProperty -> declaration.name
            else -> return null
        }
        for (superRef in ownerDecl.superTypeRefs) {
            val t = (superRef as? CfirResolvedTypeRef)?.coneType as? ConeClassLikeType ?: continue
            val sd = context.session.symbolProvider.getClassLikeSymbolByClassId(t.classId)?.cfir
                as? org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration ?: continue
            val match = sd.declarations.firstOrNull { m ->
                when (declaration) {
                    is CfirNamedFunction -> m is CfirNamedFunction && m.name == declName
                    is CfirProperty -> m is CfirProperty && m.name == declName
                    else -> false
                }
            }
            if (match != null) return match
        }
        return null
    }

    /**
     * 判断声明是否带 `@Deprecated` 注解。
     */
    private fun hasDeprecatedAnnotation(declaration: CfirDeclaration): Boolean {
        return declaration.hasAnnotation(DEPRECATED)
    }

    /**
     * 对齐 C++ `IsDeprecatedStrict` (Utils.cpp:571):
     * `@Deprecated(strict: true)` 为 ERROR 级别,否则为 WARNING。
     */
    private fun isDeprecatedErrorLevel(declaration: CfirDeclaration): Boolean {
        val ann = declaration.findAnnotations(DEPRECATED).firstOrNull() as? CfirAnnotationCall ?: return false
        return ann.booleanArgument("strict") == true
    }
}
