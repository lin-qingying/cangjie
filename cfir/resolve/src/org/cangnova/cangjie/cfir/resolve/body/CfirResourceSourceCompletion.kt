package org.cangnova.cangjie.cfir.resolve.body

import java.util.IdentityHashMap
import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostic.ConeTypeMismatchError
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.CfirResolutionSnapshot
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.inference.*
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeArgumentConstraintPosition
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.type.AbstractTypeChecker

/** 资源说明的结果分开表示未定型和确定失败，pending 不是诊断。 */
internal enum class CfirResourceTypeCheck { VALID, PENDING, REJECTED }

internal fun checkResourceSpecification(
    resource: CfirResourceVariable,
    resourceType: ConeCangJieType,
    session: org.cangnova.cangjie.cfir.session.CfirSession,
    actualSystem: ConstraintSystemImpl?,
): CfirResourceTypeCheck {
    val sourceType = checkNotNull(resource.returnTypeRef.coneTypeOrNull)
    val actual = sourceType.fullyExpandedType(session)
    if (actual is ConeErrorType) return CfirResourceTypeCheck.REJECTED
    if (actual.contains { it is ConeTypeVariableType }) {
        val system = checkNotNull(actualSystem) { "Pending Resource type has no actual inference owner" }
        actual.forEachTypeIncludingMetadata { type ->
            if (type is ConeTypeVariableType) check(type.typeConstructor in system.allTypeVariables)
        }
        if (java.lang.Boolean.getBoolean("cangjie.debug.resourceSource")) {
            println("RESOURCE SOURCE CONSTRAINT resource=${resource.name}@${System.identityHashCode(resource)} " +
                "type=$actual system=${System.identityHashCode(system)} " +
                "fixed=${system.fixedTypeVariables} pending=" + system.notFixedTypeVariables.entries.joinToString { (key, value) ->
                    "${System.identityHashCode(key)}:$key=" + value.constraints.joinToString { "${it.kind}:${it.type}" }
                })
        }
        // 对齐官方 IsPlaceholderSubtype/Unify：加入真实约束，等待实际 completion 的固定结果。
        system.addSubtypeConstraint(actual, resourceType, ConeArgumentConstraintPosition(resource))
        return CfirResourceTypeCheck.PENDING
    }
    if (!actual.isNothing && AbstractTypeChecker.isSubtypeOf(session.typeContext, actual, resourceType)) {
        return CfirResourceTypeCheck.VALID
    }
    resource.replaceReturnTypeRef(resource.returnTypeRef.resolvedTypeFromPrototype(
        ConeErrorType(ConeTypeMismatchError(resourceType, actual)), resource.source,
    ))
    return CfirResourceTypeCheck.REJECTED
}

/** RAW 复制只复制语法，source/cache/回调能力必须由复制后的实际 owner 重新建立。 */
private inline fun <reified V : Any> transientSourceData(description: String): CfirRawDeclarationDataPolicy<V> =
    object : CfirRawDeclarationDataPolicy<V> {
        override val description: String = description
        override fun decode(value: Any): V { require(value is V); return value }
        override fun collectOwned(value: V, context: CfirRawCopyContext, visit: (CfirElement) -> Unit) {}
        override fun copy(value: V, context: CfirRawCopyContext, install: (V) -> Unit) {}
    }

private object ResourceCacheKey : CfirDeclarationDataKey() {
    override val rawCopyPolicy = transientSourceData<ImplicitBodyResolveComputationSession>("resource source cache owner")
}
private object ResourceCompletionKey : CfirDeclarationDataKey() {
    override val rawCopyPolicy = transientSourceData<CfirResourceSourceCompletion>("resource source completion generation")
}
internal var CfirResourceVariable.resourceSourceCache: ImplicitBodyResolveComputationSession? by CfirDeclarationDataRegistry.data(ResourceCacheKey)
internal var CfirResourceVariable.resourceSourceCompletion: CfirResourceSourceCompletion? by CfirDeclarationDataRegistry.data(ResourceCompletionKey)

