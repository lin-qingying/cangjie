package org.cangnova.cangjie.analysis.test.framework

import com.intellij.mock.MockProject
import com.intellij.openapi.project.Project
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

/**
 * 提供 MockProject 的基础测试类（对齐 Kotlin 的 TestWithMockProject）。
 *
 * 注意：此类创建的 project 是一个功能极简的 stub，
 * 仅适用于不依赖 Project 实际功能的测试。
 */
abstract class TestWithMockProject : TestWithDisposable() {
    /**
     * 当前测试用例持有的 mock project。
     */
    private var _project: Project? = null

    /**
     * 当前测试可使用的非空 project。
     */
    protected val project: Project get() = _project!!

    /**
     * 在测试开始前基于当前 disposable 创建 mock project。
     */
    @BeforeEach
    fun initProject() {
        _project = MockProject(null, disposable)
    }

    /**
     * 在测试结束后清空 mock project 引用，实际资源由 root disposable 释放。
     */
    @AfterEach
    fun cleanup() {
        _project = null
    }
}
