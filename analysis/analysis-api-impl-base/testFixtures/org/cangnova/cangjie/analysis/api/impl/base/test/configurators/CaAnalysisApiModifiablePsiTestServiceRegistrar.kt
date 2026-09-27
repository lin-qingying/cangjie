package org.cangnova.cangjie.analysis.api.impl.base.test.configurators

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.lang.ASTNode
import com.intellij.mock.MockApplication
import com.intellij.mock.MockFileDocumentManagerImpl
import com.intellij.mock.MockProject
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.editor.impl.DocumentWriteAccessGuard
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.impl.source.codeStyle.IndentHelper
import org.cangnova.cangjie.analysis.test.framework.projectStructure.cjTestModuleStructure
import org.cangnova.cangjie.analysis.test.framework.test.configurators.AnalysisApiTestServiceRegistrar
import org.cangnova.cangjie.test.services.TestServices

/**
 * 注册可修改 PSI 测试所需的 IntelliJ application 服务与写访问扩展点。
 */
object CaAnalysisApiModifiablePsiTestServiceRegistrar : AnalysisApiTestServiceRegistrar() {
    override fun registerApplicationServices(application: MockApplication, testServices: TestServices) {
        application.apply {
            registerFileDocumentManager()
            registerService(IndentHelper::class.java, MockIndentHelper::class.java)
            CoreApplicationEnvironment.registerExtensionPoint(
                extensionArea,
                DocumentWriteAccessGuard.EP_NAME,
                MockDocumentWriteAccessGuard::class.java,
            )
        }
    }

    /** 在项目和 application 尚存活时初始化 SmartPointerTracker，避免清理阶段才注册 LowMemoryWatcher。 */
    override fun registerProjectServices(project: MockProject, testServices: TestServices) {
        val sourceElement = testServices.cjTestModuleStructure.allSourceFiles.firstOrNull() ?: return
        SmartPointerManager.getInstance(project).createSmartPsiElementPointer(sourceElement)
    }

    private fun MockApplication.registerFileDocumentManager() {
        picoContainer.unregisterComponent(FileDocumentManager::class.java.name)
        registerService(
            FileDocumentManager::class.java,
            object : MockFileDocumentManagerImpl(
                FileDocumentManagerBase.HARD_REF_TO_DOCUMENT_KEY,
                { DocumentImpl(it) },
            ) {
                override fun getDocument(file: VirtualFile): Document? {
                    val document = super.getDocument(file) ?: return null
                    file.putUserDataIfAbsent(FileDocumentManagerBase.HARD_REF_TO_DOCUMENT_KEY, document)
                    return document
                }
            },
        )
    }
}

@Suppress("UnstableApiUsage")
private class MockDocumentWriteAccessGuard : DocumentWriteAccessGuard() {
    override fun isWritable(document: Document): Result = success()
}

private class MockIndentHelper : IndentHelper() {
    override fun getIndent(file: PsiFile, element: ASTNode): Int = 0

    override fun getIndent(file: PsiFile, element: ASTNode, includeNonSpace: Boolean): Int = 0
}
