package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.lambdaParameterShapeExpectedFunctionType
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.diagnostic.ConeDiagnosticWithSingleCandidate
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirWrappedExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.semantics.AbstractCallCandidate
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid

/**
 * Lambda 头部诊断使用的目标函数类型。
 *
 * resolve 阶段在错误候选上可能不会把参数类型写回 lambda header；checker 阶段需要
 * 从最终 CFIR 树和当前遍历上下文恢复官方 `ChkLamParamTys` 使用的函数形状。
 */
internal fun CfirAnonymousFunction.lambdaExpectedFunctionType(context: CheckerContext): ConeFunctionType? =
    lambdaParameterShapeExpectedFunctionType
        ?: matchingParameterFunctionType.functionTypeForLambdaShape(context)
        ?: expectedFunctionTypeFromContainingVariable(context)
        ?: expectedFunctionTypeFromContainingCall(context)

/** 判断表达式是否承载指定匿名函数。 */
internal fun CfirExpression.isExpressionForAnonymousFunction(
    anonymousFunction: CfirAnonymousFunction,
): Boolean {
    var found = false
    accept(object : CfirVisitorVoid() {
        override fun visitElement(element: CfirElement) {
            if (found) return
            when (element) {
                is CfirAnonymousFunctionExpression -> {
                    if (element.anonymousFunction.isSameLambdaAs(anonymousFunction)) {
                        found = true
                    }
                    return
                }
                is CfirWrappedExpression -> Unit
                else -> Unit
            }
            element.acceptChildren(this, null)
        }
    }, null)
    return found
}

/** 判断两个匿名函数是否同一实例：同一节点或同一 symbol 视为相同。 */
private fun CfirAnonymousFunction.isSameLambdaAs(other: CfirAnonymousFunction): Boolean =
    this === other || symbol == other.symbol

/** 将类型规整为 lambda 目标函数类型。 */
internal fun ConeCangJieType?.functionTypeForLambdaShape(context: CheckerContext): ConeFunctionType? {
    if (this == null) return null
    if (this is ConeFunctionType) return this
    if (this is ConeErrorType) {
        val delegated = delegatedType
        return delegated as? ConeFunctionType
            ?: delegated?.fullyExpandedType(context.session) as? ConeFunctionType
    }
    return fullyExpandedType(context.session) as? ConeFunctionType
}

/**
 * 从包含该 lambda 的变量初始化器恢复目标函数类型。
 *
 * 遍历上下文中最近的、初始化器包含当前 lambda 的变量声明，
 * 用其返回类型规整出函数形状；不匹配时返回 `null` 继续下一来源。
 */
private fun CfirAnonymousFunction.expectedFunctionTypeFromContainingVariable(
    context: CheckerContext,
): ConeFunctionType? {
    val variable = context.containingDeclarations
        .asReversed()
        .filterIsInstance<CfirVariableSymbol<*>>()
        .map { it.cfir }
        .firstOrNull { variable ->
            variable.initializer?.isExpressionForAnonymousFunction(this) == true
        }
        ?: return null
    return variable.returnTypeRef.coneTypeOrNull.functionTypeForLambdaShape(context)
}

/**
 * 从包含该 lambda 的调用实参恢复目标函数类型。
 *
 * 优先使用诊断携带的唯一候选的实参映射类型；不可用时回退到
 * 被调用声明对应位置的形参类型。
 */
private fun CfirAnonymousFunction.expectedFunctionTypeFromContainingCall(
    context: CheckerContext,
): ConeFunctionType? {
    val call = context.callsOrAssignments
        .asReversed()
        .filterIsInstance<CfirFunctionCall>()
        .firstOrNull { call ->
            call.argumentList.arguments.any { argument ->
                argument.isExpressionForAnonymousFunction(this)
            }
        }
        ?: return null

    call.singleDiagnosticCandidateOrNull()
        ?.expectedTypeForLambdaArgument(this)
        ?.functionTypeForLambdaShape(context)
        ?.let { return it }

    val argumentIndex = call.argumentList.arguments.indexOfFirst { argument ->
        argument.isExpressionForAnonymousFunction(this)
    }
    if (argumentIndex < 0) return null
    return call.valueParameterTypeForArgument(argumentIndex)
        .functionTypeForLambdaShape(context)
}

/** 从候选的实参映射中读取承载该 lambda 的实参对应的形参类型。 */
private fun AbstractCallCandidate<*>.expectedTypeForLambdaArgument(
    anonymousFunction: CfirAnonymousFunction,
): ConeCangJieType? {
    if (!argumentMappingInitialized) return null
    val parameter = argumentMapping.entries
        .firstOrNull { (atom, _) ->
            atom.expression.isExpressionForAnonymousFunction(anonymousFunction)
        }
        ?.value
        ?: return null
    return parameter.returnTypeRef.coneTypeOrNull
}

/** 若调用引用的诊断携带唯一候选则返回之；多候选或无候选返回 `null`。 */
private fun CfirFunctionCall.singleDiagnosticCandidateOrNull(): AbstractCallCandidate<*>? {
    val diagnostic = (calleeReference as? CfirDiagnosticHolder)?.diagnostic
    return (diagnostic as? ConeDiagnosticWithSingleCandidate)?.candidate
}

/**
 * 读取被调用声明第 [index] 个形参的已解析类型。
 *
 * 函数/构造器取形参类型；变量按函数类型取第 [index] 个参数类型；
 * 引用无候选或声明形态不支持时返回 `null`。
 */
private fun CfirFunctionCall.valueParameterTypeForArgument(index: Int): ConeCangJieType? {
    val declaration = when (val reference = calleeReference) {
        is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol.cfir
        is CfirDiagnosticHolder ->
            (reference.diagnostic as? ConeDiagnosticWithSingleCandidate)?.candidateSymbol?.cfir
        else -> null
    }

    return when (declaration) {
        is CfirFunction -> declaration.valueParameters.getOrNull(index)?.resolvedParameterType()
        is CfirConstructor -> declaration.valueParameters.getOrNull(index)?.resolvedParameterType()
        is CfirEnumConstructor -> declaration.valueParameters.getOrNull(index)?.resolvedParameterType()
        is CfirVariable -> {
            val functionType = declaration.returnTypeRef.coneTypeOrNull as? ConeFunctionType
                ?: return null
            functionType.parameterTypes.getOrNull(index)
        }
        else -> null
    }
}

/** 读取形参的已解析类型；typeRef 未解析时返回 `null`。 */
private fun CfirValueParameter.resolvedParameterType(): ConeCangJieType? =
    (returnTypeRef as? CfirResolvedTypeRef)?.coneType
