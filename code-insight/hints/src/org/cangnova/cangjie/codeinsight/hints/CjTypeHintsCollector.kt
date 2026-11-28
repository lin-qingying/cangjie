package org.cangnova.cangjie.codeinsight.hints

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForSource
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjDeclarationWithBody
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjVariableDeclaration
import org.cangnova.cangjie.psi.psiUtil.collectDescendantsOfType

/**
 * 类型提示收集器。
 *
 * 分两层：候选挑选是纯 PSI 逻辑（可在无 `CaSession` 的解析测试里验证），
 * 类型解析与渲染必须发生在 `analyze` 块内（`session` 由调用方提供）。
 *
 * 生命周期约束：本类型不缓存 session，也不把 `CaType` 带出渲染步骤。
 */
internal object CjTypeHintsCollector {
    /**
     * 类型文本渲染策略。
     *
     * `CaRenderer.render()` 的默认是全限定名，源码视角的提示必须用短名，
     * 否则 `let x = 1` 会提示 `: std.core.Int64`。
     */
    private val TYPE_RENDERER = CaTypeRendererForSource.WITH_SHORT_NAMES

    /**
     * 收集 `file` 内（可限定到 `range`）的类型提示。
     *
     * 单个候选的解析失败只丢该条提示，不影响其余候选。
     */
    fun collect(
        file: CjFile,
        range: TextRange?,
        session: CaSession,
        options: CangJieInlayHintOptions,
    ): List<CangJieInlayHint> =
        candidates(file, range, options).mapNotNull { candidate -> hintFor(file, candidate, session) }

    /**
     * 挑选所有「源码没写类型」的候选。
     *
     * 纯 PSI 逻辑：只判断结构、开关与 range，不做任何类型解析。
     */
    fun candidates(
        file: CjFile,
        range: TextRange?,
        options: CangJieInlayHintOptions,
    ): List<CjTypeHintCandidate> {
        val candidates = ArrayList<CjTypeHintCandidate>()
        if (options.showPropertyTypes || options.showLocalVariableTypes) {
            file.collectDescendantsOfType<CjVariableDeclaration>().mapNotNullTo(candidates) { declaration ->
                variableCandidate(declaration, range, options)
            }
        }
        if (options.showFunctionReturnTypes) {
            file.collectDescendantsOfType<CjNamedFunction>().mapNotNullTo(candidates) { function ->
                functionCandidate(function, range)
            }
        }
        return candidates
    }


    /**
     * 变量类候选：只有「无显式类型 + 有初始化器 + 有可锚定的名字」才值得提示。
     */
    private fun variableCandidate(
        declaration: CjVariableDeclaration,
        range: TextRange?,
        options: CangJieInlayHintOptions,
    ): CjTypeHintCandidate? {
        // 用 `colon` 而不是 `typeReference` 判定显式类型：`CjProperty.typeReference` 在
        // stub 缺类型节点时会 `LOG.error`，而 `colon` 是纯结构判定，无副作用。
        if (declaration.colon != null) return null
        if (declaration.initializer == null) return null
        if (!isVariableTypeEnabled(declaration, options)) return null
        if (CjInlayHintFilters.hasErrorAncestor(declaration)) return null
        if (declaration.initializer?.let(CjInlayHintFilters::containsErrorElement) == true) return null

        val anchor = declaration.declarationNameElement() ?: return null
        val offset = anchor.textRange.endOffset
        if (!CjInlayHintFilters.isInRange(offset, range)) return null
        return CjTypeHintCandidate.Variable(offset, declaration, variableTarget(declaration))
    }

    /**
     * 函数返回类型候选：只有「无显式返回类型 + 有函数体」才值得提示。
     */
    private fun functionCandidate(
        function: CjNamedFunction,
        range: TextRange?,
    ): CjTypeHintCandidate? {
        if (function.colon != null) return null
        val body = function as? CjDeclarationWithBody ?: return null
        if (!body.hasBody()) return null
        // 函数只查祖先链：函数体内部的错误会由类型推导自己失败，逐函数扫描函数体
        // 会把「收集一屏提示」的成本抬到大文件不可接受的程度。
        if (CjInlayHintFilters.hasErrorAncestor(function)) return null

        // 返回类型写在参数列表之后，因此锚点优先取 `)`；无参数列表的形态退回函数名。
        val anchor = function.valueParameterList?.rightParenthesis ?: function.nameIdentifier ?: return null
        val offset = anchor.textRange.endOffset
        if (!CjInlayHintFilters.isInRange(offset, range)) return null
        return CjTypeHintCandidate.Function(offset, function)
    }

