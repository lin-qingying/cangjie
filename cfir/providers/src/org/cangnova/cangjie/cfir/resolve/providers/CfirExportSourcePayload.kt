package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.annotations.*
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirFunctionBodyOwner
import org.cangnova.cangjie.cfir.CfirIterationOwner
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.patterns.*
import org.cangnova.cangjie.cfir.references.*
import org.cangnova.cangjie.cfir.references.impl.CfirResolvedAppliedCallableReference
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirThisOwnerSymbol
import org.cangnova.cangjie.cfir.source.CfirSourceForInKind
import org.cangnova.cangjie.cfir.source.CfirSourceCallKind
import org.cangnova.cangjie.cfir.source.CfirSourceOperator
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement
import java.util.Collections
import java.util.LinkedHashMap

/**
 * SOURCE 图的不可变语义槽。所有 CFIR 对象字段仅作为本代身份，消费者从 sourceElement 查询其内容。
 *
 * 与 visitor 的来源闭包分开保留声明/表达式角色、可空固定槽、操作、绑定和非拥有控制目标。
 * 这是 CFIR 的完成输入，不是 CJO tag；Range、is/as、smartcast、effect 等仍须由语义 owner
 * 生产原生 canonical 图。捕获器不执行 desugar、类型解析或 helper 生成。
 */
sealed interface CfirExportSourcePayload {
    /** Type 使用保留原节点角色；解析结果与原语法拥有边不能由去重 children 重建。 */
    sealed interface TypeReference : CfirExportSourcePayload { val origin: CfirTypeRefOrigin }
    data class ResolvedType(override val origin: CfirTypeRefOrigin, val prototype: CfirTypeRef?) : TypeReference
    data class ParenthesizedType(override val origin: CfirTypeRefOrigin, val inner: CfirTypeRef) : TypeReference
    data class UserType(override val origin: CfirTypeRefOrigin, val qualifiers: List<org.cangnova.cangjie.cfir.CfirQualifierPart>) : TypeReference
    data class BasicType(override val origin: CfirTypeRefOrigin, val name: Name) : TypeReference
    data class FunctionType(override val origin: CfirTypeRefOrigin, val parameters: List<CfirTypeRef>, val returnType: CfirTypeRef) : TypeReference
    data class TupleType(override val origin: CfirTypeRefOrigin, val elements: List<CfirTypeRef>) : TypeReference
    data class VArrayType(override val origin: CfirTypeRefOrigin, val element: CfirTypeRef, val sizeLiteral: String) : TypeReference
    data class OptionType(override val origin: CfirTypeRefOrigin, val element: CfirTypeRef) : TypeReference
    data class ImplicitType(override val origin: CfirTypeRefOrigin) : TypeReference
    data class TypeQualifier(val name: Name, val origin: CfirTypeRefOrigin, val arguments: List<CfirTypeRef>) : CfirExportSourcePayload
    data class Block(val statements: List<CfirExportSourceStatement>) : CfirExportSourcePayload
    data class Literal(val kind: CfirLiteralKind, val value: Any?) : CfirExportSourcePayload
    data class Constant(val value: CfirExportConstantGraph) : CfirExportSourcePayload
    data class ArrayLiteral(val elements: List<CfirExpression>) : CfirExportSourcePayload
    data class TupleLiteral(val elements: List<CfirExpression>) : CfirExportSourcePayload
    data class Interpolation(val parts: List<CfirExpression>) : CfirExportSourcePayload
    data class Quote(val text: String, val interpolations: List<CfirExpression>) : CfirExportSourcePayload

    data class Access(
        val reference: CfirReference,
        val dispatchReceiver: CfirExpression?,
        val explicitReceiver: CfirExpression?,
        val typeArguments: List<CfirTypeRef>,
    ) : CfirExportSourcePayload

    data class Call(
        val access: Access,
        val argumentList: CfirArgumentList,
        val origin: CfirFunctionCallOrigin,
        val hasTrailingLambda: Boolean,
        val varraySizeLiteral: String?,
        val hasSideEffect: Boolean,
    ) : CfirExportSourcePayload

