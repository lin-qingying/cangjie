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
import org.cangnova.cangjie.cfir.analysis.checkers.context.findClosestDeclaration
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.correspondingProperty
import org.cangnova.cangjie.cfir.unwrapFakeOverridesOrDelegated
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.diagnostic.ConeDiagnosticWithSingleCandidate
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReferenceWithCandidateBase
import org.cangnova.cangjie.cfir.references.CfirResolvedErrorReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
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

/**
 * 构造器参数默认值中的成员访问合法性检查。
 *
 * 对齐官方 Cangjie `TypeCheckAccess.cpp#CheckMemberAccessInCtorParamOrCtorArg`：
 * 构造器默认参数表达式中不能读取当前对象或父对象的实例成员，因为对象初始化尚未完成。
 * 普通函数默认参数不属于该规则，保持由普通名称解析和类型检查处理。
 */
object CfirConstructorParameterThisOrSuperDefaultValueChecker : CfirValueParameterChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirValueParameter) {
        val constructor = context.closestFunctionLikeDeclaration() as? CfirConstructor ?: return
        val owner = context.findClosestDeclaration<CfirClassLikeDeclaration>()
        val defaultValue = declaration.defaultValue ?: return
        defaultValue.checkConstructorMemberAccessBeforeInitialization(
            owner = owner,
            place = ConstructorMemberAccessPlace.DEFAULT_PARAMETER_VALUE,
            constructor = constructor,
            currentParameter = declaration,
        )
    }
}

/**
 * 官方 `CheckMemberAccessInCtorParamOrCtorArg` 的 CFIR 侧入口。
 *
 * 构造器默认参数、`this(...)` 委托参数和 `super(...)` 委托参数共享同一条规则：
 * 对象完成初始化前，不能读取当前对象或父对象的实例成员；`super(...)` 只禁止父类实例成员。
 */
internal enum class ConstructorMemberAccessPlace(
    val reportThis: Boolean,
    val diagnosticContext: String,
) {
    DEFAULT_PARAMETER_VALUE(
        reportThis = true,
        diagnosticContext = "default parameter value of the constructor",
    ),
    THIS_DELEGATION_ARGUMENT(
        reportThis = true,
        diagnosticContext = "arguments of constructor call",
    ),
    SUPER_DELEGATION_ARGUMENT(
        reportThis = false,
        diagnosticContext = "arguments of constructor call",
    ),
}

