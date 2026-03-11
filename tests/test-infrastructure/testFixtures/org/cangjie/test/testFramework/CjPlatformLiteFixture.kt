package org.cangjie.test.testFramework

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.core.CoreProjectEnvironment
import com.intellij.openapi.application.AsyncExecutionService
import com.intellij.openapi.application.AppUIExecutor
import com.intellij.openapi.application.ExpirableExecutor
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.NonBlockingReadAction
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.application.TransactionGuardImpl
import com.intellij.openapi.Disposable
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileTypes.BinaryFileDecompiler
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiTreeChangeListener
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.CancellablePromise
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.function.BooleanSupplier
import java.util.function.Consumer

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
        appEnv.application.registerService(TransactionGuard::class.java, TransactionGuardImpl::class.java)
        registerAsyncExecutionService(appEnv)
        CoreApplicationEnvironment.registerExtensionPoint(
            appEnv.application.extensionArea,
            ExtensionPointName.create<BinaryFileDecompiler>("com.intellij.filetype.decompiler"),
            BinaryFileDecompiler::class.java,
        )
        val projectEnv = CoreProjectEnvironment(fixtureDisposable, appEnv)
        registerProjectExtensionPoints(projectEnv)
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
        return java.io.File(filePath).readText(Charsets.UTF_8).replace("\r\n", "\n")
    }

    private fun registerAsyncExecutionService(appEnv: CoreApplicationEnvironment) {
        val application = appEnv.application
        if (application.getService(AsyncExecutionService::class.java) != null) return

        application.registerService(AsyncExecutionService::class.java, TestAsyncExecutionService::class.java)
    }

    private fun registerProjectExtensionPoints(projectEnv: CoreProjectEnvironment) {
        val area = projectEnv.project.extensionArea
        CoreApplicationEnvironment.registerExtensionPoint(
            area,
            PsiTreeChangePreprocessor.EP.name,
            PsiTreeChangePreprocessor::class.java,
        )
        CoreApplicationEnvironment.registerExtensionPoint(
            area,
            PsiTreeChangeListener.EP.name,
            PsiTreeChangeListener::class.java,
        )
    }
}

private class TestAsyncExecutionService : AsyncExecutionService() {
    override fun createExecutor(executor: Executor): ExpirableExecutor = ImmediateExpirableExecutor(executor)

    override fun createUIExecutor(modalityState: ModalityState): AppUIExecutor = ImmediateAppUiExecutor()

    override fun createWriteThreadExecutor(modalityState: ModalityState): AppUIExecutor = ImmediateAppUiExecutor()

    override fun <T> buildNonBlockingReadAction(computation: Callable<out T>): NonBlockingReadAction<T> {
        return ImmediateNonBlockingReadAction(computation)
    }
}

private class ImmediateExpirableExecutor(
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
        return submit(Callable {
            task.run()
            null
        })
    }
}

private class ImmediateAppUiExecutor : AppUIExecutor {
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
        return submit(Callable {
            task.run()
            null
        })
    }
}

private class ImmediateNonBlockingReadAction<T>(
    private val computation: Callable<out T>,
) : NonBlockingReadAction<T> {
    private var uiCallback: Consumer<in T>? = null

    override fun inSmartMode(project: Project): NonBlockingReadAction<T> = this

    override fun withDocumentsCommitted(project: Project): NonBlockingReadAction<T> = this

    override fun expireWhen(expireCondition: BooleanSupplier): NonBlockingReadAction<T> = this

    override fun wrapProgress(progressIndicator: ProgressIndicator): NonBlockingReadAction<T> = this

    override fun expireWith(parentDisposable: Disposable): NonBlockingReadAction<T> = this

    override fun finishOnUiThread(modalityState: ModalityState, uiThreadAction: Consumer<in T>): NonBlockingReadAction<T> = apply {
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
