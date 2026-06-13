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

/*
 * 本文件由生成器自动生成
 * 请勿手动修改
 */

import org.cangnova.cangjie.cfir.expressions.*

typealias CfirBasicExpressionChecker = CfirExpressionChecker<CfirStatement>
typealias CfirLiteralExpressionChecker = CfirExpressionChecker<CfirLiteralExpression>
typealias CfirFunctionCallChecker = CfirExpressionChecker<CfirFunctionCall>
typealias CfirNamedAccessChecker = CfirExpressionChecker<CfirNamedAccessExpression>
typealias CfirQualifiedAccessChecker = CfirExpressionChecker<CfirQualifiedAccessExpression>
typealias CfirSuperReceiverExpressionChecker = CfirExpressionChecker<CfirSuperReceiverExpression>
typealias CfirAssignmentChecker = CfirExpressionChecker<CfirAssignment>
typealias CfirIncrementDecrementExpressionChecker = CfirExpressionChecker<CfirIncrementDecrementExpression>
typealias CfirBinaryOpChecker = CfirExpressionChecker<CfirBinaryOp>
typealias CfirComparisonExpressionChecker = CfirExpressionChecker<CfirComparisonExpression>
typealias CfirTypeOperatorChecker = CfirExpressionChecker<CfirTypeOperator>
typealias CfirIfExpressionChecker = CfirExpressionChecker<CfirIfExpression>
typealias CfirMatchExpressionChecker = CfirExpressionChecker<CfirMatchExpression>
typealias CfirTryExpressionChecker = CfirExpressionChecker<CfirTryExpression>
typealias CfirThrowExpressionChecker = CfirExpressionChecker<CfirThrowExpression>
typealias CfirReturnExpressionChecker = CfirExpressionChecker<CfirReturnExpression>
typealias CfirLoopJumpChecker = CfirExpressionChecker<CfirLoopJump>
typealias CfirRangeExpressionChecker = CfirExpressionChecker<CfirRangeExpression>
typealias CfirSubscriptExpressionChecker = CfirExpressionChecker<CfirSubscriptExpression>
typealias CfirErrorExpressionChecker = CfirExpressionChecker<CfirErrorExpression>
