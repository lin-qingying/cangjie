package org.cangnova.cangjie

import com.intellij.codeInsight.ContainerProvider
import com.intellij.codeInsight.TargetElementUtil
import com.intellij.codeInsight.TargetElementUtilExtender
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupManagerImpl
import com.intellij.codeInsight.multiverse.EditorContextManager
import com.intellij.core.CoreApplicationEnvironment
import com.intellij.lang.ASTNode
import com.intellij.lang.MetaLanguage
import com.intellij.lang.LanguageExtensionPoint
import com.intellij.mock.MockApplication
import com.intellij.mock.MockProject
import com.intellij.model.psi.ImplicitReferenceProvider
import com.intellij.model.psi.PsiSymbolDeclarationProvider
import com.intellij.model.psi.PsiSymbolReferenceProviderBean
import com.intellij.model.psi.PsiSymbolReferenceService
import com.intellij.model.psi.PsiSymbolService
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.AsyncExecutionService
import com.intellij.openapi.application.AppUIExecutor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ExpirableExecutor
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.NonBlockingReadAction
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.application.TransactionGuardImpl
import com.intellij.openapi.extensions.ExtensionsArea
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSet
import com.intellij.openapi.vfs.VirtualFileSetFactory
import com.intellij.pom.PomModel
import com.intellij.pom.core.impl.PomModelImpl
import com.intellij.pom.tree.TreeAspect
import com.intellij.psi.FileContextProvider
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiTreeChangeListener
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import com.intellij.psi.impl.search.PsiSearchHelperImpl
import com.intellij.psi.impl.smartPointers.SmartPointerAnchorProvider
import com.intellij.psi.impl.source.codeStyle.IndentHelper
import com.intellij.psi.impl.source.tree.TreeCopyHandler
import com.intellij.psi.impl.source.resolve.reference.PsiReferenceContributorEP
import com.intellij.psi.meta.MetaDataContributor
import com.intellij.psi.search.UseScopeEnlarger
import com.intellij.psi.stubs.StubInconsistencyReporter
import com.intellij.util.QueryExecutor
import com.intellij.util.concurrency.TransferredWriteActionService
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.CancellablePromise
import java.lang.reflect.Constructor
import java.util.Collection
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.function.BooleanSupplier
import java.util.function.Consumer

/**
 * 仓颉 headless IntelliJ 容器的统一平台引导器。
 *
 * 当前实现明确约束为“纯 IntelliJ 平台 + 仓颉 PSI 自身”，不引入 Java PSI / Java 插件依赖。
 * 因此这里补齐的是 references、target extraction、rename 基础设施直接依赖的平台 EP，
 * 以及 headless 场景缺失但 Analysis API 会直接请求的应用级服务。
 */
internal object CangJieHeadlessPlatformBootstrap {
    /**
     * headless 统计服务读取设备 ID 的系统属性名。
     */
    private const val HEADLESS_STATISTICS_DEVICE_ID_PROPERTY = "idea.headless.statistics.device.id"

    /**
     * headless 统计服务读取 salt 的系统属性名。
     */
    private const val HEADLESS_STATISTICS_SALT_PROPERTY = "idea.headless.statistics.salt"

    /**
     * headless 统计服务读取最大上传文件数的系统属性名。
     */
    private const val HEADLESS_STATISTICS_MAX_FILES_TO_SEND_PROPERTY = "idea.headless.statistics.max.files.to.send"

    /**
     * headless 环境固定使用的虚拟统计设备 ID。
     */
    private const val HEADLESS_STATISTICS_DEVICE_ID = "000000000000000-0000-0000-0000-000000000000"

    /**
     * headless 环境固定使用的统计 salt。
     */
    private const val HEADLESS_STATISTICS_SALT = "cangjie-headless-statistics-salt"

    /**
     * headless 环境禁止统计服务上传文件。
     */
    private const val HEADLESS_STATISTICS_MAX_FILES_TO_SEND = "0"

    /**
     * 初始化 application 级扩展点、仓颉 PSI 基础设施和平台服务。
     */
    fun initializeApplicationEnvironment(
        applicationEnvironment: CangjieCoreApplicationEnvironment,
    ) {
        registerApplicationExtensionPoints(applicationEnvironment.application.extensionArea)
        registerCangJiePsiInfrastructure(applicationEnvironment)
        registerApplicationServices(applicationEnvironment)
    }

