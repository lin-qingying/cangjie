@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class, org.cangnova.cangjie.source.SuspiciousFakeSourceCheck::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.file.builder

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.partialBodyAnalysisState
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.builder.toParsingContext
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.directComponentTypes
import org.cangnova.cangjie.cfir.resolve.dfa.createDfaSourceVisitor
import org.cangnova.cangjie.cfir.resolve.dfa.CfirControlFlowGraphReferenceImpl
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraph
import org.cangnova.cangjie.cfir.types.ConeUnreportedDuplicateDiagnostic
import org.cangnova.cangjie.cfir.types.DiagnosticSourceOwner
import org.cangnova.cangjie.cfir.types.DiagnosticSourceUpdate
import org.cangnova.cangjie.cfir.diagnostic.ConeDiagnosticWithCandidates
import org.cangnova.cangjie.cfir.diagnostic.ConeAmbiguityError
import org.cangnova.cangjie.cfir.diagnostic.ConeAmbiguousFunctionReferenceError
import org.cangnova.cangjie.cfir.diagnostic.MemberLookupBlockedByDeclaredSupertype
import org.cangnova.cangjie.cfir.diagnostics.CfirDiagnosticHolder
import org.cangnova.cangjie.cfir.semantics.AbstractCallCandidate
import org.cangnova.cangjie.cfir.resolve.providers.macro.IfAvailableSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceDecl
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceExpr
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceNode
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceParam
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceSourceRange
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceToken
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroTokenSourceOrigin
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import org.cangnova.cangjie.parsing.ParsedCangJiePsiFile
import org.cangnova.cangjie.parsing.CjParsingSourceBindingScope
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.source.*
import java.util.Collections
import java.util.IdentityHashMap

/**
 * 一个原始文件入口、完整目标 PSI 产品及保留 CFIR 图的共同代次。
 * 原对象导航和语义保留证明分别冻结；旧产品失效后修改服务只使用已登记的 owner 关联。
 */
internal class LLCfirFileSourceGeneration(
    val product: ParsedCangJiePsiFile,
    val cfirFile: CfirFile,
    languageVersionSettings: LanguageVersionSettings,
) {
    val origins: Map<PsiElement, PsiElement> = product.captureOriginalElements()
    val targets: Map<PsiElement, PsiElement> = IdentityHashMap<PsiElement, PsiElement>().apply {
        origins.forEach { (target, origin) -> check(put(origin, target) == null) }
    }
    val semantics = LLCfirSourceSemanticSnapshot(product, origins, languageVersionSettings)
    private val declarations = IdentityHashMap<CjElement, CfirDeclaration>()

    /** 消费 session 身份与模块编译语境必须共同一致；模块相等不能替代 session 同代。 */
    fun requireConsistentConfiguration(session: CfirSession) {
        check(cfirFile.moduleData.session === session) {
            "A module source cache must consume declarations from its own session generation."
        }
        val context = session.cjmpSettings.toParsingContext(session.languageVersionSettings)
        check(product.actualInputSeed.cjmpContext == context) {
            "A cached source generation cannot mix its frozen parsing context with a different session configuration."
        }
    }

    /** 缓存产品保持源码代次与消费 session 的编译语境，不允许新配置读取旧 raw 属性。 */
    fun requireConsistentInput(session: CfirSession) {
        requireConsistentConfiguration(session)
        product.requireConsistentInput()
    }

    /** 在有效代次登记修改 owner；以后不通过名称或已经失效的产品查找它。 */
    fun registerDeclarationOwners() {
        val visited = Collections.newSetFromMap(IdentityHashMap<CfirElement, Boolean>())
        cfirFile.accept(object : CfirDefaultVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (!visited.add(element)) return
                val original = origins[element.psi]
                if (element is CfirNamedFunction && original is CjNamedFunction ||
                    element is CfirProperty && original is CjProperty ||
                    element is CfirPropertyAccessor && original is CjPropertyAccessor ||
                    element is CfirCodeFragment && original is CjCodeFragment
                ) {
                    check(declarations.put(original as CjElement, element as CfirDeclaration).let { it == null || it === element }) {
                        "An original modification owner must identify one actual CFIR declaration."
                    }
                }
                element.acceptChildren(this)
                if (element is CfirResolvedTypeRef) element.delegatedTypeRef?.accept(this)
            }

            override fun visitLazyBlock(lazyBlock: CfirLazyBlock) = Unit
        })
    }

    fun declarationForModification(owner: CjElement): CfirDeclaration? = declarations[owner]

    fun inheritDeclarationOwners(previous: LLCfirFileSourceGeneration) {
        previous.declarations.forEach { (origin, declaration) ->
            if (origin in targets) declarations[origin] = declaration
        }
    }

    fun compilationElement(element: PsiElement, session: CfirSession): PsiElement? {
        requireConsistentInput(session)
        return when (element.containingFile) {
            product.file -> element
            product.originFile -> targets[element]
            else -> null
        }
    }

    /** 公开源展示仅返回原树对象，编译器始终保留目标树中的真实 source。 */
    fun originalSource(source: AbstractCjSourceElement, session: CfirSession): AbstractCjSourceElement? {
        requireConsistentInput(session)
        val syntax = when (source) {
            is CjSourceElement -> source
            is CjOffsetsOnlySourceElement -> source.sourceAnchor ?: return null
        }
        return mapPsiSource(syntax) { origins[it] }
    }
}

