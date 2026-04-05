package org.cangnova.cangjie.lsp

import org.cangnova.cangjie.CangJieCoreEnvironmentMode
import org.cangnova.cangjie.lsp.analysis.AnalysisApiCangjieAnalysisFacade
import org.cangnova.cangjie.lsp.analysis.CangjieAnalysisFacade
import org.cangnova.cangjie.lsp.analysis.CangjieAnalysisLifecycleContext
import org.cangnova.cangjie.lsp.capabilities.CangjieLanguageServerDescriptor

/**
 * LSP 服务器启动选项。
 */
data class CangjieLspServerOptions(
    val descriptor: CangjieLanguageServerDescriptor = CangjieLanguageServerDescriptor(),
    val environmentMode: CangJieCoreEnvironmentMode = CangJieCoreEnvironmentMode.Production,
    val environmentFactory: () -> CangjieLspEnvironment = { CangjieLspEnvironment.create(environmentMode) },
    val analysisFacadeFactory: (CangjieAnalysisLifecycleContext) -> CangjieAnalysisFacade = { AnalysisApiCangjieAnalysisFacade(it) },
    /**
     * 进程退出策略。
     *
     * 标准独立进程模式下应退出整个 JVM；
     * 但在测试、嵌入式宿主或同进程桥接场景中，必须允许替换为受控策略，
     * 否则一次 `exit` RPC 会连同宿主进程一起杀掉。
     */
    val exitHandler: (Int) -> Unit = { code -> System.exit(code) },
)
