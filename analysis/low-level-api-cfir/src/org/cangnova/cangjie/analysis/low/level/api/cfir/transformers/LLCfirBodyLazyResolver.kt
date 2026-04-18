/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiElement
import com.intellij.psi.util.descendantsOfType
import kotlinx.collections.immutable.toPersistentList
import org.cangnova.cangjie.CjPsiSourceElement
import org.cangnova.cangjie.CjRealSourceElementKind
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getResolutionFacade
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets.*
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.throwUnexpectedCfirElementError
import org.cangnova.cangjie.analysis.low.level.api.cfir.compile.codeFragmentScopeProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.LLCfirDeclarationModificationService
import org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve.LLCfirPhaseUpdater
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.llCfirModuleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.*
import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.utils.evaluatedInitializer
import org.cangnova.cangjie.cfir.declarations.utils.getExplicitBackingField
import org.cangnova.cangjie.cfir.declarations.utils.isLocal
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.resolve.CfirCodeFragmentContext
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.ScopeSession
import org.cangnova.cangjie.cfir.resolve.codeFragmentContext
import org.cangnova.cangjie.cfir.resolve.dfa.CfirControlFlowGraphReferenceImpl
import org.cangnova.cangjie.cfir.resolve.dfa.RealVariable
import org.cangnova.cangjie.cfir.resolve.dfa.SnapshotCfirMapper
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.*
import org.cangnova.cangjie.cfir.resolve.dfa.controlFlowGraph
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.*
import org.cangnova.cangjie.cfir.scopes.DelicateScopeAPI
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.ConeKotlinType
import org.cangnova.cangjie.cfir.types.hasResolvedType
import org.cangnova.cangjie.cfir.utils.exceptions.withCfirEntry
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.psi
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.utils.exceptions.checkWithAttachment
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment
import org.cangnova.cangjie.utils.exceptions.withPsiEntry
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KProperty1

internal object LLCfirBodyLazyResolver : LLCfirLazyResolver(CfirResolvePhase.BODY_RESOLVE) {
    override fun createTargetResolver(target: LLCfirResolveTarget): LLCfirTargetResolver = LLCfirBodyTargetResolver(target)

    override fun phaseSpecificCheckIsResolved(target: CfirElementWithResolveState) {
        when (target) {
            is CfirValueParameter -> checkDefaultValueIsResolved(target)
            is CfirVariable -> checkInitializerIsResolved(target)
            is CfirConstructor -> checkBodyIsResolved(target)
            is CfirFunction -> checkBodyIsResolved(target)
        }
    }
}

/**
 * An exception signifying that the requested part of the declaration body was successfully resolved.
 * The exception is thrown to stop analysis finalization steps in the LL API.
 *
 * The exception must always be handled on the LL API side (so it must be user-invisible).
 */
internal object PartialBodyAnalysisSuspendedException :
    RuntimeException(
        /* message = */ "Partial body analysis was suspended",
        /* cause = */ null,
        /* enableSuppression = */ false,
        /* writableStackTrace = */ false
    )

/**
 * A declaration transformer providing fast-track for declarations with partial analysis state.
 * Signatures of such declarations (e.g., value of type parameters) are already resolved, so we can skip them.
 *
 * Logic of implementations should be consistent with those in the [CfirDeclarationsResolveTransformer].
 * In particular, all work that happens after body resolution should also happen in the [CfirPartialBodyDeclarationResolveTransformer].
 */
private class CfirPartialBodyDeclarationResolveTransformer(
    transformer: CfirAbstractBodyResolveTransformerDispatcher
) : CfirDeclarationsResolveTransformer(transformer) {
    override fun transformFunctionContent(
        function: CfirFunction,
        resolutionModeForBody: ResolutionMode,
        shouldResolveEverything: Boolean
    ): CfirFunction {
        if (function.partialBodyAnalysisState != null) {
            function.transformBody(this, resolutionModeForBody)
            function.replaceControlFlowGraphReference(dataFlowAnalyzer.exitFunction(function))
            return function
        }

        return super.transformFunctionContent(function, resolutionModeForBody, shouldResolveEverything)
    }

    override fun transformConstructorContent(constructor: CfirConstructor, data: ResolutionMode): CfirConstructor {
        if (constructor.partialBodyAnalysisState != null) {
            context.forConstructor(constructor) {
                context.forConstructorBody(constructor, session) {
                    constructor.transformBody(this, data)
                }
            }

            constructor.replaceControlFlowGraphReference(dataFlowAnalyzer.exitFunction(constructor))
            return constructor
        }

        return super.transformConstructorContent(constructor, data)
    }

    override fun transformAnonymousInitializerContent(
        anonymousInitializer: CfirAnonymousInitializer,
        data: ResolutionMode
    ): CfirAnonymousInitializer {
        if (anonymousInitializer.partialBodyAnalysisState != null) {
            context.withAnonymousInitializer(anonymousInitializer, session) {
                val result = transformDeclarationContent(
                    anonymousInitializer,
                    ResolutionMode.ContextIndependent
                ) as CfirAnonymousInitializer

                val graph = dataFlowAnalyzer.exitInitBlock(result)
                result.replaceControlFlowGraphReference(CfirControlFlowGraphReferenceImpl(graph))
                return result
            }
        }

        return super.transformAnonymousInitializerContent(anonymousInitializer, data)
    }
}

