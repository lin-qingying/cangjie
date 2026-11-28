package org.cangnova.cangjie.analysis.api.cfir.test

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirConstructorSymbol
import org.cangnova.cangjie.analysis.api.cfir.test.configurators.CaCfirConfiguredAnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.api.cfir.test.configurators.CaCfirAnalysisApiTestConfiguratorFactory
import org.cangnova.cangjie.analysis.api.platform.CaDeserializedDeclarationsOrigin
import org.cangnova.cangjie.analysis.api.platform.CaPlatformSettings
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibrarySourceModule
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolUnavailableException
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolUnavailabilityReason
import org.cangnova.cangjie.analysis.decompiled.psi.file.CjDecompiledFile
import org.cangnova.cangjie.analysis.test.framework.base.AbstractAnalysisApiExecutionTest
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.analysis.test.framework.services.configuration.AnalysisApiBinaryLibraryIndexingMode
import org.cangnova.cangjie.analysis.test.framework.services.configuration.AnalysisApiIndexingConfiguration
import org.cangnova.cangjie.analysis.test.framework.services.libraries.CompiledLibrary
import org.cangnova.cangjie.analysis.test.framework.services.libraries.TestModuleDecompiler
import org.cangnova.cangjie.analysis.test.framework.services.libraries.TestModuleDecompilerDirectory
import org.cangnova.cangjie.analysis.test.framework.services.libraries.compiledLibraryProvider
import org.cangnova.cangjie.analysis.test.framework.services.libraries.testModuleCompiler
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisSessionMode
import org.cangnova.cangjie.analysis.test.framework.test.configurators.FrontendKind
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator
import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.psi.CjClassLikeDeclaration
import org.cangnova.cangjie.psi.CjConstructor
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.test.TestInfrastructureInternals
import org.cangnova.cangjie.test.builders.TestConfigurationBuilder
import org.cangnova.cangjie.test.model.TestModuleStructure
import org.cangnova.cangjie.test.services.TestServices
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path

/**
 * 验证真实 compiled PSI 与当前 library binary 不一致时，符号缺失会沿生产 API 降级。
 */
class CompiledDeclarationUnavailableTest : AbstractAnalysisApiExecutionTest(
    "analysis/analysis-api-cfir/testData/compiledSymbolUnavailable",
) {
    override val configurator: AnalysisApiTestConfigurator = CompiledDeclarationUnavailableConfigurator

    /**
     * 旧 CJO 的构造器 PSI 在当前 binary 中没有对应 class 时，形参查询必须给出可分类缺失。
     */
    @Test
    fun compiledDeclarationUnavailable(mainFile: CjFile, testServices: TestServices) {
        assertEquals(
            CaDeserializedDeclarationsOrigin.BINARIES,
            CaPlatformSettings.getInstance(mainFile.project).deserializedDeclarationsOrigin,
            "本用例必须通过当前 binary metadata 查询符号，不能从旧 PSI 反序列化出目标声明。",
        )

        val mainModule = testServices.cjTestModuleStructure.getModule("main")
        assertInstanceOf(CaLibrarySourceModule::class.java, mainModule.caModule)

        val libraryModule = testServices.cjTestModuleStructure.getModule("lib")
        val oldLibraryFile = libraryModule.cjFiles.single()
        assertInstanceOf(CjDecompiledFile::class.java, oldLibraryFile)

        val removedDeclaration = PsiTreeUtil
            .findChildrenOfType(oldLibraryFile, CjClassLikeDeclaration::class.java)
            .single { declaration -> declaration.name == "RemovedDeclaration" }
        val removedConstructor = PsiTreeUtil
            .findChildrenOfType(removedDeclaration, CjConstructor::class.java)
            .single()

        assertTrue(removedDeclaration.isPhysical, "旧 CJO 必须提供物理 compiled PSI，才能分类为 MISSING_DECLARATION。")
        assertTrue(oldLibraryFile.isCompiled, "目标声明必须来自真实 compiled PSI。")
        assertSame(
            libraryModule,
            testServices.cjTestModuleStructure.requireModuleByFile(oldLibraryFile),
            "旧 PSI 必须映射到 binary roots 属于当前版本的 library 模块。",
        )

        val currentLibrary = requireNotNull(testServices.compiledLibraryProvider.getCompiledLibrary("lib"))
        val currentClassNames = currentLibrary.roots
            .flatMap { root -> TestModuleDecompilerDirectory().getAllPsiFilesFromLibrary(root, mainFile.project) }
            .filterIsInstance<CjFile>()
            .flatMap { file -> PsiTreeUtil.findChildrenOfType(file, CjClassLikeDeclaration::class.java).mapNotNull { it.name } }
        assertTrue("CurrentMarker" in currentClassNames)
        assertFalse("RemovedDeclaration" in currentClassNames, "当前 binary 不得包含旧版本声明：$currentClassNames")

        val unavailabilityReason = analyzeForTest(mainFile) {
            // 直接构造生产 PSI 符号叶子，让它在 LibrarySource use-site 中解析映射到 `lib` 的 compiled PSI。
            val constructorSymbol = CaCfirConstructorSymbol(removedConstructor, this as CaCfirSession)
            assertNull(constructorSymbol.cfirSymbolOrNull)
            assertTrue(constructorSymbol.returnType.isErrorType)
            val unavailable = org.junit.jupiter.api.Assertions.assertThrows(CaSymbolUnavailableException::class.java) {
                constructorSymbol.valueParameters
            }
            unavailable.reason
        }

        assertEquals(CaSymbolUnavailabilityReason.MISSING_DECLARATION, unavailabilityReason)
    }
}