    /**
     * 把单个候选解析成提示；任何失败都只丢这一条。
     */
    private fun hintFor(
        file: CjFile,
        candidate: CjTypeHintCandidate,
        session: CaSession,
    ): CangJieInlayHint? = try {
        resolveHint(file, candidate, session)
    } catch (cancelled: ProcessCanceledException) {
        // 取消必须原样上抛：它不是「这条提示算不出来」，而是整个请求不要了。
        throw cancelled
    } catch (failure: Throwable) {
        LOG.warn("仓颉 inlay hint 解析失败：${candidate.description}", failure)
        null
    }

    /**
     * 在 session 内解析类型并渲染成纯文本。
     */
    private fun resolveHint(
        file: CjFile,
        candidate: CjTypeHintCandidate,
        session: CaSession,
    ): CangJieInlayHint? {
        if (CjInlayHintFilters.isInsideMacroExpansion(file, candidate.declaration.textRange.startOffset, session)) return null

        val type = with(session) {
            when (candidate) {
                is CjTypeHintCandidate.Variable -> candidate.declaration.initializer?.expressionType
                is CjTypeHintCandidate.Function -> candidate.declaration.returnType
            }
        } ?: return null

        val isErrorType = with(session) { type.isErrorType }
        if (isErrorType) return null

        val rendered = with(session) { type.render(TYPE_RENDERER) }.trim()
        if (rendered.isEmpty()) return null
        // 函数体的 `: Unit` 是省略返回类型的常态，提示出来只是噪声。
        if (candidate is CjTypeHintCandidate.Function && rendered == UNIT_TYPE_NAME) return null

        return CangJieInlayHint(
            offset = candidate.offset,
            kind = CangJieInlayHintKind.TYPE,
            target = candidate.target,
            label = listOf(CangJieInlayHintLabelPart(": $rendered")),
        )
    }

    /**
     * 按声明宿主判断该种类是否被开关打开。
     */
    private fun isVariableTypeEnabled(
        declaration: CjVariableDeclaration,
        options: CangJieInlayHintOptions,
    ): Boolean = when {
        // 局部 `let`/`var` 只有局部开关控制；顶层变量、`prop`、成员字段都算属性类。
        declaration is CjPatternVariable && declaration.isLocal -> options.showLocalVariableTypes
        else -> options.showPropertyTypes
    }

    /**
     * 变量声明的语义种类，供宿主映射到「变量 / 局部变量 / 属性」三个开关。
     */
    private fun variableTarget(declaration: CjVariableDeclaration): CangJieInlayHintTarget = when {
        declaration !is CjPatternVariable -> CangJieInlayHintTarget.PROPERTY_TYPE
        declaration.isLocal -> CangJieInlayHintTarget.LOCAL_VARIABLE_TYPE
        else -> CangJieInlayHintTarget.VARIABLE_TYPE
    }

    /**
     * 取声明名节点。
     *
     * 模式变量没有单一名字节点，只有在简单绑定（非解构）时才能锚定。
     */
    private fun CjVariableDeclaration.declarationNameElement(): PsiElement? =
        if (this is CjPatternVariable) {
            (pattern as? CjBindingPattern)?.nameIdentifier
        } else {
            nameIdentifier
        }

    /**
     * 单元类型名：函数省略返回类型时按 `Unit` 推导，这类提示不产出。
     */
    private const val UNIT_TYPE_NAME = "Unit"

    private val LOG: Logger = Logger.getInstance(CjTypeHintsCollector::class.java)
}

/**
 * 类型提示候选。
 *
 * 只携带 PSI 锚点与 offset，不含任何 session 生命周期对象。
 */
internal sealed class CjTypeHintCandidate {
    /** 提示插入位置。 */
    abstract val offset: Int

    /** 候选来源声明，用于宏展开判定与日志。 */
    abstract val declaration: PsiElement

    /** 该候选属于哪一类提示，宿主据此做逐项开关。 */
    abstract val target: CangJieInlayHintTarget

    /** 供日志区分的候选描述。 */
    open val description: String
        get() = "${declaration::class.java.simpleName}@${declaration.textRange.startOffset}"

    /**
     * 变量类候选：类型来自初始化器表达式。
     */
    data class Variable(
        override val offset: Int,
        override val declaration: CjVariableDeclaration,
        override val target: CangJieInlayHintTarget,
    ) : CjTypeHintCandidate()

    /**
     * 函数返回类型候选。
     */
    data class Function(
        override val offset: Int,
        override val declaration: CjNamedFunction,
    ) : CjTypeHintCandidate() {
        override val target: CangJieInlayHintTarget
            get() = CangJieInlayHintTarget.FUNCTION_RETURN_TYPE
    }
}
