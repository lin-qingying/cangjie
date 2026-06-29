package org.cangnova.cangjie.lsp.analysis

import com.intellij.model.psi.PsiSymbolService
import com.intellij.model.psi.impl.targetDeclarationAndReferenceSymbols
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjBasicType
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjClassLikeDeclaration
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjDeclarationContainer
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.cangnova.cangjie.psi.CjReferenceExpression
import org.cangnova.cangjie.psi.CjSimpleNameExpression
import org.cangnova.cangjie.psi.psiUtil.collectDescendantsOfType
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.Range

/**
 * Analysis API 驱动的 LSP 语义支撑层。
 *
 * 这一层统一负责：
 * 1. `LSP 文档 -> PSI snapshot -> Analysis API session` 的接入；
 * 2. 光标位置与 PSI 叶子节点之间的稳定映射；
 * 3. 工作区文件、打开文档和磁盘源码之间的统一枚举；
 * 4. 语义目标键与 LSP Location 的统一转换。
 */
internal class AnalysisApiLspSemanticSupport(
    lifecycleContext: CangjieAnalysisLifecycleContext,
    /**
     * LSP 文档到 Analysis API PSI 快照的工厂。
     */
    private val psiDocumentFactory: AnalysisApiPsiDocumentFactory,
) {
    /**
     * 指定位置抽取出的声明目标和引用目标。
     */
    internal data class PositionTargets(
        /**
         * 当前位置命中的声明目标 PSI。
         */
        val declarationTargets: List<PsiElement>,

        /**
         * 当前位置命中的引用目标 PSI。
         */
        val referenceTargets: List<PsiElement>,
    ) {
        /**
         * 根据调用方偏好的目标类型选择优先目标列表。
         *
         * 引用目标优先于声明目标，符合跳转/引用请求对 use-site 的常见期望。
         */
        fun preferredTargets(targetKinds: Set<AnalysisApiLspTargetKind>): List<PsiElement> {
            if (AnalysisApiLspTargetKind.REFERENCE in targetKinds && referenceTargets.isNotEmpty()) {
                return referenceTargets
            }
            if (AnalysisApiLspTargetKind.DECLARATION in targetKinds && declarationTargets.isNotEmpty()) {
                return declarationTargets
            }
            return emptyList()
        }
    }

    /**
     * 工作区源码文件在 LSP 与 Analysis API 之间的上下文描述。
     */
    internal data class WorkspaceFileContext(
        /**
         * 工作区可见的仓颉 PSI 文件。
         */
        val psiFile: CjFile,

        /**
         * 该文件对应的 LSP 文档 URI。
         */
        val documentUri: String,

        /**
         * 用于 offset/position 转换的分析文档快照。
         */
        val analysisDocument: LspTextDocument,

        /**
         * 如果文件当前已打开，则为打开文档快照。
         */
        val openedDocument: LspTextDocument?,
    ) {
        /**
         * 打开文档的版本号；未打开文件返回 null。
         */
        val versionOrNull: Int?
            get() = openedDocument?.version
    }

    /**
     * 当前 LSP 文档存储。
     */
    private val documentStore = lifecycleContext.documentStore

    /**
     * 当前 project 的 LSP 项目结构状态。
     */
    private val projectStructureState = AnalysisApiLspProjectStructureState.getInstance(lifecycleContext.environment.project)

    /**
     * 基于当前文档文本创建可分析快照，并在其 use-site 模块上执行 Analysis API 操作。
     */
    inline fun <R> analyzeSnapshot(
        document: LspTextDocument,
        crossinline action: CaSession.(AnalysisApiPsiSnapshot) -> R,
    ): R {
        val snapshot = psiDocumentFactory.createAnalyzableSnapshot(document)
        return analyze(snapshot.useSiteModule) { action(snapshot) }
    }

    /**
     * 以给定 PSI 文件自身作为 use-site 入口执行 Analysis API。
     */
    inline fun <R> analyzeFile(
        file: CjFile,
        action: CaSession.() -> R,
    ): R = analyze(file, action)

    /**
     * 统一通过 IntelliJ target extraction 获取当前位置的 declaration/reference targets。
     *
     * 这里直接对齐 Kotlin 当前使用的平台能力，不再由 LSP 自己顺着父链猜测命中对象。
     */
    fun findTargets(
        document: LspTextDocument,
        file: CjFile,
        position: Position,
    ): PositionTargets {
        val offset = analysisOffset(document, file, position)
        val symbolService = PsiSymbolService.getInstance()
        val (declared, referenced) = targetDeclarationAndReferenceSymbols(file, offset)
        return PositionTargets(
            declarationTargets = declared
                .mapNotNull(symbolService::extractElementFromSymbol)
                .distinctBy(::targetIdentity),
            referenceTargets = referenced
                .mapNotNull(symbolService::extractElementFromSymbol)
                .distinctBy(::targetIdentity),
        )
    }

    /**
     * 查找当前位置符合目标类型要求的 PSI 元素列表。
     */
    fun findTargetElements(
        document: LspTextDocument,
        file: CjFile,
        position: Position,
        targetKinds: Set<AnalysisApiLspTargetKind> = AnalysisApiLspTargetKind.ALL,
    ): List<PsiElement> {
        return findTargets(document, file, position).preferredTargets(targetKinds)
    }

    /**
     * 查找当前位置最优先的语义目标元素。
     */
    fun findPrimaryTarget(
        document: LspTextDocument,
        file: CjFile,
        position: Position,
        targetKinds: Set<AnalysisApiLspTargetKind> = AnalysisApiLspTargetKind.ALL,
    ): PsiElement? {
        return findTargetElements(document, file, position, targetKinds).firstOrNull()
    }

    /**
     * 计算当前位置可用于 hover 展示的文本范围。
     */
    fun hoverRange(
        document: LspTextDocument,
        file: CjFile,
        position: Position,
    ): Range? {
        val offset = analysisOffset(document, file, position)
        file.findReferenceAt(offset)?.element?.textRange?.let { range ->
            return document.analysisRangeOf(range.startOffset, range.endOffset)
        }
        file.findElementAt(offset)?.textRange?.let { range ->
            return document.analysisRangeOf(range.startOffset, range.endOffset)
        }
        return null
    }

    /**
     * 把 LSP 光标位置稳定映射到最适合语义分析的 PSI 叶子节点。
     */
    fun findSemanticLeaf(
        document: LspTextDocument,
        file: CjFile,
        position: Position,
    ): PsiElement? {
        val requestedOffset = document.analysisOffsetAt(position)
        val normalizedOffset = requestedOffset.coerceIn(0, file.textLength.coerceAtLeast(0))
        val direct = file.findElementAt(normalizedOffset)
        if (direct != null && !direct.textRange.isEmpty) {
            return direct
        }

        val backward = (normalizedOffset - 1).takeIf { it >= 0 }?.let(file::findElementAt)
        if (backward != null && !backward.textRange.isEmpty) {
            return backward
        }

        val forward = (normalizedOffset + 1).takeIf { it < file.textLength }?.let(file::findElementAt)
        return if (forward != null && !forward.textRange.isEmpty) forward else null
    }

    /**
     * 查找当前位置所在的引用表达式。
     */
    fun findReferenceExpression(
        document: LspTextDocument,
        file: CjFile,
        position: Position,
    ): CjReferenceExpression? =
        findSemanticLeaf(document, file, position)
            ?.let { leaf -> leaf.parentsWithSelf().filterIsInstance<CjReferenceExpression>().firstOrNull() }

    /**
     * 查找当前位置所在的简单名表达式。
     */
    fun findSimpleNameExpression(
        document: LspTextDocument,
        file: CjFile,
        position: Position,
    ): CjSimpleNameExpression? =
        findSemanticLeaf(document, file, position)
            ?.let { leaf -> leaf.parentsWithSelf().filterIsInstance<CjSimpleNameExpression>().firstOrNull() }

    /**
     * 查找当前位置所在的调用表达式。
     */
    fun findCallExpression(
        document: LspTextDocument,
        file: CjFile,
        position: Position,
    ): CjCallExpression? =
        findSemanticLeaf(document, file, position)
            ?.let { leaf -> leaf.parentsWithSelf().filterIsInstance<CjCallExpression>().firstOrNull() }

    /**
     * 查找当前位置所在的表达式。
     */
    fun findExpression(
        document: LspTextDocument,
        file: CjFile,
        position: Position,
    ): CjExpression? =
        findSemanticLeaf(document, file, position)
            ?.let { leaf -> leaf.parentsWithSelf().filterIsInstance<CjExpression>().firstOrNull() }

    /**
     * 查找当前位置所在的具名声明。
     */
    fun findNamedDeclaration(
        document: LspTextDocument,
        file: CjFile,
        position: Position,
    ): CjNamedDeclaration? =
        findSemanticLeaf(document, file, position)
            ?.let { leaf -> leaf.parentsWithSelf().filterIsInstance<CjNamedDeclaration>().firstOrNull() }

    /**
     * 将 PSI 元素映射为 LSP Location。
     *
     * 打开文档优先使用内存文本做 offset -> position 转换；
     * 否则使用磁盘 PSI 文本本身，保证工作区未打开文件也能稳定定位。
     */
    fun toLocation(element: PsiElement): Location? {
        val containingFile = element.containingFile as? CjFile ?: return null
        val range = element.textRange ?: return null
        val documentContext = workspaceFileContext(containingFile) ?: return null

        val start = documentContext.analysisDocument.analysisPositionAt(range.startOffset)
        val end = documentContext.analysisDocument.analysisPositionAt(range.endOffset)
        return Location(
            documentContext.documentUri,
            org.eclipse.lsp4j.Range(start, end),
        )
    }

    /**
     * 查询指定 PSI 文件当前是否存在打开文档快照。
     */
    fun openedDocument(file: CjFile): LspTextDocument? =
        documentUriOf(file)?.let(documentStore::get)

    /**
     * 将 PSI 文件映射回 LSP 文档 URI。
     */
    fun documentUriOf(file: CjFile): String? =
        projectStructureState.documentUriOf(file) ?: file.virtualFile?.url

    /**
     * 枚举当前工作区所有可见源码文件。
     *
     * 这里直接以项目结构快照为事实来源，不再由 LSP 层单独遍历 workspaceFolders。
     * 这样打开文档快照、工作区磁盘文件和模块可见性边界都与 Analysis API 平台状态保持一致。
     */
    @OptIn(CaPlatformInterface::class)
    fun workspaceFiles(@Suppress("UNUSED_PARAMETER") context: CangjieAnalysisRequestContext): List<WorkspaceFileContext> {
        return projectStructureState.snapshot.allSourceFiles
            .asSequence()
            .filterIsInstance<CjFile>()
            .mapNotNull(::workspaceFileContext)
            .distinctBy(WorkspaceFileContext::documentUri)
            .toList()
    }

    /**
     * 返回容器声明的直接子声明。
     */
    fun declarationChildren(container: CjDeclarationContainer): List<CjDeclaration> = container.declarations

    /**
     * 将公开符号或源码声明规约成可跨文档比较的稳定语义键。
     */
    fun CaSession.targetKeyFor(reference: CjReferenceExpression): AnalysisApiLspTargetKey? {
        val resolvedSymbol = runCatching { reference.resolveToSymbol() }.getOrNull()
        if (resolvedSymbol != null) {
            return resolvedSymbol.toTargetKey(this)
        }

        val resolvedPsi = runCatching {
            reference.references.asSequence().mapNotNull { it.resolve() }.firstOrNull()
        }.getOrNull()
        return targetKeyFor(resolvedPsi)
    }

    /**
     * 将引用类 PSI 元素规约成可比较的目标键。
     */
    fun CaSession.targetKeyForReferenceLike(element: PsiElement): AnalysisApiLspTargetKey? {
        return when (element) {
            is CjReferenceExpression -> targetKeyFor(element)
            is CjBasicType -> element.references
                .asSequence()
                .mapNotNull { reference -> reference.resolve() }
                .mapNotNull(::targetKeyFor)
                .firstOrNull()

            else -> null
        }
    }

    /**
     * 将声明 PSI 元素规约成可跨文档比较的目标键。
     */
    fun targetKeyFor(declaration: PsiElement?): AnalysisApiLspTargetKey? {
        return when (declaration) {
            is CjNamedDeclaration -> {
                val containingFile = declaration.containingFile as? CjFile ?: return null
                val range = declaration.nameIdentifier?.textRange ?: declaration.textRange ?: return null
                val documentUri = documentUriOf(containingFile) ?: return null

                when (declaration) {
                    is CjClassLikeDeclaration -> {
                        declaration.getClassId()?.let(AnalysisApiLspTargetKey::ClassLike)
                            ?: AnalysisApiLspTargetKey.Local(documentUri, range.startOffset, range.endOffset, declaration.name)
                    }

                    is CjCallableDeclaration -> {
                        declaration.fqName?.let { fqName ->
                            val callableName = fqName.shortName()
                            val packageName = fqName.parent()
                            AnalysisApiLspTargetKey.Callable(CallableId(packageName, null, callableName))
                        } ?: AnalysisApiLspTargetKey.Local(documentUri, range.startOffset, range.endOffset, declaration.name)
                    }

                    else -> AnalysisApiLspTargetKey.Local(documentUri, range.startOffset, range.endOffset, declaration.name)
                }
            }

            is CjFile -> AnalysisApiLspTargetKey.File(declaration.packageFqName, declaration.name)
            else -> null
        }
    }

    /**
     * 枚举文件中可作为引用目标的 PSI 元素。
     */
    fun referenceLikeElements(file: CjFile): Sequence<PsiElement> {
        return sequence {
            yieldAll(file.collectDescendantsOfType<CjSimpleNameExpression>().asSequence())
            yieldAll(file.collectDescendantsOfType<CjBasicType>().asSequence())
        }
    }

    /**
     * 将 LSP position 转换为当前 analysis 文本内的合法 offset。
     */
    private fun analysisOffset(
        document: LspTextDocument,
        file: CjFile,
        position: Position,
    ): Int {
        return document.analysisOffsetAt(position).coerceIn(0, file.textLength.coerceAtLeast(0))
    }

    /**
     * 构造 PSI 目标去重所需的稳定身份字符串。
     */
    private fun targetIdentity(element: PsiElement): String {
        val file = (element.containingFile as? CjFile)?.virtualFile?.url ?: "<memory>"
        val range = element.textRange
        return buildString {
            append(file)
            append(':')
            append(range?.startOffset ?: -1)
            append(':')
            append(range?.endOffset ?: -1)
            append(':')
            append(element::class.java.name)
        }
    }

    /**
     * 为工作区 PSI 文件构造 LSP/Analysis 文档上下文。
     */
    private fun workspaceFileContext(file: CjFile): WorkspaceFileContext? {
        val documentUri = documentUriOf(file) ?: return null
        val openedDocument = documentStore.get(documentUri)
        val analysisDocument = openedDocument ?: LspTextDocument(
            uri = documentUri,
            languageId = null,
            version = 0,
            text = file.text,
        )
        return WorkspaceFileContext(
            psiFile = file,
            documentUri = documentUri,
            analysisDocument = analysisDocument,
            openedDocument = openedDocument,
        )
    }

    /**
     * 对没有稳定全局标识的本地/匿名 symbol，统一退回到“文件 + 文本区间”的局部键，
     * 让 LSP 的跨文档比对仍然建立在 Analysis API 当前公开能力之上。
     */
    private fun org.cangnova.cangjie.analysis.api.symbols.CaSymbol.localTargetKey(
        session: CaSession,
        stableName: String? = null,
    ): AnalysisApiLspTargetKey.Local? {
        val originalPsi = psi ?: return null
        val containingFile = originalPsi.containingFile as? CjFile ?: return null
        val uri = documentUriOf(containingFile) ?: return null
        val range = originalPsi.textRange ?: return null
        return AnalysisApiLspTargetKey.Local(uri, range.startOffset, range.endOffset, stableName)
    }

    /**
     * 将 Analysis API 公开符号规约为 LSP 语义目标键。
     */
    private fun org.cangnova.cangjie.analysis.api.symbols.CaSymbol.toTargetKey(session: CaSession): AnalysisApiLspTargetKey? =
        when (this) {
            is org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol -> AnalysisApiLspTargetKey.Package(fqName)
            is org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol -> {
                classId?.let(AnalysisApiLspTargetKey::ClassLike)
                    ?: localTargetKey(session, (this as? CaNamedSymbol)?.name?.asString())
            }

            is org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol -> {
                val stableCallableId = callableId
                if (stableCallableId != null) {
                    AnalysisApiLspTargetKey.Callable(stableCallableId)
                } else {
                    localTargetKey(session, (this as? CaNamedSymbol)?.name?.asString())
                }
            }

            is org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol -> AnalysisApiLspTargetKey.File(packageFqName, file.name)
            else -> null
        }

    /**
     * 从当前 PSI 元素向父节点方向遍历自身和祖先。
     */
    private fun PsiElement.parentsWithSelf(): Sequence<PsiElement> = generateSequence(this) { current ->
        current.parent as? PsiElement
    }
}

