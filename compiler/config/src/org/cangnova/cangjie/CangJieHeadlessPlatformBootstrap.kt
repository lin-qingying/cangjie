package org.cangnova.cangjie

import com.intellij.codeInsight.ContainerProvider
import com.intellij.codeInsight.TargetElementUtil
import com.intellij.codeInsight.TargetElementUtilExtender
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupManagerImpl
import com.intellij.codeInsight.multiverse.EditorContextManager
import com.intellij.core.CoreApplicationEnvironment
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
import com.intellij.openapi.application.ExpirableExecutor
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.NonBlockingReadAction
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.application.TransactionGuardImpl
import com.intellij.openapi.extensions.ExtensionsArea
import com.intellij.openapi.fileTypes.BinaryFileDecompiler
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSet
import com.intellij.openapi.vfs.VirtualFileSetFactory
import com.intellij.psi.FileContextProvider
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiTreeChangeListener
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import com.intellij.psi.impl.search.PsiSearchHelperImpl
import com.intellij.psi.impl.smartPointers.SmartPointerAnchorProvider
import com.intellij.psi.impl.source.resolve.reference.PsiReferenceContributorEP
import com.intellij.psi.meta.MetaDataContributor
import com.intellij.psi.search.UseScopeEnlarger
import com.intellij.util.QueryExecutor
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
 * 浠撻 headless IntelliJ 瀹瑰櫒鐨勭粺涓€骞冲彴寮曞鍣ㄣ€? *
 * 褰撳墠瀹炵幇鏄庣‘绾︽潫涓衡€滅函 IntelliJ 骞冲彴 + 浠撻 PSI 鑷韩鈥濓紝
 * 涓嶅紩鍏?Java PSI / Java 鎻掍欢渚濊禆銆? *
 * 鍥犳杩欓噷琛ラ綈鐨勬槸锛? * - 涓?references / target extraction / rename 鍩虹璁炬柦鐩存帴鐩稿叧鐨勫钩鍙?EP
 * - headless 鍦烘櫙涓嬬己澶便€佷絾 Analysis API 浼氱洿鎺ヨ姹傜殑搴旂敤绾ф湇鍔? * - 浠撻璇█鑷繁鐨?FileType / ParserDefinition 鍩虹璁炬柦
 */
internal object CangJieHeadlessPlatformBootstrap {
    fun initializeApplicationEnvironment(
        applicationEnvironment: CangjieCoreApplicationEnvironment,
    ) {
        registerApplicationExtensionPoints(applicationEnvironment.application.extensionArea)
        registerCangJiePsiInfrastructure(applicationEnvironment)
        registerApplicationServices(applicationEnvironment)
    }

