package org.cangnova.cangjie.cfir.resolve.body

import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * [classIdForClassNesting] 的仓颉公开 ClassId 规则测试。
 */
class CfirClassIdUtilsTest {
    /**
     * 验证空嵌套名不能构造公开 ClassId。
     */
    @Test
    fun `returns null for empty nesting`() {
        assertNull(classIdForClassNesting(FqName("sample.pkg"), emptyList()))
    }

    /**
     * 验证顶层类名可以转换为公开 ClassId。
     */
    @Test
    fun `builds top level class id`() {
        val packageFqName = FqName("sample.pkg")
        val classId = classIdForClassNesting(packageFqName, listOf(Name.identifier("Top")))

        assertEquals(ClassId(packageFqName, FqName.topLevel(Name.identifier("Top")), isLocal = false), classId)
    }

    /**
     * 验证仓颉公开 ClassId 模型不接受嵌套类链。
     */
    @Test
    fun `nested class ids are not part of cangjie public class id model`() {
        val packageFqName = FqName("sample.pkg")
        val classId = classIdForClassNesting(
            packageFqName,
            listOf(Name.identifier("Outer"), Name.identifier("Inner"), Name.identifier("Leaf")),
        )

        assertNull(classId)
    }
}
