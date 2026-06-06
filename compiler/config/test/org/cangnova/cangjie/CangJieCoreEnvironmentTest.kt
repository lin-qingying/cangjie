package org.cangnova.cangjie

import com.intellij.lang.LanguageParserDefinitions
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.AsyncExecutionService
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFileSetFactory
import com.intellij.model.psi.PsiSymbolReferenceService
import com.intellij.model.psi.PsiSymbolService
import com.intellij.psi.FileContextProvider
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiTreeChangeListener
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.impl.smartPointers.SmartPointerAnchorProvider
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.targetPlatform
import org.cangnova.cangjie.platform.CangJiePlatforms
import org.cangnova.cangjie.psi.CjFile
import java.awt.GraphicsEnvironment
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CangJieCoreEnvironmentTest {
    @Test
    fun `application environment registers platform services and extension points`() {
        withEnvironment { environment ->
            val application = environment.applicationEnvironment.application
            val extensionArea = application.extensionArea

            assertNotNull(application.getService(TransactionGuard::class.java))
            assertNotNull(application.getService(AsyncExecutionService::class.java))
            assertNotNull(application.getService(PsiSymbolService::class.java))
            assertNotNull(application.getService(PsiSymbolReferenceService::class.java))
            assertNotNull(application.getService(VirtualFileSetFactory::class.java))

            assertTrue(extensionArea.hasExtensionPoint(PsiReferenceContributor.EP_NAME.name))
            assertTrue(extensionArea.hasExtensionPoint("com.intellij.psi.symbolReferenceProvider"))
            assertTrue(extensionArea.hasExtensionPoint("com.intellij.psi.implicitReferenceProvider"))
            assertTrue(extensionArea.hasExtensionPoint("com.intellij.psi.declarationProvider"))
            assertTrue(extensionArea.hasExtensionPoint("com.intellij.referencesSearch"))
            assertTrue(extensionArea.hasExtensionPoint(SmartPointerAnchorProvider.EP_NAME.name))
            assertTrue(extensionArea.hasExtensionPoint(FileContextProvider.EP_NAME.name))
        }
    }

    @Test
    fun `project environment exposes CangJie psi infrastructure directly from core environment`() {
        withEnvironment { environment ->
            val project = environment.project
            val extensionArea = project.extensionArea

            assertTrue(extensionArea.hasExtensionPoint(PsiTreeChangePreprocessor.EP.name))
            assertTrue(extensionArea.hasExtensionPoint(PsiTreeChangeListener.EP.name))
            assertNotNull(project.getService(PsiSearchHelper::class.java))

            assertNotNull(LanguageParserDefinitions.INSTANCE.forLanguage(CangJieLanguage))
            assertSame(CangJieFileType.INSTANCE, FileTypeRegistry.getInstance().getFileTypeByFileName("sample.cj"))
            assertSame(CangJieBuiltInFileType, FileTypeRegistry.getInstance().getFileTypeByFileName("stdlib.cjo"))

            val psiFile = PsiFileFactory.getInstance(project).createFileFromText(
                "sample.cj",
                CangJieFileType.INSTANCE,
                "package sample\n",
            )

            assertInstanceOf(CjFile::class.java, psiFile)
        }
    }

    @Test
    fun `unit test application only allows write access inside write action`() {
        withEnvironment { _ ->
            val application = ApplicationManager.getApplication()
            assertTrue(application.isUnitTestMode)
            assertFalse(application.isWriteAccessAllowed)

            application.runWriteAction {
                assertTrue(application.isWriteAccessAllowed)
            }

            assertFalse(application.isWriteAccessAllowed)
        }
    }

    @Test
    fun `core environment forces awt headless mode for standalone bootstrap`() {
        withEnvironment { _ ->
            assertSame("true", System.getProperty("java.awt.headless"))
            assertTrue(GraphicsEnvironment.isHeadless())
        }
    }

    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `compiler configuration stores cjvm target platform placeholder`() {
        val configuration = CompilerConfiguration()

        configuration.targetPlatform = CangJiePlatforms.cjvm

        assertSame(CangJiePlatforms.cjvm, configuration.targetPlatform)
    }

    private fun withEnvironment(
        action: (CangJieCoreEnvironment) -> Unit,
    ) {
        val disposable = Disposer.newDisposable("CangJieCoreEnvironmentTest")
        try {
            val environment = CangJieCoreEnvironment.createForTests(disposable)
            action(environment)
        } finally {
            val application = ApplicationManager.getApplication()
            if (application != null) {
                application.runWriteAction {
                    Disposer.dispose(disposable)
                }
            } else {
                Disposer.dispose(disposable)
            }
        }
    }
}
