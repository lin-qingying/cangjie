package org.cangnova.cangjie.analysis.api.cfir.components

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaMacroExpansionMapping
import org.cangnova.cangjie.analysis.api.components.CaMacroExpansionMappingProvider
import org.cangnova.cangjie.analysis.api.components.CaMacroExpansionReliability
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent
import org.cangnova.cangjie.analysis.api.impl.base.components.withPsiValidityAssertion
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfir
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroCallForestBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroCallNode
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroPayloadChannel
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceExpr
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceLookupResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.incomingChannel
import org.cangnova.cangjie.cfir.resolve.providers.macro.lookupSurfaceAt
import org.cangnova.cangjie.cfir.session.rawMacroSurfaceUsageStoreOrNull
import org.cangnova.cangjie.psi.CjFile

/**
 * 宏展开源映射的 CFIR 后端实现。
 *
 * 实现刻意只读既有结构：raw builder 收集的 expression macro surface 已经按文件身份
 * 记在 [org.cangnova.cangjie.cfir.resolve.providers.macro.CfirRawMacroSurfaceUsageStore] 里，
 * 这里把它按区间组织成 [org.cangnova.cangjie.cfir.resolve.providers.macro.MacroCallForest]
 * 并做一次区间定位——不新建缓存、不重新解析源码，也不把 surface 暴露给调用方。
 *
 * 可靠性**只**取载体通道的既有状态（父边通道），不做启发式判断：
 * - 子展开面：通道为 `UNRESOLVED` ⇒ 不可靠，`ATTR`/`INPUT` ⇒ 可靠；
 * - root 展开面：没有父边，也就不存在通道歧义 ⇒ 可靠。
 */
@OptIn(CaExperimentalApi::class, CaImplementationDetail::class)
internal class CaCfirMacroExpansionMappingProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaMacroExpansionMappingProvider {

    /**
     * 按元素起始偏移映射。
     *
     * 用起始偏移而不是整个区间：调用方给的多半是一个词法元素，
     * 它的任意位置与展开面的关系由区间定位统一裁决。
     */
    override val PsiElement.macroExpansionMapping: CaMacroExpansionMapping
        get() = withPsiValidityAssertion {
            val file = containingFile as? CjFile ?: return@withPsiValidityAssertion CaMacroExpansionMapping.None
            val offset = textRange?.startOffset ?: return@withPsiValidityAssertion CaMacroExpansionMapping.None
            mappingAt(file, offset)
        }

    override fun macroExpansionMappingAt(file: CjFile, offset: Int): CaMacroExpansionMapping =
        withPsiValidityAssertion { mappingAt(file, offset) }

    /** 定位并转换为公开结论。 */
    private fun mappingAt(file: CjFile, offset: Int): CaMacroExpansionMapping {
        val surfaces = surfacesFor(file)
        if (surfaces.isEmpty()) return CaMacroExpansionMapping.None
        val forest = MacroCallForestBuilder.build(surfaces)
        return when (val result = forest.lookupSurfaceAt(offset)) {
            MacroSurfaceLookupResult.NoSurface -> CaMacroExpansionMapping.None
            // 歧义不是「无展开」：消费方对它必须保守。
            is MacroSurfaceLookupResult.Ambiguous -> CaMacroExpansionMapping.Ambiguous
            is MacroSurfaceLookupResult.Innermost -> {
                val range = result.node.surface.sourceRange ?: return CaMacroExpansionMapping.None
                CaMacroExpansionMapping.Inside(
                    surfaceId = result.node.surface.surfaceId,
                    startOffset = range.startOffset,
                    endOffset = range.endOffset,
                    reliability = reliabilityOf(result.node),
                )
            }
        }
    }

    /**
     * 取该文件的 expression macro surface。
     *
     * 与 `CfirImportsChecker` 同源：surface 由 raw builder 按「CFIR 文件对象」与
     * 「原始 PSI 文件身份」两把键记录，且记录发生在**该文件所属模块的会话**上，
     * 因此两把键 × 可达的两个会话都要查——少查任何一处都会把「有宏」读成「无展开」。
     */
    private fun surfacesFor(file: CjFile): List<MacroSurfaceExpr> {
        val cfirFile = cfirFileOf(file) ?: return emptyList()
        val sourceFileIdentity = cfirFile.psi ?: file
        val sessions = listOfNotNull(analysisSession.cfirSession, cfirFile.moduleData.session).distinct()
        return buildList {
            for (session in sessions) {
                val store = session.rawMacroSurfaceUsageStoreOrNull ?: continue
                addAll(store.expressionSurfaces(cfirFile))
                addAll(store.expressionSurfacesForSourceFile(sourceFileIdentity))
            }
        }.distinctBy { it.surfaceId }
    }

    /** 取得该 PSI 文件对应的 CFIR 文件；未构建时按需构建（surface 只在构建后存在）。 */
    private fun cfirFileOf(file: CjFile): CfirFile? =
        with(analysisSession) { file.getOrBuildCfir(resolutionFacade) as? CfirFile }

    /** 可靠性取父边通道。 */
    private fun reliabilityOf(node: MacroCallNode): CaMacroExpansionReliability =
        when (node.incomingChannel()) {
            null,
            MacroPayloadChannel.ATTR,
            MacroPayloadChannel.INPUT,
            -> CaMacroExpansionReliability.RELIABLE

            MacroPayloadChannel.UNRESOLVED -> CaMacroExpansionReliability.UNRELIABLE
        }
}
