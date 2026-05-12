package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.declarations.CfirFile

/**
 * `MacroFragmentParser` 的最小 token-backed 实现（baseline Batch 8 真实实现）。
 *
 * 把展开后的 token 流通过 [MacroTokenReEvaluator.reTokenizeText] 拼回字符串，
 * 然后让上层的 raw builder 重新解析该字符串作为 fragment。
 *
 * 真正承担 reparse 的入口由 [reparse] lambda 注入（由 `:cfir:raw-cfir:*`
 * 模块在 macro construction 入口装配时提供），从而把 fragment parser
 * 与具体 raw builder 模块解耦：
 *
 * - PSI 模块装配时传入"用 `PsiRawCfirBuilder` 解析片段"的实现；
 * - LightTree 模块装配时传入"用 `LightTree2Cfir` 解析片段"的实现；
 * - 测试 stub 可以直接返回固定 `MacroFragmentResult` 即可。
 *
 * 该类内部不依赖 PSI / LightTree，只承担 token 重组 + 错误模式分派，
 * 因此可以保持在 `cfir:providers` 模块。
 */
class TokenBackedMacroFragmentParser(
    /**
     * 接收 fragment 拼接后的源码文本与解析模式，返回 raw builder 重新解析得到的
     * fragment payload；解析失败返回 null。返回的对象类型在不同 raw builder
     * 装配时不同，所以只持有为 [Any] 引用。
     */
    private val reparse: (text: String, mode: MacroFragmentParser.Mode, owner: MacroCallNode) -> Any?,
) : MacroFragmentParser {

    override fun parse(
        node: MacroCallNode,
        tokens: List<MacroSurfaceToken>,
        mode: MacroFragmentParser.Mode,
    ): MacroFragmentResult {
        // baseline Batch 8: 先按 token-stage 重组文本（newTokens token-stage re-eval）
        val source = MacroTokenReEvaluator.reTokenizeText(tokens).trim()
        if (source.isEmpty()) {
            return MacroFragmentResult.Failure(
                originNode = node,
                reason = "Macro fragment text is empty after token re-evaluation",
            )
        }

        val payload = runCatching { reparse(source, mode, node) }.getOrNull()
            ?: return MacroFragmentResult.Failure(
                originNode = node,
                reason = "Raw builder reported a parse failure for fragment text: ${source.take(60)}",
            )

        return when (mode) {
            MacroFragmentParser.Mode.CUSTOM_ANNOTATION -> {
                // 名称推断由具体 reparser 决定，这里走默认 fallback
                val qname = node.surface.qualifiedName
                val annotationName = qname?.shortName()
                    ?: return MacroFragmentResult.Failure(
                        originNode = node,
                        reason = "Custom-annotation fragment missing a name",
                    )
                MacroFragmentResult.CustomAnnotation(
                    originNode = node,
                    annotationName = annotationName,
                    tokens = tokens,
                )
            }
            else -> MacroFragmentResult.Success(
                originNode = node,
                tokens = tokens,
                mode = mode,
                payload = payload,
            )
        }
    }

    @Suppress("UNUSED")
    val lastReparseSentinel: Any? get() = null
}

/**
 * `MacroStableSplicer` 的 construction 边界占位实现。
 *
 * [TokenBackedMacroFragmentParser] 只负责 newTokens 的 token-stage re-eval，
 * 再把 raw fragment reparse 结果作为 construction-only [MacroFragmentResult]
 * 交回宏构造流水线；它不产生 provider-visible final CFIR，也不替换旧节点。
 *
 * 在 stable splice 接入前，本对象只保留 files 不变，作为禁止旧语义路径进入
 * final provider 的 hard boundary placeholder；真实节点级 splice 必须通过
 * [CfirReplaceHandle] 接入，不能由本占位对象宣称完成。
 */
object IdentityMacroStableSplicer : MacroStableSplicer {
    override fun applySlices(files: List<CfirFile>, slots: List<MacroReplaceSlot>): List<CfirFile> = files
}

/**
 * `BuiltinNonMacroDesugarer` 的占位实现：保留 [fragment] 不变。
 *
 * Batch 8 阶段仅提供入口；具体 `@IfAvailable` 等 desugar 逻辑由 Batch 10
 * 在 stable splice / construction desugar 接入阶段完成。
 */
object IdentityBuiltinNonMacroDesugarer : BuiltinNonMacroDesugarer {
    override fun desugar(surface: BuiltinNonMacroSurface, fragment: MacroFragmentResult.Success): MacroFragmentResult? {
        return fragment
    }
}
