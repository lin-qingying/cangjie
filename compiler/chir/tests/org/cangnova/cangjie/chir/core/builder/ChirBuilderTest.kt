package org.cangnova.cangjie.chir.core.builder

import org.cangnova.cangjie.chir.core.context.DefaultChirContext
import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.controlflow.ChirBranchTerminator
import org.cangnova.cangjie.chir.core.controlflow.ChirReturnTerminator
import org.cangnova.cangjie.chir.core.declaration.DefaultChirFunctionDeclaration
import org.cangnova.cangjie.chir.core.declaration.DefaultChirVariableDeclaration
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.symbol.ChirSymbol
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ChirBuilderTest {

    @Test
    fun `builder rejects function with missing entry block`() {
        val diagnostics = RecordingChirDiagnosticCollector()
        val builder = DefaultChirBuilder(DefaultChirContext(), diagnostics = diagnostics)

        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:test"),
            name = "test",
            returnType = ChirResolvedTypeRef(ChirPrimitiveType.UNIT),
            parameters = emptyList(),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:actual"),
                    name = "actual",
                    expressions = emptyList(),
                    terminator = ChirReturnTerminator(ChirSemanticId("term:ret")),
                ),
            ),
            entryBlockId = ChirSemanticId("block:missing"),
        )

        assertFalse(builder.registerDeclaration(function))
        assertTrue(diagnostics.errors.any { it is ChirBuildError.InvalidGraph })
    }

    @Test
    fun `builder detects duplicate symbol`() {
        val diagnostics = RecordingChirDiagnosticCollector()
        val builder = DefaultChirBuilder(DefaultChirContext(), diagnostics = diagnostics)
        val declarationId = ChirSemanticId("decl:foo")

        val first = ChirSymbol(semanticId = ChirSemanticId("sym:foo"), name = "foo", declarationId = declarationId)
        val second = ChirSymbol(semanticId = ChirSemanticId("sym:foo2"), name = "foo", declarationId = ChirSemanticId("decl:bar"))

        assertTrue(builder.declareSymbol(first))
        assertFalse(builder.declareSymbol(second))
        assertTrue(diagnostics.errors.any { it is ChirBuildError.DuplicateSymbol })
    }

    @Test
    fun `builder can bind symbol reference after package registration`() {
        val context = DefaultChirContext()
        val builder = DefaultChirBuilder(context)

        val intType = ChirResolvedTypeRef(ChirPrimitiveType.INT32)
        val parameter = DefaultChirVariableDeclaration(
            semanticId = ChirSemanticId("var:x"),
            name = "x",
            type = intType,
            mutable = false,
        )
        val function = DefaultChirFunctionDeclaration(
            semanticId = ChirSemanticId("fn:foo"),
            name = "foo",
            returnType = intType,
            parameters = listOf(parameter),
            blocks = listOf(
                ChirBlock(
                    semanticId = ChirSemanticId("block:entry"),
                    name = "entry",
                    expressions = emptyList(),
                    terminator = ChirBranchTerminator(
                        semanticId = ChirSemanticId("term:branch"),
                        targetBlockId = ChirSemanticId("block:exit"),
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

        val packageNode = ChirPackage(
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

        assertTrue(builder.registerPackage(packageNode))
        assertNotNull(context.findDeclaration(function.semanticId))

        assertTrue(builder.declareSymbol(ChirSymbol(ChirSemanticId("sym:foo"), "foo", function.semanticId)))
        val bound = requireNotNull(builder.bindReference(UnboundChirReference(ChirSemanticId("ref:1"), "foo")))
        assertEquals(function.semanticId, bound.targetDeclarationId)
    }
}