/**
 * 先完成全部保留子图 source 的映射与校验，再统一写入。声明及 symbol 不重建、不再次 bind。
 * delegated type ref 不在生成 visitor 子图中，但它仍属于保留的源展示图，必须同时迁移。
 */
internal class LLCfirFileSourceReplacement(
    private val previous: LLCfirFileSourceGeneration,
    private val next: LLCfirFileSourceGeneration,
    private val discardedRoots: Set<CfirElement>,
    private val discardedGraphs: Set<ControlFlowGraph>,
    private val modifiedDeclarations: Set<CfirDeclaration>,
) {
    private class Slot(val before: CjSourceElement, val after: CjSourceElement, val read: () -> CjSourceElement?, val write: (CjSourceElement) -> Unit)
    private val slots = mutableListOf<Slot>()
    private class ViewSlot(
        val owner: CfirAnnotationCall,
        val before: CfirAnnotationArgumentView,
        val after: CfirAnnotationArgumentView,
    )
    private val viewSlots = mutableListOf<ViewSlot>()
    private val diagnosticUpdates = mutableListOf<DiagnosticSourceUpdate>()
    val retainedDeclarations: MutableSet<CfirDeclaration> = linkedSetOf()

    fun mapSource(source: CjSourceElement): CjSourceElement? = mapPsiSource(source) { old ->
        if (old.containingFile !== previous.product.file) old else previous.origins[old]?.let(next.targets::get)
    }

    /** offset-only载荷也必须保留原语法锚点；只有选定该锚点之后才迁移其内部边界。 */
    private fun mapDiagnosticSource(source: AbstractCjSourceElement): AbstractCjSourceElement = when (source) {
        is CjSourceElement -> checkNotNull(mapSource(source)) { "A retained diagnostic source has no current carrier." }
        is CjOffsetsOnlySourceElement -> {
            val anchor = checkNotNull(source.sourceAnchor) { "A retained offset diagnostic must own its syntax anchor." }
            val mapped = checkNotNull(mapSource(anchor))
            if (mapped === anchor) source else {
                val oldPsi = checkNotNull(anchor.psi)
                val newPsi = checkNotNull(mapped.psi)
                val mapper: (PsiElement) -> PsiElement? = { previous.origins[it]?.let(next.targets::get) }
                CjOffsetsOnlySourceElement(
                    checkNotNull(mapPsiOffset(oldPsi, newPsi, source.startOffset, false, mapper)),
                    checkNotNull(mapPsiOffset(oldPsi, newPsi, source.endOffset, true, mapper)),
                    mapped,
                )
            }
        }
    }

    /** 非 IR annotation/token 来源也属于同一产品，不能只迁移 visitor 可见的 source。 */
    fun mapTokens(tokens: List<MacroSurfaceToken>): List<MacroSurfaceToken> = tokens.map { token ->
        val spans = token.sourceSpans.map span@{ span ->
            val origin = span.origin as? MacroTokenSourceOrigin.Input ?: return@span span
            val mapped = checkNotNull(mapSource(origin.source)) { "A retained token span must identify its current target carrier." }
            if (mapped === origin.source) span else {
                check(origin.source.text == mapped.text) { "A retained token span changed its grammar text." }
                span.copy(origin = MacroTokenSourceOrigin.Input(
                    mapped, origin.renderedText, origin.isLinear, next.product.sourceFileLinesMapping,
                ))
            }
        }
        val remapped = if (spans == token.sourceSpans) token else token.copy(sourceSpans = spans)
        val originalRange = token.sourceRange ?: return@map remapped
        val originalSource = originalRange.source ?: return@map remapped
        val mapped = checkNotNull(mapSource(originalSource)) { "A retained token source must identify its current target carrier." }
        if (mapped === originalSource) return@map remapped
        check(originalSource.text == mapped.text) { "A retained token carrier changed its grammar text." }
        val delta = mapped.startOffset - originalSource.startOffset
        val start = if (token.isSourceToken) token.startOffset + delta else token.startOffset
        val end = if (token.isSourceToken) token.endOffset + delta else token.endOffset
        if (token.isSourceToken) {
            check(next.product.lexicalInput.source.text.substring(start, end) == token.text)
        }
        remapped.copy(
            startOffset = start, endOffset = end,
            sourceRange = originalRange.copy(source = mapped, startOffset = originalRange.startOffset + delta, endOffset = originalRange.endOffset + delta),
        )
    }

    fun mapSurface(surface: MacroSurface): MacroSurface {
        val range = checkNotNull(surface.sourceRange)
        val originalSource = checkNotNull(range.source)
        val mapped = checkNotNull(mapSource(originalSource))
        val psi = checkNotNull(mapped.psi) { "A retained macro surface must retain its actual invocation PSI." }
        val boundFacts = surface.parsingFacts?.let {
            val facts = checkNotNull(when (psi) {
                is CjMacroExpression -> psi.parsingFacts
                is CjAnnotation -> psi.parsingFacts
                else -> null
            }) { "A retained parser-produced surface must still have its own invocation facts." }
            facts.copy(declarationPrefix = CjParsingSourceBindingScope(next.product.file).bind(facts.declarationPrefix, psi))
        }
        val sourceRange = MacroSurfaceSourceRange(mapped, mapped.startOffset, mapped.endOffset)
        val attrTokens = mapTokens(surface.attrTokens)
        val inputTokens = mapTokens(surface.inputTokens)
        return when (surface) {
            is MacroSurfaceExpr -> surface.copy(parsingFacts = checkNotNull(boundFacts), sourceRange = sourceRange, attrTokens = attrTokens, inputTokens = inputTokens)
            is MacroSurfaceNode -> surface.copy(parsingFacts = checkNotNull(boundFacts), sourceRange = sourceRange, attrTokens = attrTokens, inputTokens = inputTokens)
            is MacroSurfaceDecl -> surface.copy(parsingFacts = boundFacts, sourceRange = sourceRange, attrTokens = attrTokens, inputTokens = inputTokens)
            is MacroSurfaceParam -> surface.copy(parsingFacts = boundFacts, sourceRange = sourceRange, attrTokens = attrTokens, inputTokens = inputTokens)
            is IfAvailableSurface -> surface.copy(
                parsingFacts = boundFacts, sourceRange = sourceRange, attrTokens = attrTokens, inputTokens = inputTokens,
                branchTokens = mapTokens(surface.branchTokens),
            )
        }
    }

    fun prepare(): Boolean {
        val visited = Collections.newSetFromMap(IdentityHashMap<CfirElement, Boolean>())
        val diagnosticVisited = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
        val typesVisited = Collections.newSetFromMap(IdentityHashMap<ConeCangJieType, Boolean>())
        val ownedDeclarations = Collections.newSetFromMap(IdentityHashMap<CfirDeclaration, Boolean>())
        previous.cfirFile.accept(object : CfirDefaultVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (element is CfirDeclaration) ownedDeclarations += element
                element.acceptChildren(this)
            }

            override fun visitLazyBlock(lazyBlock: CfirLazyBlock) = Unit
        })
        var complete = true
        fun recordDiagnosticOwner(owner: Any): Boolean {
            if (!diagnosticVisited.add(owner)) return false
            if (owner is DiagnosticSourceOwner) diagnosticUpdates += owner.diagnosticSourceSlots.map { it.prepare(::mapDiagnosticSource) }
            return true
        }
        fun recordDiagnostic(diagnostic: ConeDiagnostic) {
            if (!recordDiagnosticOwner(diagnostic)) return
            when (diagnostic) {
                is ConeUnreportedDuplicateDiagnostic -> recordDiagnostic(diagnostic.original)
                is ConeAmbiguityError -> {
                    diagnostic.candidatesWithErrors.values.filterNotNull().forEach(::recordDiagnostic)
                    diagnostic.dominatedNestedDiagnostics.forEach(::recordDiagnostic)
                }
                is ConeAmbiguousFunctionReferenceError -> diagnostic.candidatesWithErrors.values.filterNotNull().forEach(::recordDiagnostic)
            }
            if (diagnostic is ConeDiagnosticWithCandidates) {
                diagnostic.candidates.filterIsInstance<AbstractCallCandidate<*>>().forEach { candidate ->
                    (candidate.diagnostics + candidate.cangjieVariadicRegularCallDiagnostics).forEach { resolutionDiagnostic ->
                        recordDiagnosticOwner(resolutionDiagnostic)
                        if (resolutionDiagnostic is MemberLookupBlockedByDeclaredSupertype) recordDiagnostic(resolutionDiagnostic.rootDiagnostic)
                    }
                }
            }
        }
        fun recordType(type: ConeCangJieType?) {
            if (type == null || !typesVisited.add(type)) return
            if (type is ConeErrorType) recordDiagnostic(type.diagnostic)
            type.directComponentTypes().forEach(::recordType)
        }
        fun record(source: CjSourceElement?, read: () -> CjSourceElement?, write: (CjSourceElement) -> Unit) {
            if (source == null) return
            val mapped = mapSource(source)
            if (mapped == null) {
                complete = false
            } else if (mapped !== source) {
                slots += Slot(source, mapped, read, write)
            }
        }
        previous.cfirFile.accept(object : CfirDefaultVisitorVoid() {
            private val dfa = createDfaSourceVisitor(
                onElement = { it.accept(this) }, onType = ::recordType,
                canVisitGraph = { it !in discardedGraphs },
            )

            override fun visitElement(element: CfirElement) {
                if (element in discardedRoots || !visited.add(element)) return
                // 保留图可以借用其他文件/会话的声明或默认值；本批只取得本文件实际载体的写入权。
                val sourcePsi = element.source?.psi
                if (element is CfirDeclaration) {
                    if (element.moduleData.session !== previous.cfirFile.moduleData.session) return
                    if (element !in ownedDeclarations && sourcePsi?.containingFile !== previous.product.file) return
                }
                if (element is CfirDeclaration) retainedDeclarations += element
                if (element is CfirDiagnosticHolder) recordDiagnostic(element.diagnostic)
                if (element is CfirResolvedTypeRef) recordType(element.coneType)
                if (element is CfirExpression) recordType(element.coneTypeOrNull)
                record(element.source, { element.source }, element::replaceSource)
                when (element) {
                    is CfirImport -> record(element.aliasSource, { element.aliasSource }, element::replaceAliasSource)
                    is CfirIfAvailableExpression -> {
                        record(element.conditionNameSource, { element.conditionNameSource }, element::replaceConditionNameSource)
                        record(element.conditionArgumentSource, { element.conditionArgumentSource }, element::replaceConditionArgumentSource)
                    }
                    is CfirNamedArgumentExpression -> record(element.nameSource, { element.nameSource }, element::replaceNameSource)
                    is CfirAugmentedAssignment -> record(element.operationSource, { element.operationSource }, element::replaceOperationSource)
                    is CfirIncrementDecrementExpression -> record(element.operationSource, { element.operationSource }, element::replaceOperationSource)
                }
                if (element is CfirAnnotationCall) {
                    element.argumentView?.let { view ->
                        val entries = view.entries.map { entry ->
                            val source = entry.source
                            val mapped = source?.let(::mapSource)
                            if (source != null && mapped == null) complete = false
                            if (mapped === source) entry else entry.copy(source = mapped)
                        }
                        if (entries.indices.any { entries[it] !== view.entries[it] }) {
                            viewSlots += ViewSlot(element, view, view.copy(entries = entries))
                        }
                    }
                }
                if (element is CfirDeclaration && element !in modifiedDeclarations) {
                    element.partialBodyAnalysisState?.analysisStateSnapshot?.acceptSourceChildren(dfa)
                }
                if (element is CfirControlFlowGraphReferenceImpl) dfa.visitGraph(element.controlFlowGraph)
                else element.acceptSourceChildren(this)
            }

            override fun visitLazyBlock(lazyBlock: CfirLazyBlock) = Unit
        })
        return complete
    }

    fun validate() {
        next.requireConsistentInput(previous.cfirFile.moduleData.session)
        slots.forEach { check(it.read() === it.before) { "A retained source slot changed before generation adoption." } }
        viewSlots.forEach { check(it.owner.argumentView === it.before) { "Annotation argument view changed before source adoption." } }
        diagnosticUpdates.forEach { it.validate() }
    }

    fun commit() {
        validate()
        slots.forEach { it.write(it.after) }
        viewSlots.forEach { it.owner.replaceArgumentView(it.after) }
        diagnosticUpdates.forEach { it.commit() }
    }
}

