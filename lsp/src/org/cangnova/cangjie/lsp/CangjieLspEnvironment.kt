package org.cangnova.cangjie.lsp

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import org.cangnova.cangjie.CangJieCoreEnvironment
import org.cangnova.cangjie.CangJieCoreEnvironmentMode
import com.intellij.lang.LanguageParserDefinitions
import org.cangnova.cangjie.lang.CangJieLanguage
import org.cangnova.cangjie.lsp.analysis.AnalysisApiLspServiceRegistrar
import org.cangnova.cangjie.parsing.CangJieParserDefinition

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
