

package org.cangnova.cangjie.cfir.analysis.checkers.expression

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOp
import org.cangnova.cangjie.cfir.expressions.CfirComparisonExpression
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirIfExpression
import org.cangnova.cangjie.cfir.expressions.CfirJumpExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.expressions.CfirPropertyAccess
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccess
import org.cangnova.cangjie.cfir.expressions.CfirRangeExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirSubscriptExpression
import org.cangnova.cangjie.cfir.expressions.CfirThrowExpression
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression
import org.cangnova.cangjie.cfir.expressions.CfirTypeOperator

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
