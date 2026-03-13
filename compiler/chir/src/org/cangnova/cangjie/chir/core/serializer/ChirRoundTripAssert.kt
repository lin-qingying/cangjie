package org.cangnova.cangjie.chir.core.serializer

import org.cangnova.cangjie.chir.core.declaration.ChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirExpression
import org.cangnova.cangjie.chir.core.model.ChirPackage

object ChirRoundTripAssert {
    fun assertSemanticallyEquivalent(expected: ChirPackage, actual: ChirPackage) {
        require(expected.semanticId == actual.semanticId) { "package id mismatch" }
        require(expected.name == actual.name) { "package name mismatch" }
        require(expected.accessLevel == actual.accessLevel) { "package access mismatch" }
        require(expected.packageInitFunctionId == actual.packageInitFunctionId) { "package init mismatch" }
        require(expected.packageLiteralInitFunctionId == actual.packageLiteralInitFunctionId) { "package literal init mismatch" }
        require(expected.members.globalVariables.size == actual.members.globalVariables.size) { "global variable count mismatch" }
        require(expected.members.globalFunctions.size == actual.members.globalFunctions.size) { "global function count mismatch" }
        require(expected.members.importedVariables.size == actual.members.importedVariables.size) { "imported variable count mismatch" }
        require(expected.members.importedFunctions.size == actual.members.importedFunctions.size) { "imported function count mismatch" }
        require(expected.typeDefinitions.size == actual.typeDefinitions.size) { "type definition count mismatch" }
        require(expected.importedTypeDefinitions.size == actual.importedTypeDefinitions.size) { "imported type definition count mismatch" }
        require(expected.modules.size == actual.modules.size) { "module count mismatch" }

        expected.modules.zip(actual.modules).forEach { (leftModule, rightModule) ->
            require(leftModule.semanticId == rightModule.semanticId) { "module id mismatch" }
            require(leftModule.declarations.size == rightModule.declarations.size) { "declaration count mismatch in ${leftModule.name}" }

            leftModule.declarations.zip(rightModule.declarations).forEach { (leftDecl, rightDecl) ->
                val leftFunction = leftDecl as? ChirFunctionDeclaration
                val rightFunction = rightDecl as? ChirFunctionDeclaration
                require(leftFunction != null && rightFunction != null) { "only function declarations supported for round-trip assert" }
                require(leftFunction.semanticId == rightFunction.semanticId) { "function id mismatch" }
                require(leftFunction.name == rightFunction.name) { "function name mismatch" }
                require(leftFunction.returnType.renderName == rightFunction.returnType.renderName) { "function return type mismatch" }
                require(leftFunction.entryBlockId == rightFunction.entryBlockId) { "entry block mismatch" }
                require(leftFunction.blocks.size == rightFunction.blocks.size) { "block count mismatch" }

                leftFunction.blocks.zip(rightFunction.blocks).forEach { (leftBlock, rightBlock) ->
                    require(leftBlock.semanticId == rightBlock.semanticId) { "block id mismatch" }
                    require(leftBlock.terminator::class == rightBlock.terminator::class) { "terminator kind mismatch" }
                    require(leftBlock.expressions.size == rightBlock.expressions.size) { "expression count mismatch" }
                    leftBlock.expressions.zip(rightBlock.expressions).forEach { (leftExpr, rightExpr) ->
                        assertExpressionEquivalent(leftExpr, rightExpr)
                    }
                }
            }
        }
    }

    private fun assertExpressionEquivalent(expected: ChirExpression, actual: ChirExpression) {
        require(expected::class == actual::class) { "expression kind mismatch" }
        require(expected.semanticId == actual.semanticId) { "expression id mismatch" }
        require(expected.domain == actual.domain) { "expression domain mismatch" }
        require(expected.resultType?.renderName == actual.resultType?.renderName) { "expression result type mismatch" }
    }
}