    /**
     * 预注册项目级扩展点。
     *
     * 当前没有 Java PSI 依赖，因此项目级只保留与 PSI 生命周期直接相关的 EP。
     */
    fun preregisterProjectEnvironment(
        projectEnvironment: CangjieCoreProjectEnvironment,
    ) {
        registerProjectExtensionPoints(projectEnvironment.project.extensionArea)
    }

    /**
     * 初始化 project 级服务和需要项目实例参与构造的平台服务。
     */
    fun initializeProjectEnvironment(
        projectEnvironment: CangjieCoreProjectEnvironment,
    ) {
        val project = projectEnvironment.project
        if (project.getService(com.intellij.psi.search.PsiSearchHelper::class.java) == null) {
            project.registerService(com.intellij.psi.search.PsiSearchHelper::class.java, PsiSearchHelperImpl::class.java)
        }
        if (project.getService(LookupManager::class.java) == null) {
            project.registerService(LookupManager::class.java, LookupManagerImpl::class.java)
        }
        registerModifiablePsiProjectServices(project)
        registerEditorContextManagerIfMissing(project)
    }

    /**
     * 注册 application 级 IntelliJ 扩展点，覆盖仓颉引用、symbol 和 target 提取链路。
     */
    private fun registerApplicationExtensionPoints(area: ExtensionsArea) {
        registerExtensionPoint(area, "com.intellij.fileContextProvider", FileContextProvider::class.java)
        registerExtensionPoint(area, "com.intellij.psi.metaDataContributor", MetaDataContributor::class.java)
        registerExtensionPoint(area, "com.intellij.containerProvider", ContainerProvider::class.java)
        registerExtensionPoint(area, "com.intellij.metaLanguage", MetaLanguage::class.java)
        registerExtensionPoint(area, "com.intellij.smartPointer.anchorProvider", SmartPointerAnchorProvider::class.java)
        registerExtensionPoint(area, TreeCopyHandler.EP_NAME.name, TreeCopyHandler::class.java)

        // References / Symbol / Target 提取链路直接依赖的 IntelliJ 平台扩展点。
        registerExtensionPoint(area, PsiReferenceContributor.EP_NAME.name, PsiReferenceContributorEP::class.java)
        registerExtensionPoint(area, "com.intellij.psi.symbolReferenceProvider", PsiSymbolReferenceProviderBean::class.java)
        registerExtensionPoint(area, "com.intellij.psi.implicitReferenceProvider", ImplicitReferenceProvider::class.java)
        registerExtensionPoint(area, "com.intellij.psi.declarationProvider", PsiSymbolDeclarationProvider::class.java)
        registerExtensionPoint(area, UseScopeEnlarger.EP_NAME.name, UseScopeEnlarger::class.java)
        registerExtensionPoint(area, "com.intellij.referencesSearch", QueryExecutor::class.java)
        registerExtensionPoint(area, "com.intellij.targetElementEvaluator", LanguageExtensionPoint::class.java)
        registerExtensionPoint(area, "com.intellij.targetElementUtilExtender", TargetElementUtilExtender::class.java)
    }

    /**
     * 注册仓颉文件类型和 parser definition。
     */
    private fun registerCangJiePsiInfrastructure(
        applicationEnvironment: CangjieCoreApplicationEnvironment,
    ) {
        applicationEnvironment.registerFileType(CangJieFileType.INSTANCE, CangJieFileType.EXTENSION)
        applicationEnvironment.registerFileType(CangJieBuiltInFileType, CangJieBuiltInFileType.defaultExtension)

        applicationEnvironment.registerParserDefinition(CangJieParserDefinition())
    }