private class CfirPartialBodyExpressionResolveTransformer(
    transformer: CfirAbstractBodyResolveTransformerDispatcher,
    private val target: LLCfirResolveTarget
) : CfirExpressionsResolveTransformer(transformer) {
    private companion object {
        // After a certain number of partial analyses,
        // trigger the full analysis so we don't return to the same declaration over and over again.
        // Note that the first analysis can also perform only default parameter value analysis and exit just after it.
        private val MAX_ANALYSES_COUNT: Int by lazy(LazyThreadSafetyMode.PUBLICATION) {
            // On various repositories, number of declarations analyzed more than five times, is under 1%.
            // So here we cap only unusually lengthy declarations.
            Registry.intValue("kotlin.analysis.partialBodyAnalysis.attemptCount", 5)
        }
    }

    private var isInsideAnalysis = false

    override fun transformBlock(block: CfirBlock, data: ResolutionMode): CfirStatement {
        val declaration = context.containerIfAny

        if (isInsideAnalysis) {
            return super.transformBlock(block, data)
        }

        val isApplicable = declaration is CfirDeclaration
                && declaration.isPartialBodyResolvable
                && declaration.body == block
                && block.isPartialAnalyzable

        if (!isApplicable) {
            performTopmostBlockAnalysis {
                return super.transformBlock(block, data)
            }
        }

        require(data is ResolutionMode.ContextIndependent)

        val state = declaration.partialBodyAnalysisState

        performTopmostBlockAnalysis {
            if (target is LLCfirPartialBodyResolveTarget && (state == null || state.performedAnalysesCount < MAX_ANALYSES_COUNT)) {
                transformPartially(target.request, block, data, state)
            } else {
                transformFully(declaration, block, data, state)
            }
        }

        return block
    }

    @OptIn(ExperimentalContracts::class)
    private inline fun performTopmostBlockAnalysis(block: () -> Unit) {
        contract {
            callsInPlace(block, InvocationKind.EXACTLY_ONCE)
        }

        require(!isInsideAnalysis)

        try {
            isInsideAnalysis = true
            block()
        } finally {
            isInsideAnalysis = false
        }
    }

    @OptIn(CfgInternals::class)
    private fun transformPartially(
        request: LLPartialBodyResolveRequest,
        block: CfirBlock,
        data: ResolutionMode,
        state: LLPartialBodyAnalysisState?
    ): CfirStatement {
        val declaration = target.target as CfirDeclaration

        if (state == null) {
            if (request.stopElement == null) {
                // Without a 'stopElement', we resolve "the rest of the body".
                // We didn't analyze the body yet, though.
                // So here we just delegate to the non-partial implementation.
                require(request.targetPsiStatementCount == request.totalPsiStatementCount)
                return super.transformBlock(block, data)
            }

            context.forBlock(session) {
                dataFlowAnalyzer.enterBlock(block)
                if (transformStatementsPartially(request, block, data, startIndex = 0, performedAnalysesCount = 0)) {
                    dataFlowAnalyzer.exitBlock(block)
                }
            }

            return block
        }

        // Required statements might already be analyzed
        if (state.analyzedPsiStatementCount >= request.targetPsiStatementCount) {
            if (!state.isFullyAnalyzed) {
                // Execution should never finish normally if the body is not entirely analyzed
                throw PartialBodyAnalysisSuspendedException
            }

            return block
        }

        val resolveSnapshot = state.analysisStateSnapshot
        checkWithAttachment(resolveSnapshot != null, { "Snapshot should be available for a partially analyzed declaration" }) {
            withCfirEntry("target", declaration)
            withEntry("state", state) { it.toString() }
        }

        // Run analysis with the previous tower data context
        context.withTowerDataContext(resolveSnapshot.towerDataContext) {
            // Not yet analyzed statements may still appear in the control flow graph, e.g., in 'CfirLocalVariableAssignmentAnalyzer'.
            // As state keepers replace unresolved statements with freshly created ones ('preservePartialBodyResolveResult'),
            // we need to adapt the snapshot so it reflects the new reality.
            val firMapper = LLSnapshotCfirMapper(block.statements.subList(state.analyzedCfirStatementCount, block.statements.size))

            // Restore the previous data flow analyzer state.
            // Here we create a snapshot right before the analysis, so if an exception occurs during this partial analysis,
            // we can still safely use the original 'dataFlowAnalyzerContext' from the 'analysisStateSnapshot' the next time.
            val originalContext = resolveSnapshot.dataFlowAnalyzerContext
            val contextSnapshot = originalContext.createSnapshot(firMapper)

            if (declaration is CfirFunction) {
                patchControlFlowGraphReferences(declaration.valueParameters, contextSnapshot.graphMapping)
            }

            patchControlFlowGraphReferences(block.statements.subList(0, state.analyzedCfirStatementCount), contextSnapshot.graphMapping)

            context.dataFlowAnalyzerContext.resetFrom(contextSnapshot.context)
            dataFlowAnalyzer.resetSmartCastPosition()

            /** No [BodyResolveContext.forBlock] as here we manually restore the tower data context from the snapshot. */
            val isAnalyzedEntirely = transformStatementsPartially(
                request, block, data,
                startIndex = state.analyzedCfirStatementCount,
                performedAnalysesCount = state.performedAnalysesCount
            )

            if (isAnalyzedEntirely) {
                dataFlowAnalyzer.exitBlock(block)
            }
        }

        return block
    }

    @CfgInternals
    private class LLSnapshotCfirMapper(private val roots: List<CfirElement>) : SnapshotCfirMapper {
        private fun shouldBeHandled(element: CfirElement): Boolean {
            /** Accepts elements handled by [org.cangnova.cangjie.cfir.resolve.dfa.CfirLocalVariableAssignmentAnalyzer] */
            val isElementKindHandled = when (element) {
                is CfirDeclaration -> element.isLocal
                is CfirLoop -> true
                else -> false
            }

            return isElementKindHandled && element.source?.kind == CjRealSourceElementKind
        }

        private val mapping: Map<PsiElement, CfirElement> by lazy(LazyThreadSafetyMode.NONE) {
            val result = HashMap<PsiElement, CfirElement>()

            val visitor = object : CfirVisitorVoid() {
                override fun visitElement(element: CfirElement) {
                    if (shouldBeHandled(element)) {
                        val psi = element.source?.psi
                        if (psi != null) {
                            val previousElement = result.put(psi, element)

                            // No clashes are expected for anchor elements stored in the CFG.
                            // Otherwise, we don't be able to patch the references.
                            checkWithAttachment(
                                previousElement == null || previousElement === element,
                                message = { "Duplicate PSI element of type ${psi::class.simpleName}" }
                            ) {
                                withCfirEntry("element", element)
                            }
                        }
                    }
                    element.acceptChildren(this)
                }
            }

            roots.forEach { it.accept(visitor) }
            result
        }

        override fun <T : CfirElement> mapElement(element: T): T {
            if (!shouldBeHandled(element)) {
                return element
            }

            // Every element stored in the CFG must have a corresponding PSI element.
            // Note that it's different from the mapping visitor – there we only search for candidate elements, not knowing yet if
            // they are mentioned in the graph.
            val psi = element.source?.psi
                ?: errorWithAttachment("No PSI for ${element::class.simpleName}") {
                    withCfirEntry("element", element)
                }

            val newElement = mapping[psi]
                ?: return element

            checkWithAttachment(
                element.javaClass == newElement.javaClass,
                message = { "Expected ${element::class.simpleName}, got ${newElement.javaClass.simpleName}" }
            ) {
                withCfirEntry("element", element)
                withEntry("mapping", mapping) { it.toString() }
            }

            @Suppress("UNCHECKED_CAST")
            return newElement as T
        }

        override fun <T : CfirBasedSymbol<*>> mapSymbol(symbol: T): T {
            @Suppress("UNCHECKED_CAST")
            return mapElement(symbol.fir).symbol as T
        }
    }

    /**
     * Replaces references to stale [ControlFlowGraph]s in already analyzed [CfirElement]s to one from the newly created snapshot.
     * Patching does not require explicit locking as clients must only access the [ControlFlowGraph] nodes through
     * the [LLPartialBodyAnalysisSnapshot].
     */
    private fun patchControlFlowGraphReferences(elements: Collection<CfirElement>, graphMapping: Map<ControlFlowGraph, ControlFlowGraph>) {
        val visitor = object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (element is CfirControlFlowGraphOwner) {
                    patchReference(element)
                }
                element.acceptChildren(this)
            }

            private fun patchReference(owner: CfirControlFlowGraphOwner) {
                val reference = owner.controlFlowGraphReference ?: return
                val existingGraph = reference.controlFlowGraph ?: return
                val newGraph = graphMapping[existingGraph] ?: return
                owner.replaceControlFlowGraphReference(CfirControlFlowGraphReferenceImpl(newGraph))
            }
        }

        for (element in elements) {
            element.accept(visitor)
        }
    }

    private fun transformStatementsPartially(
        request: LLPartialBodyResolveRequest,
        block: CfirBlock,
        data: ResolutionMode,
        startIndex: Int,
        performedAnalysesCount: Int,
    ): Boolean {
        val declaration = request.target

        val stopElement = request.stopElement
        val stopElements = stopElement?.descendantsOfType<CjElement>(childrenCfirst = false)?.toHashSet().orEmpty()

        var index = 0
        val iterator = (block.statements as MutableList<CfirStatement>).listIterator()

        while (iterator.hasNext()) {
            val statement = iterator.next()

            // Skip already analyzed statements
            if (index >= startIndex) {
                if (stopElement != null && !shouldTransform(statement, stopElement, stopElements)) {
                    // Here we reached a stop element.
                    // It means all statements up to the target one are now analyzed.
                    // So now we save the current context and suspend further analysis.
                    publishPartialAnalysisState(
                        request = request,
                        statementRange = startIndex..<index,
                        performedAnalysesCount = performedAnalysesCount,
                        analysisStateSnapshot = LLPartialBodyAnalysisSnapshot(
                            result = LLPartialBodyAnalysisResult(
                                statements = block.statements.take(index),
                                defaultParameterValues = collectDefaultParameterValues(declaration)
                            ),
                            towerDataContext = context.towerDataContext.createSnapshot(keepMutable = true),
                            dataFlowAnalyzerContext = context.dataFlowAnalyzerContext
                        )
                    )

                    throw PartialBodyAnalysisSuspendedException
                }

                val newStatement = statement.transform<CfirStatement, ResolutionMode>(transformer, data)
                if (statement !== newStatement) {
                    iterator.set(newStatement)
                }
            }

            index += 1
        }

        // Nothing stopped us from analyzing all statements for some reason.
        // Most likely, we missed the stop element.
        // Let's still wrap things out – the function is now fully analyzed.
        block.transformOtherChildren(transformer, data)

        // This makes the compiler think the declaration is fully resolved (see 'CfirExpression.isResolved')
        block.writeResultType(session)

        publishPartialAnalysisState(
            request = request,
            statementRange = startIndex..<block.statements.size,
            performedAnalysesCount = performedAnalysesCount,
            analysisStateSnapshot = null
        )

        return true
    }

    private fun publishPartialAnalysisState(
        request: LLPartialBodyResolveRequest,
        statementRange: IntRange,
        performedAnalysesCount: Int,
        analysisStateSnapshot: LLPartialBodyAnalysisSnapshot?,
    ) {
        LLCfirPhaseUpdater.updatePartiallyAnalyzedDeclarationContent(
            target = request.target,
            updateSignatureBody = performedAnalysesCount == 0,
            statementRange = statementRange
        )

        request.target.partialBodyAnalysisState = LLPartialBodyAnalysisState(
            totalPsiStatementCount = request.totalPsiStatementCount,
            analyzedPsiStatementCount = request.targetPsiStatementCount,
            analyzedCfirStatementCount = statementRange.last + 1,
            performedAnalysesCount = performedAnalysesCount + 1,
            analysisStateSnapshot = analysisStateSnapshot
        )
    }

    private fun collectDefaultParameterValues(declaration: CfirDeclaration): List<CfirExpression> {
        if (declaration is CfirFunction) {
            val result = declaration.valueParameters.mapNotNull { it.defaultValue }
            return result.ifEmpty { emptyList() }
        }

        return emptyList()
    }

    /**
     * Analyzes the body completely.
     * Note that even in [transformFully], [transformPartially] may be called if the body was already partially analyzed.
     */
    private fun transformFully(
        declaration: CfirDeclaration,
        block: CfirBlock,
        data: ResolutionMode,
        currentState: LLPartialBodyAnalysisState?
    ): CfirStatement {
        if (currentState == null) {
            // The declaration body is not resolved at all, and a full resolution is requested.
            // So, here we delegate straight to the non-partial implementation.
            return super.transformBlock(block, data)
        }

        val request = LLPartialBodyResolveRequest(
            target = declaration,
            totalPsiStatementCount = currentState.totalPsiStatementCount,
            targetPsiStatementCount = currentState.totalPsiStatementCount,
            stopElement = null
        )

        // Otherwise, use the partial resolve to finish the ongoing resolution
        return transformPartially(request, block, data, currentState)
    }

    private fun shouldTransform(element: CfirElement, stopElement: CjElement, stopElements: Set<CjElement>): Boolean {
        val source = element.source
        if (source is CjPsiSourceElement) {
            // Potentially, more expensive `source.psi in stopElements` check may be dropped, but then we need a strong guarantee that
            // all topmost CFIR statements have corresponding topmost PSI statements in source elements.
            if (source.psi == stopElement || source.psi in stopElements) {
                return false
            }
        }

        return true
    }
}

