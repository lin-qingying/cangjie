package org.cangnova.cangjie.cfir.serialization.cjo

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.source.*

/**
 * 固定 Native ExprWriter 的实际拥有槽保存序。语义闭包的 expressionChildren 只用于依赖验证，
 * 不能裁定表序；本 helper 同时处理 Block 的 d/e、内联 F 和 P，不按来源拆分保存路径。
 */
internal fun CfirSourceExpressionPayload.visitSavedChildren(
    expression: (CfirSourceExpressionIdentity) -> Unit,
    declaration: (CfirDeclaration) -> Unit,
    pattern: (CfirSourcePattern) -> Unit,
    function: (CfirSourceFunctionBody) -> Unit,
    reference: (CfirSourceExpressionPayload.Reference) -> Unit,
    target: (CfirDeclaration, CfirSourceReferenceKind) -> Unit,
    savedReference: (CfirSourceSavedReference) -> Unit,
) {
    when (this) {
        CfirSourceExpressionPayload.PrimitiveType, CfirSourceExpressionPayload.Wildcard,
        is CfirSourceExpressionPayload.Jump, is CfirSourceExpressionPayload.Literal -> Unit
        is CfirSourceExpressionPayload.Reference -> reference(this)
        is CfirSourceExpressionPayload.Return -> expression(value)
        is CfirSourceExpressionPayload.Member -> { expression(receiver); reference(this.reference) }
        is CfirSourceExpressionPayload.Call -> { expression(base); arguments.forEach(expression) }
        is CfirSourceExpressionPayload.Unary -> expression(operand)
        is CfirSourceExpressionPayload.Increment -> expression(operand)
        is CfirSourceExpressionPayload.Binary -> { expression(left); expression(right) }
        is CfirSourceExpressionPayload.Subscript -> { expression(receiver); indices.forEach(expression) }
        is CfirSourceExpressionPayload.Assignment -> { expression(left); expression(right) }
        is CfirSourceExpressionPayload.Array -> {
            arguments.forEach(expression)
            initializer?.let { target(it, initializerKind) }
            savedInitializer?.let(savedReference)
        }
        is CfirSourceExpressionPayload.Pointer -> argument?.let(expression)
        is CfirSourceExpressionPayload.TypeConversion -> expression(value)
        is CfirSourceExpressionPayload.Throw -> expression(exception)
        is CfirSourceExpressionPayload.Perform -> expression(value)
        is CfirSourceExpressionPayload.Spawn -> { argument?.let(expression); declaration(future) }
        is CfirSourceExpressionPayload.ArrayLiteral -> {
            elements.forEach(expression)
            initializer?.let { target(it, initializerKind) }
            savedInitializer?.let(savedReference)
        }
        is CfirSourceExpressionPayload.Tuple -> elements.forEach(expression)
        is CfirSourceExpressionPayload.Conditional -> {
            expression(condition); expression(thenBranch); elseBranch?.let(expression)
        }
        is CfirSourceExpressionPayload.Try -> {
            resources.forEach(declaration)
            expression(body)
            catches.forEach(expression)
            finallyBlock?.let(expression)
            patterns.forEach(pattern)
        }
        is CfirSourceExpressionPayload.Loop -> {
            if (isDoWhile) { expression(body); expression(condition) }
            else { expression(condition); expression(body) }
        }
        is CfirSourceExpressionPayload.Lambda -> function(this.function)
        is CfirSourceExpressionPayload.Block -> statements.forEach { slot ->
            when (slot) {
                is CfirSourceSlot.Declaration -> declaration(slot.declaration)
                is CfirSourceSlot.Expression -> expression(slot.identity)
            }
        }
        is CfirSourceExpressionPayload.Match -> { selector?.let(expression); cases.forEach(expression) }
        is CfirSourceExpressionPayload.LetPatternDestructor -> { patterns.forEach(pattern); expression(initializer) }
        is CfirSourceExpressionPayload.ForIn -> {
            pattern(this.pattern); expression(iterable); guard?.let(expression); expression(body)
        }
        is CfirSourceExpressionPayload.MatchCase -> { guard?.let(expression); patterns.forEach(pattern); expression(body) }
        is CfirSourceExpressionPayload.MatchCaseOther -> { expression(condition); expression(body) }
        is CfirSourceExpressionPayload.Argument -> value?.let(expression)
    }
}

/** P 内的 d/e 与嵌套 P 使用实际槽序；selector 是借用边，永不进入此遍历。 */
internal fun CfirSourcePattern.visitSavedChildren(
    expression: (CfirSourceExpressionIdentity) -> Unit,
    declaration: (CfirDeclaration) -> Unit,
    pattern: (CfirSourcePattern) -> Unit,
) {
    slots.forEach { slot ->
        when (slot) {
            is CfirSourceSlot.Declaration -> declaration(slot.declaration)
            is CfirSourceSlot.Expression -> expression(slot.identity)
        }
    }
    patterns.forEach(pattern)
}

