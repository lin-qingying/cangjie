package org.cangnova.cangjie.test.testFramework

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFileFactory
import org.cangnova.cangjie.CangJieCoreEnvironment

/**
 * 表示 `CjPlatformLiteFixture`，承载测试基础设施中的配置数据、测试产物或处理步骤。
 */
open class CjPlatformLiteFixture : CjUsefulTestCase() {
    /**
     * 保存 `fixtureDisposable`，供测试基础设施在测试执行期间读取或传递。
     */
    private val fixtureDisposable = Disposer.newDisposable("CangjieKtPlatformLiteFixture")
    /**
     * 保存 `environment`，供测试基础设施在测试执行期间读取或传递。
     */
    private lateinit var environment: CangJieCoreEnvironment

    /**
     * 保存 `testRootDisposable`，供测试基础设施在测试执行期间读取或传递。
     */
    protected val testRootDisposable: Disposable
        get() = fixtureDisposable

    /**
     * 保存 `project`，供测试基础设施在测试执行期间读取或传递。
     */
    protected lateinit var project: Project
        private set

    /**
     * 保存 `psiFileFactory`，供测试基础设施在测试执行期间读取或传递。
     */
    protected lateinit var psiFileFactory: PsiFileFactory
        private set

    /**
     * 保存 `myFileExt`，供测试基础设施在测试执行期间读取或传递。
     */
    protected var myFileExt: String = "cj"
    /**
     * 保存 `myFileText`，供测试基础设施在测试执行期间读取或传递。
     */
    protected var myFileText: String = ""

    /**
     * 执行 `setUp` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    override fun setUp() {
        super.setUp()
        environment = CangJieCoreEnvironment.createForTests(fixtureDisposable)
        project = environment.project
        psiFileFactory = PsiFileFactory.getInstance(project)
    }

    /**
     * 执行 `tearDown` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    override fun tearDown() {
        try {
            val application = ApplicationManager.getApplication()
            if (application != null) {
                application.runWriteAction {
                    Disposer.dispose(fixtureDisposable)
                }
            } else {
                Disposer.dispose(fixtureDisposable)
            }
        } finally {
            super.tearDown()
        }
    }

    /**
     * 提供 `loadFile` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    protected fun loadFile(filePath: String): String {
        return java.io.File(filePath).readText(Charsets.UTF_8).replace("\r\n", "\n")
    }
}
