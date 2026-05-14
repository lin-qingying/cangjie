package org.cangnova.cangjie.cfir.analysis.collectors

import org.cangnova.cangjie.cfir.CfirAnnotationContainer
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContextForProvider
import org.cangnova.cangjie.cfir.analysis.checkers.context.MutableCheckerContext
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirMainFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.expressions.CfirAssignment
import org.cangnova.cangjie.cfir.expressions.CfirBreakExpression
import org.cangnova.cangjie.cfir.expressions.CfirContinueExpression
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.expressions.CfirLoopJump
import org.cangnova.cangjie.cfir.expressions.CfirNamedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirQualifiedAccessExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
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
 */
abstract class AbstractDiagnosticCollectorVisitor(
    @set:PrivateForInline var context: CheckerContextForProvider,

    ) : CfirDefaultVisitor<Unit, Nothing?>() {

    protected abstract fun checkElement(element: CfirElement)
    protected open fun onDeclarationExit(declaration: CfirDeclaration) {}
    open fun checkSettings() {}

    // --- 访问入口 ---

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

    override fun visitAnnotationContainer(annotationContainer: CfirAnnotationContainer, data: Nothing?) {
        withAnnotationContainer(annotationContainer) {
            checkElement(annotationContainer)
            visitNestedElements(annotationContainer)
        }
    }

    private fun visitJump(loopJump: CfirLoopJump) {
        withAnnotationContainer(loopJump) {
            checkElement(loopJump)
            // 对齐 Kotlin：loop jump 只在 target 是错误 loop 节点时才回访 target。
            // 当前仓颉 raw CFIR 没有独立的 CfirErrorLoop，错误 jump 诊断直接挂在 jump 自身，
            // 因此这里不能像普通节点那样重新进入 target loop，否则会把 loop body 递归回收一遍。
        }
    }

    override fun visitBreakExpression(breakExpression: CfirBreakExpression, data: Nothing?) {
        visitJump(breakExpression)
    }

    override fun visitContinueExpression(continueExpression: CfirContinueExpression, data: Nothing?) {
        visitJump(continueExpression)
    }

    override fun visitFile(file: CfirFile, data: Nothing?) {
        withAnnotationContainer(file) {
            visitWithFile(file)
        }
    }

    override fun visitDeclaration(declaration: CfirDeclaration, data: Nothing?) {
        visitWithDeclaration(declaration)
    }

    override fun visitClass(klass: CfirClass, data: Nothing?) {
        withAnnotationContainer(klass) {
            visitWithDeclaration(klass)
        }
    }

    override fun visitInterface(`interface`: CfirInterface, data: Nothing?) {
        withAnnotationContainer(`interface`) {
            visitWithDeclaration(`interface`)
        }
    }

    override fun visitStruct(struct: CfirStruct, data: Nothing?) {
        withAnnotationContainer(struct) {
            visitWithDeclaration(struct)
        }
    }

    override fun visitEnum(enum: CfirEnum, data: Nothing?) {
        withAnnotationContainer(enum) {
            visitWithDeclaration(enum)
        }
    }

    override fun visitExtend(extend: CfirExtend, data: Nothing?) {
        withAnnotationContainer(extend) {
            visitWithDeclaration(extend)
        }
    }

    override fun visitExpression(expression: CfirExpression, data: Nothing?) {
        withStatement(expression) {
            checkElement(expression)
            expression.acceptChildren(this, null)
        }
    }

    override fun visitTypeRef(typeRef: CfirTypeRef, data: Nothing?) {
        if (typeRef.source?.kind?.shouldSkipErrorTypeReporting == false) {
            withTypeRefAnnotationContainer(typeRef) {
                checkElement(typeRef)
                visitNestedElements(typeRef)
            }
        }
    }

    override fun visitErrorTypeRef(errorTypeRef: CfirErrorTypeRef, data: Nothing?) {
        visitResolvedTypeRef(errorTypeRef, data)
    }

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

    override fun visitFunctionCall(functionCall: CfirFunctionCall, data: Nothing?) {
        visitWithCallOrAssignment(functionCall)
    }

    override fun visitQualifiedAccessExpression(qualifiedAccessExpression: CfirQualifiedAccessExpression, data: Nothing?) {
        visitWithCallOrAssignment(qualifiedAccessExpression)
    }

    override fun visitNamedAccessExpression(namedAccessExpression: CfirNamedAccessExpression, data: Nothing?) {
        visitWithCallOrAssignment(namedAccessExpression)
    }

    override fun visitAssignment(assignment: CfirAssignment, data: Nothing?) {
        visitWithCallOrAssignment(assignment)
    }

    private fun visitWithCallOrAssignment(callOrAssignment: CfirStatement) {
        withCallOrAssignment(callOrAssignment) {
            visitElement(callOrAssignment, null)
        }
    }

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

    override fun visitMainFunction(mainFunction: CfirMainFunction, data: Nothing?) {
        withAnnotationContainer(mainFunction) {
            visitWithDeclaration(mainFunction)
        }
    }

    override fun visitMacroDeclaration(macroDeclaration: CfirMacroDeclaration, data: Nothing?) {
        withAnnotationContainer(macroDeclaration) {
            visitWithDeclaration(macroDeclaration)
        }
    }
    override fun visitNamedFunction(namedFunction: CfirNamedFunction, data: Nothing?) {
        withAnnotationContainer(namedFunction) {
            visitWithDeclaration(namedFunction)
        }
    }

    override fun visitConstructor(constructor: CfirConstructor, data: Nothing?) {
        withAnnotationContainer(constructor) {
            visitWithDeclaration(constructor)
        }
    }

    // --- 声明遍历 ---
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
    protected open fun shouldVisitDeclaration(declaration: CfirDeclaration): Boolean = true
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

    protected inline fun <R> withDeclaration(decl: CfirDeclaration, block: () -> R): R {
        context.addDeclaration(decl)
        try {
            return block()
        } finally {
            context.dropDeclaration()
        }
    }

    protected inline fun <R> withStatement(stmt: CfirStatement, block: () -> R): R {
        context.addStatement(stmt)
        try {
            return block()
        } finally {
            context.dropStatement()
        }
    }

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

    protected open fun visitNestedElements(element: CfirElement) {
        element.acceptChildren(this, null)
    }

    protected inline fun visitWithFile(
        file: CfirFile,
        block: () -> Unit = { visitNestedElements(file) }
    ) {
        withFile(file) {
            visitWithDeclaration(file, block)
        }
    }
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
