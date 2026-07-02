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

package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.expressions.CfirAnonymousFunctionExpression
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.expressions.CfirLazyExpression
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.resolvedTypeFromPrototype
import org.cangnova.cangjie.cfir.resolve.calls.ConeResolutionAtom
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.visitors.CfirDefaultTransformer

/**
 * PCLA 完成后替换 lambda 子树中遗留的类型变量。
 *
 * 无上下文 lambda 会先用 placeholder 解析 body，再由外层调用完成约束。完成阶段必须
 * 把最终 substitutor 应用到已分析 lambda 的表达式类型、resolved type ref 和候选 receiver
 * atom；否则后续 checker 仍会看到旧的 placeholder 类型并产生级联诊断。
 */
internal class CfirTypeVariablesAfterPCLATransformer(
    private val substitutor: ConeSubstitutor,
) : CfirDefaultTransformer<Nothing?>() {

    override fun <E : CfirElement> transformElement(element: E, data: Nothing?): E {
        if (element is CfirResolvable) {
            element.calleeReference.candidateOrNull()?.let(::processCandidate)
        }

        if (element is CfirExpression && element.canReplaceTypeAfterPCLA()) {
            element.coneTypeOrNull
                ?.let(substitutor::substituteOrNull)
                ?.let(element::replaceConeTypeOrNull)
        }

        @Suppress("UNCHECKED_CAST")
        return element.transformChildren(this, null) as E
    }

    override fun transformResolvedTypeRef(resolvedTypeRef: CfirResolvedTypeRef, data: Nothing?): CfirTypeRef {
        return substitutor.substituteOrNull(resolvedTypeRef.coneType)
            ?.let { resolvedTypeRef.resolvedTypeFromPrototype(it, resolvedTypeRef.source) }
            ?: resolvedTypeRef
    }

    /**
     * candidate receiver atom 不是普通 children；PCLA 后要显式同步其中表达式的最终类型。
     */
    private fun processCandidate(candidate: Candidate) {
        candidate.dispatchReceiver = candidate.dispatchReceiver.transformAtomExpression()
        candidate.chosenExtensionReceiver = candidate.chosenExtensionReceiver.transformAtomExpression()
        candidate.contextArguments = candidate.contextArguments?.mapNotNull { atom ->
            atom.transformAtomExpression()
        }
    }

    private fun ConeResolutionAtom?.transformAtomExpression(): ConeResolutionAtom? =
        ConeResolutionAtom.createRawAtom(this?.expression?.transform(this@CfirTypeVariablesAfterPCLATransformer, null))

    private fun CfirReference.candidateOrNull(): Candidate? =
        (this as? CfirNamedReferenceWithCandidate)?.candidate

    private fun CfirExpression.canReplaceTypeAfterPCLA(): Boolean =
        this !is CfirAnonymousFunctionExpression &&
                this !is CfirErrorExpression &&
                this !is CfirLazyBlock &&
                this !is CfirLazyExpression
}
