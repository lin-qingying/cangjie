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

package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid

/**
 * 字段初始化器中的 `this` / `super` 引用合法性检查。
 *
 * 对齐官方 Cangjie `TypeCheckReference.cpp#CheckThisOrSuperInInitializer`：
 * - 当前检查节点是 `VAR_DECL` 时才应用本规则；
 * - static 字段初始化器中显式 `this` / `super` 都非法；
 * - 非 static 字段初始化器中 `super` 与裸 `this` 非法；
 * - 非 static 字段初始化器中的 `this.member` 不由本规则报告。
 *
 * Kotlin 没有同语义规则，但成员属性初始化相关诊断由 `FirMemberPropertiesChecker`
 * 这样的声明检查入口统一处理；本 checker 因此挂在 `CfirFieldVariableChecker`。
 */
object CfirFieldVariableThisOrSuperInitializerChecker : CfirFieldVariableChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFieldVariable) {
        val initializer = declaration.initializer ?: return
        initializer.accept(FieldInitializerReferenceVisitor(declaration, context, reporter))
    }
}

private class FieldInitializerReferenceVisitor(
    private val field: CfirFieldVariable,
    private val context: CheckerContext,
    private val reporter: DiagnosticReporter,
) : CfirDefaultVisitorVoid() {
    private val qualifiedAccessStack = ArrayDeque<CfirQualifiedAccessExpression>()

    override fun visitElement(element: CfirElement) {
        if (element is CfirThisReceiverExpression) {
            checkThisReceiver(element)
        }
        element.acceptChildren(this)
    }

    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: CfirQualifiedAccessExpression) {
        if (qualifiedAccessExpression is CfirSuperReceiverExpression) {
            reportIllegalReference(qualifiedAccessExpression, "super")
        }

        qualifiedAccessStack.addLast(qualifiedAccessExpression)
        try {
            qualifiedAccessExpression.acceptChildren(this)
        } finally {
            qualifiedAccessStack.removeLast()
        }
    }

    override fun visitFunction(function: CfirFunction) {
        // 字段初始化器内的嵌套函数体有自己的函数上下文，官方规则中不属于 initializer 引用。
    }

    override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: CfirAnonymousFunctionExpression) {
        // lambda/匿名函数体在官方 `curFuncBody` 语义下不属于字段初始化器直接引用。
    }

    private fun checkThisReceiver(expression: CfirThisReceiverExpression) {
        if (expression.calleeReference.isImplicit) return
        if (!field.status.isStatic && expression.isReceiverOfQualifiedAccess()) return
        reportIllegalReference(expression, "this")
    }

    private fun CfirExpression.isReceiverOfQualifiedAccess(): Boolean {
        return qualifiedAccessStack.any { parent ->
            parent.explicitReceiver === this || parent.dispatchReceiver === this
        }
    }

    private fun reportIllegalReference(expression: CfirSuperReceiverExpression, keyword: String) {
        with(context) {
            reporter.reportOn(
                source = expression.calleeReference.source ?: expression.source,
                factory = CfirErrors.ILLEGAL_THIS_OR_SUPER_CALL,
                a = keyword,
            )
        }
    }

    private fun reportIllegalReference(expression: CfirThisReceiverExpression, keyword: String) {
        with(context) {
            reporter.reportOn(
                source = expression.calleeReference.source ?: expression.source,
                factory = CfirErrors.ILLEGAL_THIS_OR_SUPER_CALL,
                a = keyword,
            )
        }
    }
}
