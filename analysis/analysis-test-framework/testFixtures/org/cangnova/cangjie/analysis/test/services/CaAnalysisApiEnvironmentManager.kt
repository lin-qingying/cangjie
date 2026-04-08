package org.cangnova.cangjie.analysis.test.services

import com.intellij.lang.LanguageParserDefinitions
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.Application
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.CangJieCoreEnvironment
import org.cangnova.cangjie.CangjieCoreApplicationEnvironment
import org.cangnova.cangjie.CangjieCoreProjectEnvironment
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.test.services.TestService
import org.cangnova.cangjie.test.services.TestServices

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
    private val sharedCoreEnvironment: CangJieCoreEnvironment by lazy {
        CangJieCoreEnvironment.createForTests(testRootDisposable)
    }

    override fun initializeEnvironment() {
        sharedCoreEnvironment

        /**
         * Analysis API 测试需要直接在内存 PSI 上工作，因此这里显式注册 ParserDefinition，
         * 保证 `CjPsiFactory` / `PsiFileFactory` 构造出的文件具有完整的 Cangjie PSI。
         */
        LanguageParserDefinitions.INSTANCE.addExplicitExtension(CangJieLanguage, CangJieParserDefinition())
    }

    override fun initializeProjectStructure() {
        val moduleStructure = testServices.cjTestModuleStructure
        CaTestProjectStructureRegistry.register(
            project = sharedCoreEnvironment.project,
            moduleStructure = moduleStructure,
            disposable = testRootDisposable,
        )
    }

    override fun getCoreEnvironment(): CangJieCoreEnvironment = sharedCoreEnvironment
}

val TestServices.environmentManager: CaAnalysisApiEnvironmentManager
    by TestServices.testServiceAccessor()
