

package org.cangnova.cangjie.cfir.analysis.checkers.expression

/*
 * 本文件由生成器自动生成
 * 请勿手动修改
 */

import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirBinaryOp
import org.cangnova.cangjie.cfir.expressions.CfirComparisonExpression
import org.cangnova.cangjie.cfir.expressions.CfirErrorExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirIfExpression
import org.cangnova.cangjie.cfir.expressions.CfirIncrementDecrementExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirLoopJump
import org.cangnova.cangjie.cfir.expressions.CfirMatchExpression
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirRangeExpression
import org.cangnova.cangjie.cfir.expressions.CfirReturnExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.expressions.CfirSubscriptExpression
import org.cangnova.cangjie.cfir.expressions.CfirSuperReceiverExpression
import org.cangnova.cangjie.cfir.expressions.CfirThrowExpression
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression
import org.cangnova.cangjie.cfir.expressions.CfirTypeOperator

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
