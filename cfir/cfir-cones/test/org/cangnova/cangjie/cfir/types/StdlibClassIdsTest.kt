package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.builtins.StandardNames
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * 验证标准库类标识集合的包名、短名和聚合列表内容。
 */
class StdlibClassIdsTest {

    /**
     * 验证所有标准库类标识都位于 std.core 或 stdx.effect 包。
     */
    @Test
    fun `all ClassIds belong to std-core or stdx-effect packages`() {
        val corePackage = StandardNames.FqNames.core
        val effectPackage = StandardNames.FqNames.effect
        for (classId in StdlibClassIds.allClassIds) {
            assertTrue(
                classId.packageFqName == corePackage || classId.packageFqName == effectPackage,
                "${classId.shortClassName} should be in std.core or stdx.effect",
            )
        }
    }

    /**
     * 验证 Object 标准库类标识的短名。
     */
    @Test
    fun `Object ClassId has correct short name`() {
        assertEquals("Object", StdlibClassIds.Object.shortClassName.asString())
    }

    /**
     * 验证 String 标准库类标识的短名。
     */
    @Test
    fun `String ClassId has correct short name`() {
        assertEquals("String", StdlibClassIds.String.shortClassName.asString())
    }

    /**
     * 验证 allClassIds 聚合列表包含当前声明的所有标准库类标识。
     */
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
        assertTrue(all.contains(StdlibClassIds.Error))
        assertTrue(all.contains(StdlibClassIds.Resource))
        assertTrue(all.contains(StdlibClassIds.Comparable))
        assertTrue(all.contains(StdlibClassIds.Equatable))
        assertTrue(all.contains(StdlibClassIds.Countable))
        assertTrue(all.contains(StdlibClassIds.Iterable))
        assertTrue(all.contains(StdlibClassIds.ToString))
        assertTrue(all.contains(StdlibClassIds.Future))
        assertTrue(all.contains(StdlibClassIds.Command))
        assertTrue(all.contains(StdlibClassIds.Resumption))
        assertEquals(17, all.size)
    }

    /**
     * 验证标准库类标识集合不混入由 PrimitiveTypeKind 表示的原始类型。
     */
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
