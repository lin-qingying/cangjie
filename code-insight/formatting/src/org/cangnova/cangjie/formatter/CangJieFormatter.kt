package org.cangnova.cangjie.formatter

import com.intellij.formatting.Formatter
import com.intellij.formatting.FormatterImpl
import com.intellij.lang.LanguageFormatting
import com.intellij.lang.LanguageFormattingRestriction
import com.intellij.mock.MockFileDocumentManagerImpl
import com.intellij.mock.MockProject
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.editor.impl.DocumentWriteAccessGuard
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerBase
import com.intellij.openapi.project.Project
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.core.CoreApplicationEnvironment
import com.intellij.mock.MockApplication
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.PomModel
import com.intellij.pom.PomModelAspect
import com.intellij.pom.core.impl.PomModelImpl
import com.intellij.pom.event.PomModelEvent
import com.intellij.pom.tree.TreeAspect
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiTreeChangeEvent
import com.intellij.psi.PsiTreeChangeListener
import com.intellij.psi.impl.source.PostprocessReformattingAspect
import com.intellij.psi.impl.source.PostprocessReformattingAspectImpl
import com.intellij.psi.codeStyle.CodeStyleSettings
import com.intellij.psi.codeStyle.CodeStyleSettingsManager
import com.intellij.psi.codeStyle.AppCodeStyleSettingsManager
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.codeStyle.ExternalFormatProcessor
import com.intellij.psi.codeStyle.FileIndentOptionsProvider
import com.intellij.psi.codeStyle.ProjectCodeStyleSettingsManager
import com.intellij.psi.impl.source.codeStyle.IndentHelper
import com.intellij.psi.impl.source.codeStyle.CodeStyleManagerImpl
import com.intellij.psi.impl.source.codeStyle.CodeFormatterFacade
import com.intellij.psi.impl.source.codeStyle.PostFormatProcessor
import com.intellij.psi.impl.source.codeStyle.PreFormatProcessor
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.psi.CjFile
import java.awt.EventQueue
import java.util.concurrent.atomic.AtomicReference

/**
 * 共享仓颉代码格式化入口。
 *
 * 规则只存在于 `code-insight:formatting`。IDE 和 LSP 都通过这里驱动 IntelliJ
 * formatter 引擎，避免在宿主层再次实现一套格式化语义。
 */
object CangJieFormatter {
    fun ensureFormattingModelRegistered() {
        ensureFormattingInfrastructureRegistered()
        if (LanguageFormatting.INSTANCE.forLanguage(CangJieLanguage) == null) {
            LanguageFormatting.INSTANCE.addExplicitExtension(CangJieLanguage, CangJieFormattingModelBuilder())
        }
    }

    fun format(
        file: CjFile,
        settings: CodeStyleSettings = CangJieCodeStyleSettingsFactory.createDefaultSettings(),
    ): String {
        ensureFormattingModelRegistered()

        val action = {
            val workingFile = file.copy() as CjFile
            val projectSettingsManager = ensureProjectCodeStyleSettingsManager(workingFile.project)
            projectSettingsManager.computeWithLocalSettings(settings) {
                CodeFormatterFacade(settings, CangJieLanguage).processElement(workingFile.node).text
            }
        }
        val application = runCatching { ApplicationManager.getApplication() }.getOrNull()

        return if (application != null) {
            runInEdtWriteAction(application, action)
        } else {
            action()
        }
    }

    /**
     * headless 宿主没有 plugin.xml 自动注册 formatting 相关扩展点，
     * 这里在共享 formatter 侧显式补齐最小平台基础设施。
     */
    private fun ensureFormattingInfrastructureRegistered() {
        val application = ApplicationManager.getApplication() as? MockApplication ?: return
        val extensionArea = application.extensionArea
        if (!extensionArea.hasExtensionPoint(FORMATTER_RESTRICTION_EXTENSION_POINT)) {
            CoreApplicationEnvironment.registerExtensionPoint(
                extensionArea,
                FORMATTER_RESTRICTION_EXTENSION_POINT,
                LanguageFormattingRestriction::class.java,
            )
        }
        if (!extensionArea.hasExtensionPoint(EXTERNAL_FORMAT_PROCESSOR_EXTENSION_POINT)) {
            CoreApplicationEnvironment.registerExtensionPoint(
                extensionArea,
                EXTERNAL_FORMAT_PROCESSOR_EXTENSION_POINT,
                ExternalFormatProcessor::class.java,
            )
        }
        if (!extensionArea.hasExtensionPoint(PRE_FORMAT_PROCESSOR_EXTENSION_POINT)) {
            CoreApplicationEnvironment.registerExtensionPoint(
                extensionArea,
                PRE_FORMAT_PROCESSOR_EXTENSION_POINT,
                PreFormatProcessor::class.java,
            )
        }
        if (!extensionArea.hasExtensionPoint(POST_FORMAT_PROCESSOR_EXTENSION_POINT)) {
            CoreApplicationEnvironment.registerExtensionPoint(
                extensionArea,
                POST_FORMAT_PROCESSOR_EXTENSION_POINT,
                PostFormatProcessor::class.java,
            )
        }
        if (!extensionArea.hasExtensionPoint(FILE_INDENT_OPTIONS_PROVIDER_EXTENSION_POINT)) {
            CoreApplicationEnvironment.registerExtensionPoint(
                extensionArea,
                FILE_INDENT_OPTIONS_PROVIDER_EXTENSION_POINT,
                FileIndentOptionsProvider::class.java,
            )
        }
        if (!extensionArea.hasExtensionPoint(DocumentWriteAccessGuard.EP_NAME.name)) {
            CoreApplicationEnvironment.registerExtensionPoint(
                extensionArea,
                DocumentWriteAccessGuard.EP_NAME.name,
                HeadlessDocumentWriteAccessGuard::class.java,
            )
        }
        if (application.getService(Formatter::class.java) == null) {
            application.registerService(Formatter::class.java, FormatterImpl::class.java)
        }
        ensureFileDocumentManager(application)
        ensureIndentHelper(application)
        ensureApplicationCodeStyleSettingsManager(application)
        ensureSmartPointerInfrastructureInitialized()
    }

