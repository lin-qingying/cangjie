package org.cangnova.cangjie.cfir.resolve.source

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.references.CfirReferenceParentType
import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider
import org.cangnova.cangjie.cfir.source.*
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import java.math.BigInteger

/** 编译期值与额外表面语法各自保留产品边界，值图不能倒推成未经绑定的构造调用。 */
internal class CfirSourceAdditionalCanonicalizer(private val context: CfirSourceGraphProducer.Context) {
    fun arrayInitializer(call: CfirFunctionCall, owner: CfirDeclaration): CfirDeclaration? {
        if (context.typeOf(call) is ConeVArrayType) return null
        val arguments = call.argumentList.arguments
        val role = when {
            arguments.size == 2 && context.typeOf(arguments[1]) is ConeFunctionType -> "arrayInitByFunction"
            arguments.size == 1 -> "arrayInitByCollection"
            arguments.size == 2 -> return null
            else -> error("SOURCE array constructor has no completed native argument shape")
        }
        return owner.moduleData.session.symbolProvider.getTopLevelFunctionSymbols(FqName("std.core"), Name.identifier(role)).single().cfir
    }

    fun constant(value: CfirConstantValue, owner: CfirDeclaration): CfirSourceExpressionPayload = when (value) {
        is CfirConstantValue.Primitive -> {
            val text = when (value.kind) {
                CfirLiteralKind.INT, CfirLiteralKind.BYTE -> (value.value as BigInteger).toString()
                CfirLiteralKind.FLOAT -> (value.value as Double).toString()
                CfirLiteralKind.BOOLEAN -> (value.value as Boolean).toString()
                CfirLiteralKind.STRING, CfirLiteralKind.JSTRING -> value.value as String
                CfirLiteralKind.RUNE -> String(Character.toChars(value.value as Int))
                CfirLiteralKind.UNIT -> "()"
            }
            CfirSourceExpressionPayload.Literal(value.kind, text, value.kind == CfirLiteralKind.JSTRING)
        }
        is CfirConstantValue.Tuple -> CfirSourceExpressionPayload.Tuple(value.elements.map { element ->
            context.generated(owner, element.type, constant(element, owner))
        })
        is CfirConstantValue.EnumValue -> {
            check(value.constructor.isBound)
            val declaration = value.constructor.cfir
            if (value.arguments.isEmpty()) CfirSourceExpressionPayload.Reference(declaration.name, CfirSourceReferenceTarget.Declaration(declaration),
                (value.type as ConeLookupTagBasedType).typeArguments.map { it.type }, CfirReferenceParentType.Absent)
            else context.builder.record(context.enumCall(value.type, declaration.name.asString(), value.arguments.map {
                context.generated(owner, it.type, constant(it, owner))
            }, owner)).payload
        }
        is CfirConstantValue.ObjectValue, is CfirConstantValue.ArrayValue, is CfirConstantValue.VArray ->
            error("Evaluated composite values require their completed constant-value product, not SOURCE reconstruction")
    }

    fun produce(expression: CfirExpression, owner: CfirDeclaration): CfirSourceExpressionPayload = when (expression) {
        is CfirOptionalExpression, is CfirOptionalChainExpression ->
            error("Optional SOURCE requires its completed Option lowering product")
        is CfirSynchronizedExpression -> error("Synchronized SOURCE requires its completed Lock lowering product")
        is CfirStringInterpolation -> error("Interpolation SOURCE requires its completed String lowering product")
        else -> error("No additional native SOURCE role for ${expression::class.qualifiedName}")
    }
}
