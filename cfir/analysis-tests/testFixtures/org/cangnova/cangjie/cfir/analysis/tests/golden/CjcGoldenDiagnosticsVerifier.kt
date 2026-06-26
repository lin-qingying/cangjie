package org.cangnova.cangjie.cfir.analysis.tests.golden

import org.cangnova.cangjie.test.codeMetaInfo.clearTextFromDiagnosticMarkup
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

/**
 * CjcGoldenDiagnosticsVerifier — 核心：清理标记 → 调用 cjc → 解析 JSON → 行级对比。
 *
 * 流程：
 * 1. 解析 `<!TAG!>` 标记 → `Map<行号, Set<标记名>>`
 * 2. 去掉标记和 `/* suggested message: ... */` → 纯仓颉代码
 * 3. 处理多文件测试（`// FILE:`）：按依赖顺序逐包编译
 * 4. 调用 cjc 并解析 JSON → `Map<行号, Set<DiagKind>>`
 * 5. 按行号对比期望与实际诊断，输出 OK/MISMATCH/SYNTAX_DIVERGENCE 状态
 */
object CjcGoldenDiagnosticsVerifier {

    /** `/* suggested message: ... */` 注释模式（可能跨行） */
    private val suggestedMessageRegex = Regex(
        pattern = """/\*\s*suggested message:.*?\*/""",
        options = setOf(RegexOption.DOT_MATCHES_ALL),
    )

    /** `<!TAG!>` 与 `<!>` 标记的简单模式 */
    private val openMarkerRegex = Regex("""<!([^!>]*?)!>""")
    /** 内联诊断结束标记 `<!>` 的匹配模式。 */
    private val closeMarkerRegex = Regex("""<!>""")

    /**
     * 验证单个测试文件，返回结果。
     *
     * @param cjcPath cjc 可执行文件路径
     * @param testFile 原始测试 .cj 文件（包含 <!TAG!> 标记）
     */
    fun verify(cjcPath: Path, testFile: File): VerificationResult {
        val originalText = testFile.readText()

        // 1. 解析 <!TAG!> 标记 → 期望诊断（按行号分组）
        val expectedByLine = parseExpectedDiagnosticsByLine(originalText)

        // 2. 清理出纯净的源代码（去掉 <!TAG!> 和 /* suggested message: ... */）
        //    注意：清理顺序很重要，先清理 suggested message 再清理标记，
        //    避免标记内的 /* */ 与 suggested message 混淆。
        val cleaned = clearTextFromDiagnosticMarkup(originalText)
            .let { suggestedMessageRegex.replace(it, "") }

        // 3. 多文件 vs 单文件
        val isMultiFile = cleaned.contains("// FILE:")
        return if (isMultiFile) {
            verifyMultiFile(cjcPath, testFile.name, originalText, cleaned, expectedByLine)
        } else {
            verifySingleFile(cjcPath, testFile.name, originalText, cleaned, expectedByLine)
        }
    }

    /**
     * 使用官方 cjc 验证单文件测试数据。
     *
     * 方法会把清理后的源码写入临时目录，避免原始 testData 被修改，并把 cjc
     * JSON 输出交给统一结果分析流程。
     */
    private fun verifySingleFile(
        cjcPath: Path,
        fileName: String,
        originalText: String,
        cleanedText: String,
        expectedByLine: Map<Int, Set<String>>,
    ): VerificationResult {
        val tempDir = Files.createTempDirectory("cjc-golden-").toFile()
        try {
            val tempFile = File(tempDir, fileName)
            tempFile.writeText(cleanedText)
            val result = CjcProcessRunner.compileSingleFile(cjcPath, tempFile)
            return analyzeResult(fileName, originalText, expectedByLine, result, tempFile.absolutePath)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // ========== 多文件验证 ==========

    /**
     * 多文件测试处理：
     * 1. 按 `// FILE:` 分割
     * 2. 提取每个文件的 `package` 与 `import`
     * 3. 拓扑排序后按依赖顺序编译
     * 4. 收集所有诊断按原始行号对比
     */
    private fun verifyMultiFile(
        cjcPath: Path,
        fileName: String,
        originalText: String,
        cleanedText: String,
        expectedByLine: Map<Int, Set<String>>,
    ): VerificationResult {
        val tempDir = Files.createTempDirectory("cjc-golden-multi-").toFile()
        try {
            val files = splitByFile(cleanedText)
            if (files.isEmpty()) {
                return VerificationResult.SyntaxDivergence(
                    fileName = fileName,
                    reason = "// FILE: 标记存在但解析为空",
                )
            }

            val packages = files.map { sub ->
                val pkg = extractPackageName(sub.content) ?: "default_pkg"
                val imports = extractImportPackages(sub.content)
                MultiFilePackage(
                    logicalFileName = sub.fileName,
                    content = sub.content,
                    packageName = pkg,
                    imports = imports,
                    fileLineOffset = sub.lineOffset,
                )
            }

            val sorted = topologicalSort(packages)
                ?: return VerificationResult.SyntaxDivergence(
                    fileName = fileName,
                    reason = "包依赖存在循环或无法解析的引用",
                )

            // 每个包对应一个目录与一个输出 staticlib
            val pkgDirs = mutableMapOf<String, File>()
            val pkgOutputs = mutableMapOf<String, File>()
            val outputDir = File(tempDir, "out").apply { mkdirs() }

            val allDiags = mutableListOf<CjcDiag>()
            // 对每个包：
            //  - 在 tempDir/<pkg>/ 写入文件
            //  - 调用 cjc -p ... --output-type=staticlib --no-sub-pkg
            //  - 后续依赖该包的编译命令通过 --import-path 引用 outputDir
            val packageNameToFiles = packages.groupBy { it.packageName }
            val sortedDistinct = LinkedHashSet<String>().apply {
                for (p in sorted) add(p.packageName)
            }
            for (pkgName in sortedDistinct) {
                val pkgFiles = packageNameToFiles[pkgName] ?: continue
                val pkgDir = File(tempDir, pkgName.replace('.', '_')).apply { mkdirs() }
                pkgDirs[pkgName] = pkgDir
                for (pf in pkgFiles) {
                    File(pkgDir, pf.logicalFileName).writeText(pf.content)
                }
                val outFile = File(outputDir, "lib${pkgName.replace('.', '_')}.a")
                pkgOutputs[pkgName] = outFile

                val importPaths = pkgFiles
                    .flatMap { it.imports }
                    .filter { pkgOutputs.containsKey(it) }
                    .map { outputDir }
                    .distinct()

                val result = CjcProcessRunner.compilePackage(
                    cjcPath = cjcPath,
                    packageDir = pkgDir,
                    outputFile = outFile,
                    importPaths = importPaths,
                )
                val diags = parseJsonOutput(result.output) ?: return VerificationResult.SyntaxDivergence(
                    fileName = fileName,
                    reason = "cjc 输出无法解析为 JSON：${result.output.take(200)}",
                )
                allDiags += diags
            }

            // 将所有诊断按 (logicalFileName -> 行号) 分组，再映射回原始文件行号。
            val actualByLine = mutableMapOf<Int, MutableSet<String>>()
            for (d in allDiags) {
                val loc = d.location ?: continue
                val baseName = File(loc.file).name
                val pkg = packages.firstOrNull { it.logicalFileName == baseName } ?: continue
                val originalLine = pkg.fileLineOffset + (loc.line - 1)
                actualByLine.getOrPut(originalLine) { mutableSetOf() }.add(d.diagKind)
            }

            return compareByLine(
                fileName = fileName,
                originalText = originalText,
                expectedByLine = expectedByLine,
                actualByLine = actualByLine,
                rawCjcDiags = allDiags,
            )
        } finally {
            tempDir.deleteRecursively()
        }
    }

