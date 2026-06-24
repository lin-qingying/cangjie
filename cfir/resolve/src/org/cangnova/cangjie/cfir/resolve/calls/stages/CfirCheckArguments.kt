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

package org.cangnova.cangjie.cfir.resolve.calls.stages

import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.diagnostic.InapplicableCandidate
import org.cangnova.cangjie.cfir.expressions.CfirArrayLiteral
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallInfo
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.resolve.calls.candidate.yieldDiagnostic
import org.cangnova.cangjie.cfir.resolve.calls.getExpectedType
import org.cangnova.cangjie.cfir.resolve.calls.prepareArgumentType
import org.cangnova.cangjie.cfir.resolve.calls.substituteExplicitTypeArgumentConstraints
import org.cangnova.cangjie.cfir.resolve.transformers.ensureResolvedTypeDeclaration
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 候选解析阶段中的实参适用性检查。
 *
 * 该阶段根据候选的参数映射为每个实参计算期望类型，并把实参表达式交给
 * [ArgumentCheckingProcessor] 推进子表达式解析和约束写入。
 */
object CfirCheckArguments : ResolutionStage() {
    /**
     * 检查候选的所有普通实参。
     */
    context(sink: CheckerSink, context: ResolutionContext)
    override suspend fun check(candidate: Candidate) {
        if (!candidate.argumentMappingInitialized) return

        val contextArgumentsOfInvoke = candidate.expectedContextParameterCountForInvoke ?: 0
        val argumentMapping = candidate.argumentMapping

        for ((index, argument) in candidate.arguments.withIndex()) {
            if (index < contextArgumentsOfInvoke) continue

            val expression = argument.expression
//            if (expression.isInaccessibleAndInapplicable()) {
//                sink.reportDiagnostic(expression.toInaccessibleReceiverDiagnostic())
//            }

            val parameter = argumentMapping[argument]
            candidate.resolveArgument(
                candidate.callInfo,
                argument,
                parameter,
                // 对齐 Kotlin CheckArguments：普通实参不能按 receiver 适用性降级。
                // 当前仓颉函数类型没有 Kotlin extension-function-type receiver 语义。
                isReceiver = false,
            )
        }

        when {
            candidate.system.hasContradiction && candidate.callInfo.arguments.isNotEmpty() -> {
                sink.yieldDiagnostic(InapplicableCandidate)
            }
        }
    }

    /**
     * 解析单个实参表达式并写入候选约束系统。
     */
    context(sink: CheckerSink, context: ResolutionContext)
    private fun Candidate.resolveArgument(
        callInfo: CallInfo,
        atom: ConeResolutionAtom,
        parameter: CfirValueParameter?,
        isReceiver: Boolean,
    ) {
        // Lambdas and callable references can be unresolved at this point
        val argument = atom.expression
        argument.coneTypeOrNull.ensureResolvedTypeDeclaration(context.session)
        val expectedType =
            prepareExpectedType(context.session, callInfo, atom, argument, parameter)
        ArgumentCheckingProcessor.resolveArgumentExpression(
            this,
            atom,
            expectedType,
            sink,
            context,
            isReceiver,
            false
        )
    }
}

/**
 * 为隐式整数转换预留的期望类型调整扩展点。
 *
 * 仓颉当前实参检查不启用该转换时返回空，调用侧会继续使用基础期望类型。
 */
private fun getExpectedTypeWithImplicitIntegerCoercion(
    session: CfirSession,
    argument: CfirExpression,
    parameter: CfirValueParameter,
    candidateExpectedType: ConeCangJieType
): ConeCangJieType? {
    return null
//    if (!session.languageVersionSettings.supportsFeature(LanguageFeature.ImplicitSignedToUnsignedIntegerConversion)) return null

}

/**
 * 计算候选参数位置上的最终期望类型。
 *
 * 该过程会处理仓颉变长参数、显式类型实参约束和候选 substitutor。
 */
context(context: ResolutionContext)
private fun Candidate.prepareExpectedType(
    session: CfirSession,
    callInfo: CallInfo,
    atom: ConeResolutionAtom,
    argument: CfirExpression,
    parameter: CfirValueParameter?,
): ConeCangJieType? {
    if (parameter == null) return null
    val basicExpectedType = selectVariadicExpectedType(session, atom, argument, parameter)
        ?: argument.getExpectedType(
            session,
            parameter,
            unwrapCangjieVariadicParameter = parameter == cangjieVariadicParameterForCall,
        )

    // 仓颉没有 SAM 转换，直接跳过那一步
    val expectedType =
        getExpectedTypeWithImplicitIntegerCoercion(session, argument, parameter, basicExpectedType)
            ?: basicExpectedType

    val substitutedExpectedType = this.substitutor.substituteOrSelf(expectedType)
    return substituteExplicitTypeArgumentConstraints(substitutedExpectedType)
}

/**
 * 根据仓颉变长参数规则选择当前实参的期望类型。
 */
context(context: ResolutionContext)
private fun Candidate.selectVariadicExpectedType(
    session: CfirSession,
    atom: ConeResolutionAtom,
    argument: CfirExpression,
    parameter: CfirValueParameter,
): ConeCangJieType? {
    variadicExpectedTypeForArgument(atom)?.let { return it }
    if (!canUseVariadicArgument(atom)) return null
    val variadicFixedPositionalArity = variadicFixedPositionalArity ?: return null
    val argumentIndex = arguments.indexOf(atom)
    if (argumentIndex < variadicFixedPositionalArity) return null

    val argumentType = argument.coneTypeOrNull ?: return null
    val normalExpectedType = this.substitutor.substituteOrSelf(argument.getExpectedType(session, parameter))
    if (argumentType is ConeErrorType || normalExpectedType is ConeErrorType) return null
    if (argument is CfirArrayLiteral) return null

    val preparedArgumentType = prepareArgumentType(argumentType, session)
    val matchesNormalArrayParameter =
        AbstractTypeChecker.isSubtypeOf(session.typeContext, preparedArgumentType, normalExpectedType) == true
    if (matchesNormalArrayParameter) return null

    // 官方 cjc 会在普通调用匹配失败后把这部分位置实参收束成 ArrayLit。
    return markVariadicArgument(atom)
}
