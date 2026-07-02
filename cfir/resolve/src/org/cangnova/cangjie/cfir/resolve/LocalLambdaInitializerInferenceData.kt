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
 * distributed under the License is distributed on an "AS IS", BASIS,
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

package org.cangnova.cangjie.cfir.resolve

import org.cangnova.cangjie.cfir.CfirDeclarationDataKey
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationDataRegistry
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.declarations.lambdaParameterShapeExpectedFunctionType
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeAttributes
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.IdealTypeResolver
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage

/**
 * 局部变量 initializer 中无上下文 lambda 的推断状态。
 *
 * 这类 lambda 先通过 synthetic accept 建立 placeholder 函数类型，随后真实调用
 * `f(arg)` 才能把实参约束导回 placeholder。状态保存在声明属性上，使函数类型
 * invoke 候选可以导入同一约束系统，并在 completion 后把最终类型写回声明树。
 */
internal class CfirLocalLambdaInitializerInferenceData(
    /** 当前 lambda placeholder 所属的约束系统快照。 */
    var constraintStorage: ConstraintStorage,
    /** 原始匿名函数表达式，用于 completion 后同步写回参数、返回和整体函数类型。 */
    val lambdaExpression: CfirAnonymousFunctionExpression,
    /** initializer PCLA 解析期间产生、需在真实函数值调用完成后继续写回的 postponed 调用。 */
    val postponedPCLACalls: List<ConeResolutionAtom>,
    /** initializer 首轮 body resolve 前的树状态，用于最终类型确定后重算 lambda body。 */
    private val preBodyResolveSnapshot: CfirResolutionSnapshot,
) {
    /** 当前 initializer body 是否已经在函数值调用 completion 中按最终参数类型重算过。 */
    var bodyReanalyzedAfterCallableValueCompletion: Boolean = false

    /**
     * 将函数类型 invoke 完成后的替换结果写回 lambda initializer 和变量声明。
     */
    fun applyCompletionResult(
        variable: CfirVariable?,
        substitutor: ConeSubstitutor,
        completedStorage: ConstraintStorage,
        restoreBodyResolveState: Boolean = false,
    ): Boolean {
        val completionResult = buildCompletionResult(variable, substitutor) ?: return false
        constraintStorage = completedStorage
        if (restoreBodyResolveState) {
            preBodyResolveSnapshot.restore()
        }
        writeCompletionResult(variable, completionResult)
        return true
    }

    /**
     * 根据首轮 body resolve 后留下的 placeholder 类型和最终 substitutor 计算 lambda 的最终函数类型。
     *
     * 计算必须发生在恢复快照之前；恢复后参数类型引用会回到隐式状态，不再携带可替换的 inference variable。
     */
    private fun buildCompletionResult(
        variable: CfirVariable?,
        substitutor: ConeSubstitutor,
    ): CompletionResult? {
        val lambda = lambdaExpression.anonymousFunction
        val fallbackFunctionType = variable?.returnTypeRef?.coneTypeOrNull
            ?.substituteForDeclaration(substitutor) as? ConeFunctionType

        val parameterTypes = lambda.valueParameters.mapIndexed { index, parameter ->
            parameter.returnTypeRef.coneTypeOrNull
                ?.substituteForDeclaration(substitutor)
                ?: fallbackFunctionType?.parameterTypes?.getOrNull(index)
                ?: return null
        }
        val returnType = lambda.returnTypeRef.coneTypeOrNull
            ?.substituteForDeclaration(substitutor)
            ?: fallbackFunctionType?.returnType
            ?: return null
        val functionType = ConeFunctionType(
            parameterTypes = parameterTypes,
            returnType = returnType,
            isCFunc = fallbackFunctionType?.isCFunc ?: false,
            isClosureType = fallbackFunctionType?.isClosureType ?: false,
            hasVariableLenArg = fallbackFunctionType?.hasVariableLenArg ?: false,
            attributes = fallbackFunctionType?.attributes ?: ConeAttributes.Empty,
        )
        return CompletionResult(parameterTypes, returnType, functionType)
    }

    /**
     * 将最终函数类型写回变量声明和匿名函数 header。
     */
    private fun writeCompletionResult(
        variable: CfirVariable?,
        completionResult: CompletionResult,
    ) {
        val lambda = lambdaExpression.anonymousFunction
        lambda.valueParameters.zip(completionResult.parameterTypes).forEach { (parameter, type) ->
            parameter.replaceReturnTypeRef(parameter.returnTypeRef.resolvedFrom(type))
        }
        lambda.replaceReturnTypeRef(lambda.returnTypeRef.resolvedFrom(completionResult.returnType))
        val functionType = completionResult.functionType
        lambda.lambdaParameterShapeExpectedFunctionType = functionType
        lambda.replaceMatchingParameterFunctionType(functionType)
        lambda.replaceTypeRef(functionType.toCfirResolvedTypeRef(lambda.typeRef.source, lambda.typeRef))
        lambdaExpression.replaceConeTypeOrNull(functionType)
        if (variable != null) {
            variable.replaceReturnTypeRef(functionType.toCfirResolvedTypeRef(variable.returnTypeRef.source, variable.returnTypeRef))
        }
    }

    /**
     * completion 后写回到声明树的最终类型。
     */
    private data class CompletionResult(
        val parameterTypes: List<ConeCangJieType>,
        val returnType: ConeCangJieType,
        val functionType: ConeFunctionType,
    )
}