    /**
     * 鐩墠鏃?Java PSI 渚濊禆锛屽洜姝ら」鐩骇鍙繚鐣欎笌 PSI 鐢熷懡鍛ㄦ湡鐩存帴鐩稿叧鐨?EP銆?     */
    fun preregisterProjectEnvironment(
        projectEnvironment: CangjieCoreProjectEnvironment,
    ) {
        registerProjectExtensionPoints(projectEnvironment.project.extensionArea)
    }

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
        registerEditorContextManagerIfMissing(project)
    }

    private fun registerApplicationExtensionPoints(area: ExtensionsArea) {
        registerExtensionPoint(area, "com.intellij.filetype.decompiler", BinaryFileDecompiler::class.java)
        registerExtensionPoint(area, "com.intellij.fileContextProvider", FileContextProvider::class.java)
        registerExtensionPoint(area, "com.intellij.psi.metaDataContributor", MetaDataContributor::class.java)
        registerExtensionPoint(area, "com.intellij.containerProvider", ContainerProvider::class.java)
        registerExtensionPoint(area, "com.intellij.metaLanguage", MetaLanguage::class.java)
        registerExtensionPoint(area, "com.intellij.smartPointer.anchorProvider", SmartPointerAnchorProvider::class.java)

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

    private fun registerCangJiePsiInfrastructure(
        applicationEnvironment: CangjieCoreApplicationEnvironment,
    ) {
        applicationEnvironment.registerFileType(CangJieFileType.INSTANCE, CangJieFileType.EXTENSION)
        applicationEnvironment.registerFileType(CangJieBuiltInFileType, CangJieBuiltInFileType.defaultExtension)

        applicationEnvironment.registerParserDefinition(CangJieParserDefinition())
    }

    private fun registerApplicationServices(
        applicationEnvironment: CangjieCoreApplicationEnvironment,
    ) {
        val application = applicationEnvironment.application

        registerApplicationServiceIfMissing(application, TransactionGuard::class.java, TransactionGuardImpl::class.java)
        registerApplicationServiceIfMissing(application, AsyncExecutionService::class.java, CangJieTestAsyncExecutionService::class.java)
        registerApplicationServiceIfMissing(application, PsiSymbolService::class.java, CangJieHeadlessPsiSymbolService::class.java)
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
        if (application.getService(TargetElementUtil::class.java) == null) {
            application.registerService(TargetElementUtil::class.java)
        }
    }

    private fun registerProjectExtensionPoints(area: ExtensionsArea) {
        registerExtensionPoint(area, PsiTreeChangePreprocessor.EP.name, PsiTreeChangePreprocessor::class.java)
        registerExtensionPoint(area, PsiTreeChangeListener.EP.name, PsiTreeChangeListener::class.java)
    }

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
 * 瀵归綈 Kotlin headless 鐜鐨勫啓鍔ㄤ綔绾︽潫銆? *
 * 榛樿 MockApplication 浼氭妸鍐欐潈闄愯涓烘亽鎴愮珛锛岃繖浼氭帺鐩?Analysis API
 * 鍦ㄥ啓鍔ㄤ綔绾︽潫涓婄殑鐪熷疄琛屼负銆傝繖閲屾敼鎴愪粎鍦?runWriteAction 浣滅敤鍩熷唴寮€鏀惧啓鏉冮檺銆? */
internal class CangJieCoreUnitTestApplication(
    parentDisposable: Disposable,
) : MockApplication(parentDisposable) {
    override fun isUnitTestMode(): Boolean = true

    override fun isWriteAccessAllowed(): Boolean = CangJieWriteAccessSupport.isWriteAccessAllowed()

    override fun runWriteAction(action: Runnable) {
        CangJieWriteAccessSupport.withWriteAccessAllowedInThread {
            action.run()
        }
    }

    override fun <T : Any?> runWriteAction(computation: Computable<T?>): T? {
        return CangJieWriteAccessSupport.withWriteAccessAllowedInThread {
            computation.compute()
        }
    }

    override fun <T : Any?, E : Throwable?> runWriteAction(
        computation: ThrowableComputable<T?, E?>,
    ): T? {
        return CangJieWriteAccessSupport.withWriteAccessAllowedInThread {
            computation.compute()
        }
    }
}

/**
 * 鍐欐潈闄愮姸鎬佹寜绾跨▼闅旂锛岄伩鍏嶅叡浜?Application 鏃朵笉鍚屼换鍔＄浉浜掓薄鏌撱€? */
private object CangJieWriteAccessSupport {
    private val isWriteAccessAllowedInThread = ThreadLocal.withInitial { false }

    fun isWriteAccessAllowed(): Boolean = isWriteAccessAllowedInThread.get()

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
 * Headless 娴嬭瘯/鏈嶅姟瀹夸富涓嬬殑寮傛鎵ц璇箟缁熶竴闄嶄负鍚屾鎵ц銆? */
internal class CangJieTestAsyncExecutionService : AsyncExecutionService() {
    override fun createExecutor(executor: Executor): ExpirableExecutor = CangJieImmediateExpirableExecutor(executor)

    override fun createUIExecutor(modalityState: ModalityState): AppUIExecutor = CangJieImmediateAppUiExecutor()

    override fun createWriteThreadExecutor(modalityState: ModalityState): AppUIExecutor = CangJieImmediateAppUiExecutor()

    override fun <T> buildNonBlockingReadAction(computation: Callable<out T>): NonBlockingReadAction<T> {
        return CangJieImmediateNonBlockingReadAction(computation)
    }
}

private class CangJieImmediateExpirableExecutor(
    executor: Executor = Executor(Runnable::run),
) : ExpirableExecutor {
    private val delegate = executor

    override fun expireWith(parentDisposable: Disposable): ExpirableExecutor = this

    override fun execute(command: Runnable) {
        delegate.execute(command)
    }

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

    override fun submit(task: Runnable): CancellablePromise<Any?> {
        return submit(
            Callable {
                task.run()
                null
            },
        )
    }
}

private class CangJieImmediateAppUiExecutor : AppUIExecutor {
    override fun later(): AppUIExecutor = this

    override fun withDocumentsCommitted(project: Project): AppUIExecutor = this

    override fun inSmartMode(project: Project): AppUIExecutor = this

    override fun expireWith(parentDisposable: Disposable): AppUIExecutor = this

    override fun execute(command: Runnable) {
        command.run()
    }

    override fun <T> submit(task: Callable<T>): CancellablePromise<T> {
        val promise = AsyncPromise<T>()
        try {
            promise.setResult(task.call())
        } catch (t: Throwable) {
            promise.setError(t)
        }
        return promise
    }

    override fun submit(task: Runnable): CancellablePromise<Any?> {
        return submit(
            Callable {
                task.run()
                null
            },
        )
    }
}

private class CangJieImmediateNonBlockingReadAction<T>(
    private val computation: Callable<out T>,
) : NonBlockingReadAction<T> {
    private var uiCallback: Consumer<in T>? = null

    override fun inSmartMode(project: Project): NonBlockingReadAction<T> = this

    override fun withDocumentsCommitted(project: Project): NonBlockingReadAction<T> = this

    override fun expireWhen(expireCondition: BooleanSupplier): NonBlockingReadAction<T> = this

    override fun wrapProgress(progressIndicator: ProgressIndicator): NonBlockingReadAction<T> = this

    override fun expireWith(parentDisposable: Disposable): NonBlockingReadAction<T> = this

    override fun finishOnUiThread(
        modalityState: ModalityState,
        uiThreadAction: Consumer<in T>,
    ): NonBlockingReadAction<T> = apply {
        uiCallback = uiThreadAction
    }

    override fun coalesceBy(vararg equality: Any): NonBlockingReadAction<T> = this

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
 * IntelliJ 242 鐨?CompactVirtualFileSetFactory 涓?CompactVirtualFileSet 鏋勯€犲櫒鍧囦负鍖呯鏈夈€? *
 * 杩欓噷涓嶉€€鍖栦负鏅€?Set锛岃€屾槸鏄惧紡鍙嶅皠澶嶇敤骞冲彴鑷繁鐨?CompactVirtualFileSet锛? * 淇濇寔涓?IntelliJ / Kotlin headless 鐜涓€鑷寸殑璇箟涓庣┖闂寸壒寰併€? */
internal class CangJieHeadlessVirtualFileSetFactory : VirtualFileSetFactory {
    override fun createCompactVirtualFileSet(): VirtualFileSet {
        return emptyConstructor.newInstance()
    }

    override fun createCompactVirtualFileSet(
        files: MutableCollection<out VirtualFile>,
    ): VirtualFileSet {
        return collectionConstructor.newInstance(files)
    }

    private companion object {
        private const val COMPACT_VIRTUAL_FILE_SET_CLASS_NAME = "com.intellij.openapi.vfs.CompactVirtualFileSet"

        private val compactVirtualFileSetClass: Class<out VirtualFileSet> by lazy {
            @Suppress("UNCHECKED_CAST")
            Class.forName(COMPACT_VIRTUAL_FILE_SET_CLASS_NAME) as Class<out VirtualFileSet>
        }

        private val emptyConstructor: Constructor<out VirtualFileSet> by lazy {
            compactVirtualFileSetClass.getDeclaredConstructor().apply {
                isAccessible = true
            }
        }

        private val collectionConstructor: Constructor<out VirtualFileSet> by lazy {
            compactVirtualFileSetClass.getDeclaredConstructor(Collection::class.java).apply {
                isAccessible = true
            }
        }
    }
}