    data class Arguments(val arguments: List<CfirExpression>) : CfirExportSourcePayload
    /** 完成源图的表达式角色沿用原节点；不重造 raw call/operator。 */
    data class SourceCall(val base: CfirExpression, val arguments: List<CfirFunctionArgument>, val kind: CfirSourceCallKind, val hasSideEffect: Boolean) : CfirExportSourcePayload
    data class SourceUnary(val operand: CfirExpression, val operation: CfirSourceOperator) : CfirExportSourcePayload
    data class SourceBinary(val left: CfirExpression, val right: CfirExpression, val operation: CfirSourceOperator) : CfirExportSourcePayload
    data class SourceArgument(val value: CfirExpression?, val isDefault: Boolean, val withInout: Boolean) : CfirExportSourcePayload
    /** value/context 两组映射来自同一已解析列表，不按名字或结果类型重建绑定。 */
    data class ResolvedArguments(
        val original: CfirArgumentList?,
        val arguments: List<CfirExpression>,
        val valueBindings: List<CfirExportArgumentBinding>,
        val allBindings: List<CfirExportArgumentBinding>,
        val cVariadicArguments: CfirCVariadicArguments<CfirExpression>? = null,
        val usesCangjieVariadicCall: Boolean = false,
    ) : CfirExportSourcePayload
    data class NamedArgument(val expression: CfirExpression, val name: Name, val nameSource: CjSourceElement?) : CfirExportSourcePayload
    data class InoutArgument(val expression: CfirExpression) : CfirExportSourcePayload
    data class Optional(val expression: CfirExpression) : CfirExportSourcePayload
    data class OptionalChain(val expression: CfirExpression) : CfirExportSourcePayload

    data class Assignment(
        val left: CfirExpression,
        val right: CfirExpression,
        val operation: Name?,
        val typeMismatch: CfirAssignmentTypeMismatchOutcome?,
    ) : CfirExportSourcePayload

    data class AugmentedAssignment(
        val left: CfirExpression,
        val right: CfirExpression,
        val operation: Name,
        val operationSource: CjSourceElement?,
    ) : CfirExportSourcePayload

    data class Binary(val left: CfirExpression, val right: CfirExpression, val kind: CfirBinaryOpKind) : CfirExportSourcePayload
    data class Comparison(val left: CfirExpression, val right: CfirExpression, val operation: CfirComparisonOp) : CfirExportSourcePayload
    data class Increment(
        val expression: CfirExpression,
        val operation: Name,
        val isPrefix: Boolean,
        val operationSource: CjSourceElement?,
    ) : CfirExportSourcePayload

    data class Subscript(
        val receiver: CfirExpression,
        val indices: List<CfirExpression>,
        val getCall: CfirFunctionCall?,
        val setCall: CfirFunctionCall?,
    ) : CfirExportSourcePayload

    data class TypeConversion(val argument: CfirExpression, val typeRef: CfirTypeRef) : CfirExportSourcePayload
    data class TypeOperator(val argument: CfirExpression, val typeRef: CfirTypeRef, val operation: CfirTypeOperationKind) : CfirExportSourcePayload
    data class Range(val start: CfirExpression, val end: CfirExpression, val step: CfirExpression?, val isInclusive: Boolean) : CfirExportSourcePayload

    data class Conditional(val condition: CfirExpression, val thenBranch: CfirBlock, val elseBranch: CfirExpression?) : CfirExportSourcePayload
    data class Availability(
        val name: Name,
        val argumentSource: CjSourceElement?,
        val nameSource: CjSourceElement?,
        val condition: CfirExpression?,
        val thenBranch: CfirExpression,
        val elseBranch: CfirExpression,
    ) : CfirExportSourcePayload

    data class Loop(val condition: CfirExpression, val body: CfirBlock, val isDoWhile: Boolean) : CfirExportSourcePayload
    data class ForIn(
        val pattern: CfirPattern,
        val iterable: CfirExpression,
        val guard: CfirExpression?,
        val body: CfirBlock,
        val iterationKind: CfirSourceForInKind,
    ) : CfirExportSourcePayload

