package org.cangjie.cfir.types

import org.cangnova.cangjie.builtins.StandardNames
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class StdlibClassIdsTest {

    @Test
    fun `all ClassIds belong to std-core package`() {
        val corePackage = StandardNames.FqNames.core
        for (classId in StdlibClassIds.allClassIds) {
            assertEquals(corePackage, classId.packageFqName,
                "${classId.shortClassName} should be in std.core")
        }
    }

    @Test
    fun `Object ClassId has correct short name`() {
        assertEquals("Object", StdlibClassIds.Object.shortClassName.asString())
    }

    @Test
    fun `String ClassId has correct short name`() {
        assertEquals("String", StdlibClassIds.String.shortClassName.asString())
    }

    @Test
    fun `allClassIds contains all declared ClassIds`() {
        val all = StdlibClassIds.allClassIds
        assertTrue(all.contains(StdlibClassIds.Object))
        assertTrue(all.contains(StdlibClassIds.Any))
        assertTrue(all.contains(StdlibClassIds.String))
        assertTrue(all.contains(StdlibClassIds.Array))
        assertTrue(all.contains(StdlibClassIds.Option))
        assertTrue(all.contains(StdlibClassIds.Range))
        assertTrue(all.contains(StdlibClassIds.Exception))
        assertTrue(all.contains(StdlibClassIds.Resource))
        assertTrue(all.contains(StdlibClassIds.Comparable))
        assertTrue(all.contains(StdlibClassIds.Equatable))
        assertTrue(all.contains(StdlibClassIds.Countable))
        assertTrue(all.contains(StdlibClassIds.Iterable))
        assertTrue(all.contains(StdlibClassIds.ToString))
        assertTrue(all.contains(StdlibClassIds.Future))
        assertEquals(14, all.size)
    }

    @Test
    fun `StdlibClassIds does not contain primitive types`() {
        // Primitive types (Int64, Bool etc.) are represented by PrimitiveTypeKind, not ClassIds
        val shortNames = StdlibClassIds.allClassIds.map { it.shortClassName.asString() }.toSet()
        assertFalse("Int64" in shortNames)
        assertFalse("Bool" in shortNames)
        assertFalse("Float64" in shortNames)
        assertFalse("Unit" in shortNames)
    }
}
