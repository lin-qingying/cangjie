package org.cangnova.cangjie.analysis.test.framework

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo

/**
 * 提供 Disposable 生命周期管理的基础测试类。
 * Kotlin 对应文件：`external/kotlin/analysis/analysis-test-framework/testFixtures/org/jetbrains/kotlin/analysis/test/framework/TestWithDisposable.kt`
 */
abstract class TestWithDisposable {
    private var _disposable: Disposable? = null
    protected val disposable: Disposable get() = _disposable!!

    @BeforeEach
    fun initDisposable(testInfo: TestInfo) {
        _disposable = Disposer.newDisposable("disposable for ${testInfo.displayName}")
    }

    @AfterEach
    fun disposeDisposable() {
        _disposable?.let { disposable ->
            /**
             * IntelliJ 新线程模型要求这类 PSI / Project 资源清理发生在 write action 中。
             *
             * 这里对齐 Kotlin analysis test framework 的 `disposeRootInWriteAction` 语义，
             * 避免测试断言本身通过后，又在 AfterEach 清理阶段因为线程约束而误报失败。
             *
             * 同时，`testAllFilesPresentInModel()` 这类纯 generated-presence 测试并不会真正拉起
             * IntelliJ application。此时没有 write action 容器可用，应直接走普通 dispose。
             */
            val application = ApplicationManager.getApplication()
            if (application != null) {
                application.runWriteAction {
                    Disposer.dispose(disposable)
                }
            } else {
                Disposer.dispose(disposable)
            }
        }
        _disposable = null
    }
}

