package org.cangnova.cangjie.cfir.resolve.body

import com.intellij.openapi.progress.ProgressManager
import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.copy.CfirOwnedGraphCopyResult
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.patterns.CfirPattern
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.patterns.visibleBindingVariables
import org.cangnova.cangjie.cfir.references.CfirControlFlowGraphReference
import org.cangnova.cangjie.cfir.resolve.dfa.*
import org.cangnova.cangjie.cfir.resolve.dfa.cfg.ControlFlowGraph
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportAnalysis
import org.cangnova.cangjie.cfir.resolve.providers.CfirDeclarationExportAnalysis
import org.cangnova.cangjie.cfir.resolve.providers.CfirExportSourcePayload
import org.cangnova.cangjie.cfir.resolve.providers.captureExportSourcePayload
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculatorForFullBodyResolve
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.resolve.dfa.CfirForInLoopExecution
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.source.CfirSourceOperator
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import java.util.Collections
import java.util.IdentityHashMap

/**
 * 解析态拥有图的正式分析重建。Kotlin 没有已解析 FIR replay 入口；这里保留其 resolve→DFA→CFG
 * 职责，按仓颉已完成节点的真实语义顺序重放同一事件。原宿主只提供词法及外域事实，只有新图
 * 接收分析产品；函数/参数类型、binding、source 和 resolve phase 均不重写。
 */
object CfirResolvedGraphAnalysisRebuilder : CfirOwnedGraphAnalysisRebuilder {
    override fun rebuild(copy: CfirOwnedGraphCopyResult, ownerContext: CfirResolvedGraphOwnerContext): CfirCompletedOwnedGraphAnalysis =
        Rebuild(copy, ownerContext).run()