    /**
     * 注册 headless application 缺失但仓颉前端/Analysis API 需要的服务。
     */
    private fun registerApplicationServices(
        applicationEnvironment: CangjieCoreApplicationEnvironment,
    ) {
        val application = applicationEnvironment.application

        ensureHeadlessStatisticsProperties()
        registerApplicationServiceIfMissing(application, TransactionGuard::class.java, TransactionGuardImpl::class.java)
        registerApplicationServiceIfMissing(application, AsyncExecutionService::class.java, CangJieTestAsyncExecutionService::class.java)
        registerApplicationServiceIfMissing(application, PsiSymbolService::class.java, CangJieHeadlessPsiSymbolService::class.java)
        registerApplicationServiceIfMissing(application, IndentHelper::class.java, CangJieHeadlessIndentHelper::class.java)
        registerApplicationCoroutineScopeServiceIfMissing(application)
        registerStatisticsApplicationServicesIfMissing(application)
        registerApplicationServiceIfMissing(
            application,
            PsiSymbolReferenceService::class.java,
            CangJieHeadlessPsiSymbolReferenceService::class.java,
        )
        registerApplicationServiceIfMissing(
            application,
            VirtualFileSetFactory::class.java,
            CangJieHeadlessVirtualFileSetFactory::class.java,
        )
        if (application.getService(TransferredWriteActionService::class.java) == null) {
            application.registerService(
                TransferredWriteActionService::class.java,
                CangJieTransferredWriteActionService(),
            )
        }
        if (application.getService(StubInconsistencyReporter::class.java) == null) {
            application.registerService(
                StubInconsistencyReporter::class.java,
                CangJieHeadlessStubInconsistencyReporter(),
            )
        }
        if (application.getService(TargetElementUtil::class.java) == null) {
            application.registerService(TargetElementUtil::class.java)
        }
    }

    /**
     * IntelliJ statistics 在 headless 环境允许通过系统属性提供稳定的 device id 与 salt。
     *
     * 这保持 `RenameUtil` 的原始统计调用链可执行，同时避免 `DeviceIdManager` 退回到完整 IDE
     * `ApplicationInfo.xml` / 用户偏好存储路径。属性只在宿主未显式提供时补齐。
     */
    private fun ensureHeadlessStatisticsProperties() {
        setHeadlessStatisticsPropertyIfMissing(HEADLESS_STATISTICS_DEVICE_ID_PROPERTY, HEADLESS_STATISTICS_DEVICE_ID)
        setHeadlessStatisticsPropertyIfMissing(HEADLESS_STATISTICS_SALT_PROPERTY, HEADLESS_STATISTICS_SALT)
        setHeadlessStatisticsPropertyIfMissing(
            HEADLESS_STATISTICS_MAX_FILES_TO_SEND_PROPERTY,
            HEADLESS_STATISTICS_MAX_FILES_TO_SEND,
        )
    }

    /**
     * 仅在宿主未显式提供时写入 headless 统计系统属性。
     */
    private fun setHeadlessStatisticsPropertyIfMissing(name: String, value: String) {
        if (System.getProperty(name) == null) {
            System.setProperty(name, value)
        }
    }

    /**
     * IntelliJ 253 平台的若干无 UI service 通过构造函数注入 application-level CoroutineScope。
     *
     * `MockApplication` 本身已经持有受 parentDisposable 管理的 scope；这里把它暴露到 Pico service 容器，
     * 使 refactoring/statistics 等平台服务按原始构造路径初始化，而不是在调用点规避平台逻辑。
     */
    private fun registerApplicationCoroutineScopeServiceIfMissing(application: MockApplication) {
        val coroutineScopeClass = Class.forName("kotlinx.coroutines.CoroutineScope")
        if (application.getService(coroutineScopeClass) != null) return

        val coroutineScope = application::class.java.getMethod("getCoroutineScope").invoke(application)
        @Suppress("UNCHECKED_CAST")
        application.registerService(coroutineScopeClass as Class<Any>, coroutineScope)
    }

    /**
     * IntelliJ refactoring 会记录 rename usage 统计；headless 容器没有加载完整 platform plugin.xml，
     * 因此需要补齐统计链路中通过 `getInstance()` 访问的 application services。
     */
    private fun registerStatisticsApplicationServicesIfMissing(application: MockApplication) {
        registerReflectiveApplicationServiceIfMissing(
            application,
            "com.intellij.internal.statistic.eventLog.validator.storage.persistence.EventLogMetadataSettingsPersistence",
        )
        registerReflectiveApplicationServiceIfMissing(
            application,
            "com.intellij.internal.statistic.eventLog.EventLogConfigOptionsService",
        )
    }

