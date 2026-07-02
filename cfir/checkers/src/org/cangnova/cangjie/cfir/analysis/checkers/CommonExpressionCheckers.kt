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

package org.cangnova.cangjie.cfir.analysis.checkers

import org.cangnova.cangjie.cfir.analysis.checkers.expression.*

/** CFIR 默认表达式 checker 注册表，按表达式节点类别汇总主干语义检查器。 */
object CommonExpressionCheckers : ExpressionCheckers() {
    /** 对所有基础表达式节点执行的通用表达式 checker 集合。 */
    override val basicExpressionCheckers: Set<CfirBasicExpressionChecker>
        get() = setOf(
            CfirLoopConditionTypeMismatchChecker,
            CfirVarInOrConditionChecker,
            CfirLetConditionPatternChecker,
            CfirSpawnSemanticsChecker,
            CfirExpressionWithErrorTypeChecker,
            CfirFunctionBodyTypeMismatchChecker,
            CfirVariableLambdaInitializerTypeMismatchChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirFinalizerThisUsageChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirOpenConstructorThisUsageChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirStaticContextThisUsageChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirEffectsBasicChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirRangeSemanticsChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirQuoteImportChecker,
        )

    /** 对 `if` 表达式条件类型执行的 checker 集合。 */
    override val ifExpressionCheckers: Set<CfirIfExpressionChecker>
        get() = setOf(CfirIfConditionTypeMismatchChecker)

    /** 对错误表达式节点执行的 checker 集合；当前默认主干不注册额外错误表达式 checker。 */
    override val errorExpressionCheckers: Set<CfirErrorExpressionChecker>
        get() = emptySet()

    /** 对 `match` 表达式的 case 类型、模式合法性、穷尽性和可达性执行的 checker 集合。 */
    override val matchExpressionCheckers: Set<CfirMatchExpressionChecker>
        get() = setOf(
            CfirMatchCaseTypeChecker,
            CfirMatchPatternLegalityChecker,
            CfirMatchExhaustivenessChecker,
            CfirOrPatternVariableChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirMatchUnreachablePatternChecker,
        )

    /** 对赋值、复合赋值和不可变上下文写入规则执行的 checker 集合。 */
    override val assignmentCheckers: Set<CfirAssignmentChecker>
        get() = setOf(
            CfirCompoundAssignmentSemanticsChecker,
            CfirAssignmentLegalityChecker,
            CfirAssignmentTypeMismatchChecker,
            CfirImmutableFunctionCannotModifyFieldChecker,
        )

    /** 对自增自减表达式合法性和类型规则执行的 checker 集合。 */
    override val incrementDecrementExpressionCheckers: Set<CfirIncrementDecrementExpressionChecker>
        get() = setOf(
            CfirIncrementDecrementLegalityChecker,
            CfirIncrementDecrementTypeChecker,
        )

    /** 对 return 表达式的目标和返回类型匹配规则执行的 checker 集合。 */
    override val returnExpressionCheckers: Set<CfirReturnExpressionChecker>
        get() = setOf(
            CfirReturnLegalityChecker,
            CfirReturnTypeMismatchChecker,
        )

    /** 对字面量取值范围和数值溢出执行的 checker 集合。 */
    override val literalExpressionCheckers: Set<CfirLiteralExpressionChecker>
        get() = setOf(
            CfirLiteralNumericOverflowChecker,
            CfirFloatLiteralRangeChecker,
        )

    /** 对函数调用、构造器调用、弃用调用、inout 和 API 等级规则执行的 checker 集合。 */
    override val functionCallCheckers: Set<CfirFunctionCallChecker>
        get() = setOf(
//            CfirArgumentTypeMismatchChecker,
            CfirSignedLiteralNumericOverflowChecker,
            CfirConstEvalArithmeticChecker,
            CfirConstructorDelegationCallChecker,
            CfirImmutableFunctionCannotAccessMutableFunctionChecker,
            CfirImmutableValueCannotAccessMutableFunctionChecker,
            CfirMockApiChecker,
            CfirDeprecatedCallChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirTrailingLambdaChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirVArrayConstructorArgChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirInoutArgumentChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirApiLevelRefHigherChecker,
            CfirInoutSemanticsChecker,
        )

    /** 对限定访问、类型实参、可见性相关语义和特殊成员访问执行的 checker 集合。 */
    override val qualifiedAccessCheckers: Set<CfirQualifiedAccessChecker>
        get() = setOf(
            CfirFunctionReferenceLegalityChecker,
            CfirGenericBareClassifierAccessChecker,
            CfirUpperBoundViolatedQualifiedAccessExpressionChecker,
            CfirCaptureHasShadowVariableChecker,
            CfirClassifierAsExpressionChecker,
            CfirMutFuncReferenceChecker,
            CfirUnsafeFuncReferenceChecker,
            CfirAbstractSuperMemberAccessChecker,
            CfirInterfaceCallWithUnimplementedCallChecker,
            CfirStaticContextNonStaticMemberAccessChecker,
            CfirOpenConstructorMemberAccessChecker,
        )

    /** 对 `super` 接收者表达式合法性执行的 checker 集合。 */
    override val superReceiverExpressionCheckers: Set<CfirSuperReceiverExpressionChecker>
        get() = setOf(CfirIllegalSuperReferenceChecker)

    /** 对 `try` 表达式返回、目标类型、资源和 catch 类型规则执行的 checker 集合。 */
    override val tryExpressionCheckers: Set<CfirTryExpressionChecker>
        get() = setOf(
            CfirTryHandleReturnChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirTryTargetTypeMismatchChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirTryResourceTypeChecker,
            org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirCatchTypeChecker,
        )

    /** 对 `throw` 表达式目标类型执行的 checker 集合。 */
    override val throwExpressionCheckers: Set<CfirThrowExpressionChecker>
        get() = setOf(
            org.cangnova.cangjie.cfir.analysis.checkers.expression.CfirThrowExpressionTypeChecker,
        )

    /** 对下标表达式赋值语义执行的 checker 集合。 */
    override val subscriptExpressionCheckers: Set<CfirSubscriptExpressionChecker>
        get() = setOf(CfirSubscriptAssignmentChecker)
}