/**
 * This resolver is responsible for [BODY_RESOLVE][CfirResolvePhase.BODY_RESOLVE] phase.
 *
 * This resolver:
 * - Transforms bodies of declarations.
 * - Builds [control flow graph][ControlFlowGraph].
 *
 * Before the transformation, the resolver [recreates][BodyStateKeepers] all bodies
 * to prevent corrupted states due to [PCE][com.intellij.openapi.progress.ProcessCanceledException].
 *
 * Special rules:
 * - [CfirFile] – All members which [isUsedInControlFlowGraphBuilderForFile] have
 *   to be resolved before the file to build correct [CFG][ControlFlowGraph].
 * - [CfirRegularClass] – All members which [isUsedInControlFlowGraphBuilderForClass] have
 *   to be resolved before the class to build correct [CFG][ControlFlowGraph].
 *
 * @see BodyStateKeepers
 * @see CfirBodyResolveTransformer
 * @see CfirResolvePhase.BODY_RESOLVE
 */
private class LLCfirBodyTargetResolver(target: LLCfirResolveTarget) : LLCfirAbstractBodyTargetResolver(target, CfirResolvePhase.BODY_RESOLVE) {
    override val transformer = BodyTransformerDispatcher()

    inner class BodyTransformerDispatcher : CfirAbstractBodyResolveTransformerDispatcher(
        resolveTargetSession,
        phase = resolverPhase,
        implicitTypeOnly = false,
        scopeSession = resolveTargetScopeSession,
        returnTypeCalculator = createReturnTypeCalculator(),
        expandTypeAliases = true
    ) {
        override val expressionsTransformer: CfirExpressionsResolveTransformer =
            CfirPartialBodyExpressionResolveTransformer(this, resolveTarget)

        override val declarationsTransformer: CfirDeclarationsResolveTransformer =
            CfirPartialBodyDeclarationResolveTransformer(this)

        override val preserveCFGForClasses: Boolean get() = false
        override val buildCfgForScripts: Boolean get() = false
        override val buildCfgForFiles: Boolean get() = false

        /**
         * It is safe to resolve foreign annotations on demand because the contract allows it
         * ([annotation arguments][CfirResolvePhase.ANNOTATION_ARGUMENTS] phase is less than [body][CfirResolvePhase.BODY_RESOLVE] phase).
         */
        override fun transformForeignAnnotationCall(symbol: CfirBasedSymbol<*>, annotationCall: CfirAnnotationCall): CfirAnnotationCall {
            // It is possible that some members of local classes will propagate annotations between each other,
            // so we should just skip them, as they will be resolved anyway
            if (symbol.cannotResolveAnnotationsOnDemand()) return annotationCall

            symbol.lazyResolveToPhase(CfirResolvePhase.ANNOTATION_ARGUMENTS)
            checkAnnotationCallIsResolved(symbol, annotationCall)
            return annotationCall
        }
    }