    /** target 是非拥有边；不沿它重新遍历或复制函数/循环。未绑定跳转保留真实缺失状态。 */
    data class Return(val result: CfirExpression, val function: CfirFunctionBodyOwner?, val label: String?) : CfirExportSourcePayload
    data class Jump(val isBreak: Boolean, val loop: CfirIterationOwner?, val label: String?) : CfirExportSourcePayload
    data class Lambda(val function: CfirAnonymousFunction, val isTrailing: Boolean) : CfirExportSourcePayload
    data class Match(val subject: CfirExpression?, val branches: List<CfirMatchBranch>, val exhaustiveness: CfirMatchExhaustivenessStatus) : CfirExportSourcePayload
    data class MatchBranch(val pattern: CfirPattern, val guard: CfirExpression?, val body: CfirBlock) : CfirExportSourcePayload
    data class LetPattern(val initializer: CfirExpression, val pattern: CfirPattern) : CfirExportSourcePayload

    data class Try(
        val resources: List<CfirFieldVariable>,
        val body: CfirBlock,
        val catches: List<CfirCatch>,
        val handlers: List<CfirHandleClause>,
        val finallyBlock: CfirBlock?,
    ) : CfirExportSourcePayload

    data class Catch(val pattern: CfirPattern, val body: CfirBlock) : CfirExportSourcePayload
    data class Handle(val pattern: CfirCommandTypePattern, val body: CfirBlock) : CfirExportSourcePayload
    data class Throw(val exception: CfirExpression) : CfirExportSourcePayload
    data class Perform(val expression: CfirExpression) : CfirExportSourcePayload
    /** Resume 的 CFIR 事实不授予其 v113 SOURCE writer tag；原生仅有 loader 分派。 */
    data class Resume(val value: CfirExpression?, val throwing: CfirExpression?) : CfirExportSourcePayload
    data class Spawn(val body: CfirBlock, val contextArgument: CfirExpression?) : CfirExportSourcePayload
    data class Synchronized(val monitor: CfirExpression, val body: CfirBlock) : CfirExportSourcePayload
    data class Unsafe(val body: CfirExpression) : CfirExportSourcePayload
    data class SmartCast(
        val original: CfirExpression,
        val typeRef: CfirTypeRef,
        val upperTypes: List<ConeCangJieType>,
        val lowerTypes: List<ConeCangJieType>,
        val stability: CfirSmartcastStability,
    ) : CfirExportSourcePayload

    data class ThisReceiver(val reference: CfirThisReference, val inaccessible: InaccessibleReceiverKind?) : CfirExportSourcePayload
    data class Error(val diagnostic: ConeDiagnostic, val expression: CfirExpression?, val otherElement: CfirElement?) : CfirExportSourcePayload

    data class NamedReference(val name: Name) : CfirExportSourcePayload
    data class BuiltinReference(val name: Name, val signature: BuiltinPrimitiveOperatorSignature) : CfirExportSourcePayload
    data class ThisReference(val symbol: CfirThisOwnerSymbol<*>?, val isBound: Boolean, val declaration: CfirDeclaration?, val isImplicit: Boolean, val diagnostic: ConeDiagnostic?) : CfirExportSourcePayload
    data class AppliedReference(
        val name: Name,
        val symbol: CfirBasedSymbol<*>,
        val isBound: Boolean,
        val target: CfirDeclaration?,
        val isFunctionValue: Boolean,
        val returnType: ConeCangJieType?,
        val parameterTypes: List<ConeCangJieType>,
        val callSiteType: ConeCangJieType?,
        val matchedParentType: CfirReferenceParentType,
    ) : CfirExportSourcePayload
    data class SuperReference(val typeRef: CfirTypeRef) : CfirExportSourcePayload
    data class ErrorReference(val name: Name, val diagnostic: ConeDiagnostic) : CfirExportSourcePayload

