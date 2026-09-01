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

package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirInitializationAssignmentClassifier
import org.cangnova.cangjie.cfir.analysis.checkers.declaration.CfirInitializationAssignmentKind
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.calls.isResolvedTypeQualifier
import org.cangnova.cangjie.cfir.correspondingProperty
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirIncrementDecrementExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirSubscriptExpression
import org.cangnova.cangjie.cfir.expressions.CfirSuperReceiverExpression
import org.cangnova.cangjie.cfir.expressions.CfirThisReceiverExpression
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeVArrayType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.AbstractCjSourceElement

/**
 * 赋值左值合法性检查。
 *
 * 这一层专门处理“这个目标能不能被赋值”，不处理赋值后的类型兼容性：
 * - `let`/不可变变量、只读属性 -> `CANNOT_ASSIGN_TO_IMMUTABLE`
 * - 函数名、类型名等非左值名字 -> `UNQUALIFIED_LEFT_VALUE_ASSIGNED`
 *
 * `subscript` 赋值仍交由独立的 `operator set` 语义链处理，这里暂不重复判定。
 */
object CfirAssignmentLegalityChecker : CfirAssignmentChecker() {
    /**
     * 检查普通赋值表达式的左值是否可写。
     *
     * 下标赋值先按 VArray 内建下标规则分类；普通 qualified access 则统一交给
     * [CfirMutationTargetClassifier] 判断可写、不可变或非左值名字，并在对应源码范围上报告诊断。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirAssignment) {
        val lValue = expression.lValue
        if (lValue is CfirSubscriptExpression) {
            when (val target = CfirMutationTargetClassifier.classifySubscriptAssignment(lValue, expression)) {
                is CfirMutationTargetClassifier.MutationTarget.ImmutableValue -> {
                    reporter.reportOn(
                        source = expression.source ?: lValue.source,
                        factory = CfirErrors.CANNOT_ASSIGN_TO_IMMUTABLE,
                    )
                }

                is CfirMutationTargetClassifier.MutationTarget.NonAssignableName -> {
                    reporter.reportOn(
                        source = lValue.source ?: expression.source,
                        factory = CfirErrors.UNQUALIFIED_LEFT_VALUE_ASSIGNED,
                        a = target.name,
                    )
                }

                CfirMutationTargetClassifier.MutationTarget.Assignable,
                null,
                -> Unit
            }
            return
        }

        val access = lValue as? CfirQualifiedAccessExpression ?: return
        val selectorSource = checkNotNull(access.calleeReference.source) {
            "Qualified assignment target must retain its callee selector source"
        }
        val assignmentSource = checkNotNull(expression.source) {
            "Assignment expression must retain its source"
        }

        if (CfirMutationTargetClassifier.isVArraySizeAccess(access)) {
            reporter.reportOn(
                source = assignmentSource,
                factory = CfirErrors.CANNOT_ASSIGN_TO_IMMUTABLE,
            )
            return
        }

        when (val target = CfirMutationTargetClassifier.classifyAssignment(access, expression)) {
            is CfirMutationTargetClassifier.MutationTarget.ImmutableValue -> {
                reporter.reportOn(
                    source = selectorSource,
                    factory = CfirErrors.CANNOT_ASSIGN_TO_IMMUTABLE,
                )
            }

            is CfirMutationTargetClassifier.MutationTarget.NonAssignableName -> {
                reporter.reportOn(
                    source = selectorSource,
                    factory = CfirErrors.UNQUALIFIED_LEFT_VALUE_ASSIGNED,
                    a = target.name,
                )
            }

            CfirMutationTargetClassifier.MutationTarget.Assignable,
            null,
            -> Unit
        }
    }
}

/**
 * 自增自减目标合法性检查。
 *
 * 官方 `InitializationChecker::CheckInitInExpr` 对 `IncOrDecExpr` 会调用同一套
 * `CheckLetFlag`，主诊断范围是整个自增自减表达式。因此这里复用修改目标分类器，
 * 只负责表达式自身是否能被修改，不在这一层处理数值类型或运算符类型检查。
 */
object CfirIncrementDecrementLegalityChecker : CfirIncrementDecrementExpressionChecker() {
    /**
     * 检查 `++` / `--` 的目标是否可被修改。
     *
     * 该入口只验证目标可写性，诊断范围保持在自增自减表达式或被引用名字上，数值类型约束由
     * [CfirIncrementDecrementTypeChecker] 负责。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirIncrementDecrementExpression) {
        val access = expression.expression as? CfirQualifiedAccessExpression ?: return
        val accessSource = access.calleeReference.source ?: access.source ?: expression.source ?: return
        val expressionSource = expression.source ?: access.source ?: access.calleeReference.source ?: return

        when (val target = CfirMutationTargetClassifier.classifyMutation(access)) {
            is CfirMutationTargetClassifier.MutationTarget.ImmutableValue -> {
                reporter.reportOn(
                    source = expressionSource,
                    factory = CfirErrors.CANNOT_ASSIGN_TO_IMMUTABLE,
                )
            }

            is CfirMutationTargetClassifier.MutationTarget.NonAssignableName -> {
                reporter.reportOn(
                    source = accessSource,
                    factory = CfirErrors.UNQUALIFIED_LEFT_VALUE_ASSIGNED,
                    a = target.name,
                )
            }

            CfirMutationTargetClassifier.MutationTarget.Assignable,
            null,
                -> Unit
        }
    }
}

/**
 * 自增自减数值类型检查。
 *
 * 官方语义要求 `++` / `--` 只作用于整数类型；表达式结果类型由 resolve 阶段固定为
 * `Unit`，这里仅补充操作数类型约束，避免把非整数目标误当作合法自增表达式。
 */
object CfirIncrementDecrementTypeChecker : CfirIncrementDecrementExpressionChecker() {
    /**
     * 检查 `++` / `--` 操作数是否为整数类型。
     *
     * 错误类型和展开后的错误类型不重复报告；合法整数类型直接通过，其他类型统一报
     * `TYPE_MISMATCH`，期望类型使用官方语义中的整数基准类型。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirIncrementDecrementExpression) {
        val source = expression.source as? AbstractCjSourceElement ?: return
        val actualType = expression.expression.coneTypeOrNull ?: return
        if (actualType is ConeErrorType) return

        val expandedType = actualType.fullyExpandedType(context.session)
        if (expandedType is ConeErrorType) return
        if ((expandedType as? ConePrimitiveType)?.kind?.isInteger == true) return

        reporter.reportOn(
            source = source,
            factory = CfirErrors.TYPE_MISMATCH,
            a = ConePrimitiveType.INT64,
            b = actualType,
            c = false,
        )
    }
}

/**
 * 表达式修改目标分类器。
 *
 * 该对象把“能否修改”的框架级判定集中在一处，供赋值、自增自减和 VArray 下标赋值复用。
 * 分类结果只描述目标语义，不直接负责诊断上报。
 */
internal object CfirMutationTargetClassifier {
    /**
     * 按 VArray 内建下标赋值规则分类修改目标。
     *
     * 只有接收者展开类型为 VArray 时才介入；嵌套下标、函数调用结果等临时值被视为不可变，
     * qualified access 接收者则继续复用普通赋值目标分类。
     */
    context(context: CheckerContext)
    fun classifySubscriptAssignment(
        subscript: CfirSubscriptExpression,
        assignment: CfirAssignment,
    ): MutationTarget? {
        subscript.receiver.coneTypeOrNull
            ?.fullyExpandedType(context.session) as? ConeVArrayType ?: return null

        return when (val receiver = subscript.receiver) {
            is CfirSubscriptExpression -> MutationTarget.ImmutableValue
            is CfirFunctionCall -> MutationTarget.ImmutableValue
            is CfirQualifiedAccessExpression -> when (val target = receiver.mutationTarget(assignment)) {
                MutationTarget.Assignable -> MutationTarget.Assignable
                is MutationTarget.ImmutableValue -> target
                is MutationTarget.NonAssignableName -> MutationTarget.ImmutableValue
                null -> null
            }
            else -> MutationTarget.ImmutableValue
        }
    }

