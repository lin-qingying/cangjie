package org.cangnova.cangjie.codegen.parity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * 将本项目生成的 LLVM IR 与预生成官方 LLVM baseline 对比的测试。
 */
class OfficialCompilerLlvmIrComparisonTest {
    /**
     * 暂时跳过的官方样例名称集合。
     */
    private val temporarilyUnsupportedSamples = emptySet<String>()

    /**
     * 样例名到官方 IR 中主函数名的映射。
     */
    private val sampleToOfficialFunction = mapOf(
        "floatCompare" to "cmpMain",
        "floatCastChain" to "castMain",
        "unaryLogicNot" to "logicNotMain",
        "unsignedChain" to "unsignedMain",
    )

    /**
     * 官方比较使用的 LLVM IR comparator。
     */
    private val comparator = LlvmIrParityComparator(
        LlvmIrNormalizationOptions(
            sortTopLevelDeclarations = false,
            collapseEmptyLines = true,
            ignoreCommentLines = true,
        ),
    )

    /**
     * 对比本项目生成 IR 与官方预生成 IR 的 canonical main 函数。
     */
    @Test
    fun `compare project llvm ir against pre-generated official llvm txt`() {
        val repoRoot = detectRepoRoot() ?: run {
            assumeTrue(false, "skip llvm parity: repo root not found")
            return
        }
        val testDataRoot = repoRoot.resolve("compiler/codegen/testData/chirParity")
        val officialRoot = repoRoot.resolve("compiler/codegen/testData/chirParity/official-generated-llvm")

        assumeTrue(testDataRoot.exists(), "skip llvm parity: missing test data root $testDataRoot")
        require(officialRoot.exists()) { "missing official baseline root: $officialRoot" }
        compareAgainstOfficialRoot(testDataRoot, officialRoot)
    }

    /**
     * 验证优化级官方 baseline 目录覆盖所有 CHIR parity fixture。
     */
    @Test
    fun `optimized official baseline sets should cover all fixtures`() {
        val repoRoot = detectRepoRoot() ?: run {
            assumeTrue(false, "skip llvm parity: repo root not found")
            return
        }
        val testDataRoot = repoRoot.resolve("compiler/codegen/testData/chirParity")
        val optimizedRoots = listOf(
            repoRoot.resolve("compiler/codegen/testData/chirParity/official-generated-llvm-o1"),
            repoRoot.resolve("compiler/codegen/testData/chirParity/official-generated-llvm-o2"),
        )

        val fixtures = testDataRoot.walkTopDown()
            .filter { it.isFile && it.extension == "json" && it.name.endsWith(".chir.json") }
            .map { file ->
                testDataRoot.toPath().relativize(file.toPath()).toString()
                    .replace('\\', '/')
                    .removeSuffix(".chir.json")
            }
            .toSet()

        optimizedRoots.forEach { root ->
            require(root.exists()) { "missing optimized official baseline root: $root" }
            val baselines = root.walkTopDown()
                .filter { it.isFile && it.extension == "txt" }
                .map { file ->
                    root.toPath().relativize(file.toPath()).toString()
                        .replace('\\', '/')
                        .removeSuffix(".txt")
                }
                .toSet()

            assertEquals(
                fixtures,
                baselines,
                "optimized baseline coverage mismatch for root ${root.name}",
            )
        }
    }

