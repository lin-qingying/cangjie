package org.cangnova.cangjie.analysis.test.services

import com.intellij.lang.LanguageParserDefinitions
import com.intellij.mock.MockApplication
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.Application
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.CangJieCoreEnvironment
import org.cangnova.cangjie.CangjieCoreApplicationEnvironment
import org.cangnova.cangjie.CangjieCoreProjectEnvironment
import org.cangnova.cangjie.analysis.api.decompiled.CaBuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.test.services.TestService
import org.cangnova.cangjie.test.services.TestServices
import java.io.File

/**
 * Analysis API 测试环境管理器。
 *
 * 对齐 Kotlin `AnalysisApiEnvironmentManager` 的职责：
 * 1. 持有共享 CoreEnvironment；
 * 2. 暴露 Project/Application；
 * 3. 在环境完成后初始化测试项目结构。
 */
abstract class CaAnalysisApiEnvironmentManager : TestService {
    abstract fun initializeEnvironment()

    abstract fun initializeProjectStructure()

    abstract fun getCoreEnvironment(): CangJieCoreEnvironment

    fun getProject(): Project = getProjectEnvironment().project

    fun getApplication(): Application = getApplicationEnvironment().application

    fun getProjectEnvironment(): CangjieCoreProjectEnvironment = getCoreEnvironment().projectEnvironment

    fun getApplicationEnvironment(): CangjieCoreApplicationEnvironment = getCoreEnvironment().applicationEnvironment
}

class CaAnalysisApiEnvironmentManagerImpl(
    private val testServices: TestServices,
    private val testRootDisposable: Disposable,
) : CaAnalysisApiEnvironmentManager() {
    private val stdlibModulePropertyName = "cangjie.stdlib.module"
    private val sharedCoreEnvironment: CangJieCoreEnvironment by lazy {
        CangJieCoreEnvironment.createForTests(testRootDisposable)
    }

    override fun initializeEnvironment() {
        ensureStdlibPropertyForAnalysisTests()
        sharedCoreEnvironment
        (getApplication() as MockApplication).apply {
            if (getServiceIfCreated(CaBuiltinsVirtualFileProvider::class.java) == null) {
                registerService(CaBuiltinsVirtualFileProvider::class.java, CaBuiltinsVirtualFileProviderTestImpl())
            }
        }

        /**
         * Analysis API 测试需要直接在内存 PSI 上工作，因此这里显式注册 ParserDefinition，
         * 保证 `CjPsiFactory` / `PsiFileFactory` 构造出的文件具有完整的 Cangjie PSI。
         */
        LanguageParserDefinitions.INSTANCE.addExplicitExtension(CangJieLanguage, CangJieParserDefinition())
    }

    override fun initializeProjectStructure() {
        sharedCoreEnvironment.projectEnvironment.project
            .getService(CaTestPlatformState::class.java)
            .install(testServices.cjTestModuleStructure)
    }

    override fun getCoreEnvironment(): CangJieCoreEnvironment = sharedCoreEnvironment

    /**
     * analysis 测试当前实际会命中 CLI builtins provider，
     * 因而这里必须像 compiler test framework 一样为 stdlib fixture 预置根路径。
     *
     * 若调用方已显式设置 `cangjie.stdlib.module` / `CANGJIE_STDLIB_MODULE`，
     * 则保持调用方配置，不做覆盖。
     */
    private fun ensureStdlibPropertyForAnalysisTests() {
        if (!System.getProperty(stdlibModulePropertyName).isNullOrBlank()) return
        if (!System.getenv("CANGJIE_STDLIB_MODULE").isNullOrBlank()) return

        val resolvedRoot = resolveStdlibRoot() ?: return
        System.setProperty(stdlibModulePropertyName, resolvedRoot.absolutePath)
    }

    private fun resolveStdlibRoot(): File? {
        val fallbackCandidates = listOf(
            File("cfir/cfir-serialization/testResources/cjo-sdk/windows_x86_64_cjnative"),
            File("cfir/cfir-serialization/build/resources/test/cjo-sdk/windows_x86_64_cjnative"),
        )

        return fallbackCandidates
            .asSequence()
            .filter { it.exists() && it.isDirectory }
            .map(::normalizeStdlibRoot)
            .firstOrNull { it.exists() && it.isDirectory }
    }

    private fun normalizeStdlibRoot(path: File): File {
        val normalized = path.normalize()
        if (normalized.resolve("std/std.core.cjo").isFile) return normalized
        if (normalized.resolve("std.core.cjo").isFile) return normalized.parentFile ?: normalized
        return normalized
    }
}

val TestServices.environmentManager: CaAnalysisApiEnvironmentManager
    by TestServices.testServiceAccessor()
