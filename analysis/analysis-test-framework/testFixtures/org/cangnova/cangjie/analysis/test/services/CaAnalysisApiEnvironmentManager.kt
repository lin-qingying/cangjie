package org.cangnova.cangjie.analysis.test.services

import com.intellij.lang.LanguageParserDefinitions
import com.intellij.mock.MockApplication
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.Application
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.CangJieCoreEnvironment
import org.cangnova.cangjie.CangjieCoreApplicationEnvironment
import org.cangnova.cangjie.CangjieCoreProjectEnvironment
import org.cangnova.cangjie.analysis.decompiled.psi.BuiltinsVirtualFileProvider
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
    /**
     * 初始化共享 core environment 及测试必须的 application 级服务。
     */
    abstract fun initializeEnvironment()

    /**
     * 将当前测试模块结构安装进测试平台状态。
     */
    abstract fun initializeProjectStructure()

    /**
     * 返回当前测试共享的 core environment。
     */
    abstract fun getCoreEnvironment(): CangJieCoreEnvironment

    /**
     * 返回当前测试 project。
     */
    fun getProject(): Project = getProjectEnvironment().project

    /**
     * 返回当前测试 application。
     */
    fun getApplication(): Application = getApplicationEnvironment().application

    /**
     * 返回当前测试 project environment。
     */
    fun getProjectEnvironment(): CangjieCoreProjectEnvironment = getCoreEnvironment().projectEnvironment

    /**
     * 返回当前测试 application environment。
     */
    fun getApplicationEnvironment(): CangjieCoreApplicationEnvironment = getCoreEnvironment().applicationEnvironment
}

/**
 * 基于共享 [CangJieCoreEnvironment] 的 Analysis API 测试环境管理器实现。
 */
class CaAnalysisApiEnvironmentManagerImpl(
    /**
     * 当前测试服务容器。
     */
    private val testServices: TestServices,
    /**
     * 当前测试根 disposable。
     */
    private val testRootDisposable: Disposable,
) : CaAnalysisApiEnvironmentManager() {
    /**
     * 指向 stdlib module 根目录的系统属性名。
     */
    private val stdlibModulePropertyName = "cangjie.stdlib.module"

    /**
     * 延迟创建的共享 core environment。
     */
    private val sharedCoreEnvironment: CangJieCoreEnvironment by lazy {
        CangJieCoreEnvironment.createForTests(testRootDisposable)
    }

    /**
     * 初始化 stdlib、builtins provider 与 parser definition。
     */
    override fun initializeEnvironment() {
        ensureStdlibPropertyForAnalysisTests()
        sharedCoreEnvironment
        (getApplication() as MockApplication).apply {
            if (getServiceIfCreated(BuiltinsVirtualFileProvider::class.java) == null) {
                registerService(BuiltinsVirtualFileProvider::class.java, BuiltinsVirtualFileProviderTestImpl())
            }
        }

        /**
         * Analysis API 测试需要直接在内存 PSI 上工作，因此这里显式注册 ParserDefinition，
         * 保证 `CjPsiFactory` / `PsiFileFactory` 构造出的文件具有完整的 Cangjie PSI。
         */
        LanguageParserDefinitions.INSTANCE.addExplicitExtension(CangJieLanguage, CangJieParserDefinition())
    }

    /**
     * 将测试模块结构安装到测试平台状态服务。
     */
    override fun initializeProjectStructure() {
        sharedCoreEnvironment.projectEnvironment.project
            .getService(CaTestPlatformState::class.java)
            .install(testServices.cjTestModuleStructure)
    }

    /**
     * 返回共享 core environment 实例。
     */
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

    /**
     * 解析 analysis 测试可用的 stdlib fixture 根目录。
     */
    private fun resolveStdlibRoot(): File? {
        val repositoryRoot = locateRepositoryRoot(File("").absoluteFile.normalize())
        val fallbackCandidates = listOf(
            repositoryRoot.resolve("cfir/cfir-serialization/testResources/cjo-sdk/windows_x86_64_cjnative"),
            repositoryRoot.resolve("cfir/cfir-serialization/build/resources/test/cjo-sdk/windows_x86_64_cjnative"),
        )

        return fallbackCandidates
            .asSequence()
            .filter { it.exists() && it.isDirectory }
            .map(::normalizeStdlibRoot)
            .firstOrNull { it.exists() && it.isDirectory }
    }

    /**
     * 将 stdlib 根目录规范化到包含 `std.core.cjo` 的父级目录。
     */
    private fun normalizeStdlibRoot(path: File): File {
        val normalized = path.normalize()
        if (normalized.resolve("std/std.core.cjo").isFile) return normalized
        if (normalized.resolve("std.core.cjo").isFile) return normalized.parentFile ?: normalized
        return normalized
    }

    /**
     * 从当前目录向上查找仓库根目录。
     */
    private fun locateRepositoryRoot(start: File): File {
        return generateSequence(start) { file -> file.parentFile }
            .firstOrNull { file -> file.resolve("settings.gradle.kts").isFile }
            ?: start
    }
}

/**
 * 当前测试服务容器中的 Analysis API 环境管理器。
 */
val TestServices.environmentManager: CaAnalysisApiEnvironmentManager
    by TestServices.testServiceAccessor()