    /**
     * FormattingDocumentModel 在格式化期间会回读项目级 code style settings manager。
     * IntelliJ 平台实际读取的是 `ProjectCodeStyleSettingsManager`，不是
     * `CodeStyleSettingsManager` 本体；headless 项目默认没有这对 service，
     * 因此共享 formatter 需要按平台真实 service 形状显式注册。
     */
    private fun ensureProjectCodeStyleSettingsManager(project: com.intellij.openapi.project.Project): CodeStyleSettingsManager {
        ensureProjectCodeStyleManager(project)
        ensureModifiablePsiProjectServices(project)
        project.getService(ProjectCodeStyleSettingsManager::class.java)?.let { return it }

        val mockProject = project as? MockProject
            ?: error("Headless formatting requires ProjectCodeStyleSettingsManager on project `${project.name}`")
        mockProject.registerService(ProjectCodeStyleSettingsManager::class.java, ProjectCodeStyleSettingsManager::class.java)
        return mockProject.getService(ProjectCodeStyleSettingsManager::class.java)
            ?: error("Failed to register ProjectCodeStyleSettingsManager for project `${project.name}`")
    }

    /**
     * `PsiBasedFormattingModel` 在应用 whitespace 变更时会通过
     * `CodeStyleManager.getInstance(project)` 进入平台的格式化禁用保护区。
     * headless 项目同样需要注册 IntelliJ 原生 `CodeStyleManagerImpl`。
     */
    private fun ensureProjectCodeStyleManager(project: com.intellij.openapi.project.Project) {
        if (project.getService(CodeStyleManager::class.java) != null) return

        val mockProject = project as? MockProject
            ?: error("Headless formatting requires CodeStyleManager on project `${project.name}`")
        mockProject.registerService(CodeStyleManager::class.java, CodeStyleManagerImpl::class.java)
    }

    /**
     * Kotlin 的 modifiable PSI 测试环境会额外注册 `TreeAspect` / `PomModel`，
     * `CodeStyleManagerImpl` 在执行 whitespace 替换时也依赖同一组服务。
     */
    private fun ensureModifiablePsiProjectServices(project: com.intellij.openapi.project.Project) {
        val mockProject = project as? MockProject
            ?: error("Headless formatting requires modifiable PSI services on project `${project.name}`")
        if (mockProject.getService(TreeAspect::class.java) == null) {
            mockProject.registerService(TreeAspect::class.java)
        }
        if (mockProject.getService(PomModel::class.java) == null) {
            mockProject.registerService(PomModel::class.java, HeadlessFormattingPomModel::class.java)
        }
    }

    /**
     * `ProjectCodeStyleSettingsManager` 初始化默认方案时会回读 application 级
     * `AppCodeStyleSettingsManager`，headless 宿主同样需要把这层 service 补齐。
     */
    private fun ensureApplicationCodeStyleSettingsManager(application: MockApplication) {
        if (application.getService(AppCodeStyleSettingsManager::class.java) == null) {
            application.registerService(AppCodeStyleSettingsManager::class.java, AppCodeStyleSettingsManager::class.java)
        }
    }

    /**
     * 对齐 Kotlin `AnalysisApiModifiablePsiTestServiceRegistrar`：
     * `MockFileDocumentManagerImpl` 默认不会把缓存文档挂回 virtual file，
     * 会导致后续可修改 PSI 路径看不到稳定 document 缓存。
     */
    private fun ensureFileDocumentManager(application: MockApplication) {
        val existing = application.getService(FileDocumentManager::class.java)
        if (existing is HeadlessFileDocumentManager) return

        application.picoContainer.unregisterComponent(FileDocumentManager::class.java.name)
        application.registerService(
            FileDocumentManager::class.java,
            HeadlessFileDocumentManager(),
        )
    }

