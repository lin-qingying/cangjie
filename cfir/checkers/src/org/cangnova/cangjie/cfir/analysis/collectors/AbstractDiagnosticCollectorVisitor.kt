package org.cangnova.cangjie.cfir.analysis.collectors

import org.cangnova.cangjie.cfir.CfirAnnotationContainer
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContextForProvider
import org.cangnova.cangjie.cfir.analysis.checkers.context.MutableCheckerContext
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirMainFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
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

    override fun visitExpression(expression: CfirExpression, data: Nothing?) {
        withStatement(expression) {
            checkElement(expression)
            expression.acceptChildren(this, null)
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
