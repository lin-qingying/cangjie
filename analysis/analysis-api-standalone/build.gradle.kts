import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

plugins {
    kotlin("jvm")
    id("java-test-fixtures")
    id("project-tests-convention")
    id("generated-sources")
}

description = "Standalone entrypoints for consuming the Cangjie frontend analysis API."

dependencies {
    compileOnly(intellijCore())

    api(project(":analysis:analysis-api"))
    api(project(":analysis:analysis-api-platform-interface"))
    implementation(project(":analysis:analysis-api-impl-base"))
    implementation(project(":analysis:decompiled:decompiler-to-psi"))
    implementation(project(":analysis:analysis-internal-utils"))
    implementation(project(":common"))
    implementation(project(":psi"))

    testImplementation(project(":analysis:analysis-api"))
    testImplementation(project(":analysis:analysis-api-standalone"))
    testImplementation(testFixtures(project(":analysis:analysis-api-cfir")))
    testImplementation(testFixtures(project(":analysis:analysis-api-impl-base")))
    testImplementation(testFixtures(project(":analysis:analysis-test-framework")))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)

    testFixturesApi(project(":analysis:analysis-api"))
    testFixturesApi(project(":analysis:analysis-api-standalone"))
    testFixturesApi(testFixtures(project(":analysis:analysis-api-cfir")))
    testFixturesApi(testFixtures(project(":analysis:analysis-api-impl-base")))
    testFixturesApi(testFixtures(project(":analysis:analysis-test-framework")))
    testFixturesApi(libs.junit.jupiter)
    testFixturesRuntimeOnly(libs.junit.platform.launcher)
}

sourceSets {
    "test" {
        projectDefault()
        generatedTestDir()
    }
    "testFixtures" { projectDefault() }
}

projectTests {
    testTask(jUnitMode = JUnitMode.JUnit5) {
        workingDir = rootDir
    }

    testGenerator("org.cangnova.cangjie.analysis.api.standalone.cfir.test.TestGeneratorKt")
}

/**
 * `analysis-api-standalone` API surface 中的一条公开声明。
 */
data class StandaloneApiSurfaceDeclaration(
    val ownerPath: String?,
    val signature: String,
)

/**
 * `analysis-api-standalone` API surface 中可以继续承载子声明的公开容器。
 */
data class StandaloneApiSurfaceContainer(
    val ownerPath: String,
)

/**
 * `analysis-api-standalone` 的源码级 API surface 提取器。
 *
 * 这里沿用 Analysis API 主模块的提取规则：
 * 1. 只提取 top-level 声明与公开容器下的直接公开成员。
 * 2. 函数体、属性实现体、lambda 局部作用域整体跳过，避免把局部变量误记为框架 API。
 * 3. companion object、跨行签名、默认参数按容器归属稳定建模。
 */
