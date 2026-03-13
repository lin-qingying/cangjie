package org.cangnova.cangjie.chir.core.context

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirModule
import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ChirReadOnlyContextTest {
    @Test
    fun `read-only views are exposed as snapshots`() {
        val context: ChirContext = DefaultChirContext()
        val pkg = ChirPackage(
            semanticId = ChirSemanticId("pkg:readonly"),
            name = "readonly.pkg",
            modules = listOf(
                ChirModule(
                    semanticId = ChirSemanticId("mod:readonly"),
                    name = "readonly.mod",
                    declarations = emptyList(),
                ),
            ),
        )
        context.registerPackage(pkg)
        context.registerModule(pkg.modules.single())

        val readOnly: ChirReadOnlyContext = context
        @Suppress("UNCHECKED_CAST")
        val mutableView = readOnly.packages as MutableCollection<ChirPackage>
        assertThrows(UnsupportedOperationException::class.java) {
            mutableView.clear()
        }

        assertNotNull(readOnly.findPackage(pkg.semanticId))
        assertEquals(1, readOnly.packages.size)
    }
}
