package org.cangnova.cangjie.cfir.analysis.collectors

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.MutableCheckerContext
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitor

/**
 * CFIR 树遍历骨架，负责管理 [MutableCheckerContext] 中的声明、语句和元素栈。
 * 对齐 K2 `AbstractDiagnosticCollectorVisitor`。
 * 子类通过 [checkElement] 实现具体的检查逻辑。
 */
abstract class CfirAbstractDiagnosticCollectorVisitor(
    protected var context: MutableCheckerContext,
) : CfirDefaultVisitor<Unit, Nothing?>() {

    protected abstract fun checkElement(element: CfirElement)
    protected open fun onDeclarationExit(declaration: CfirDeclaration) {}
    open fun checkSettings() {}

    // --- 访问入口 ---

    override fun visitElement(element: CfirElement, data: Nothing?) {
        withElement(element) {
            checkElement(element)
            element.acceptChildren(this, null)
        }
    }

    override fun visitFile(file: CfirFile, data: Nothing?) {
        visitWithDeclaration(file)
    }

    override fun visitDeclaration(declaration: CfirDeclaration, data: Nothing?) {
        visitWithDeclaration(declaration)
    }

    override fun visitExpression(expression: CfirExpression, data: Nothing?) {
        withStatement(expression) {
            checkElement(expression)
            expression.acceptChildren(this, null)
        }
    }

    // --- 声明遍历 ---

    protected fun visitWithDeclaration(declaration: CfirDeclaration) {
        checkElement(declaration)
        withDeclaration(declaration) {
            declaration.acceptChildren(this, null)
        }
        onDeclarationExit(declaration)
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

    protected inline fun <R> withElement(elem: CfirElement, block: () -> R): R {
        context.addElement(elem)
        try {
            return block()
        } finally {
            context.dropElement()
        }
    }
}