    /**
     * 通过类名反射注册可由默认构造函数创建的 application service。
     */
    private fun registerReflectiveApplicationServiceIfMissing(
        application: MockApplication,
        className: String,
    ) {
        val serviceClass = Class.forName(className)
        if (application.getService(serviceClass) != null) return

        @Suppress("UNCHECKED_CAST")
        application.registerService(serviceClass as Class<Any>)
    }

    /**
     * 注册 project 级 PSI 生命周期扩展点。
     */
    private fun registerProjectExtensionPoints(area: ExtensionsArea) {
        registerExtensionPoint(area, PsiTreeChangePreprocessor.EP.name, PsiTreeChangePreprocessor::class.java)
        registerExtensionPoint(area, PsiTreeChangeListener.EP.name, PsiTreeChangeListener::class.java)
    }

    /**
     * 如果指定 application service 尚未存在，则按实现类注册。
     */
    private fun <T : Any> registerApplicationServiceIfMissing(
        application: MockApplication,
        serviceInterface: Class<T>,
        implementationClass: Class<out T>,
    ) {
        if (application.getService(serviceInterface) == null) {
            application.registerService(serviceInterface, implementationClass)
        }
    }

    /**
     * `EditorContextManagerImpl` 是 IntelliJ 平台 internal 类，
     * headless 宿主仍需按平台构造签名 `(Project, CoroutineScope)` 物化该 project service。
     */
    private fun registerModifiablePsiProjectServices(project: MockProject) {
        if (project.getService(TreeAspect::class.java) == null) {
            project.registerService(TreeAspect::class.java)
        }
        if (project.getService(PomModel::class.java) == null) {
            project.registerService(PomModel::class.java, PomModelImpl::class.java)
        }
    }

    /**
     * 如果缺失 `EditorContextManager`，则按平台 internal 构造签名反射创建。
     */
    private fun registerEditorContextManagerIfMissing(project: MockProject) {
        if (project.getService(EditorContextManager::class.java) != null) return

        val implementationClass = Class.forName("com.intellij.codeInsight.multiverse.EditorContextManagerImpl")
        val coroutineScopeClass = Class.forName("kotlinx.coroutines.CoroutineScope")
        val constructor = implementationClass.getDeclaredConstructor(Project::class.java, coroutineScopeClass)
        constructor.isAccessible = true
        val coroutineScope = project::class.java.getMethod("getCoroutineScope").invoke(project)
        @Suppress("UNCHECKED_CAST")
        val serviceInstance = constructor.newInstance(project, coroutineScope) as EditorContextManager
        project.registerService(EditorContextManager::class.java, serviceInstance)
    }

    /**
     * 向指定扩展区注册扩展点，已存在时保持幂等。
     */
    private fun <T : Any> registerExtensionPoint(
        area: ExtensionsArea,
        name: String,
        extensionPointClass: Class<out T>,
    ) {
        if (!area.hasExtensionPoint(name)) {
            CoreApplicationEnvironment.registerExtensionPoint(area, name, extensionPointClass)
        }
    }
}

/**
 * 对齐 Kotlin headless 环境的写动作约束。
 *
 * 默认 MockApplication 会把写权限视为恒成立，这会掩盖 Analysis API 在写动作约束上的真实行为。
 * 这里改成仅在 `runWriteAction` 作用域内开放写权限。
 */
internal class CangJieCoreUnitTestApplication(
    parentDisposable: Disposable,
) : MockApplication(parentDisposable) {
    /**
     * 单测核心环境固定报告 unit-test 模式。
     */
    override fun isUnitTestMode(): Boolean = true

    /**
     * 根据当前线程写动作标记返回写权限状态。
     */
    override fun isWriteAccessAllowed(): Boolean = CangJieWriteAccessSupport.isWriteAccessAllowed()

    /**
     * 在当前线程临时开放写权限并执行写动作。
     */
    override fun runWriteAction(action: Runnable) {
        CangJieWriteAccessSupport.withWriteAccessAllowedInThread {
            action.run()
        }
    }

    /**
     * 在当前线程临时开放写权限并执行可返回值的写动作。
     */
    override fun <T : Any?> runWriteAction(computation: Computable<T?>): T? {
        return CangJieWriteAccessSupport.withWriteAccessAllowedInThread {
            computation.compute()
        }
    }

    /**
     * 在当前线程临时开放写权限并执行可抛异常的写动作。
     */
    override fun <T : Any?, E : Throwable?> runWriteAction(
        computation: ThrowableComputable<T?, E?>,
    ): T? {
        return CangJieWriteAccessSupport.withWriteAccessAllowedInThread {
            computation.compute()
        }
    }
}

