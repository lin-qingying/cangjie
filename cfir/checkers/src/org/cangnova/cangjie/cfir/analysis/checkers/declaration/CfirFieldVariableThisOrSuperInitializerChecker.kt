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
import org.cangnova.cangjie.cfir.declarations.CfirStruct
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
 * - static 字段初始化器中显式 `this` 与 `super.member` / `super.call` 非法；
 * - 非 static 字段初始化器中 `super.member` / `super.call` 与裸 `this` 非法；
 * - 裸 `super` 只由 `ILLEGAL_SUPER_ALONE` 规则报告，不叠加 initializer 诊断；
 * - class 字段初始化器中的 `this.member` 不由本规则报告；
 * - struct 字段初始化器中的 `this.member` 按官方 struct-this 规则报告。
 *
 * Kotlin 没有同语义规则，但成员属性初始化相关诊断由 `FirMemberPropertiesChecker`
 * 这样的声明检查入口统一处理；本 checker 因此挂在 `CfirFieldVariableChecker`。
 */
object CfirFieldVariableThisOrSuperInitializerChecker : CfirFieldVariableChecker() {
    /**
     * 检查字段初始化器中的显式 this/super 引用。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirFieldVariable) {
        val initializer = declaration.initializer ?: return
        val owner = context.findClosestDeclaration<CfirClassLikeDeclaration>()
        initializer.accept(FieldInitializerReferenceVisitor(declaration, owner, context, reporter))
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
    /**
     * 检查构造器参数默认值中的构造前成员访问。
     */
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
    /**
     * 当前场景是否需要报告 `this` 相关访问。
     */
    val reportThis: Boolean,

    /**
     * 诊断消息中展示的构造器求值位置描述。
     */
    val diagnosticContext: String,
) {
    /**
     * 构造器默认参数表达式。
     */
    DEFAULT_PARAMETER_VALUE(
        reportThis = true,
        diagnosticContext = "default parameter value of the constructor",
    ),

    /**
     * `this(...)` 构造器委托实参表达式。
     */
    THIS_DELEGATION_ARGUMENT(
        reportThis = true,
        diagnosticContext = "arguments of constructor call",
    ),

    /**
     * `super(...)` 构造器委托实参表达式。
     */
    SUPER_DELEGATION_ARGUMENT(
        reportThis = false,
        diagnosticContext = "arguments of constructor call",
    ),
}

/**
 * 在构造器初始化完成前访问检查规则下扫描表达式。
 *
 * 调用方传入具体场景、当前构造器和当前参数后，visitor 会统一报告 this/super 引用、
 * 实例成员访问以及成员参数 shadow 捕获问题。
 */
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

/**
 * 字段初始化器中的 this/super 引用 visitor。
 *
 * 该 visitor 只处理字段初始化器的直接求值路径，嵌套函数和匿名函数体不属于当前初始化器语义。
 */