    /**
     * 判断访问是否为 VArray 的只读 `size` 属性。
     *
     * VArray 的 `size` 是内建不可变成员，不应按普通属性 setter 或变量规则处理。
     */
    context(context: CheckerContext)
    fun isVArraySizeAccess(access: CfirQualifiedAccessExpression): Boolean {
        val name = (access.calleeReference as? CfirNamedReference)?.name ?: return false
        if (name.asString() != "size") return false
        return access.explicitReceiver
            ?.coneTypeOrNull
            ?.fullyExpandedType(context.session) is ConeVArrayType
    }

    /**
     * 分类普通赋值左侧 qualified access 的修改目标。
     *
     * [assignment] 用于区分未初始化局部 `let` 的首次初始化赋值与后续非法写入。
     */
    context(context: CheckerContext)
    fun classifyAssignment(
        access: CfirQualifiedAccessExpression,
        assignment: CfirAssignment,
    ): MutationTarget? {
        return access.mutationTarget(assignment)
    }

    /**
     * 分类非赋值形态的修改目标。
     *
     * 自增、自减等语义没有初始化赋值上下文，因此这里不传入 [CfirAssignment]。
     */
    context(context: CheckerContext)
    fun classifyMutation(access: CfirQualifiedAccessExpression): MutationTarget? {
        return access.mutationTarget(assignment = null)
    }

