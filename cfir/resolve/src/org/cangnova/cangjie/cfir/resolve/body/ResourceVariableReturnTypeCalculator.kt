package org.cangnova.cangjie.cfir.resolve.body

import java.util.IdentityHashMap
import org.cangnova.cangjie.cfir.DeepCopyCfirTreeWithSymbols
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirTryExpression
import org.cangnova.cangjie.cfir.resolve.ResolutionMode
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallInfo
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CallKind
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.inference.*
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.transformers.ReturnTypeCalculator
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.BodyResolveContext
import org.cangnova.cangjie.cfir.scopes.CallableCopyTypeCalculator
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.resolve.calls.inference.model.UnstableSystemMergeMode
import org.cangnova.cangjie.resolve.calls.tasks.ExplicitReceiverKind
import org.cangnova.cangjie.type.model.TypeConstructorMarker

/**
 * 同一 try 作用域资源的按需类型计算器。
 *
 * 官方 SynTargetOnUsed 提前综合后序资源的类型，初始化检查仍按源码顺序进行。
 * 词法入口只读保存；每次依赖计算借用当前 active parent 的推断变量，复制完整 RAW
 * initializer，并在导出类型之前移交未完成的约束、调用图和写回任务。
 */
@OptIn(org.cangnova.cangjie.util.PrivateForInline::class)
internal class ResourceVariableReturnTypeCalculator(
    private val components: CfirAbstractBodyResolveTransformer.BodyResolveTransformerComponents,
    private val resources: List<CfirResourceVariable>,
    private val tryExpression: CfirTryExpression,
    private val resourceType: ConeCangJieType,
) : ReturnTypeCalculator() {
    private val delegate = components.context.returnTypeCalculator
    private val resourceSymbols = resources.mapTo(hashSetOf()) { it.symbol }
    private val computationSession = ImplicitBodyResolveComputationSession().also { cache ->
        resources.filter { it.resourceSourceCompletion?.let { task -> task.isCurrent && task.isCompleted } != true }.forEach { resource ->
            resource.resourceSourceCache = cache
        }
    }
    private val preparationContext = components.context.createLocalDeclarationContext(this)
    private val expressionContext = components.transformer.expressionsTransformer.captureLocalDeclarationExpressionContext()
    private val enclosingScope = components.context.implicitTypeComputationScope
    private val graphs = enclosingScope?.graph?.registry ?: IdentityHashMap<CfirResourceVariable, CfirImplicitTypeComputation>()
    /** ReturnTypeCalculator 的跳转共享此对象；当前 transformer 的 context 必须沿真实调用栈传递。 */
    private val activeContexts = ArrayDeque<BodyResolveContext>().apply { addLast(components.context) }

    override val callableCopyTypeCalculator: CallableCopyTypeCalculator
        get() = delegate.callableCopyTypeCalculator

    private fun canonicalResource(resource: CfirResourceVariable): CfirResourceVariable =
        if (enclosingScope == null) resource else enclosingScope.canonicalElement(resource) as CfirResourceVariable

    override fun tryCalculateReturnTypeOrNull(declaration: CfirCallableDeclaration): CfirResolvedTypeRef? {
        if (declaration !is CfirResourceVariable || declaration.symbol !in resourceSymbols) {
            return delegate.tryCalculateReturnTypeOrNull(declaration)
        }
        (declaration.returnTypeRef as? CfirResolvedTypeRef)?.let { return it }
        graphs[canonicalResource(declaration)]?.resultTypeRef?.let { return it }
        return when (val status = computationSession.getStatus(declaration.symbol)) {
            is CfirImplicitBodyResolveComputationStatus.Computed -> status.resolvedTypeRef
            is CfirImplicitBodyResolveComputationStatus.ComputedType -> status.resolvedTypeRef
            is CfirImplicitBodyResolveComputationStatus.Computing -> recursiveResourceType(declaration)
            is CfirImplicitBodyResolveComputationStatus.NotComputed -> computationSession.computeType(declaration.symbol) {
                computeResourceType(declaration)
            }
        }
    }

    /** source preview 只进入 CFG 和真实类型检查；PCLA 的 final writer 完成后才发布并退役。 */
    fun resolveResource(resource: CfirResourceVariable, transformation: () -> CfirResourceVariable): CfirResourceVariable {
        resource.resourceSourceCompletion?.takeIf { it.isCurrent && it.isCompleted }?.let { return resource }
        val graph = graphs[canonicalResource(resource)]
        val targetScope = components.context.implicitTypeComputationScope
        val resolved = if (graph == null) transformation() else {
            graph.projectOriginsToSource(targetScope)
            components.context.withImplicitTypeComputationScope(
                CfirImplicitTypeComputationScope(graph, CfirImplicitTypeComputationMode.FORMAL, targetScope), transformation,
            )
        }
        val task = CfirResourceSourceCompletion(resolved, tryExpression, resourceType, computationSession, graph, targetScope)
        resolved.resourceSourceCompletion?.takeIf { !it.isCompleted }?.cancel()
        resolved.resourceSourceCompletion = task
        graph?.bindFormalResourceSource(task)
        val parent = activeContexts.last().inferenceSession as? CfirPCLAInferenceSession
        val actualSystem = parent?.currentCommonSystem ?: graph?.constraintSystemOwner
        // 确定错误仍在正文之前发布；TV 只加入 actual PCLA constraints，绝不写伪错误类型。
        checkResourceSpecification(resolved, resourceType, components.session, actualSystem)
        computationSession.previewResourceSource(resolved)
        if (parent != null) parent.registerResourceSourceCompletion(task) else {
            val substitutor = actualSystem?.buildCurrentSubstitutor()?.asCone() ?: ConeSubstitutor.Empty
            task.complete(actualSystem, substitutor)
            task.retireAfterSourceCompletion()
        }
        return resolved
    }

    @OptIn(UnstableSystemMergeMode::class)
    private fun computeResourceType(resource: CfirResourceVariable): CfirResolvedTypeRef {
        val sourceInitializer = resource.initializer
            ?: return components.transformer.declarationsTransformer.inferVariableTypeFromInitializer(resource, null)
        val activeInference = activeContexts.last().inferenceSession
        val parent = activeInference as? CfirPCLAInferenceSession
        val parentBaseline = parent?.currentCommonSystem?.captureBaseline()
        val incoming = parentBaseline?.storage?.allTypeVariables?.keys?.toSet().orEmpty()
        val incomingNotFixed = parentBaseline?.storage?.notFixedTypeVariables?.keys?.toSet().orEmpty()
        val context = preparationContext.createLocalDeclarationContext(this)
        context.inferenceSession = activeInference
        val copyContext = createRawExpressionCopyContext(sourceInitializer, context, incoming, resources + resource)
        val copiedInitializer = DeepCopyCfirTreeWithSymbols(copyContext).copyRoot(sourceInitializer)
        val canonical = canonicalResource(resource)
        val graph = CfirImplicitTypeComputation(
            canonical, copiedInitializer, copyContext.state.elementMapping(), copyContext.state.symbolMapping(),
            enclosingScope, graphs, incoming,
        )
        graph.parentConstraintBaseline = parentBaseline
        check(graphs.put(canonical, graph) == null) { "A resource initializer graph was instantiated twice" }
        context.implicitTypeComputationScope = CfirImplicitTypeComputationScope(graph, CfirImplicitTypeComputationMode.PRIVATE)

        val rootCandidate = parent?.let {
            val base = components.session.inferenceComponents.createConstraintSystem().apply {
                addOuterSystem(checkNotNull(graph.parentConstraintBaseline).storage)
            }
            Candidate(
                symbol = resource.symbol,
                dispatchReceiver = null,
                givenExtensionReceiver = null,
                explicitReceiverKind = ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                constraintSystemFactory = components.session.inferenceComponents.constraintSystemFactory,
                baseSystem = base.currentStorage(),
                callInfo = CallInfo(
                    callSite = resource, callKind = CallKind.NamedValueAccess, name = resource.name,
                    explicitReceiver = null, arguments = emptyList(), isUsedAsGetClassReceiver = false,
                    typeArguments = emptyList(), session = components.session, containingFile = context.file,
                    containingDeclarations = context.containers.toList(), resolutionMode = ResolutionMode.ContextIndependent,
                ),
                originScope = null,
                bodyResolveContext = context,
            ).apply { initializeSubstitutorAndVariables(ConeSubstitutor.Empty, emptyList()) }
        }
        val child = rootCandidate?.let { checkNotNull(parent).createChildSession(it) }
        if (child != null) context.inferenceSession = child
        val transformer = CfirBodyResolveTransformer(
            components.session, components.scopeSession, this, context,
            phase = CfirResolvePhase.IMPLICIT_TYPES, implicitTypeOnly = false,
        )
        expressionContext(transformer.expressionsTransformer)
        transformer.dataFlowAnalyzer.enterResourceInitializer(resource)
        graph.registerPrivateInitializerContext(resource, context)
        if (child != null) {
            val privateAnalyzer = transformer.components.callCompleter.createPostponedArgumentsAnalyzer(transformer.resolutionContext)
            graph.analyzePostponedAtom = { system, atom, candidate, withPcla ->
                val previousResults = candidate.capturePCLACompletionState()
                val previousVariables = system.currentStorage().allTypeVariables.keys.toSet()
                val previousSelectedCandidates = graph.selectedCandidates().toSet()
                withActiveContext(context) {
                    graph.withPrivateOrigins {
                        child.withActualCompletionSystem(system) {
                            transformer.dataFlowAnalyzer.withResourceInitializer(resource) {
                                privateAnalyzer.analyze(system, atom, candidate, withPcla)
                                graph.capturePrivateAnalysisResults(candidate, previousResults, previousVariables, previousSelectedCandidates)
                            }
                        }
                    }
                }
            }
        }
        try {
            transformer.dataFlowAnalyzer.enterVariableDeclaration(resource)
            val result = withActiveContext(context) {
                context.withContainer(resource) {
                    context.withVariableInitializer(resource) {
                        graph.initializer = graph.initializer.transformSingle(transformer, ResolutionMode.ContextIndependent)
                        transformer.declarationsTransformer.inferVariableTypeFromInitializer(resource, graph.initializer)
                    }
                }
            }
            transformer.dataFlowAnalyzer.exitVariableDeclaration(resource)
            transformer.dataFlowAnalyzer.suspendResourceInitializer(resource)
            if (child != null) {
                // 独立 FULL 调用不经过 PCLA partial 回调，其最终变量/约束同样必须归父图。
                child.currentCommonSystem.integrateInferenceForest(graph.independentStorages(), requireClosed = false)
                child.applyResultsToMainCandidate()
                check(incomingNotFixed.none { it in rootCandidate!!.system.fixedTypeVariables }) {
                    "Resource inference fixed a variable owned by its active parent"
                }
                checkNotNull(parent).integrateRetainedChildSession(
                    graph, child, checkNotNull(rootCandidate),
                    writeCall = { call, substitutor ->
                        withActiveContext(context) {
                            transformer.dataFlowAnalyzer.withResourceInitializer(resource) {
                                call.expression.transformSingle(transformer.components.callCompleter.createCompletionResultsWriter(
                                    substitutor, completionSystem = checkNotNull(graph.constraintSystemOwner),
                                    completionRoot = call.expression,
                                    completionCandidate = (call as? org.cangnova.cangjie.cfir.resolve.calls.ConeAtomWithCandidate)?.candidate,
                                ), null)
                            }
                        }
                    },
                    writeAdditionalResults = { callbacks, lambdas, substitutor ->
                        withActiveContext(context) {
                            transformer.dataFlowAnalyzer.withResourceInitializer(resource) {
                                transformer.components.callCompleter.createCompletionResultsWriter(substitutor)
                                    .writePCLAAdditionalResults(callbacks, lambdas)
                            }
                        }
                    },
                )
            } else {
                val storage = components.session.inferenceComponents.createConstraintSystem()
                // 只汇合完整完成的森林根。外层 FULL storage 已包含并固定内层 partial 的
                // 变量，不能再用内层的旧 notFixed 记录把它们重新引入。
                storage.integrateInferenceForest(graph.independentStorages(), requireClosed = true)
                graph.retainDefaultFinalStorage(storage.captureStorage(), storage)
                graph.retainVariableOrigins(storage.currentStorage(), emptyList())
            }
            val type = parent?.substituteCurrentType(result.coneType) ?: result.coneType
            type.forEachTypeIncludingMetadata { component ->
                check(component !is ConeStubType && (component !is ConeTypeVariableType ||
                    parent != null && component.typeConstructor in parent.currentCommonSystem.allTypeVariables)) {
                    "A resource type escaped without its retained inference owner: $component"
                }
            }
            return type.toCfirResolvedTypeRef(result.source, result).also(graph::storeResult)
        } catch (failure: Throwable) {
            graphs.remove(canonical)
            throw failure
        }
    }

    /** 初始解析与晚 analyzer/writer 共用真实调用栈；lambda 内切换会话仍从该 context 读取。 */
    private fun <T> withActiveContext(context: BodyResolveContext, block: () -> T): T {
        activeContexts.addLast(context)
        return try {
            block()
        } finally {
            check(activeContexts.removeLast() === context)
        }
    }

    /** 局部资源循环的主诊断来自初始化检查；失败类型沿既有 duplicate 诊断通道传播。 */
    private fun recursiveResourceType(resource: CfirResourceVariable): CfirResolvedTypeRef {
        computationSession.calculateAndStoreNonTrivialLoop(resource.symbol)
        return ConeErrorType(ConeUnreportedDuplicateDiagnostic(ConeSimpleDiagnostic("Recursive resource initializer type")))
            .toCfirResolvedTypeRef(resource.returnTypeRef.source)
    }

}
