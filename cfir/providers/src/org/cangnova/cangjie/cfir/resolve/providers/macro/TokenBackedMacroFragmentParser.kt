package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.declarations.CfirFile

/**
 * `MacroFragmentParser` 的 token-backed 实现。
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
 *
 * @property reparse raw builder 注入的 fragment reparse 回调。
 * @property reTokenize lexer/token-stage 复扫回调。
 */
class TokenBackedMacroFragmentParser(
    /**
     * 接收 fragment 拼接后的源码文本与解析模式，返回 raw builder 重新解析得到的
     * 完整结果；parser 语法错误不再被压成 null 或通用宏语义错误。
     */
    private val reparse: (text: String, input: MacroFragmentInput) -> MacroFragmentReparseResult,
    /**
     * 对 macro executor 产出的 newTokens 做 lexer/token-stage 复扫。
     *
     * providers 模块不能直接依赖 PSI lexer；装配方必须注入 raw-cfir-common
     * 的 tokenizer，测试可注入等价实现。
     */
    private val reTokenize: (List<MacroSurfaceToken>) -> List<MacroSurfaceToken>,
) : MacroFragmentParser {

    /**
     * 解析单个 macro fragment 输入。
     *
     * 本实现先根据 [MacroFragmentInput.mode] 选择 token 来源，再执行 token-stage
     * re-evaluation，最后调用注入的 [reparse] 生成 construction-only payload。
     */
    override fun parse(input: MacroFragmentInput): MacroFragmentResult {
        val node = input.node
        val mode = input.mode
        val tokens = if (mode == MacroFragmentParser.Mode.CUSTOM_ANNOTATION) {
            input.annotationSnapshot?.tokens ?: return MacroFragmentResult.Failure(
                originNode = node,
                reason = "Custom-annotation fragment requires a full annotation slot snapshot.",
            )
        } else {
            input.tokens
        }
        // baseline Batch 8: 先按 token-stage 重组到稳定点（newTokens token-stage re-eval）
        val reEvaluatedTokens = MacroTokenReEvaluator.reTokenizeUntilStable(tokens, reTokenize)
        val hasSyntaxOutput = reEvaluatedTokens.any { !it.isTrivia }
        val parseTokens = if (mode == MacroFragmentParser.Mode.DECLARATION && hasSyntaxOutput) {
            input.inheritedAnnotationTokens + reEvaluatedTokens
        } else {
            reEvaluatedTokens
        }
        val untrimmedSourceMap = MacroFragmentSourceMap.materialize(parseTokens, node.surface.surfaceId)
        val trimmedPrefixLength = untrimmedSourceMap.text.indexOfFirst { !it.isWhitespace() }
            .takeIf { it >= 0 } ?: untrimmedSourceMap.text.length
        val sourceMap = untrimmedSourceMap.trimmed()
        val source = sourceMap.text
        if (mode == MacroFragmentParser.Mode.CUSTOM_ANNOTATION &&
            source != input.annotationSnapshot!!.rawSyntax.trim()
        ) {
            return MacroFragmentResult.Failure(
                originNode = node,
                reason = "Custom annotation token text differs from its retained original syntax.",
            )
        }
        val reparseInput = input.copy(tokens = reEvaluatedTokens, sourceMap = sourceMap)
        if (source.isEmpty() || mode == MacroFragmentParser.Mode.DECLARATION && !hasSyntaxOutput) {
            if (mode == MacroFragmentParser.Mode.DECLARATION) {
                return MacroFragmentResult.Success(
                    originNode = node,
                    tokens = reEvaluatedTokens,
                    mode = mode,
                    payload = MacroFragmentPayload.EMPTY,
                    sourceMapping = reparseInput.sourceMapping,
                )
            }
            return MacroFragmentResult.Failure(
                originNode = node,
                reason = "Macro fragment text is empty after token re-evaluation",
            )
        }

        val diagnosticSourceMap = MacroParserDiagnosticSourceMap.fromTokens(
            tokens = parseTokens,
            defaultSourceRange = input.node.surface.sourceRange,
            trimmedPrefixLength = trimmedPrefixLength,
            textLength = source.length,
        )

        // 用户语法/形状失败使用类型化结果；内部不变量与取消必须传播，不能转成可降级结果。
        val reparsed = reparse(source, reparseInput)
        when (reparsed) {
            is MacroFragmentReparseResult.SyntaxFailure -> return MacroFragmentResult.SyntaxFailure(
                node,
                reparsed.parsedText,
                reparsed.diagnosticPublication,
                reparsed.parsedFileFacts,
                diagnosticSourceMap,
            )
            is MacroFragmentReparseResult.Failure -> return MacroFragmentResult.Failure(node, reparsed.reason, reparsed.parsedFileFacts)
            else -> Unit
        }

        return when (mode) {
            MacroFragmentParser.Mode.CUSTOM_ANNOTATION -> {
                val annotationPayload = (reparsed as? MacroFragmentReparseResult.AnnotationSuccess)?.annotation
                    ?: return MacroFragmentResult.Failure(
                        originNode = node,
                        reason = "Custom-annotation fragment did not produce an annotation call.",
                        parsedFileFacts = reparsed.parsedFileFacts,
                    )
                val original = input.annotationSnapshot?.originalAnnotation
                if (original != null && annotationPayload === original) {
                    return MacroFragmentResult.Failure(
                        originNode = node,
                        reason = "Custom-annotation fragment reused the original annotation payload.",
                        parsedFileFacts = reparsed.parsedFileFacts,
                    )
                }
                MacroFragmentResult.CustomAnnotation(
                    originNode = node,
                    payload = annotationPayload,
                    tokens = reEvaluatedTokens,
                    parsedFileFacts = reparsed.parsedFileFacts,
                    sourceMap = diagnosticSourceMap,
                    sourceMapping = reparseInput.sourceMapping,
                    diagnosticPublication = reparsed.diagnosticPublication,
                )
            }
            else -> {
                val macroPayload = (reparsed as? MacroFragmentReparseResult.Success)?.payload
                    ?: return MacroFragmentResult.Failure(
                        originNode = node,
                        reason = "Macro fragment did not produce direct parser roots.",
                        parsedFileFacts = reparsed.parsedFileFacts,
                    )
                MacroFragmentResult.Success(
                    originNode = node,
                    tokens = reEvaluatedTokens,
                    mode = mode,
                    payload = macroPayload,
                    parsedFileFacts = reparsed.parsedFileFacts,
                    sourceMap = diagnosticSourceMap,
                    sourceMapping = reparseInput.sourceMapping,
                    diagnosticPublication = reparsed.diagnosticPublication,
                )
            }
        }
    }

    /** 测试哨兵属性；生产逻辑不读取该值。 */
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
    /** 占位 splice：返回原文件列表，不宣称完成节点级替换。 */
    override fun applySlices(files: List<CfirFile>, slots: List<MacroReplaceSlot>): MacroStableSpliceResult =
        MacroStableSpliceResult(files, emptyList())
}