class StandaloneApiSurfaceExtractor(
    private val sourceRoot: File,
) {
    fun buildDump(moduleLabel: String): String {
        val renderedFiles = sourceRoot.walkTopDown()
            .filter { file -> file.isFile && file.extension == "kt" }
            .sortedBy { file -> file.relativeTo(sourceRoot).invariantSeparatorsPath }
            .mapNotNull(::renderFile)
            .toList()

        return buildString {
            appendLine("# $moduleLabel")
            appendLine()
            renderedFiles.forEachIndexed { index, rendered ->
                if (index > 0) appendLine()
                append(rendered)
            }
        }.trimEnd() + System.lineSeparator()
    }

    private fun renderFile(file: File): String? {
        val source = sanitizeSource(file.readText(Charsets.UTF_8))
        val declarations = extractDeclarations(source)
        if (declarations.isEmpty()) return null

        val packageName = source.lineSequence()
            .map(String::trim)
            .firstOrNull { line -> line.startsWith("package ") }
            ?.removePrefix("package ")
            ?: "<root>"

        return buildString {
            appendLine("## ${file.relativeTo(sourceRoot).invariantSeparatorsPath}")
            appendLine("package $packageName")
            declarations.forEach { declaration ->
                val prefix = declaration.ownerPath?.let { "$it." }.orEmpty()
                appendLine("$prefix${declaration.signature}")
            }
        }.trimEnd()
    }

    private fun extractDeclarations(source: String): List<StandaloneApiSurfaceDeclaration> {
        val declarations = mutableListOf<StandaloneApiSurfaceDeclaration>()
        val bracePairs = buildBracePairs(source)
        collectDeclarations(
            source = source,
            startIndex = 0,
            endIndex = source.length,
            ownerPath = null,
            bracePairs = bracePairs,
            declarations = declarations,
        )
        return declarations
    }

    private fun collectDeclarations(
        source: String,
        startIndex: Int,
        endIndex: Int,
        ownerPath: String?,
        bracePairs: IntArray,
        declarations: MutableList<StandaloneApiSurfaceDeclaration>,
    ) {
        val buffer = StringBuilder()
        var index = startIndex

        while (index < endIndex) {
            val ch = source[index]
            if (buffer.isEmpty()) {
                when {
                    ch.isWhitespace() -> {
                        index++
                        continue
                    }

                    ch == '@' -> {
                        index = skipToNextLine(source, index, endIndex)
                        continue
                    }

                    !startsWithDeclarationHead(source.substring(index, endIndex)) -> {
                        index++
                        continue
                    }
                }
            }

            when {
                ch == '{' -> {
                    val extracted = extractDeclaration(normalizeDeclarationHead(buffer.toString()), ownerPath)
                    if (extracted != null) {
                        declarations += extracted.declaration
                        extracted.container?.let { container ->
                            collectDeclarations(
                                source = source,
                                startIndex = index + 1,
                                endIndex = bracePairs[index],
                                ownerPath = container.ownerPath,
                                bracePairs = bracePairs,
                                declarations = declarations,
                            )
                        }
                    }
                    buffer.clear()
                    index = bracePairs[index] + 1
                }

                ch == '=' && currentNesting(buffer.toString()).isTopLevel -> {
                    extractDeclaration(normalizeDeclarationHead(buffer.toString()), ownerPath)?.let { declarations += it.declaration }
                    buffer.clear()
                    index = skipExpressionBody(source, index + 1, endIndex, bracePairs)
                }

                ch == '\n' || ch == ';' -> {
                    val candidate = buffer.toString()
                    if (shouldFlushAtLineBreak(source, index + 1, endIndex, candidate)) {
                        extractDeclaration(normalizeDeclarationHead(candidate), ownerPath)?.let { declarations += it.declaration }
                        buffer.clear()
                    } else if (candidate.isNotBlank()) {
                        buffer.append(' ')
                    }
                    index++
                }

                else -> {
                    buffer.append(ch)
                    index++
                }
            }
        }

        extractDeclaration(normalizeDeclarationHead(buffer.toString()), ownerPath)?.let { declarations += it.declaration }
    }

    private fun skipToNextLine(
        source: String,
        startIndex: Int,
        endIndex: Int,
    ): Int {
        var index = startIndex
        while (index < endIndex && source[index] != '\n') {
            index++
        }
        return if (index < endIndex) index + 1 else index
    }

    private fun skipExpressionBody(
        source: String,
        startIndex: Int,
        endIndex: Int,
        bracePairs: IntArray,
    ): Int {
        var index = startIndex
        var nesting = NestingState()
        while (index < endIndex) {
            val ch = source[index]
            when {
                ch == '{' -> index = bracePairs[index] + 1
                ch == '\n' || ch == ';' -> {
                    if (nesting.isTopLevel && startsWithDeclarationBoundary(source, index + 1, endIndex)) {
                        return index + 1
                    }
                    index++
                }

                else -> {
                    nesting = nesting.consume(ch)
                    index++
                }
            }
        }
        return index
    }

    private fun startsWithDeclarationBoundary(
        source: String,
        startIndex: Int,
        endIndex: Int,
    ): Boolean {
        var index = startIndex
        while (index < endIndex) {
            val ch = source[index]
            when {
                ch.isWhitespace() -> index++
                ch == '}' -> return true
                ch == '@' -> return true
                else -> {
                    val tail = source.substring(index, endIndex)
                    return startsWithDeclarationHead(tail)
                }
            }
        }
        return true
    }

    private fun shouldFlushAtLineBreak(
        source: String,
        nextIndex: Int,
        endIndex: Int,
        candidate: String,
    ): Boolean {
        val normalized = normalizeDeclarationHead(candidate)
        if (normalized.isBlank()) return false
        if (!startsWithDeclarationHead(normalized)) return false
        if (!currentNesting(normalized).isTopLevel) return false
        if (normalized.endsWith(",") || normalized.endsWith("=") || normalized.endsWith(":")) return false

        val nextToken = readNextToken(source, nextIndex, endIndex)
        return nextToken !in continuationTokens
    }

    private fun sanitizeSource(source: String): String {
        val withoutBlockComments = Regex("/\\*.*?\\*/", setOf(RegexOption.DOT_MATCHES_ALL))
            .replace(source, "")
        return withoutBlockComments.lineSequence()
            .map { line ->
                val commentIndex = line.indexOf("//")
                if (commentIndex >= 0) line.substring(0, commentIndex) else line
            }
            .joinToString(System.lineSeparator())
    }

    private fun normalizeDeclarationHead(text: String): String {
        val normalized = text.lineSequence()
            .map(String::trim)
            .filter { line ->
                line.isNotBlank() &&
                    !line.startsWith("@") &&
                    !line.startsWith("package ") &&
                    !line.startsWith("import ")
            }
            .joinToString(" ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .removeSuffix(":")
            .trim()

        if (normalized.isBlank()) return normalized
        return when {
            propertyHeadRegex.containsMatchIn(normalized) -> truncatePropertyTail(normalized)
            else -> normalized
        }
    }

    private fun truncatePropertyTail(head: String): String {
        var nesting = NestingState()
        var index = 0
        while (index < head.length) {
            val ch = head[index]
            nesting = nesting.consume(ch)
            if (nesting.isTopLevel && ch == ' ') {
                val remainder = head.substring(index + 1)
                if (remainder.startsWith("by ")) return head.substring(0, index).trim()
                if (remainder.startsWith("get(") || remainder.startsWith("get()")) return head.substring(0, index).trim()
                if (remainder.startsWith("set(")) return head.substring(0, index).trim()
            }
            index++
        }
        return head
    }

    private fun extractDeclaration(
        normalizedHead: String,
        ownerPath: String?,
    ): ExtractedDeclaration? {
        if (normalizedHead.isBlank()) return null
        if (!startsWithDeclarationHead(normalizedHead)) return null
        if (nonPublicModifierRegex.containsMatchIn(normalizedHead)) return null

        val declarationKind = declarationKindRegex.find(normalizedHead)?.groupValues?.get(1) ?: return null
        val containerName = extractContainerName(normalizedHead, declarationKind)
        val declaration = StandaloneApiSurfaceDeclaration(
            ownerPath = ownerPath,
            signature = ensurePublicModifier(normalizedHead),
        )
        val container = if (declarationKind in containerKinds && containerName != null) {
            StandaloneApiSurfaceContainer(
                ownerPath = listOfNotNull(ownerPath, containerName).joinToString("."),
            )
        } else {
            null
        }

        return ExtractedDeclaration(
            declaration = declaration,
            container = container,
        )
    }

    private fun extractContainerName(
        head: String,
        declarationKind: String,
    ): String? {
        if (declarationKind !in containerKinds) return null
        companionObjectRegex.find(head)?.let { match ->
            return match.groupValues.getOrNull(1)?.ifBlank { null } ?: "Companion"
        }
        val match = containerNameRegex.find(head) ?: return null
        return match.groupValues.drop(1).firstOrNull(String::isNotBlank)
    }

    private fun ensurePublicModifier(head: String): String {
        val normalized = head.trim()
        return if (normalized.startsWith("public ")) normalized else "public $normalized"
    }

    private fun startsWithDeclarationHead(text: String): Boolean {
        val match = declarationHeadRegex.find(text) ?: return false
        return match.range.first == 0
    }

    private fun currentNesting(text: String): NestingState {
        var nesting = NestingState()
        text.forEach { ch -> nesting = nesting.consume(ch) }
        return nesting
    }

    private fun readNextToken(
        source: String,
        startIndex: Int,
        endIndex: Int,
    ): String? {
        var index = startIndex
        while (index < endIndex) {
            val ch = source[index]
            if (!ch.isWhitespace()) {
                if (ch == ':' || ch == '@') return ch.toString()
                val tokenStart = index
                while (index < endIndex && (source[index].isLetterOrDigit() || source[index] == '_')) {
                    index++
                }
                return source.substring(tokenStart, index)
            }
            index++
        }
        return null
    }

    private fun buildBracePairs(source: String): IntArray {
        val bracePairs = IntArray(source.length) { -1 }
        val stack = ArrayDeque<Int>()
        source.forEachIndexed { index, ch ->
            when (ch) {
                '{' -> stack.addLast(index)
                '}' -> {
                    val openIndex = stack.removeLastOrNull()
                        ?: error("API surface 提取器遇到不匹配的 `}`，位置：$index")
                    bracePairs[openIndex] = index
                }
            }
        }
        check(stack.isEmpty()) { "API surface 提取器遇到不匹配的 `{`" }
        return bracePairs
    }

    private data class ExtractedDeclaration(
        val declaration: StandaloneApiSurfaceDeclaration,
        val container: StandaloneApiSurfaceContainer?,
    )

    private data class NestingState(
        val parenthesis: Int = 0,
        val angle: Int = 0,
        val square: Int = 0,
    ) {
        val isTopLevel: Boolean
            get() = parenthesis == 0 && angle == 0 && square == 0

        fun consume(ch: Char): NestingState {
            return when (ch) {
                '(' -> copy(parenthesis = parenthesis + 1)
                ')' -> copy(parenthesis = (parenthesis - 1).coerceAtLeast(0))
                '<' -> copy(angle = angle + 1)
                '>' -> copy(angle = (angle - 1).coerceAtLeast(0))
                '[' -> copy(square = square + 1)
                ']' -> copy(square = (square - 1).coerceAtLeast(0))
                else -> this
            }
        }
    }

    private companion object {
        private val declarationHeadRegex = Regex(
            """^(?:public\s+|private\s+|protected\s+|internal\s+|open\s+|abstract\s+|final\s+|sealed\s+|data\s+|enum\s+|annotation\s+|value\s+|operator\s+|infix\s+|inline\s+|suspend\s+|override\s+|const\s+|lateinit\s+|companion\s+)*\b(?:interface|class|object|fun|val|var|typealias)\b""",
        )
        private val declarationKindRegex = Regex("""\b(interface|class|object|fun|val|var|typealias)\b""")
        private val nonPublicModifierRegex = Regex("""\b(private|protected|internal)\b""")
        private val propertyHeadRegex = Regex("""\b(val|var)\b""")
        private val companionObjectRegex = Regex("""\bcompanion\s+object(?:\s+([A-Za-z_][A-Za-z0-9_]*))?\b""")
        private val containerNameRegex = Regex(
            """\b(?:interface|class|object)\s+([A-Za-z_][A-Za-z0-9_]*)\b|\b(?:sealed|data|enum|annotation|value)\s+class\s+([A-Za-z_][A-Za-z0-9_]*)\b""",
        )
        private val containerKinds = setOf("interface", "class", "object")
        private val continuationTokens = setOf(":", "where")
    }
}

/**
 * 生成 `analysis-api-standalone` API surface dump 的任务。
 */
abstract class GenerateStandaloneApiSurfaceDumpTask : DefaultTask() {
    @get:Input
    abstract val moduleLabel: Property<String>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceRoot: DirectoryProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val dumpText = StandaloneApiSurfaceExtractor(sourceRoot.get().asFile).buildDump(moduleLabel.get())
        val targetFile = outputFile.get().asFile
        targetFile.parentFile.mkdirs()
        targetFile.writeText(dumpText, Charsets.UTF_8)
    }
}

