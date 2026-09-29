package org.cangnova.cangjie.name

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

/** 组合限定名必须逐段维护父链；字符串相等不足以保证名称模型等价。 */
class FqNamePathCompositionTest {
    @Test
    fun composedPathsHaveTheSameParentsAsParsedPaths() {
        for ((prefix, local) in listOf("" to "std.collection.ArrayList", "a" to "a.B", "a.b" to "c.D", "a" to "B")) {
            var composed = FqName(prefix).child(FqName(local))
            var parsed = FqName(listOf(prefix, local).filter(String::isNotEmpty).joinToString("."))
            while (!parsed.isRoot) {
                assertEquals(parsed, composed)
                assertEquals(parsed.shortName(), composed.shortName())
                assertEquals(parsed.parent(), composed.parent())
                composed = composed.parent()
                parsed = parsed.parent()
            }
            assertEquals(FqName.ROOT, composed)
        }
    }

    @Test
    fun emptySuffixDoesNotCreateAFalseParent() {
        val path = FqName("a.b")
        assertSame(path, path.child(FqName.ROOT))
        assertSame(FqName.ROOT, FqName.ROOT.child(FqName.ROOT))
    }
}
