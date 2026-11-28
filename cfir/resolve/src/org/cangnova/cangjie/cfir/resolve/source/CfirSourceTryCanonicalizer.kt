package org.cangnova.cangjie.cfir.resolve.source

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression
import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider
import org.cangnova.cangjie.cfir.source.*
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.types.*

/** v113 TryExpr：资源逆序构图、顺序初始化、逆序释放；异常变量和每个模式 binding 独立。 */
internal class CfirSourceTryCanonicalizer(private val context: CfirSourceGraphProducer.Context) {
    fun produce(expression: CfirTryExpression, owner: CfirDeclaration): CfirSourceExpressionPayload.Try {
        check(expression.handlers.isEmpty()) { "Effect handlers require their completed effect SOURCE product" }
        var body = context.block(expression.tryBlock, owner)
        val catches = expression.catches.map { context.block(it.body, owner) }
        val patterns = expression.catches.map { context.catchPattern(it.pattern, owner) }
        var finallyBlock = expression.finallyBlock?.let { context.block(it, owner) }
        if (expression.resources.isNotEmpty()) {
            val exception = checkNotNull(owner.moduleData.session.symbolProvider.getClassLikeSymbolByClassId(StdlibClassIds.Exception)).constructType()
            val option = context.optionType(exception, owner)
            body = unitBlock(body, owner)
            expression.resources.asReversed().forEach { resource ->
                context.variable(resource)
                val fresh = context.freshVariable("v-freshExc", option, true, owner)
                context.builder.initializer(fresh, context.enumReference(option, "None", owner))
                val caught = context.freshVariable("v-compiler", exception, false, owner)
                val save = context.generated(owner, ConePrimitiveType.UNIT, CfirSourceExpressionPayload.Assignment(
                    context.reference(fresh, owner), context.enumCall(option, "Some", listOf(context.reference(caught, owner)), owner),
                    CfirSourceOperator.ASSIGN, false))
                val protect = context.generated(owner, ConePrimitiveType.UNIT, CfirSourceExpressionPayload.Try(body,
                    listOf(context.expressionBlock(owner, save)), listOf(exceptionPattern(caught, exception)),
                    resourceFinally(resource, fresh, exception, owner), emptyList()))
                body = context.generatedBlock(owner, listOf(CfirSourceSlot.Declaration(fresh), CfirSourceSlot.Declaration(resource), CfirSourceSlot.Expression(protect)))
            }
            if (catches.isEmpty() && finallyBlock == null) finallyBlock = context.generatedBlock(owner, emptyList())
        }
        return CfirSourceExpressionPayload.Try(body,
            if (expression.resources.isNotEmpty()) catches.map { unitBlock(it, owner) } else catches, patterns, finallyBlock, emptyList())
    }

    private fun unitBlock(identity: CfirSourceExpressionIdentity, owner: CfirDeclaration): CfirSourceExpressionIdentity {
        val record = context.builder.record(identity)
        if (record.type == ConePrimitiveType.UNIT) return identity
        val block = record.payload as CfirSourceExpressionPayload.Block
        return context.generatedBlock(owner, block.statements + CfirSourceSlot.Expression(context.unit(owner)))
    }

    private fun exceptionPattern(variable: CfirVariable, type: ConeCangJieType): CfirSourcePattern =
        CfirSourcePattern(CfirSourcePatternKind.EXCEPTION_TYPE, listOf(type, type), emptyList(), listOf(context.variablePattern(variable, type)))

    private fun resourceFinally(resource: CfirVariable, fresh: CfirVariable, exception: ConeCangJieType, owner: CfirDeclaration): CfirSourceExpressionIdentity {
        val type = context.typeOf(resource.returnTypeRef)
        val isClosed = context.memberCall(context.reference(resource, owner), type, "isClosed", emptyList(), owner)
        val condition = context.generated(owner, ConePrimitiveType.BOOLEAN, CfirSourceExpressionPayload.Unary(isClosed, CfirSourceOperator.NOT))
        val close = context.memberCall(context.reference(resource, owner), type, "close", emptyList(), owner)
        val conditional = context.generated(owner, ConePrimitiveType.UNIT, CfirSourceExpressionPayload.Conditional(condition, context.expressionBlock(owner, close), null))
        val closeException = context.freshVariable("v-compiler", exception, false, owner)
        val closeMatch = exceptionMatch(fresh, context.reference(closeException, owner), owner)
        val closeTry = context.generated(owner, ConePrimitiveType.UNIT, CfirSourceExpressionPayload.Try(context.expressionBlock(owner, conditional),
            listOf(context.expressionBlock(owner, closeMatch)), listOf(exceptionPattern(closeException, exception)), null, emptyList()))
        val rethrow = exceptionMatch(fresh, null, owner)
        return context.generatedBlock(owner, listOf(CfirSourceSlot.Expression(closeTry), CfirSourceSlot.Expression(rethrow)))
    }

    private fun exceptionMatch(fresh: CfirVariable, closeException: CfirSourceExpressionIdentity?, owner: CfirDeclaration): CfirSourceExpressionIdentity {
        val option = context.typeOf(fresh.returnTypeRef) as ConeEnumType
        val exception = option.typeArguments.single().type
        val saved = context.freshVariable("v-compiler", exception, false, owner)
        val some = CfirSourcePattern(CfirSourcePatternKind.ENUM, listOf(option),
            listOf(CfirSourceSlot.Expression(context.enumReference(option, "Some", owner))), listOf(context.variablePattern(saved, exception)))
        val none = CfirSourcePattern(CfirSourcePatternKind.ENUM, listOf(option),
            listOf(CfirSourceSlot.Expression(context.enumReference(option, "None", owner))), emptyList())
        val throwSaved = context.generated(owner, ConePrimitiveType.NOTHING, CfirSourceExpressionPayload.Throw(context.reference(saved, owner)))
        val noneValue = closeException?.let { context.generated(owner, ConePrimitiveType.NOTHING, CfirSourceExpressionPayload.Throw(it)) } ?: context.unit(owner)
        val cases = listOf(some to throwSaved, none to noneValue).map { (pattern, value) -> context.generated(owner,
            context.builder.record(value).type, CfirSourceExpressionPayload.MatchCase(listOf(pattern), null, context.expressionBlock(owner, value))) }
        return context.generated(owner, if (closeException == null) ConePrimitiveType.UNIT else ConePrimitiveType.NOTHING,
            CfirSourceExpressionPayload.Match(context.reference(fresh, owner), cases))
    }
}
