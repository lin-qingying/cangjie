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

package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.resolve.calls.inference.model.*
import org.cangnova.cangjie.type.model.*
import org.cangnova.cangjie.types.AbstractTypeApproximator
import org.cangnova.cangjie.utils.SmartSet
import java.util.*

/**
 * 约束合并器（Constraint Incorporator）。
 *
 * 负责从已有约束中推导出新的隐含约束，是类型推断引擎的核心组件之一。
 *
 * 核心规则（两种合并方向）：
 * 1. 直接合并（[directWithVariable]）：
 *    A <:(=) α <:(=) B  =>  A <: B
 * 2. 间接合并（[insideOtherConstraint]）：
 *    α <: Number, β <: Inv<α>  =>  β <: Inv<out Number>
 *
 * @param typeApproximator 类型近似器，用于将捕获类型近似为上界或下界。（仓颉不再使用捕获类型，保留供其他近似场景使用）
 * @param trivialConstraintTypeInferenceOracle 判断新生成的约束是否平凡（无需加入）的决策器。
 * @param utilContext 约束系统工具方法上下文。
 * @param languageVersionSettings 语言版本配置。
 * @param inferenceLoggerParameter 可选的推断日志记录器，用于调试。
 */
class ConstraintIncorporator(
    val typeApproximator: AbstractTypeApproximator,

    val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,
    val utilContext: ConstraintSystemUtilContext,
    private val languageVersionSettings: LanguageVersionSettings,
    inferenceLoggerParameter: InferenceLogger? = null,
) {
    /**
     * 推断日志记录器。
     * 仅在非 Dummy 实例时启用，避免无效调用产生性能损耗。
     */
    val inferenceLogger = inferenceLoggerParameter.takeIf { it !is InferenceLogger.Dummy }

    /**
     * 约束合并器所需的类型系统上下文接口。
     *
     * 在 [TypeSystemInferenceExtensionContext] 的基础上，额外提供：
     * - 所有类型变量及其约束的访问能力
     * - 新约束的注册入口
     * - 类型近似缓存
     */
    interface Context : TypeSystemInferenceExtensionContext {
        /** 当前约束存储中所有带约束的类型变量集合。 */
        val allTypeVariablesWithConstraints: Collection<VariableWithConstraints>

        /**
         * 查询约束中包含指定类型变量的所有其他变量。
         * 用于间接合并（[insideOtherConstraint]）阶段快速定位受影响的变量。
         */
        fun getVariablesWithConstraintsContainingGivenTypeVariable(
            variableConstructorMarker: TypeConstructorMarker,
        ): Collection<VariableWithConstraints>

        /**
         * 根据类型构造器查找对应的类型变量。
         * 若该变量已被固定（fixed），则属于错误状态。
         */
        fun getTypeVariable(typeConstructor: TypeConstructorMarker): TypeVariableMarker?

        /** 获取指定类型变量的所有当前约束列表。 */
        fun getConstraintsForVariable(typeVariable: TypeVariableMarker): List<Constraint>

        /**
         * 注册一条由合并过程产生的新初始约束（A <: B）。
         *
         * @param lowerType 下界类型 A。
         * @param upperType 上界类型 B。
         * @param newDerivedFrom 新约束的推导来源，为两条原始约束 derivedFrom 的并集。
         * @param isFromDeclaredUpperBound 是否来自声明上界约束位置。
         * @param isNoInfer 是否标记为禁止推断。
         */
        fun processNewInitialConstraintFromIncorporation(
            lowerType: CangJieTypeMarker,
            upperType: CangJieTypeMarker,
            newDerivedFrom: Set<TypeVariableMarker>,
            isFromDeclaredUpperBound: Boolean,
            isNoInfer: Boolean,
        )

        /**
         * 将合并阶段产生的新约束加入约束存储。
         *
         * 与 [processNewInitialConstraintFromIncorporation] 的区别在于：
         * 此方法用于间接合并（第二类合并）产生的约束，携带完整的 [ConstraintContext]。
         */
        fun addNewIncorporatedConstraint(
            typeVariable: TypeVariableMarker,
            type: CangJieTypeMarker,
            constraintContext: ConstraintContext,
        )

    }

    /**
     * 对类型变量 α 的一条约束执行合并，推导出新的隐含约束。
     *
     * 跳过递归约束（约束类型中包含 α 自身），防止无限展开。
     *
     * @param typeVariable α，当前正在处理的类型变量。
     * @param constraint α 的一条待合并约束。
     */
    context(c: Context)
    fun incorporate(typeVariable: TypeVariableMarker, constraint: Constraint) {
        // 递归约束（如 α <: List<α>）跳过，避免无限展开
        if (constraint.areThereRecursiveConstraints(typeVariable)) return

        directWithVariable(typeVariable, constraint)
        insideOtherConstraint(typeVariable, constraint)
    }

    /**
     * 判断约束的类型中是否包含对 [typeVariable] 自身的递归引用。
     */
    context(c: Context)
    private fun Constraint.areThereRecursiveConstraints(typeVariable: TypeVariableMarker) =
        type.contains {
            it.typeConstructor().unwrapStubTypeVariableConstructor() == typeVariable.freshTypeConstructor()
        }

    /**
     * 第一类合并：直接与变量的其他约束合并。
     *
     * 规则：A <:(=) α <:(=) B  =>  A <: B
     *
     * - 若当前约束是上界（α <: B），则遍历 α 的所有下界/等式约束 A，生成 A <: B
     * - 若当前约束是下界（A <: α），则遍历 α 的所有上界/等式约束 B，生成 A <: B
     */
    context(c: Context)
    private fun directWithVariable(typeVariable: TypeVariableMarker, constraint: Constraint) {
        // 当前约束是上界：遍历 α 的下界/等式约束，生成 lower <: constraint.type
        if (constraint.kind != ConstraintKind.LOWER) {
            typeVariable.forEachConstraint {
                if (it !== constraint && it.kind != ConstraintKind.UPPER) {
                    inferenceLogger?.withOrigins(
                        typeVariable, it,
                        typeVariable, constraint,
                    ) {
                        c.processNewInitialConstraintFromIncorporation(
                            lowerType = it.type,
                            upperType = constraint.type,
                            newDerivedFrom = constraint.computeNewDerivedFrom(it),
                            isFromDeclaredUpperBound = false,
                            isNoInfer = constraint.isNoInfer || it.isNoInfer,
                        )
                    } ?: c.processNewInitialConstraintFromIncorporation(
                        lowerType = it.type,
                        upperType = constraint.type,
                        newDerivedFrom = constraint.computeNewDerivedFrom(it),
                        isFromDeclaredUpperBound = false,
                        isNoInfer = constraint.isNoInfer || it.isNoInfer,
                    )
                }
            }
        }

        // 当前约束是下界：遍历 α 的上界/等式约束，生成 constraint.type <: upper
        if (constraint.kind != ConstraintKind.UPPER) {
            typeVariable.forEachConstraint {
                if (it !== constraint && it.kind != ConstraintKind.LOWER) {
                    // 来自声明上界位置（DeclaredUpperBound）且不是类型变量时，标记来源
                    val isFromDeclaredUpperBound =
                        it.position.from is DeclaredUpperBoundConstraintPosition<*> &&
                                !it.type.typeConstructor().isTypeVariable()

                    inferenceLogger?.withOrigins(
                        typeVariable, constraint,
                        typeVariable, it,
                    ) {
                        c.processNewInitialConstraintFromIncorporation(
                            lowerType = constraint.type,
                            upperType = it.type,
                            newDerivedFrom = constraint.computeNewDerivedFrom(it),
                            isFromDeclaredUpperBound = isFromDeclaredUpperBound,
                            isNoInfer = constraint.isNoInfer || it.isNoInfer,
                        )
                    } ?: c.processNewInitialConstraintFromIncorporation(
                        lowerType = constraint.type,
                        upperType = it.type,
                        newDerivedFrom = constraint.computeNewDerivedFrom(it),
                        isFromDeclaredUpperBound = isFromDeclaredUpperBound,
                        isNoInfer = constraint.isNoInfer || it.isNoInfer,
                    )
                }
            }
        }
    }

    /**
     * 合并两条约束的 derivedFrom 集合，用于追踪新约束的推导来源。
     * 结果是自反的（交换两个参数结果相同）。
     */
    private fun Constraint.computeNewDerivedFrom(other: Constraint): Set<TypeVariableMarker> =
        when {
            derivedFrom.isEmpty() -> other.derivedFrom
            other.derivedFrom.isEmpty() -> derivedFrom
            else -> derivedFrom + other.derivedFrom
        }

    /**
     * 遍历类型变量的所有约束并执行 [action]。
     *
     * 使用下标循环而非迭代器，因为遍历过程中约束列表可能追加新元素（只追加，不删除），
     * 下标循环可以安全处理这种情况。
     */
    context(c: Context)
    private inline fun TypeVariableMarker.forEachConstraint(action: (Constraint) -> Unit) {
        val constraints = c.getConstraintsForVariable(this)
        var i = 0
        while (i < constraints.size) {
            action(constraints[i++])
        }
    }

    /**
     * 第二类合并：将约束传播到包含该类型变量的其他约束中。
     *
     * 规则：α <: Number, β <: Inv<α>  =>  β <: Inv<out Number>
     *
     * 查找所有约束中含有 α 的其他变量 β，对每条含 α 的约束执行替换和近似。
     */
    context(c: Context)
    private fun insideOtherConstraint(
        typeVariable: TypeVariableMarker,
        constraint: Constraint,
    ) {
        // 若 α 已在约束的 derivedFrom 中，说明该约束本身就是由 α 推导来的，跳过以防循环
        if (typeVariable in constraint.derivedFrom) return

        val freshTypeConstructor = typeVariable.freshTypeConstructor()
        for (storageForOtherVariable in c.getVariablesWithConstraintsContainingGivenTypeVariable(freshTypeConstructor)) {
            for (otherConstraint in storageForOtherVariable.getConstraintsContainedSpecifiedTypeVariable(freshTypeConstructor)) {
                inferenceLogger?.withOrigins(
                    typeVariable, constraint,
                    storageForOtherVariable.typeVariable, otherConstraint,
                ) {
                    generateNewConstraintForSecondIncorporationKind(
                        typeVariable, constraint,
                        storageForOtherVariable.typeVariable, otherConstraint,
                    )
                } ?: generateNewConstraintForSecondIncorporationKind(
                    typeVariable, constraint,
                    storageForOtherVariable.typeVariable, otherConstraint,
                )
            }
        }
    }

    /**
     * 为第二类合并生成新约束。
     *
     * 规则：α <: Number, β <: Inv<α>  =>  β <: Inv<out Number>
     *
     * 先计算替换后的约束类型（可能需要近似），再分别生成上界和下界方向的新约束。
     *
     * @param causeOfIncorporationVariable α，触发合并的类型变量。
     * @param causeOfIncorporationConstraint α 的约束（如 α <: Number）。
     * @param otherVariable β，受影响的其他类型变量。
     * @param otherConstraint β 的约束（如 β <: Inv<α>）。
     */
    context(c: Context)
    private fun generateNewConstraintForSecondIncorporationKind(
        causeOfIncorporationVariable: TypeVariableMarker,
        causeOfIncorporationConstraint: Constraint,
        otherVariable: TypeVariableMarker,
        otherConstraint: Constraint,
    ) {
        if (causeOfIncorporationVariable in otherConstraint.derivedFrom) return

        // 生成上界方向的新约束（β <: Inv<Number>）
        if (otherConstraint.kind != ConstraintKind.LOWER) {
            computeConstraintTypeForSecondIncorporationKind(
                causeOfIncorporationVariable,
                causeOfIncorporationConstraint,
                otherConstraint,
                toSuper = true,
            )?.let { type ->
                addNewConstraintForSecondIncorporationKind(
                    causeOfIncorporationVariable, causeOfIncorporationConstraint,
                    otherVariable, otherConstraint,
                    type, isSubtype = false,
                )
            }
        }

        // 生成下界方向的新约束
        if (otherConstraint.kind != ConstraintKind.UPPER) {
            computeConstraintTypeForSecondIncorporationKind(
                causeOfIncorporationVariable,
                causeOfIncorporationConstraint,
                otherConstraint,
                toSuper = false,
            )?.let { type ->
                addNewConstraintForSecondIncorporationKind(
                    causeOfIncorporationVariable, causeOfIncorporationConstraint,
                    otherVariable, otherConstraint,
                    type, isSubtype = true,
                )
            }
        }
    }

    /**
     * 计算第二类合并中，将 α 替换为对应类型后的约束类型。
     *
     * Kotlin 在泛型容器中传播非等式约束时会创建 captured projection，
     * 再按目标方向近似为 `out`/`in` 投影。仓颉没有 use-site projection，
     * 且官方语义对同构造器泛型实参做双向统一；因此非等式约束不能被直接
     * 替换进带实参的泛型容器，否则会把 `Base<K>` 错误收紧成 `Base<Any>`。
     *
     * - 等式约束：直接替换，保留精确信息。
     * - 非等式约束 + 非泛型类型：按 Kotlin captured 近似后的方向结果替换。
     * - 非等式约束 + 泛型类型：无可表示的投影结果，跳过该派生约束。
     *
     * @param causeOfIncorporationVariable α
     * @param causeOfIncorporationConstraint α 的约束
     * @param otherConstraint β 的约束（含 α 的那条）
     * @param toSuper true 表示生成上界方向约束，false 表示生成下界方向约束
     * @return 替换后的约束类型；若无可表示的仓颉类型则返回 null
     */
    context(c: Context)
    private fun computeConstraintTypeForSecondIncorporationKind(
        causeOfIncorporationVariable: TypeVariableMarker,
        causeOfIncorporationConstraint: Constraint,
        otherConstraint: Constraint,
        toSuper: Boolean,
    ): CangJieTypeMarker? {
        val isBaseGenericType = otherConstraint.type.argumentsCount() != 0
        val alphaReplacement = when (causeOfIncorporationConstraint.kind) {
            ConstraintKind.EQUALITY -> {
                // 等式约束：直接替换
                causeOfIncorporationConstraint.type
            }
            ConstraintKind.UPPER -> {
                if (isBaseGenericType) return null
                // α <: Number：上界方向得到 Number，下界方向得到 Nothing。
                if (toSuper) causeOfIncorporationConstraint.type else c.nothingType()
            }
            ConstraintKind.LOWER -> {
                if (isBaseGenericType) return null
                // Number <: α：上界方向得到 Any，下界方向得到 Number。
                if (toSuper) c.anyType() else causeOfIncorporationConstraint.type
            }
        }

        return otherConstraint.type.substitute(causeOfIncorporationVariable, alphaReplacement)
    }

    /**
     * 将第二类合并产生的新约束加入约束存储（若满足条件）。
     *
     * 过滤条件（任一不满足则跳过）：
     * 1. 新约束类型不包含目标变量自身（防止递归）
     * 2. 来源约束是等式、或来自变量固定位置、或新类型包含原约束类型（防止信息丢失）
     * 3. 新约束对推断有实质帮助（非平凡约束）
     *
     * @param causeOfIncorporationVariable α
     * @param causeOfIncorporationConstraint α 的约束
     * @param targetVariable β，新约束的目标变量
     * @param otherConstraint β 的原约束
     * @param newConstraintType 替换近似后的新约束类型
     * @param isSubtype true 表示生成下界约束，false 表示上界约束
     */
    context(c: Context)
    private fun addNewConstraintForSecondIncorporationKind(
        causeOfIncorporationVariable: TypeVariableMarker,
        causeOfIncorporationConstraint: Constraint,
        targetVariable: TypeVariableMarker,
        otherConstraint: Constraint,
        newConstraintType: CangJieTypeMarker,
        isSubtype: Boolean,
    ) {
        // 新约束类型包含目标变量自身，跳过（防止递归约束）
        if (newConstraintType.containsNestedTypeVariable(targetVariable)) return

        val isFromVariableFixation =
            otherConstraint.position.from is FixVariableConstraintPosition<*> ||
                    causeOfIncorporationConstraint.position.from is FixVariableConstraintPosition<*>

        // 过滤无意义的约束：非等式来源、非固定位置、且新类型不包含原约束构造器
        if (!causeOfIncorporationConstraint.kind.isEqual() &&
            !isFromVariableFixation &&
            !newConstraintType.containsConstrainingTypeWithoutProjection(causeOfIncorporationConstraint)
        ) return

        // 平凡约束（如 T <: Any）无推断价值，跳过
        if (trivialConstraintTypeInferenceOracle.isGeneratedConstraintTrivial(
                otherConstraint, causeOfIncorporationConstraint, newConstraintType, isSubtype,
            )
        ) return

        // 合并两条原始约束的 derivedFrom，并加入 α 自身
        val derivedFrom = SmartSet.create(otherConstraint.derivedFrom).also {
            it.addAll(causeOfIncorporationConstraint.derivedFrom)
        }
        derivedFrom.add(causeOfIncorporationVariable)

        val kind = if (isSubtype) ConstraintKind.LOWER else ConstraintKind.UPPER

        // 保留输入类型位置信息，用于后续推断阶段的诊断
        val inputTypePosition =
            otherConstraint.position.from as? OnlyInputTypeConstraintPosition
                ?: otherConstraint.inputTypePositionBeforeIncorporation

        val constraintContext = ConstraintContext(
            kind = kind,
            derivedFrom = derivedFrom,
            inputTypePositionBeforeIncorporation = inputTypePosition,
            isNoInfer = causeOfIncorporationConstraint.isNoInfer || otherConstraint.isNoInfer,
        )

        c.addNewIncorporatedConstraint(targetVariable, newConstraintType, constraintContext)
    }

    /**
     * 判断类型的嵌套类型实参中是否存在与 [otherConstraint] 类型构造器相同的类型。
     *
     * 用于过滤阶段：确保新约束中保留了原约束类型的信息，不造成信息丢失。
     */
    context(c: Context)
    private fun CangJieTypeMarker.containsConstrainingTypeWithoutProjection(
        otherConstraint: Constraint,
    ): Boolean {
        return anyNestedArgument {
            it.getType()?.typeConstructor() == otherConstraint.type.typeConstructor()
        }
    }

    /**
     * 判断类型的嵌套类型实参中是否包含指定的类型变量。
     *
     * 用于防止生成包含目标变量自身的递归约束。
     */
    context(c: Context)
    private fun CangJieTypeMarker.containsNestedTypeVariable(
        targetVariable: TypeVariableMarker,
    ): Boolean {
        return anyNestedArgument { typeArgument ->
            targetVariable == typeArgument.getType()
                ?.let { c.getTypeVariable(it.typeConstructor().unwrapStubTypeVariableConstructor()) }
        }
    }

    /**
     * 将类型中的类型变量 [typeVariable] 替换为 [value]。
     *
     * 通过构造单变量替换器实现，是约束传播的基础操作。
     */
    context(c: Context)
    private fun CangJieTypeMarker.substitute(
        typeVariable: TypeVariableMarker,
        value: CangJieTypeMarker,
    ): CangJieTypeMarker {
        val substitutor = c.typeSubstitutorByTypeConstructor(
            mapOf(typeVariable.freshTypeConstructor() to value)
        )
        return substitutor.safeSubstitute(this)
    }

}

/**
 * 深度遍历类型的所有嵌套类型实参，对每个实参执行 [predicate]，
 * 若任一实参满足条件则返回 true。
 *
 * 使用显式栈（ArrayDeque）实现深度优先遍历，避免递归栈溢出。
 * 将类型自身包装为类型实参作为遍历起点，统一处理入口。
 */
context(c: TypeSystemInferenceExtensionContext)
private inline fun CangJieTypeMarker.anyNestedArgument(
    predicate: (TypeArgumentMarker) -> Boolean,
): Boolean {
    val stack = ArrayDeque<TypeArgumentMarker>()
    stack.push(c.createTypeArgument(this))

    while (!stack.isEmpty()) {
        val typeProjection = stack.pop()
        val typeProjectionType = typeProjection.getType() ?: continue

        if (predicate(typeProjection)) return true

        // 将当前类型的所有实参压栈，继续深度遍历
        for (argumentIndex in 0 until typeProjectionType.argumentsCount()) {
            stack.add(typeProjectionType.getArgument(argumentIndex))
        }
    }

    return false
}
