package org.cangnova.cangjie.test

import com.intellij.openapi.Disposable

/**
 * 表示 `TestDisposable`，承载测试基础设施中的配置数据、测试产物或处理步骤。
 */
class TestDisposable(private val debugName: String) : Disposable {
    /**
     * 执行 `dispose` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    override fun dispose() = Unit

    /**
     * 执行 `toString` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    override fun toString(): String = "TestDisposable($debugName)"
}
