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

import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.diagnostic.WrongArgumentCount
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.resolve.calls.ResolutionContext
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CheckerSink
import org.cangnova.cangjie.cfir.resolve.calls.candidate.yieldDiagnostic
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef


/**
 * 对齐 Kotlin K2 FIR 的 `MapTypeArguments`。
 *
 * 本阶段从 [CallInfo.typeArguments] 构建 [TypeArgumentMapping] 并赋值给候选。
 * 在 [CfirCreateFreshTypeVariableSubstitutorStage] **之前**执行，
 * 确保后续创建新鲜类型变量时可以读取显式类型实参。
 *
 * 同时检查显式类型参数个数是否与声明的泛型参数个数匹配。
 */
object CfirMapTypeArguments : ResolutionStage() {
    context(sink: CheckerSink, context: ResolutionContext)
    /** 解析显式类型实参并写入候选，同时检查数量是否匹配声明泛型参数。 */
    override suspend fun check(candidate: Candidate) {
        val declaration = candidate.symbol.takeIf { it.isBound }?.cfir
        if (candidate.isQualifiedEnumMemberWithTypeArguments(declaration)) {
            /*
             * `TimeUnit<Int32>.Year<Int64>` 中的 `Int64` 属于 enum member access，
             * 不能再次绑定 owner enum。合法性由表达式 checker 独立报告，候选仍按
             * qualifier 已确定的 owner 类型继续完成，避免破坏返回类型和后续 pattern。
             */
            candidate.typeArgumentMapping = TypeArgumentMapping.NoExplicitArguments
            return
        }

        val mapping = buildTypeArgumentMapping(candidate)
        candidate.typeArgumentMapping = mapping

        checkTypeArgumentCount(candidate, context)
    }

    /** 从候选调用信息构造类型实参映射。 */
    private fun buildTypeArgumentMapping(candidate: Candidate): TypeArgumentMapping {
        val explicitTypeArguments = candidate.resolvedExplicitTypeArguments()

        if (explicitTypeArguments.isEmpty()) {
            return TypeArgumentMapping.NoExplicitArguments
        }

        return TypeArgumentMapping.Mapped(explicitTypeArguments)
    }

    /**
     * 当显式类型参数个数与声明泛型参数个数不匹配时报告诊断。
     *
     * 复用已有的 [WrongArgumentCount] ResolutionDiagnostic，
     * 它在 `ConeInapplicableCandidateError` 路径中被映射。
     */
    context(sink: CheckerSink)
    private suspend fun checkTypeArgumentCount(
        candidate: Candidate,
        context: ResolutionContext,
    ) {
        val explicitCount = candidate.resolvedExplicitTypeArguments().size
        if (explicitCount == 0) return

        val declaration = candidate.symbol.takeIf { it.isBound }?.cfir ?: return
        val declaredTypeParams = CfirCreateFreshTypeVariableSubstitutorStage
            .collectCandidateTypeParametersForFreshVariables(context.session, candidate, declaration)

        val expected = declaredTypeParams.size
        if (expected != explicitCount) {
            sink.yieldDiagnostic(WrongArgumentCount(expected, explicitCount))
        }
    }

    /** 读取候选显式类型实参，优先使用 callInfo，必要时回退到 call-site。 */
    private fun Candidate.resolvedExplicitTypeArguments(): List<CfirResolvedTypeRef> {
        val fromCallInfo = callInfo.typeArguments.mapNotNull { it as? CfirResolvedTypeRef }
        if (fromCallInfo.isNotEmpty()) return fromCallInfo

        return (callInfo.callSite as? CfirQualifiedAccessExpression)
            ?.typeArguments
            ?.mapNotNull { it as? CfirResolvedTypeRef }
            .orEmpty()
    }

    /**
     * 判断显式类型实参是否写在已有 enum owner qualifier 后的成员上。
     *
     * 裸 `Year<Int32>(...)` 是 enum sugar，类型实参仍用于实例化 owner；只有
     * `TimeUnit<Int32>.Year<Int64>(...)` 这种已有显式 receiver 的成员实参非法。
     */
    private fun Candidate.isQualifiedEnumMemberWithTypeArguments(declaration: Any?): Boolean =
        declaration is CfirEnumConstructor &&
                callInfo.explicitReceiver != null &&
                resolvedExplicitTypeArguments().isNotEmpty()
}