private class FieldInitializerReferenceVisitor(
    /**
     * 当前正在检查的字段声明。
     */
    private val field: CfirFieldVariable,

    /**
     * 当前字段所在的 class-like 声明，用于区分 struct 字段初始化器中的 `this.member`。
     */
    private val owner: CfirClassLikeDeclaration?,

    /**
     * 当前 checker 上下文。
     */
    private val context: CheckerContext,

    /**
     * 诊断报告器。
     */
    private val reporter: DiagnosticReporter,
) : CfirDefaultVisitorVoid() {
    /**
     * 当前遍历路径上的 qualified access 栈，用于判断 `this` 是否只是成员访问 receiver。
     */
    private val qualifiedAccessStack = ArrayDeque<CfirQualifiedAccessExpression>()

    /**
     * 默认元素访问：检查显式 this 并继续遍历子节点。
     */
    override fun visitElement(element: CfirElement) {
        if (element is CfirThisReceiverExpression) {
            checkThisReceiver(element)
        }
        element.acceptChildren(this)
    }

    /**
     * 访问 qualified access 并维护 receiver 判断所需的访问栈。
     */
    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: CfirQualifiedAccessExpression) {
        if (qualifiedAccessExpression is CfirSuperReceiverExpression && qualifiedAccessExpression.isReceiverOfQualifiedAccess()) {
            reportInitializerReference(qualifiedAccessExpression, "super")
        }

        qualifiedAccessStack.addLast(qualifiedAccessExpression)
        try {
            qualifiedAccessExpression.acceptChildren(this)
        } finally {
            qualifiedAccessStack.removeLast()
        }
    }

    /**
     * 字段初始化器中的 `this()` / `super()` 由调用级 checker 归类为 outside-ctor 诊断；
     * 本 visitor 只继续扫描实参，避免把 callee receiver 再归入 initializer 引用规则。
     */
    override fun visitFunctionCall(functionCall: CfirFunctionCall) {
        if (functionCall.constructorDelegationKindOrNull() != null) {
            functionCall.argumentList.accept(this)
            return
        }

        visitQualifiedAccessExpression(functionCall)
    }

    /**
     * 跳过字段初始化器内的具名函数体。
     */
    override fun visitFunction(function: CfirFunction) {
        // 字段初始化器内的嵌套函数体有自己的函数上下文，官方规则中不属于 initializer 引用。
    }

    /**
     * 跳过字段初始化器内的匿名函数表达式 body。
     */
    override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: CfirAnonymousFunctionExpression) {
        // lambda/匿名函数体在官方 `curFuncBody` 语义下不属于字段初始化器直接引用。
    }

    /**
     * 按字段 static 状态检查显式 this receiver。
     */
    private fun checkThisReceiver(expression: CfirThisReceiverExpression) {
        if (expression.calleeReference.isImplicit) return
        if (!field.status.isStatic && owner is CfirStruct) {
            reportIllegalThisOutsideStructConstructor(expression)
            return
        }
        if (!field.status.isStatic && expression.isReceiverOfQualifiedAccess()) return
        reportInitializerReference(expression, "this")
    }

    /**
     * 判断表达式是否作为当前访问栈中某个 qualified access 的 receiver。
     */
    private fun CfirExpression.isReceiverOfQualifiedAccess(): Boolean {
        return qualifiedAccessStack.any { parent ->
            parent.explicitReceiver === this || parent.dispatchReceiver === this
        }
    }

    /**
     * 报告字段初始化器中的非法 super 引用。
     */
    private fun reportInitializerReference(expression: CfirSuperReceiverExpression, keyword: String) {
        with(context) {
            reporter.reportOn(
                source = expression.calleeReference.source ?: expression.source,
                factory = initializerReferenceDiagnosticFactory(),
                a = keyword,
            )
        }
    }

    /**
     * 报告字段初始化器中的非法 this 引用。
     */
    private fun reportInitializerReference(expression: CfirThisReceiverExpression, keyword: String) {
        with(context) {
            reporter.reportOn(
                source = expression.calleeReference.source ?: expression.source,
                factory = initializerReferenceDiagnosticFactory(),
                a = keyword,
            )
        }
    }

    /**
     * struct 字段初始化器中的 `this.member` 对齐官方 struct-this 规则，不走初始化读诊断。
     */
    private fun reportIllegalThisOutsideStructConstructor(expression: CfirThisReceiverExpression) {
        with(context) {
            reporter.reportOn(
                source = expression.calleeReference.source ?: expression.source,
                factory = CfirErrors.ILLEGAL_THIS_OUTSIDE_STRUCT_CONSTRUCTOR,
            )
        }
    }

    /**
     * static 与非 static 字段初始化器使用官方区分的诊断名。
     */
    private fun initializerReferenceDiagnosticFactory() =
        if (field.status.isStatic) {
            CfirErrors.THIS_OR_SUPER_NOT_ALLOWED_TO_INITIALIZE_STATIC_MEMBER
        } else {
            CfirErrors.THIS_OR_SUPER_NOT_ALLOWED_TO_INITIALIZE_NON_STATIC_MEMBER
        }
}

/**
 * 构造器默认参数与委托实参中的构造前成员访问 visitor。
 *
 * visitor 按场景区分是否报告当前对象 `this`，并对解析到的实例成员、父类成员和后序
 * 主构造成员参数进行统一诊断。
 */