/**
 * 写权限状态按线程隔离，避免共享 application 时不同任务相互污染。
 */
private object CangJieWriteAccessSupport {
    /**
     * 当前线程是否处于仓颉 core 写动作作用域。
     */
    private val isWriteAccessAllowedInThread = ThreadLocal.withInitial { false }

    /**
     * 返回当前线程是否允许写访问。
     */
    fun isWriteAccessAllowed(): Boolean = isWriteAccessAllowedInThread.get()

    /**
     * 在当前线程临时打开写访问标记并执行回调，结束后恢复为关闭状态。
     */
    inline fun <T> withWriteAccessAllowedInThread(action: () -> T): T {
        isWriteAccessAllowedInThread.set(true)
        try {
            return action
                .invoke()
        } finally {
            isWriteAccessAllowedInThread.set(false)
        }
    }
}

/**
 * Headless 测试/服务宿主下的异步执行语义统一降为同步执行。
 */
internal class CangJieTestAsyncExecutionService : AsyncExecutionService() {
    /**
     * 创建立即执行的可过期 executor。
     */
    override fun createExecutor(executor: Executor): ExpirableExecutor = CangJieImmediateExpirableExecutor(executor)

    /**
     * 创建立即执行的 UI executor，并以写动作方式运行命令。
     */
    override fun createUIExecutor(modalityState: ModalityState): AppUIExecutor = CangJieImmediateAppUiExecutor(runAsWriteAction = true)

    /**
     * 创建立即执行的写线程 executor。
     */
    override fun createWriteThreadExecutor(modalityState: ModalityState): AppUIExecutor = CangJieImmediateAppUiExecutor(runAsWriteAction = true)

    /**
     * 创建立即执行的 non-blocking read action。
     */
    override fun <T> buildNonBlockingReadAction(computation: Callable<out T>): NonBlockingReadAction<T> {
        return CangJieImmediateNonBlockingReadAction(computation)
    }
}

/**
 * 将 ExpirableExecutor 语义压缩为同步执行的 headless executor。
 */
private class CangJieImmediateExpirableExecutor(
    executor: Executor = Executor(Runnable::run),
) : ExpirableExecutor {
    /**
     * 实际执行命令的底层 executor。
     */
    private val delegate = executor

    /**
     * Headless 立即执行器不跟踪 disposable 过期状态，直接返回自身。
     */
    override fun expireWith(parentDisposable: Disposable): ExpirableExecutor = this

    /**
     * 立即委托执行命令。
     */
    override fun execute(command: Runnable) {
        delegate.execute(command)
    }

    /**
     * 提交 callable 并把结果或异常写入 promise。
     */
    override fun <T> submit(task: Callable<T>): CancellablePromise<T> {
        val promise = AsyncPromise<T>()
        try {
            delegate.execute {
                try {
                    promise.setResult(task.call())
                } catch (t: Throwable) {
                    promise.setError(t)
                }
            }
        } catch (t: Throwable) {
            promise.setError(t)
        }
        return promise
    }

    /**
     * 提交 runnable，并以 `null` 作为成功结果。
     */
    override fun submit(task: Runnable): CancellablePromise<Any?> {
        return submit(
            Callable {
                task.run()
                null
            },
        )
    }
}

/**
 * Headless 环境中的立即执行 UI executor。
 */