    /**
     * 在指定官方 baseline 根目录下逐文件执行 IR 对比。
     */
    private fun compareAgainstOfficialRoot(testDataRoot: File, officialRoot: File) {
        val runner = Runner()
        var compared = 0
        val officialBaselines = officialRoot.walkTopDown()
            .filter { it.isFile && it.extension == "txt" }
            .toList()

        officialBaselines.forEach { officialTxt ->
            val baselineRelative = officialRoot.toPath().relativize(officialTxt.toPath()).toString()
                .replace('\\', '/')
            if (baselineRelative == "README.md") return@forEach

            val fixtureRelative = baselineRelative.removeSuffix(".txt") + ".chir.json"
            val fixture = File(testDataRoot, fixtureRelative.replace('/', File.separatorChar))
            require(fixture.exists()) {
                "missing fixture for official baseline: $baselineRelative (expected: $fixture)"
            }
            val sample = fixture.name.removeSuffix(".chir.json")
            if (sample in temporarilyUnsupportedSamples) return@forEach

            val fixtureText = fixture.readText(StandardCharsets.UTF_8)
            val projectIr = runner.generateIrFromFixtureText(fixtureText)
            val officialIr = officialTxt.readText(StandardCharsets.UTF_8)
            assertExternalInteropInvariants(sample, projectIr)

            val projectMain = extractCanonicalFunction(projectIr, preferredName = "main")
            val officialMain = extractCanonicalFunction(
                officialIr,
                preferredName = sampleToOfficialFunction[sample] ?: "main",
                fallbackName = "main",
            )

            assertEquals(
                officialMain.returnType,
                projectMain.returnType,
                "main return type mismatch for sample $sample in root ${officialRoot.name}",
            )
            assertEquals(
                officialMain.parameterTypes,
                projectMain.parameterTypes,
                "main parameter list mismatch for sample $sample in root ${officialRoot.name}",
            )
            assertFalse(
                projectMain.returnSignatures.isEmpty(),
                "project main has no return value for sample $sample in root ${officialRoot.name}",
            )
            assertEquals(
                officialMain.returnSignatures,
                projectMain.returnSignatures,
                "main return values mismatch for sample $sample in root ${officialRoot.name}",
            )

            val textResult = comparator.compare(
                expected = officialMain.canonicalText,
                actual = projectMain.canonicalText,
            )
            assertEquals(
                true,
                textResult.matches,
                buildString {
                    appendLine("main canonical text mismatch for sample $sample in root ${officialRoot.name}")
                    appendLine(comparator.formatFirstDiffReport(textResult))
                    appendLine(formatOpcodeDiffSummary(officialMain, projectMain))
                    appendLine(formatInstructionWindowSummary(officialMain, projectMain))
                }.trimEnd(),
            )
            compared++
        }

        require(compared > 0) { "no official baseline files under $officialRoot" }
    }

    /**
     * 从 LLVM IR 中抽取出的 canonical main 函数信息。
     */
    private data class CanonicalMainFunction(
        /**
         * 函数返回类型。
         */
        val returnType: String,
        /**
         * 函数参数类型列表。
         */
        val parameterTypes: List<String>,
        /**
         * canonical return 指令签名集合。
         */
        val returnSignatures: Set<String>,
        /**
         * canonical 化后的函数体文本。
         */
        val canonicalText: String,
        /**
         * 函数体指令 opcode 序列。
         */
        val opcodeSequence: List<String>,
        /**
         * 函数体指令文本序列。
         */
        val instructionSequence: List<String>,
    )

    /**
     * 从 LLVM IR 中提取指定 main 函数的 canonical 表示。
     */
    private fun extractCanonicalFunction(
        ir: String,
        preferredName: String,
        fallbackName: String? = null,
    ): CanonicalMainFunction {
        val normalizedIr = ir.replace("\r\n", "\n")
        val functionRegex = """define\s+([^@\s]+)\s+(@[^(\s]+|@"[^"]+")\(([^)]*)\)\s*[^{]*\{([\s\S]*?)\n\}""".toRegex()
        val matches = functionRegex.findAll(normalizedIr).toList()
        val match = findMatchingFunction(matches, preferredName)
            ?: fallbackName?.let { findMatchingFunction(matches, it) }
            ?: error("IR does not contain expected function '$preferredName'")

        val returnType = match.groupValues[1].trim()
        val parameterTypes = parseParameterTypes(match.groupValues[3])
        val body = match.groupValues[4]
        val canonicalLines = canonicalizeMainBodyLines(body)
        val returnSignatures = extractCanonicalReturnSignatures(canonicalLines)
            .ifEmpty { extractReturnSignatures(body) }
            .toSet()

        return CanonicalMainFunction(
            returnType = returnType,
            parameterTypes = parameterTypes,
            returnSignatures = returnSignatures,
            canonicalText = canonicalLines.joinToString("\n"),
            opcodeSequence = extractOpcodeSequence(body),
            instructionSequence = extractInstructionSequence(body),
        )
    }