    /**
     * 解析 qualified access 背后的声明符号，并转换为统一的修改目标分类。
     *
     * 该函数同时处理字段、变量、属性、函数名、类型名以及不可变 struct 接收者链，保持所有
     * 表达式修改入口使用同一套左值语义。
     */
    context(context: CheckerContext)
    private fun CfirQualifiedAccessExpression.mutationTarget(assignment: CfirAssignment?): MutationTarget? {
        val resolvedSymbol = resolvedAssignableSymbolOrNull()
        val directTarget = when (resolvedSymbol) {
            is CfirFieldVariableSymbol -> {
                val field = resolvedSymbol.takeIf { it.isBound }?.cfir
                if (field != null && isImmutableFieldAssignmentForbidden(field, assignment)) {
                    MutationTarget.ImmutableValue
                } else {
                    MutationTarget.Assignable
                }
            }

            is CfirVariableSymbol<*> -> {
                val variable = resolvedSymbol.takeIf { it.isBound }?.cfir
                if (variable != null && isImmutableVariableAssignmentForbidden(variable, assignment)) {
                    MutationTarget.ImmutableValue
                } else {
                    MutationTarget.Assignable
                }
            }

            is CfirPropertySymbol -> {
                val property = resolvedSymbol.takeIf { it.isBound }?.cfir as? CfirProperty
                if (property != null && !property.isEffectivelyWritable()) {
                    MutationTarget.ImmutableValue
                } else {
                    MutationTarget.Assignable
                }
            }

            is CfirEnumConstructorSymbol -> MutationTarget.ImmutableValue
            is CfirFunctionSymbol<*> -> MutationTarget.NonAssignableName(referenceNameOrFallback())
            is CfirClassLikeSymbol<*> -> MutationTarget.NonAssignableName(referenceNameOrFallback())
            null -> null
            else -> null
        }
        if (directTarget is MutationTarget.ImmutableValue || directTarget is MutationTarget.NonAssignableName) {
            return directTarget
        }
        if (isImmutableStructReceiverMutationForbidden()) return MutationTarget.ImmutableValue
        return directTarget
    }

    /**
     * 从引用节点读取可作为赋值目标的解析符号。
     *
     * 已解析引用和候选引用都可能出现在诊断阶段，因此两类引用都需要纳入分类。
     */
    private fun CfirQualifiedAccessExpression.resolvedAssignableSymbolOrNull(): CfirBasedSymbol<*>? {
        return when (val reference = calleeReference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol
            else -> null
        }
    }

