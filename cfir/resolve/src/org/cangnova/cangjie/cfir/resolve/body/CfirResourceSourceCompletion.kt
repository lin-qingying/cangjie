package org.cangnova.cangjie.cfir.resolve.body

import java.util.IdentityHashMap
import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.diagnostic.ConeTypeMismatchError
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.resolve.CfirResolutionSnapshot
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.calls.candidate.CfirNamedReferenceWithCandidate
import org.cangnova.cangjie.cfir.resolve.inference.*
import org.cangnova.cangjie.cfir.resolve.inference.model.ConeArgumentConstraintPosition
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.firstErrorTypeOrNull
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.firstSourceErrorOrPending
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.isPostponedInference
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.isResourceProtocolMismatch
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.cfir.visitors.transformSingle
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintSystemImpl
import org.cangnova.cangjie.type.AbstractTypeChecker

/** 资源说明的协议判断以声明综合成功为前提，未完成的综合允许加入实际推断约束。 */
internal enum class CfirResourceTypeCheck { VALID, PENDING, SYNTHESIS_FAILED, REJECTED }

internal fun checkResourceSpecification(
    resource: CfirResourceVariable,
    resourceType: ConeCangJieType,
    session: CfirSession,
    actualSystem: ConstraintSystemImpl?,
): CfirResourceTypeCheck {
    val sourceType = checkNotNull(resource.returnTypeRef.coneTypeOrNull)
    val actual = sourceType.fullyExpandedType(session)
    val synthesisError = resource.resourceSynthesisError
    if (synthesisError != null && !synthesisError.isPostponedInference) {
        // 显式类型仍由原注解拥有；隐式绑定必须向后续资源发布 initializer 的真实失败。
        if (resource.isTypeImplicit && sourceType !is ConeErrorType) {
            resource.replaceReturnTypeRef(resource.returnTypeRef.resolvedTypeFromPrototype(synthesisError, resource.source))
        }
        return CfirResourceTypeCheck.SYNTHESIS_FAILED
    }
    if (actual is ConeErrorType) {
        return if (actual.diagnostic.isResourceProtocolMismatch) CfirResourceTypeCheck.REJECTED else CfirResourceTypeCheck.PENDING
    }
    if (actual.contains { it is ConeTypeVariableType }) {
        val system = checkNotNull(actualSystem) { "Pending Resource type has no actual inference owner" }
        actual.forEachTypeIncludingMetadata { type ->
            if (type is ConeTypeVariableType) check(type.typeConstructor in system.allTypeVariables)
        }
        // 对齐官方 IsPlaceholderSubtype/Unify：加入真实约束，等待实际 completion 的固定结果。
        system.addSubtypeConstraint(actual, resourceType, ConeArgumentConstraintPosition(resource))
        return CfirResourceTypeCheck.PENDING
    }
    if (synthesisError != null) return CfirResourceTypeCheck.PENDING
    if (!actual.isNothing && AbstractTypeChecker.isSubtypeOf(session.typeContext, actual, resourceType)) {
        return CfirResourceTypeCheck.VALID
    }
    resource.replaceReturnTypeRef(resource.returnTypeRef.resolvedTypeFromPrototype(
        ConeErrorType(ConeTypeMismatchError(resourceType, actual, ConeTypeMismatchError.Origin.RESOURCE_PROTOCOL)),
        resource.source,
    ))
    return CfirResourceTypeCheck.REJECTED
}

/** 资源协议失败使资源绑定无效。只有资源声明综合失败才使外层 try 无效。 */
internal val CfirResourceVariable.resourceSynthesisError: ConeErrorType?
    get() = sequenceOf(
        (returnTypeRef.coneTypeOrNull as? ConeErrorType)?.takeUnless { it.diagnostic.isResourceProtocolMismatch },
        initializer?.firstErrorTypeOrNull(moduleData.session),
    ).firstSourceErrorOrPending()