/** 真正 source declaration 的一次完成任务；对象身份同时是 AST/cache/CS 试算代次。 */
internal class CfirResourceSourceCompletion(
    val resource: CfirResourceVariable,
    private val tryExpression: CfirTryExpression,
    private val resourceType: ConeCangJieType,
    private val cache: ImplicitBodyResolveComputationSession,
    val graph: CfirImplicitTypeComputation?,
    private val targetScope: CfirImplicitTypeComputationScope?,
) : (ConeSubstitutor) -> Unit {
    private var owner: Candidate? = null
    private var completed = false
    private var canceled = false
    val isCompleted: Boolean get() = completed
    val isCurrent: Boolean get() = !canceled && resource.resourceSourceCompletion === this

    fun isOwnedBy(candidate: Candidate): Boolean = owner === candidate && isActive()

    /** 仅实际 PCLA 移交可以重绑；原队列对象和其他任务保持不动。 */
    fun bindOwner(candidate: Candidate) {
        check(!completed && !canceled && resource.resourceSourceCompletion === this)
        if (owner !== candidate) owner?.onPCLACompletionResultsWritingCallbacks?.removeAll { it === this }
        owner = candidate
        if (candidate.onPCLACompletionResultsWritingCallbacks.none { it === this }) candidate.onPCLACompletionResultsWritingCallbacks += this
    }

    override fun invoke(substitutor: ConeSubstitutor) {
        error("Resource source completion requires an actual final writer system")
    }

    private fun isActive(): Boolean = isCurrent && !completed && resource.resourceSourceCache === cache &&
        (owner == null || checkNotNull(owner).onPCLACompletionResultsWritingCallbacks.any { it === this })

    /** 标准 typed-IR 写回后验证 source 的真实结果，不能由 fixed map 扫描制造消费。 */
    fun complete(system: ConstraintSystemImpl?, substitutor: ConeSubstitutor, writerOwner: Candidate? = null) {
        if (!isActive()) return
        check(owner === writerOwner) { "Resource source callback changed its actual candidate owner" }
        graph?.validateResourceSourceOwner(this)
        graph?.validatePrivateAdditionalResultsCompletion(system)
        resource.transformSingle(CfirTypeVariablesAfterPCLATransformer(substitutor), null)
        check(!hasPendingResourceSourceTypes(resource, writerOwner, system)) { "Final Resource source still has unresolved types or calls" }
        val outcome = checkResourceSpecification(resource, resourceType, resource.moduleData.session, system)
        check(outcome != CfirResourceTypeCheck.PENDING)
        val initializerError = firstSourceError(resource.initializer)
        // 显式声明类型属于绑定自身；initializer 的根错误仍传播到 try，不能覆盖合法类型注解。
        if (initializerError != null && resource.isTypeImplicit && resource.returnTypeRef.coneTypeOrNull !is ConeErrorType) {
            resource.replaceReturnTypeRef(resource.returnTypeRef.resolvedTypeFromPrototype(initializerError, resource.source))
        }
        val sourceError = resource.returnTypeRef.coneTypeOrNull as? ConeErrorType ?: initializerError
        if (sourceError != null && tryExpression.coneTypeOrNull !is ConeErrorType) {
            tryExpression.replaceConeTypeOrNull(ConeErrorType(ConeUnreportedDuplicateDiagnostic(sourceError.diagnostic)))
        }
        if (targetScope?.privateTreeScope() == null) {
            graph?.validatePrivateResultsRetirement(sourceRejected = sourceError != null)
        }
        graph?.finishPrivateInitializerRoot()
        cache.publishResourceSource(resource)
        if (resource.resolvePhase >= CfirResolvePhase.IMPLICIT_TYPES && resource.resolvePhase < CfirResolvePhase.BODY_RESOLVE) {
            resource.replaceResolvePhase(CfirResolvePhase.BODY_RESOLVE)
        }
        completed = true
        graph?.publishFormalResourceType(this, checkNotNull(resource.returnTypeRef as? CfirResolvedTypeRef))
        owner?.onPCLACompletionResultsWritingCallbacks?.removeAll { it === this }
    }

    /** 返回实际退休结果；PRIVATE 槽完成不消费 canonical graph 的退休资格。 */
    fun retireAfterSourceCompletion(): Boolean {
        if (!isCurrent || !completed || targetScope?.privateTreeScope() != null) return false
        val sourceGraph = graph ?: return false
        sourceGraph.retirePrivateResults(
            targetScope,
            sourceRejected = resource.returnTypeRef.coneTypeOrNull is ConeErrorType || firstSourceError(resource.initializer) != null,
        )
        return true
    }

    fun cancel() {
        canceled = true
        owner?.onPCLACompletionResultsWritingCallbacks?.removeAll { it === this }
    }

    internal fun captureState(): () -> Unit {
        val oldOwner = owner
        val oldCompleted = completed
        val oldCanceled = canceled
        val oldIndex = oldOwner?.onPCLACompletionResultsWritingCallbacks?.indexOfFirst { it === this } ?: -1
        val oldTryType = tryExpression.coneTypeOrNull
        return {
            if (owner !== oldOwner) owner?.onPCLACompletionResultsWritingCallbacks?.removeAll { it === this }
            owner = oldOwner
            completed = oldCompleted
            canceled = oldCanceled
            tryExpression.replaceConeTypeOrNull(oldTryType)
            if (oldIndex >= 0) {
                val callbacks = checkNotNull(oldOwner).onPCLACompletionResultsWritingCallbacks
                if (callbacks.none { it === this }) callbacks.add(oldIndex.coerceAtMost(callbacks.size), this)
            } else {
                oldOwner?.onPCLACompletionResultsWritingCallbacks?.removeAll { it === this }
            }
        }
    }
}

