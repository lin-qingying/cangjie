/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.*
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
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
            if (callable.hasInheritedNonStaticSignatureIgnoringStatic()) {
                reporter.reportOn(
                    redefModifier.source,
                    CfirErrors.NOTHING_TO_OVERRIDE,
                )
            }
        }

    }

    context(context: CheckerContext)
    private fun CfirCallableDeclaration.hasInheritedNonStaticSignatureIgnoringStatic(): Boolean {
        val owner = context.findClosestDeclaration<CfirClassLikeDeclaration>() ?: return false
        val classScope = context.createUseSiteMemberScope(owner)
        return when (val symbol = symbol) {
            is CfirNamedFunctionSymbol -> classScope
                .collectDirectOverriddenFunctionsIgnoringStatic(symbol)
                .any { !it.cfir.status.isStatic && it.canParticipateInOverrideTargetSearch(owner, context) }

            is CfirPropertySymbol -> classScope
                .collectDirectOverriddenPropertiesIgnoringStatic(symbol)
                .any { !it.cfir.status.isStatic && it.canParticipateInOverrideTargetSearch(owner, context) }

            else -> false
        }
    }

    private fun CfirCallableDeclaration.declarationKindName(): String = when (this) {
        is CfirNamedFunction -> "function"
        is CfirProperty -> "property"
        else -> "member"
    }
}
