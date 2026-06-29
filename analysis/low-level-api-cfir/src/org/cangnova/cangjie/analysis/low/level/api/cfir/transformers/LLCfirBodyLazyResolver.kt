/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.transformers

import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiElement
import com.intellij.psi.util.descendantsOfType
import kotlinx.collections.immutable.toPersistentList
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
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.resolve.CfirCodeFragmentContext
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.resolve.dfa.CfirControlFlowGraphReferenceImpl
import org.cangnova.cangjie.cfir.resolve.dfa.controlFlowGraph
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.CfgInternals
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraph
import org.cangnova.cangjie.cfir.resolve.body.CfirAbstractBodyResolveTransformerDispatcher
import org.cangnova.cangjie.cfir.resolve.body.CfirDataFlowAnalyzerContext
import org.cangnova.cangjie.cfir.resolve.body.CfirDeclarationsResolveTransformer
import org.cangnova.cangjie.cfir.resolve.body.CfirExpressionsResolveTransformer
import org.cangnova.cangjie.cfir.resolve.body.CfirTowerDataElement
import org.cangnova.cangjie.cfir.resolve.body.CfirTowerDataContext
import org.cangnova.cangjie.cfir.resolve.body.SnapshotCfirMapper
import org.cangnova.cangjie.cfir.resolve.codeFragmentContext
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.*
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirTypeParameterScopeImpl
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.types.hasResolvedType
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.source.CjRealSourceElementKind
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.utils.exceptions.checkWithAttachment
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment
import org.cangnova.cangjie.utils.exceptions.withPsiEntry
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.reflect.KProperty1

/**
 * BODY_RESOLVE 阶段的低阶懒解析入口。
 */
internal object LLCfirBodyLazyResolver : LLCfirLazyResolver(CfirResolvePhase.BODY_RESOLVE) {
    /**
     * 为 [target] 创建 BODY_RESOLVE 阶段目标解析器。
     */
    override fun createTargetResolver(target: LLCfirResolveTarget): LLCfirTargetResolver = LLCfirBodyTargetResolver(target)

    /**
     * 校验 body 相关结构已经完成解析。
     */
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
    /**
     * 解析函数内容；如果函数已有局部 body 分析状态，则只继续转换 body。
     */
    override fun transformFunctionContent(
        function: CfirFunction,
        resolutionModeForBody: ResolutionMode,
        shouldResolveEverything: Boolean,
    ): CfirFunction {
        if (function.partialBodyAnalysisState != null) {
            function.transformBody(this, resolutionModeForBody)
            return function
        }

        return super.transformFunctionContent(function, resolutionModeForBody, shouldResolveEverything)
    }

    /**
     * 解析构造器内容；如果构造器已有局部 body 分析状态，则只继续转换 body。
     */
    override fun transformConstructorContent(constructor: CfirConstructor, data: ResolutionMode): CfirConstructor {
        if (constructor.partialBodyAnalysisState != null) {
            constructor.transformBody(this, data)
            return constructor
        }

        return super.transformConstructorContent(constructor, data)
    }
}

/**
 * 支持局部 body 分析的表达式解析 transformer。
 *
 * 当目标是 [LLCfirPartialBodyResolveTarget] 时，它只解析到请求的 stop element 前，并保存 tower/data-flow 快照供后续继续分析。
 */
