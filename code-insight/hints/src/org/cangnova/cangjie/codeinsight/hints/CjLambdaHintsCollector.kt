package org.cangnova.cangjie.codeinsight.hints

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForSource
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjFunctionLiteral
import org.cangnova.cangjie.psi.CjReturnExpression
import org.cangnova.cangjie.psi.psiUtil.collectDescendantsOfType

/**
 * lambda 提示收集器。
 *
 * 目前只做「最后一个表达式的类型」：仓颉 lambda 的返回值是函数体末表达式的值，
 * 但源码里既不写 `return` 也不写类型，读起来最不直观。提示插在末表达式之后，
 * 形如 `{ i: Int => i + 20 }: Int`（实际渲染为 `i + 20: Int`）。
 *
 * 未做「隐式接收者 / 隐式形参」提示（`str.run { this: String }`）：`CaFunctionType`
 * 不暴露接收者类型，取不到要显示的文本，详见 design.md 的 D48。
 *
 * 分两层：候选挑选是纯 PSI 逻辑，类型解析与渲染必须在 `analyze` 块内完成。
 */
internal object CjLambdaHintsCollector {
    /**
     * 类型文本渲染策略：源码视角必须用短名。
     */
    private val TYPE_RENDERER = CaTypeRendererForSource.WITH_SHORT_NAMES

    /**
     * 收集 `file` 内（可限定到 `range`）的 lambda 返回类型提示。
     */
    fun collect(
        file: CjFile,
        range: TextRange?,
        session: CaSession,
    ): List<CangJieInlayHint> =
        candidates(file, range).mapNotNull { candidate -> hintFor(file, candidate, session) }

    /**
     * 挑选所有「末表达式即返回值」的 lambda。
     *
     * 纯 PSI 逻辑：只判断结构、显式返回类型与 range，不做类型解析。
     */
    fun candidates(
        file: CjFile,
        range: TextRange?,
    ): List<CjLambdaReturnHintCandidate> =
        file.collectDescendantsOfType<CjFunctionLiteral>().mapNotNull { literal ->
            candidate(literal, range)
        }

    /**
     * 单个 lambda 的候选判定。
     */
    private fun candidate(
        literal: CjFunctionLiteral,
        range: TextRange?,
    ): CjLambdaReturnHintCandidate? {
        // 写了显式返回类型就没有提示的必要。
        if (literal.typeReference != null) return null
        if (CjInlayHintFilters.hasErrorAncestor(literal)) return null

        val last = literal.bodyExpression?.lastStatement ?: return null
        // 显式 `return` 已经写明返回，末语句是 `return` 时不再补类型。
        if (last is CjReturnExpression) return null

        val offset = last.textRange.endOffset
        if (!CjInlayHintFilters.isInRange(offset, range)) return null
        return CjLambdaReturnHintCandidate(literal, last, offset)
    }

    /**
     * 把单个候选解析成提示；任何失败都只丢这一条。
     */
    private fun hintFor(
        file: CjFile,
        candidate: CjLambdaReturnHintCandidate,
        session: CaSession,
    ): CangJieInlayHint? = try {
        resolveHint(file, candidate, session)
    } catch (cancelled: ProcessCanceledException) {
        // 取消必须原样上抛：它不是「这条提示算不出来」，而是整个请求不要了。
        throw cancelled
    } catch (failure: Throwable) {
        LOG.warn("仓颉 inlay hint（lambda 返回类型）解析失败", failure)
        null
    }

    /**
     * 在 session 内解析末表达式类型并渲染成纯文本。
     */
    private fun resolveHint(
        file: CjFile,
        candidate: CjLambdaReturnHintCandidate,
        session: CaSession,
    ): CangJieInlayHint? {
        if (CjInlayHintFilters.isInsideMacroExpansion(file, candidate.literal.textRange.startOffset, session)) {
            return null
        }

        val type = with(session) { candidate.lastStatement.expressionType } ?: return null
        if (with(session) { type.isErrorType }) return null

        val rendered = with(session) { type.render(TYPE_RENDERER) }.trim()
        if (rendered.isEmpty()) return null
        // 末表达式是 `Unit` 的 lambda 到处都有（`forEach { println(it) }`），提示出来只是噪声。
        if (rendered == UNIT_TYPE_NAME) return null

        return CangJieInlayHint(
            offset = candidate.offset,
            kind = CangJieInlayHintKind.TYPE,
            target = CangJieInlayHintTarget.LAMBDA_RETURN_TYPE,
            label = listOf(CangJieInlayHintLabelPart(": $rendered")),
        )
    }

    /**
     * 单元类型名：与函数返回类型提示保持同一套噪声过滤。
     */
    private const val UNIT_TYPE_NAME = "Unit"

    private val LOG: Logger = Logger.getInstance(CjLambdaHintsCollector::class.java)
}

/**
 * lambda 返回类型提示的候选：承载 lambda、末表达式与插入位置。
 *
 * 只携带 PSI 锚点，不含任何 session 生命周期对象。
 */
internal data class CjLambdaReturnHintCandidate(
    /** 承载提示的 lambda 字面量。 */
    val literal: CjFunctionLiteral,
    /** lambda 函数体的末表达式，其类型即返回值类型。 */
    val lastStatement: CjExpression,
    /** 提示插入位置（末表达式之后）。 */
    val offset: Int,
)