/**
 * 校验 `analysis-api-standalone` API surface dump 的任务。
 */
abstract class CheckStandaloneApiSurfaceDumpTask : DefaultTask() {
    @get:Input
    abstract val moduleLabel: Property<String>

    @get:Input
    abstract val projectPathLabel: Property<String>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceRoot: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val dumpFile: RegularFileProperty

    @TaskAction
    fun checkDump() {
        val expectedFile = dumpFile.get().asFile
        check(expectedFile.isFile) {
            "缺少 API surface dump：${expectedFile.invariantSeparatorsPath}。请先运行 generateApiSurfaceDump。"
        }

        val expected = expectedFile.readText(Charsets.UTF_8)
        val actual = StandaloneApiSurfaceExtractor(sourceRoot.get().asFile).buildDump(moduleLabel.get())
        check(expected == actual) {
            buildString {
                appendLine("${projectPathLabel.get()} 的公开 API surface dump 已过期。")
                appendLine("请运行 ${projectPathLabel.get()}:generateApiSurfaceDump 更新基线。")
                appendLine("dump 文件：${expectedFile.invariantSeparatorsPath}")
            }
        }
    }
}

val apiSurfaceDumpFile = layout.projectDirectory.file("api/analysis-api-standalone.txt")
val apiSourceRoot = layout.projectDirectory.dir("src")

val generateApiSurfaceDump by tasks.registering(GenerateStandaloneApiSurfaceDumpTask::class) {
    group = "verification"
    description = "生成 analysis-api-standalone 的公开 API surface dump。"
    moduleLabel.set("analysis-api-standalone")
    sourceRoot.set(apiSourceRoot)
    outputFile.set(apiSurfaceDumpFile)
}

val checkApiSurfaceDump by tasks.registering(CheckStandaloneApiSurfaceDumpTask::class) {
    group = "verification"
    description = "校验 analysis-api-standalone 当前源码与公开 API surface dump 一致。"
    moduleLabel.set("analysis-api-standalone")
    projectPathLabel.set(path)
    sourceRoot.set(apiSourceRoot)
    dumpFile.set(generateApiSurfaceDump.flatMap { it.outputFile })
}

tasks.named("check") {
    dependsOn(checkApiSurfaceDump)
}
