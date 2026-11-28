package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.CfirConstantValue
import org.cangnova.cangjie.cfir.references.CfirReferenceParentType
import org.cangnova.cangjie.cfir.source.*
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.CfirTypeRef

/** 准备和捕获使用同一份完成图的词法关系，null owner 是实际顶层事实，不启动 provider 补索引。 */
internal fun CfirSourceGraph.forEachExportLexicalOwner(action: (CfirDeclaration, CfirDeclaration?) -> Unit) {
    declarationOwners.forEach { (declaration, parent) ->
        val lexical = when (declaration) {
            is CfirValueParameter -> declaration.containingDeclarationSymbol.cfir
            is CfirTypeParameter -> declaration.containingDeclarationSymbol.cfir
            is CfirPropertyAccessor -> declarationOwners[parent]
            is CfirPatternBindingVariable -> if (parent is CfirPatternVariable) declarationOwners.getValue(parent) else parent
            else -> functionContexts[declaration]?.declarationContext ?: parent
        }
        action(declaration, lexical)
    }
    functionContexts.forEach { (function, context) -> action(function, context.declarationContext) }
    functionHeaders.forEach { (function, header) -> header.parameterLists.flatten().forEach { action(it, function) } }
    generatedDeclarations.forEach { (declaration, context) -> action(declaration, context.owner) }
}

/**
 * SOURCE 准备和冻结共同使用的依赖边协议。声明引用只提供语义身份，
 * 不授予 borrowed target 的正文拥有权；类型始终保留表达式/声明自己的 session 域。
 */
internal class CfirExportSourceDependencies(
    private val declaration: (CfirDeclaration) -> Unit,
    private val type: (ConeCangJieType?, CfirDeclaration) -> Unit,
    private val typeReference: (CfirTypeRef, CfirDeclaration) -> Unit,
    private val constant: (CfirConstantValue, CfirDeclaration) -> Unit,
    private val patternInput: (CfirSourcePattern, CfirDeclaration) -> Unit,
) {
    fun visit(graph: CfirSourceGraph) {
        graph.inheritedTypeReferences.forEach { (owner, refs) ->
            declaration(owner)
            refs.forEach { typeReference(it, owner) }
        }
        graph.generatedDeclarations.forEach { (generated, context) ->
            declaration(generated)
            declaration(context.owner)
        }
        graph.expressions.forEach { (identity, record) ->
            declaration(record.context)
            type(record.type, record.context)
            record.payload.expressionChildren().forEach { graph.expression(it) }
            record.mapExpression?.let(graph::expression)
            record.payload.patterns().forEach { pattern(it, record.context) }
            when (val payload = record.payload) {
                is CfirSourceExpressionPayload.Reference -> reference(payload, record.context)
                is CfirSourceExpressionPayload.Member -> reference(payload.reference, record.context)
                is CfirSourceExpressionPayload.Block -> payload.statements.filterIsInstance<CfirSourceSlot.Declaration>()
                    .forEach { declaration(it.declaration) }
                is CfirSourceExpressionPayload.Return -> declaration(payload.function)
                is CfirSourceExpressionPayload.Lambda -> {
                    declaration(payload.function.owner)
                    payload.function.parameters.forEach(declaration)
                    val signature = payload.function.signature
                    check(signature is CfirSourceCallableSignature.Resolved) { "A lambda must have its completed semantic signature" }
                    type(signature.returnType, payload.function.owner)
                }
                is CfirSourceExpressionPayload.Array -> payload.initializer?.let(declaration)
                is CfirSourceExpressionPayload.ArrayLiteral -> payload.initializer?.let(declaration)
                is CfirSourceExpressionPayload.Spawn -> declaration(payload.future)
                is CfirSourceExpressionPayload.Try -> payload.resources.forEach(declaration)
                else -> Unit
            }
            check(graph.expression(identity) === record)
        }
        graph.functionBodies.keys.forEach(declaration)
        graph.functionHeaders.forEach { (function, header) ->
            declaration(function)
            header.parameterLists.flatten().forEach(declaration)
            header.desugaredParameterLists.flatten().filterNotNull().forEach(declaration)
            type(header.returnType, function)
        }
        graph.constantValues.forEach { (variable, value) ->
            declaration(variable)
            constant(value, variable)
        }
        graph.declarationOwners.forEach { (child, owner) ->
            declaration(child)
            owner?.let(declaration)
        }
        graph.functionContexts.forEach { (function, context) ->
            declaration(function)
            context.declarationContext?.let(declaration)
            context.outerFunction?.let(declaration)
        }
        graph.declarationPatterns.forEach { (variable, value) ->
            declaration(variable)
            pattern(value, variable)
        }
        graph.initializers.keys.forEach(declaration)
        graph.defaults.keys.forEach(declaration)
        graph.annotationPlans.forEach { (owner, plans) ->
            declaration(owner)
            plans.forEach { plan ->
                plan.target?.let(declaration)
                plan.arguments.forEach { graph.expression(graph.expressionOf(it.expression)) }
            }
        }
        graph.annotations.forEach { (owner, annotations) ->
            declaration(owner)
            annotations.forEach { annotation ->
                annotation.target?.let(declaration)
                annotation.arguments.forEach { graph.expression(it.expression) }
            }
        }
    }

    private fun pattern(pattern: CfirSourcePattern, context: CfirDeclaration) {
        patternInput(pattern, context)
        pattern.types.forEach { type(it, context) }
        pattern.typeReferences.forEach { typeReference(it, context) }
        pattern.slots.filterIsInstance<CfirSourceSlot.Declaration>().forEach { declaration(it.declaration) }
        pattern.patterns.forEach { pattern(it, context) }
    }

    private fun reference(reference: CfirSourceExpressionPayload.Reference, context: CfirDeclaration) {
        (reference.target as? CfirSourceReferenceTarget.Declaration)?.let { declaration(it.declaration) }
        reference.instantiatedTypes.forEach { type(it, context) }
        (reference.matchedParentType as? CfirReferenceParentType.Instantiated)?.let { type(it.type, context) }
    }
}
