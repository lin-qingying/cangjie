package org.cangnova.cangjie.chir.core.printer

import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.testkit.ChirTestAssertions
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.value.ChirConstantValue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChirPrinterInspectorTest {

    @Test
    fun `printer emits stable sorted output with node identifiers`() {
        val printed = ChirPrinter.print(samplePackage())

        val expected = """
package printer.pkg id=pkg:printer
  module alpha.mod id=mod:alpha
    function alpha id=fn:alpha return=int32 entry=block:entry-alpha
      block[0] entry-alpha id=block:entry-alpha
        term return id=term:return-alpha value=const(id=const:alpha,type=int32,literal=7)
  module beta.mod id=mod:beta
    function beta id=fn:beta return=int32 entry=block:entry-beta
      param[0] x id=param:x type=int32 mutable=false
      block[0] entry-beta id=block:entry-beta
        expr[0] binary id=expr:add op=add left=const(id=const:1,type=int32,literal=1) right=const(id=const:2,type=int32,literal=2) type=int32
        term branch id=term:branch target=block:exit-beta
      block[1] exit-beta id=block:exit-beta
        term return id=term:return-beta value=const(id=const:ret,type=int32,literal=3)
        """.trimIndent()

        assertEquals(expected, printed)
    }

    @Test
    fun `inspector emits structured deterministic summary`() {
        val fixture = samplePackage()
        ChirTestAssertions.assertPrinterStable(fixture)
        val inspect = ChirInspector.inspect(fixture)

        assertTrue(inspect.contains("\"packageId\": \"pkg:printer\""))
        assertTrue(inspect.contains("\"moduleCount\": 2"))
        assertTrue(inspect.contains("\"functionCount\": 2"))
        assertTrue(inspect.contains("\"blockCount\": 3"))
        assertTrue(inspect.contains("\"expressionCount\": 1"))

        val alphaPos = inspect.indexOf("\"id\": \"mod:alpha\"")
        val betaPos = inspect.indexOf("\"id\": \"mod:beta\"")
        assertTrue(alphaPos in 0 until betaPos)
    }

    private fun samplePackage(): ChirPackage {
        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val beta = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:beta"),
            name = "beta",
            returnType = intType,
            parameters = listOf(
                org.cangnova.cangjie.chir.core.declaration.DefaultChirVariableDeclaration(
                    semanticId = ChirSemanticId("param:x"),
                    name = "x",
                    type = intType,
                    mutable = false,
                ),
            ),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry-beta"),
                    name = "entry-beta",
                    expressions = listOf(
                        org.cangnova.cangjie.chir.core.expression.ChirBinaryExpression(
                            semanticId = ChirSemanticId("expr:add"),
                            operator = "add",
                            left = ChirConstantValue(ChirSemanticId("const:1"), intType, "1"),
                            right = ChirConstantValue(ChirSemanticId("const:2"), intType, "2"),
                            resultType = intType,
                        ),
                    ),
                    terminator = ChirBranchTerminator(
                        semanticId = ChirSemanticId("term:branch"),
                        targetBlockId = ChirSemanticId("block:exit-beta"),
                    ),
                ),
                ChirBlock(
                    semanticId = ChirSemanticId("block:exit-beta"),
                    name = "exit-beta",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return-beta"),
                        returnValue = ChirConstantValue(ChirSemanticId("const:ret"), intType, "3"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry-beta"),
        )
        val alpha = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:alpha"),
            name = "alpha",
            returnType = intType,
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry-alpha"),
                    name = "entry-alpha",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(
                        semanticId = ChirSemanticId("term:return-alpha"),
                        returnValue = ChirConstantValue(ChirSemanticId("const:alpha"), intType, "7"),
                    ),
                ),
            ),
            entryBlockId = ChirSemanticId("block:entry-alpha"),
        )

        return ChirPackage(
            semanticId = ChirSemanticId("pkg:printer"),
            name = "printer.pkg",
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("mod:beta"),
                    name = "beta.mod",
                    declarations = listOf(beta),
                ),
                ChirModule(
                    semanticId = ChirSemanticId("mod:alpha"),
                    name = "alpha.mod",
                    declarations = listOf(alpha),
                ),
            ),
        )
    }
}