/** 当前 source slot 的类型与候选状态；结构性无类型节点不冒充推断失败。 */
private fun hasPendingResourceSourceTypes(
    root: CfirElement,
    writerOwner: Candidate?,
    completionSystem: ConstraintSystemImpl?,
): Boolean {
    var pending = false
    // 仅沿实际 AST 记录包含声明，不能通过名字、offset 或可变 BodyResolveContext 反查 owner。
    val declarations = ArrayDeque<CfirCallableDeclaration>()
    root.accept(object : CfirVisitorVoid() {
        override fun visitElement(element: CfirElement) {
            if (element is CfirCallableDeclaration) declarations.addLast(element)
            val type = when (element) {
                is CfirResourceVariable -> element.returnTypeRef.coneTypeOrNull
                is CfirResolvedTypeRef -> element.coneType
                is CfirExpression -> element.coneTypeOrNull
                else -> null
            }
            val pendingType = type != null && type !is ConeErrorType &&
                type.contains { it is ConeTypeVariableType || it is ConeStubType }
            val reference = (element as? CfirResolvable)?.calleeReference
            val pendingCall = reference is CfirNamedReferenceWithCandidate && !reference.isError
            if (pendingType || pendingCall) {
                pending = true
                CfirRetainedCompletionTrace.event("SOURCE_PENDING_ELEMENT") {
                    val completionStorage = completionSystem?.captureStorage()
                    val declaration = declarations.lastOrNull()
                    val variables = mutableListOf<ConeTypeVariableType>()
                    type?.forEachType { if (it is ConeTypeVariableType) variables += it }
                    "root=" + CfirRetainedCompletionTrace.identity(root) +
                        " element=" + CfirRetainedCompletionTrace.identity(element) +
                        " source=" + CfirRetainedCompletionTrace.identity(element.source) +
                        " declaration=" + CfirRetainedCompletionTrace.identity(declaration) +
                        " declarationSymbol=" + CfirRetainedCompletionTrace.identity(declaration?.symbol) +
                        " declarationTypeRef=" + CfirRetainedCompletionTrace.identity(declaration?.returnTypeRef) +
                        " declarationPhase=" + declaration?.resolvePhase +
                        " declarationPath=" + declarations.joinToString("/") { CfirRetainedCompletionTrace.identity(it) } +
                        " writerOwner=" + CfirRetainedCompletionTrace.candidate(writerOwner) +
                        " completionSystem=" + CfirRetainedCompletionTrace.identity(completionSystem) +
                        " pendingType=" + pendingType + " pendingCall=" + pendingCall +
                        " type=" + type + " typeIdentity=" + CfirRetainedCompletionTrace.identity(type) +
                        " reference=" + CfirRetainedCompletionTrace.identity(reference) +
                        " actualTarget=" + CfirRetainedCompletionTrace.identity((reference as? CfirResolvedNamedReference)?.resolvedSymbol) +
                        " candidate=" + CfirRetainedCompletionTrace.candidate((reference as? CfirNamedReferenceWithCandidate)?.candidate) +
                        " variables=" + variables.joinToString { variableType ->
                            val constructor = variableType.typeConstructor
                            "constructor=" + CfirRetainedCompletionTrace.identity(constructor) +
                                ",variable=" + CfirRetainedCompletionTrace.identity(completionStorage?.allTypeVariables?.get(constructor)) +
                                ",fixed=" + completionStorage?.fixedTypeVariables?.get(constructor) +
                                ",notFixed=" + (completionStorage?.notFixedTypeVariables?.containsKey(constructor) == true)
                        } +
                        " stack=" + Throwable().stackTrace.joinToString(" <- ")
                }
            }
            element.acceptChildren(this)
            if (element is CfirCallableDeclaration) declarations.removeLast()
        }
    })
    return pending
}