    sealed interface Pattern : CfirExportSourcePayload
    data object WildcardPattern : Pattern
    data class BindingPattern(
        val name: Name,
        val typeRef: CfirTypeRef?,
        val variable: CfirPatternBindingVariable?,
        val nested: CfirPattern?,
    ) : Pattern
    data class TypePattern(
        val kind: CfirTypePatternMatchingKind,
        val typeRef: CfirTypeRef,
        val nested: CfirPattern,
        val matchBeforeRuntime: Boolean,
        val needRuntimeTypeCheck: Boolean,
        val semanticType: CfirTypeRef?,
    ) : Pattern
    data class VarOrEnumPattern(val name: Name, val variable: CfirPatternBindingVariable?) : Pattern
    data class EnumPattern(val constructor: CfirExpression, val arguments: List<CfirPattern>, val semanticType: CfirTypeRef?) : Pattern
    data class ConstantPattern(val expression: CfirExpression, val operatorCall: CfirExpression?, val semanticType: CfirTypeRef?) : Pattern
    data class ExpressionPattern(val expression: CfirExpression) : Pattern
    data class TuplePattern(val elements: List<CfirPattern>) : Pattern
    data class OrPattern(val alternatives: List<CfirPattern>) : Pattern
    data class CatchPattern(
        val nested: CfirPattern,
        val types: List<CfirTypeRef>,
        val semanticType: CfirTypeRef?,
    ) : Pattern
    data class CommandPattern(val nested: CfirPattern, val types: List<CfirTypeRef>, val semanticType: CfirTypeRef?) : Pattern

    /** 注解是独立语义角色；其参数表达式仍进入同一来源图。 */
    data class Annotation(
        val typeRef: CfirTypeRef,
        val arguments: List<CfirExpression>,
        val argumentMapping: Map<Name, CfirExpression>,
        val classId: ClassId?,
        val target: CangjieAnnotationTarget?,
        val forcedCustom: Boolean,
        val sourceName: String?,
        val moduleName: String,
        val kind: BuiltInAnnotationKind?,
        val identity: CangjieAnnotationIdentity?,
        val origin: CangjieAnnotationOrigin?,
        val isCompileTimeVisible: Boolean?,
        val callee: CfirReference?,
        val argumentList: CfirArgumentList?,
        val argumentView: CfirExportAnnotationArgumentView?,
        val resolveState: CfirAnnotationResolveState?,
        val owner: CfirDeclaration?,
        val ownerSymbol: CfirBasedSymbol<*>?,
        val ownerIsBound: Boolean,
    ) : CfirExportSourcePayload
}

