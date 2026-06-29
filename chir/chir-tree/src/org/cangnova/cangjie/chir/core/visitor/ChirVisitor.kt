package org.cangnova.cangjie.chir.core.visitor

import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.declaration.ChirDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirExpression
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage

/**
 * CHIR 节点访问器接口。
 */
interface ChirVisitor {
    /**
     * 访问包节点。
     */
    fun visitPackage(chirPackage: ChirPackage) {}

    /**
     * 访问模块节点。
     */
    fun visitModule(module: ChirModule) {}

    /**
     * 访问声明节点。
     */
    fun visitDeclaration(declaration: ChirDeclaration) {}

    /**
     * 访问函数声明节点。
     */
    fun visitFunction(function: ChirFunctionDeclaration) {}

    /**
     * 访问基本块节点。
     */
    fun visitBlock(block: ChirBlock) {}

    /**
     * 访问表达式节点。
     */
    fun visitExpression(expression: ChirExpression) {}
}

/**
 * 深度优先遍历 CHIR 包、模块、函数、基本块和表达式的访问器基类。
 */
open class ChirWalker : ChirVisitor {
    /**
     * 访问包并递归访问包级成员、类型定义和模块。
     */
    final override fun visitPackage(chirPackage: ChirPackage) {
        onPackage(chirPackage)
        chirPackage.members.globalVariables.forEach(::visitDeclaration)
        chirPackage.members.globalFunctions.forEach { declaration ->
            visitDeclaration(declaration)
            visitFunction(declaration)
        }
        chirPackage.members.importedVariables.forEach(::visitDeclaration)
        chirPackage.members.importedFunctions.forEach { declaration ->
            visitDeclaration(declaration)
            visitFunction(declaration)
        }
        chirPackage.typeDefinitions.forEach(::visitDeclaration)
        chirPackage.importedTypeDefinitions.forEach(::visitDeclaration)
        chirPackage.modules.forEach(::visitModule)
    }

    /**
     * 访问模块并递归访问模块声明。
     */
    final override fun visitModule(module: ChirModule) {
        onModule(module)
        module.declarations.forEach { declaration ->
            visitDeclaration(declaration)
            if (declaration is ChirFunctionDeclaration) {
                visitFunction(declaration)
            }
        }
    }

    /**
     * 访问函数并递归访问其基本块。
     */
    final override fun visitFunction(function: ChirFunctionDeclaration) {
        onFunction(function)
        function.blocks.forEach(::visitBlock)
    }

    /**
     * 访问基本块并递归访问其中表达式。
     */
    final override fun visitBlock(block: ChirBlock) {
        onBlock(block)
        block.expressions.forEach(::visitExpression)
    }

    /**
     * 访问表达式节点。
     */
    final override fun visitExpression(expression: ChirExpression) {
        onExpression(expression)
    }

    /**
     * 访问声明节点。
     */
    override fun visitDeclaration(declaration: ChirDeclaration) {
        onDeclaration(declaration)
    }

    /**
     * 包节点 hook。
     */
    protected open fun onPackage(chirPackage: ChirPackage) {}

    /**
     * 模块节点 hook。
     */
    protected open fun onModule(module: ChirModule) {}

    /**
     * 声明节点 hook。
     */
    protected open fun onDeclaration(declaration: ChirDeclaration) {}

    /**
     * 函数声明节点 hook。
     */
    protected open fun onFunction(function: ChirFunctionDeclaration) {}

    /**
     * 基本块节点 hook。
     */
    protected open fun onBlock(block: ChirBlock) {}

    /**
     * 表达式节点 hook。
     */
    protected open fun onExpression(expression: ChirExpression) {}
}