    /**
     * 判断属性契约是否允许写入。
     *
     * `mut` 是属性对调用方暴露的可写契约；抽象接口属性即使没有本地 setter body，
     * 仍然必须保持可写。setter 的实现位置和可见性属于 accessor 解析，不得反向把
     * 一个 `mut prop` 降级为 immutable。
     */
    private fun CfirProperty.isEffectivelyWritable(): Boolean = status.isMut

    /**
     * 取得当前访问引用的名称，缺失时返回错误名占位。
     *
     * 非左值名字诊断需要一个稳定的 [Name]，即使引用节点尚未完整解析也不能中断分类流程。
     */
    private fun CfirQualifiedAccessExpression.referenceNameOrFallback(): Name {
        return (calleeReference as? CfirNamedReference)?.name ?: Name.ERROR_NAME
    }

    /**
     * 判断字段写入是否违反不可变字段规则。
     *
     * `var` 字段始终可写；`let` 字段只允许构造阶段的合法初始化写入，已由主构造参数占用或
     * 已有初始化器的字段不能再次赋值。
     */
    context(context: CheckerContext)
    private fun CfirQualifiedAccessExpression.isImmutableFieldAssignmentForbidden(
        field: CfirFieldVariable,
        assignment: CfirAssignment?,
    ): Boolean {
        if (field.isVar) return false
        val constructor = context.findClosestDeclaration<CfirConstructor>() ?: return true
        if (field.status.isStatic != constructor.status.isStatic) return true
        if (field.hasSameNamePrimaryConstructorPropertyInOwner()) return true
        if (field.initializer != null) return true
        return when (assignment?.let { CfirInitializationAssignmentClassifier.classifyAssignment(it, context) }) {
            CfirInitializationAssignmentKind.INITIALIZATION,
            CfirInitializationAssignmentKind.PRIORITY_INITIALIZATION_DIAGNOSTIC,
            -> false

            CfirInitializationAssignmentKind.REASSIGNMENT,
            CfirInitializationAssignmentKind.NOT_TRACKED,
            null,
            -> true
        }
    }

    /**
     * 显式 `let` 字段与主构造参数生成属性同名时，官方 Sema 把字段视为已被构造参数占用。
     * 因此构造器体里的后续赋值不能作为 `let` 字段首次初始化。
     */
    context(context: CheckerContext)
    private fun CfirFieldVariable.hasSameNamePrimaryConstructorPropertyInOwner(): Boolean {
        val owner = context.findClosestDeclaration<CfirClassLikeDeclaration>() ?: return false
        val targetName = name
        return owner.declarations
            .asSequence()
            .filterIsInstance<CfirConstructor>()
            .flatMap { constructor -> constructor.valueParameters.asSequence() }
            .mapNotNull { parameter -> parameter.correspondingProperty }
            .any { property -> property.name == targetName }
    }

    /**
     * 判断变量写入是否违反不可变变量规则。
     *
     * 局部无初始化器 `let` 可以通过初始化赋值完成一次赋值，其他不可变变量写入都需要报告。
     */
    context(context: CheckerContext)
    private fun isImmutableVariableAssignmentForbidden(
        variable: CfirVariable,
        assignment: CfirAssignment?,
    ): Boolean {
        if (variable.isVar) return false
        if (assignment != null && variable.isLocal && variable.initializer == null) {
            return when (CfirInitializationAssignmentClassifier.classifyAssignment(assignment, context)) {
                CfirInitializationAssignmentKind.INITIALIZATION,
                CfirInitializationAssignmentKind.PRIORITY_INITIALIZATION_DIAGNOSTIC,
                -> false

                CfirInitializationAssignmentKind.REASSIGNMENT,
                CfirInitializationAssignmentKind.NOT_TRACKED,
                -> true
            }
        }
        return true
    }

    /**
     * 判断通过不可变 struct 值接收者修改成员是否非法。
     *
     * 对值类型接收者而言，即使最终成员本身可写，只要接收者变量不可变，也不能通过成员访问链
     * 修改其内部状态。
     */
    context(context: CheckerContext)
    private fun CfirQualifiedAccessExpression.isImmutableStructReceiverMutationForbidden(): Boolean {
        val receiver = explicitReceiver ?: dispatchReceiver ?: return false
        return receiver.isImmutableStructMutationReceiverAccess()
    }

