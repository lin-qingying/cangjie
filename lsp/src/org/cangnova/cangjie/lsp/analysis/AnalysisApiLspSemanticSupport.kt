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
    private val psiDocumentFactory: AnalysisApiPsiDocumentFactory,
) {
    internal data class PositionTargets(
        val declarationTargets: List<PsiElement>,
        val referenceTargets: List<PsiElement>,
    ) {
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

    fun findTargetElements(
        document: LspTextDocument,
        file: CjFile,
        position: Position,
        targetKinds: Set<AnalysisApiLspTargetKind> = AnalysisApiLspTargetKind.ALL,
    ): List<PsiElement> {
        return findTargets(document, file, position).preferredTargets(targetKinds)
    }

    fun findPrimaryTarget(
        document: LspTextDocument,
        file: CjFile,
        position: Position,
        targetKinds: Set<AnalysisApiLspTargetKind> = AnalysisApiLspTargetKind.ALL,
    ): PsiElement? {
        return findTargetElements(document, file, position, targetKinds).firstOrNull()
    }

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
    @OptIn(CaPlatformInterface::class)
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
        val resolvedSymbol = runCatching { reference.resolveToSymbol() }.getOrNull()
        if (resolvedSymbol != null) {
            return resolvedSymbol.toTargetKey(this)
        }

        val resolvedPsi = runCatching {
            reference.references.asSequence().mapNotNull { it.resolve() }.firstOrNull()
        }.getOrNull()
        return targetKeyFor(resolvedPsi)
    }

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

    fun referenceLikeElements(file: CjFile): Sequence<PsiElement> {
        return sequence {
            yieldAll(file.collectDescendantsOfType<CjSimpleNameExpression>().asSequence())
            yieldAll(file.collectDescendantsOfType<CjBasicType>().asSequence())
        }
    }

    private fun analysisOffset(
        document: LspTextDocument,
        file: CjFile,
        position: Position,
    ): Int {
        return document.analysisOffsetAt(position).coerceIn(0, file.textLength.coerceAtLeast(0))
    }

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

internal enum class AnalysisApiLspTargetKind {
    DECLARATION,
    REFERENCE,
    ;

    companion object {
        val ALL: Set<AnalysisApiLspTargetKind> = setOf(DECLARATION, REFERENCE)
    }
}
