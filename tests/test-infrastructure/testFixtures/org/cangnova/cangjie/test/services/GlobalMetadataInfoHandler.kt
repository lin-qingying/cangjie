package org.cangnova.cangjie.test.services

import org.cangnova.cangjie.test.codeMetaInfo.CodeMetaInfoParser
import org.cangnova.cangjie.test.codeMetaInfo.CodeMetaInfoRenderer
import org.cangnova.cangjie.test.codeMetaInfo.clearTextFromDiagnosticMarkup
import org.cangnova.cangjie.test.codeMetaInfo.model.CodeMetaInfo
import org.cangnova.cangjie.test.codeMetaInfo.model.ParsedCodeMetaInfo
import org.cangnova.cangjie.test.model.TestFile
import org.cangnova.cangjie.test.model.TestModule

/**
 * 表示 `GlobalMetadataInfoHandler`，承载测试服务中的配置数据、测试产物或处理步骤。
 */
class GlobalMetadataInfoHandler(
    /**
     * 保存 `testServices`，供测试服务在测试执行期间读取或传递。
     */
    val testServices: TestServices,
    /**
     * 保存 `processors`，供测试服务在测试执行期间读取或传递。
     */
    private val processors: List<AdditionalMetaInfoProcessor>
) : TestService {
    /**
     * 保存 `existingInfosPerFile`，供测试服务在测试执行期间读取或传递。
     */
    private lateinit var existingInfosPerFile: Map<TestFile, List<ParsedCodeMetaInfo>>

    /**
     * 保存 `infosPerFile`，供测试服务在测试执行期间读取或传递。
     */
    private val infosPerFile: MutableMap<TestFile, MutableList<CodeMetaInfo>> =
        mutableMapOf<TestFile, MutableList<CodeMetaInfo>>().withDefault { mutableListOf() }

    /**
     * 保存 `existingInfosPerFilePerInfoCache`，供测试服务在测试执行期间读取或传递。
     */
    private val existingInfosPerFilePerInfoCache = mutableMapOf<Pair<TestFile, CodeMetaInfo>, List<ParsedCodeMetaInfo>>()

    /**
     * 执行 `parseExistingMetadataInfosFromAllSources` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    @OptIn(ExperimentalStdlibApi::class)
    fun parseExistingMetadataInfosFromAllSources() {
        existingInfosPerFile = buildMap {
            for (file in testServices.moduleStructure.modules.flatMap { it.files }) {
                put(file, CodeMetaInfoParser.getCodeMetaInfoFromText(file.originalContent))
            }
        }
    }

    /**
     * 执行 `getExistingMetaInfosForFile` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    fun getExistingMetaInfosForFile(file: TestFile): List<ParsedCodeMetaInfo> {
        return existingInfosPerFile.getValue(file)
    }

    /**
     * 执行 `getReportedMetaInfosForFile` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    fun getReportedMetaInfosForFile(file: TestFile): List<CodeMetaInfo> {
        return infosPerFile.getValue(file)
    }

    /**
     * 执行 `getExistingMetaInfosForActualMetadata` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    fun getExistingMetaInfosForActualMetadata(file: TestFile, metaInfo: CodeMetaInfo): List<ParsedCodeMetaInfo> {
        return existingInfosPerFilePerInfoCache.getOrPut(file to metaInfo) {
            getExistingMetaInfosForFile(file).filter { it == metaInfo }
        }
    }

    /**
     * 执行 `addMetadataInfosForFile` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    fun addMetadataInfosForFile(file: TestFile, codeMetaInfos: List<CodeMetaInfo>) {
        val infos = infosPerFile.getOrPut(file) { mutableListOf() }
        infos += codeMetaInfos
    }

    /**
     * 执行 `compareAllMetaDataInfos` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    fun compareAllMetaDataInfos() {
        // TODO: adapt to multiple testdata files
        val moduleStructure = testServices.moduleStructure
        val builder = StringBuilder()
        // In SplittingModuleTransformerForBoxTests, files may have been assigned to modules in a different order than declared in the original source file.
        // So, to reconstruct the original source file it needs not per-module traversal, but in original file order.
        val filesInOriginalOrder = moduleStructure.modules
            .flatMap { module -> module.files.map { file -> module to file } }
            .sortedBy { (_, file) -> file.startLineNumberInOriginalFile }
        for ((module, file) in filesInOriginalOrder) {
            if (!file.isAdditional) {
                processors.forEach { it.processMetaInfos(module, file) }
                val codeMetaInfos = infosPerFile.getValue(file)
                val fileBuilder = StringBuilder()
                CodeMetaInfoRenderer.renderTagsToText(
                    fileBuilder,
                    codeMetaInfos,
                    testServices.sourceFileProvider.getContentOfSourceFile(file)
                )
                val reverseTransformers = testServices.sourceFileProvider.preprocessors.filterIsInstance<ReversibleSourceFilePreprocessor>()
                val initialFileContent = fileBuilder.stripAdditionalEmptyLines(file).toString()
                val actualFileContent =
                    reverseTransformers.foldRight(initialFileContent) { transformer, source -> transformer.revert(file, source) }
                builder.append(actualFileContent)
            }
        }
        val actualText = builder.toString()
        testServices.assertions.assertEqualsToFile(moduleStructure.originalTestDataFiles.single(), actualText)
    }
    /**
     * 提供 `stripAdditionalEmptyLines` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    private fun StringBuilder.stripAdditionalEmptyLines(file: TestFile): CharSequence {
        return if (file.startLineNumberInOriginalFile != 0) {
            this.removePrefix((1..file.startLineNumberInOriginalFile).joinToString(separator = "") { "\n" })
        } else {
            this.toString()
        }
    }

    /**
     * 提供 `buildMismatchMessage` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    private fun buildMismatchMessage(
        expectedFile: java.io.File,
        actualText: String,
        fileComparisons: List<FileComparison>,
    ): String {
        val mismatches = fileComparisons.filter {
            normalizeLineSeparators(it.expectedContent) != normalizeLineSeparators(it.actualContent)
        }
        if (mismatches.isEmpty()) {
            return renderGlobalMismatch(expectedFile, actualText)
        }

        return buildString {
            appendLine("Meta info output differs from expected file: ${expectedFile.path}")
            for ((index, comparison) in mismatches.withIndex()) {
                if (index > 0) appendLine()
                appendLine("Source file: ${comparison.file.relativePath}")
                append(renderComparison(comparison))
            }
        }.trimEnd()
    }

    /**
     * 提供 `renderGlobalMismatch` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    private fun renderGlobalMismatch(expectedFile: java.io.File, actualText: String): String {
        val expectedText = expectedFile.readText()
        val normalizedExpected = normalizeLineSeparators(expectedText)
        val normalizedActual = normalizeLineSeparators(actualText)
        val firstDiff = firstDifferentOffset(normalizedExpected, normalizedActual)

        return buildString {
            appendLine("Actual data differs from file content: ${expectedFile.path}")
            appendLine("No per-file meta-info mismatch was isolated; difference appears in the reconstructed aggregate text.")
            appendLine("Expected length: ${normalizedExpected.length}, actual length: ${normalizedActual.length}")

            if (firstDiff != null) {
                val lineAndColumn = lineAndColumn(normalizedExpected, firstDiff)
                appendLine("First global diff at line ${lineAndColumn.first}, column ${lineAndColumn.second}.")
                appendLine("Expected: ${renderLineSnippet(normalizedExpected, firstDiff)}")
                appendLine("Actual  : ${renderLineSnippet(normalizedActual, firstDiff)}")
            }
        }.trimEnd()
    }

    /**
     * 提供 `renderComparison` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    private fun renderComparison(comparison: FileComparison): String {
        val expectedContent = normalizeLineSeparators(comparison.expectedContent)
        val actualContent = normalizeLineSeparators(comparison.actualContent)
        val expectedInfos = CodeMetaInfoParser.getCodeMetaInfoFromText(expectedContent)
        val actualInfos = CodeMetaInfoParser.getCodeMetaInfoFromText(actualContent)
        val sourceText = clearTextFromDiagnosticMarkup(expectedContent)

        val expectedCounts = expectedInfos.groupingBy { MetaInfoKey.from(it) }.eachCount()
        val actualCounts = actualInfos.groupingBy { MetaInfoKey.from(it) }.eachCount()
        val allKeys = (expectedCounts.keys + actualCounts.keys).sortedWith(
            compareBy<MetaInfoKey> { it.start }
                .thenBy { it.end }
                .thenBy { it.tag }
                .thenBy { it.rendered },
        )

        val missing = mutableListOf<String>()
        val unexpected = mutableListOf<String>()
        for (key in allKeys) {
            val expectedCount = expectedCounts[key] ?: 0
            val actualCount = actualCounts[key] ?: 0
            if (expectedCount > actualCount) {
                repeat(expectedCount - actualCount) { missing += renderMetaInfo(key, sourceText) }
            } else if (actualCount > expectedCount) {
                repeat(actualCount - expectedCount) { unexpected += renderMetaInfo(key, sourceText) }
            }
        }

        return buildString {
            if (missing.isNotEmpty()) {
                appendLine("Missing meta infos:")
                missing.forEach { appendLine("  - $it") }
            }
            if (unexpected.isNotEmpty()) {
                appendLine("Unexpected meta infos:")
                unexpected.forEach { appendLine("  - $it") }
            }

            val firstDiff = firstDifferentOffset(expectedContent, actualContent)
            if (firstDiff != null) {
                val lineAndColumn = lineAndColumn(expectedContent, firstDiff)
                appendLine("First textual diff at line ${lineAndColumn.first}, column ${lineAndColumn.second}.")
                appendLine("Expected: ${renderLineSnippet(expectedContent, firstDiff)}")
                appendLine("Actual  : ${renderLineSnippet(actualContent, firstDiff)}")
            }
        }.trimEnd()
    }

    /**
     * 提供 `renderMetaInfo` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    private fun renderMetaInfo(key: MetaInfoKey, sourceText: String): String {
        val start = key.start.coerceIn(0, sourceText.length)
        val end = key.end.coerceIn(start, sourceText.length)
        val startPos = lineAndColumn(sourceText, start)
        val endPos = lineAndColumn(sourceText, end)
        val snippet = sourceSnippet(sourceText, start, end)
        return "${key.rendered} @ ${startPos.first}:${startPos.second}..${endPos.first}:${endPos.second} " +
            "(offsets [${key.start}, ${key.end})) on \"$snippet\""
    }

    /**
     * 提供 `lineAndColumn` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    private fun lineAndColumn(text: String, offset: Int): Pair<Int, Int> {
        val safeOffset = offset.coerceIn(0, text.length)
        var line = 1
        var column = 1
        for (i in 0 until safeOffset) {
            if (text[i] == '\n') {
                line++
                column = 1
            } else {
                column++
            }
        }
        return line to column
    }

    /**
     * 提供 `sourceSnippet` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    private fun sourceSnippet(text: String, start: Int, end: Int): String {
        if (start == end) {
            val from = (start - 15).coerceAtLeast(0)
            val to = (start + 15).coerceAtMost(text.length)
            return text.substring(from, to)
                .replace("\n", "\\n")
                .let { if (it.isEmpty()) "<empty>" else it }
        }

        return text.substring(start, end)
            .replace("\n", "\\n")
            .let { if (it.length > 80) it.take(77) + "..." else it }
    }

    /**
     * 提供 `firstDifferentOffset` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    private fun firstDifferentOffset(expected: String, actual: String): Int? {
        val limit = minOf(expected.length, actual.length)
        for (index in 0 until limit) {
            if (expected[index] != actual[index]) return index
        }
        return if (expected.length != actual.length) limit else null
    }

    /**
     * 提供 `renderLineSnippet` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    private fun renderLineSnippet(text: String, offset: Int): String {
        val safeOffset = offset.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', (safeOffset - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', safeOffset).let { if (it == -1) text.length else it }
        val line = text.substring(lineStart, lineEnd).replace("\t", "\\t")
        val column = (safeOffset - lineStart).coerceAtLeast(0)
        return "$line [col ${column + 1}]"
    }

    /**
     * 提供 `normalizeLineSeparators` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    private fun normalizeLineSeparators(text: String): String {
        return text.replace("\r\n", "\n").replace('\r', '\n')
    }

    /**
     * 表示 `FileComparison`，承载测试服务中的配置数据、测试产物或处理步骤。
     */
    private data class FileComparison(
        /**
         * 保存 `file`，供测试服务在测试执行期间读取或传递。
         */
        val file: TestFile,
        /**
         * 保存 `expectedContent`，供测试服务在测试执行期间读取或传递。
         */
        val expectedContent: String,
        /**
         * 保存 `actualContent`，供测试服务在测试执行期间读取或传递。
         */
        val actualContent: String,
    )

    /**
     * 表示 `MetaInfoKey`，承载测试服务中的配置数据、测试产物或处理步骤。
     */
    private data class MetaInfoKey(
        /**
         * 保存 `start`，供测试服务在测试执行期间读取或传递。
         */
        val start: Int,
        /**
         * 保存 `end`，供测试服务在测试执行期间读取或传递。
         */
        val end: Int,
        /**
         * 保存 `tag`，供测试服务在测试执行期间读取或传递。
         */
        val tag: String,
        /**
         * 保存 `rendered`，供测试服务在测试执行期间读取或传递。
         */
        val rendered: String,
    ) {
        companion object {
            fun from(info: ParsedCodeMetaInfo): MetaInfoKey {
                return MetaInfoKey(
                    start = info.start,
                    end = info.end,
                    tag = info.tag,
                    rendered = info.asString(),
                )
            }
        }
    }
}

/**
 * 保存 `TestServices.globalMetadataInfoHandler`，供测试服务在测试执行期间读取或传递。
 */
val TestServices.globalMetadataInfoHandler: GlobalMetadataInfoHandler by TestServices.testServiceAccessor()

/**
 * 表示 `AdditionalMetaInfoProcessor`，承载测试服务中的配置数据、测试产物或处理步骤。
 */
abstract class AdditionalMetaInfoProcessor(protected val testServices: TestServices) {
    /**
     * 保存 `globalMetadataInfoHandler`，供测试服务在测试执行期间读取或传递。
     */
    protected val globalMetadataInfoHandler: GlobalMetadataInfoHandler
        get() = testServices.globalMetadataInfoHandler

    /**
     * 提供 `processMetaInfos` 对应的测试服务流程，维持测试框架的阶段契约。
     */
    abstract fun processMetaInfos(module: TestModule, file: TestFile)
}
