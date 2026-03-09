package org.cangjie.test.testFramework

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.core.CoreProjectEnvironment
import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileTypes.BinaryFileDecompiler
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory

open class CjPlatformLiteFixture : CjUsefulTestCase() {
    private val fixtureDisposable = Disposer.newDisposable("CangjieKtPlatformLiteFixture")

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
        val appEnv = CoreApplicationEnvironment(fixtureDisposable, true)
        CoreApplicationEnvironment.registerExtensionPoint(
            appEnv.application.extensionArea,
            ExtensionPointName.create<BinaryFileDecompiler>("com.intellij.filetype.decompiler"),
            BinaryFileDecompiler::class.java,
        )
        val projectEnv = CoreProjectEnvironment(fixtureDisposable, appEnv)
        project = projectEnv.project
        psiFileFactory = PsiFileFactory.getInstance(project)
    }

    override fun tearDown() {
        try {
            Disposer.dispose(fixtureDisposable)
        } finally {
            super.tearDown()
        }
    }

    protected fun loadFile(filePath: String): String {
        return java.io.File(filePath).readText(Charsets.UTF_8)
    }
}