    /**
     * No one should depend on body resolution of another declaration
     */
    override val skipDependencyTargetResolutionStep: Boolean get() = true

    override fun doResolveWithoutLock(target: CfirElementWithResolveState): Boolean {
        when (target) {
            is CfirRegularClass -> {
                if (checkAnalysisReadiness(target, containingDeclarations, resolverPhase)) return true

                // resolve class CFG graph here, to do this we need to have property & init blocks resoled
                resolveMembersForControlFlowGraph(
                    declarationWithMembers = target,
                    withDeclaration = this::withRegularClass,
                    declarationsProvider = CfirRegularClass::declarations,
                    isUsedInControlFlowBuilder = CfirDeclaration::isUsedInClassControlFlowGraphBuilder,
                )

                performCustomResolveUnderLock(target) {
                    calculateControlFlowGraph(target)
                }

                return true
            }

            is CfirFile -> {
                if (checkAnalysisReadiness(target, containingDeclarations, resolverPhase)) return true

                // resolve file CFG graph here, to do this we need to have property blocks resoled
                resolveMembersForControlFlowGraph(
                    declarationWithMembers = target,
                    withDeclaration = this::withFile,
                    declarationsProvider = CfirFile::declarations,
                    isUsedInControlFlowBuilder = CfirDeclaration::isUsedInFileControlFlowGraphBuilder,
                )

                performCustomResolveUnderLock(target) {
                    calculateControlFlowGraph(target)
                }

                return true
            }

            is CfirCodeFragment -> {
                val context = resolveCodeFragmentContext(target)
                performCustomResolveUnderLock(target) {
                    target.codeFragmentContext = context
                    resolve(target, BodyStateKeepers.CODE_FRAGMENT)
                }

                return true
            }
        }

        return false
    }