    /**
     * 在正则匹配结果中查找指定函数名的无参函数。
     */
    private fun findMatchingFunction(matches: List<MatchResult>, functionName: String): MatchResult? {
        val signatureName = "<$functionName>"
        return matches.firstOrNull { match ->
            val symbol = match.groupValues[2]
            val normalizedSymbol = symbol.removePrefix("@").trim('"')
            val hasNoParams = match.groupValues[3].trim().isEmpty()
            hasNoParams && (
                symbol == "@$functionName" ||
                    symbol.contains(signatureName) ||
                    normalizedSymbol == functionName ||
                    normalizedSymbol.contains(functionName)
                )
        }
    }

    /**
     * 格式化 opcode 计数和首个 opcode 序列差异摘要。
     */
    private fun formatOpcodeDiffSummary(expected: CanonicalMainFunction, actual: CanonicalMainFunction): String {
        val expectedCounts = expected.opcodeSequence.groupingBy { it }.eachCount()
        val actualCounts = actual.opcodeSequence.groupingBy { it }.eachCount()
        val allOpcodes = (expectedCounts.keys + actualCounts.keys).toSortedSet()

        val missing = allOpcodes
            .mapNotNull { opcode ->
                val left = expectedCounts[opcode] ?: 0
                val right = actualCounts[opcode] ?: 0
                if (left > right) "$opcode(expected=$left, actual=$right)" else null
            }
        val unexpected = allOpcodes
            .mapNotNull { opcode ->
                val left = expectedCounts[opcode] ?: 0
                val right = actualCounts[opcode] ?: 0
                if (right > left) "$opcode(expected=$left, actual=$right)" else null
            }

        val firstSequenceMismatch = findFirstSequenceMismatch(expected.opcodeSequence, actual.opcodeSequence)
        return buildString {
            appendLine("opcode-count expected=$expectedCounts")
            appendLine("opcode-count actual  =$actualCounts")
            appendLine("opcode-missing      =${if (missing.isEmpty()) "<none>" else missing.joinToString(", ")}")
            appendLine("opcode-unexpected   =${if (unexpected.isEmpty()) "<none>" else unexpected.joinToString(", ")}")
            if (firstSequenceMismatch != null) {
                appendLine(
                    "opcode-first-mismatch=index=${firstSequenceMismatch.index}," +
                        " expected=${firstSequenceMismatch.expected ?: "<end>"}," +
                        " actual=${firstSequenceMismatch.actual ?: "<end>"}",
                )
            }
        }.trimEnd()
    }

    /**
     * opcode 序列首个差异位置。
     */
    private data class OpcodeMismatch(val index: Int, val expected: String?, val actual: String?)

    /**
     * 查找两个 opcode 序列的首个差异。
     */
    private fun findFirstSequenceMismatch(expected: List<String>, actual: List<String>): OpcodeMismatch? {
        val max = maxOf(expected.size, actual.size)
        for (i in 0 until max) {
            val left = expected.getOrNull(i)
            val right = actual.getOrNull(i)
            if (left != right) return OpcodeMismatch(i, left, right)
        }
        return null
    }

