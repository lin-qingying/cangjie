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
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.resolve.body.*
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.type
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.calls.inference.addEqualityConstraintIfCompatible
import org.cangnova.cangjie.resolve.calls.inference.addSubtypeConstraintIfCompatible
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemCompletionMode
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintStorage
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import org.cangnova.cangjie.type.model.TypeVariableMarker

/**
 * body resolve 期间可插拔的调用推断会话。
 *
 * 默认实现不共享约束系统；PCLA 等高级模式通过子类改写候选基础约束和 lambda 完成策略。
 */
abstract class CfirInferenceSession {
    /** 半固定的代表类型不撤销变量所有权，也不从约束系统中移除变量。 */
    open val semiFixedVariables: Map<TypeConstructorMarker, org.cangnova.cangjie.type.model.CangJieTypeMarker>
        get() = emptyMap()

    /** 仅为已有 proper 约束的变量登记代表类型，完整固定仍由其所属调用完成。 */
    open fun getAndSemiFixCurrentResultIfTypeVariable(type: ConeCangJieType): ConeCangJieType? = null

    /** 输入类型试算允许以其他未固定变量作为代表类型。 */
    open fun semiFixTypeVariablesAllowingFixationToOtherOnes(type: ConeCangJieType, myCs: ConstraintSystemImpl) {
    }

    /** 当前会话正在使用的约束系统是否已失败。 */
    open val hasCurrentConstraintContradiction: Boolean
        get() = false

    /** 返回指定候选应复用的基础约束存储。 */
    open fun baseConstraintStorageForCandidate(
        candidate: org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate,
        bodyResolveContext: BodyResolveContext,
    ): ConstraintStorage? = null

    /** 返回指定调用应使用的自定义完成模式；null 表示执行普通 full completion。 */
    open fun customCompletionModeInsteadOfFull(call: org.cangnova.cangjie.cfir.expressions.CfirResolvable): ConstraintSystemCompletionMode? =
        null

    /** 处理一次部分解析调用的回调。 */
    abstract fun <T> processPartiallyResolvedCall(
        call: T,
        resolutionMode: org.cangnova.cangjie.cfir.resolve.ResolutionMode,
        completionMode: ConstraintSystemCompletionMode,
    ) where T : org.cangnova.cangjie.cfir.expressions.CfirResolvable, T : org.cangnova.cangjie.cfir.expressions.CfirExpression

    /** 在当前推断会话内执行 lambda completion。 */
    open fun runLambdaCompletion(
        candidate: org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate,
        forOverloadByLambdaReturnType: Boolean,
        block: () -> Unit,
    ): ConstraintStorage? {
        block()
        return null
    }

    /** 在兼容时向指定系统添加 subtype 约束。 */
    open fun addSubtypeConstraintIfCompatible(
        lowerType: org.cangnova.cangjie.cfir.types.ConeCangJieType,
        upperType: org.cangnova.cangjie.cfir.types.ConeCangJieType,
        element: CfirElement,
    ) {
    }

    /** 在当前推断会话的共享系统中添加 equality 约束。 */
    open fun addEqualityConstraintIfCompatible(
        firstType: org.cangnova.cangjie.cfir.types.ConeCangJieType,
        secondType: org.cangnova.cangjie.cfir.types.ConeCangJieType,
    ): Boolean = false

    /** 在当前推断会话中注册结构化表达式推断产生的类型变量。 */
    open fun registerInferenceVariable(variable: TypeVariableMarker) {
    }

    /**
     * 判断类型中是否含当前推断会话系统已登记的推断变量。
     *
     * fresh lambda receiver 的成员候选会在 fresh-variable 阶段为 owner 泛型参数创建
     * 占位变量（官方 `ConstrainByCtor` 的 T-Fly）；这些变量属于 lambda 的求解系统，
     * 由同一 lambda 函数体的约束在定义点完成阶段定型。含这类变量的类型不能作为「单 owner 收敛」的
     * 硬界写回（`x <: Array<T-Fly>` 会把形参定型成含未解变量的非 proper 类型），
     * 默认会话不维护变量登记，恒为 false。
     */
    open fun containsRegisteredInferenceVariable(type: ConeCangJieType): Boolean = false

    /**
     * 收窄 fresh lambda receiver 的成员 owner 候选集合。
     *
     * 默认推断会话不维护跨表达式候选集合；PCLA 会话用它模拟官方 `TryInferFromSyntaxInfo`
     * 中同一 lambda 参数成员候选集合的交集。
     */
    open fun refineFreshReceiverCandidateOwners(
        receiverTypeConstructor: TypeConstructorMarker,
        ownerTypes: List<ConeCangJieType>,
    ): Set<ConeCangJieType>? = null