private class CangJieImmediateAppUiExecutor(
    /**
     * 是否将执行体包装进 application 写动作。
     */
    private val runAsWriteAction: Boolean,
) : AppUIExecutor {
    /**
     * Headless 环境不延迟调度，直接返回当前 executor。
     */
    override fun later(): AppUIExecutor = this

    /**
     * Headless 环境不需要等待文档提交，直接返回当前 executor。
     */
    override fun withDocumentsCommitted(project: Project): AppUIExecutor = this

    /**
     * Headless 环境没有 dumb/smart 模式切换，直接返回当前 executor。
     */
    override fun inSmartMode(project: Project): AppUIExecutor = this

    /**
     * Headless 立即执行器不跟踪 disposable 过期状态，直接返回自身。
     */
    override fun expireWith(parentDisposable: Disposable): AppUIExecutor = this

    /**
     * 立即执行命令，并按配置决定是否包裹写动作。
     */
    override fun execute(command: Runnable) {
        if (runAsWriteAction) {
            ApplicationManager.getApplication().runWriteAction(command)
        } else {
            command.run()
        }
    }

    /**
     * 立即执行 callable，并把结果或异常写入 promise。
     */
    override fun <T> submit(task: Callable<T>): CancellablePromise<T> {
        val promise = AsyncPromise<T>()
        try {
            if (runAsWriteAction) {
                promise.setResult(
                    ApplicationManager.getApplication().runWriteAction(
                        Computable {
                            task.call()
                        },
                    ),
                )
            } else {
                promise.setResult(task.call())
            }
        } catch (t: Throwable) {
            promise.setError(t)
        }
        return promise
    }

    /**
     * 提交 runnable，并以 `null` 作为成功结果。
     */
    override fun submit(task: Runnable): CancellablePromise<Any?> {
        return submit(
            Callable {
                task.run()
                null
            },
        )
    }
}

/**
 * Headless 环境中的 transferred write action 服务实现。
 */
private class CangJieTransferredWriteActionService : TransferredWriteActionService {
    /**
     * Headless 环境没有 EDT 切换，直接执行传入写动作。
     */
    override fun runOnEdtWithTransferredWriteActionAndWait(action: Runnable) {
        action.run()
    }
}

/**
 * Headless PSI mutation 只需要稳定保存空白信息；真实格式化由 code-insight formatter 负责。
 */
private class CangJieHeadlessIndentHelper : IndentHelper() {
    /**
     * Headless 缩进服务不计算实际缩进，统一返回 0。
     */
    override fun getIndent(file: PsiFile, element: ASTNode): Int = 0

    /**
     * Headless 缩进服务不区分是否包含非空白字符，统一返回 0。
     */
    override fun getIndent(file: PsiFile, element: ASTNode, includeNonSpace: Boolean): Int = 0
}

/**
 * Headless 环境下的 stub 不一致报告器。
 */
private class CangJieHeadlessStubInconsistencyReporter : StubInconsistencyReporter {
    /**
     * 忽略 PSI 文本与 stub 之间的一致性报告。
     */
    override fun reportStubInconsistencyBetweenPsiAndText(
        project: Project,
        sourceOfCheck: StubInconsistencyReporter.SourceOfCheck?,
        inconsistencyType: StubInconsistencyReporter.InconsistencyType,
    ) {
    }

    /**
     * 忽略带强制类型的 PSI 文本与 stub 一致性报告。
     */
    override fun reportStubInconsistencyBetweenPsiAndText(
        project: Project,
        sourceOfCheck: StubInconsistencyReporter.SourceOfCheck,
        inconsistencyType: StubInconsistencyReporter.InconsistencyType,
        enforcedInconsistencyType: StubInconsistencyReporter.EnforcedInconsistencyType?,
    ) {
    }

    /**
     * 忽略 Kotlin 描述符缺失报告，仓颉 headless 环境不依赖该描述符链路。
     */
    override fun reportKotlinDescriptorNotFound(project: Project?) {
    }

    /**
     * 忽略 Kotlin 类名缺失报告，仓颉 headless 环境不依赖该描述符链路。
     */
    override fun reportKotlinMissingClassName(project: Project, hasClassName: Boolean, hasFacadeClassName: Boolean) {
    }

    /**
     * 忽略 stub tree 与索引不一致报告。
     */
    override fun reportStubTreeAndIndexDoNotMatch(
        project: Project,
        source: StubInconsistencyReporter.StubTreeAndIndexDoNotMatchSource,
    ) {
    }
}

/**
 * Headless 环境中的同步 NonBlockingReadAction 实现。
 */