    /**
     * 格式化首个 opcode 差异附近的指令窗口。
     */
    private fun formatInstructionWindowSummary(expected: CanonicalMainFunction, actual: CanonicalMainFunction): String {
        val center = findFirstSequenceMismatch(expected.opcodeSequence, actual.opcodeSequence)?.index
            ?: findFirstSequenceMismatch(expected.instructionSequence, actual.instructionSequence)?.index
            ?: return "instruction-window: <no mismatch>"
        val radius = 4
        val start = maxOf(0, center - radius)
        val endExclusive = maxOf(
            center + radius + 1,
            minOf(maxOf(expected.instructionSequence.size, actual.instructionSequence.size), center + radius + 1),
        )
        val expectedWindow = formatWindowLines(expected.instructionSequence, start, endExclusive)
        val actualWindow = formatWindowLines(actual.instructionSequence, start, endExclusive)
        return buildString {
            appendLine("instruction-window center=$center range=[$start, ${endExclusive - 1}]")
            appendLine("expected-window:")
            appendLine(expectedWindow)
            appendLine("actual-window:")
            append(actualWindow)
        }.trimEnd()
    }

    /**
     * 格式化指定区间的指令窗口行。
     */
    private fun formatWindowLines(lines: List<String>, start: Int, endExclusive: Int): String {
        if (start >= endExclusive) return "<empty>"
        return (start until endExclusive).joinToString("\n") { index ->
            val value = lines.getOrNull(index) ?: "<end>"
            "[$index] $value"
        }
    }

    /**
     * 从 LLVM 函数参数文本中抽取参数类型列表。
     */
    private fun parseParameterTypes(rawParameters: String): List<String> {
        val text = rawParameters.trim()
        if (text.isEmpty()) return emptyList()
        return text.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { parameter ->
                parameter.substringBeforeLast(' ').trim().ifEmpty { parameter }
            }
    }

    /**
     * 校验外部互操作相关样例必须保留的 IR 模式。
     */
    private fun assertExternalInteropInvariants(sample: String, projectIr: String) {
        when (sample) {
            "importedCall" -> {
                assertPattern(
                    text = projectIr,
                    pattern = """declare\s+fastcc\s+i32\s+@ext_add\(\s*i32(?:\s+\w+)?\s*\)""".toRegex(),
                    message = "importedCall must declare fastcc ext_add(i32)",
                )
                assertPattern(
                    text = projectIr,
                    pattern = """call\s+fastcc\s+i32\s+@ext_add\(""".toRegex(),
                    message = "importedCall must call ext_add with fastcc",
                )
            }

            "voidCall" -> {
                assertPattern(
                    text = projectIr,
                    pattern = """declare\s+void\s+@ext_log\(\s*i32(?:\s+\w+)?\s*\)""".toRegex(),
                    message = "voidCall must declare ext_log(i32)",
                )
                assertPattern(
                    text = projectIr,
                    pattern = """call\s+void\s+@ext_log\(""".toRegex(),
                    message = "voidCall must call ext_log",
                )
            }

            "tailCallCc" -> {
                assertPattern(
                    text = projectIr,
                    pattern = """declare\s+fastcc\s+i32\s+@ext_fast\(\s*i32(?:\s+\w+)?\s*\)""".toRegex(),
                    message = "tailCallCc must declare fastcc ext_fast(i32)",
                )
                assertPattern(
                    text = projectIr,
                    pattern = """tail\s+call\s+fastcc\s+i32\s+@ext_fast\(""".toRegex(),
                    message = "tailCallCc must emit tail call fastcc to ext_fast",
                )
            }
        }
    }

    /**
     * 断言文本匹配指定正则模式。
     */
    private fun assertPattern(text: String, pattern: Regex, message: String) {
        assertTrue(pattern.containsMatchIn(text), message)
    }