    /**
     * 分析单次 cjc 编译结果并转换为 golden verifier 结果。
     *
     * 该方法负责解析 JSON、识别未标注 parse 错误，并将单文件诊断映射到原始行号。
     */
    private fun analyzeResult(
        fileName: String,
        originalText: String,
        expectedByLine: Map<Int, Set<String>>,
        result: CjcProcessRunner.CjcCompilationResult,
        compiledPath: String,
    ): VerificationResult {
        val diags = parseJsonOutput(result.output) ?: return VerificationResult.SyntaxDivergence(
            fileName = fileName,
            reason = "cjc 输出无法解析为 JSON：${result.output.take(200)}",
        )

        val hasParseError = diags.any { it.diagKind.startsWith("parse_") }
        if (hasParseError && expectedByLine.isEmpty()) {
            return VerificationResult.SyntaxDivergence(
                fileName = fileName,
                reason = "cjc 报告解析错误且测试未标注任何诊断",
            )
        }

        // 将诊断按行号分组
        val actualByLine = mutableMapOf<Int, MutableSet<String>>()
        for (d in diags) {
            val line = d.location?.line ?: continue
            // 单文件测试：JSON 中的 line 与清理后源码的行号一致
            // 而清理后源码与原始文件行号一致（标记和 suggested-message 都是行内替换）
            actualByLine.getOrPut(line) { mutableSetOf() }.add(d.diagKind)
        }

        return compareByLine(
            fileName = fileName,
            originalText = originalText,
            expectedByLine = expectedByLine,
            actualByLine = actualByLine,
            rawCjcDiags = diags,
        )
    }