/** 资源 try 的即时解析和最终写回按同一顺序消费声明综合与各子句的错误。 */
internal fun CfirTryExpression.resourceTryInputError(session: CfirSession): ConeErrorType? =
    (resources.asSequence().map { it.resourceSynthesisError } +
        sequenceOf(tryBlock.firstErrorTypeOrNull(session)) +
        catches.asSequence().map { it.pattern.resolvedTypeRef?.coneType as? ConeErrorType } +
        catches.asSequence().map { it.body.firstErrorTypeOrNull(session) } +
        handlers.asSequence().map { it.firstErrorTypeOrNull(session) } +
        sequenceOf(finallyBlock?.firstErrorTypeOrNull(session))).firstSourceErrorOrPending()

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
        check(!hasPendingResourceSourceTypes(resource)) { "Final Resource source still has unresolved types or calls" }
        val outcome = checkResourceSpecification(resource, resourceType, resource.moduleData.session, system)
        check(outcome != CfirResourceTypeCheck.PENDING)
        if (targetScope?.privateTreeScope() == null) {
            graph?.validatePrivateResultsRetirement(
                sourceRejected = outcome != CfirResourceTypeCheck.VALID,
            )
        }
        graph?.finishPrivateInitializerRoot()
        cache.publishResourceSource(resource)
        if (resource.resolvePhase >= CfirResolvePhase.IMPLICIT_TYPES && resource.resolvePhase < CfirResolvePhase.BODY_RESOLVE) {
            resource.replaceResolvePhase(CfirResolvePhase.BODY_RESOLVE)
        }
        completed = true
        updateTryResultAfterResourceCompletion()
        graph?.publishFormalResourceType(this, checkNotNull(resource.returnTypeRef as? CfirResolvedTypeRef))
        owner?.onPCLACompletionResultsWritingCallbacks?.removeAll { it === this }
    }

    /** 仅替换旧 pending 或传播本次声明的新错误；已确定的外层错误仍由原子句拥有。 */
    private fun updateTryResultAfterResourceCompletion() {
        val current = tryExpression.coneTypeOrNull ?: return
        val session = resource.moduleData.session
        val error = if (current is ConeErrorType) {
            if (!current.isPostponedInference) return
            tryExpression.resourceTryInputError(session)
        } else resource.resourceSynthesisError
        if (error != null) {
            tryExpression.replaceConeTypeOrNull(ConeErrorType(ConeUnreportedDuplicateDiagnostic(error.diagnostic)))
        } else if (current is ConeErrorType &&
            tryExpression.resources.none { it.resourceSourceCompletion?.isCompleted == false }
        ) {
            tryExpression.replaceConeTypeOrNull(ConePrimitiveType.UNIT)
        }
    }

    /** 返回实际退休结果；PRIVATE 槽完成不消费 canonical graph 的退休资格。 */
    fun retireAfterSourceCompletion(): Boolean {
        if (!isCurrent || !completed || targetScope?.privateTreeScope() != null) return false
        val sourceGraph = graph ?: return false
        sourceGraph.retirePrivateResults(
            targetScope,
            sourceRejected = resource.returnTypeRef.coneTypeOrNull is ConeErrorType || resource.resourceSynthesisError != null,
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
): Boolean {
    var pending = false
    root.accept(object : CfirVisitorVoid() {
        override fun visitElement(element: CfirElement) {
            val type = when (element) {
                is CfirResourceVariable -> element.returnTypeRef.coneTypeOrNull
                is CfirResolvedTypeRef -> element.coneType
                is CfirExpression -> element.coneTypeOrNull
                else -> null
            }
            val pendingType = type?.contains {
                it is ConeTypeVariableType || it is ConeStubType || it is ConeErrorType && it.isPostponedInference
            } == true
            val reference = (element as? CfirResolvable)?.calleeReference
            val pendingCall = reference is CfirNamedReferenceWithCandidate && !reference.isError
            if (pendingType || pendingCall) {
                pending = true
            }
            element.acceptChildren(this)
        }
    })
    return pending
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
