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

package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.name.Name

/**
 * 对齐官方 `mut/immutable` 核心语义的第一步：
 * 在 struct 的非 mut 成员函数中，`this` 视角下的可变操作必须被拦截。
 *
 * 这一批先只处理最稳定的两类行为：
 * 1. 赋值到当前实例字段；
 * 2. 调用当前实例上的 mut 成员函数。
 */
object CfirImmutableFunctionCannotModifyFieldChecker : CfirAssignmentChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirAssignment) {
        context.currentImmutableStructFunction() ?: return

        val lValue = expression.lValue as? CfirQualifiedAccessExpression ?: return
        if (!lValue.isCurrentStructReceiverAccess()) return

        val fieldSymbol = lValue.resolvedFieldSymbolOrNull() ?: return
        val field = fieldSymbol.takeIf { it.isBound }?.cfir as? CfirFieldVariable ?: return
        if (!field.isVar) return

        reporter.reportOn(
            source = lValue.calleeReference.source ?: lValue.source ?: expression.source,
            factory = CfirErrors.CANNOT_MODIFY_VAR,
            a = field.name,
        )
    }
}

object CfirImmutableFunctionCannotAccessMutableFunctionChecker : CfirFunctionCallChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirFunctionCall) {
        val currentFunction = context.currentImmutableStructFunction() ?: return
        if (!expression.isCurrentStructReceiverAccess()) return

        val targetSymbol = expression.resolvedFunctionSymbolOrNull() ?: return
        val targetFunction = targetSymbol.takeIf { it.isBound }?.cfir as? CfirNamedFunction ?: return
        if (!targetFunction.status.isMut || targetFunction.status.isConst) return

        reporter.reportOn(
            source = expression.calleeReference.source ?: expression.source,
            factory = CfirErrors.IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION,
            a = currentFunction.name,
            b = targetFunction.name,
        )
    }
}

/**
 * 不可变 struct 值禁止调用 mut 成员函数。
 *
 * 对齐官方 `TypeCheckAccess::CheckLetInstanceAccessMutableFunc`：`let`/`const`
 * 的 struct 值、属性值以及非 class-like 的临时值，不能作为 mut 函数调用的接收者。
 */
object CfirImmutableValueCannotAccessMutableFunctionChecker : CfirFunctionCallChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirFunctionCall) {
        val receiver = expression.explicitReceiver ?: return
        val targetSymbol = expression.resolvedFunctionSymbolOrNull() ?: return
        val targetFunction = targetSymbol.takeIf { it.isBound }?.cfir as? CfirNamedFunction ?: return
        if (!targetFunction.status.isMut || targetFunction.status.isConst) return
        if (!receiver.isImmutableStructValueAccess()) return

        reporter.reportOn(
            source = receiver.source ?: expression.source,
            factory = CfirErrors.IMMUTABLE_FUNCTION_CANNOT_ACCESS_MUTABLE_FUNCTION,
            a = receiver.diagnosticNameOr(targetFunction.name),
            b = targetFunction.name,
        )
    }
}

private fun CheckerContext.currentImmutableStructFunction(): CfirNamedFunction? {
    val function = findClosestDeclaration<CfirNamedFunction>() ?: return null
    if (function.status.isMut) return null
    if (findClosestDeclaration<CfirStruct>() == null) return null
    return function
}

private fun CfirQualifiedAccessExpression.isCurrentStructReceiverAccess(): Boolean {
    return explicitReceiver == null || explicitReceiver is CfirThisReceiverExpression
}

private fun CfirQualifiedAccessExpression.resolvedFieldSymbolOrNull(): CfirFieldVariableSymbol? {
    return when (val reference = calleeReference) {
        is CfirResolvedNamedReference -> reference.resolvedSymbol as? CfirFieldVariableSymbol
        is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol as? CfirFieldVariableSymbol
        else -> null
    }
}

private fun CfirFunctionCall.resolvedFunctionSymbolOrNull(): CfirFunctionSymbol<*>? {
    return when (val reference = calleeReference) {
        is CfirResolvedNamedReference -> reference.resolvedSymbol as? CfirFunctionSymbol<*>
        is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol as? CfirFunctionSymbol<*>
        else -> null
    }
}

private fun CfirQualifiedAccessExpression.resolvedVariableOrPropertySymbolOrNull(): org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol<*>? {
    return when (val reference = calleeReference) {
        is CfirResolvedNamedReference -> reference.resolvedSymbol
        is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol
        else -> null
    }
}

private fun CfirExpression.isImmutableStructValueAccess(): Boolean {
    if (this is CfirThisReceiverExpression || this is CfirSuperReceiverExpression) return false
    if (!coneTypeOrNull.mayBeStructValueType()) return false

    val access = this as? CfirQualifiedAccessExpression ?: return true
    val symbol = access.resolvedVariableOrPropertySymbolOrNull()
    val receiver = access.explicitReceiver ?: access.dispatchReceiver
    return when (symbol) {
        is CfirVariableSymbol<*> -> {
            val variable = symbol.takeIf { it.isBound }?.cfir ?: return true
            if (!variable.isVar) return true
            receiver?.isImmutableStructValueAccess() == true
        }

        is CfirPropertySymbol -> true
        else -> true
    }
}

private fun CfirExpression.diagnosticNameOr(defaultName: Name): Name {
    val access = this as? CfirQualifiedAccessExpression ?: return defaultName
    return when (val symbol = access.resolvedVariableOrPropertySymbolOrNull()) {
        is CfirVariableSymbol<*> -> symbol.name
        is CfirPropertySymbol -> (symbol.takeIf { it.isBound }?.cfir as? CfirProperty)?.name ?: symbol.name
        else -> defaultName
    }
}
