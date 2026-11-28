package org.cangnova.cangjie.codeinsight.hints

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.resolution.successfulFunctionCallOrNull
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.psiUtil.collectDescendantsOfType

/**
 * 参数名提示收集器。
 *
 * 与类型提示同构分两层：候选挑选只判断结构（哪些位置写的是位置实参、是否命名实参、
 * 是否在请求范围内）；形参名必须从 `resolveToCall()` 的实参映射里取，因而在 `analyze` 块内完成。
 *
 * 生命周期约束：形参名在这里就渲染成 `String`，`CaCall`/`CaVariableSignature` 不出块。
 */
internal object CjParameterNameHintsCollector {
    /**
     * 收集 `file` 内（可限定到 `range`）的参数名提示。
     */
    fun collect(
        file: CjFile,
        range: TextRange?,
        session: CaSession,
    ): List<CangJieInlayHint> =
        candidates(file, range).flatMap { candidate -> hintsFor(file, candidate, session) }

    /**
     * 挑选所有「写了位置实参」的调用。
     *
     * 纯 PSI 逻辑：命名实参（`name: value`）已经在源码里写明了形参名，不重复提示；
     * 括号外尾随的 lambda 不在 `valueArgumentList` 里，天然不参与。
     */
    fun candidates(
        file: CjFile,
        range: TextRange?,
    ): List<CjParameterNameHintCandidate> =
        file.collectDescendantsOfType<CjCallExpression>().mapNotNull { call -> candidate(call, range) }

    /**
     * 单个调用的候选判定：至少要有一个落在 range 内的位置实参。
     */
    private fun candidate(
        call: CjCallExpression,
        range: TextRange?,
    ): CjParameterNameHintCandidate? {
        val arguments = call.valueArgumentList?.arguments ?: return null
        if (arguments.isEmpty()) return null
        if (CjInlayHintFilters.hasErrorAncestor(call)) return null

        val positions = arguments.mapNotNull { argument ->
            if (argument.isNamed()) return@mapNotNull null
            val expression = argument.getArgumentExpression() ?: return@mapNotNull null
            // `inout x` 这类修饰不该被提示文本切断，锚点取真正的实参表达式。
            val offset = expression.textRange.startOffset
            if (!CjInlayHintFilters.isInRange(offset, range)) return@mapNotNull null
            CjParameterNameHintPosition(offset, expression)
        }
        if (positions.isEmpty()) return null
        return CjParameterNameHintCandidate(call, positions)
    }

    /**
     * 解析单个调用的实参绑定并产出提示；任何失败都只丢这个调用的提示。
     */
    private fun hintsFor(
        file: CjFile,
        candidate: CjParameterNameHintCandidate,
        session: CaSession,
    ): List<CangJieInlayHint> = try {
        resolveHints(file, candidate, session)
    } catch (cancelled: ProcessCanceledException) {
        throw cancelled
    } catch (failure: Throwable) {
        LOG.warn("仓颉参数名提示解析失败：${candidate.description}", failure)
        emptyList()
    }

    /**
     * 在 session 内取出「实参表达式 → 形参名」，出块后只保留纯文本。
     */
    private fun resolveHints(
        file: CjFile,
        candidate: CjParameterNameHintCandidate,
        session: CaSession,
    ): List<CangJieInlayHint> {
        if (CjInlayHintFilters.isInsideMacroExpansion(file, candidate.call.textRange.startOffset, session)) {
            return emptyList()
        }

        val parameterNames = with(session) {
            val callInfo = candidate.call.resolveToCall() ?: return@with null
            val functionCall = callInfo.successfulFunctionCallOrNull() ?: return@with null
            functionCall.valueArgumentMapping.entries.associate { (expression, signature) ->
                expression to signature.symbol.name.asString()
            }
        } ?: return emptyList()

        return candidate.positions.mapNotNull { position ->
            val name = parameterNames[position.expression] ?: return@mapNotNull null
            if (name.isEmpty() || isSuppressed(position.expression, name)) return@mapNotNull null
            CangJieInlayHint(
                offset = position.offset,
                kind = CangJieInlayHintKind.PARAMETER_NAME,
                target = CangJieInlayHintTarget.PARAMETER_NAME,
                label = listOf(CangJieInlayHintLabelPart("$name: ")),
            )
        }
    }

    /**
     * 提示文本与实参同名的抑制，也是 exclude-list 的扩展落点。
     *
     * `f(count: count)` 这类实参与形参同名的调用，提示不携带任何额外信息。
     * 后续若需要按调用目标或形参名配置排除项（IDE 设置），在这里加规则即可，
     * 调用点不需要改。
     */
    private fun isSuppressed(
        expression: CjExpression,
        name: String,
    ): Boolean = expression.text == name

    private val LOG: Logger = Logger.getInstance(CjParameterNameHintsCollector::class.java)
}

/**
 * 参数名提示候选：一个调用 + 它落在请求范围内的位置实参。
 *
 * 只携带 PSI，不含任何 session 生命周期对象。
 */
internal data class CjParameterNameHintCandidate(
    /** 承载实参的调用表达式。 */
    val call: CjCallExpression,
    /** 待提示的实参位置。 */
    val positions: List<CjParameterNameHintPosition>,
) {
    /** 供日志区分的候选描述。 */
    val description: String
        get() = "CjCallExpression@${call.textRange.startOffset}"
}

/**
 * 单个实参的提示位置。
 */
internal data class CjParameterNameHintPosition(
    /** 提示插入位置，取实参表达式起点。 */
    val offset: Int,
    /** 该位置的实参表达式，用于在实参映射里定位形参。 */
    val expression: CjExpression,
)