    @OptIn(CfirImplementationDetail::class)
    private class Rebuild(
        private val copy: CfirOwnedGraphCopyResult,
        private val ownerContext: CfirResolvedGraphOwnerContext,
    ) : SessionHolder {
        override val session: CfirSession = ownerContext.session
        private val elements = IdentityHashMap<CfirElement, CfirElement>()
        private val owned = identitySet<CfirElement>()
        private val visited = identitySet<CfirElement>()
        private val functions = identitySet<CfirFunctionBodyOwner>()
        private val graphs = IdentityHashMap<CfirElement, CfirControlFlowGraphReference>()
        private val graphByElement = IdentityHashMap<CfirElement, ControlFlowGraph>()
        private val originalGraphs = IdentityHashMap<CfirControlFlowGraphOwner, CfirControlFlowGraphReference?>()
        private val phases = IdentityHashMap<CfirDeclaration, CfirResolvePhase>()
        private val states = IdentityHashMap<CfirElement, NodeState>()
        private val mapper = object : SnapshotCfirMapper {
            @Suppress("UNCHECKED_CAST")
            override fun <T : CfirElement> mapElement(element: T): T = (elements[element] ?: element) as T
            @Suppress("UNCHECKED_CAST")
            override fun <T : CfirBasedSymbol<*>> mapSymbol(symbol: T): T = copy.getCopiedSymbol(symbol)
        }
        private val context = BodyResolveContext(
            ReturnTypeCalculatorForFullBodyResolve.Default,
            CfirDataFlowAnalyzerContext(variableAssignmentAnalyzer = CfirLocalVariableAssignmentAnalyzer(mapper)),
        )
        private val analyzer = CfirDataFlowAnalyzer(this, context, rebuildingResolvedGraph = true, symbolMapper = mapper::mapSymbol)
        private lateinit var sourceInput: CfirDeclarationExportAnalysis
        private val requiredDeclarations = copy.resolvedDeclarationsRequiringAnalysisRebuild

        init {
            context.dataFlowAnalyzerContext.graphBuilder.completedAnalysisGraph = { declaration -> graphs[mapper.mapElement(declaration)] }
            copy.forEachCopied { original, fresh ->
                elements[original] = fresh
                owned += fresh
                if (original is CfirDeclaration && fresh is CfirDeclaration) {
                    require(fresh.moduleData.session === session) { "Owned analysis cannot cross its module session" }
                    phases[fresh] = fresh.resolvePhase
                }
                if (original is CfirControlFlowGraphOwner && fresh is CfirControlFlowGraphOwner) {
                    originalGraphs[fresh] = original.controlFlowGraphReference
                }
                states[fresh] = NodeState.capture(fresh)
            }
        }

        fun run(): CfirCompletedOwnedGraphAnalysis {
            require(requiredDeclarations.all { it.ownedGraphAnalysisState == CfirOwnedGraphAnalysisState.PENDING }) {
                "Analysis rebuilding requires a fresh pending copy transaction"
            }
            requiredDeclarations.forEach { it.ownedGraphAnalysisState = CfirOwnedGraphAnalysisState.BUILDING }
            try {
                sourceInput = CfirExportAnalysis.analyze(ownerContext.files)
                sourceInput.validateSourceUnchanged()
                ownerContext.files.forEach { visit(it) }
                check(visited.containsAll(copy.resolvedOwnersRequiringAnalysisRebuild)) {
                    "The copied analysis owners are not reachable in their actual lexical context"
                }
                check(visited.containsAll(requiredDeclarations)) { "A required declaration was not analyzed in its actual context" }
                requiredDeclarations.forEach { declaration ->
                    checkNotNull(graphs[declaration]) { "A required declaration has no completed CFG: ${declaration.symbol}" }
                }
                check(context.dataFlowAnalyzerContext.graphBuilder.currentGraphOrNull == null) {
                    "Resolved analysis left an unfinished graph"
                }
                states.forEach { (element, state) -> state.validate(element) }
                phases.forEach { (declaration, phase) -> check(declaration.resolvePhase == phase) }
                originalGraphs.forEach { (owner, previous) ->
                    if (previous != null) checkNotNull(graphs[owner]) { "An actual graph owner was not rebuilt" }
                }
                sourceInput.validateSourceUnchanged()
                graphs.filterKeys { it in owned }.forEach { (_, reference) ->
                    checkNotNull(reference.controlFlowGraph).requireSemanticReadiness(
                        "Full owned analysis requires completed semantic transfer and DFA flow for every actual node",
                        "An actual semantic event has no completed transfer",
                    )
                }
                graphs.forEach { (owner, reference) ->
                    if (owner in owned && owner is CfirControlFlowGraphOwner) owner.replaceControlFlowGraphReference(reference)
                }
                val completedGraphs = graphs.filterKeys { it in owned }
                completedGraphs.forEach { (owner, reference) ->
                    check(checkNotNull(reference.controlFlowGraph).declaration === owner)
                    if (owner is CfirControlFlowGraphOwner) check(reference !== originalGraphs[owner]) { "A rebuilt owner reused its old CFG" }
                }
                val completed = Completed(copy.roots, copy.resolvedOwnersRequiringAnalysisRebuild, session, completedGraphs, graphByElement, states, phases, requiredDeclarations)
                requiredDeclarations.forEach {
                    it.ownedGraphAnalysisReadiness = completed
                    it.ownedGraphAnalysisState = CfirOwnedGraphAnalysisState.COMPLETE
                }
                copy.requireAnalysisReady()
                return completed
            } catch (failure: Throwable) {
                requiredDeclarations.forEach { it.ownedGraphAnalysisState = CfirOwnedGraphAnalysisState.ABORTED }
                throw failure
            }
        }

        private fun visit(original: CfirElement) {
            ProgressManager.checkCanceled()
            val element = mapper.mapElement(original)
            visited += element
            context.dataFlowAnalyzerContext.graphBuilder.currentGraphOrNull?.let { graphByElement[element] = it }
            when (element) {
                is CfirFile -> {
                    analyzer.enterFile(element, buildGraph = true)
                    element.declarations.forEach(::visit)
                    analyzer.exitFile()?.let { graphs[element] = CfirControlFlowGraphReferenceImpl(it) }
                }
                is CfirClass -> {
                    analyzer.enterClass(element, buildGraph = true)
                    element.declarations.forEach(::visit)
                    analyzer.exitClass()?.let { graphs[element] = CfirControlFlowGraphReferenceImpl(it) }
                }
                is CfirFunction -> visitFunction(element)
                is CfirEnumConstructor -> if (element.body != null) {
                    visitFunction(element)
                } else {
                    element.valueParameters.forEach(::visit)
                }
                is CfirCodeFragment -> {
                    analyzer.enterCodeFragment(element)
                    visit(element.block)
                    graphs[element] = CfirControlFlowGraphReferenceImpl(analyzer.exitCodeFragment(element))
                }
                is CfirValueParameter -> {
                    analyzer.enterValueParameter(element)
                    element.defaultValue?.let(::visit)
                    analyzer.exitValueParameter(element)?.let { graphs[element] = CfirControlFlowGraphReferenceImpl(it) }
                }
                is CfirPatternVariable -> {
                    if (element.pattern !is org.cangnova.cangjie.cfir.patterns.CfirWildcardPattern) {
                        analyzer.requirePatternSemanticTransfer(element.pattern)
                    }
                    val bindings = element.pattern.visibleBindingVariables().map(mapper::mapElement)
                    bindings.forEach { binding ->
                        visited += binding
                        context.dataFlowAnalyzerContext.graphBuilder.currentGraphOrNull?.let { graphByElement[binding] = it }
                        analyzer.enterVariableDeclaration(binding)
                    }
                    element.initializer?.let(::visit)
                    visitPatternValues(element.pattern)
                    bindings.forEach(analyzer::exitPatternBindingDeclaration)
                }
                is CfirVariable -> visitVariable(element)
                is CfirProperty -> {
                    element.getter?.let(::visit)
                    element.setter?.let(::visit)
                }
                is CfirClassLikeDeclaration -> element.declarations.forEach(::visit)
                is CfirExtend -> element.declarations.forEach(::visit)
                is CfirExpression -> visitExpression(element)
                is CfirPattern -> visitPattern(element)
                else -> element.acceptChildren(object : CfirVisitorVoid() {
                    override fun visitElement(element: CfirElement) = visit(element)
                })
            }
        }

        private fun visitFunction(function: CfirFunctionBodyOwner) {
            if (!functions.add(function)) return
            // ownerFunc 与词法 outer 分开；helper 先在当前外层流中建立自己的函数域。
            function.valueParameters.mapNotNull { it.defaultValueMetadata?.function }.forEach { helper ->
                val projected = mapper.mapElement(helper)
                if (projected in owned) visit(projected)
            }
            analyzer.enterFunction(function)
            graphByElement[function] = context.dataFlowAnalyzerContext.graphBuilder.currentGraph
            function.valueParameters.forEach(::visit)
            function.body?.let(::visit)
            graphs[function] = checkNotNull(analyzer.exitFunction(function))
        }

        private fun visitVariable(variable: CfirVariable) {
            analyzer.enterVariableDeclaration(variable)
            val fieldGraph = variable is CfirFieldVariable && !variable.isLocal
            if (fieldGraph) analyzer.enterFieldInitializer(variable as CfirFieldVariable)
            variable.initializer?.let(::visit)
            if (fieldGraph) analyzer.exitFieldInitializer()?.let { graphs[variable] = CfirControlFlowGraphReferenceImpl(it) }
            analyzer.exitVariableDeclaration(variable)
        }

        private fun visitPattern(pattern: CfirElement) {
            check(pattern is CfirPattern)
            if (pattern !is org.cangnova.cangjie.cfir.patterns.CfirWildcardPattern) analyzer.requirePatternSemanticTransfer(pattern)
            visitPatternValues(pattern)
            registerPatternBindings(pattern)
        }

        private fun registerPatternBindings(pattern: CfirElement) {
            check(pattern is CfirPattern) { "Element is not a completed pattern" }
            val bindings = pattern.visibleBindingVariables()
            bindings.forEach { binding ->
                val projected = mapper.mapElement(binding)
                visited += projected
                context.dataFlowAnalyzerContext.graphBuilder.currentGraphOrNull?.let { graphByElement[projected] = it }
                analyzer.enterVariableDeclaration(projected)
                // 已完成模式的初始化是每轮运行时绑定的一部分；仅登记符号会漏掉
                // RANGE 的 index 读取与 STRING 的真实下标读取，不能据此发布分析就绪。
                projected.initializer?.let(::visit)
                analyzer.exitPatternBindingDeclaration(projected)
            }
        }

        private fun visitPatternValues(pattern: CfirElement) {
            visited += mapper.mapElement(pattern)
            when (val payload = pattern.captureExportSourcePayload()) {
                is CfirExportSourcePayload.ConstantPattern -> {
                    visit(payload.expression)
                    payload.operatorCall?.let(::visit)
                }
                is CfirExportSourcePayload.ExpressionPattern -> visit(payload.expression)
                is CfirExportSourcePayload.TuplePattern -> payload.elements.forEach(::visitPatternValues)
                is CfirExportSourcePayload.OrPattern -> payload.alternatives.forEach(::visitPatternValues)
                is CfirExportSourcePayload.EnumPattern -> {
                    visit(payload.constructor)
                    payload.arguments.forEach(::visitPatternValues)
                }
                is CfirExportSourcePayload.TypePattern -> visitPatternValues(payload.nested)
                is CfirExportSourcePayload.CatchPattern -> visitPatternValues(payload.nested)
                is CfirExportSourcePayload.CommandPattern -> visitPatternValues(payload.nested)
                is CfirExportSourcePayload.BindingPattern -> payload.nested?.let(::visitPatternValues)
                else -> Unit
            }
        }

        private fun visitAccessReceivers(access: CfirExportSourcePayload.Access) {
            access.explicitReceiver?.let(::visit)
            if (access.dispatchReceiver !== access.explicitReceiver) access.dispatchReceiver?.let(::visit)
        }

        /** 实参阶段只产生闭包值/capture，正文完成在 arguments 出口后按正常调用生命周期调度。 */
        private fun visitCallArgument(original: CfirExpression, deferred: MutableList<CfirAnonymousFunction>) {
            val expression = mapper.mapElement(original)
            if (expression is CfirAnonymousFunctionExpression) {
                visited += expression
                context.dataFlowAnalyzerContext.graphBuilder.currentGraphOrNull?.let { graphByElement[expression] = it }
                analyzer.enterAnonymousFunctionExpression(expression)
                deferred += mapper.mapElement(expression.anonymousFunction)
            } else if (expression is CfirNamedArgumentExpression || expression is CfirInoutArgumentExpression) {
                check(expression is CfirWrappedExpression)
                visited += expression
                context.dataFlowAnalyzerContext.graphBuilder.currentGraphOrNull?.let { graphByElement[expression] = it }
                visitCallArgument(expression.expression, deferred)
                analyzer.exitWrappedExpression(expression)
            } else visit(expression)
        }

        private fun visitExpression(expression: CfirExpression) {
            when (val payload = expression.captureExportSourcePayload()) {
                is CfirExportSourcePayload.Block -> {
                    analyzer.enterBlock(expression as CfirBlock)
                    payload.statements.forEach { slot -> when (slot) {
                        is org.cangnova.cangjie.cfir.resolve.providers.CfirExportSourceStatement.Declaration -> visit(slot.declaration)
                        is org.cangnova.cangjie.cfir.resolve.providers.CfirExportSourceStatement.Expression -> visit(slot.expression)
                    } }
                    analyzer.exitBlock(expression)
                }
                is CfirExportSourcePayload.Call -> {
                    val call = expression as CfirFunctionCall
                    analyzer.enterCallArguments(call, call.argumentList.arguments.map(mapper::mapElement))
                    visitAccessReceivers(payload.access)
                    analyzer.exitCallExplicitReceiver()
                    val deferred = mutableListOf<CfirAnonymousFunction>()
                    call.argumentList.arguments.forEach { visitCallArgument(it, deferred) }
                    analyzer.exitCallArguments()
                    analyzer.enterFunctionCall(call)
                    deferred.forEach(::visit)
                    analyzer.exitFunctionCall(call, callCompleted = true)
                }
                is CfirExportSourcePayload.Access -> {
                    visitAccessReceivers(payload)
                    analyzer.exitQualifiedAccessExpression(expression as CfirQualifiedAccessExpression)
                }
                is CfirExportSourcePayload.Literal -> analyzer.exitLiteralExpression(expression as CfirLiteralExpression)
                is CfirExportSourcePayload.Conditional -> {
                    val conditional = expression as CfirIfExpression
                    analyzer.enterIfExpression(conditional)
                    visit(payload.condition)
                    analyzer.exitIfCondition(conditional)
                    visit(payload.thenBranch)
                    analyzer.exitIfThenBranch(conditional)
                    payload.elseBranch?.let(::visit)
                    analyzer.exitIfExpression(conditional, callCompleted = true)
                }
                is CfirExportSourcePayload.ForIn -> {
                    val loop = expression as CfirForInExpression
                    val execution = CfirForInLoopExecution.read(loop)
                    // 存储容器不作为普通 Block/Match 求值；局部子根复制也必须登记投影后的身份。
                    fun registerStructure(original: CfirElement) {
                        val projected = mapper.mapElement(original)
                        visited += projected
                        graphByElement[projected] = context.dataFlowAnalyzerContext.graphBuilder.currentGraph
                    }
                    registerStructure(payload.iterable)
                    if (execution is CfirForInLoopExecution.Iterator) {
                        registerStructure(execution.selectorHolder)
                        registerStructure(execution.some)
                        registerStructure(execution.some.constructor)
                        execution.some.constructorReference?.let(::registerStructure)
                    }
                    execution.accept(object : CfirForInLoopExecution.Visitor {
                        override fun expression(expression: CfirExpression) = visit(expression)
                        override fun initialize(variable: CfirVariable) = visit(variable)
                        override fun enterLoop(execution: CfirForInLoopExecution) = analyzer.enterForInLoop(loop)
                        override fun enterCondition(execution: CfirForInLoopExecution) = analyzer.enterForInLoopCondition(loop)
                        override fun enterIteration(execution: CfirForInLoopExecution) = analyzer.enterForInLoopIteration(loop)
                        private fun checkOperands(execution: CfirForInLoopExecution) {
                            when (execution) {
                                is CfirForInLoopExecution.Range -> analyzer.exitForInLoopRangeCheck(
                                    mapper.mapElement(execution.comparison), mapper.mapElement(execution.index), mapper.mapElement(execution.bound),
                                )
                                is CfirForInLoopExecution.StringIteration -> analyzer.exitForInLoopStringCheck(
                                    loop, mapper.mapElement(execution.index), mapper.mapElement(execution.bound),
                                )
                                is CfirForInLoopExecution.Iterator -> analyzer.exitForInLoopIteratorCheck(
                                    loop, mapper.mapElement(execution.next),
                                    mapper.mapElement(execution.some.constructorReference as CfirResolvedNamedReference),
                                    mapper.mapSymbol((execution.some.constructorReference as CfirResolvedNamedReference).resolvedSymbol),
                                )
                                is CfirForInLoopExecution.Direct -> Unit
                            }
                        }
                        override fun initialCheck(execution: CfirForInLoopExecution) {
                            if (execution is CfirForInLoopExecution.Range) visit(execution.comparison)
                            else checkOperands(execution)
                            analyzer.exitForInLoopInitialCheck(loop)
                        }
                        override fun check(execution: CfirForInLoopExecution) {
                            checkOperands(execution)
                            analyzer.exitForInLoopIteration(loop)
                        }
                        override fun bindAndBody(execution: CfirForInLoopExecution) {
                            visit(execution.pattern)
                            payload.guard?.let(::visit)
                            analyzer.exitForInLoopGuard(loop)
                            visit(payload.body)
                        }
                        override fun exitBody(execution: CfirForInLoopExecution) = analyzer.exitForInLoopBody(loop)
                        override fun updateString(execution: CfirForInLoopExecution.StringIteration) =
                            analyzer.exitForInLoopStringUpdate(loop, mapper.mapElement(execution.index))
                        override fun exitLoop(execution: CfirForInLoopExecution) = analyzer.exitForInLoop(loop)
                    })
                }
                is CfirExportSourcePayload.Loop -> {
                    val loop = expression as CfirLoopExpression
                    if (payload.isDoWhile) {
                        analyzer.enterDoWhileLoop(loop)
                        visit(payload.body)
                        analyzer.enterDoWhileLoopCondition(loop)
                        visit(payload.condition)
                        analyzer.exitDoWhileLoop(loop)
                    } else {
                        analyzer.enterWhileLoop(loop)
                        visit(payload.condition)
                        analyzer.exitWhileLoopCondition(loop)
                        visit(payload.body)
                        analyzer.exitWhileLoop(loop)
                    }
                }
                is CfirExportSourcePayload.Return -> {
                    analyzer.enterJump(expression as CfirReturnExpression)
                    visit(payload.result)
                    analyzer.exitJump(expression)
                }
                is CfirExportSourcePayload.Jump -> analyzer.exitJump(expression as CfirJump<*>)
                is CfirExportSourcePayload.Lambda -> {
                    analyzer.enterAnonymousFunctionExpression(expression as CfirAnonymousFunctionExpression)
                    visit(payload.function)
                }
                is CfirExportSourcePayload.Assignment -> {
                    visit(payload.left)
                    visit(payload.right)
                    val assignment = expression as CfirAssignment
                    analyzer.recordAssignment(assignment)
                    analyzer.exitVariableAssignment(expression)
                }
                is CfirExportSourcePayload.SourceCall -> {
                    visit(payload.base)
                    payload.arguments.forEach(::visit)
                    analyzer.exitCallExpression(expression as CfirCallExpression)
                }
                is CfirExportSourcePayload.SourceUnary -> {
                    visit(payload.operand)
                    analyzer.exitUnaryExpression(expression as CfirUnaryExpression)
                }
                is CfirExportSourcePayload.SourceBinary -> {
                    val shortCircuit = payload.operation == CfirSourceOperator.LOGICAL_AND || payload.operation == CfirSourceOperator.LOGICAL_OR
                    if (shortCircuit) analyzer.enterBooleanOperatorExpression(expression)
                    visit(payload.left)
                    if (shortCircuit) analyzer.exitLeftBooleanOperatorExpressionArgument(expression)
                    visit(payload.right)
                    if (shortCircuit) analyzer.exitBooleanOperatorExpression(expression)
                    else analyzer.exitBinaryExpression(expression as CfirBinaryExpression)
                }
                is CfirExportSourcePayload.SourceArgument -> payload.value?.let(::visit)
                is CfirExportSourcePayload.Increment -> {
                    visit(payload.expression)
                    analyzer.exitIncrementDecrementExpression(expression as CfirIncrementDecrementExpression)
                }
                is CfirExportSourcePayload.Binary -> when (payload.kind) {
                    CfirBinaryOpKind.AND, CfirBinaryOpKind.OR -> {
                        analyzer.enterBooleanOperatorExpression(expression as CfirBinaryOp)
                        visit(payload.left)
                        analyzer.exitLeftBooleanOperatorExpressionArgument(expression)
                        visit(payload.right)
                        analyzer.exitBooleanOperatorExpression(expression)
                    }
                    CfirBinaryOpKind.COALESCING -> error("Coalescing requires the shared conditional analysis protocol")
                    CfirBinaryOpKind.PIPELINE, CfirBinaryOpKind.COMPOSITION -> error("Flow expressions must complete their actual call lowering before analysis rebuilding")
                }
                is CfirExportSourcePayload.Comparison -> {
                    visit(payload.left); visit(payload.right)
                    analyzer.exitComparisonExpression(expression as CfirComparisonExpression)
                }
                is CfirExportSourcePayload.TypeOperator -> {
                    visit(payload.argument)
                    analyzer.exitTypeOperatorCall(expression as CfirTypeOperator)
                }
                is CfirExportSourcePayload.TypeConversion -> visit(payload.argument)
                is CfirExportSourcePayload.Subscript -> {
                    val getCall = payload.getCall
                    if (getCall != null) visit(getCall) else {
                        visit(payload.receiver)
                        payload.indices.forEach(::visit)
                    }
                }
                is CfirExportSourcePayload.Match -> {
                    val match = expression as CfirMatchExpression
                    analyzer.enterMatchExpression(match)
                    payload.subject?.let(::visit)
                    payload.branches.forEach { branch ->
                        visited += branch
                        analyzer.enterMatchBranchCondition(branch)
                        visitPatternValues(branch.pattern)
                        analyzer.exitMatchBranchCondition(branch) {
                            registerPatternBindings(branch.pattern)
                            branch.guard?.let(::visit)
                        }
                        if (branch.guard == null) registerPatternBindings(branch.pattern)
                        visit(branch.body)
                        analyzer.exitMatchBranchResult(branch)
                    }
                    analyzer.exitMatchExpression(match, analyzer.matchSyntheticElseDecision(match), callCompleted = true)
                }
                is CfirExportSourcePayload.Try -> {
                    val attempt = expression as CfirTryExpression
                    analyzer.enterTryExpression(attempt)
                    payload.resources.forEach(::visit)
                    visit(payload.body)
                    analyzer.exitTryMainBlock()
                    payload.catches.forEach { clause ->
                        visited += clause
                        analyzer.enterCatchClause(clause)
                        visitPattern(clause.pattern)
                        visit(clause.body)
                        analyzer.exitCatchClause(clause)
                    }
                    payload.handlers.forEach { clause ->
                        visited += clause
                        analyzer.enterHandleClause(clause)
                        visitPattern(clause.commandPattern)
                        visit(clause.body)
                        analyzer.exitHandleClause(clause)
                    }
                    payload.finallyBlock?.let {
                        analyzer.enterFinallyBlock(); visit(it); analyzer.exitFinallyBlock()
                    }
                    analyzer.exitTryExpression(callCompleted = true)
                }
                is CfirExportSourcePayload.Throw -> { visit(payload.exception); analyzer.exitThrowException(expression as CfirThrowExpression) }
                is CfirExportSourcePayload.OptionalChain -> {
                    analyzer.enterOptionalChain(expression as CfirOptionalChainExpression)
                    visit(payload.expression)
                    analyzer.exitOptionalChain(expression)
                }
                is CfirExportSourcePayload.NamedArgument -> { visit(payload.expression); analyzer.exitWrappedExpression(expression as CfirWrappedExpression) }
                is CfirExportSourcePayload.InoutArgument -> { visit(payload.expression); analyzer.exitWrappedExpression(expression as CfirWrappedExpression) }
                is CfirExportSourcePayload.Optional -> visit(payload.expression)
                is CfirExportSourcePayload.SmartCast -> visit(payload.original)
                is CfirExportSourcePayload.Unsafe -> {
                    analyzer.enterUnsafeExpression(expression as CfirUnsafeExpression)
                    visit(payload.body)
                    analyzer.exitUnsafeExpression(expression)
                }
                is CfirExportSourcePayload.Synchronized -> {
                    analyzer.enterSynchronizedExpression(expression as CfirSynchronizedExpression)
                    visit(payload.monitor); visit(payload.body)
                    analyzer.exitSynchronizedExpression(expression)
                }
                is CfirExportSourcePayload.Spawn -> {
                    visit(payload.body); payload.contextArgument?.let(::visit)
                    analyzer.exitSpawnExpression(expression as CfirSpawnExpression)
                }
                is CfirExportSourcePayload.ArrayLiteral -> payload.elements.forEach(::visit)
                is CfirExportSourcePayload.TupleLiteral -> {
                    val tuple = expression as CfirTupleLiteral
                    analyzer.enterTupleLiteral(tuple)
                    payload.elements.forEachIndexed { index, element ->
                        visit(element)
                        analyzer.exitTupleLiteralElement(tuple, index, mapper.mapElement(element))
                    }
                    analyzer.exitTupleLiteral(tuple)
                }
                is CfirExportSourcePayload.Interpolation -> payload.parts.forEach(::visit)
                is CfirExportSourcePayload.Quote -> payload.interpolations.forEach(::visit)
                is CfirExportSourcePayload.Range -> { visit(payload.start); visit(payload.end); payload.step?.let(::visit) }
                is CfirExportSourcePayload.LetPattern -> { visit(payload.initializer); visitPattern(payload.pattern) }
                is CfirExportSourcePayload.Perform -> visit(payload.expression)
                is CfirExportSourcePayload.Resume -> { payload.value?.let(::visit); payload.throwing?.let(::visit) }
                is CfirExportSourcePayload.Error -> { payload.expression?.let(::visit); payload.otherElement?.let(::visit) }
                is CfirExportSourcePayload.Constant, is CfirExportSourcePayload.ThisReceiver, is CfirExportSourcePayload.Annotation -> Unit
                is CfirExportSourcePayload.AugmentedAssignment -> error("Augmented assignment must complete its actual writeback lowering before analysis rebuilding")
                is CfirExportSourcePayload.Availability -> error("Availability requires its shared conditional analysis protocol")
                else -> error("Resolved expression has no complete analysis event protocol: ${expression::class.qualifiedName}")
            }
        }
    }

