package org.cangjie.analysis.test.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.Application
import com.intellij.openapi.project.Project
import org.cangjie.test.services.TestService
import org.cangjie.test.services.TestServices
import org.cangnova.cangjie.cli.CangjieCoreApplicationEnvironment
import org.cangnova.cangjie.cli.CangjieCoreProjectEnvironment

/**
 * 测试环境管理器（对齐 Kotlin 的 AnalysisApiEnvironmentManager）。
 *
 * 管理 [cli.CangjieCoreApplicationEnvironment] 和 [cli.CangjieCoreProjectEnvironment] 的生命周期。
 */
abstract class CaAnalysisApiEnvironmentManager : TestService {
    abstract fun initializeEnvironment()
    abstract fun initializeProjectStructure()

    fun getProject(): Project = getProjectEnvironment().project
    fun getApplication(): Application = getApplicationEnvironment().application

    abstract fun getProjectEnvironment(): CangjieCoreProjectEnvironment
    abstract fun getApplicationEnvironment(): CangjieCoreApplicationEnvironment
}

class CaAnalysisApiEnvironmentManagerImpl(
    private val testRootDisposable: Disposable,
) : CaAnalysisApiEnvironmentManager() {

    private val _applicationEnvironment: CangjieCoreApplicationEnvironment by lazy {
        CangjieCoreApplicationEnvironment.create(testRootDisposable, unitTestMode = true)
    }

    private val _projectEnvironment: CangjieCoreProjectEnvironment by lazy {
        CangjieCoreProjectEnvironment(testRootDisposable, _applicationEnvironment)
    }

    override fun initializeEnvironment() {
        // 触发 lazy 初始化
        _applicationEnvironment
        _projectEnvironment
    }

    override fun initializeProjectStructure() {
        // TODO: 注册项目级服务（如 CaProjectStructureProvider），当基础设施就绪后实现
    }

    override fun getProjectEnvironment(): CangjieCoreProjectEnvironment = _projectEnvironment
    override fun getApplicationEnvironment(): CangjieCoreApplicationEnvironment = _applicationEnvironment
}

val TestServices.environmentManager: CaAnalysisApiEnvironmentManager
    by TestServices.testServiceAccessor()
