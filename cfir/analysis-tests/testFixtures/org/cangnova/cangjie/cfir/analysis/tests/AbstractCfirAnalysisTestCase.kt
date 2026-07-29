package org.cangnova.cangjie.cfir.analysis.tests

import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
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
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.buildPreMacroRawFiles
import org.cangnova.cangjie.cfir.resolve.providers.macro.expandWithDefaultContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.recordExpandedRawFilesOnce
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import java.io.File
import java.nio.file.Path

/**
 * CFIR analysis 测试的基础用例。
 *
 * 该基类统一提供 PSI 文件创建、最小 CFIR session 组装、Raw CFIR 构建、
 * 宏构造记录以及 golden 文件断言能力，使 analysis-tests 中的解析、
 * resolve 与诊断测试共享同一套测试环境。
 */
abstract class AbstractCfirAnalysisTestCase : CjParsingTestCase(
    "",
    "cj",
    CangJieFileType.INSTANCE,
    CangJieParserDefinition(),
) {
    /**
     * 基于给定源码文本创建一个 `.cj` PSI 文件。
     *
     * 返回值固定为 [CjFile]，用于后续 Raw CFIR builder 直接消费。
     */
    protected fun createCjFile(name: String, text: String = ""): CjFile {
        return createPsiFile("$name.cj", text) as CjFile
    }

    /**
     * 创建 analysis-tests 使用的最小 source session。
     *
     * Session 内注册 module data、scope provider、source provider 与 builtin symbol provider，
     * 保证单文件测试也能按正式 CFIR provider 链路解析源码与内建符号。
     */
    protected fun createTestSession(): CfirSession {
        return object : CfirSession(Kind.Source) {}.also { session ->
            val moduleData = CfirSourceModuleData(
                name = Name.identifier("<analysis-test>"),
                dependencies = emptyList(),
                refinementDependencies = emptyList(),
                targetPlatform = CangJiePlatforms.defaultCangJiePlatform,
                platform = CfirPlatform.DEFAULT,
            ).apply {
                bindSession(session)
            }
            session.register(CfirModuleData::class, moduleData)

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

    /**
     * 将当前 PSI 文件转换为 [CfirFile]，并把 Raw CFIR 文件记录进 session provider。
     *
     * 记录过程会经过 identity macro construction，保持测试路径与正式宏构造入口一致。
     */
    protected fun CjFile.toCfirFile(
        session: CfirSession = createTestSession(),
        bodyBuildingMode: BodyBuildingMode = BodyBuildingMode.NORMAL,
    ): CfirFile {
        val builder = PsiRawCfirBuilder(session, bodyBuildingMode)
        return builder.buildCfirFile(this).also { cfirFile ->
            session.recordRawCfirFile(cfirFile, builder.consumeCollectedMacroSurfaces())
        }
    }

    /**
     * 将 Raw CFIR 文件写入 session 的 CFIR provider。
     *
     * 该方法只使用 identity 宏服务，不展开外部宏进程；它的职责是生成可被后续
     * provider、scope 与 resolve 阶段查询的 recordable file 集合。
     */
    private fun CfirSession.recordRawCfirFile(cfirFile: CfirFile, surfaces: List<MacroSurface>) {
        val provider = cfirProvider as CfirProviderImpl
        val pre = buildPreMacroRawFiles(this, listOf(cfirFile), listOf(surfaces))
        val result = MacroConstructionService.Identity.expandWithDefaultContext(
            pre = pre,
            mode = MacroConstructionService.Mode.STRICT,
        )
        val success = result as? MacroConstructionResult.Success
            ?: error("Identity macro construction must return Success, got ${result::class.simpleName}")
        recordExpandedRawFilesOnce(provider, success.recordableFiles, success.registry)
    }

    /**
     * 使用 golden 兼容渲染器输出 CFIR 文件文本。
     *
     * 输出格式用于 `.cfir.txt` golden 文件比对，必须与测试生成器期望保持一致。
     */
    protected fun dumpCfirFile(cfirFile: CfirFile): String {
        return CfirRenderer.withGoldenCompat().renderElementAsString(cfirFile)
    }

    /**
     * 解析测试数据路径。
     *
     * 绝对路径直接返回；相对路径会先按当前工作目录查找，随后逐级向上搜索，
     * 以兼容 IDE、Gradle 子项目和仓库根目录下的不同运行入口。
     */
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

    /**
     * 断言当前测试类覆盖其 `@TestMetadata` 指向目录下的所有 `.cj` 测试数据。
     *
     * 该检查用于发现测试生成器漏生成的方法，避免新增 testData 后没有对应测试入口。
     */
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
    }

    /**
     * 根据当前测试类的 `@TestMetadata` 定位实际负责的 testData 目录。
     *
     * 当注解值既不是直接目录也不是根目录下的子目录时，回退到传入的根目录。
     */
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

    /**
     * 收集当前测试类及其嵌套类中已经由 `@TestMetadata` 方法覆盖的相对路径。
     */
    private fun collectCoveredRelativePaths(testDataDir: File): Set<String> {
        val covered = linkedSetOf<String>()
        collectCoveredFromClass(this::class.java, testDataDir, testDataDir, covered)
        return covered
    }

    /**
     * 递归遍历测试类层级，按类级 `@TestMetadata` 继承规则收集方法覆盖的 `.cj` 文件。
     */
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

    /**
     * 计算指定测试类在 testData 树中的作用域目录。
     *
     * 类级 metadata 可以是绝对/仓库相对目录，也可以是相对父测试目录的嵌套目录。
     */
    private fun classScopedDir(
        klass: Class<*>,
        rootTestDataDir: File,
        inheritedDir: File,
    ): File {
        val classMetadata = klass.getAnnotation(org.cangnova.cangjie.test.TestMetadata::class.java) ?: return inheritedDir
        val metadataPath = classMetadata.value.replace('\\', '/')
        val direct = resolveTestDataPath(metadataPath)
        if (direct.isDirectory) return direct
        val nested = rootTestDataDir.resolve(metadataPath)
        if (nested.isDirectory) return nested
        val inheritedNested = inheritedDir.resolve(metadataPath)
        if (inheritedNested.isDirectory) return inheritedNested
        return inheritedDir
    }

    /**
     * 判断当前文件是否位于给定父目录之下。
     *
     * 使用 canonical path 消除 `..`、符号链接等路径差异，避免 metadata 越界。
     */
    private fun File.isUnder(parent: File): Boolean {
        val parentPath = parent.canonicalFile.toPath()
        val childPath = canonicalFile.toPath()
        return childPath.startsWith(parentPath)
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