private fun firstSourceError(root: CfirElement?): ConeErrorType? {
    var error: ConeErrorType? = null
    root?.accept(object : CfirVisitorVoid() {
        override fun visitElement(element: CfirElement) {
            if (error == null) error = (element as? CfirExpression)?.coneTypeOrNull as? ConeErrorType
            element.acceptChildren(this)
        }
    })
    return error
}

/** 资源的私有类型 cache 和 source task 与同一 AST 快照原位恢复。 */
internal class CfirResourceSourceSnapshot private constructor(
    val resource: CfirResourceVariable,
    private val cache: ImplicitBodyResolveComputationSession?,
    private val cacheSnapshot: CfirImplicitBodyResolveComputationSessionSnapshot?,
    private val task: CfirResourceSourceCompletion?,
    private val restoreTask: (() -> Unit)?,
    private val sourceTypeRef: CfirTypeRef,
    private val sourceResolveState: CfirResolveState,
    private val sourceInitializer: CfirExpression?,
    private val initializerState: CfirResolutionSnapshot?,
) {
    fun validateRestore() {
        cacheSnapshot?.let { checkNotNull(cache).validateRestore(it) }
        resource.resourceSourceCache?.takeIf { it !== cache }?.validateDiscardResourceSource(resource.symbol)
    }

    fun validateDiscard() { resource.resourceSourceCache?.validateDiscardResourceSource(resource.symbol) }

    fun discard() {
        validateDiscard()
        resource.resourceSourceCompletion?.cancel()
        resource.resourceSourceCache?.discardResourceSource(resource.symbol)
        resource.resourceSourceCompletion = null
        resource.resourceSourceCache = null
    }
    fun restore() {
        restoreTypeResolutionState()
        resource.replaceInitializer(sourceInitializer)
        initializerState?.restore()
    }

    /** PCLA 的类型重放保留当前正在遍历的 initializer 结构；完整回滚仍恢复原 owning slot。 */
    @OptIn(ResolveStateAccess::class)
    fun restoreTypeResolutionState() {
        validateRestore()
        resource.resolveState = sourceResolveState
        resource.replaceReturnTypeRef(sourceTypeRef)
        initializerState?.restoreTypeResolutionState()
        resource.resourceSourceCompletion?.takeIf { it !== task }?.cancel()
        resource.resourceSourceCache?.takeIf { it !== cache }?.discardResourceSource(resource.symbol)
        resource.resourceSourceCache = cache
        if (cacheSnapshot != null) checkNotNull(cache).restore(cacheSnapshot, setOf(resource.symbol))
        resource.resourceSourceCompletion = task
        restoreTask?.invoke()
    }
    companion object {
        @OptIn(ResolveStateAccess::class)
        fun capture(resource: CfirResourceVariable): CfirResourceSourceSnapshot {
            val cache = resource.resourceSourceCache
            val task = resource.resourceSourceCompletion
            return CfirResourceSourceSnapshot(
                resource, cache, cache?.capture(setOf(resource.symbol)), task, task?.captureState(),
                resource.returnTypeRef, resource.resolveState, resource.initializer,
                resource.initializer?.let { CfirResolutionSnapshot.captureResourceInitializer(it) },
            )
        }
    }
}

internal fun captureResourceSourceStates(root: CfirElement): List<CfirResourceSourceSnapshot> = buildList {
    root.accept(object : CfirVisitorVoid() {
        override fun visitElement(element: CfirElement) {
            if (element is CfirResourceVariable) add(CfirResourceSourceSnapshot.capture(element))
            element.acceptChildren(this)
        }
    })
}

/** 完整 AST 回滚同时撤销这次物化的新资源，已从 parent 队列摘掉的任务不能由旧快照重放。 */
internal fun restoreResourceSourceStates(root: CfirElement, snapshots: List<CfirResourceSourceSnapshot>) {
    val previous = snapshots.mapTo(java.util.Collections.newSetFromMap(IdentityHashMap<CfirResourceVariable, Boolean>())) { it.resource }
    val introduced = captureResourceSourceStates(root).filter { it.resource !in previous }
    snapshots.forEach { it.validateRestore() }
    introduced.forEach { it.validateDiscard() }
    introduced.forEach { it.discard() }
    snapshots.forEach { it.restore() }
}
