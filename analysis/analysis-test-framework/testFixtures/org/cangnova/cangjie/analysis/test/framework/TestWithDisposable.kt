package org.cangnova.cangjie.analysis.test.framework

import com.intellij.openapi.Disposable
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
        _disposable?.let { Disposer.dispose(it) }
        _disposable = null
    }
}

