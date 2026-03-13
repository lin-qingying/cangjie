/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangjie.cfir.analysis.checkers.expression

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

import org.cangjie.cfir.expressions.CfirAssignment
import org.cangjie.cfir.expressions.CfirBinaryOp
import org.cangjie.cfir.expressions.CfirComparisonExpression
import org.cangjie.cfir.expressions.CfirErrorExpression
import org.cangjie.cfir.expressions.CfirFunctionCall
import org.cangjie.cfir.expressions.CfirIfExpression
import org.cangjie.cfir.expressions.CfirJumpExpression
import org.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangjie.cfir.expressions.CfirMatchExpression
import org.cangjie.cfir.expressions.CfirPropertyAccess
import org.cangjie.cfir.expressions.CfirQualifiedAccess
import org.cangjie.cfir.expressions.CfirRangeExpression
import org.cangjie.cfir.expressions.CfirReturnExpression
import org.cangjie.cfir.expressions.CfirStatement
import org.cangjie.cfir.expressions.CfirSubscriptExpression
import org.cangjie.cfir.expressions.CfirThrowExpression
import org.cangjie.cfir.expressions.CfirTryExpression
import org.cangjie.cfir.expressions.CfirTypeOperator

typealias CfirBasicExpressionChecker = CfirExpressionChecker<CfirStatement>
typealias CfirLiteralExpressionChecker = CfirExpressionChecker<CfirLiteralExpression>
typealias CfirFunctionCallChecker = CfirExpressionChecker<CfirFunctionCall>
typealias CfirPropertyAccessChecker = CfirExpressionChecker<CfirPropertyAccess>
typealias CfirQualifiedAccessChecker = CfirExpressionChecker<CfirQualifiedAccess>
typealias CfirAssignmentChecker = CfirExpressionChecker<CfirAssignment>
typealias CfirBinaryOpChecker = CfirExpressionChecker<CfirBinaryOp>
typealias CfirComparisonExpressionChecker = CfirExpressionChecker<CfirComparisonExpression>
typealias CfirTypeOperatorChecker = CfirExpressionChecker<CfirTypeOperator>
typealias CfirIfExpressionChecker = CfirExpressionChecker<CfirIfExpression>
typealias CfirMatchExpressionChecker = CfirExpressionChecker<CfirMatchExpression>
typealias CfirTryExpressionChecker = CfirExpressionChecker<CfirTryExpression>
typealias CfirThrowExpressionChecker = CfirExpressionChecker<CfirThrowExpression>
typealias CfirReturnExpressionChecker = CfirExpressionChecker<CfirReturnExpression>
typealias CfirJumpExpressionChecker = CfirExpressionChecker<CfirJumpExpression>
typealias CfirRangeExpressionChecker = CfirExpressionChecker<CfirRangeExpression>
typealias CfirSubscriptExpressionChecker = CfirExpressionChecker<CfirSubscriptExpression>
typealias CfirErrorExpressionChecker = CfirExpressionChecker<CfirErrorExpression>
