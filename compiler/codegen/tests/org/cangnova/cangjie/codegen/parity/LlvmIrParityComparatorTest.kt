package org.cangnova.cangjie.codegen.parity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * `LlvmIrParityComparator` 的规范化和差异报告测试。
 */
class LlvmIrParityComparatorTest {
    /**
     * 测试使用的默认 parity comparator。
     */
    private val comparator = LlvmIrParityComparator()

    /**
     * 验证规范化会排序顶层声明。
     */
    @Test
    fun `normalization sorts top-level declarations`() {
        val ir = """
target triple = "x86_64-unknown-linux-gnu"
@g = global i32 0
declare void @foo()
%struct.B = type { i8 }
%struct.A = type { i8 }
        """.trimIndent()

        val normalized = comparator.normalize(ir)
        val lines = normalized.lines()
        assertEquals("%struct.A = type { i8 }", lines[1])
        assertEquals("%struct.B = type { i8 }", lines[2])
        assertEquals("@g = global i32 0", lines[3])
        assertEquals("declare void @foo()", lines[4])
    }

    /**
     * 验证比较结果会记录首个不同行。
     */
    @Test
    fun `compare finds first mismatch`() {
        val expected = """
define i32 @main() {
entry:
  ret i32 0
}
        """.trimIndent()
        val actual = """
define i32 @main() {
entry:
  ret i32 1
}
        """.trimIndent()

        val result = comparator.compare(expected, actual)

        assertFalse(result.matches)
        assertEquals(3, result.firstDiff?.lineNumber)
        assertEquals("  ret i32 0", result.firstDiff?.expected)
        assertEquals("  ret i32 1", result.firstDiff?.actual)
    }

    /**
     * 验证差异报告包含行号、期望行和实际行。
     */
    @Test
    fun `format report includes line and values`() {
        val result = comparator.compare(
            "define i32 @main() {\n  ret i32 0\n}",
            "define i32 @main() {\n  ret i32 2\n}",
        )
        val report = comparator.formatFirstDiffReport(result)

        assertTrue(report.contains("LLVM-IR parity mismatch"))
        assertTrue(report.contains("line: 2"))
        assertTrue(report.contains("expected:   ret i32 0"))
        assertTrue(report.contains("actual  :   ret i32 2"))
    }

    /**
     * 验证默认规范化会忽略 LLVM 注释行。
     */
    @Test
    fun `normalization ignores comment lines`() {
        val expected = """
; comment
define i32 @main() {
  ret i32 0
}
        """.trimIndent()
        val actual = """
define i32 @main() {
  ret i32 0
}
        """.trimIndent()

        val result = comparator.compare(expected, actual)
        assertTrue(result.matches, comparator.formatFirstDiffReport(result))
    }
}