    /**
     * 将 main 函数体规范化为可与官方 baseline 比较的行序列。
     */
    private fun canonicalizeMainBodyLines(functionBody: String): List<String> {
        val literalByPointer = mutableMapOf<String, String>()
        val literalByRegister = mutableMapOf<String, String>()
        val canonical = mutableListOf<String>()

        functionBody.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .forEach { line ->
                if (line.endsWith(":")) return@forEach
                if (line.startsWith("br label ")) return@forEach

                val sanitized = line
                    .replace(""",\s*!dbg\s*!\d+""".toRegex(), "")
                    .replace(""",\s*align\s+\d+""".toRegex(), "")

                val storeLiteral = """store\s+i32\s+(-?\d+),\s+i32\*\s+([%]\w+)""".toRegex().find(sanitized)
                if (storeLiteral != null) {
                    literalByPointer[storeLiteral.groupValues[2]] = storeLiteral.groupValues[1]
                    return@forEach
                }

                val loadLiteral = """([%]\w+)\s*=\s*load\s+i32,\s+i32\*\s+([%]\w+)""".toRegex().find(sanitized)
                if (loadLiteral != null) {
                    val value = literalByPointer[loadLiteral.groupValues[2]]
                    if (value != null) {
                        literalByRegister[loadLiteral.groupValues[1]] = value
                    }
                    return@forEach
                }

                val retViaRegister = """ret\s+i32\s+([%]\w+)""".toRegex().find(sanitized)
                if (retViaRegister != null) {
                    val register = retViaRegister.groupValues[1]
                    val literal = literalByRegister[register]
                    if (literal != null) {
                        canonical += "ret i32 $literal"
                        return@forEach
                    }
                }

                val retLiteral = """ret\s+i32\s+(-?\d+)""".toRegex().find(sanitized)
                if (retLiteral != null) {
                    canonical += "ret i32 ${retLiteral.groupValues[1]}"
                    return@forEach
                }
            }

        return canonical
    }

    /**
     * 从原始函数体中抽取 return 指令签名。
     */
    private fun extractReturnSignatures(functionBody: String): List<String> {
        return functionBody.lineSequence()
            .map { it.trim() }
            .filter { it.startsWith("ret ") }
            .map { line ->
                line.replace(""",\s*!dbg\s*!\d+""".toRegex(), "")
                    .replace(""",\s*align\s+\d+""".toRegex(), "")
                    .replace("""%[\w.]+""".toRegex(), "%reg")
                    .replace("""\s+""".toRegex(), " ")
                    .trim()
            }
            .toList()
    }

    /**
     * 从 canonical 行中抽取 return 指令签名。
     */
    private fun extractCanonicalReturnSignatures(canonicalLines: List<String>): List<String> {
        return canonicalLines.filter { it.startsWith("ret ") }
    }

    /**
     * 抽取函数体中所有指令 opcode。
     */
    private fun extractOpcodeSequence(functionBody: String): List<String> {
        return functionBody.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.endsWith(":") && !it.startsWith(";") }
            .mapNotNull { line ->
                val instruction = line
                    .replace(""",\s*!dbg\s*!\d+""".toRegex(), "")
                    .substringAfter("=", line)
                    .trim()
                val opcode = instruction.substringBefore(' ').trim()
                opcode.ifEmpty { null }
            }
            .toList()
    }

    /**
     * 抽取函数体中所有指令文本。
     */
    private fun extractInstructionSequence(functionBody: String): List<String> {
        return functionBody.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.endsWith(":") && !it.startsWith(";") }
            .map { line ->
                line.replace(""",\s*!dbg\s*!\d+""".toRegex(), "")
                    .replace(""",\s*align\s+\d+""".toRegex(), "")
            }
            .toList()
    }

    /**
     * 从当前工作目录向上查找仓库根目录。
     */
    private fun detectRepoRoot(): File? {
        var current = File(System.getProperty("user.dir")).absoluteFile
        while (true) {
            if (File(current, "settings.gradle.kts").exists()) return current
            val parent = current.parentFile ?: return null
            current = parent
        }
    }

    /**
     * 测试内部复用 `AbstractCodegenParityTestCase` 生成 fixture IR 的 runner。
     */
    private class Runner : AbstractCodegenParityTestCase() {
        /**
         * 直接从 fixture 文本生成 LLVM IR。
         */
        fun generateIrFromFixtureText(fixtureText: String): String = generateFixtureIr(fixtureText)
    }
}