/**
 * LSP 层跨文档比较引用目标时使用的稳定语义键。
 */
internal sealed interface AnalysisApiLspTargetKey {
    /**
     * package 级目标键。
     */
    data class Package(val fqName: FqName) : AnalysisApiLspTargetKey

    /**
     * 文件级目标键。
     */
    data class File(val packageFqName: FqName, val fileName: String) : AnalysisApiLspTargetKey

    /**
     * 类、接口、结构体、枚举等 class-like 目标键。
     */
    data class ClassLike(val classId: ClassId) : AnalysisApiLspTargetKey

    /**
     * 可调用声明目标键。
     */
    data class Callable(val callableId: CallableId) : AnalysisApiLspTargetKey

    /**
     * 缺少全局标识的本地目标键。
     */
    data class Local(
        /**
         * 目标所在文档 URI。
         */
        val documentUri: String,

        /**
         * 目标起始 offset。
         */
        val startOffset: Int,

        /**
         * 目标结束 offset。
         */
        val endOffset: Int,

        /**
         * 可选展示名称。
         */
        val name: String?,
    ) : AnalysisApiLspTargetKey
}

/**
 * LSP 位置目标选择时允许的目标类别。
 */
internal enum class AnalysisApiLspTargetKind {
    /** 声明目标。 */
    DECLARATION,
    /** 引用目标。 */
    REFERENCE,
    ;

    companion object {
        /**
         * 同时允许声明和引用目标的默认集合。
         */
        val ALL: Set<AnalysisApiLspTargetKind> = setOf(DECLARATION, REFERENCE)
    }
}
