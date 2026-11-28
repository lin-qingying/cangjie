package org.cangnova.cangjie.lsp

import org.cangnova.cangjie.CangJieCoreEnvironmentMode
import org.cangnova.cangjie.lsp.analysis.AnalysisApiCangjieAnalysisFacade
import org.cangnova.cangjie.lsp.analysis.CangjieAnalysisFacade
import org.cangnova.cangjie.lsp.analysis.CangjieAnalysisLifecycleContext
import org.cangnova.cangjie.lsp.analysis.LspMacroExpansionOptions
import org.cangnova.cangjie.lsp.capabilities.CangjieLanguageServerDescriptor

/**
 * LSP 服务器启动选项。
 */
data class CangjieLspServerOptions(
    /**
     * 描述服务器默认能力、名称、版本和协议特性的配置对象。
     */
    val descriptor: CangjieLanguageServerDescriptor = CangjieLanguageServerDescriptor(),

    /**
     * 创建编译器核心环境时使用的运行模式。
     */
    val environmentMode: CangJieCoreEnvironmentMode = CangJieCoreEnvironmentMode.Production,

    /**
     * 宏语义展开接入点。
     *
     * `null` 表示 LSP 不注册 environment：行为与未接入宏展开一致（不构造、不发布快照）。
     * 产品壳需要展开宏时，注入 SDK / 引擎 / 编译器配置装配（见 [org.cangnova.cangjie.lsp.analysis.LspMacroExpansionOptions]）。
     */
    val macroExpansion: LspMacroExpansionOptions? = null,

    /**
     * 构造 LSP 运行环境的工厂。
     *
     * 测试和嵌入式宿主可以通过该工厂替换默认环境创建逻辑。
     * 默认实现会把 [environmentMode] 与 [macroExpansion] 一起传入。
     */
    val environmentFactory: () -> CangjieLspEnvironment = {
        CangjieLspEnvironment.create(environmentMode, macroExpansion)
    },

    /**
     * 根据生命周期上下文构造分析外观的工厂。
     *
     * 默认实现接入 Analysis API，测试可替换为协议契约或录制型 facade。
     */
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
