package org.cangnova.cangjie.lsp

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.cangnova.cangjie.CangJieCoreEnvironment
import org.cangnova.cangjie.CangJieCoreEnvironmentMode
import com.intellij.lang.LanguageParserDefinitions
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.lsp.analysis.AnalysisApiLspServiceRegistrar
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import java.util.concurrent.atomic.AtomicBoolean

/**
 * LSP 运行时绑定的仓颉核心环境。
 *
 * 当前框架阶段只负责把 headless IntelliJ/编译器环境托管好，
 * 后续真实分析接入时直接复用这里暴露的 [project] 与 [coreEnvironment]。
 */
class CangjieLspEnvironment private constructor(
    /**
     * 承载 LSP 环境内所有 IntelliJ/编译器资源的根 disposable。
     */
    private val rootDisposable: Disposable,

    /**
     * LSP 会话持有的仓颉核心编译器环境。
     */
    val coreEnvironment: CangJieCoreEnvironment,
) : AutoCloseable {
    /**
     * 防止环境重复释放的原子关闭标记。
     */
    private val closed = AtomicBoolean(false)

    /**
     * 当前 LSP 环境对应的 IntelliJ project。
     *
     * Analysis API、PSI 工厂和服务注册均通过该 project 访问平台服务。
     */
    val project: Project
        get() = coreEnvironment.project

    /**
     * 释放 LSP 环境持有的 IntelliJ 和编译器资源。
     *
     * 释放过程保证幂等，并在存在 application 时进入写动作以满足 PSI/VirtualFile 清理约束。
     */
    override fun close() {
        if (!closed.compareAndSet(false, true)) return

        // IntelliJ 的 PSI/VirtualFile 清理链要求写动作；LSP 测试会在 JSON-RPC worker 上关闭环境，
        // 这里统一收敛到写动作，避免测试 stderr 被线程断言噪音污染。
        val application = runCatching { ApplicationManager.getApplication() }.getOrNull()
        if (application != null) {
            application.runWriteAction {
                Disposer.dispose(rootDisposable)
            }
        } else {
            Disposer.dispose(rootDisposable)
        }
    }

    companion object {
        /**
         * 创建完整的 LSP 运行环境。
         *
         * 该方法负责建立根 disposable、核心环境、仓颉 PSI 基础设施，并注册 Analysis API 所需服务。
         */
        fun create(
            mode: CangJieCoreEnvironmentMode = CangJieCoreEnvironmentMode.Production,
        ): CangjieLspEnvironment {
            val rootDisposable = Disposer.newDisposable("cangjie-lsp-root")
            val coreEnvironment = CangJieCoreEnvironment.create(rootDisposable, mode)
            ensureCangjiePsiInfrastructure()
            return CangjieLspEnvironment(rootDisposable, coreEnvironment).also(AnalysisApiLspServiceRegistrar::register)
        }

        /**
         * LSP 运行时同样需要在内存文本上即时构造仓颉 PSI。
         *
         * Analysis API 测试框架已经显式注册了 parser definition；LSP 平台层也必须自己建立这条约束，
         * 否则 `PsiFileFactory` 只会退化成 plain text，后续所有 snapshot/session 语义都会失真。
         */
        private fun ensureCangjiePsiInfrastructure() {
            if (LanguageParserDefinitions.INSTANCE.forLanguage(CangJieLanguage) == null) {
                LanguageParserDefinitions.INSTANCE.addExplicitExtension(CangJieLanguage, CangJieParserDefinition())
            }
        }
    }
}