    @OptIn(CfirImplementationDetail::class)
    private data class NodeState(
        val source: org.cangnova.cangjie.source.CjSourceElement?,
        val type: ConeCangJieType?,
        val resolvedType: ConeCangJieType?,
        val children: List<CfirElement>,
        val payload: CfirExportSourcePayload?,
    ) {
        fun validate(element: CfirElement) {
            val current = capture(element)
            check(source === current.source && type === current.type && resolvedType === current.resolvedType &&
                children.size == current.children.size && children.zip(current.children).all { (a, b) -> a === b } &&
                payload == current.payload) { "Analysis rebuilding changed completed semantic input: ${element::class.qualifiedName}" }
        }

        companion object {
            fun capture(element: CfirElement): NodeState {
                val children = mutableListOf<CfirElement>()
                element.acceptSourceChildren(object : CfirVisitorVoid() {
                    override fun visitElement(element: CfirElement) {
                        if (element !is CfirControlFlowGraphReference) children += element
                    }
                })
                return NodeState(element.source, (element as? CfirExpression)?.coneTypeOrNull,
                    (element as? CfirResolvedTypeRef)?.coneType, children.toList(), element.captureExportSourcePayload())
            }
        }
    }

    private class Completed(
        roots: List<CfirElement>,
        owners: List<CfirControlFlowGraphOwner>,
        private val session: CfirSession,
        graphs: Map<CfirElement, CfirControlFlowGraphReference>,
        graphByElement: Map<CfirElement, ControlFlowGraph>,
        states: Map<CfirElement, NodeState>,
        phases: Map<CfirDeclaration, CfirResolvePhase>,
        private val requiredDeclarations: List<CfirDeclaration>,
    ) : CfirCompletedOwnedGraphAnalysis {
        override val roots: List<CfirElement> = Collections.unmodifiableList(roots.toList())
        override val requiredOwners: List<CfirControlFlowGraphOwner> = Collections.unmodifiableList(owners.toList())
        private val graphs = IdentityHashMap(graphs)
        private val graphByElement = IdentityHashMap(graphByElement)
        private val states = IdentityHashMap(states)
        private val phases = IdentityHashMap(phases)

        override fun requireReady(root: CfirElement, session: CfirSession) {
            check(this.session === session && states.containsKey(root)) { "Completed analysis belongs to another root/session" }
            requiredDeclarations.forEach { declaration ->
                check(declaration.ownedGraphAnalysisState == CfirOwnedGraphAnalysisState.COMPLETE)
            }
            graphs.forEach { (owner, reference) -> if (owner is CfirControlFlowGraphOwner) check(owner.controlFlowGraphReference === reference) }
            graphs.values.forEach { reference ->
                checkNotNull(reference.controlFlowGraph).requireSemanticReadiness(
                    "Owned analysis semantic capability is incomplete", "Owned semantic events are incomplete",
                )
            }
            states.forEach { (element, state) -> state.validate(element) }
            phases.forEach { (declaration, phase) -> check(declaration.resolvePhase == phase) }
        }

        override fun graphOf(root: CfirElement): CfirControlFlowGraphReference {
            requireReady(root, session)
            return graphs[root] ?: CfirControlFlowGraphReferenceImpl(graphByElement.getValue(root))
        }
    }

    /** 两个正式入口共享原因优先级；全部节点能力通过后才检查剩余事件义务。 */
    private fun ControlFlowGraph.requireSemanticReadiness(nodeMessage: String, eventMessage: String) {
        check(nodes.all {
            it.flowInitialized && it.hasCompletedRequiredAlternateFlows() &&
                it.dataFlowAnalysisState == org.cangnova.cangjie.cfir.resolve.dfa.cfg.DataFlowAnalysisState.SEMANTICS_READY
        }) { nodeMessage }
        check(!hasPendingSemanticTransfers) { eventMessage }
    }

    private fun projectPattern(declaration: CfirDeclaration): List<CfirDeclaration> =
        if (declaration is CfirPatternVariable) declaration.pattern.bindingVariables() else listOf(declaration)

    private fun <T> identitySet(): MutableSet<T> = Collections.newSetFromMap(IdentityHashMap<T, Boolean>())
}