    private fun calculateControlFlowGraph(target: CfirRegularClass) {
        checkWithAttachment(
            target.controlFlowGraphReference == null,
            { "'controlFlowGraphReference' should be 'null' if the class phase < $resolverPhase)" },
        ) {
            withCfirEntry("firClass", target)
        }

        val dataFlowAnalyzer = transformer.declarationsTransformer.dataFlowAnalyzer
        dataFlowAnalyzer.enterClass(target, buildGraph = true)
        val controlFlowGraph = dataFlowAnalyzer.exitClass()
            ?: errorWithAttachment("CFG should not be 'null' as 'buildGraph' is specified") {
                withCfirEntry("firClass", target)
            }

        target.replaceControlFlowGraphReference(CfirControlFlowGraphReferenceImpl(controlFlowGraph))
    }

    private inline fun <T : CfirElementWithResolveState> resolveMembersForControlFlowGraph(
        declarationWithMembers: T,
        withDeclaration: (T, () -> Unit) -> Unit,
        declarationsProvider: (T) -> List<CfirDeclaration>,
        crossinline isUsedInControlFlowBuilder: (CfirDeclaration) -> Boolean,
    ) {
        val declarations = declarationsProvider(declarationWithMembers)
        if (declarations.none(isUsedInControlFlowBuilder)) return

        withDeclaration(declarationWithMembers) {
            for (declaration in declarations) {
                if (isUsedInControlFlowBuilder(declaration)) {
                    declaration.lazyResolveToPhase(resolverPhase.previous)
                    performResolve(declaration)
                }
            }
        }
    }

