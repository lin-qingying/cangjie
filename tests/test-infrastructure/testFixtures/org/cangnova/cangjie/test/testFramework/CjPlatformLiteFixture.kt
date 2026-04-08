package org.cangnova.cangjie.test.testFramework

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFileFactory
import org.cangnova.cangjie.CangJieCoreEnvironment

open class CjPlatformLiteFixture : CjUsefulTestCase() {
    private val fixtureDisposable = Disposer.newDisposable("CangjieKtPlatformLiteFixture")
    private lateinit var environment: CangJieCoreEnvironment

    protected val testRootDisposable: Disposable
        get() = fixtureDisposable

    protected lateinit var project: Project
        private set

    protected lateinit var psiFileFactory: PsiFileFactory
        private set

    protected var myFileExt: String = "cj"
    protected var myFileText: String = ""

    override fun setUp() {
        super.setUp()
        environment = CangJieCoreEnvironment.createForTests(fixtureDisposable)
        project = environment.project
        psiFileFactory = PsiFileFactory.getInstance(project)
    }

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

    protected fun loadFile(filePath: String): String {
        return java.io.File(filePath).readText(Charsets.UTF_8).replace("\r\n", "\n")
    }
}
