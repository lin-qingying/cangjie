package org.cangnova.cangjie.chir.core.model

import org.cangnova.cangjie.chir.core.declaration.DefaultChirClassDeclaration
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.type.ChirTypeDeclarationId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/** 验证包级名义类型定义索引使用跨包稳定身份。 */
class ChirPackageNominalTypeIndexTest {
    @Test
    fun `indexes same short name from distinct packages independently`() {
        val leftId = ChirTypeDeclarationId("left", "Node")
        val rightId = ChirTypeDeclarationId("right", "Node")
        val left = DefaultChirClassDeclaration(
            semanticId = ChirSemanticId("left:Node"),
            name = "Node",
            typeDeclarationId = leftId,
        )
        val right = DefaultChirClassDeclaration(
            semanticId = ChirSemanticId("right:Node"),
            name = "Node",
            typeDeclarationId = rightId,
        )
        val chirPackage = ChirPackage(
            semanticId = ChirSemanticId("package:test"),
            name = "test",
            modules = emptyList(),
            typeDefinitions = listOf(left, right),
        )

        assertEquals(setOf(leftId, rightId), chirPackage.nominalTypeDefinitionsById.keys)
        assertSame(left, chirPackage.nominalTypeDefinitionsById.getValue(leftId))
        assertSame(right, chirPackage.nominalTypeDefinitionsById.getValue(rightId))
    }

    @Test
    fun `rejects duplicate nominal declaration identity across package definition sets`() {
        val id = ChirTypeDeclarationId("shared", "Node")
        val local = DefaultChirClassDeclaration(
            semanticId = ChirSemanticId("local:Node"),
            name = "Node",
            typeDeclarationId = id,
        )
        val imported = DefaultChirClassDeclaration(
            semanticId = ChirSemanticId("imported:Node"),
            name = "Node",
            typeDeclarationId = id,
        )
        val chirPackage = ChirPackage(
            semanticId = ChirSemanticId("package:test"),
            name = "test",
            modules = emptyList(),
            typeDefinitions = listOf(local),
            importedTypeDefinitions = listOf(imported),
        )

        assertThrows<IllegalStateException> { chirPackage.nominalTypeDefinitionsById }
    }
}
