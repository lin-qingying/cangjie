/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.test.codeMetaInfo

import org.cangnova.cangjie.test.codeMetaInfo.model.ParsedCodeMetaInfo

/**
 * 提供 `CodeMetaInfoParser` 单例，集中承载代码元信息测试的共享状态、常量或默认行为。
 */
object CodeMetaInfoParser {
    /**
     * 保存 `openingRegex`，供代码元信息测试在测试执行期间读取或传递。
     */
    val openingRegex = """(<!([^"]*?((".*?")(, ".*?")*?)?[^"]*?)!>)""".toRegex()
    /**
     * 保存 `closingRegex`，供代码元信息测试在测试执行期间读取或传递。
     */
    val closingRegex = """(<!>)""".toRegex()

    /**
     * 保存 `openingOrClosingRegex`，供代码元信息测试在测试执行期间读取或传递。
     */
    val openingOrClosingRegex = """(${closingRegex.pattern}|${openingRegex.pattern})""".toRegex()

    /*
     * ([\S&&[^,(){}]]+) -- tag, allowing all non-space characters except bracers and curly bracers
     * ([{](.*?)[}])? -- list of attributes
     * (\("((?:\\"|.)*?)"\))? -- arguments of meta info
     * (, )? -- possible separator between different infos
     * 
     * Note about escaping quotes in arguments:
     * `".*?"` matches everything between `"` and the closest next `"` that follows after. `\\"`
     * enforces that escaped `"` are matched "along with" other symbols matched via `.`, so that
     * the closing quote no longer has a change to match `\\"`.
     * Note that just using `.*` would match `<!TAG("A"), RAG("B")!>` as `A"), RAG("B`.
     */
    private val tagRegex = """([\S&&[^,(){}]]+)([{](.*?)[}])?(\("((?:\\"|.)*?)"\))?(, )?""".toRegex()

    /**
     * 表示 `Opening`，承载代码元信息测试中的配置数据、测试产物或处理步骤。
     */
    private class Opening(val index: Int, val tags: String, val startOffset: Int) {
        /**
         * 执行 `equals` 对应的代码元信息测试流程，维持测试框架的阶段契约。
         */
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Opening

            if (index != other.index) return false

            return true
        }

        /**
         * 执行 `hashCode` 对应的代码元信息测试流程，维持测试框架的阶段契约。
         */
        override fun hashCode(): Int {
            return index
        }
    }

    /**
     * 执行 `getCodeMetaInfoFromText` 对应的代码元信息测试流程，维持测试框架的阶段契约。
     */
    fun getCodeMetaInfoFromText(renderedText: String): List<ParsedCodeMetaInfo> {
        var text = renderedText

        val openings = ArrayDeque<Opening>()
        val stackOfOpenings = ArrayDeque<Opening>()
        val closingOffsets = mutableMapOf<Opening, Int>()
        val result = mutableListOf<ParsedCodeMetaInfo>()

        var counter = 0

        while (true) {
            var openingStartOffset = Int.MAX_VALUE
            var closingStartOffset = Int.MAX_VALUE
            val opening = openingRegex.find(text)
            val closing = closingRegex.find(text)
            if (opening == null && closing == null) break

            if (opening != null)
                openingStartOffset = opening.range.first
            if (closing != null)
                closingStartOffset = closing.range.first

            text = if (openingStartOffset < closingStartOffset) {
                requireNotNull(opening)
                val openingMatch = Opening(counter++, opening.groups[2]!!.value, opening.range.first)
                openings.addLast(openingMatch)
                stackOfOpenings.addLast(openingMatch)
                text.removeRange(openingStartOffset, opening.range.last + 1)
            } else {
                requireNotNull(closing)
                closingOffsets[stackOfOpenings.removeLast()] = closing.range.first
                text.removeRange(closingStartOffset, closing.range.last + 1)
            }
        }
        if (openings.size != closingOffsets.size) {
            error("Opening and closing tags counts are not equals")
        }
        while (!openings.isEmpty()) {
            val openingMatchResult = openings.removeLast()
            val closingMatchResult = closingOffsets.getValue(openingMatchResult)
            val allMetaInfos = openingMatchResult.tags
            tagRegex.findAll(allMetaInfos).map { it.groups }.forEach {
                val tag = it[1]!!.value
                val attributes = it[3]?.value?.split(";") ?: emptyList()
                val description = it[5]?.value

                val distinctAttributes = attributes.distinct()
                val duplications = attributes.size - distinctAttributes.size
                (0..duplications).mapTo(result) {
                    ParsedCodeMetaInfo(
                        openingMatchResult.startOffset,
                        closingMatchResult,
                        distinctAttributes.toMutableList(),
                        tag,
                        description
                    )
                }
            }
        }
        return result
    }
}