    /**
     * 按行号比较期望诊断与 cjc 实际诊断。
     *
     * 当前 verifier 做行级模糊匹配：同一行存在任意期望诊断与任意实际诊断即视为覆盖，
     * 只报告整行缺失或整行多余的诊断集合。
     */
    private fun compareByLine(
        fileName: String,
        originalText: String,
        expectedByLine: Map<Int, Set<String>>,
        actualByLine: Map<Int, Set<String>>,
        rawCjcDiags: List<CjcDiag>,
    ): VerificationResult {
        val missing = mutableListOf<LineMismatch>()
        val unexpected = mutableListOf<LineMismatch>()

        val allLines = (expectedByLine.keys + actualByLine.keys).toSortedSet()
        for (line in allLines) {
            val expected = expectedByLine[line].orEmpty()
            val actual = actualByLine[line].orEmpty()
            if (expected.isNotEmpty() && actual.isEmpty()) {
                // 期望有诊断但 cjc 没报告
                missing += LineMismatch(line = line, expectedNames = expected, actualKinds = emptySet())
            } else if (expected.isEmpty() && actual.isNotEmpty()) {
                // 期望无诊断但 cjc 报错
                unexpected += LineMismatch(line = line, expectedNames = emptySet(), actualKinds = actual)
            }
            // expected.isNotEmpty() && actual.isNotEmpty() → 行级模糊匹配通过
        }

        if (missing.isEmpty() && unexpected.isEmpty()) {
            return VerificationResult.Ok(fileName = fileName, totalDiagnostics = rawCjcDiags.size)
        }
        return VerificationResult.Mismatch(
            fileName = fileName,
            originalText = originalText,
            missing = missing,
            unexpected = unexpected,
            rawCjcDiagnostics = rawCjcDiags,
        )
    }

    // ========== 解析辅助 ==========