private class ConstructorMemberAccessBeforeInitializationVisitor(
    /**
     * 当前构造器所属的 class-like 声明。
     */
    private val owner: CfirClassLikeDeclaration?,

    /**
     * 当前检查场景。
     */
    private val place: ConstructorMemberAccessPlace,

    /**
     * 当前构造器声明；默认参数检查需要用它判断参数顺序。
     */
    private val constructor: CfirConstructor?,

    /**
     * 当前正在检查默认值的构造器参数。
     */
    private val currentParameter: CfirValueParameter?,

    /**
     * 当前 checker 上下文。
     */
    private val context: CheckerContext,

    /**
     * 诊断报告器。
     */
    private val reporter: DiagnosticReporter,
) : CfirDefaultVisitorVoid() {
    /**
     * 默认元素访问：在需要时报告显式 this，并继续遍历子节点。
     */
    override fun visitElement(element: CfirElement) {
        if (element is CfirThisReceiverExpression && place.reportThis && !element.calleeReference.isImplicit) {
            reportIllegalReference(element, "this")
        }
        element.acceptChildren(this)
    }

    /**
     * 函数调用也可能是成员访问，因此进入构造前成员访问检查。
     */
    override fun visitFunctionCall(functionCall: CfirFunctionCall) {
        visitConstructorMemberAccessExpression(functionCall)
    }

    /**
     * 检查普通 qualified access 的构造前成员访问。
     */
    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: CfirQualifiedAccessExpression) {
        visitConstructorMemberAccessExpression(qualifiedAccessExpression)
    }

    /**
     * 检查单个 qualified access 是否触发 this/super 或实例成员访问限制。
     */
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

    /**
     * 只遍历匿名函数，跳过具名函数体的独立求值上下文。
     */
    override fun visitFunction(function: CfirFunction) {
        if (function is CfirAnonymousFunction) {
            function.acceptChildren(this)
        }
        // 具名嵌套函数体有独立函数上下文，不属于构造前直接求值路径。
    }

    /**
     * 匿名函数表达式仍属于当前表达式树，继续遍历其子节点。
     */
    override fun visitAnonymousFunctionExpression(anonymousFunctionExpression: CfirAnonymousFunctionExpression) {
        anonymousFunctionExpression.acceptChildren(this)
    }

    /**
     * 检查表达式解析到的实例成员或后序成员参数访问。
     */
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

    /**
     * 解析 qualified access 的 callable 目标声明。
     *
     * 兼容成功引用、错误恢复引用以及带单候选诊断的错误引用，确保语义检查不会因为
     * 前面阶段产生可恢复错误而丢失构造前访问诊断。
     */
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

    /**
     * 判断 callable 声明是否应作为构造前实例成员访问报告。
     */
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

    /**
     * 判断构造器参数名是否遮蔽当前 owner 中的实例成员。
     */
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

    /**
     * 报告构造前读取实例成员的诊断。
     */
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

    /**
     * 报告构造器参数默认值捕获到后序 shadow 成员参数的问题。
     */
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

    /**
     * 报告构造前非法 super 引用。
     */
    private fun reportIllegalReference(expression: CfirSuperReceiverExpression, keyword: String) {
        with(context) {
            reporter.reportOn(
                source = expression.calleeReference.source ?: expression.source,
                factory = CfirErrors.ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER,
                a = keyword,
                b = place.diagnosticContext,
            )
        }
    }

    /**
     * 报告构造前非法 this 引用。
     */
    private fun reportIllegalReference(expression: CfirThisReceiverExpression, keyword: String) {
        with(context) {
            reporter.reportOn(
                source = expression.calleeReference.source ?: expression.source,
                factory = CfirErrors.ASSIGNMENT_OF_MEMBER_VARIABLE_CANNOT_USE_THIS_OR_SUPER,
                a = keyword,
                b = place.diagnosticContext,
            )
        }
    }
}

/**
 * 取得当前 checker 上下文中最近的函数类声明。
 */
private fun CheckerContext.closestFunctionLikeDeclaration(): CfirFunction? {
    return findClosestDeclaration<CfirFunction>()
}
