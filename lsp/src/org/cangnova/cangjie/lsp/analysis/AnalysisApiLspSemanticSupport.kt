package org.cangnova.cangjie.lsp.analysis

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
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
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.Position

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
    private val psiDocumentFactory: AnalysisApiPsiDocumentFactory,
) {
    internal data class WorkspaceFileContext(
        val psiFile: CjFile,
        val documentUri: String,
        val analysisDocument: LspTextDocument,
        val openedDocument: LspTextDocument?,
    ) {
        val versionOrNull: Int?
            get() = openedDocument?.version
    }

    private val documentStore = lifecycleContext.documentStore
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

    fun findReferenceExpression(
        document: LspTextDocument,
        file: CjFile,
        position: Position,
    ): CjReferenceExpression? =
        findSemanticLeaf(document, file, position)
            ?.let { leaf -> leaf.parentsWithSelf().filterIsInstance<CjReferenceExpression>().firstOrNull() }

    fun findSimpleNameExpression(
        document: LspTextDocument,
        file: CjFile,
        position: Position,
    ): CjSimpleNameExpression? =
        findSemanticLeaf(document, file, position)
            ?.let { leaf -> leaf.parentsWithSelf().filterIsInstance<CjSimpleNameExpression>().firstOrNull() }

    fun findCallExpression(
        document: LspTextDocument,
        file: CjFile,
        position: Position,
    ): CjCallExpression? =
        findSemanticLeaf(document, file, position)
            ?.let { leaf -> leaf.parentsWithSelf().filterIsInstance<CjCallExpression>().firstOrNull() }

    fun findExpression(
        document: LspTextDocument,
        file: CjFile,
        position: Position,
    ): CjExpression? =
        findSemanticLeaf(document, file, position)
            ?.let { leaf -> leaf.parentsWithSelf().filterIsInstance<CjExpression>().firstOrNull() }

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

    fun openedDocument(file: CjFile): LspTextDocument? =
        documentUriOf(file)?.let(documentStore::get)

    fun documentUriOf(file: CjFile): String? =
        projectStructureState.documentUriOf(file) ?: file.virtualFile?.url

    /**
     * 枚举当前工作区所有可见源码文件。
     *
     * 这里直接以项目结构快照为事实来源，不再由 LSP 层单独遍历 workspaceFolders。
     * 这样打开文档快照、工作区磁盘文件和模块可见性边界都与 Analysis API 平台状态保持一致。
     */
    fun workspaceFiles(@Suppress("UNUSED_PARAMETER") context: CangjieAnalysisRequestContext): List<WorkspaceFileContext> {
        return projectStructureState.snapshot.allSourceFiles
            .asSequence()
            .filterIsInstance<CjFile>()
            .mapNotNull(::workspaceFileContext)
            .distinctBy(WorkspaceFileContext::documentUri)
            .toList()
    }

    fun declarationChildren(container: CjDeclarationContainer): List<CjDeclaration> = container.declarations

    /**
     * 将公开符号或源码声明规约成可跨文档比较的稳定语义键。
     */
    fun CaSession.targetKeyFor(reference: CjReferenceExpression): AnalysisApiLspTargetKey? {
        val resolvedSymbol = reference.resolveToSymbol()
        if (resolvedSymbol != null) {
            return resolvedSymbol.toTargetKey(this)
        }

        val resolvedPsi = reference.references.asSequence().mapNotNull { it.resolve() }.firstOrNull()
        return targetKeyFor(resolvedPsi)
    }

    fun targetKeyFor(declaration: PsiElement?): AnalysisApiLspTargetKey? {
        val namedDeclaration = declaration as? CjNamedDeclaration ?: return null
        val containingFile = namedDeclaration.containingFile as? CjFile ?: return null
        val range = namedDeclaration.nameIdentifier?.textRange ?: namedDeclaration.textRange ?: return null
        val documentUri = documentUriOf(containingFile) ?: return null

        return when (namedDeclaration) {
            is CjClassLikeDeclaration -> {
                namedDeclaration.getClassId()?.let(AnalysisApiLspTargetKey::ClassLike)
                    ?: AnalysisApiLspTargetKey.Local(documentUri, range.startOffset, range.endOffset, namedDeclaration.name)
            }

            is CjCallableDeclaration -> {
                namedDeclaration.fqName?.let { fqName ->
                    val callableName = fqName.shortName()
                    val packageName = fqName.parent()
                    AnalysisApiLspTargetKey.Callable(CallableId(packageName, null, callableName))
                } ?: AnalysisApiLspTargetKey.Local(documentUri, range.startOffset, range.endOffset, namedDeclaration.name)
            }

            else -> AnalysisApiLspTargetKey.Local(documentUri, range.startOffset, range.endOffset, namedDeclaration.name)
        }
    }

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

    private fun org.cangnova.cangjie.analysis.api.symbols.CaSymbol.toTargetKey(session: CaSession): AnalysisApiLspTargetKey? =
        when (this) {
            is org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol -> AnalysisApiLspTargetKey.Package(fqName)
            is org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol -> AnalysisApiLspTargetKey.ClassLike(classId)
            is org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol -> {
                val stableCallableId = callableId
                if (stableCallableId != null) {
                    AnalysisApiLspTargetKey.Callable(stableCallableId)
                } else {
                    val originalPsi = session.run { getOriginalPsi() } ?: return null
                    val containingFile = session.run { getContainingFile() } ?: return null
                    val uri = documentUriOf(containingFile) ?: return null
                    val range = originalPsi.textRange ?: return null
                    AnalysisApiLspTargetKey.Local(uri, range.startOffset, range.endOffset, name)
                }
            }

            is org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol -> AnalysisApiLspTargetKey.File(packageFqName, file.name)
            else -> null
        }

    private fun PsiElement.parentsWithSelf(): Sequence<PsiElement> = generateSequence(this) { current ->
        current.parent as? PsiElement
    }
}

/**
 * LSP 层跨文档比较引用目标时使用的稳定语义键。
 */
internal sealed interface AnalysisApiLspTargetKey {
    data class Package(val fqName: FqName) : AnalysisApiLspTargetKey

    data class File(val packageFqName: FqName, val fileName: String) : AnalysisApiLspTargetKey

    data class ClassLike(val classId: ClassId) : AnalysisApiLspTargetKey

    data class Callable(val callableId: CallableId) : AnalysisApiLspTargetKey

    data class Local(
        val documentUri: String,
        val startOffset: Int,
        val endOffset: Int,
        val name: String?,
    ) : AnalysisApiLspTargetKey
}
