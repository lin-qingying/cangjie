package org.cangnova.cangjie.codeinsight.hints

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.TextRange
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.psiUtil.collectDescendantsOfType

/**
 * 注解实参名提示收集器。
 *
 * `@Ann(1, 2)` 里每个实参对应哪个形参只能靠数位置。这里在位置实参前补上形参名
 * （`@Ann(x: 1, y: 2)` 的提示形态），与调用点参数名提示保持同一套文本。
 *
 * 注解在 PSI 里是宏表达式，实参被 token 化，所以候选阶段要重建表达式
 * （见 [CjAnnotationArgumentHints]）；本类只负责「解析形参名 → 产出提示」。
 *
 * 定位不到注解声明（跨包未加载、宏而非注解类、实参数多于形参数）时整条跳过——
 * 不做部分推测，避免给出错的形参名。
 */
internal object CjAnnotationArgumentHintsCollector {
    /**
     * 收集 `file` 内（可限定到 `range`）的注解实参名提示。
     */
    fun collect(
        file: CjFile,
        range: TextRange?,
        session: CaSession,
    ): List<CangJieInlayHint> {
        val candidates = collectAnnotationCandidates(file, range)
        return candidates.flatMap { candidate -> hintsFor(file, candidate, session) }
    }

    /**
     * 产出单个注解调用对应的提示；任何失败都只丢这一条。
     */
    private fun hintsFor(
        file: CjFile,
        candidate: CjAnnotationArgumentHintCandidate,
        session: CaSession,
    ): List<CangJieInlayHint> = try {
        resolveHints(file, candidate, session)
    } catch (cancelled: ProcessCanceledException) {
        // 取消必须原样上抛：它不是「这条提示算不出来」，而是整个请求不要了。
        throw cancelled
    } catch (failure: Throwable) {
        LOG.warn("仓颉 inlay hint（注解实参名）解析失败", failure)
        emptyList()
    }

    /**
     * 解析注解声明的主构造函数形参，按位置配对后产出提示。
     */
    private fun resolveHints(
        file: CjFile,
        candidate: CjAnnotationArgumentHintCandidate,
        session: CaSession,
    ): List<CangJieInlayHint> {
        // 不要在这里套 `isInsideMacroExpansion`：注解本身在 PSI 里就是宏表达式，
        // 它的区间会被算进宏展开，套上去会让整类提示永远为空。这里的锚点是用户在注解
        // 里亲手写的实参，不存在「指向展开产物」的问题。

        // 只在能定位到注解声明（class / struct 等类型声明）时才提示；
        // 真正的宏（`macro foo(...)`）解析结果不是类型声明，自然被排除。
        //
        // 解析必须走 Analysis API：PSI 层的 `reference` 在悬空会话里是 null
        // （实测），而 `resolveSymbol()` 在同一个会话内能正常拿到符号。
        val declaration = resolveAnnotationDeclaration(candidate, session) ?: return emptyList()
        val parameters = declaration.primaryConstructor?.valueParameters ?: return emptyList()
        // 实参比形参多说明这不是凭位置对应的调用，宁可整条不提示。
        if (candidate.arguments.size > parameters.size) return emptyList()

        return candidate.arguments.mapIndexedNotNull { index, argument ->
            val name = parameters[index].name ?: return@mapIndexedNotNull null
            if (name.isEmpty()) return@mapIndexedNotNull null
            // `@Ann(x: x)` 这类实参与形参同名，提示不携带额外信息。
            if (name == argument.text) return@mapIndexedNotNull null
            CangJieInlayHint(
                offset = argument.offset,
                kind = CangJieInlayHintKind.PARAMETER_NAME,
                target = CangJieInlayHintTarget.ANNOTATION_ARGUMENT_NAME,
                label = listOf(CangJieInlayHintLabelPart("$name: ")),
            )
        }
    }

    /**
     * 定位注解对应的类型声明。
     *
     * 先走 Analysis API 的 `resolveSymbol()`；但**宏表达式的 callee 在 Analysis API 里解析
     * 不出来**（实测返回 null，注解本身就是宏表达式），所以保留一条同文件按名查找的回退。
     * 限制：注解类声明在其它文件/包时没有提示——宁可少提示，也不猜形参名。
     */
    private fun resolveAnnotationDeclaration(
        candidate: CjAnnotationArgumentHintCandidate,
        session: CaSession,
    ): CjTypeStatement? {
        val reference = candidate.macro.referenceExpression ?: return null
        val bySymbol = with(session) { reference.resolveSymbol() }?.psi as? CjTypeStatement
        if (bySymbol != null) return bySymbol

        val name = candidate.macro.shortName?.asString() ?: reference.referencedName
        if (name.isEmpty()) return null
        return candidate.macro.containingFile
            .collectDescendantsOfType<CjTypeStatement>()
            .firstOrNull { declaration -> declaration.name == name }
    }

    private val LOG: Logger = Logger.getInstance(CjAnnotationArgumentHintsCollector::class.java)
}
