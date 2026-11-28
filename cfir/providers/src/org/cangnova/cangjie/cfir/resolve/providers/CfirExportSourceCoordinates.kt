package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.source.CjSourceFileLinesMapping

/** 同代源码坐标产品；保存行首值，metadata消费者不能重读活动PSI document或文件。 */
class CfirExportSourceCoordinates private constructor(private val lineStarts: IntArray, val lastOffset: Int) {
    fun lineAndColumn(offset: Int): Pair<Int, Int> {
        require(offset in 0..lastOffset)
        val found = lineStarts.binarySearch(offset)
        val line = if (found >= 0) found else -found - 2
        check(line >= 0)
        return line to offset - lineStarts[line]
    }

    internal fun validate(mapping: CjSourceFileLinesMapping?) {
        check(mapping != null && mapping.lastOffset == lastOffset && mapping.linesCount == lineStarts.size &&
            lineStarts.indices.all { mapping.getLineStartOffset(it) == lineStarts[it] }) {
            "Export source coordinates changed after their input was captured"
        }
    }

    companion object {
        internal fun capture(mapping: CjSourceFileLinesMapping?): CfirExportSourceCoordinates? {
            if (mapping == null || mapping.linesCount == 0 || mapping.lastOffset < 0) return null
            val offsets = IntArray(mapping.linesCount) { mapping.getLineStartOffset(it) }
            check(offsets.first() == 0 && offsets.asList().zipWithNext().all { (first, second) -> first < second })
            return CfirExportSourceCoordinates(offsets, mapping.lastOffset)
        }
    }
}
