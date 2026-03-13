package org.cangnova.cangjie.analysis.test.services

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.Application
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.test.services.TestService
import org.cangnova.cangjie.test.services.TestServices
import org.cangnova.cangjie.cli.CangJieCoreEnvironment
import org.cangnova.cangjie.cli.CangjieCoreApplicationEnvironment
import org.cangnova.cangjie.cli.CangjieCoreProjectEnvironment

/**
 * Aligns with Kotlin's AnalysisApiEnvironmentManager by owning a single shared core environment.
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
    private val testRootDisposable: Disposable,
) : CaAnalysisApiEnvironmentManager() {

    private val coreEnvironment: CangJieCoreEnvironment by lazy {
        CangJieCoreEnvironment.createForTests(testRootDisposable)
    }

    override fun initializeEnvironment() {
        coreEnvironment
    }

    override fun initializeProjectStructure() {
        // TODO: Register project-level services when structure providers are introduced.
    }

    override fun getCoreEnvironment(): CangJieCoreEnvironment = coreEnvironment
}

val TestServices.environmentManager: CaAnalysisApiEnvironmentManager
    by TestServices.testServiceAccessor()

