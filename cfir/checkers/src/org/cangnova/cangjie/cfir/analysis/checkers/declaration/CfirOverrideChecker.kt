package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.SourceElementPositioningStrategies
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * Override checker aligned to Kotlin FIR order:
 * 1) check override target existence
 * 2) check override-target visibility
 * 3) check visibility compatibility
 * 4) check return type compatibility
 *
 * 中文说明：
 * 这里处理的是“声明级继承规则”，因此应放在 declaration checker 层。
 * 解析阶段只负责把可见性失败保留在引用/候选上，不在这里反向改写解析语义。
 */
object CfirOverrideChecker : CfirClassLikeChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        val classScope = context.createUseSiteMemberScope(declaration)

        for (member in declaration.declarations) {
            val callable = member as? CfirCallableDeclaration ?: continue
            if (!callable.isSourceDeclaration || !callable.hasOverrideLikeModifier()) continue
            if (!callable.isValidOverrideLikeDeclaration()) continue

            val overriddenCandidates = when (val symbol = callable.symbol) {
                is CfirFunctionSymbol<*> -> classScope.collectDirectOverriddenFunctions(symbol)
                is CfirPropertySymbol -> classScope.collectDirectOverriddenProperties(symbol)
                else -> emptyList()
            }

            if (overriddenCandidates.isEmpty()) {
                reporter.reportOn(
                    source = callable.source,
                    factory = CfirErrors.NOTHING_TO_OVERRIDE,
                )
                continue
            }

            val visibleOverriddenSymbols = overriddenCandidates.filter { it.isVisibleIn(declaration, context) }
            if (visibleOverriddenSymbols.isEmpty()) {
                // 这里区分“没有候选”和“有候选但全部不可见”，避免把继承可见性语义退化成 NOTHING_TO_OVERRIDE。
                reporter.reportOn(
                    source = callable.source,
                    factory = CfirErrors.CANNOT_OVERRIDE_INVISIBLE_MEMBER,
                    a = overriddenCandidates.first().name,
                )
                continue
            }

            checkVisibilityCompatibility(callable, visibleOverriddenSymbols)
            checkReturnTypeCompatibility(callable, visibleOverriddenSymbols)
        }
    }

    private fun CfirCallableDeclaration.hasOverrideLikeModifier(): Boolean {
        return status.isOverride || status.isRedef
    }

    private fun CfirCallableDeclaration.isValidOverrideLikeDeclaration(): Boolean {
        if (status.isRedef && !status.isStatic) return false
        if (status.isOverride && status.isStatic) return false
        return true
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkVisibilityCompatibility(
        declaration: CfirCallableDeclaration,
        overriddenSymbols: List<CfirCallableSymbol<*>>,
    ) {
        val declarationVisibility = declaration.status.visibility
        val firstIncompatibleOverridden = overriddenSymbols.firstOrNull { overridden ->
            val compareResult = Visibilities.compare(declarationVisibility, overridden.cfir.status.visibility)
            compareResult == null || compareResult < 0
        }
        if (firstIncompatibleOverridden == null) return

        reporter.reportOn(
            source = declaration.source,
            factory = CfirErrors.CANNOT_WEAKEN_ACCESS_PRIVILEGE,
            a = firstIncompatibleOverridden.name,
            b = firstIncompatibleOverridden.cfir.status.visibility,
            positioningStrategy = if (declaration.status.isVisibilityExplicit) {
                SourceElementPositioningStrategies.VISIBILITY_MODIFIER
            } else {
                null
            },
        )
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkReturnTypeCompatibility(
        declaration: CfirCallableDeclaration,
        overriddenSymbols: List<CfirCallableSymbol<*>>,
    ) {
        val declarationSymbol = declaration.symbol as? CfirCallableSymbol<*> ?: return
        val overridingReturnType = context.returnTypeCalculator.tryCalculateReturnType(declaration).coneType
        if (overridingReturnType is ConeErrorType) return

        for (overridden in overriddenSymbols) {
            if (!overridden.isBound) continue
            val overriddenReturnType = context.returnTypeCalculator.tryCalculateReturnType(overridden.cfir).coneType
            if (overriddenReturnType is ConeErrorType) continue

            val isCompatible = when {
                declarationSymbol is CfirPropertySymbol &&
                    overridden is CfirPropertySymbol &&
                    overridden.cfir.status.isMut -> AbstractTypeChecker.equalTypes(
                    context.session.typeContext,
                    overridingReturnType,
                    overriddenReturnType,
                )

                else -> AbstractTypeChecker.isSubtypeOf(
                    context.session.typeContext,
                    overridingReturnType,
                    overriddenReturnType,
                )
            }

            if (isCompatible) continue

            reporter.reportOn(
                source = declaration.source,
                factory = CfirErrors.OVERRIDING_RETURN_TYPE_MISMATCH,
                a = overridingReturnType,
                b = overriddenReturnType,
                c = overridden.name,
            )
            return
        }
    }
}