private class CfirPartialBodyExpressionResolveTransformer(
    transformer: CfirAbstractBodyResolveTransformerDispatcher,
    /**
     * 当前 body 解析请求目标。
     */
    private val target: LLCfirResolveTarget
) : CfirExpressionsResolveTransformer(transformer) {
    /**
     * 局部 body 分析相关常量。
     */
    private companion object {
        // After a certain number of partial analyses,
        // trigger the full analysis so we don't return to the same declaration over and over again.
        // Note that the first analysis can also perform only default parameter value analysis and exit just after it.
        /**
         * 单个声明允许重复局部分析的最大次数。
         */
        private val MAX_ANALYSES_COUNT: Int by lazy(LazyThreadSafetyMode.PUBLICATION) {
            // On various repositories, number of declarations analyzed more than five times, is under 1%.
            // So here we cap only unusually lengthy declarations.
            Registry.intValue("cangjie.analysis.partialBodyAnalysis.attemptCount", 5)
        }
    }

    /**
     * 标记当前是否已经处于顶层 block 分析过程中。
     */
    private var isInsideAnalysis = false

    /**
     * 转换 block，并在可局部分析时选择局部或完整 body 解析路径。
     */
    override fun transformBlock(block: CfirBlock, data: ResolutionMode): CfirExpression {
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

    /**
     * 以“最外层 block 分析”身份执行 [block]，避免嵌套 block 重复触发局部分析。
     */
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

    /**
     * 从已有局部分析状态继续解析 [block]，或首次解析到请求的 stop element。
     */
    @OptIn(CfgInternals::class)
    private fun transformPartially(
        request: LLPartialBodyResolveRequest,
        block: CfirBlock,
        data: ResolutionMode,
        state: LLPartialBodyAnalysisState?
    ): CfirExpression {
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
            val dataFlowSnapshot = resolveSnapshot.dataFlowAnalyzerContext.createSnapshot(
                LLSnapshotCfirMapper(listOf(block, declaration))
            )
            patchControlFlowGraphReferences(listOf(block, declaration), dataFlowSnapshot.graphMapping)
            context.dataFlowAnalyzerContext.resetFrom(dataFlowSnapshot.context)

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

    /**
     * 将旧 CFG 快照中的 CFIR 元素映射到当前 body 中的对应元素。
     */
    @CfgInternals
    private class LLSnapshotCfirMapper(private val roots: List<CfirElement>) : SnapshotCfirMapper {
        /**
         * 判断 [element] 是否需要参与 CFG 快照映射。
         */
        private fun shouldBeHandled(element: CfirElement): Boolean {
            /** Accepts elements handled by [org.cangnova.cangjie.cfir.resolve.dfa.CfirLocalVariableAssignmentAnalyzer] */
            val isElementKindHandled = when (element) {
                is CfirCallableDeclaration -> element.isLocal
                is CfirLoopExpression -> true
                else -> false
            }

            return isElementKindHandled && element.source?.kind == CjRealSourceElementKind
        }

        /**
         * PSI 到当前 CFIR 元素的映射表。
         */
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

        /**
         * 把旧快照中的 [element] 映射到当前 body 中的对应 CFIR 元素。
         */
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

        /**
         * 把旧快照中的 [symbol] 映射到当前 body 中的对应符号。
         */
        override fun <T : CfirBasedSymbol<*>> mapSymbol(symbol: T): T {
            @Suppress("UNCHECKED_CAST")
            return mapElement(symbol.cfir).symbol as T
        }
    }

    /**
     * 将已分析 [CfirElement] 中指向旧 [ControlFlowGraph] 的引用替换为新快照中的图。
     *
     * 客户端只能通过 [LLPartialBodyAnalysisSnapshot] 访问 CFG 节点，因此这里不需要额外加锁。
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

    /**
     * 从 [startIndex] 开始局部转换 [block] 中的语句，直到到达 stop element 或完整解析结束。
     */
    private fun transformStatementsPartially(
        request: LLPartialBodyResolveRequest,
        block: CfirBlock,
        data: ResolutionMode,
        startIndex: Int,
        performedAnalysesCount: Int,
    ): Boolean {
        val declaration = request.target

        val stopElement = request.stopElement
        val stopElements = stopElement?.descendantsOfType<CjElement>(childrenFirst = false)?.toHashSet().orEmpty()

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

    /**
     * 发布 [request] 对应的局部 body 分析状态。
     */
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

    /**
     * 收集函数参数默认值，用于局部 body 分析结果。
     */
    private fun collectDefaultParameterValues(declaration: CfirDeclaration): List<CfirExpression> {
        if (declaration is CfirFunction) {
            val result = declaration.valueParameters.mapNotNull { it.defaultValue }
            return result.ifEmpty { emptyList() }
        }

        return emptyList()
    }

    /**
     * 完整解析声明 body。
     *
     * 如果 body 已经存在局部分析状态，[transformFully] 仍会委托 [transformPartially] 从已有状态继续到结尾。
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

    /**
     * 判断 [element] 是否应该在到达 [stopElement] 前继续转换。
     */
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
 * BODY_RESOLVE 阶段的目标解析器。
 *
 * 该解析器负责转换声明 body 并构建 [ControlFlowGraph]。为避免取消异常或局部分析中断留下损坏状态，解析前会通过
 * [BodyStateKeepers] 保存并在失败时恢复 body、默认值、CFG 引用和局部分析状态。
 *
 * 文件和 class 有额外规则：用于构建 CFG 的成员必须先于容器本身完成 BODY_RESOLVE，之后容器才能生成正确 CFG。
 *
 * @see BodyStateKeepers
 * @see CfirResolvePhase.BODY_RESOLVE
 */
private class LLCfirBodyTargetResolver(target: LLCfirResolveTarget) : LLCfirAbstractBodyTargetResolver(target, CfirResolvePhase.BODY_RESOLVE) {
    /**
     * BODY_RESOLVE 阶段使用的 dispatcher。
     */
    override val transformer = BodyTransformerDispatcher()

    /**
     * low-level body 解析 dispatcher，接入局部 body 分析专用的声明和表达式 transformer。
     */
    inner class BodyTransformerDispatcher : CfirAbstractBodyResolveTransformerDispatcher(resolverPhase, false) {
        /**
         * body 解析上下文，持有返回类型计算器和数据流分析上下文。
         */
        override val context: BodyResolveContext = BodyResolveContext(
            returnTypeCalculator = createReturnTypeCalculator(),
            dataFlowAnalyzerContext = CfirDataFlowAnalyzerContext(),
        )

        /**
         * body 解析组件集合。
         */
        override val components: BodyResolveTransformerComponents =
            BodyResolveTransformerComponents(
                session = resolveTargetSession,
                scopeSession = resolveTargetScopeSession,
                transformer = this,
                context = context,
                expandTypeAliases = true,
            )

        /**
         * 表达式 transformer，支持局部 body 分析中断和恢复。
         */
        override val expressionsTransformer: CfirExpressionsResolveTransformer =
            CfirPartialBodyExpressionResolveTransformer(this, resolveTarget)

        /**
         * 声明 transformer，支持已有局部 body 分析状态的快速路径。
         */
        override val declarationsTransformer: CfirDeclarationsResolveTransformer =
            CfirPartialBodyDeclarationResolveTransformer(this)

        /**
         * low-level body 解析不复用类 CFG。
         */
        override val preserveCFGForClasses: Boolean get() = false
        /**
         * 文件 CFG 由 [LLCfirBodyTargetResolver] 显式构建。
         */
        override val buildCfgForFiles: Boolean get() = false
    }

    /**
     * BODY_RESOLVE 不应成为其他声明解析的依赖，因此跳过通用依赖解析步骤。
     */
    override val skipDependencyTargetResolutionStep: Boolean get() = true

    /**
     * 在无目标锁阶段处理文件、class 和代码片段这类需要自定义锁内流程的目标。
     */
    override fun doResolveWithoutLock(target: CfirElementWithResolveState): Boolean {
        when (target) {
            is CfirClass -> {
                if (checkAnalysisReadiness(target, containingDeclarations, resolverPhase)) return true

                // resolve class CFG graph here, to do this we need to have property & init blocks resoled
                resolveMembersForControlFlowGraph(
                    declarationWithMembers = target,
                    withDeclaration = this::withClass,
                    declarationsProvider = CfirClass::declarations,
                    isUsedInControlFlowBuilder = CfirDeclaration::isUsedInClassControlFlowGraphBuilder,
                )

                performCustomResolveUnderLock(target) {
                    calculateControlFlowGraph(target)
                }

                return true
            }

            is CfirClassLikeDeclaration -> {
                if (checkAnalysisReadiness(target, containingDeclarations, resolverPhase)) return true

                performCustomResolveUnderLock(target) {
                    // 非 regular class 的 class-like 容器没有额外 CFG 入口；
                    // 这里仅推进容器自身 phase，成员仍作为独立 target 按既有路径完成 BODY_RESOLVE。
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

    /**
     * 为 [target] class 构建并写入 CFG 引用。
     */
    private fun calculateControlFlowGraph(target: CfirClass) {
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

    /**
     * 在构建容器 CFG 前，先解析参与 CFG 构建的成员声明。
     */
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

    /**
     * 为 [target] 文件构建并写入 CFG 引用。
     */
    private fun calculateControlFlowGraph(target: CfirFile) {
        checkWithAttachment(
            target.controlFlowGraphReference == null,
            { "'controlFlowGraphReference' should be 'null' if the file phase < $resolverPhase)" },
        ) {
            withCfirEntry("cfirFile", target)
        }

        val dataFlowAnalyzer = transformer.declarationsTransformer.dataFlowAnalyzer
        dataFlowAnalyzer.enterFile(target, buildGraph = true)
        val controlFlowGraph = dataFlowAnalyzer.exitFile()
            ?: errorWithAttachment("CFG should not be 'null' as 'buildGraph' is specified") {
                withCfirEntry("cfirFile", target)
            }

        target.replaceControlFlowGraphReference(CfirControlFlowGraphReferenceImpl(controlFlowGraph))
    }

    /**
     * 为 [cfirCodeFragment] 构建 code fragment body 解析上下文。
     */
    private fun resolveCodeFragmentContext(cfirCodeFragment: CfirCodeFragment): LLCfirCodeFragmentContext {
        val cjCodeFragment = cfirCodeFragment.psi as? CjCodeFragment
            ?: errorWithAttachment("Code fragment source not found") {
                withCfirEntry("cfirCodeFragment", cfirCodeFragment)
            }

        val module = cfirCodeFragment.llCfirModuleData.caModule
        val resolutionFacade = module.getResolutionFacade(cjCodeFragment.project)

        fun CfirTowerDataContext.withExtraScopes(): CfirTowerDataContext {
            return resolutionFacade.useSiteCfirSession.codeFragmentScopeProvider.getExtraScopes(cjCodeFragment)
                .fold(this) { context, scope ->
                    val scopeWithProperSession = scope.withReplacedSessionOrNull(resolveTargetSession, resolveTargetScopeSession) ?: scope
                    val localScope = scopeWithProperSession as? CfirLocalScope
                    if (localScope != null) {
                        context.addLocalScope(localScope)
                    } else {
                        context.addNonLocalScope(scopeWithProperSession)
                    }
                }
        }

        val contextPsiElement = cjCodeFragment.context
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
                    .withExtraScopes()
            )
        } else {
            val towerDataContext = CfirTowerDataContext().withExtraScopes()
            LLCfirCodeFragmentContext(towerDataContext)
        }
    }

    /**
     * 将 tower data context 中的 scope 与 receiver 替换到指定 [session]/[scopeSession]。
     */
    private fun CfirTowerDataContext.withProperSession(session: CfirSession, scopeSession: ScopeSession): CfirTowerDataContext {
        return replaceTowerDataElements(
            towerDataElements.map { it.withProperSession(session, scopeSession) }.toPersistentList(),
            nonLocalTowerDataElements.map { it.withProperSession(session, scopeSession) }.toPersistentList(),
        )
    }

    /**
     * 将单个 tower data element 的 scope 与 implicit receiver 替换到指定 [session]/[scopeSession]。
     */
    private fun CfirTowerDataElement.withProperSession(
        session: CfirSession,
        scopeSession: ScopeSession,
    ): CfirTowerDataElement = CfirTowerDataElement(
        scope?.withReplacedSessionOrNull(session, scopeSession) ?: scope,
        implicitReceiver?.withReplacedSessionOrNull(session, scopeSession),
        isLocal,
        staticScopeOwnerSymbol
    )

    /**
     * 在目标锁内解析拥有 body 的声明。
     */
    override fun doLazyResolveUnderLock(target: CfirElementWithResolveState) {
        // There is no sense to resolve such declarations as they do not have bodies
        // Also, they have STUB expression instead of default values, so we shouldn't change them
        if (target is CfirCallableDeclaration && target.canHaveDeferredReturnTypeCalculation) return

        when (target) {
            is CfirFile, is CfirClassLikeDeclaration, is CfirCodeFragment -> error("Should have been resolved in ${::doResolveWithoutLock.name}")
            is CfirExtend -> {
                // extend 自身不拥有独立 body，成员会作为独立目标继续推进到 BODY_RESOLVE。
            }
            is CfirConstructor -> resolve(target, BodyStateKeepers.CONSTRUCTOR)
            is CfirFunction -> resolve(target, BodyStateKeepers.FUNCTION)
            is CfirProperty -> resolve(target, BodyStateKeepers.PROPERTY)
            is CfirFieldVariable -> resolve(target, BodyStateKeepers.FIELD)
            is CfirVariable -> resolve(target, BodyStateKeepers.VARIABLE)
            is CfirTypeAlias,
                -> {
                // No bodies here
            }
            else -> throwUnexpectedCfirElementError(target)
        }
    }

    /**
     * 执行 raw body 解析，并在完整或局部成功时通知声明修改服务。
     */
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

/**
 * BODY_RESOLVE 阶段使用的状态保持器集合。
 */
internal object BodyStateKeepers {
    /**
     * code fragment block 状态保持器。
     */
    val CODE_FRAGMENT: StateKeeper<CfirCodeFragment, CfirDesignation> = stateKeeper { builder, _, _ ->
        builder.add(CfirCodeFragment::block, CfirCodeFragment::replaceBlock, ::blockGuard)
    }

    /**
     * 可局部 body 分析声明的局部分析状态保持器。
     */
    val PARTIAL_BODY_RESOLVABLE: StateKeeper<CfirDeclaration, CfirDesignation> = stateKeeper { builder, declaration, context ->
        builder.add(CfirDeclaration::partialBodyAnalysisState::get, CfirDeclaration::partialBodyAnalysisState::set)
    }

    /**
     * 函数 body、返回类型、参数默认值和 CFG 引用状态保持器。
     */
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

    /**
     * 构造器状态保持器，复用函数状态规则。
     */
    val CONSTRUCTOR: StateKeeper<CfirConstructor, CfirDesignation> = stateKeeper { builder, _, designation ->
        builder.add(FUNCTION, designation)
    }

    /**
     * 变量返回类型与 initializer 状态保持器。
     */
    val VARIABLE: StateKeeper<CfirVariable, CfirDesignation> = stateKeeper { builder, variable, _ ->
        builder.add(CfirVariable::returnTypeRef, CfirVariable::replaceReturnTypeRef)

        if (!isCallableWithSpecialBody(variable)) {
            variable.initializerGetterIfUnresolved?.let {
                builder.add(it, CfirVariable::replaceInitializer, ::expressionGuard)
            }
        }
    }

    /**
     * 值参数默认值、求值 initializer 和 CFG 引用状态保持器。
     */
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

    /**
     * 字段变量状态保持器，复用变量状态规则。
     */
    val FIELD: StateKeeper<CfirFieldVariable, CfirDesignation> = stateKeeper { builder, _, designation ->
        builder.add(VARIABLE, designation)
    }

    /**
     * 属性 body 解析状态、返回类型、访问器和 CFG 引用状态保持器。
     */
    val PROPERTY: StateKeeper<CfirProperty, CfirDesignation> = stateKeeper { builder, property, designation ->
        if (property.bodyResolveState >= CfirPropertyBodyResolveState.ALL_BODIES_RESOLVED) {
            return@stateKeeper
        }

        builder.add(CfirProperty::bodyResolveState, CfirProperty::replaceBodyResolveState)
        builder.add(CfirProperty::returnTypeRef, CfirProperty::replaceReturnTypeRef)

        builder.entity(property.getterIfUnresolved, FUNCTION, designation)
        builder.entity(property.setterIfUnresolved, FUNCTION, designation)

        builder.add(CfirProperty::controlFlowGraphReference, CfirProperty::replaceControlFlowGraphReference)
    }
}

/**
 * 保存函数已解析 body 的局部分析结果，供状态恢复时拼回新 body。
 */
private fun StateKeeperScope<CfirFunction, CfirDesignation>.preserveResolvedState(builder: StateKeeperBuilder, function: CfirFunction) {
    preservePartialBodyResolveResult(builder, function, CfirFunction::body, CfirFunction::valueParameters)
}

/**
 * 保存 [declaration] 的局部 body 分析结果。
 *
 * @return 已分析 CFIR 语句数量；没有局部结果时返回 `null`。
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
                newBodyStatements[index] = oldBody.statements[index] as CfirStatement
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

/**
 * 判断函数 body 是否已经确定完成解析。
 */
private val CfirFunction.isCertainlyResolved: Boolean
    get() {
        if (this is CfirPropertyAccessor) {
            val requiredState = when {
                !isGetter -> CfirPropertyBodyResolveState.ALL_BODIES_RESOLVED
                else -> CfirPropertyBodyResolveState.INITIALIZER_AND_GETTER_RESOLVED
            }

            if (propertySymbol.cfir.bodyResolveState >= requiredState) {
                return true
            }
        }

        val body = this.body ?: return false // Not completely sure
        return body !is CfirLazyBlock && body.hasResolvedType
    }

/**
 * 返回尚未解析 initializer 的变量 initializer 属性访问器。
 */
private val CfirVariable.initializerGetterIfUnresolved: KProperty1<CfirVariable, CfirExpression?>?
    get() = CfirVariable::initializer.takeUnless { this is CfirProperty && bodyResolveState >= CfirPropertyBodyResolveState.INITIALIZER_RESOLVED }

/**
 * 返回尚未解析 getter body 的属性访问器。
 */
private val CfirProperty.getterIfUnresolved: CfirPropertyAccessor?
    get() = if (bodyResolveState < CfirPropertyBodyResolveState.INITIALIZER_AND_GETTER_RESOLVED) getter else null

/**
 * 返回尚未解析 setter body 的属性访问器。
 */
private val CfirProperty.setterIfUnresolved: CfirPropertyAccessor?
    get() = if (bodyResolveState < CfirPropertyBodyResolveState.ALL_BODIES_RESOLVED) setter else null

/**
 * code fragment body 解析使用的上下文实现。
 */
private class LLCfirCodeFragmentContext(
    /**
     * code fragment 可见的 tower data context。
     */
    override val towerDataContext: CfirTowerDataContext,
) : CfirCodeFragmentContext

/**
 * 判断声明是否会参与文件级 CFG 构建。
 */
private val CfirDeclaration.isUsedInFileControlFlowGraphBuilder: Boolean
    get() = this is CfirControlFlowGraphOwner

/**
 * 判断声明是否会参与 class 级 CFG 构建。
 */
private val CfirDeclaration.isUsedInClassControlFlowGraphBuilder: Boolean
    get() = this is CfirControlFlowGraphOwner
