package org.cangnova.cangjie.cfir.analysis.collectors

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.context.MutableCheckerContext
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirStatement
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitor

/**
 * CFIR 鏍戦亶鍘嗛鏋讹紝绠＄悊 [MutableCheckerContext] 涓殑澹版槑/璇彞/鍏冪礌鏍堛€? *
 * 瀵归綈 K2 `AbstractDiagnosticCollectorVisitor`銆? *
 * 瀛愮被閫氳繃 [checkElement] 瀹炵幇鍏蜂綋鐨勬鏌ラ€昏緫銆? */
abstract class CfirAbstractDiagnosticCollectorVisitor(
    protected var context: MutableCheckerContext,
) : CfirDefaultVisitor<Unit, Nothing?>() {

    protected abstract fun checkElement(element: CfirElement)
    protected open fun onDeclarationExit(declaration: CfirDeclaration) {}
    open fun checkSettings() {}

    // --- 璁块棶鍏ュ彛 ---

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

    // --- 澹版槑閬嶅巻 ---

    protected fun visitWithDeclaration(declaration: CfirDeclaration) {
        checkElement(declaration)
        withDeclaration(declaration) {
            declaration.acceptChildren(this, null)
        }
        onDeclarationExit(declaration)
    }

    // --- 涓婁笅鏂囩鐞?---

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

