package org.cangnova.cangjie.cli

import com.intellij.core.CoreApplicationEnvironment
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.AsyncExecutionService
import com.intellij.openapi.application.AppUIExecutor
import com.intellij.openapi.application.ExpirableExecutor
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.NonBlockingReadAction
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.application.TransactionGuardImpl
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.fileTypes.BinaryFileDecompiler
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiTreeChangeListener
import com.intellij.psi.impl.PsiTreeChangePreprocessor
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.concurrency.CancellablePromise
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.function.BooleanSupplier
import java.util.function.Consumer

sealed interface CangJieCoreEnvironmentMode {
    data object Production : CangJieCoreEnvironmentMode
    data object UnitTest : CangJieCoreEnvironmentMode
}

class CangJieCoreEnvironment private constructor(
    val projectEnvironment: CangjieCoreProjectEnvironment,
) {
    val project: Project
        get() = projectEnvironment.project

    val applicationEnvironment: CangjieCoreApplicationEnvironment
        get() = projectEnvironment.environment as CangjieCoreApplicationEnvironment

    companion object {
        fun createForTests(
            parentDisposable: Disposable,
        ): CangJieCoreEnvironment = create(parentDisposable, CangJieCoreEnvironmentMode.UnitTest)

        fun create(
            parentDisposable: Disposable,
            mode: CangJieCoreEnvironmentMode,
        ): CangJieCoreEnvironment {
            ensureIdeaStandaloneProperties()
            val appEnv = CangjieCoreApplicationEnvironment.create(
                parentDisposable = parentDisposable,
                unitTestMode = mode == CangJieCoreEnvironmentMode.UnitTest,
            )
            registerApplicationServices(appEnv)
            registerApplicationExtensionPoints(appEnv)
            val projectEnv = CangjieCoreProjectEnvironment(parentDisposable, appEnv)
            registerProjectExtensionPoints(projectEnv)
            return CangJieCoreEnvironment(projectEnv)
        }

        private fun registerApplicationServices(appEnv: CangjieCoreApplicationEnvironment) {
            appEnv.application.registerService(TransactionGuard::class.java, TransactionGuardImpl::class.java)
            if (appEnv.application.getService(AsyncExecutionService::class.java) == null) {
                appEnv.application.registerService(AsyncExecutionService::class.java, TestAsyncExecutionService::class.java)
            }
        }

        private fun registerApplicationExtensionPoints(appEnv: CangjieCoreApplicationEnvironment) {
            CoreApplicationEnvironment.registerExtensionPoint(
                appEnv.application.extensionArea,
                ExtensionPointName.create<BinaryFileDecompiler>("com.intellij.filetype.decompiler"),
                BinaryFileDecompiler::class.java,
            )
        }

        private fun registerProjectExtensionPoints(projectEnv: CangjieCoreProjectEnvironment) {
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

        private fun ensureIdeaStandaloneProperties() {
            if (System.getProperty(IDEA_HOME_PATH_PROPERTY).isNullOrBlank()) {
                System.setProperty(IDEA_HOME_PATH_PROPERTY, ideaHomePath.toString())
            }
            if (System.getProperty(IDEA_CONFIG_PATH_PROPERTY).isNullOrBlank()) {
                System.setProperty(IDEA_CONFIG_PATH_PROPERTY, Files.createDirectories(processTmpRoot.resolve("config")).toString())
            }
            if (System.getProperty(IDEA_SYSTEM_PATH_PROPERTY).isNullOrBlank()) {
                System.setProperty(IDEA_SYSTEM_PATH_PROPERTY, Files.createDirectories(processTmpRoot.resolve("system")).toString())
            }
            if (System.getProperty(IDEA_PLUGINS_COMPATIBLE_BUILD_PROPERTY).isNullOrBlank()) {
                System.setProperty(IDEA_PLUGINS_COMPATIBLE_BUILD_PROPERTY, "999.SNAPSHOT")
            }
            if (System.getProperty(IDEA_IGNORE_DISABLED_PLUGINS_PROPERTY).isNullOrBlank()) {
                System.setProperty(IDEA_IGNORE_DISABLED_PLUGINS_PROPERTY, "true")
            }
        }

        private const val IDEA_HOME_PATH_PROPERTY = "idea.home.path"
        private const val IDEA_CONFIG_PATH_PROPERTY = "idea.config.path"
        private const val IDEA_SYSTEM_PATH_PROPERTY = "idea.system.path"
        private const val IDEA_PLUGINS_COMPATIBLE_BUILD_PROPERTY = "idea.plugins.compatible.build"
        private const val IDEA_IGNORE_DISABLED_PLUGINS_PROPERTY = "idea.ignore.disabled.plugins"

        private val processTmpRoot: Path by lazy {
            Files.createTempDirectory("cangjie-test-intellij-home")
        }

        private val ideaHomePath: Path by lazy {
            val home = Files.createDirectories(processTmpRoot.resolve("idea-home"))
            val bin = Files.createDirectories(home.resolve("bin"))
            Files.writeString(home.resolve("build.txt"), "IC-999.SNAPSHOT")
            Files.writeString(bin.resolve("idea.properties"), "idea.config.path=\n")
            home
        }
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
