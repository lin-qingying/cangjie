package org.cangnova.cangjie.chir.core.testkit

import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirVariableDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirMemoryExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.expression.ChirUnaryExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.model.ChirPackageAccessLevel
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirRefType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirImportedFunctionValue
import org.cangnova.cangjie.chir.core.value.ChirLocalValue

object ChirTestFixtures {
    fun codecPackage(): ChirPackage {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val boolType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val refIntType = ChirResolvedTypeRef(ChirRefType(intType, mutable = true))
        val helperType = ChirResolvedTypeRef(
            ChirFunctionType(
                parameterTypes = listOf(intType),
                returnType = intType,
            ),
        )
        val constOne = ChirConstantValue(ChirSemanticId("const:1"), intType, "1")
        val constTwo = ChirConstantValue(ChirSemanticId("const:2"), intType, "2")
        val condition = ChirConstantValue(ChirSemanticId("const:cond"), boolType, "1")
        val slot = ChirLocalValue(ChirSemanticId("local:slot"), refIntType, "slot")
        val helper = ChirImportedFunctionValue(ChirSemanticId("import:helper"), helperType, "helper")
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:codec"),
            name = "codec",
            returnType = intType,
            parameters = listOf(
                DefaultChirVariableDeclaration(
                    semanticId = ChirSemanticId("param:x"),
                    name = "x",
                    type = intType,
                    mutable = false,
                ),
            ),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(
                        ChirUnaryExpression(
                            semanticId = ChirSemanticId("expr:neg"),
                            operator = "neg",
                            operand = constOne,
                            resultType = intType,
                        ),
                        ChirBinaryExpression(
                            semanticId = ChirSemanticId("expr:add"),
                            operator = "udiv",
                            left = constTwo,
                            right = constOne,
                            resultType = intType,
                        ),
                        ChirMemoryExpression(
                            semanticId = ChirSemanticId("expr:load"),
                            operation = "load",
                            address = slot,
                            resultType = intType,
                        ),
                        ChirCallExpression(
                            semanticId = ChirSemanticId("expr:call"),
                            callee = helper,
                            arguments = listOf(constOne),
                            resultType = intType,
                        ),
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:select"),
                            operation = "select",
                            operands = listOf(condition, constOne, constTwo),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirBranchTerminator(
                        semanticId = ChirSemanticId("term:goto-exit"),
                        targetBlockId = ChirSemanticId("block:exit"),
                    ),
                ),
                ChirBlock(
                    semanticId = ChirSemanticId("block:exit"),
                    name = "exit",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirConstantValue(ChirSemanticId("const:ret"), intType, "3"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )

        return ChirPackage(
            semanticId = ChirSemanticId("pkg:codec"),
            name = "codec.pkg",
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("mod:codec"),
                    name = "codec.mod",
                    declarations = listOf(function),
                ),
            ),
            packageInitFunctionId = function.semanticId,
            packageLiteralInitFunctionId = function.semanticId,
            accessLevel = ChirPackageAccessLevel.PUBLIC,
        )
    }
}
