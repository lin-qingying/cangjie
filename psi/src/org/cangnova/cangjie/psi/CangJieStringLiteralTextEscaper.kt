/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.psi

import org.cangnova.cangjie.psi.psiUtil.getContentRange
import org.cangnova.cangjie.psi.psiUtil.isSingleQuoted
import com.intellij.openapi.util.TextRange
import com.intellij.psi.LiteralTextEscaper
import kotlin.math.min

/**
 * 提供 `toNativeArray` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
 */
fun List<Int>.toNativeArray(dest: IntArray, offset: Int, len: Int) {
    require(len >= 0) { "Length cannot be negative." }
    require(offset in 0 until this.size) { "Offset out of bounds: $offset" }

    if (len > 0) {
        // 确保不超出数组边界
        if (offset + len > this.size) {
            throw ArrayIndexOutOfBoundsException("Offset + len exceeds list size.")
        }
        // 执行数组拷贝
        this.subList(offset, offset + len).toIntArray().copyInto(dest, 0, 0, len)
    }
}

/**
 * 提供 `toNativeArray` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
 */
fun List<Int>.toNativeArray(): IntArray {
    return this.toNativeArray(0, this.size)
}

/**
 * 提供 `toNativeArray` 操作，封装仓颉 PSI节点的访问、构造或判断逻辑。
 */
fun List<Int>.toNativeArray(offset: Int, len: Int): IntArray {
    require(len >= 0) { "Length cannot be negative." }
    require(offset in 0..this.size) { "Offset out of bounds: $offset" }

    if (offset + len > this.size) {
        throw ArrayIndexOutOfBoundsException("Offset + len exceeds list size.")
    }

    return IntArray(len).also { dest ->
        this.subList(offset, offset + len).toIntArray().copyInto(dest, 0, 0, len)
    }
}

/**
 * 表示 `CangJieStringLiteralTextEscaper`，承载仓颉 PSI中的语法节点、索引桩或辅助模型。
 */
class CangJieStringLiteralTextEscaper(host: CjStringTemplateExpression) :
    LiteralTextEscaper<CjStringTemplateExpression>(host) {
    /**
     * 保存 `sourceOffsets` 的内部状态，供仓颉 PSI实现维护节点缓存或解析上下文。
     */
    private var sourceOffsets: IntArray? = null

    /**
     * 实现 `decode` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun decode(rangeInsideHost: TextRange, outChars: StringBuilder): Boolean {
        val sourceOffsetsList = mutableListOf<Int>()
        var sourceOffset = 0

        for (child in myHost.entries) {
            val childRange = TextRange.from(child.startOffsetInParent, child.textLength)
            if (rangeInsideHost.endOffset <= childRange.startOffset) {
                break
            }
            if (childRange.endOffset <= rangeInsideHost.startOffset) {
                continue
            }
            when (child) {
                is CjEscapeStringTemplateEntry -> {
                    if (!rangeInsideHost.contains(childRange)) {
                        sourceOffsetsList.add(sourceOffset)
                        sourceOffsets = sourceOffsetsList.toNativeArray()
                        return false
                    }
                    val unescaped = child.unescapedValue
                    outChars.append(unescaped)
                    repeat(unescaped.length) {
                        sourceOffsetsList.add(sourceOffset)
                    }
                    sourceOffset += child.getTextLength()
                }

                else -> {
                    val textRange = rangeInsideHost.intersection(childRange)!!.shiftRight(-childRange.startOffset)
                    outChars.append(child.text, textRange.startOffset, textRange.endOffset)
                    repeat(textRange.length) {
                        sourceOffsetsList.add(sourceOffset++)
                    }
                }
            }
        }
        sourceOffsetsList.add(sourceOffset)
        sourceOffsets = sourceOffsetsList.toNativeArray()
        return true
    }

    /**
     * 实现 `getOffsetInHost` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getOffsetInHost(offsetInDecoded: Int, rangeInsideHost: TextRange): Int {
        val offsets = sourceOffsets
        if (offsets == null || offsetInDecoded >= offsets.size) return -1
        return min(offsets[offsetInDecoded], rangeInsideHost.length) + rangeInsideHost.startOffset
    }

    /**
     * 实现 `getRelevantTextRange` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getRelevantTextRange(): TextRange {
        return myHost.getContentRange()
    }

    /**
     * 实现 `isOneLine` 的仓颉 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun isOneLine(): Boolean {
        return myHost.isSingleQuoted()
    }
}
