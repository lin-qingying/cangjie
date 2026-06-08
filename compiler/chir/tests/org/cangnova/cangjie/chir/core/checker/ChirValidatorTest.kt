package org.cangnova.cangjie.chir.core.checker

import org.cangnova.cangjie.chir.core.context.DefaultChirContext
import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirConditionalBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.expression.ChirCallExpression
import org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression
import org.cangnova.cangjie.chir.core.expression.ChirOtherExpression
import org.cangnova.cangjie.chir.core.expression.ChirUnaryExpression
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.pipeline.ChirPipelineGate
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirUnresolvedTypeRef
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.cangnova.cangjie.chir.core.value.ChirImportedFunctionValue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChirValidatorTest {

    @Test
    fun `validator reports invalid cfg and formatter emits context`() {
        val pkg = invalidPackage()
        val report = DefaultChirValidator().validatePackage(pkg, DefaultChirContext())

        assertTrue(report.hasErrors)
        assertTrue(report.issues.any { it.code == "INVALID_CFG" })

        val formatted = ChirValidationReportFormatter.render(report)
        assertTrue(formatted.contains("INVALID_CFG"))
        assertTrue(formatted.contains("node=fn:invalid"))
    }

    @Test
    fun `pipeline gate blocks invalid package`() {
        val pkg = invalidPackage()
        assertThrows(IllegalArgumentException::class.java) {
            ChirPipelineGate.requireValidForStage(pkg, stageName = "unit-test")
        }
    }

    @Test
    fun `validator reports return type mismatch as error`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val boolType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:return-mismatch"),
            name = "returnMismatch",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirConstantValue(
                            semanticId = ChirSemanticId("const:true"),
                            type = boolType,
                            literal = "true",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
        val pkg = packageWith(function)

        val report = DefaultChirValidator().validatePackage(pkg, DefaultChirContext())
        assertTrue(report.hasErrors)
        assertTrue(report.issues.any { it.code == "RETURN_TYPE_MISMATCH" })
    }

    @Test
    fun `validator rejects non bool conditional branch condition`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:bad-branch-condition"),
            name = "badBranchCondition",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirConditionalBranchTerminator(
                        semanticId = ChirSemanticId("term:conditional"),
                        condition = ChirConstantValue(
                            semanticId = ChirSemanticId("const:one"),
                            type = intType,
                            literal = "1",
                        ),
                        trueTargetBlockId = ChirSemanticId("block:true"),
                        falseTargetBlockId = ChirSemanticId("block:false"),
                    ),
                ),
                ChirBlock(
                    semanticId = ChirSemanticId("block:true"),
                    name = "true",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:true:return"),
                        returnValue = ChirConstantValue(ChirSemanticId("const:true:return"), intType, "1"),
                    ),
                ),
                ChirBlock(
                    semanticId = ChirSemanticId("block:false"),
                    name = "false",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:false:return"),
                        returnValue = ChirConstantValue(ChirSemanticId("const:false:return"), intType, "0"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )

        val report = DefaultChirValidator().validatePackage(packageWith(function), DefaultChirContext())
        assertTrue(report.hasErrors)
        assertTrue(report.issues.any { it.code == "BRANCH_CONDITION_TYPE_MISMATCH" })
    }

    @Test
    fun `validator reports call argument type mismatch`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val boolType = ChirResolvedTypeRef(ChirPrimitiveType.BOOL)

        val callExpr = ChirCallExpression(
            semanticId = ChirSemanticId("expr:call"),
            callee = ChirImportedFunctionValue(
                semanticId = ChirSemanticId("imp:callee"),
                type = ChirResolvedTypeRef(ChirFunctionType(parameterTypes = listOf(intType), returnType = intType)),
                name = "callee",
            ),
            arguments = listOf(
                ChirConstantValue(
                    semanticId = ChirSemanticId("const:true"),
                    type = boolType,
                    literal = "true",
                ),
            ),
            resultType = intType,
        )
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:call-mismatch"),
            name = "callMismatch",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(callExpr),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirConstantValue(
                            semanticId = ChirSemanticId("const:zero"),
                            type = intType,
                            literal = "0",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
        val pkg = packageWith(function)

        val report = DefaultChirValidator().validatePackage(pkg, DefaultChirContext())
        assertTrue(report.hasErrors)
        assertTrue(report.issues.any { it.code == "CALL_ARGUMENT_TYPE_MISMATCH" })
    }

    @Test
    fun `validator rejects unsupported other operation`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:unsupported-other"),
            name = "unsupportedOther",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(
                        ChirOtherExpression(
                            semanticId = ChirSemanticId("expr:unknown"),
                            operation = "mystery-op",
                            operands = emptyList(),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirConstantValue(
                            semanticId = ChirSemanticId("const:zero"),
                            type = intType,
                            literal = "0",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
        val pkg = packageWith(function)

        val report = DefaultChirValidator().validatePackage(pkg, DefaultChirContext())
        assertTrue(report.hasErrors)
        assertTrue(report.issues.any { it.code == "UNSUPPORTED_OTHER_OPERATION" })
    }

    @Test
    fun `validator rejects unsupported unary operator`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:unsupported-unary"),
            name = "unsupportedUnary",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(
                        ChirUnaryExpression(
                            semanticId = ChirSemanticId("expr:unary"),
                            operator = "mystery-unary",
                            operand = ChirConstantValue(
                                semanticId = ChirSemanticId("const:one"),
                                type = intType,
                                literal = "1",
                            ),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirConstantValue(
                            semanticId = ChirSemanticId("const:zero"),
                            type = intType,
                            literal = "0",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
        val report = DefaultChirValidator().validatePackage(packageWith(function), DefaultChirContext())
        assertTrue(report.hasErrors)
        assertTrue(report.issues.any { it.code == "UNSUPPORTED_UNARY_OPERATOR" })
    }

    @Test
    fun `validator rejects unsupported binary operator`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:unsupported-binary"),
            name = "unsupportedBinary",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = listOf(
                        ChirBinaryExpression(
                            semanticId = ChirSemanticId("expr:binary"),
                            operator = "mystery-binary",
                            left = ChirConstantValue(
                                semanticId = ChirSemanticId("const:left"),
                                type = intType,
                                literal = "1",
                            ),
                            right = ChirConstantValue(
                                semanticId = ChirSemanticId("const:right"),
                                type = intType,
                                literal = "2",
                            ),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return"),
                        returnValue = ChirConstantValue(
                            semanticId = ChirSemanticId("const:zero"),
                            type = intType,
                            literal = "0",
                        ),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
        val report = DefaultChirValidator().validatePackage(packageWith(function), DefaultChirContext())
        assertTrue(report.hasErrors)
        assertTrue(report.issues.any { it.code == "UNSUPPORTED_BINARY_OPERATOR" })
    }

    @Test
    fun `validator rejects unresolved type references before lowering`() {
        val unresolved = ChirUnresolvedTypeRef("TPlaceholder")
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:unresolved-type"),
            name = "unresolvedType",
            returnType = unresolved,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(ChirSemanticId("term:return")),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )
        val pkg = packageWith(function)

        val report = DefaultChirValidator().validatePackage(pkg, DefaultChirContext())
        assertTrue(report.hasErrors)
        assertTrue(report.issues.any { it.code == "UNRESOLVED_TYPE_REFERENCE" })
    }

    @Test
    fun `validator rejects unsupported terminator kind`() {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val unsupported = object : ChirTerminator {
            override val semanticId: ChirSemanticId = ChirSemanticId("term:unknown")
        }
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:unsupported-terminator"),
            name = "unsupportedTerminator",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = unsupported,
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )

        val report = DefaultChirValidator().validatePackage(packageWith(function), DefaultChirContext())
        assertTrue(report.hasErrors)
        assertTrue(report.issues.any { it.code == "UNSUPPORTED_TERMINATOR" })
    }

    private fun invalidPackage(): ChirPackage {
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:invalid"),
            name = "invalid",
            returnType = ChirResolvedTypeRef(ChirPrimitiveType.INT32),
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirBranchTerminator(
                        semanticId = ChirSemanticId("term:branch"),
                        targetBlockId = ChirSemanticId("block:missing"),
                    ),
                ),
                ChirBlock(
                    semanticId = ChirSemanticId("block:exit"),
                    name = "exit",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(ChirSemanticId("term:return")),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry"),
        )

        return ChirPackage(
            semanticId = ChirSemanticId("pkg:invalid"),
            name = "invalid.pkg",
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("mod:invalid"),
                    name = "invalid.mod",
                    declarations = listOf(function),
                ),
            ),
        )
    }

    private fun packageWith(function: DefaultChirFunctionDeclaration): ChirPackage {
        return ChirPackage(
            semanticId = ChirSemanticId("pkg:test"),
            name = "test.pkg",
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("mod:test"),
                    name = "test.mod",
                    declarations = listOf(function),
                ),
            ),
        )
    }
}