    /**
     * fresh lambda 形参在当前会话中的已知类型界。
     *
     * 官方 `ChkLambda` 逐语句分析，前序语句的调用完成即固定形参类型，后续成员访问
     * 按已固定的具体类型解析。CFIR 的 PCLA_POSTPONED_CALL 把参数检查推迟到 body
     * 转换之后，前序约束不会出现在共享系统的即时视图里；PCLA 会话通过读取已排队
     * postponed 调用的参数映射补足同一信息（如 `getB19(x)` 的映射携带 `x -> B19`），
     * 使后续 `x.foo19(y)` 的 owner 收窄与官方一致。
     */
    open fun knownBoundsForFreshReceiver(
        receiverTypeConstructor: TypeConstructorMarker,
    ): List<ConeCangJieType> = emptyList()

    /**
     * 记录一次 fresh lambda receiver 成员访问暴露的「owner -> 成员返回类型」形状。
     *
     * 官方 `TryInitializeBaseSum` 播种候选 owner 集时同时保留每个 owner 下可调用的成员
     * 签名；PCLA 会话用这些形状支撑实参侧过滤——外部被调方的形参类型作为目标时
     * （官方 `FilterSumUpperbound`），需要逐 owner 检查该成员的返回类型是否满足上界。
     * 默认无操作。
     */
    open fun recordFreshReceiverMemberAccessShapes(
        receiverTypeConstructor: TypeConstructorMarker,
        shapes: List<FreshReceiverMemberAccessShape>,
    ) {
    }

    /**
     * 返回 fresh lambda receiver 当前维护的候选 owner 类型集合；无记录时返回空列表。
     *
     * 操作符内建签名筛选（官方播种 sum 被操作符可用性收窄的场景）用它读取既有
     * owner-sum 证据；默认推断会话不维护集合，恒为空。
     */
    open fun knownCandidateOwnerTypesForFreshReceiver(
        receiverTypeConstructor: TypeConstructorMarker,
    ): List<ConeCangJieType> = emptyList()

    /**
     * 用「成员返回类型 <: 上界」过滤 fresh receiver 的候选 owner 并并入交集语义。
     *
     * 对应官方实参侧 `FilterSumUpperbound`：外部被调方的形参声明类型作为目标，
     * 逐 owner 检查该成员在其上的返回类型是否满足上界（如 `{x => g29(x.v)}` 中
     * g29 形参 String 把 sum={A29,B29} 收窄到唯一 B29）；幸存者经
     * [refineFreshReceiverCandidateOwners] 与既有集合相交并在单一收敛时写入约束。
     * 默认无操作。
     */
    open fun refineFreshReceiverOwnersByMemberReturnBound(
        receiverTypeConstructor: TypeConstructorMarker,
        memberName: Name,
        returnTypeUpperBound: ConeCangJieType,
    ) {
    }

    /**
     * 语句级推断边界：块内每条语句解析完成后回调。
     *
     * 官方 `ChkLambda`/`SynLamExpr` 按语句顺序分析 lambda body，前序语句完成即固定
     * 已有唯一解的形参 tyvar，后续语句按具体类型解析。CFIR 的 PCLA_POSTPONED_CALL
     * 把调用参数检查推迟到 body 转换之后，该回调给推断会话一个在语句边界即时消化
     * 本语句排队 postponed 调用的机会。默认无操作，仅 synthetic 顶层 lambda 的 PCLA
     * 会话启用逐语句处理。
     */
    open fun onStatementResolved(statement: CfirStatement) {
    }

    companion object {
        /** 默认推断会话，不改变普通调用解析行为。 */
        val DEFAULT: CfirInferenceSession = object : CfirInferenceSession() {
            override fun <T> processPartiallyResolvedCall(
                call: T,
                resolutionMode: org.cangnova.cangjie.cfir.resolve.ResolutionMode,
                completionMode: ConstraintSystemCompletionMode,
            ) where T : org.cangnova.cangjie.cfir.expressions.CfirResolvable,
                    T : org.cangnova.cangjie.cfir.expressions.CfirExpression {
                // 普通会话不共享约束系统，也不登记 postponed 调用。
            }
        }

        /** 基于外层系统构造一个共享基础约束系统。 */
        @JvmStatic
        protected fun prepareSharedBaseSystem(
            outerSystem: ConstraintSystemImpl,
            components: InferenceComponents,
        ): ConstraintSystemImpl {
            return components.createConstraintSystem().apply {
                addOuterSystem(outerSystem.currentStorage())
            }
        }
    }
}
