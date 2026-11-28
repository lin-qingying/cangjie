package org.cangnova.cangjie.lsp.analysis

import com.intellij.model.psi.PsiSymbolService
import com.intellij.openapi.application.ReadAction
import com.intellij.model.psi.impl.targetDeclarationAndReferenceSymbols
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.resolution.CaSymbolResolutionSuccess
import org.cangnova.cangjie.analysis.api.restoreSymbol
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPatternVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.symbols.symbol
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.PsiElementResolveResult
import org.cangnova.cangjie.psi.cangJieCanonicalTarget
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.psi.CjBasicType
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjDeclarationContainer
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CangJieReferenceProvidersService
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
        return ReadAction.compute<R, RuntimeException> {
            val snapshot = psiDocumentFactory.createAnalyzableSnapshot(document)
            analyze(snapshot.useSiteModule) { action(snapshot) }
        }
    }

    /**
     * 在平台读锁中以文件自身进入分析，宏写屏障必须等当前分析完整结束。
     */
    inline fun <R> analyzeFile(
        file: CjFile,
        crossinline action: CaSession.() -> R,
    ): R = ReadAction.compute<R, RuntimeException> { analyze(file) { action() } }

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
                .distinct(),
            referenceTargets = referenced
                .mapNotNull(symbolService::extractElementFromSymbol)
                .distinct(),
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
    fun workspaceFiles(@Suppress("UNUSED_PARAMETER") context: CangjieAnalysisRequestContext): List<WorkspaceFileContext> =
        ReadAction.compute<List<WorkspaceFileContext>, RuntimeException> {
            projectStructureState.snapshot.allSourceFiles
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
    fun CaSession.targetKeyFor(reference: CjReferenceExpression): AnalysisApiLspTargetKey? =
        reference.resolveToSymbol()?.toTargetKey()

    /**
     * 优先用当前位置的引用表达式解析目标键。
     *
     * 宏生成声明没有物理 PSI，平台 target 提取会把它还原成宏来源 PSI；
     * 直接解析引用符号可以保留生成声明的 `CallableId`。
     */
    fun referenceTargetKey(
        session: CaSession,
        document: LspTextDocument,
        file: CjFile,
        position: Position,
    ): AnalysisApiLspTargetKey? = session.run {
        findReferenceExpression(document, file, position)?.let { reference ->
            if (namedDeclarationTarget(reference) == null) targetKeyFor(reference) else null
        }
    }

    /**
     * 将引用类 PSI 元素规约成可比较的目标键。
     */
    fun CaSession.targetKeyForReferenceLike(element: PsiElement): AnalysisApiLspTargetKey? {
        if (namedDeclarationTarget(element) != null) return null
        return when (element) {
            is CjReferenceExpression -> targetKeyFor(element)
            is CjBasicType -> element.references
                .asSequence()
                .mapNotNull { reference -> reference.resolve() }
                .mapNotNull { target -> targetKeyFor(target) }
                .firstOrNull()

            else -> null
        }
    }

    /**
     * 将声明 PSI 元素规约成可跨文档比较的目标键。
     */
    fun CaSession.targetKeyFor(declaration: PsiElement?): AnalysisApiLspTargetKey? {
        if (declaration is CjFile) return declaration.symbol.toTargetKey()
        val canonical = declaration?.let(::namedDeclarationTarget) as? CjElement ?: return null
        val success = canonical.tryResolveSymbols() as? CaSymbolResolutionSuccess ?: return null
        return success.symbols.singleOrNull()?.toTargetKey()
    }

    /**
     * 从当前有效解析结果发布声明位置；共同宏调用来源只承担导航，不参与符号身份比较。
     * compiled PSI 在 use-site session 内解析，避免把库声明误当作可分析的源码模块。
     */
    fun CaSession.referenceNavigationTargets(
        element: PsiElement,
        targets: Collection<AnalysisApiLspTargetKey>,
    ): List<PsiElement> = element.references.asSequence()
        .flatMap { reference ->
            when (reference) {
                is PsiPolyVariantReference -> reference.multiResolve(false).asSequence()
                else -> reference.resolve()?.let { sequenceOf(PsiElementResolveResult(it)) } ?: emptySequence()
            }
        }
        .filter { it.isValidResult }
        .filter { result ->
            val canonical = result.cangJieCanonicalTarget as? CjElement
            val success = canonical?.tryResolveSymbols() as? CaSymbolResolutionSuccess
            val canonicalKey = success?.symbols?.singleOrNull()?.toTargetKey()
            targets.any { it.matches(this, canonicalKey) }
        }
        .mapNotNull { it.element }
        .distinct()
        .toList()

    /** references 的声明位置只收窄到具名声明的名字；definition 保留正式导航 target 的完整范围。 */
    fun CaSession.referenceDeclarationTargets(
        element: PsiElement,
        targets: Collection<AnalysisApiLspTargetKey>,
    ): List<PsiElement> = referenceNavigationTargets(element, targets)
        .map { (it as? PsiNameIdentifierOwner)?.nameIdentifier ?: it }
        .distinct()

    /** 声明和 references 消费同一决议，类型、initializer 和 constructor 使用均不是变量声明名。 */
    private fun namedDeclarationTarget(element: PsiElement): PsiNameIdentifierOwner? =
        CangJieReferenceProvidersService.getInstance(element.project).getNamedDeclarationTarget(element)

    /** 枚举规范具名声明，包含模式叶子及宿主绑定，排除没有真实 binder 的模式。 */
    fun namedDeclarationElements(file: CjFile): Sequence<PsiNameIdentifierOwner> =
        file.collectDescendantsOfType<PsiNameIdentifierOwner>().asSequence()
            .mapNotNull(::namedDeclarationTarget).distinct()

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

    /** 身份由 Analysis API 的声明模块和专用 pointer 承载，不在 LSP 重建名称或文本坐标。 */
    private fun CaSymbol.toTargetKey(): AnalysisApiLspTargetKey =
        AnalysisApiLspTargetKey(containingModule, createPointer(), this is CaPackageSymbol, this is CaPatternVariableSymbol)

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
internal class AnalysisApiLspTargetKey(
    private val containingModule: CaModule,
    private val pointer: CaSymbolPointer<CaSymbol>,
    private val packageSymbol: Boolean,
    private val patternDeclaration: Boolean,
) {
    /**
     * 在当前会话恢复真实声明。package 是聚合符号，其 containingModule 按 API 契约是查询上下文；
     * 其它 symbol 必须保持真实声明模块，旧会话中的公开 symbol 不越过生命周期边界。
     */
    fun restore(session: CaSession): CaSymbol? =
        session.restoreSymbol(pointer)?.takeIf { it is CaPackageSymbol || it.containingModule == containingModule }

    /** 比较真实声明身份；不可恢复的 pointer 不制造替代身份。 */
    fun matches(session: CaSession, other: AnalysisApiLspTargetKey?): Boolean {
        if (other == null) return false
        // API 的模式容器与具名叶子是不同声明角色；不能恢复无关 PSI 来试探身份。
        if (packageSymbol != other.packageSymbol || patternDeclaration != other.patternDeclaration) return false
        // package 是查询上下文聚合；其余声明身份必须先保持实际声明 owner。
        if (!packageSymbol && containingModule != other.containingModule) return false
        val symbol = restore(session) ?: return false
        val otherSymbol = other.restore(session) ?: return false
        return symbol == otherSymbol
    }
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