/**
 * 函数类型 receiver 引用到的局部 lambda initializer 推断状态。
 */
internal data class CfirLocalLambdaInitializerInferenceReference(
    /** 被调用的局部变量声明。 */
    val variable: CfirVariable,
    /** 变量 initializer 上保存的推断状态。 */
    val data: CfirLocalLambdaInitializerInferenceData,
)

private object LocalLambdaInitializerInferenceDataKey : CfirDeclarationDataKey()

/**
 * 声明上的局部 lambda initializer 推断状态。
 */
internal var CfirDeclaration.localLambdaInitializerInferenceData: CfirLocalLambdaInitializerInferenceData? by
    CfirDeclarationDataRegistry.data(LocalLambdaInitializerInferenceDataKey)

/**
 * 读取并规范化变量声明上的局部 lambda initializer 推断状态。
 *
 * 部分声明会先通过 initializer 完成函数类型占位符，再由 lazy/implicit body resolve
 * 发布变量声明；若声明属性尚未同步，这里从 initializer 的匿名函数声明把状态提升到
 * 变量声明，保证函数值调用入口看到同一份约束系统。
 */
internal fun CfirVariable.localLambdaInitializerInferenceDataOrNull():
        CfirLocalLambdaInitializerInferenceData? {
    localLambdaInitializerInferenceData?.let { return it }

    val data = (initializer as? CfirAnonymousFunctionExpression)
        ?.anonymousFunction
        ?.localLambdaInitializerInferenceData
        ?: return null
    localLambdaInitializerInferenceData = data
    return data
}

/**
 * 若表达式是局部变量访问且该变量由无上下文 lambda 初始化，返回其推断状态。
 */
internal fun CfirExpression.localLambdaInitializerInferenceReferenceOrNull():
        CfirLocalLambdaInitializerInferenceReference? {
    val access = this as? CfirQualifiedAccessExpression ?: return null
    val symbol = when (val reference = access.calleeReference) {
        is CfirResolvedNamedReference -> reference.resolvedSymbol
        is CfirNamedReferenceWithCandidate -> reference.candidateSymbol
        else -> null
    } as? CfirVariableSymbol<*> ?: return null

    val variable = symbol.cfir
    val data = variable.localLambdaInitializerInferenceDataOrNull() ?: return null
    return CfirLocalLambdaInitializerInferenceReference(variable, data)
}

private fun CfirValueParameter.substituteReturnTypeRef(substitutor: ConeSubstitutor) {
    val currentType = returnTypeRef.coneTypeOrNull ?: return
    val substituted = currentType.substituteForDeclaration(substitutor)
    if (substituted != currentType) {
        replaceReturnTypeRef(returnTypeRef.resolvedFrom(substituted))
    }
}

private fun CfirAnonymousFunction.substituteReturnTypeRef(substitutor: ConeSubstitutor) {
    val currentType = returnTypeRef.coneTypeOrNull ?: return
    val substituted = currentType.substituteForDeclaration(substitutor)
    if (substituted != currentType) {
        replaceReturnTypeRef(returnTypeRef.resolvedFrom(substituted))
    }
}

private fun CfirTypeRef.resolvedFrom(type: ConeCangJieType) =
    type.toCfirResolvedTypeRef(source, this)

private fun ConeCangJieType.substituteForDeclaration(substitutor: ConeSubstitutor): ConeCangJieType =
    IdealTypeResolver.resolveIfIdeal(substitutor.substituteOrNull(this) ?: this)

private fun CfirAnonymousFunction.buildCurrentFunctionType(
    fallback: ConeCangJieType?,
): ConeFunctionType? {
    val fallbackFunctionType = fallback as? ConeFunctionType
    val parameterTypes = valueParameters.map { parameter ->
        parameter.returnTypeRef.coneTypeOrNull ?: return fallbackFunctionType
    }
    val returnType = returnTypeRef.coneTypeOrNull ?: return fallbackFunctionType
    return ConeFunctionType(
        parameterTypes = parameterTypes,
        returnType = returnType,
        isCFunc = fallbackFunctionType?.isCFunc ?: false,
        isClosureType = fallbackFunctionType?.isClosureType ?: false,
        hasVariableLenArg = fallbackFunctionType?.hasVariableLenArg ?: false,
        attributes = fallbackFunctionType?.attributes ?: ConeAttributes.Empty,
    )
}
