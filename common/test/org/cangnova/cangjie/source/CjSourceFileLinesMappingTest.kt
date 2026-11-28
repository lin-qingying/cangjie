package org.cangnova.cangjie.source

import kotlin.test.Test
import kotlin.test.assertEquals

class CjSourceFileLinesMappingTest {
    @Test
    fun `raw mixed line endings retain original offsets`() {
        val source = "a\rb\r\nc\nd\r"
        val mapping = source.toSourceLinesMapping()
        assertEquals(source.length, mapping.lastOffset)
        assertEquals(listOf(0, 2, 5, 7, 9), (0 until mapping.linesCount).map(mapping::getLineStartOffset))
        assertEquals(1 to 0, mapping.getLineAndColumnByOffset(2))
        assertEquals(2 to 0, mapping.getLineAndColumnByOffset(5))
        assertEquals(4 to 0, mapping.getLineAndColumnByOffset(source.length))
    }
}
