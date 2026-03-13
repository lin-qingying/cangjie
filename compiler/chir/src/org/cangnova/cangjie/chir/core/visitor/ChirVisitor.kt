package org.cangnova.cangjie.chir.core.visitor

import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.declaration.ChirDeclaration
import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirExpression
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage

interface ChirVisitor {
    fun visitPackage(chirPackage: ChirPackage) {}
    fun visitModule(module: ChirModule) {}
    fun visitDeclaration(declaration: ChirDeclaration) {}
    fun visitFunction(function: ChirFunctionDeclaration) {}
    fun visitBlock(block: ChirBlock) {}
    fun visitExpression(expression: ChirExpression) {}
}

open class ChirWalker : ChirVisitor {
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

    final override fun visitModule(module: ChirModule) {
        onModule(module)
        module.declarations.forEach { declaration ->
            visitDeclaration(declaration)
            if (declaration is ChirFunctionDeclaration) {
                visitFunction(declaration)
            }
        }
    }

    final override fun visitFunction(function: ChirFunctionDeclaration) {
        onFunction(function)
        function.blocks.forEach(::visitBlock)
    }

    final override fun visitBlock(block: ChirBlock) {
        onBlock(block)
        block.expressions.forEach(::visitExpression)
    }

    final override fun visitExpression(expression: ChirExpression) {
        onExpression(expression)
    }

    override fun visitDeclaration(declaration: ChirDeclaration) {
        onDeclaration(declaration)
    }

    protected open fun onPackage(chirPackage: ChirPackage) {}
    protected open fun onModule(module: ChirModule) {}
    protected open fun onDeclaration(declaration: ChirDeclaration) {}
    protected open fun onFunction(function: ChirFunctionDeclaration) {}
    protected open fun onBlock(block: ChirBlock) {}
    protected open fun onExpression(expression: ChirExpression) {}
}
