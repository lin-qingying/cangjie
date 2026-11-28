package org.cangnova.cangjie.cfir.resolve.source

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.patterns.simpleVariableBindingOrNull
import org.cangnova.cangjie.cfir.source.*
import org.cangnova.cangjie.cfir.types.*
import java.math.BigInteger

/** v113 ForInExpr 的范围/iterator 分支；生成图独立于原 CFG，Jump 使用明确的 loop 映射。 */
internal class CfirSourceForInCanonicalizer(private val context: CfirSourceGraphProducer.Context) {
    fun produce(loop: CfirForInExpression, owner: CfirDeclaration, identity: CfirSourceExpressionIdentity): CfirSourceExpressionPayload {
        // 保存后的非 DIRECT For 已持有官方完成的 iterable Block、模式和 kind；
        // 再按原始 iterable 调 iterator 会改变实际阶段产品及控制目标。
        if (loop.iterationKind != CfirSourceForInKind.DIRECT) {
            check(loop.iterable is CfirBlock) { "A completed native For must retain its actual iterable Block" }
            context.bindLoop(loop, identity)
            return CfirSourceExpressionPayload.ForIn(
                context.expression(loop.iterable, owner),
                loop.patternGuard?.let { context.expression(it, owner) },
                context.block(loop.body, owner),
                context.pattern(loop.pattern, owner),
                loop.iterationKind,
            )
        }
        val range = loop.iterable as? CfirRangeExpression
        val step = range?.step?.let(::integerConstant) ?: if (range?.step == null) BigInteger.ONE else null
        val integerRange = range != null && (context.typeOf(range.start) as? ConePrimitiveType)?.kind?.isInteger == true &&
            (context.typeOf(range.end) as? ConePrimitiveType)?.kind?.isInteger == true
        if (integerRange && step?.abs() == BigInteger.ONE) {
            context.bindLoop(loop, identity)
            checkNotNull(range)
            if (range.isInclusive) return CfirSourceExpressionPayload.ForIn(context.expression(range, owner),
                loop.patternGuard?.let { context.expression(it, owner) }, context.block(loop.body, owner),
                context.pattern(loop.pattern, owner), CfirSourceForInKind.DIRECT)
            val type = context.typeOf(range.start)
            val binding = loop.pattern.simpleVariableBindingOrNull()
            val index = context.freshVariable(binding?.name?.asString()?.let { "\$iter-$it" } ?: "\$index-compiler", type, true, owner, loop.pattern)
            val stop = context.freshVariable("\$stop-compiler", context.typeOf(range.end), false, owner, range.end)
            context.builder.initializer(index, context.expression(range.start, owner))
            context.builder.initializer(stop, context.expression(range.end, owner))
            binding?.let { context.builder.initializer(it, context.reference(index, owner)) }
            val update = context.generated(owner, ConePrimitiveType.UNIT, CfirSourceExpressionPayload.Assignment(
                context.reference(index, owner), range.step?.let { context.expression(it, owner) }
                    ?: context.literal(owner, type, CfirLiteralKind.INT, "1"), CfirSourceOperator.ADD_ASSIGN, true))
            val increasing = checkNotNull(step).signum() > 0
            val condition = context.generated(owner, ConePrimitiveType.BOOLEAN, CfirSourceExpressionPayload.Binary(
                context.reference(index, owner), context.reference(stop, owner),
                if (increasing) CfirSourceOperator.LESS else CfirSourceOperator.GREATER))
            val iterable = context.generatedBlock(owner, listOf(CfirSourceSlot.Declaration(index), CfirSourceSlot.Declaration(stop),
                CfirSourceSlot.Expression(update), CfirSourceSlot.Expression(condition)))
            return CfirSourceExpressionPayload.ForIn(iterable, loop.patternGuard?.let { context.expression(it, owner) },
                context.block(loop.body, owner), context.pattern(loop.pattern, owner), CfirSourceForInKind.RANGE)
        }
        return iterator(loop, owner)
    }