context(context: CheckerContext, reporter: DiagnosticReporter)
internal fun CfirExpression.checkConstructorMemberAccessBeforeInitialization(
    owner: CfirClassLikeDeclaration?,
    place: ConstructorMemberAccessPlace,
    constructor: CfirConstructor? = null,
    currentParameter: CfirValueParameter? = null,
) {
    accept(
        ConstructorMemberAccessBeforeInitializationVisitor(
            owner = owner,
            place = place,
            constructor = constructor,
            currentParameter = currentParameter,
            context = context,
            reporter = reporter,
        )
    )
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

private class ConstructorMemberAccessBeforeInitializationVisitor(
    private val owner: CfirClassLikeDeclaration?,
    private val place: ConstructorMemberAccessPlace,
    private val constructor: CfirConstructor?,
    private val currentParameter: CfirValueParameter?,
    private val context: CheckerContext,
    private val reporter: DiagnosticReporter,
) : CfirDefaultVisitorVoid() {
    override fun visitElement(element: CfirElement) {
        if (element is CfirThisReceiverExpression && place.reportThis && !element.calleeReference.isImplicit) {
            reportIllegalReference(element, "this")
        }
        element.acceptChildren(this)
    }

    override fun visitFunctionCall(functionCall: CfirFunctionCall) {
        visitConstructorMemberAccessExpression(functionCall)
    }

    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: CfirQualifiedAccessExpression) {
        visitConstructorMemberAccessExpression(qualifiedAccessExpression)
    }

    private fun visitConstructorMemberAccessExpression(expression: CfirQualifiedAccessExpression) {
        if (expression is CfirSuperReceiverExpression) {
            reportIllegalReference(expression, "super")
            return
        }

        when (val receiver = expression.explicitReceiver) {
            is CfirSuperReceiverExpression -> {
                reportIllegalReference(receiver, "super")
                return
            }

            is CfirThisReceiverExpression -> {
                if (place.reportThis) {
                    reportIllegalReference(receiver, "this")
                } else {
                    checkInstanceMemberAccess(expression)
                }
                return
            }
        }

        checkInstanceMemberAccess(expression)
        expression.acceptChildren(this)
    }

    override fun visitFunction(function: CfirFunction) {
        if (function is CfirAnonymousFunction) {
            function.acceptChildren(this)
        }
        // 具名嵌套函数体有独立函数上下文，不属于构造前直接求值路径。
    }

    override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: CfirAnonymousFunctionExpression) {
        anonymousFunctionExpression.acceptChildren(this)
    }

    private fun checkInstanceMemberAccess(expression: CfirQualifiedAccessExpression) {
        if (expression.explicitReceiver != null && expression.explicitReceiver !is CfirThisReceiverExpression) return

        val target = expression.resolvedCallableTarget() ?: return
        val name = when (val reference = expression.calleeReference) {
            is CfirNamedReference -> reference.name.asString()
            else -> return
        }

        if (target is CfirValueParameter && target.shouldReportUninitializedMemberParameterAccess()) {
            reportIllegalMemberAccess(expression, name)
            reportCaptureHasShadowVariable(expression, target)
            return
        }

        if (!target.shouldReportConstructorMemberAccess()) return
        reportIllegalMemberAccess(expression, name)
    }

    private fun CfirQualifiedAccessExpression.resolvedCallableTarget(): CfirCallableDeclaration? {
        return when (val reference = calleeReference) {
            is CfirResolvedNamedReference -> reference.resolvedSymbol.cfir as? CfirCallableDeclaration
            is CfirResolvedErrorReference -> reference.resolvedSymbol.cfir as? CfirCallableDeclaration
            is CfirNamedReferenceWithCandidateBase -> reference.candidateSymbol?.cfir as? CfirCallableDeclaration
            is CfirErrorNamedReference ->
                (reference.diagnostic as? ConeDiagnosticWithSingleCandidate)?.candidateSymbol?.cfir as? CfirCallableDeclaration
            else -> null
        }
    }

    private fun CfirCallableDeclaration.shouldReportConstructorMemberAccess(): Boolean {
        if (this is CfirConstructor || this is CfirEnumConstructor) return false
        if (status.isStatic || dispatchReceiverType == null) return false
        val targetOwnerClassId = unwrapFakeOverridesOrDelegated().symbol.callableId.classId
            ?: symbol.callableId.classId
            ?: return false
        val ownerClassId = owner?.symbol?.classId
        if (ownerClassId == null) return place.reportThis
        return if (targetOwnerClassId == ownerClassId) place.reportThis else true
    }

    /**
     * 主构造成员参数在默认值里会先解析为 value parameter。
     * 排在当前参数之后的成员参数尚未初始化，语义上等价于读取构造中对象的成员。
     */
    private fun CfirValueParameter.shouldReportUninitializedMemberParameterAccess(): Boolean {
        if (correspondingProperty == null && !shadowsInstanceMemberInOwner()) return false
        val constructor = constructor ?: return false
        val currentParameter = currentParameter ?: return false
        val currentIndex = constructor.valueParameters.indexOf(currentParameter)
        val targetIndex = constructor.valueParameters.indexOf(this)
        return currentIndex >= 0 && targetIndex > currentIndex
    }

    private fun CfirValueParameter.shadowsInstanceMemberInOwner(): Boolean {
        val owner = owner ?: return false
        return owner.declarations.any { declaration ->
            val callable = declaration as? CfirCallableDeclaration ?: return@any false
            callable.symbol.callableId.callableName == this.name &&
                    callable.dispatchReceiverType != null &&
                    !callable.status.isStatic &&
                    callable !is CfirConstructor &&
                    callable !is CfirEnumConstructor
        }
    }

    private fun reportIllegalMemberAccess(expression: CfirQualifiedAccessExpression, memberName: String) {
        with(context) {
            reporter.reportOn(
                source = expression.calleeReference.source?.firstCharacterDiagnosticSource()
                    ?: expression.source?.firstCharacterDiagnosticSource(),
                factory = CfirErrors.ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER,
                a = memberName,
                b = place.diagnosticContext,
            )
        }
    }

    private fun reportCaptureHasShadowVariable(expression: CfirQualifiedAccessExpression, parameter: CfirValueParameter) {
        with(context) {
            reporter.reportOn(
                source = expression.calleeReference.source?.firstCharacterDiagnosticSource()
                    ?: expression.source?.firstCharacterDiagnosticSource(),
                factory = CfirErrors.CAPTURE_HAS_SHADOW_VARIABLE,
                a = parameter.name,
            )
        }
    }

    private fun reportIllegalReference(expression: CfirSuperReceiverExpression, keyword: String) {
        with(context) {
            reporter.reportOn(
                source = expression.calleeReference.source?.firstCharacterDiagnosticSource() ?: expression.source,
                factory = CfirErrors.ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER,
                a = keyword,
                b = place.diagnosticContext,
            )
        }
    }

    private fun reportIllegalReference(expression: CfirThisReceiverExpression, keyword: String) {
        with(context) {
            reporter.reportOn(
                source = expression.calleeReference.source?.firstCharacterDiagnosticSource() ?: expression.source,
                factory = CfirErrors.ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER,
                a = keyword,
                b = place.diagnosticContext,
            )
        }
    }
}

private fun CheckerContext.closestFunctionLikeDeclaration(): CfirFunction? {
    return containingDeclarations
        .asReversed()
        .firstOrNull { declaration -> declaration is CfirFunction } as? CfirFunction
}