/** PSI 载体先按对象映射；偏移策略只迁移已选中载体的范围，不据范围选择声明 owner。 */
internal fun mapPsiSource(source: CjSourceElement, mapper: (PsiElement) -> PsiElement?): CjSourceElement? {
    val psiSource = when (source) {
        is CjPsiSourceElement -> source
        is CjLightSourceElement -> source.unwrapToCjPsiSourceElement() ?: return null
        is CjBinarySourceElement -> return source
    }
    val target = mapper(psiSource.psi) ?: return null
    if (target === psiSource.psi) return source
    if (psiSource !is CjFakePsiSourceElementWithCustomOffsetStrategy) return target.toCjPsiSourceElement(psiSource.kind)
    val strategy = when (val original = psiSource.strategy) {
        is CjSourceElementOffsetStrategy.Custom.Delegated -> CjSourceElementOffsetStrategy.Custom.Delegated(
            mapPsiSource(original.startOffsetAnchor, mapper) ?: return null,
            mapPsiSource(original.endOffsetAnchor, mapper) ?: return null,
        )
        is CjSourceElementOffsetStrategy.Custom.Initialized -> {
            CjSourceElementOffsetStrategy.Custom.Initialized(
                mapPsiOffset(psiSource.psi, target, original.startOffset, false, mapper) ?: return null,
                mapPsiOffset(psiSource.psi, target, original.endOffset, true, mapper) ?: return null,
            )
        }
    }
    return CjFakePsiSourceElementWithCustomOffsetStrategy(target, psiSource.kind as CjFakeSourceElementKind, strategy)
}

/** 只在已经按对象选定的载体内寻找实际叶边界；不能用数字偏移查找另一个声明owner。 */
private fun mapPsiOffset(
    previous: PsiElement,
    target: PsiElement,
    offset: Int,
    isEnd: Boolean,
    mapper: (PsiElement) -> PsiElement?,
): Int? {
    val before = previous.textRange
    if (offset == before.startOffset) return target.textRange.startOffset
    if (offset == before.endOffset) return target.textRange.endOffset
    if (offset !in before.startOffset..before.endOffset) return null
    fun leaf(element: PsiElement): PsiElement? {
        val children = generateSequence(element.firstChild) { it.nextSibling }.toList()
        if (children.isEmpty()) return element
        val owner = children.singleOrNull { child ->
            val range = child.textRange
            if (isEnd) offset > range.startOffset && offset <= range.endOffset else offset >= range.startOffset && offset < range.endOffset
        } ?: return null
        return leaf(owner)
    }
    val actual = leaf(previous) ?: return null
    val successor = mapper(actual) ?: return null
    if (actual.text != successor.text) return null
    return successor.textRange.startOffset + offset - actual.textRange.startOffset
}