    private fun calculateControlFlowGraph(target: CfirFile) {
        checkWithAttachment(
            target.controlFlowGraphReference == null,
            { "'controlFlowGraphReference' should be 'null' if the file phase < $resolverPhase)" },
        ) {
            withCfirEntry("firFile", target)
        }

        val dataFlowAnalyzer = transformer.declarationsTransformer.dataFlowAnalyzer
        dataFlowAnalyzer.enterFile(target, buildGraph = true)
        val controlFlowGraph = dataFlowAnalyzer.exitFile()
            ?: errorWithAttachment("CFG should not be 'null' as 'buildGraph' is specified") {
                withCfirEntry("firFile", target)
            }

        target.replaceControlFlowGraphReference(CfirControlFlowGraphReferenceImpl(controlFlowGraph))
    }

    @OptIn(DelicateScopeAPI::class)
    private fun resolveCodeFragmentContext(firCodeFragment: CfirCodeFragment): LLCfirCodeFragmentContext {
        val ktCodeFragment = firCodeFragment.psi as? CjCodeFragment
            ?: errorWithAttachment("Code fragment source not found") {
                withCfirEntry("firCodeFragment", firCodeFragment)
            }

        val module = firCodeFragment.llCfirModuleData.ktModule
        val resolutionFacade = module.getResolutionFacade(ktCodeFragment.project)

        fun CfirTowerDataContext.withExtraScopes(): CfirTowerDataContext {
            return resolutionFacade.useSiteCfirSession.codeFragmentScopeProvider.getExtraScopes(ktCodeFragment)
                .fold(this) { context, scope ->
                    val scopeWithProperSession = scope.withReplacedSessionOrNull(resolveTargetSession, resolveTargetScopeSession) ?: scope
                    context.addLocalScope(scopeWithProperSession)
                }
        }

        val contextPsiElement = ktCodeFragment.context
        val contextCjFile = contextPsiElement?.containingFile as? CjFile

        return if (contextCjFile != null) {
            val contextCfirFile = resolutionFacade.getOrBuildCfirFile(contextCjFile)

            // Avoid using body context of expressions/statements, as those can contribute additional smart casts.
            // Still use body contexts for declarations (e.g., to be able to address parameters of a primary constructor).
            val preferBodyContext = when (contextPsiElement) {
                is CjDeclaration -> contextPsiElement !is CjProperty || !contextPsiElement.isLocal
                is CjBlockExpression -> true
                else -> false
            }

            val elementContext = ContextCollector.process(resolutionFacade, contextCfirFile, contextPsiElement, preferBodyContext)
                ?: errorWithAttachment("Cannot find enclosing context for ${contextPsiElement::class}") {
                    withPsiEntry("contextPsiElement", contextPsiElement)
                }

            LLCfirCodeFragmentContext(
                elementContext.towerDataContext.withProperSession(resolveTargetSession, resolveTargetScopeSession)
                    .withExtraScopes(),
                elementContext.smartCasts
            )
        } else {
            val towerDataContext = CfirTowerDataContext().withExtraScopes()
            LLCfirCodeFragmentContext(towerDataContext, emptyMap())
        }
    }

    @DelicateScopeAPI
    private fun CfirTowerDataContext.withProperSession(session: CfirSession, scopeSession: ScopeSession): CfirTowerDataContext {
        return replaceTowerDataElements(
            towerDataElements.map { it.withProperSession(session, scopeSession) }.toPersistentList(),
            nonLocalTowerDataElements.map { it.withProperSession(session, scopeSession) }.toPersistentList(),
        )
    }

    @DelicateScopeAPI
    private fun CfirTowerDataElement.withProperSession(
        session: CfirSession,
        scopeSession: ScopeSession,
    ): CfirTowerDataElement = CfirTowerDataElement(
        scope?.withReplacedSessionOrNull(session, scopeSession) ?: scope,
        implicitReceiver?.withReplacedSessionOrNull(session, scopeSession),
        contextParameterGroup,
        isLocal,
        staticScopeOwnerSymbol
    )

    override fun doLazyResolveUnderLock(target: CfirElementWithResolveState) {
        // There is no sense to resolve such declarations as they do not have bodies
        // Also, they have STUB expression instead of default values, so we shouldn't change them
        if (target is CfirCallableDeclaration && target.canHaveDeferredReturnTypeCalculation) return

        when (target) {
            is CfirFile, is CfirRegularClass, is CfirCodeFragment -> error("Should have been resolved in ${::doResolveWithoutLock.name}")
            is CfirConstructor -> resolve(target, BodyStateKeepers.CONSTRUCTOR)
            is CfirFunction -> resolve(target, BodyStateKeepers.FUNCTION)
            is CfirProperty -> resolve(target, BodyStateKeepers.PROPERTY)
            is CfirField -> resolve(target, BodyStateKeepers.FIELD)
            is CfirVariable -> resolve(target, BodyStateKeepers.VARIABLE)
            is CfirAnonymousInitializer -> resolve(target, BodyStateKeepers.ANONYMOUS_INITIALIZER)
            is CfirDanglingModifierList,
            is CfirTypeAlias,
                -> {
                // No bodies here
            }
            else -> throwUnexpectedCfirElementError(target)
        }
    }

