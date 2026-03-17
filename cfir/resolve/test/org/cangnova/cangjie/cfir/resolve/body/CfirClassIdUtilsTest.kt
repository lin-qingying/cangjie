package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CfirClassIdUtilsTest {
    @Test
    fun `returns null for empty nesting`() {
        assertNull(classIdForClassNesting(FqName("sample.pkg"), emptyList()))
    }

    @Test
    fun `builds top level class id`() {
        val packageFqName = FqName("sample.pkg")
        val classId = classIdForClassNesting(packageFqName, listOf(Name.identifier("Top")))

        assertEquals(ClassId(packageFqName, FqName.topLevel(Name.identifier("Top")), isLocal = false), classId)
    }

    @Test
    fun `builds nested class id using relative class name chain`() {
        val packageFqName = FqName("sample.pkg")
        val classId = classIdForClassNesting(
            packageFqName,
            listOf(Name.identifier("Outer"), Name.identifier("Inner"), Name.identifier("Leaf")),
        )

        assertEquals(
            ClassId(packageFqName, FqName("Outer.Inner.Leaf"), isLocal = false),
            classId,
        )
    }
}

