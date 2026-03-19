package org.cangnova.cangjie.cfir.builder

import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.common.CfirPlatform
import org.cangnova.cangjie.cfir.common.CfirSourceModuleData
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.renderer.CfirRenderer
import org.cangnova.cangjie.cfir.resolve.providers.CfirBuiltinSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompositeSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirProviderImpl
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjFile
import java.nio.file.Path
import java.io.File

/**
 * Raw CFIR 构建测试入口基类。
 */
abstract class AbstractRawCfirBuilderTestCase : CjParsingTestCase(
    "",
    "cj",
    CangJieFileType.INSTANCE,
    CangJieParserDefinition(),
) {

    protected fun runTest(testDataFilePath: String) {
        doRawCfirTest(testDataFilePath)
    }

    protected fun createCjFile(name: String, text: String = ""): CjFile {
        return createPsiFile("$name.cj", text) as CjFile
    }

    protected fun createTestSession(): CfirSession {
        return object : CfirSession(Kind.Source) {}.also { session ->
            val moduleData = CfirSourceModuleData(
                name = Name.identifier("<test>"),
                dependencies = emptyList(),
                refinementDependencies = emptyList(),
                platform = CfirPlatform.DEFAULT,
            ).apply {
                bindSession(session)
            }
            session.register(CfirModuleData::class, moduleData)

            // Align test session baseline with source-session wiring used by entrypoint factories.
            val scopeProvider = CfirCangJieScopeProvider()
            session.register(CfirCangJieScopeProvider::class, scopeProvider)

            val sourceProvider = CfirProviderImpl(session, scopeProvider)
            session.register(CfirProvider::class, sourceProvider)

            val symbolProvider = CfirCompositeSymbolProvider(
                session,
                listOf(
                    sourceProvider.symbolProvider,
                    CfirBuiltinSymbolProvider(session),
                ),
            )
            session.register(CfirSymbolProvider::class, symbolProvider)
        }
    }

    protected fun CjFile.toCfirFile(
        session: CfirSession = createTestSession(),
        bodyBuildingMode: BodyBuildingMode = BodyBuildingMode.NORMAL,
    ): CfirFile {
        return PsiRawCfirBuilder(session, bodyBuildingMode).buildCfirFile(this).also { cfirFile ->
            (runCatching { session.cfirProvider }.getOrNull() as? CfirProviderImpl)?.recordFile(cfirFile)
        }
    }

    protected fun dumpCfirFile(cfirFile: CfirFile): String {
        return CfirRenderer.withGoldenCompat().renderElementAsString(cfirFile)
    }

    protected fun assertAllFilesPresentByMetadata(testDataRootRelativePath: String) {
        val testDataDir = resolveTestDataPath(testDataRootRelativePath)
        require(testDataDir.isDirectory) { "testData dir not found: ${testDataDir.path}" }

        val currentDir = currentClassTestDataDir(testDataDir)
        val expected = currentDir.walkTopDown()
            .filter { it.isFile && it.extension == "cj" }
            .map { it.relativeTo(currentDir).invariantSeparatorsPath }
            .toSet()

        val covered = collectCoveredRelativePaths(currentDir)
        val missing = expected - covered
        check(missing.isEmpty()) {
            "Missing generated tests for testData files in ${currentDir.path}: ${missing.sorted()}"
        }

        validateRawBuilderConventionsIfNeeded(testDataDir)
    }

    private fun currentClassTestDataDir(rootTestDataDir: File): File {
        val classMetadata = this::class.java.getAnnotation(org.cangnova.cangjie.test.TestMetadata::class.java)
        if (classMetadata != null) {
            val metadataPath = classMetadata.value.replace('\\', '/')
            val candidate = File(metadataPath)
            if (candidate.isDirectory) return candidate
            val nestedCandidate = rootTestDataDir.resolve(metadataPath)
            if (nestedCandidate.isDirectory) return nestedCandidate
        }
        return rootTestDataDir
    }

    private fun collectCoveredRelativePaths(testDataDir: File): Set<String> {
        val annotationRegex = "@TestMetadata\\(\"([^\"]+\\.cj)\"\\)".toRegex()
        val runTestRegex = "runTest\\(\"([^\"]+\\.cj)\"\\)".toRegex()
        val covered = linkedSetOf<String>()

        collectCoveredFromClass(this::class.java, testDataDir, testDataDir, covered)

        val testSources = resolveTestDataPath("cfir/raw-cfir/psi2cfir/tests-gen/org/cangjie/cfir/builder")
        if (testSources.isDirectory) {
            testSources.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { generatedFile ->
                val content = generatedFile.readText(Charsets.UTF_8)
                annotationRegex.findAll(content).forEach { match ->
                    val metadataPath = match.groupValues[1]
                    val candidate = testDataDir.resolve(metadataPath)
                    if (candidate.isFile && candidate.extension == "cj") {
                        covered += candidate.relativeTo(testDataDir).invariantSeparatorsPath
                    }
                }
                runTestRegex.findAll(content).forEach { match ->
                    val runPath = match.groupValues[1].replace('\\', '/')
                    val testDirPath = testDataDir.invariantSeparatorsPath
                    val idx = runPath.indexOf(testDirPath)
                    if (idx >= 0) {
                        val subPath = runPath.substring(idx + testDirPath.length).trimStart('/')
                        if (subPath.endsWith(".cj")) {
                            covered += subPath
                        }
                    }
                }
            }
        }

        return covered
    }

    private fun collectCoveredFromClass(
        klass: Class<*>,
        rootTestDataDir: File,
        inheritedDir: File,
        covered: MutableSet<String>,
    ) {
        val classScopedDir = classScopedDir(klass, rootTestDataDir, inheritedDir)
        for (method in klass.declaredMethods) {
            val metadata = method.getAnnotation(org.cangnova.cangjie.test.TestMetadata::class.java) ?: continue
            val metadataPath = metadata.value.replace('\\', '/')
            val candidate = classScopedDir.resolve(metadataPath)
            if (candidate.isFile && candidate.extension == "cj" && candidate.isUnder(rootTestDataDir)) {
                covered += candidate.relativeTo(rootTestDataDir).invariantSeparatorsPath
            }
        }
        for (nested in klass.declaredClasses) {
            collectCoveredFromClass(nested, rootTestDataDir, classScopedDir, covered)
        }
    }

    private fun classScopedDir(
        klass: Class<*>,
        rootTestDataDir: File,
        inheritedDir: File,
    ): File {
        val classMetadata =
            klass.getAnnotation(org.cangnova.cangjie.test.TestMetadata::class.java) ?: return inheritedDir
        val metadataPath = classMetadata.value.replace('\\', '/')
        val direct = resolveTestDataPath(metadataPath)
        if (direct.isDirectory) return direct
        val nested = rootTestDataDir.resolve(metadataPath)
        if (nested.isDirectory) return nested
        val inheritedNested = inheritedDir.resolve(metadataPath)
        if (inheritedNested.isDirectory) return inheritedNested
        return inheritedDir
    }

    private fun File.isUnder(parent: File): Boolean {
        val parentPath = parent.canonicalFile.toPath()
        val childPath = canonicalFile.toPath()
        return childPath.startsWith(parentPath)
    }

    private fun validateRawBuilderConventionsIfNeeded(testDataDir: File) {
        val rawBuilderRoot = testDataDir.findAncestorNamed("rawBuilder") ?: return
        if (!rawBuilderRoot.isDirectory) return

        val allCjFiles = rawBuilderRoot.walkTopDown()
            .filter { it.isFile && it.extension == "cj" }
            .sortedBy { it.invariantSeparatorsPath }
            .toList()

        val illegalNames = allCjFiles
            .map { it.relativeTo(rawBuilderRoot).invariantSeparatorsPath }
            .filterNot { it.substringAfterLast('/').matches(Regex("^[a-z][A-Za-z0-9]*\\.cj$")) }

        check(illegalNames.isEmpty()) {
            "rawBuilder file names must be camelCase and .cj: ${illegalNames.joinToString()}"
        }

        val updateMode = java.lang.Boolean.getBoolean(UPDATE_TEST_DATA_PROPERTY)
        if (!updateMode) {
            val missingGolden = allCjFiles
                .filterNot { File(it.parentFile, "${it.nameWithoutExtension}.txt").exists() }
                .map { it.relativeTo(rawBuilderRoot).invariantSeparatorsPath }

            check(missingGolden.isEmpty()) {
                "Missing .txt golden files for rawBuilder tests: ${missingGolden.joinToString()}"
            }
        }

        validateCoverageMatrix(rawBuilderRoot, allCjFiles)
    }

    private fun validateCoverageMatrix(rawBuilderRoot: File, allCjFiles: List<File>) {
        val matrixFile = File(rawBuilderRoot, "coverage-matrix.md")
        check(matrixFile.exists()) {
            "rawBuilder coverage matrix is missing: ${matrixFile.path}"
        }

        val entryRegex = Regex("""^- `([^`]+)`: (.+)$""")
        val mappedPaths = linkedSetOf<String>()
        val invalidMappedPaths = mutableListOf<String>()

        matrixFile.readLines(Charsets.UTF_8)
            .map { it.trim() }
            .filter { it.startsWith("- `") }
            .forEach { line ->
                val match = entryRegex.matchEntire(line)
                if (match == null) {
                    invalidMappedPaths += "Invalid matrix line: $line"
                    return@forEach
                }
                val rawPaths = match.groupValues[2]
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                rawPaths.forEach { rel ->
                    val normalized = rel.replace('\\', '/')
                    mappedPaths += normalized
                    val file = File(rawBuilderRoot, normalized)
                    if (!file.exists()) {
                        invalidMappedPaths += "Matrix path not found: $normalized"
                    }
                }
            }

        check(invalidMappedPaths.isEmpty()) {
            "rawBuilder coverage matrix has invalid entries:\n${invalidMappedPaths.joinToString("\n")}"
        }

        val actualPaths = allCjFiles
            .map { it.relativeTo(rawBuilderRoot).invariantSeparatorsPath }
            .toSet()
        val uncovered = actualPaths - mappedPaths
        check(uncovered.isEmpty()) {
            "rawBuilder coverage matrix misses test files: ${uncovered.sorted()}"
        }
    }

    private fun File.findAncestorNamed(dirName: String): File? {
        var current: File? = this
        while (current != null) {
            if (current.name == dirName) return current
            current = current.parentFile
        }
        return null
    }

    open fun doRawCfirTest(filePath: String) {
        val file = resolveTestDataPath(filePath)
        val sourceText = loadFile(file.path).trim()
        val cjFile = createCjFile(file.nameWithoutExtension, sourceText)
        val cfirFile = cjFile.toCfirFile()
        val actual = dumpCfirFile(cfirFile)
        val expectedPath = file.path.replace(".cj", ".txt")
        assertEqualsToFile(File(expectedPath), actual)
    }

    protected fun resolveTestDataPath(path: String): File {
        val direct = Path.of(path).toFile()
        if (direct.isAbsolute) return direct
        if (direct.exists()) return direct

        var cursor = File(System.getProperty("user.dir", ".")).absoluteFile
        while (true) {
            val candidate = cursor.resolve(path)
            if (candidate.exists()) return candidate
            val parent = cursor.parentFile ?: break
            cursor = parent
        }
        return direct
    }

    companion object {
        private const val UPDATE_TEST_DATA_PROPERTY = "update.test.data"

        fun assertEqualsToFile(expectedFile: File, actual: String) {
            val actualTrimmed = actual.trim()
            val updateMode = java.lang.Boolean.getBoolean(UPDATE_TEST_DATA_PROPERTY)
            if (!expectedFile.exists()) {
                if (updateMode) {
                    expectedFile.parentFile.mkdirs()
                    expectedFile.writeText(actualTrimmed)
                    throw AssertionError("Golden file created: ${expectedFile.path}\nPlease verify and re-run.")
                }
                throw AssertionError(
                    "Golden file missing: ${expectedFile.path}\n" +
                            "Run with -D$UPDATE_TEST_DATA_PROPERTY=true to create/update golden files.\n" +
                            "=== Actual ===\n$actualTrimmed"
                )
            }
            val expected = expectedFile.readText(Charsets.UTF_8).replace("\r\n", "\n").trim()
            if (expected != actualTrimmed) {
                if (updateMode) {
                    expectedFile.writeText(actualTrimmed)
                }
                throw AssertionError(
                    "Golden file mismatch: ${expectedFile.path}\n" +
                            if (updateMode) {
                                "File updated. Re-run to verify.\n"
                            } else {
                                "Run with -D$UPDATE_TEST_DATA_PROPERTY=true to update golden files.\n"
                            } +
                            "=== Expected ===\n$expected\n=== Actual ===\n$actualTrimmed"
                )
            }
        }
    }
}