    override fun rawResolve(target: CfirElementWithResolveState) {
        try {
            super.rawResolve(target)
        } catch (e: PartialBodyAnalysisSuspendedException) {
            // We successfully analyzed some part of the body so need to keep track of it
            LLCfirDeclarationModificationService.bodyResolved(target, resolverPhase)
            throw e
        }

        LLCfirDeclarationModificationService.bodyResolved(target, resolverPhase)
    }
}

internal object BodyStateKeepers {
    val CODE_FRAGMENT: StateKeeper<CfirCodeFragment, CfirDesignation> = stateKeeper { builder, _, _ ->
        builder.add(CfirCodeFragment::block, CfirCodeFragment::replaceBlock, ::blockGuard)
    }

    val PARTIAL_BODY_RESOLVABLE: StateKeeper<CfirDeclaration, CfirDesignation> = stateKeeper { builder, declaration, context ->
        builder.add(CfirDeclaration::partialBodyAnalysisState::get, CfirDeclaration::partialBodyAnalysisState::set)
    }

    val ANONYMOUS_INITIALIZER: StateKeeper<CfirAnonymousInitializer, CfirDesignation> = stateKeeper { builder, initializer, designation ->
        builder.add(PARTIAL_BODY_RESOLVABLE, designation)
        preserveResolvedState(builder, initializer)

        builder.add(CfirAnonymousInitializer::body, CfirAnonymousInitializer::replaceBody, ::blockGuard)
        builder.add(CfirAnonymousInitializer::controlFlowGraphReference, CfirAnonymousInitializer::replaceControlFlowGraphReference)
    }

    val FUNCTION: StateKeeper<CfirFunction, CfirDesignation> = stateKeeper { builder, function, designation ->
        if (function.isCertainlyResolved) {
            if (!isCallableWithSpecialBody(function)) {
                builder.entityList(function.valueParameters, VALUE_PARAMETER, designation)
            }

            return@stateKeeper
        }

        builder.add(CfirFunction::returnTypeRef, CfirFunction::replaceReturnTypeRef)

        if (!isCallableWithSpecialBody(function)) {
            builder.add(PARTIAL_BODY_RESOLVABLE, designation)
            preserveResolvedState(builder, function)

            builder.add(CfirFunction::body, CfirFunction::replaceBody, ::blockGuard)
            builder.entityList(function.valueParameters, VALUE_PARAMETER, designation)
        }

        builder.add(CfirFunction::controlFlowGraphReference, CfirFunction::replaceControlFlowGraphReference)
    }

    val CONSTRUCTOR: StateKeeper<CfirConstructor, CfirDesignation> = stateKeeper { builder, _, designation ->
        builder.add(FUNCTION, designation)
    }

    val VARIABLE: StateKeeper<CfirVariable, CfirDesignation> = stateKeeper { builder, variable, _ ->
        builder.add(CfirVariable::returnTypeRef, CfirVariable::replaceReturnTypeRef)

        if (!isCallableWithSpecialBody(variable)) {
            variable.initializerGetterIfUnresolved?.let {
                builder.add(it, CfirVariable::replaceInitializer, ::expressionGuard)
            }

            variable.delegateGetterIfUnresolved?.let {
                builder.add(it, CfirVariable::replaceDelegate, ::expressionGuard)
            }
        }
    }

    private val VALUE_PARAMETER: StateKeeper<CfirValueParameter, CfirDesignation> = stateKeeper { builder, valueParameter, _ ->
        if (valueParameter.defaultValue != null) {
            builder.add(CfirValueParameter::defaultValue, CfirValueParameter::replaceDefaultValue, ::expressionGuard)
            builder.add(
                { parameter -> parameter.evaluatedInitializer },
                { parameter, evaluatorResult -> parameter.evaluatedInitializer = evaluatorResult },
            )
        }

        builder.add(CfirValueParameter::controlFlowGraphReference, CfirValueParameter::replaceControlFlowGraphReference)
    }

    val FIELD: StateKeeper<CfirField, CfirDesignation> = stateKeeper { builder, _, designation ->
        builder.add(VARIABLE, designation)
        builder.add(CfirField::controlFlowGraphReference, CfirField::replaceControlFlowGraphReference)
    }

    val PROPERTY: StateKeeper<CfirProperty, CfirDesignation> = stateKeeper { builder, property, designation ->
        if (property.bodyResolveState >= CfirPropertyBodyResolveState.ALL_BODIES_RESOLVED) {
            return@stateKeeper
        }

        builder.add(VARIABLE, designation)

        builder.add(CfirProperty::bodyResolveState, CfirProperty::replaceBodyResolveState)
        builder.add(CfirProperty::returnTypeRef, CfirProperty::replaceReturnTypeRef)

        builder.entity(property.getterIfUnresolved, FUNCTION, designation)
        builder.entity(property.setterIfUnresolved, FUNCTION, designation)
        builder.entity(property.backingFieldIfUnresolved, VARIABLE, designation)

        builder.add(CfirProperty::controlFlowGraphReference, CfirProperty::replaceControlFlowGraphReference)
    }
}