/**
 * 仅为 G6 用例替换 decompiler service，保持共享 Analysis API 模块工厂和默认 decompiler 原样。
 */
private object CompiledDeclarationUnavailableConfigurator : CaCfirConfiguredAnalysisApiTestConfigurator(
    moduleKind = TestModuleKind.Source,
    serviceRegistrars = g6IdeConfigurator.serviceRegistrars,
    analysisApiMode = AnalysisApiMode.Ide,
    analyseInDependentSession = false,
) {
    override val testPrefixes: List<String>
        get() = g6IdeConfigurator.testPrefixes

    @OptIn(TestInfrastructureInternals::class)
    override fun configureTest(builder: TestConfigurationBuilder, disposable: Disposable) {
        super.configureTest(builder, disposable)
        builder.useAdditionalService {
            AnalysisApiIndexingConfiguration(AnalysisApiBinaryLibraryIndexingMode.NO_INDEXING)
        }
        builder.useAdditionalService<TestModuleDecompiler> { testServices ->
            RemovedArtifactPsiTestModuleDecompiler(testServices)
        }
    }
}

private val g6IdeConfigurator = CaCfirAnalysisApiTestConfiguratorFactory.createConfigurator(
    AnalysisApiTestConfiguratorFactoryData(
        frontend = FrontendKind.Cfir,
        moduleKind = TestModuleKind.Source,
        analysisSessionMode = AnalysisSessionMode.Normal,
        analysisApiMode = AnalysisApiMode.Ide,
    ),
)

/**
 * 为目标 `lib` binary roots 提供旧源码模块编译得到的 compiled PSI。
 */
private class RemovedArtifactPsiTestModuleDecompiler(
    private val testServices: TestServices,
) : TestModuleDecompiler {
    private val directoryDecompiler = TestModuleDecompilerDirectory()
    private val removedArtifact: CompiledLibrary by lazy(LazyThreadSafetyMode.NONE) {
        val sourceModule = testServices.g6TestModuleStructure.modules.single { module -> module.name == "removedArtifact" }
        testServices.testModuleCompiler.compileTestModuleToLibrary(
            module = sourceModule,
            dependencyBinaryRoots = emptyList(),
            testServices = testServices,
        )
    }

    override fun getAllPsiFilesFromLibrary(artifactRoot: Path, project: Project): List<com.intellij.psi.PsiFile> {
        requireCurrentLibraryRoot(artifactRoot)
        return removedArtifact.roots.flatMap { psiRoot ->
            directoryDecompiler.getAllPsiFilesFromLibrary(psiRoot, project)
        }
    }

    /** 要求 decompiler 只处理目标 `lib` binary，错路由时直接失败。 */
    private fun requireCurrentLibraryRoot(artifactRoot: Path): Path {
        val currentLibrary = requireNotNull(testServices.compiledLibraryProvider.getCompiledLibrary("lib")) {
            "The current `lib` binary must be compiled before decompilation."
        }
        val normalizedRoot = artifactRoot.toAbsolutePath().normalize()
        require(currentLibrary.roots.any { root -> root.toAbsolutePath().normalize() == normalizedRoot }) {
            "G6 decompiler received an unexpected artifact root: $artifactRoot"
        }
        return artifactRoot
    }
}

/** 原始多模块测试输入，供 G6 专用 decompiler 读取旧版本源码。 */
private val TestServices.g6TestModuleStructure: TestModuleStructure by TestServices.testServiceAccessor()
