package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.CangJieTarget
import org.cangnova.cangjie.cfir.analysis.checkers.SourceModifier
import org.cangnova.cangjie.cfir.analysis.checkers.actualParentTargets
import org.cangnova.cangjie.cfir.analysis.checkers.actualTargetsFor
import org.cangnova.cangjie.cfir.analysis.checkers.checkModifiersCompatibility
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.deprecatedParentTargetMap
import org.cangnova.cangjie.cfir.analysis.checkers.deprecatedTargetMap
import org.cangnova.cangjie.cfir.analysis.checkers.firstOrThisDescription
import org.cangnova.cangjie.cfir.analysis.checkers.isConstructorSource
import org.cangnova.cangjie.cfir.analysis.checkers.modifierByToken
import org.cangnova.cangjie.cfir.analysis.checkers.possibleParentTargetPredicateMap
import org.cangnova.cangjie.cfir.analysis.checkers.possibleTargetMap
import org.cangnova.cangjie.cfir.analysis.checkers.realSourceModifiers
import org.cangnova.cangjie.cfir.analysis.checkers.redundantTargetMap
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.lexer.CjTokens

object CfirModifierChecker : CfirBasicDeclarationChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirDeclaration) {
        val source = declaration.source ?: return
        if (declaration is CfirConstructor && declaration.isPrimary && !source.isConstructorSource()) {
            return
        }

        val modifiers = source.realSourceModifiers() ?: return
        val reportedNodes = hashSetOf<SourceModifier>()

        checkModifiersCompatibility(declaration, modifiers, reportedNodes)

        val actualTargets = context.actualTargetsFor(declaration)
        val actualParents = context.actualParentTargets()

        for (modifier in modifiers) {
            if (modifier in reportedNodes) continue
            when {
                !checkTarget(modifier, actualTargets) -> reportedNodes += modifier
                !checkParent(modifier, actualParents) -> reportedNodes += modifier
            }
        }

        checkOverrideAndRedef(declaration, modifiers)
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkTarget(
        modifier: SourceModifier,
        actualTargets: List<CangJieTarget>,
    ): Boolean {
        val modifierToken = modifier.token

        val isWrongTarget = actualTargets.none {
            possibleTargetMap[modifierToken]?.contains(it) == true
        }
        if (isWrongTarget) {
            reporter.reportOn(
                modifier.source,
                CfirErrors.WRONG_MODIFIER_TARGET,
                modifierToken,
                actualTargets.firstOrThisDescription(),
            )
            return false
        }

        val deprecatedTargets = deprecatedTargetMap[modifierToken]
        if (deprecatedTargets != null && actualTargets.any { it in deprecatedTargets }) {
            reporter.reportOn(
                modifier.source,
                CfirErrors.DEPRECATED_MODIFIER_FOR_TARGET,
                modifierToken,
                actualTargets.firstOrThisDescription(),
            )
            return true
        }

        val redundantTargets = redundantTargetMap[modifierToken]
        if (redundantTargets != null && actualTargets.any { it in redundantTargets }) {
            reporter.reportOn(
                modifier.source,
                CfirErrors.REDUNDANT_MODIFIER_FOR_TARGET,
                modifierToken,
                actualTargets.firstOrThisDescription(),
            )
        }

        return true
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkParent(
        modifier: SourceModifier,
        actualParents: List<CangJieTarget>,
    ): Boolean {
        val modifierToken = modifier.token

        val deprecatedParents = deprecatedParentTargetMap[modifierToken]
        if (deprecatedParents != null && actualParents.any { it in deprecatedParents }) {
            reporter.reportOn(
                modifier.source,
                CfirErrors.DEPRECATED_MODIFIER_CONTAINING_DECLARATION,
                modifierToken,
                actualParents.firstOrThisDescription(),
            )
            return true
        }

        val possibleParentPredicate = possibleParentTargetPredicateMap[modifierToken] ?: return true
        if (actualParents.any { possibleParentPredicate.isAllowed(it, context.languageVersionSettings) }) {
            return true
        }

        reporter.reportOn(
            modifier.source,
            CfirErrors.WRONG_MODIFIER_CONTAINING_DECLARATION,
            modifierToken,
            actualParents.firstOrThisDescription(),
        )
        return false
    }

    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkOverrideAndRedef(
        declaration: CfirDeclaration,
        modifiers: List<SourceModifier>,
    ) {
        val callable = declaration as? CfirCallableDeclaration ?: return

        if (callable.status.isRedef && !callable.status.isStatic) {
            val redefModifier = modifiers.modifierByToken(CjTokens.REDEF_KEYWORD) ?: return
            reporter.reportOn(
                redefModifier.source,
                CfirErrors.REDEF_INSTANCE_ERROR,
                callable.declarationKindName(),
            )
        }

        if (callable.status.isOverride && callable.status.isStatic) {
            val overrideModifier = modifiers.modifierByToken(CjTokens.OVERRIDE_KEYWORD) ?: return
            reporter.reportOn(
                overrideModifier.source,
                CfirErrors.OVERRIDE_STATIC_ERROR,
                callable.declarationKindName(),
            )
        }
    }

    private fun CfirCallableDeclaration.declarationKindName(): String = when (this) {
        is CfirNamedFunction -> "function"
        is CfirProperty -> "property"
        else -> "member"
    }
}
