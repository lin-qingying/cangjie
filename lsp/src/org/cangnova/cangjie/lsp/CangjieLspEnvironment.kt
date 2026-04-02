package org.cangnova.cangjie.lsp

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.cangnova.cangjie.CangJieCoreEnvironment
import org.cangnova.cangjie.CangJieCoreEnvironmentMode

/**
 * LSP 运行时绑定的仓颉核心环境。
 *
 * 当前框架阶段只负责把 headless IntelliJ/编译器环境托管好，
 * 后续真实分析接入时直接复用这里暴露的 [project] 与 [coreEnvironment]。
 */
class CangjieLspEnvironment private constructor(
    private val rootDisposable: Disposable,
    val coreEnvironment: CangJieCoreEnvironment,
) : AutoCloseable {
    val project: Project
        get() = coreEnvironment.project

    override fun close() {
        Disposer.dispose(rootDisposable)
    }

    companion object {
        fun create(
            mode: CangJieCoreEnvironmentMode = CangJieCoreEnvironmentMode.Production,
        ): CangjieLspEnvironment {
            val rootDisposable = Disposer.newDisposable("cangjie-lsp-root")
            val coreEnvironment = CangJieCoreEnvironment.create(rootDisposable, mode)
            return CangjieLspEnvironment(rootDisposable, coreEnvironment)
        }
    }
}