/** 按节点的实际角色捕获专属字段；来源遍历和校验调用同一个无副作用函数。 */
fun CfirElement.captureExportSourcePayload(): CfirExportSourcePayload? = when (this) {
    is CfirResolvedTypeRef -> CfirExportSourcePayload.ResolvedType(typeUseOrigin, delegatedTypeRef)
    is CfirParenthesizedTypeRef -> CfirExportSourcePayload.ParenthesizedType(typeUseOrigin, innerTypeRef)
    is CfirUserTypeRef -> CfirExportSourcePayload.UserType(typeUseOrigin, frozenExportList(qualifier))
    is CfirBasicTypeRef -> CfirExportSourcePayload.BasicType(typeUseOrigin, name)
    is CfirFunctionTypeRef -> CfirExportSourcePayload.FunctionType(typeUseOrigin, frozenExportList(parameterTypeRefs), returnTypeRef)
    is CfirTupleTypeRef -> CfirExportSourcePayload.TupleType(typeUseOrigin, frozenExportList(elementTypeRefs))
    is CfirVArrayTypeRef -> CfirExportSourcePayload.VArrayType(typeUseOrigin, elementTypeRef, sizeLiteral)
    is CfirOptionTypeRef -> CfirExportSourcePayload.OptionType(typeUseOrigin, componentTypeRef)
    is CfirImplicitTypeRef -> CfirExportSourcePayload.ImplicitType(typeUseOrigin)
    is org.cangnova.cangjie.cfir.CfirQualifierPart -> CfirExportSourcePayload.TypeQualifier(name, typeUseOrigin, frozenExportList(typeArguments))
    is CfirAnnotation -> CfirExportSourcePayload.Annotation(
        typeRef, frozenExportList(arguments), Collections.unmodifiableMap(LinkedHashMap(argumentMapping.mapping)),
        annotationClassId, annotationTarget, forcedCustom, annotationSourceName, sourceModuleName,
        annotationKind, annotationIdentity, annotationOrigin, isCompileTimeVisible,
        (this as? CfirAnnotationCall)?.calleeReference, (this as? CfirAnnotationCall)?.argumentList,
        (this as? CfirAnnotationCall)?.argumentView?.let(CfirExportAnnotationArgumentView::capture),
        (this as? CfirAnnotationCall)?.annotationResolveState,
        (this as? CfirAnnotationCall)?.containingDeclarationSymbol?.takeIf { it.isBound }?.cfir,
        (this as? CfirAnnotationCall)?.containingDeclarationSymbol,
        (this as? CfirAnnotationCall)?.containingDeclarationSymbol?.isBound == true,
    )
    is CfirBlock -> CfirExportSourcePayload.Block(frozenExportList(statements.map { statement ->
        when (statement) {
            is CfirDeclaration -> CfirExportSourceStatement.Declaration(statement)
            is CfirExpression -> CfirExportSourceStatement.Expression(statement)
            else -> error("Source block statement has no declaration/expression role: ${statement::class.qualifiedName}")
        }
    }))
    is CfirLiteralExpression -> CfirExportSourcePayload.Literal(kind, value)
    is CfirCallExpression -> CfirExportSourcePayload.SourceCall(base, frozenExportList(arguments), callKind, hasSideEffect)
    is CfirUnaryExpression -> CfirExportSourcePayload.SourceUnary(operand, operation)
    is CfirBinaryExpression -> CfirExportSourcePayload.SourceBinary(left, right, operation)
    is CfirFunctionArgument -> CfirExportSourcePayload.SourceArgument(value, isDefaultValue, withInout)
    is CfirConstantValueExpression -> CfirExportSourcePayload.Constant(CfirExportConstantGraph.capture(constantValue))
    is CfirArrayLiteral -> CfirExportSourcePayload.ArrayLiteral(frozenExportList(elements))
    is CfirTupleLiteral -> CfirExportSourcePayload.TupleLiteral(frozenExportList(elements))
    is CfirStringInterpolation -> CfirExportSourcePayload.Interpolation(frozenExportList(parts))
    is CfirQuoteExpression -> CfirExportSourcePayload.Quote(rawText, frozenExportList(interpolations))
    is CfirFunctionCall -> CfirExportSourcePayload.Call(
        captureExportAccess(), argumentList, origin, hasTrailingLambda, varraySizeLiteral, sourceCallHasSideEffect,
    )
    is CfirQualifiedAccessExpression -> captureExportAccess()
    is CfirResolvedArgumentList -> CfirExportSourcePayload.ResolvedArguments(
        originalArgumentList, frozenExportList(arguments),
        frozenExportList(mapping.entries.map { (argument, parameter) -> CfirExportArgumentBinding(argument, parameter) }),
        frozenExportList(mappingIncludingContextArguments.entries.map { (argument, parameter) -> CfirExportArgumentBinding(argument, parameter) }),
        cVariadicArguments,
        usesCangjieVariadicCall,
    )
    is CfirArgumentList -> CfirExportSourcePayload.Arguments(frozenExportList(arguments))
    is CfirNamedArgumentExpression -> CfirExportSourcePayload.NamedArgument(expression, argumentName, nameSource)
    is CfirInoutArgumentExpression -> CfirExportSourcePayload.InoutArgument(expression)
    is CfirOptionalChainExpression -> CfirExportSourcePayload.OptionalChain(expression)
    is CfirOptionalExpression -> CfirExportSourcePayload.Optional(expression)
    is CfirAssignment -> CfirExportSourcePayload.Assignment(lValue, rValue, augmentedOperation, typeMismatchOutcome)
    is CfirAugmentedAssignment -> CfirExportSourcePayload.AugmentedAssignment(leftArgument, rightArgument, operation, operationSource)
    is CfirBinaryOp -> CfirExportSourcePayload.Binary(left, right, kind)
    is CfirComparisonExpression -> CfirExportSourcePayload.Comparison(left, right, operation)
    is CfirIncrementDecrementExpression -> CfirExportSourcePayload.Increment(expression, operationName, isPrefix, operationSource)
    is CfirSubscriptExpression -> CfirExportSourcePayload.Subscript(receiver, frozenExportList(indices), resolvedGetCall, resolvedSetCall)
    is CfirTypeConversion -> CfirExportSourcePayload.TypeConversion(argument, targetTypeRef)
    is CfirTypeOperator -> CfirExportSourcePayload.TypeOperator(argument, typeRef, operation)
    is CfirRangeExpression -> CfirExportSourcePayload.Range(start, end, step, isInclusive)
    is CfirIfExpression -> CfirExportSourcePayload.Conditional(condition, thenBranch, elseBranch)
    is CfirIfAvailableExpression -> CfirExportSourcePayload.Availability(
        conditionName, conditionArgumentSource, conditionNameSource, condition, thenBranch, elseBranch,
    )
    is CfirForInExpression -> CfirExportSourcePayload.ForIn(pattern, iterable, patternGuard, body, iterationKind)
    is CfirLoopExpression -> CfirExportSourcePayload.Loop(condition, body, isDoWhile)
    is CfirReturnExpression -> CfirExportSourcePayload.Return(result, target.takeIf { it.isBound }?.labeledElement, target.labelName)
    is CfirBreakExpression -> CfirExportSourcePayload.Jump(true, target.takeIf { it.isBound }?.labeledElement, target.labelName)
    is CfirContinueExpression -> CfirExportSourcePayload.Jump(false, target.takeIf { it.isBound }?.labeledElement, target.labelName)
    is CfirAnonymousFunctionExpression -> CfirExportSourcePayload.Lambda(anonymousFunction, isTrailingLambda)
    is CfirMatchExpression -> CfirExportSourcePayload.Match(subject, frozenExportList(branches), frozenExportExhaustiveness(exhaustiveness))
    is CfirMatchBranch -> CfirExportSourcePayload.MatchBranch(pattern, guard, body)
    is CfirLetPatternExpression -> CfirExportSourcePayload.LetPattern(initializer, pattern)
    is CfirTryExpression -> CfirExportSourcePayload.Try(
        frozenExportList(resources), tryBlock, frozenExportList(catches), frozenExportList(handlers), finallyBlock,
    )
    is CfirCatch -> CfirExportSourcePayload.Catch(pattern, body)
    is CfirHandleClause -> CfirExportSourcePayload.Handle(commandPattern, body)
    is CfirThrowExpression -> CfirExportSourcePayload.Throw(exception)
    is CfirPerformExpression -> CfirExportSourcePayload.Perform(expression)
    is CfirResumeExpression -> CfirExportSourcePayload.Resume(withExpression, throwingExpression)
    is CfirSpawnExpression -> CfirExportSourcePayload.Spawn(body, threadContextArgument)
    is CfirSynchronizedExpression -> CfirExportSourcePayload.Synchronized(monitor, body)
    is CfirUnsafeExpression -> CfirExportSourcePayload.Unsafe(body)
    is CfirSmartCastExpression -> CfirExportSourcePayload.SmartCast(
        originalExpression, smartcastType, frozenExportList(upperTypesFromSmartCast),
        frozenExportList(lowerTypesFromSmartCast), smartcastStability,
    )
    is CfirThisReceiverExpression -> CfirExportSourcePayload.ThisReceiver(calleeReference, (this as? CfirInaccessibleReceiverExpression)?.kind)
    is CfirErrorExpression -> CfirExportSourcePayload.Error(diagnostic, expression, nonExpressionElement)
    is CfirResolvedBuiltinOperatorReference -> CfirExportSourcePayload.BuiltinReference(name, signature)
    is CfirThisReference -> CfirExportSourcePayload.ThisReference(
        boundSymbol, boundSymbol?.isBound == true, boundSymbol?.takeIf { it.isBound }?.cfir, isImplicit, diagnostic,
    )
    is CfirSuperReference -> CfirExportSourcePayload.SuperReference(superTypeRef)
    is CfirErrorNamedReference -> CfirExportSourcePayload.ErrorReference(name, diagnostic)
    is CfirResolvedAppliedCallableReference -> CfirExportSourcePayload.AppliedReference(
        name, resolvedSymbol, resolvedSymbol.isBound, resolvedSymbol.takeIf { it.isBound }?.cfir,
        isFunctionValue, substitutedReturnType, frozenExportList(substitutedParameterTypes), callSiteType, matchedParentType,
    )
    is CfirNamedReference -> CfirExportSourcePayload.NamedReference(name)
    is CfirWildcardPattern -> CfirExportSourcePayload.WildcardPattern
    is CfirBindingPattern -> CfirExportSourcePayload.BindingPattern(name, typeRef, bindingVariable, nestedPattern)
    is CfirTypePattern -> CfirExportSourcePayload.TypePattern(matchingKind, typeRef, nestedPattern, matchBeforeRuntime, needRuntimeTypeCheck, semanticTypeRef)
    is CfirVarOrEnumPattern -> CfirExportSourcePayload.VarOrEnumPattern(name, bindingVariable)
    is CfirEnumPattern -> CfirExportSourcePayload.EnumPattern(constructor, frozenExportList(arguments), semanticTypeRef)
    is CfirConstPattern -> CfirExportSourcePayload.ConstantPattern(expression, operatorCall, semanticTypeRef)
    is CfirExpressionPattern -> CfirExportSourcePayload.ExpressionPattern(expression)
    is CfirTuplePattern -> CfirExportSourcePayload.TuplePattern(frozenExportList(elements))
    is CfirOrPattern -> CfirExportSourcePayload.OrPattern(frozenExportList(alternatives))
    is CfirCatchPattern -> CfirExportSourcePayload.CatchPattern(nestedPattern, frozenExportList(typeRefs), semanticTypeRef)
    is CfirCommandTypePattern -> CfirExportSourcePayload.CommandPattern(nestedPattern, frozenExportList(typeRefs), semanticTypeRef)
    else -> {
        check(this !is CfirExpression && this !is CfirPattern) {
            "Source node has no frozen semantic payload: ${this::class.qualifiedName}"
        }
        null
    }
}

