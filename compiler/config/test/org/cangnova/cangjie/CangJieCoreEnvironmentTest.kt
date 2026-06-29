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

/**
 * 覆盖仓颉 core headless 环境的 application、project、写动作和基础配置装配。
 */
class CangJieCoreEnvironmentTest {
    /**
     * 验证 application 环境注册 IntelliJ 平台服务和仓颉依赖的基础扩展点。
     */
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

    /**
     * 验证 project 环境可以直接提供仓颉 PSI、文件类型和项目级扩展基础设施。
     */
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

    /**
     * 验证单测 application 只在显式写动作内部报告写权限。
     */
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

    /**
     * 验证 standalone bootstrap 会强制设置 AWT headless 模式。
     */
    @Test
    fun `core environment forces awt headless mode for standalone bootstrap`() {
        withEnvironment { _ ->
            assertSame("true", System.getProperty("java.awt.headless"))
            assertTrue(GraphicsEnvironment.isHeadless())
        }
    }

    /**
     * 验证编译配置可以存储并读取 CJVM 目标平台占位对象。
     */
    @Test
    @OptIn(CompilerConfiguration.Internals::class)
    fun `compiler configuration stores cjvm target platform placeholder`() {
        val configuration = CompilerConfiguration()

        configuration.targetPlatform = CangJiePlatforms.cjvm

        assertSame(CangJiePlatforms.cjvm, configuration.targetPlatform)
    }

    /**
     * 创建 core 测试环境并保证 disposable 在写动作中释放。
     */
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
