package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirStruct
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

        /*
         * 官方 `DeclAttributeChecker::CheckDeprecationOfOverride` / `CheckDeprecationOfRedef` 不要求
         * 子声明显式写 override / redef：接口成员的实现按 override 报告，遮蔽父类 static 成员按 redef
         * 报告（cjc 1.0.5 实测 `class C <: I` 实现弃用接口函数、`class F <: E` 遮蔽弃用 static 函数）。
         */
        val parentIsStatic = when (val parent = parentDecl) {
            is CfirNamedFunction -> parent.status.isStatic
            is CfirProperty -> parent.status.isStatic
            else -> false
        }
        val effectiveOverride = isOverride || (!isRedef && parentDecl != null && !parentIsStatic)
        val effectiveRedef = isRedef || (!isOverride && parentDecl != null && parentIsStatic)

        if (parentHasDeprecated && !selfHasDeprecated) {
            if (effectiveOverride) {
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
            if (effectiveRedef) {
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
        return declaration.hasBuiltinAnnotation(BuiltInAnnotationKind.DEPRECATED)
    }

    /**
     * 对齐 C++ `IsDeprecatedStrict` (Utils.cpp:571):
     * `@Deprecated(strict: true)` 为 ERROR 级别,否则为 WARNING。
     */
    private fun isDeprecatedErrorLevel(declaration: CfirDeclaration): Boolean {
        val ann = declaration.findBuiltinAnnotations(BuiltInAnnotationKind.DEPRECATED)
            .firstOrNull() as? CfirAnnotationCall ?: return false
        return ann.booleanArgument("strict") == true
    }
}

/**
 * class-like 级弃用继承检查器
 *
 * 对齐 C++ `TypeChecker::CheckDeprecationLevelOnInheritors`：
 * 弃用 class-like 的直接子声明（不含自身注解）按父级严格度报
 * DEPRECATION_OVERRIDE_ERROR / _WARNING；子声明带非 strict 注解时报 DEPRECATION_WEAKENING。
 */
object CfirDeprecatedClassLikeChecker : CfirClassLikeChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        val parent = declaration.deprecatedClassLikeSupertype() ?: return
        val parentIsStrict = parent.isDeprecatedErrorLevel()
        val declName = declaration.name
        val kind = when (declaration) {
            is CfirClass -> "class"
            is CfirInterface -> "interface"
            is CfirStruct -> "struct"
            is CfirEnum -> "enum"
            else -> "declaration"
        }

        val selfHasDeprecated = declaration.hasBuiltinAnnotation(BuiltInAnnotationKind.DEPRECATED)
        if (selfHasDeprecated) {
            if (parentIsStrict && !declaration.isDeprecatedErrorLevel()) {
                reporter.reportOn(source = declaration.source, factory = CfirErrors.DEPRECATION_WEAKENING)
            }
            return
        }

        reporter.reportOn(
            source = declaration.source,
            factory = if (parentIsStrict) CfirErrors.DEPRECATION_OVERRIDE_ERROR else CfirErrors.DEPRECATION_OVERRIDE_WARNING,
            a = kind,
            b = declName,
        )
    }

    /** 找出第一个带 `@Deprecated` 的父 class-like 声明。 */
    context(context: CheckerContext)
    private fun CfirClassLikeDeclaration.deprecatedClassLikeSupertype(): CfirClassLikeDeclaration? {
        for (superTypeRef in superTypeRefs) {
            val coneType = (superTypeRef as? CfirResolvedTypeRef)?.coneType as? ConeClassLikeType ?: continue
            val superDecl = context.session.symbolProvider.getClassLikeSymbolByClassId(coneType.classId)?.cfir
                as? CfirClassLikeDeclaration ?: continue
            if (superDecl.hasBuiltinAnnotation(BuiltInAnnotationKind.DEPRECATED)) return superDecl
        }
        return null
    }

    /** 读取 `@Deprecated(strict: true)`。 */
    private fun CfirDeclaration.isDeprecatedErrorLevel(): Boolean {
        val annotation = findBuiltinAnnotations(BuiltInAnnotationKind.DEPRECATED)
            .firstOrNull() as? CfirAnnotationCall ?: return false
        return annotation.booleanArgument("strict") == true
    }
}