private fun CfirQualifiedAccessExpression.captureExportAccess(): CfirExportSourcePayload.Access =
    CfirExportSourcePayload.Access(calleeReference, dispatchReceiver, explicitReceiver, frozenExportList(typeArguments))

private fun <T> frozenExportList(elements: Collection<T>): List<T> = Collections.unmodifiableList(elements.toList())

/** Block 的声明和表达式槽分别保留身份，格式层再将它们映射到各自的索引池。 */
sealed interface CfirExportSourceStatement {
    data class Declaration(val declaration: CfirDeclaration) : CfirExportSourceStatement
    data class Expression(val expression: CfirExpression) : CfirExportSourceStatement
}

data class CfirExportArgumentBinding(val argument: CfirExpression, val parameter: CfirValueParameter)

/** annotation 视图标量、借用引用和坐标值独立冻结，不复制 default 表达式的拥有域。 */
data class CfirExportAnnotationArgumentView(val entries: List<CfirExportAnnotationArgumentEntry>) {
    companion object {
        internal fun capture(view: CfirAnnotationArgumentView): CfirExportAnnotationArgumentView = CfirExportAnnotationArgumentView(
            frozenExportList(view.entries.map { entry ->
                CfirExportAnnotationArgumentEntry(
                    entry.sourceOrder, entry.explicitName, entry.argument, entry.resolvedParameter, entry.status,
                    entry.isDefaultOrigin, entry.constantExpression, CfirExportSourceSpan.capture(entry.source, false),
                )
            }),
        )
    }
}

data class CfirExportAnnotationArgumentEntry(
    val sourceOrder: Int,
    val explicitName: Name?,
    val argument: CfirExpression?,
    val parameter: CfirValueParameter?,
    val status: CfirAnnotationArgumentStatus,
    val isDefaultOrigin: Boolean,
    val constantExpression: CfirExpression?,
    val span: CfirExportSourceSpan?,
)

private fun frozenExportExhaustiveness(status: CfirMatchExhaustivenessStatus): CfirMatchExhaustivenessStatus = when (status) {
    is CfirMatchExhaustivenessStatus.NonExhaustive -> status.copy(missingCaseTexts = frozenExportList(status.missingCaseTexts))
    else -> status
}
