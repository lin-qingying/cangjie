package org.cangnova.cangjie.analysis.macro.expansion

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
 * 记录调用次数的测试引擎。
 *
 * 默认在 [expand] 返回 [MacroConstructionResult.ExecutorUnavailable]，用例通过
 * [expandResultProvider] 注入可发布 / 失败 / 取消等结果。
 */
class FakeMacroSemanticExpansionEngine : MacroSemanticExpansionEngine {
    /** `prepareArtifacts` 调用次数。 */
    var prepareArtifactsCount: Int = 0
        private set

    /** `buildSymbolIndex` 调用次数。 */
    var buildSymbolIndexCount: Int = 0
        private set

    /** `bindImports` 调用次数。 */
    var bindImportsCount: Int = 0
        private set

    /** `expand` 调用次数。 */
    var expandCount: Int = 0
        private set

    /** artifact 准备结果。 */
    var preparation: MacroExpansionArtifactPreparation = MacroExpansionArtifactPreparation()

    /** 展开结果注入点。 */
    var expandResultProvider: (
        PreMacroRawBuildResult,
        MacroResolutionContext,
        MacroDemandClassification,
        List<MacroConstructionDiagnostic>,
    ) -> MacroConstructionResult = { _, _, _, _ ->
        MacroConstructionResult.ExecutorUnavailable(org.cangnova.cangjie.cfir.resolve.providers.macro.MacroExpansionRegistry.EMPTY)
    }

    override fun prepareArtifacts(
        configuration: CompilerConfiguration,
        classifications: List<MacroDemandClassification>,
    ): MacroExpansionArtifactPreparation {
        prepareArtifactsCount++
        return preparation
    }

    override fun buildSymbolIndex(
        pre: PreMacroRawBuildResult,
        artifacts: MacroExpansionArtifactPreparation,
    ): MacroSymbolIndex {
        buildSymbolIndexCount++
        return buildMacroSymbolIndex(pre, macroArtifactDefinitions = artifacts.definitions)
    }

    override fun bindImports(
        pre: PreMacroRawBuildResult,
        index: MacroSymbolIndex,
    ): MacroResolutionContext {
        bindImportsCount++
        return bindMacroImports(pre, index)
    }

    override fun expand(
        pre: PreMacroRawBuildResult,
        context: MacroResolutionContext,
        classification: MacroDemandClassification,
        configuration: CompilerConfiguration,
        diagnostics: List<MacroConstructionDiagnostic>,
        mode: MacroConstructionService.Mode,
    ): MacroConstructionResult {
        expandCount++
        return expandResultProvider(pre, context, classification, diagnostics)
    }
}