private fun StateKeeperScope<CfirAnonymousInitializer, CfirDesignation>.preserveResolvedState(
    builder: StateKeeperBuilder,
    initializer: CfirAnonymousInitializer
) {
    preservePartialBodyResolveResult(
        builder = builder,
        declaration = initializer,
        bodySupplier = CfirAnonymousInitializer::body,
        parameterSupplier = { emptyList() },
    )
}

private fun StateKeeperScope<CfirFunction, CfirDesignation>.preserveResolvedState(builder: StateKeeperBuilder, function: CfirFunction) {
    preservePartialBodyResolveResult(builder, function, CfirFunction::body, CfirFunction::valueParameters)
}

/**
 * @return the number of analyzed fir statements or null if no partial result is present
 */
private fun <T : CfirDeclaration> StateKeeperScope<T, CfirDesignation>.preservePartialBodyResolveResult(
    builder: StateKeeperBuilder,
    declaration: T,
    bodySupplier: (T) -> CfirBlock?,
    parameterSupplier: (T) -> List<CfirValueParameter>
): Int? {
    val oldBody = bodySupplier(declaration)
    val oldDefaultValues = parameterSupplier(declaration).map { it.defaultValue }

    // No need to check parameters explicitly as they are substituted together with the body
    if (oldBody == null || oldBody is CfirLazyBlock) {
        return null
    }

    val state = declaration.partialBodyAnalysisState ?: return null

    builder.postProcess {
        val newBody = bodySupplier(declaration)
        if (newBody != null && newBody.statements.isNotEmpty()) {
            requireWithAttachment(oldBody.statements.size == newBody.statements.size, { "Bodies do not match" }) {
                withCfirEntry("oldBody", oldBody)
                withCfirEntry("newBody", newBody)
            }

            val newBodyStatements = newBody.statements as MutableList<CfirStatement>
            for (index in 0..<state.analyzedCfirStatementCount) {
                newBodyStatements[index] = oldBody.statements[index]
            }
        }

        // NOTE: parameters might have `evaluatedInitializer` that is not stored in the partial context,
        // but it is not a problem as long as annotation constructors are not partially resolvable
        val newParameters = parameterSupplier(declaration)
        for ((index, newParameter) in newParameters.withIndex()) {
            if (newParameter.defaultValue != null) {
                newParameter.replaceDefaultValue(oldDefaultValues[index])
            }
        }
    }

    return state.analyzedCfirStatementCount
}

private val CfirFunction.isCertainlyResolved: Boolean
    get() {
        if (this is CfirPropertyAccessor) {
            val requiredState = when {
                isSetter -> CfirPropertyBodyResolveState.ALL_BODIES_RESOLVED
                else -> CfirPropertyBodyResolveState.INITIALIZER_AND_GETTER_RESOLVED
            }

            if (propertySymbol.cfir.bodyResolveState >= requiredState) {
                return true
            }
        }

        val body = this.body ?: return false // Not completely sure
        return body !is CfirLazyBlock && body.hasResolvedType
    }

private val CfirVariable.initializerGetterIfUnresolved: KProperty1<CfirVariable, CfirExpression?>?
    get() = CfirVariable::initializer.takeUnless { this is CfirProperty && bodyResolveState >= CfirPropertyBodyResolveState.INITIALIZER_RESOLVED }

private val CfirVariable.delegateGetterIfUnresolved: KProperty1<CfirVariable, CfirExpression?>?
    get() = CfirVariable::delegate.takeUnless { this is CfirProperty && bodyResolveState >= CfirPropertyBodyResolveState.ALL_BODIES_RESOLVED }

private val CfirProperty.backingFieldIfUnresolved: CfirBackingField?
    get() = if (bodyResolveState < CfirPropertyBodyResolveState.INITIALIZER_RESOLVED) getExplicitBackingField() else null

private val CfirProperty.getterIfUnresolved: CfirPropertyAccessor?
    get() = if (bodyResolveState < CfirPropertyBodyResolveState.INITIALIZER_AND_GETTER_RESOLVED) getter else null

private val CfirProperty.setterIfUnresolved: CfirPropertyAccessor?
    get() = if (bodyResolveState < CfirPropertyBodyResolveState.ALL_BODIES_RESOLVED) setter else null

private class LLCfirCodeFragmentContext(
    override val towerDataContext: CfirTowerDataContext,
    override val smartCasts: Map<RealVariable, Set<ConeKotlinType>>,
) : CfirCodeFragmentContext

private val CfirDeclaration.isUsedInFileControlFlowGraphBuilder: Boolean
    get() = this is CfirControlFlowGraphOwner && isUsedInControlFlowGraphBuilderForFile

private val CfirDeclaration.isUsedInClassControlFlowGraphBuilder: Boolean
    get() = this is CfirControlFlowGraphOwner && isUsedInControlFlowGraphBuilderForClass