    /**
     * 解析 `<!TAG!>` 标记，返回每行对应的标记名集合。
     *
     * 注意：清理 `/* suggested message: ... */` 不会改变行数，所以可以直接基于
     * 原始文本逐字符扫描，标记的起始位置即对应行号。
     */
    private fun parseExpectedDiagnosticsByLine(source: String): Map<Int, Set<String>> {
        val result = mutableMapOf<Int, MutableSet<String>>()
        val stack = mutableListOf<Pair<List<String>, Int>>() // names → startLine
        var line = 1
        var i = 0
        while (i < source.length) {
            if (source.startsWith("<!", i)) {
                val end = source.indexOf('>', i + 2)
                if (end < 0) break
                val payload = source.substring(i + 2, end).removeSuffix("!").trim()
                if (payload.isEmpty()) {
                    if (stack.isNotEmpty()) {
                        val (names, startLine) = stack.removeAt(stack.lastIndex)
                        for (name in names) {
                            result.getOrPut(startLine) { mutableSetOf() }.add(name)
                        }
                    }
                } else {
                    val names = splitTopLevelByComma(payload)
                        .asSequence()
                        .map { it.substringBefore("(").trim() }
                        .filter { it.isNotEmpty() }
                        .map { it.removePrefix("CFIR_").trim() }
                        .toList()
                    if (names.isNotEmpty()) {
                        stack += names to line
                    }
                }
                i = end + 1
                continue
            }
            if (source[i] == '\n') line++
            i++
        }
        return result
    }

    /**
     * 按顶层逗号切分内联诊断负载。
     *
     * 括号内逗号属于诊断参数，不参与分割。
     */
    private fun splitTopLevelByComma(raw: String): List<String> {
        val result = mutableListOf<String>()
        var depth = 0
        var start = 0
        for (i in raw.indices) {
            when (raw[i]) {
                '(' -> depth++
                ')' -> if (depth > 0) depth--
                ',' -> if (depth == 0) {
                    result += raw.substring(start, i)
                    start = i + 1
                }
            }
        }
        result += raw.substring(start)
        return result
    }

    /**
     * 从 cjc 进程输出中解析诊断 JSON。
     *
     * cjc 可能在 JSON 前输出额外文本，因此先截取第一个 `{` 后的内容；
     * 空输出按“无诊断”处理。
     */
    private fun parseJsonOutput(output: String): List<CjcDiag>? {
        // cjc 可能输出非 JSON 内容（比如崩溃栈、警告前缀），尝试在 `{` 起始处截取
        val start = output.indexOf('{')
        if (start < 0) {
            // 没有 JSON 输出 = 编译成功且无诊断
            if (output.isBlank()) return emptyList()
            return null
        }
        val candidate = output.substring(start)
        return try {
            CjcDiagnosticJsonParser.parse(candidate).diags
        } catch (e: Exception) {
            null
        }
    }

    private fun splitByFile(source: String): List<SubFile> {
        if (!source.contains("// FILE:")) return emptyList()
        val result = mutableListOf<SubFile>()
        val lines = source.lines()
        var currentName: String? = null
        var currentBuilder = StringBuilder()
        var currentStartLine = 1
        var currentLineNumber = 0
        for ((idx, l) in lines.withIndex()) {
            currentLineNumber = idx + 1
            val trimmed = l.trimStart()
            if (trimmed.startsWith("// FILE:")) {
                if (currentName != null) {
                    result += SubFile(
                        fileName = currentName,
                        content = currentBuilder.toString().trimEnd('\n'),
                        lineOffset = currentStartLine,
                    )
                }
                currentName = trimmed.removePrefix("// FILE:").trim()
                currentBuilder = StringBuilder()
                currentStartLine = idx + 2 // 下一行开始
                continue
            }
            if (currentName != null) {
                currentBuilder.append(l).append('\n')
            }
        }
        if (currentName != null) {
            result += SubFile(
                fileName = currentName,
                content = currentBuilder.toString().trimEnd('\n'),
                lineOffset = currentStartLine,
            )
        }
        return result
    }

    private val packageRegex = Regex("""^\s*package\s+([\w.]+)""", RegexOption.MULTILINE)
    private val importRegex = Regex("""^\s*import\s+([\w.]+)(?:\.\*)?""", RegexOption.MULTILINE)

    private fun extractPackageName(content: String): String? {
        return packageRegex.find(content)?.groupValues?.get(1)
    }

    private fun extractImportPackages(content: String): List<String> {
        return importRegex.findAll(content).map { it.groupValues[1] }.toList()
    }

    /**
     * 拓扑排序：返回按依赖顺序排好的包列表（被依赖的在前）。
     * 若存在环，返回 null。
     */
    private fun topologicalSort(packages: List<MultiFilePackage>): List<MultiFilePackage>? {
        val byPackage = packages.groupBy { it.packageName }
        val packageNames = byPackage.keys
        val edges = mutableMapOf<String, MutableSet<String>>() // pkg -> 依赖的包
        for ((pkg, files) in byPackage) {
            val deps = files.flatMap { it.imports }.filter { it in packageNames && it != pkg }.toMutableSet()
            edges[pkg] = deps
        }

        val sorted = mutableListOf<String>()
        val visited = mutableSetOf<String>()
        val visiting = mutableSetOf<String>()

        fun visit(p: String): Boolean {
            if (p in visited) return true
            if (p in visiting) return false
            visiting += p
            for (dep in edges[p].orEmpty()) {
                if (!visit(dep)) return false
            }
            visiting -= p
            visited += p
            sorted += p
            return true
        }

        for (p in packageNames) {
            if (!visit(p)) return null
        }
        // sorted 已是被依赖在前的顺序
        return sorted.flatMap { byPackage[it].orEmpty() }
    }

    // ========== 数据类 ==========

    private data class SubFile(
        val fileName: String,
        val content: String,
        val lineOffset: Int,
    )

    private data class MultiFilePackage(
        val logicalFileName: String,
        val content: String,
        val packageName: String,
        val imports: List<String>,
        val fileLineOffset: Int,
    )

    data class LineMismatch(
        val line: Int,
        val expectedNames: Set<String>,
        val actualKinds: Set<String>,
    )

    sealed class VerificationResult {
        /**
         * 当前验证结果对应的原始测试文件名。
         */
        abstract val fileName: String

        /**
         * 诊断 golden 验证通过。
         *
         * @property fileName 原始测试文件名。
         * @property totalDiagnostics cjc 输出的诊断总数。
         */
        data class Ok(
            /** 原始测试文件名。 */
            override val fileName: String,
            /** cjc 输出的诊断总数。 */
            val totalDiagnostics: Int,
        ) : VerificationResult()

        /**
         * 测试标记与 cjc 诊断不一致。
         *
         * @property fileName 原始测试文件名。
         * @property originalText 带内联标记的原始源码。
         * @property missing 测试标记存在但 cjc 未报告的行。
         * @property unexpected cjc 报告但测试未标注的行。
         * @property rawCjcDiagnostics cjc 原始诊断列表。
         */
        data class Mismatch(
            /** 原始测试文件名。 */
            override val fileName: String,
            /** 带内联标记的原始源码。 */
            val originalText: String,
            /** 测试标记存在但 cjc 未报告的行。 */
            val missing: List<LineMismatch>,
            /** cjc 报告但测试未标注的行。 */
            val unexpected: List<LineMismatch>,
            /** cjc 原始诊断列表。 */
            val rawCjcDiagnostics: List<CjcDiag>,
        ) : VerificationResult() {
            /**
             * 渲染适合断言失败输出的 mismatch 文本。
             */
            fun render(): String = buildString {
                appendLine("=== MISMATCH: $fileName ===")
                if (unexpected.isNotEmpty()) {
                    appendLine("[UNEXPECTED] cjc 报错但测试未标注：")
                    for (u in unexpected) {
                        appendLine("  line ${u.line}: cjc=${u.actualKinds.joinToString(", ")}")
                    }
                }
                if (missing.isNotEmpty()) {
                    appendLine("[MISSING] 测试标注但 cjc 未报错：")
                    for (m in missing) {
                        appendLine("  line ${m.line}: test=${m.expectedNames.joinToString(", ")}")
                    }
                }
            }
        }

        /**
         * 测试源码与官方 cjc 语法或输出格式存在分歧，无法进行普通诊断对比。
         *
         * @property fileName 原始测试文件名。
         * @property reason 分歧原因。
         */
        data class SyntaxDivergence(
            /** 原始测试文件名。 */
            override val fileName: String,
            /** 分歧原因。 */
            val reason: String,
        ) : VerificationResult()
    }
}
