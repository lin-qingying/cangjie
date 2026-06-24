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

package org.cangnova.cangjie.cfir.analysis.collectors

import org.cangnova.cangjie.cfir.CfirAnnotationContainer
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContextForProvider
import org.cangnova.cangjie.cfir.analysis.checkers.context.MutableCheckerContext
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitor
import org.cangnova.cangjie.cfir.whileAnalysing
import org.cangnova.cangjie.util.PrivateForInline

/**
 * CFIR 树遍历骨架，负责管理 [MutableCheckerContext] 中的声明、语句和元素栈。
 * 对齐 K2 `AbstractDiagnosticCollectorVisitor`。
 * 子类通过 [checkElement] 实现具体的检查逻辑。
 *
 * @property context 当前遍历过程中的 checker context，可在进入声明、语句、元素时更新。
 */
abstract class AbstractDiagnosticCollectorVisitor(
    @set:PrivateForInline var context: CheckerContextForProvider,

    ) : CfirDefaultVisitor<Unit, Nothing?>() {

    /** 对当前访问到的 CFIR 元素执行实际 checker 分发。 */
    protected abstract fun checkElement(element: CfirElement)

    /** 在声明节点及其子树遍历完成后触发的扩展点。 */
    protected open fun onDeclarationExit(declaration: CfirDeclaration) {}

    /** 在遍历具体 CFIR 声明前执行的全局设置检查入口。 */
    open fun checkSettings() {}

    // --- 访问入口 ---

    /** 访问普通 CFIR 元素，并根据元素是否支持注解容器选择对应上下文栈。 */
    override fun visitElement(element: CfirElement, data: Nothing?) {
        when (element) {
            is CfirAnnotationContainer -> withAnnotationContainer(element) {
                checkElement(element)
                visitNestedElements(element)
            }
            else -> withElement(element) {
                checkElement(element)
                visitNestedElements(element)
            }
        }
    }

    /** 访问显式注解容器元素，并在注解容器上下文中继续遍历其子节点。 */
    override fun visitAnnotationContainer(annotationContainer: CfirAnnotationContainer, data: Nothing?) {
        withAnnotationContainer(annotationContainer) {
            checkElement(annotationContainer)
            visitNestedElements(annotationContainer)
        }
    }

    /** 访问 break/continue 这类 loop jump 节点，避免重复进入目标 loop body。 */
    private fun visitJump(loopJump: CfirLoopJump) {
        withAnnotationContainer(loopJump) {
            checkElement(loopJump)
            // 对齐 Kotlin：loop jump 只在 target 是错误 loop 节点时才回访 target。
            // 当前仓颉 raw CFIR 没有独立的 CfirErrorLoop，错误 jump 诊断直接挂在 jump 自身，
            // 因此这里不能像普通节点那样重新进入 target loop，否则会把 loop body 递归回收一遍。
        }
    }

    /** 访问 break 表达式并按 loop jump 规则处理。 */
    override fun visitBreakExpression(breakExpression: CfirBreakExpression, data: Nothing?) {
        visitJump(breakExpression)
    }

    /** 访问 continue 表达式并按 loop jump 规则处理。 */
    override fun visitContinueExpression(continueExpression: CfirContinueExpression, data: Nothing?) {
        visitJump(continueExpression)
    }

    /** 访问文件节点，并建立文件级注解容器和文件上下文。 */
    override fun visitFile(file: CfirFile, data: Nothing?) {
        withAnnotationContainer(file) {
            visitWithFile(file)
        }
    }

    /** 访问普通声明节点。 */
    override fun visitDeclaration(declaration: CfirDeclaration, data: Nothing?) {
        visitWithDeclaration(declaration)
    }

    /** 访问成员声明节点，并保留其注解容器上下文。 */
    override fun visitMemberDeclaration(memberDeclaration: CfirMemberDeclaration, data: Nothing?) {
        withAnnotationContainer(memberDeclaration) {
            visitWithDeclaration(memberDeclaration)
        }
    }

    /** 访问变量声明节点，并保留其注解容器上下文。 */
    override fun visitVariable(variable: CfirVariable, data: Nothing?) {
        withAnnotationContainer(variable) {
            visitWithDeclaration(variable)
        }
    }

    /** 访问值参数声明节点，并保留其注解容器上下文。 */
    override fun visitValueParameter(valueParameter: CfirValueParameter, data: Nothing?) {
        withAnnotationContainer(valueParameter) {
            visitWithDeclaration(valueParameter)
        }
    }

    /** 访问类型参数声明节点，并保留其注解容器上下文。 */
    override fun visitTypeParameter(typeParameter: CfirTypeParameter, data: Nothing?) {
        withAnnotationContainer(typeParameter) {
            visitWithDeclaration(typeParameter)
        }
    }

    /** 访问 class 声明节点，并保留其注解容器上下文。 */
    override fun visitClass(klass: CfirClass, data: Nothing?) {
        withAnnotationContainer(klass) {
            visitWithDeclaration(klass)
        }
    }

    /** 访问 interface 声明节点，并保留其注解容器上下文。 */
    override fun visitInterface(`interface`: CfirInterface, data: Nothing?) {
        withAnnotationContainer(`interface`) {
            visitWithDeclaration(`interface`)
        }
    }

    /** 访问 struct 声明节点，并保留其注解容器上下文。 */
    override fun visitStruct(struct: CfirStruct, data: Nothing?) {
        withAnnotationContainer(struct) {
            visitWithDeclaration(struct)
        }
    }

    /** 访问 enum 声明节点，并保留其注解容器上下文。 */
    override fun visitEnum(enum: CfirEnum, data: Nothing?) {
        withAnnotationContainer(enum) {
            visitWithDeclaration(enum)
        }
    }

    /** 访问 extend 声明节点，并保留其注解容器上下文。 */
    override fun visitExtend(extend: CfirExtend, data: Nothing?) {
        withAnnotationContainer(extend) {
            visitWithDeclaration(extend)
        }
    }

    /** 访问表达式节点，并把表达式压入 statement 上下文栈。 */
    override fun visitExpression(expression: CfirExpression, data: Nothing?) {
        withStatement(expression) {
            checkElement(expression)
            expression.acceptChildren(this, null)
        }
    }

    /** 访问注解调用，并按调用/赋值上下文处理。 */
    override fun visitAnnotationCall(annotationCall: CfirAnnotationCall, data: Nothing?) {
        visitWithCallOrAssignment(annotationCall)
    }

    /** 访问未解析或普通类型引用，并在允许报告错误类型时进入类型引用注解容器。 */
    override fun visitTypeRef(typeRef: CfirTypeRef, data: Nothing?) {
        if (typeRef.source?.kind?.shouldSkipErrorTypeReporting == false) {
            withTypeRefAnnotationContainer(typeRef) {
                checkElement(typeRef)
                visitNestedElements(typeRef)
            }
        }
    }

    /** 访问错误类型引用，复用已解析类型引用的遍历逻辑。 */
    override fun visitErrorTypeRef(errorTypeRef: CfirErrorTypeRef, data: Nothing?) {
        visitResolvedTypeRef(errorTypeRef, data)
    }

    /** 访问已解析类型引用，并沿 delegated type-ref 链继续遍历原始类型引用。 */
    override fun visitResolvedTypeRef(resolvedTypeRef: CfirResolvedTypeRef, data: Nothing?) {
        val resolvedTypeRefType = resolvedTypeRef.coneType
        if (resolvedTypeRefType is ConeErrorType) {
            visitTypeRef(resolvedTypeRef, data)
        }
        if (resolvedTypeRef.source?.kind?.shouldSkipErrorTypeReporting == true) return
        withTypeRefAnnotationContainer(resolvedTypeRef) {
            if (resolvedTypeRefType !is ConeErrorType) {
                checkElement(resolvedTypeRef)
            }
            resolvedTypeRef.delegatedTypeRef?.accept(this, data)
        }
    }

    /** 访问函数调用表达式，并按调用/赋值上下文处理。 */
    override fun visitFunctionCall(functionCall: CfirFunctionCall, data: Nothing?) {
        visitWithCallOrAssignment(functionCall)
    }

    /** 访问限定访问表达式，并按调用/赋值上下文处理。 */
    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: CfirQualifiedAccessExpression, data: Nothing?) {
        visitWithCallOrAssignment(qualifiedAccessExpression)
    }

    /** 访问命名访问表达式，并按调用/赋值上下文处理。 */
    override fun visitNamedAccessExpression(namedAccessExpression: CfirNamedAccessExpression, data: Nothing?) {
        visitWithCallOrAssignment(namedAccessExpression)
    }

    /** 访问赋值表达式，并按调用/赋值上下文处理。 */
    override fun visitAssignment(assignment: CfirAssignment, data: Nothing?) {
        visitWithCallOrAssignment(assignment)
    }

    /** 在调用/赋值上下文中访问一个 statement。 */
    private fun visitWithCallOrAssignment(callOrAssignment: CfirStatement) {
        withCallOrAssignment(callOrAssignment) {
            visitElement(callOrAssignment, null)
        }
    }

    /** 临时把调用或赋值节点压入 context，供 checker 查询最近调用/赋值上下文。 */
    @OptIn(PrivateForInline::class)
    inline fun <R> withCallOrAssignment(callOrAssignment: CfirStatement, block: () -> R): R {
        val existingContext = context
        context = context.addCallOrAssignment(callOrAssignment)
        try {
            return block()
        } finally {
            existingContext.dropCallOrAssignment()
            context = existingContext
        }
    }

    /** 访问 main 函数声明，并保留其注解容器上下文。 */
    override fun visitMainFunction(mainFunction: CfirMainFunction, data: Nothing?) {
        withAnnotationContainer(mainFunction) {
            visitWithDeclaration(mainFunction)
        }
    }

    /** 访问 macro 声明，并保留其注解容器上下文。 */
    override fun visitMacroDeclaration(macroDeclaration: CfirMacroDeclaration, data: Nothing?) {
        withAnnotationContainer(macroDeclaration) {
            visitWithDeclaration(macroDeclaration)
        }
    }

    /** 访问 finalizer 声明，并保留其注解容器上下文。 */
    override fun visitFinalizer(finalizer: CfirFinalizer, data: Nothing?) {
        withAnnotationContainer(finalizer) {
            visitWithDeclaration(finalizer)
        }
    }

    /** 访问普通命名函数声明，并保留其注解容器上下文。 */
    override fun visitNamedFunction(namedFunction: CfirNamedFunction, data: Nothing?) {
        withAnnotationContainer(namedFunction) {
            visitWithDeclaration(namedFunction)
        }
    }

    /** 访问构造器声明，并保留其注解容器上下文。 */
    override fun visitConstructor(constructor: CfirConstructor, data: Nothing?) {
        withAnnotationContainer(constructor) {
            visitWithDeclaration(constructor)
        }
    }

    // --- 声明遍历 ---
    /** 将注解容器中的 suppression 信息加入 context；当前实现保留接入点。 */
    @OptIn(PrivateForInline::class)
    fun addSuppressedDiagnosticsToContext(annotationContainer: CfirAnnotationContainer) {
//        val arguments = AbstractDiagnosticCollector.getDiagnosticsSuppressedForContainer(annotationContainer) ?: return
//        context = context.addSuppressedDiagnostics(
//            arguments,
//            allInfosSuppressed = AbstractDiagnosticCollector.SUPPRESS_ALL_INFOS in arguments,
//            allWarningsSuppressed = AbstractDiagnosticCollector.SUPPRESS_ALL_WARNINGS in arguments,
//            allErrorsSuppressed = AbstractDiagnosticCollector.SUPPRESS_ALL_ERRORS in arguments
//        )
    }

    /** 判断当前声明是否需要进入诊断遍历。 */
    protected open fun shouldVisitDeclaration(declaration: CfirDeclaration): Boolean = true

    /** 在注解容器上下文中执行代码块，并维护 annotation container 栈。 */
    @OptIn(PrivateForInline::class)
    inline fun <R> withAnnotationContainer(annotationContainer: CfirAnnotationContainer, block: () -> R): R {
        return withElement(annotationContainer) {
            val existingContext = context
            addSuppressedDiagnosticsToContext(annotationContainer)
            val notEmptyAnnotations = annotationContainer.annotations.isNotEmpty()
            if (notEmptyAnnotations) {
                context = context.addAnnotationContainer(annotationContainer)
            }
            try {
                block()
            } finally {
                if (notEmptyAnnotations) {
                    existingContext.dropAnnotationContainer()
                }
                context = existingContext
            }
        }
    }

    /** 访问声明节点、进入声明上下文、遍历子节点，并在退出时触发声明结束回调。 */
    protected inline fun visitWithDeclaration(
        declaration: CfirDeclaration,
        block: () -> Unit = { visitNestedElements(declaration) }
    ) {
        if (shouldVisitDeclaration(declaration)) {
            checkElement(declaration)
            withDeclaration(declaration) {
                block()
            }
            onDeclarationExit(declaration)
        }
    }
    // --- 上下文管理 ---

    /** 临时把声明压入 context 声明栈并执行代码块。 */
    protected inline fun <R> withDeclaration(decl: CfirDeclaration, block: () -> R): R {
        context.addDeclaration(decl)
        try {
            return block()
        } finally {
            context.dropDeclaration()
        }
    }

    /** 临时把语句压入 context 语句栈并执行代码块。 */
    protected inline fun <R> withStatement(stmt: CfirStatement, block: () -> R): R {
        context.addStatement(stmt)
        try {
            return block()
        } finally {
            context.dropStatement()
        }
    }

    /** 处理 resolved/delegated 类型引用嵌套场景，避免重复压入已经处于注解容器链中的 type-ref。 */
    private inline fun <R> withTypeRefAnnotationContainer(annotationContainer: CfirTypeRef, block: () -> R): R {
        var containingTypeRef = context.annotationContainers.lastOrNull() as? CfirResolvedTypeRef
        while (containingTypeRef != null && containingTypeRef.delegatedTypeRef != annotationContainer) {
            containingTypeRef = containingTypeRef.delegatedTypeRef as? CfirResolvedTypeRef
        }
        return if (containingTypeRef != null) {
            block()
        } else {
            withAnnotationContainer(annotationContainer, block)
        }
    }

    /** 遍历一个元素的所有子节点。 */
    protected open fun visitNestedElements(element: CfirElement) {
        element.acceptChildren(this, null)
    }

    /** 在文件上下文中访问文件声明及其子树。 */
    protected inline fun visitWithFile(
        file: CfirFile,
        block: () -> Unit = { visitNestedElements(file) }
    ) {
        withFile(file) {
            visitWithDeclaration(file, block)
        }
    }

    /** 临时进入文件上下文并执行代码块。 */
    @OptIn(PrivateForInline::class)
    inline fun <R> withFile(file: CfirFile, block: () -> R): R {
        val existingContext = context
        context = context.enterFile(file)
        try {
            return block()
        } finally {
            existingContext.exitFile(file)
            context = existingContext
        }
    }

    /** 临时把任意 CFIR 元素压入 context 元素栈，并在 session 的分析保护块中执行代码。 */
    @OptIn(PrivateForInline::class)
    inline fun <T> withElement(element:CfirElement, block: () -> T): T {
        val existingContext = context
        context = context.addElement(element)
        return try {
            whileAnalysing(context.session, element) {
                block()
            }
        } finally {
            existingContext.dropElement()
            context = existingContext
        }
    }

}