private class CangJieImmediateNonBlockingReadAction<T>(
    /**
     * 需要在 read action 语义下执行的计算。
     */
    private val computation: Callable<out T>,
) : NonBlockingReadAction<T> {
    /**
     * 计算完成后需要在 UI 线程执行的回调；headless 环境中同步调用。
     */
    private var uiCallback: Consumer<in T>? = null

    /**
     * Headless 环境没有 smart mode 等待，直接返回自身。
     */
    override fun inSmartMode(project: Project): NonBlockingReadAction<T> = this

    /**
     * Headless 环境不等待文档提交，直接返回自身。
     */
    override fun withDocumentsCommitted(project: Project): NonBlockingReadAction<T> = this

    /**
     * Headless 同步实现不跟踪过期条件，直接返回自身。
     */
    override fun expireWhen(expireCondition: BooleanSupplier): NonBlockingReadAction<T> = this

    /**
     * Headless 同步实现不包装进度指示器，直接返回自身。
     */
    override fun wrapProgress(progressIndicator: ProgressIndicator): NonBlockingReadAction<T> = this

    /**
     * Headless 同步实现不跟踪 disposable 过期状态，直接返回自身。
     */
    override fun expireWith(parentDisposable: Disposable): NonBlockingReadAction<T> = this

    /**
     * 记录计算完成后的 UI 回调，并继续返回当前 action。
     */
    override fun finishOnUiThread(
        modalityState: ModalityState,
        uiThreadAction: Consumer<in T>,
    ): NonBlockingReadAction<T> = apply {
        uiCallback = uiThreadAction
    }

    /**
     * Headless 同步实现不做任务合并，直接返回自身。
     */
    override fun coalesceBy(vararg equality: Any): NonBlockingReadAction<T> = this

    /**
     * 使用传入 executor 执行同步计算，并将结果写入 promise。
     */
    override fun submit(backgroundThreadExecutor: Executor): CancellablePromise<T> {
        val promise = AsyncPromise<T>()
        backgroundThreadExecutor.execute {
            try {
                val result = executeSynchronously()
                promise.setResult(result)
            } catch (t: Throwable) {
                promise.setError(t)
            }
        }
        return promise
    }

    /**
     * 同步执行计算，并在成功后立即调用 UI 回调。
     */
    override fun executeSynchronously(): T {
        try {
            val result = computation.call()
            uiCallback?.accept(result)
            return result
        } catch (e: ProcessCanceledException) {
            throw e
        } catch (e: RuntimeException) {
            throw e
        } catch (t: Throwable) {
            throw RuntimeException(t)
        }
    }
}

/**
 * IntelliJ 242 的 `CompactVirtualFileSetFactory` 与 `CompactVirtualFileSet` 构造器均为包私有。
 *
 * 这里不退化为普通 `Set`，而是显式反射复用平台自己的 `CompactVirtualFileSet`，
 * 保持与 IntelliJ / Kotlin headless 环境一致的语义与空间特征。
 */
internal class CangJieHeadlessVirtualFileSetFactory : VirtualFileSetFactory {
    /**
     * 创建空的紧凑虚拟文件集合。
     */
    override fun createCompactVirtualFileSet(): VirtualFileSet {
        return emptyConstructor.newInstance()
    }

    /**
     * 基于已有虚拟文件集合创建紧凑虚拟文件集合。
     */
    override fun createCompactVirtualFileSet(
        files: MutableCollection<out VirtualFile>,
    ): VirtualFileSet {
        return collectionConstructor.newInstance(files)
    }

    private companion object {
        /**
         * IntelliJ 平台内部紧凑虚拟文件集合实现类名。
         */
        private const val COMPACT_VIRTUAL_FILE_SET_CLASS_NAME = "com.intellij.openapi.vfs.CompactVirtualFileSet"

        /**
         * 反射解析到的紧凑虚拟文件集合实现类。
         */
        private val compactVirtualFileSetClass: Class<out VirtualFileSet> by lazy {
            @Suppress("UNCHECKED_CAST")
            Class.forName(COMPACT_VIRTUAL_FILE_SET_CLASS_NAME) as Class<out VirtualFileSet>
        }

        /**
         * 创建空紧凑集合的无参构造器。
         */
        private val emptyConstructor: Constructor<out VirtualFileSet> by lazy {
            compactVirtualFileSetClass.getDeclaredConstructor().apply {
                isAccessible = true
            }
        }

        /**
         * 基于已有虚拟文件集合创建紧凑集合的构造器。
         */
        private val collectionConstructor: Constructor<out VirtualFileSet> by lazy {
            compactVirtualFileSetClass.getDeclaredConstructor(Collection::class.java).apply {
                isAccessible = true
            }
        }
    }
}
