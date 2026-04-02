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
)
