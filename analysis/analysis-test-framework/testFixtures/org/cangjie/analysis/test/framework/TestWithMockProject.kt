package org.cangjie.analysis.test.framework

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
    private var _project: Project? = null
    protected val project: Project get() = _project!!

    @BeforeEach
    fun initProject() {
        _project = MockProject(null, disposable)
    }

    @AfterEach
    fun cleanup() {
        _project = null
    }
}
