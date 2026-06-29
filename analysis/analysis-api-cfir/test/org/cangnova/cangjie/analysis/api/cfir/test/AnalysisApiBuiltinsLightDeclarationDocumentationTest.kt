package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledBinaryIndex
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclaration
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclarationOriginKind
import org.cangnova.cangjie.analysis.api.lightDeclarations.CaLightDeclarationProvider
import org.cangnova.cangjie.analysis.api.lightDeclarations.documentation
import org.cangnova.cangjie.analysis.api.impl.base.test.configurators.CaAnalysisApiDecompiledTestServiceRegistrar
import org.cangnova.cangjie.analysis.decompiled.psi.BuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.api.standalone.projectStructure.AnalysisApiServiceRegistrar
import org.cangnova.cangjie.analysis.api.standalone.cfir.test.configurators.CaCfirStandaloneAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.CjTestModule
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjNamedDeclaration
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.isRegularFile

/**
 * 锁定 builtins/decompiled light declaration 的文档边界。
 *
 * 这里验证两件事：
 * 1. builtins use-site 下，light declaration provider 能返回 decompiled 视图；
 * 2. 当前 decompiled 边界没有真实 CDoc 时，文档恢复稳定返回 null。
 */
class AnalysisApiBuiltinsLightDeclarationDocumentationTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/lightDeclarationDocsBuiltins",
) {
    /**
     * 指向测试内临时 stdlib 根目录的系统属性名。
     */
    private val stdlibModulePropertyName = "cangjie.stdlib.module"

    /**
     * 测试开始前已有的 stdlib 系统属性值，用于在测试结束后恢复调用方环境。
     */
    private var previousStdlibModulePropertyValue: String? = null

    /**
     * 当前测试创建的精简 stdlib fixture 根目录。
     */
    private lateinit var testStdlibRoot: Path

    /**
     * 使用 CFIR standalone 配置运行 builtins light declaration 文档测试。
     */
    override val configurator = CaCfirStandaloneAnalysisApiTestConfigurator

    /**
     * 额外启用 decompiled PSI 服务，使 builtins 二进制文件可以恢复为可查询的反编译视图。
     */
    override val additionalServiceRegistrars: List<AnalysisApiServiceRegistrar<TestServices>> =
        listOf(CaAnalysisApiDecompiledTestServiceRegistrar)

    /**
     * 在每个用例前安装只包含必要 builtins 文件的 stdlib fixture。
     */
    @BeforeEach
    fun installStdlibFixtureProperty() {
        previousStdlibModulePropertyValue = System.getProperty(stdlibModulePropertyName)
        testStdlibRoot = createSlimStdlibFixture("std.cjo", "std/std.core.cjo", "std/std.objectpool.cjo")
        System.setProperty(stdlibModulePropertyName, testStdlibRoot.toString())
    }

    /**
     * 在每个用例后恢复调用方原有的 stdlib 系统属性状态。
     */
    @AfterEach
    fun restoreStdlibFixtureProperty() {
        if (previousStdlibModulePropertyValue == null) {
            System.clearProperty(stdlibModulePropertyName)
        } else {
            System.setProperty(stdlibModulePropertyName, previousStdlibModulePropertyValue)
        }
    }

    /**
     * 验证 builtins 的 decompiled light declaration 能被发现，并且无 CDoc 时文档结果稳定为空。
     */
    @Test
    fun builtinsLightDeclarationDocs(mainFile: CjFile, mainModule: CjTestModule) {
        val decompiledFile = findBuiltinsObjectPool(mainFile)
        assertNotNull(decompiledFile, "builtins decompiled PSI 应可恢复 `std.objectpool`")

        val provider = CaLightDeclarationProvider.getInstance(mainFile.project)
        val allDeclarations = ApplicationManager.getApplication().runWriteAction<List<CaLightDeclaration>> {
            provider.getLightDeclarations(decompiledFile!!, mainModule.caModule)
        }
        val declaration = allDeclarations.firstOrNull { lightDeclaration ->
            lightDeclaration.origin.kind == CaLightDeclarationOriginKind.DECOMPILED_PSI
        }
        assertNotNull(declaration, "builtins light declaration provider 应返回 decompiled 视图")
        assertTrue(declaration!!.origin.containingFile?.isCompiled == true)
        assertFalse(declaration.origin.description.isBlank())

        analyzeForTest(mainFile) {
            assertEquals(null, documentation(declaration))
        }
    }

    /**
     * 验证 `std.objectpool` 的反编译文本可恢复且规模保持在可控范围内。
     */
    @Test
    fun builtinsDecompiledText(mainFile: CjFile, mainModule: CjTestModule) {
        val decompiledFile = findBuiltinsObjectPool(mainFile)
        assertNotNull(decompiledFile)
        assertTrue(decompiledFile!!.text.contains("ObjectPool"))
        assertTrue(decompiledFile.text.length < 200_000, "std.objectpool decompiled text 不应异常膨胀")
    }

    /**
     * 验证精简 builtins fixture 只暴露当前用例需要的顶层反编译声明。
     */
    @Test
    fun builtinsDecompiledTopLevelDeclarations(mainFile: CjFile, mainModule: CjTestModule) {
        val decompiledFile = findBuiltinsObjectPool(mainFile)
        assertNotNull(decompiledFile)
        val actualDeclarations = decompiledFile!!.declarations.filterIsInstance<CjNamedDeclaration>().mapNotNull { declaration -> declaration.name }
        assertEquals(
            listOf("ObjectPool"),
            actualDeclarations,
        )
    }

    /**
     * 从 builtins 二进制索引或虚拟文件提供器中定位 `std.objectpool` 的反编译 PSI 文件。
     */
    private fun findBuiltinsObjectPool(mainFile: CjFile): CjFile? {
        return ApplicationManager.getApplication().runWriteAction<CjFile?> {
            val binaryIndex = mainFile.project.getService(CaDecompiledBinaryIndex::class.java)
            val builtinFiles = BuiltinsVirtualFileProvider.getInstance().getBuiltinVirtualFiles()
            val binaryFile = binaryIndex.findBuiltinsBinaryFile(FqName("std.objectpool"))
                ?: builtinFiles.firstOrNull { virtualFile ->
                        virtualFile.name.equals("std.objectpool.cjo", ignoreCase = true) &&
                            binaryIndex.readPackageFqName(virtualFile) == FqName("std.objectpool")
                    }
                ?: error(
                    "Cannot find std.objectpool in builtins files: " +
                        builtinFiles.map { file -> file.path + ":" + binaryIndex.readPackageFqName(file) }.sorted(),
                )
            PsiManager.getInstance(mainFile.project).findFile(binaryFile) as? CjFile
        }
    }

    /**
     * 在仓库内定位可复制的 stdlib fixture 根目录。
     */
    private fun locateStdlibFixtureRoot(): Path {
        val repoRoot = locateRepositoryRoot(Paths.get("").toAbsolutePath().normalize())
        val fixtureRoot = repoRoot
            .resolve("cfir")
            .resolve("cfir-serialization")
            .resolve("testResources")
            .resolve("cjo-sdk")
            .resolve("windows_x86_64_cjnative")

        require(fixtureRoot.resolve("std.cjo").isRegularFile()) {
            "Cannot locate stdlib fixture root under $fixtureRoot"
        }
        return fixtureRoot
    }

    /**
     * 从给定目录向上查找包含 `settings.gradle.kts` 的仓库根目录。
     */
    private fun locateRepositoryRoot(start: Path): Path {
        return generateSequence(start) { current -> current.parent }
            .firstOrNull { candidate -> candidate.resolve("settings.gradle.kts").isRegularFile() }
            ?: error("Cannot locate repository root from $start")
    }

    /**
     * 把测试需要的 stdlib 文件复制到临时目录，避免完整 SDK 进入 builtins 测试边界。
     */
    private fun createSlimStdlibFixture(
        vararg relativePaths: String,
    ): Path {
        val sourceRoot = locateStdlibFixtureRoot()
        val tempRoot = Files.createTempDirectory("cangjie-analysis-api-stdlib")
        relativePaths.forEach { relativePath ->
            val sourceFile = sourceRoot.resolve(relativePath)
            require(sourceFile.isRegularFile()) { "Missing stdlib fixture file: $sourceFile" }
            val targetFile = tempRoot.resolve(relativePath)
            Files.createDirectories(targetFile.parent)
            Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING)
        }
        return tempRoot
    }
}
