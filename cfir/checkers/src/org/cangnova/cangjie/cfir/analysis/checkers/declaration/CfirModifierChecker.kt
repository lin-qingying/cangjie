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
import org.cangnova.cangjie.cfir.analysis.checkers.modifier.ModifierTarget
import org.cangnova.cangjie.cfir.analysis.checkers.modifier.ModifierTargetPredicate
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.lexer.CjTokens

/**
 * 源码修饰符目标、父声明和组合兼容性检查器。
 *
 * 该检查器从真实源码修饰符列表出发，统一处理修饰符目标、包含声明目标、兼容性以及
 * `redef`/`override` 相关的额外声明规则。
 */
object CfirModifierChecker : CfirBasicDeclarationChecker() {
    /**
     * 检查声明上的所有真实源码修饰符。
     */
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

    /**
     * 检查修饰符是否允许出现在当前声明目标上。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkTarget(
        modifier: SourceModifier,
        actualTargets: List<ModifierTarget>,
    ): Boolean {
        val modifierToken = modifier.token
        val possiblePredicate = possibleTargetMap[modifierToken]

        // 不在允许表中的修饰符视为无目标约束，直接通过。
        val isWrongTarget = possiblePredicate == null || actualTargets.none {
            possiblePredicate.isAllowed(it, context.languageVersionSettings)
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

        // 弃用目标谓词表，当前仓颉主干未启用项，保留作扩展点（对齐 Kotlin `deprecatedTargetPredicateMap`）。
        val deprecatedPredicate = deprecatedTargetMap[modifierToken]
        if (deprecatedPredicate != null && actualTargets.any { deprecatedPredicate.isAllowed(it, context.languageVersionSettings) }) {
            reporter.reportOn(
                modifier.source,
                CfirErrors.DEPRECATED_MODIFIER_FOR_TARGET,
                modifierToken,
                actualTargets.firstOrThisDescription(),
            )
            return true
        }

        val redundantPredicate = redundantTargetMap[modifierToken]
        if (redundantPredicate != null && actualTargets.any { redundantPredicate.isAllowed(it, context.languageVersionSettings) }) {
            reporter.reportOn(
                modifier.source,
                CfirErrors.REDUNDANT_MODIFIER_FOR_TARGET,
                modifierToken,
                actualTargets.firstOrThisDescription(),
            )
        }

        return true
    }

    /**
     * 检查修饰符是否允许出现在当前声明的父声明上下文中。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkParent(
        modifier: SourceModifier,
        actualParents: List<ModifierTarget>,
    ): Boolean {
        val modifierToken = modifier.token

        // 弃用父目标谓词表，当前仓颉主干未启用项，保留作扩展点（对齐 Kotlin `deprecatedParentTargetMap`）。
        val deprecatedParentPredicate = deprecatedParentTargetMap[modifierToken]
        if (deprecatedParentPredicate != null && actualParents.any { deprecatedParentPredicate.isAllowed(it, context.languageVersionSettings) }) {
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

    /**
     * 检查 `redef` 与实例成员覆盖语义的额外限制。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    private fun checkOverrideAndRedef(
        declaration: CfirDeclaration,
        modifiers: List<SourceModifier>,
    ) {
        val callable = declaration as? CfirCallableDeclaration ?: return

        val redefModifier = modifiers.modifierByToken(CjTokens.REDEF_KEYWORD)
        val staticModifier = modifiers.modifierByToken(CjTokens.STATIC_KEYWORD)
        if (redefModifier != null && staticModifier == null) {
            reporter.reportOn(
                redefModifier.source,
                CfirErrors.REDEF_INSTANCE_ERROR,
                callable.declarationKindName(),
            )
            if (callable.hasInheritedNonStaticSignatureIgnoringStatic()) {
                reporter.reportOn(
                    callable.source,
                    CfirErrors.NOTHING_TO_OVERRIDE,
                )
            }
        }

    }

    /**
     * 判断当前 callable 是否存在忽略 static 差异后的继承非 static 签名。
     */
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

    /**
     * 返回 callable 在诊断中的成员种类名称。
     */
    private fun CfirCallableDeclaration.declarationKindName(): String = when (this) {
        is CfirNamedFunction -> "function"
        is CfirProperty -> "property"
        else -> "member"
    }
}