    /**
     * 修改目标分类结果。
     *
     * 该密封层级表达左值语义的三种结论：可写、不可变值、以及语法上是名字但不是可赋值实体。
     */
    sealed interface MutationTarget {
        /**
         * 表示目标可以被当前表达式修改。
         */
        data object Assignable : MutationTarget

        /**
         * 表示目标是合法值但不可写，例如 `let` 变量、只读属性或临时 VArray 值。
         */
        data object ImmutableValue : MutationTarget

        /**
         * 表示引用解析到函数、类型等非左值名字。
         *
         * [name] 用于构造 `UNQUALIFIED_LEFT_VALUE_ASSIGNED` 诊断。
         */
        data class NonAssignableName(val name: Name) : MutationTarget
    }
}

/**
 * 判断作为赋值链中间节点的 struct 值是否不可变。
 *
 * 与调用 mut 函数的 receiver 规则不同，赋值链必须把 `mut prop` 视为可写存储契约：
 * `mut prop t1: S0` 允许在 mut 函数中通过 `t1.t0` 修改并经 setter 写回。普通只读
 * 属性、let/const 变量和临时 struct 值仍然阻止链式字段写入；判断沿 qualified access
 * 的真实声明递归进行，从而同时覆盖隐式 this 与显式嵌套 receiver。
 */
context(context: CheckerContext)
private fun CfirExpression.isImmutableStructMutationReceiverAccess(): Boolean {
    if (this is CfirThisReceiverExpression || this is CfirSuperReceiverExpression) return false
    if (isResolvedTypeQualifier(context.session)) return false
    if (!coneTypeOrNull.mayBeStructValueType()) return false

    val access = this as? CfirQualifiedAccessExpression ?: return true
    val symbol = when (val reference = access.calleeReference) {
        is CfirResolvedNamedReference -> reference.resolvedSymbol
        is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol
        else -> null
    }
    val receiver = access.explicitReceiver ?: access.dispatchReceiver

    return when (symbol) {
        is CfirFieldVariableSymbol -> {
            val field = symbol.takeIf { it.isBound }?.cfir ?: return true
            !field.isVar || receiver?.isImmutableStructMutationReceiverAccess() == true
        }

        is CfirVariableSymbol<*> -> {
            val variable = symbol.takeIf { it.isBound }?.cfir ?: return true
            !variable.isVar || receiver?.isImmutableStructMutationReceiverAccess() == true
        }

        is CfirPropertySymbol -> {
            val property = symbol.takeIf { it.isBound }?.cfir as? CfirProperty ?: return true
            !property.status.isMut ||
                    context.findClosestDeclaration<CfirFunction>()?.isMutStructMemberContext() != true ||
                    receiver?.isImmutableStructMutationReceiverAccess() == true
        }

        is CfirPropertyAccessorSymbol -> {
            val property = symbol.propertySymbol.takeIf { it.isBound }?.cfir as? CfirProperty ?: return true
            !property.status.isMut ||
                    context.findClosestDeclaration<CfirFunction>()?.isMutStructMemberContext() != true ||
                    receiver?.isImmutableStructMutationReceiverAccess() == true
        }

        else -> true
    }
}

/**
 * 判断类型是否可能是 struct 值类型。
 *
 * 类型参数通过已解析上界递归判断，接口类型按可能被 struct 实现处理；该判断用于不可变接收者
 * 的保守左值语义，不负责类型展开或错误类型恢复。
 */
internal fun ConeCangJieType?.mayBeStructValueType(): Boolean = when (this) {
    is ConeStructType -> true
    is ConeTypeParameterType -> lookupTag.typeParameterSymbol.resolvedBounds.any { it.coneType.mayBeStructValueType() }
    is ConeClassLikeType -> isInterface
    else -> false
}