    fun range(range: CfirRangeExpression, owner: CfirDeclaration): CfirSourceExpressionPayload.Call {
        val values = listOf(context.expression(range.start, owner), context.expression(range.end, owner),
            range.step?.let { context.expression(it, owner) } ?: context.literal(owner, ConePrimitiveType.INT64, CfirLiteralKind.INT, "1"),
            context.boolean(owner, true), context.boolean(owner, true), context.boolean(owner, range.isInclusive))
        return context.constructorCall(context.typeOf(range), values, owner)
    }

    private fun iterator(loop: CfirForInExpression, owner: CfirDeclaration): CfirSourceExpressionPayload.Block {
        val iterableType = context.typeOf(loop.iterable)
        val iteratorCall = context.memberCall(context.expression(loop.iterable, owner), iterableType, "iterator", emptyList(), owner)
        val iteratorType = context.builder.record(iteratorCall).type
        val iterator = context.freshVariable("\$iterator-compiler", iteratorType, false, owner, loop)
        context.builder.initializer(iterator, iteratorCall)
        val whileId = context.builder.reserveGenerated()
        context.bindLoop(loop, whileId)
        val next = context.memberCall(context.reference(iterator, owner), iteratorType, "next", emptyList(), owner)
        val nextType = context.builder.record(next).type as? ConeEnumType ?: error("SOURCE iterator.next requires its actual Option result")
        val elementType = nextType.typeArguments.single().type
        val value = context.freshVariable("v-compiler", elementType, false, owner)
        val none = CfirSourcePattern(CfirSourcePatternKind.ENUM, listOf(nextType),
            listOf(CfirSourceSlot.Expression(context.enumReference(nextType, "None", owner))), emptyList())
        val some = CfirSourcePattern(CfirSourcePatternKind.ENUM, listOf(nextType),
            listOf(CfirSourceSlot.Expression(context.enumReference(nextType, "Some", owner))), listOf(context.variablePattern(value, elementType)))
        val jumpBreak = context.generated(owner, ConePrimitiveType.NOTHING, CfirSourceExpressionPayload.Jump(true, whileId))
        val jumpContinue = context.generated(owner, ConePrimitiveType.NOTHING, CfirSourceExpressionPayload.Jump(false, whileId))
        val body = context.block(loop.body, owner)
        val matched = context.generated(owner, ConePrimitiveType.UNIT, CfirSourceExpressionPayload.Match(context.reference(value, owner), listOf(
            context.generated(owner, context.typeOf(loop.body), CfirSourceExpressionPayload.MatchCase(
                context.alternatives(loop.pattern, owner), loop.patternGuard?.let { context.expression(it, owner) }, body)),
            context.generated(owner, ConePrimitiveType.NOTHING, CfirSourceExpressionPayload.MatchCase(listOf(context.wildcardPattern(elementType)),
                null, context.expressionBlock(owner, jumpContinue))),
        )))
        val select = context.generated(owner, ConePrimitiveType.UNIT, CfirSourceExpressionPayload.Match(next, listOf(
            context.generated(owner, ConePrimitiveType.NOTHING, CfirSourceExpressionPayload.MatchCase(listOf(none), null, context.expressionBlock(owner, jumpBreak))),
            context.generated(owner, ConePrimitiveType.UNIT, CfirSourceExpressionPayload.MatchCase(listOf(some), null, context.expressionBlock(owner, matched))),
        )))
        context.builder.fill(whileId, CfirSourceExpressionRecord(owner, ConePrimitiveType.UNIT, CfirSourceSpan.capture(loop.source),
            CfirSourceExpressionPayload.Loop(context.boolean(owner, true), context.expressionBlock(owner, select), false)))
        return CfirSourceExpressionPayload.Block(listOf(CfirSourceSlot.Declaration(iterator), CfirSourceSlot.Expression(whileId)), false)
    }

    private fun integerConstant(expression: CfirExpression): BigInteger? = when (expression) {
        is CfirLiteralExpression -> if (expression.kind == CfirLiteralKind.INT) when (val value = expression.value) {
            is BigInteger -> value; is Long -> BigInteger.valueOf(value); is Int -> BigInteger.valueOf(value.toLong())
            else -> null
        } else null
        else -> null
    }
}
