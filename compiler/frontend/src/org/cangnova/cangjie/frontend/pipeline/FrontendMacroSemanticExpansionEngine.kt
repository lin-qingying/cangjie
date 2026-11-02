package org.cangnova.cangjie.frontend.pipeline

import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionDiagnostic
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroConstructionService
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroDemandClassification
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroResolutionContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSymbolIndex
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroRawBuildResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.bindMacroImports
import org.cangnova.cangjie.cfir.resolve.providers.macro.buildMacroSymbolIndex
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.macro.expansion.MacroExpansionArtifactPreparation
import org.cangnova.cangjie.macro.expansion.MacroSemanticExpansionEngine

/**
 * `compiler:frontend` 提供的宏语义展开引擎实现。
 *
 * 对现有构造入口的 1:1 包装：artifact 准备、符号索引、import 绑定与
 * [FrontendMacroConstructionService] 展开算法本身都不变。分层上看，
 * 本类只实现 `macro:macro-common` 的引擎 SPI，**不引入** 对 `analysis:*` 的依赖。
 */
class FrontendMacroSemanticExpansionEngine : MacroSemanticExpansionEngine {
    /** 委托现有 artifact 准备入口，只投影引擎 SPI 需要的定义与诊断。 */
    override fun prepareArtifacts(
        configuration: CompilerConfiguration,
        classifications: List<MacroDemandClassification>,
    ): MacroExpansionArtifactPreparation {
        val preparation = prepareMacroArtifactDefinitionsForExpansion(
            configuration = configuration,
            classifications = classifications,
        )
        return MacroExpansionArtifactPreparation(
            definitions = preparation.definitions,
            diagnostics = preparation.diagnostics,
        )
    }

    /** 委托现有宏符号索引构建入口。 */
    override fun buildSymbolIndex(
        pre: PreMacroRawBuildResult,
        artifacts: MacroExpansionArtifactPreparation,
    ): MacroSymbolIndex = buildMacroSymbolIndex(
        pre = pre,
        macroArtifactDefinitions = artifacts.definitions,
    )

    /** 委托现有宏 import 绑定入口。 */
    override fun bindImports(
        pre: PreMacroRawBuildResult,
        index: MacroSymbolIndex,
    ): MacroResolutionContext = bindMacroImports(pre, index)

    /** 委托现有宏构造服务；[configuration] 由宿主 environment 提供。 */
    override fun expand(
        pre: PreMacroRawBuildResult,
        context: MacroResolutionContext,
        classification: MacroDemandClassification,
        configuration: CompilerConfiguration,
        diagnostics: List<MacroConstructionDiagnostic>,
        mode: MacroConstructionService.Mode,
    ): MacroConstructionResult = FrontendMacroConstructionService(configuration).expand(
        pre = pre,
        context = context,
        classification = classification,
        mode = mode,
        preConstructionDiagnostics = diagnostics,
    )
}
