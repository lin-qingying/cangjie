package org.cangnova.cangjie.chir.core.checker

import org.cangnova.cangjie.chir.core.context.DefaultChirContext
import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.pipeline.ChirPipelineGate
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
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
}
