package org.cangjie.cfir.builder

import org.cangjie.cfir.common.CfirModuleData
import org.cangjie.cfir.declarations.CfirFile
import org.cangjie.cfir.renderer.CfirRenderer
import org.cangjie.cfir.session.CfirSession
import org.cangjie.cfir.builder.BodyBuildingMode
import org.cangjie.test.testFramework.CjParsingTestCase
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
        return object : CfirSession(Kind.Source) {}.apply {
            val moduleData = CfirModuleData(Name.identifier("<test>"))
            register(CfirModuleData::class, moduleData)
        }
    }

    protected fun CjFile.toCfirFile(
        session: CfirSession = createTestSession(),
        bodyBuildingMode: BodyBuildingMode = BodyBuildingMode.NORMAL,
    ): CfirFile {
        return PsiRawCfirBuilder(session, bodyBuildingMode).buildCfirFile(this)
    }

    protected fun dumpCfirFile(cfirFile: CfirFile): String {
        return CfirRenderer.withGoldenCompat().renderElementAsString(cfirFile)
    }

    protected fun assertAllFilesPresentByMetadata(testDataRootRelativePath: String) {
        val testDataDir = Path.of(testDataRootRelativePath).toFile()
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
    }

    private fun currentClassTestDataDir(rootTestDataDir: File): File {
        val classMetadata = this::class.java.getAnnotation(org.cangjie.test.TestMetadata::class.java)
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

        for (testClass in this::class.java.declaredMethods) {
            val metadata = testClass.getAnnotation(org.cangjie.test.TestMetadata::class.java) ?: continue
            val metadataPath = metadata.value.replace('\\', '/')
            val candidate = testDataDir.resolve(metadataPath)
            if (candidate.isFile && candidate.extension == "cj") {
                covered += candidate.relativeTo(testDataDir).invariantSeparatorsPath
            }
        }

        for (nestedClass in this::class.java.declaredClasses) {
            for (method in nestedClass.declaredMethods) {
                val metadata = method.getAnnotation(org.cangjie.test.TestMetadata::class.java) ?: continue
                val metadataPath = metadata.value.replace('\\', '/')
                val candidate = testDataDir.resolve(metadataPath)
                if (candidate.isFile && candidate.extension == "cj") {
                    covered += candidate.relativeTo(testDataDir).invariantSeparatorsPath
                }
            }
        }

        val testSources = File("cfir/raw-cfir/psi2cfir/tests-gen/org/cangjie/cfir/builder")
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

    open fun doRawCfirTest(filePath: String) {
        val file = File(filePath)
        val sourceText = file.readText(Charsets.UTF_8).trim()
        val cjFile = createCjFile(file.nameWithoutExtension, sourceText)
        val cfirFile = cjFile.toCfirFile()
        val actual = dumpCfirFile(cfirFile)
        val expectedPath = filePath.replace(".cj", ".txt")
        assertEqualsToFile(File(expectedPath), actual)
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