    private fun ensureIndentHelper(application: MockApplication) {
        if (application.getService(IndentHelper::class.java) == null) {
            application.registerService(IndentHelper::class.java, HeadlessIndentHelper::class.java)
        }
    }

    /**
     * `SmartPointerManagerImpl.dispose()` 会在销毁阶段触发 `SmartPointerTracker` 的类初始化。
     * 如果首次初始化发生在 application 已经销毁之后，`LowMemoryWatcher.register()` 会向
     * 已销毁的根 disposable 挂子节点并直接抛错。
     *
     * 共享 formatter 会创建可修改 PSI，因此这里在基础设施初始化阶段提前完成 smart
     * pointer 相关类初始化，保证 watcher 绑定发生在 application 生命周期内。
     */
    private fun ensureSmartPointerInfrastructureInitialized() {
        Class.forName(
            "com.intellij.psi.impl.smartPointers.SmartPointerTracker",
            true,
            CangJieFormatter::class.java.classLoader,
        )
    }

    /**
     * IntelliJ 253 以后，`DocumentImpl` 的物理写入要求同时满足写动作与 EDT 断言。
     * 共享 formatter 在 headless/LSP/单测环境里也必须显式切到 EDT，再进入写动作。
     */
    private fun <T> runInEdtWriteAction(
        application: com.intellij.openapi.application.Application,
        action: () -> T,
    ): T {
        if (EventQueue.isDispatchThread()) {
            return runUndoTransparentWriteAction(application, action)
        }

        val result = AtomicReference<T>()
        val failure = AtomicReference<Throwable>()
        EventQueue.invokeAndWait {
            try {
                result.set(runUndoTransparentWriteAction(application, action))
            } catch (t: Throwable) {
                failure.set(t)
            }
        }

        failure.get()?.let { throw it }
        return result.get()
    }

    private fun <T> runUndoTransparentWriteAction(
        application: com.intellij.openapi.application.Application,
        action: () -> T,
    ): T {
        val result = AtomicReference<T>()
        CommandProcessor.getInstance().runUndoTransparentAction {
            result.set(application.runWriteAction<T>(action))
        }
        return result.get()
    }

    private const val FORMATTER_RESTRICTION_EXTENSION_POINT = "com.intellij.lang.formatter.restriction"
    private const val EXTERNAL_FORMAT_PROCESSOR_EXTENSION_POINT = "com.intellij.externalFormatProcessor"
    private const val PRE_FORMAT_PROCESSOR_EXTENSION_POINT = "com.intellij.preFormatProcessor"
    private const val POST_FORMAT_PROCESSOR_EXTENSION_POINT = "com.intellij.postFormatProcessor"
    private const val FILE_INDENT_OPTIONS_PROVIDER_EXTENSION_POINT = "com.intellij.fileIndentOptionsProvider"
}

@Suppress("UnstableApiUsage")
private class HeadlessDocumentWriteAccessGuard : DocumentWriteAccessGuard() {
    override fun isWritable(document: Document): Result = success()
}

private class HeadlessIndentHelper : IndentHelper() {
    override fun getIndent(file: PsiFile, element: com.intellij.lang.ASTNode): Int = 0

    override fun getIndent(file: PsiFile, element: com.intellij.lang.ASTNode, includeNonSpace: Boolean): Int = 0
}

private class HeadlessFileDocumentManager :
    MockFileDocumentManagerImpl(FileDocumentManagerBase.HARD_REF_TO_DOCUMENT_KEY, { DocumentImpl(it) }) {
    override fun getDocument(file: VirtualFile): Document? {
        val document = super.getDocument(file) ?: return null
        file.putUserDataIfAbsent(FileDocumentManagerBase.HARD_REF_TO_DOCUMENT_KEY, document)
        return document
    }
}

/**
 * `PomModelImpl` 默认只暴露 `TreeAspect` 与 `PsiEventWrapperAspect`。
 * formatter 在 `CodeStyleManagerImpl.performActionWithFormatterDisabled()` 中
 * 还会通过 `PomManager.getModel(project).getModelAspect(PostprocessReformattingAspect)` 读取
 * postprocess aspect，因此 headless formatter 需要在标准 `PomModelImpl` 之上补齐这层能力。
 */
private class HeadlessFormattingPomModel(
    project: Project,
) : PomModelImpl(project) {
    private val postprocessAspect = PostprocessReformattingAspectImpl(project)

    override fun <T : PomModelAspect?> getModelAspect(aClass: Class<T>): T? {
        if (aClass == PostprocessReformattingAspect::class.java) {
            @Suppress("UNCHECKED_CAST")
            return postprocessAspect as T
        }
        return super.getModelAspect(aClass)
    }

    override fun updateDependentAspects(event: PomModelEvent) {
        super.updateDependentAspects(event)
        postprocessAspect.update(event)
    }
}
