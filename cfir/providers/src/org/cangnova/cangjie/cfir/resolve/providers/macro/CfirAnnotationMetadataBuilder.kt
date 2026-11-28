package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.declarations.defaultValueMetadata

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import java.util.Collections
import java.util.IdentityHashMap

/**
 * 一次文件构建代次的私有 metadata 事务；raw、fragment 和字段派生显式共享此实例。
 * seal 只表示元数据不可变，不代表 macro construction 已完成。
 */
class CfirAnnotationMetadataBuilder : CfirAnnotationMetadata {
    private enum class State { OPEN, SEALED, ABORTED }
    private var state = State.OPEN
    private val lock = Any()
    // 此 token 不反向持有 builder，已发布的单个 owner 不会拖住整份临时图。
    private val generation = Any()
    private val slotsByOwner = IdentityHashMap<CfirDeclaration, LinkedHashMap<Int, Slot>>()
    private val carrierSlots = LinkedHashMap<CfirAnnotationReplaceCarrier, Slot>()
    private val slotsByAnnotation = IdentityHashMap<CfirAnnotationCall, Slot>()
    private val recordingOrder = mutableListOf<Slot>()
    private val retiredOwners = Collections.newSetFromMap(IdentityHashMap<CfirDeclaration, Boolean>())

    private class Slot(var snapshot: CfirAnnotationSlotSnapshot, var active: Boolean = true)

    /** 私有事务的当前快照，按记录顺序用于 raw 结构检查；不发布到 session。 */
    val snapshots: List<CfirAnnotationSlotSnapshot>
        get() = synchronized(lock) {
            check(state != State.ABORTED) { "Annotation metadata transaction was aborted." }
            recordingOrder.filter { it.active }.map { it.snapshot }
        }

    private fun requireOpen() {
        check(state == State.OPEN) { "Annotation metadata transaction is $state." }
    }

    /** 共享字段生成在同一私有事务中完成，seal 不能观察到一半成员或一半派生槽位。 */
    internal fun <T> withOpenTransaction(action: () -> T): T = synchronized(lock) {
        requireOpen()
        action()
    }

    fun record(snapshot: CfirAnnotationSlotSnapshot): CfirAnnotationReplaceCarrier = synchronized(lock) {
        requireOpen()
        require(snapshot.owner.annotations.getOrNull(snapshot.annotationIndex) === snapshot.originalAnnotation) {
            "Annotation metadata must point at the actual owner slot after append."
        }
        require(snapshot.owner !in retiredOwners) { "Cannot record metadata on a replaced owner." }
        val carrier = CfirAnnotationReplaceCarrier(snapshot.owner, snapshot.annotationIndex, snapshot.originalAnnotation)
        require(slotsByOwner[snapshot.owner]?.containsKey(snapshot.annotationIndex) != true && carrier !in carrierSlots) {
            "Duplicate annotation metadata for owner/index ${snapshot.annotationIndex}."
        }
        require(snapshot.originalAnnotation !in slotsByAnnotation) { "An annotation identity cannot belong to multiple slots." }
        val slots = slotsByOwner.getOrPut(snapshot.owner) { LinkedHashMap() }
        val captured = snapshot.copy(tokens = immutableList(snapshot.tokens))
        val slot = Slot(captured)
        slots[snapshot.annotationIndex] = slot
        carrierSlots[carrier] = slot
        slotsByAnnotation[snapshot.originalAnnotation] = slot
        recordingOrder += slot
        carrier
    }

    fun recordDerivedSlot(
        sourceAnnotation: CfirAnnotationCall,
        owner: CfirDeclaration,
        annotationIndex: Int,
        derivedAnnotation: CfirAnnotationCall,
    ): CfirAnnotationReplaceCarrier = synchronized(lock) {
        requireOpen()
        require(sourceAnnotation !== derivedAnnotation) { "A derived annotation slot requires an independent annotation identity." }
        val source = checkNotNull(slotsByAnnotation[sourceAnnotation]?.snapshot) {
            "Cannot derive annotation metadata without the source annotation snapshot."
        }
        require(source.owner.moduleData.session === owner.moduleData.session) {
            "Derived annotation metadata cannot cross sessions."
        }
        record(source.copy(owner = owner, annotationIndex = annotationIndex, originalAnnotation = derivedAnnotation))
    }

    override fun snapshot(carrier: CfirAnnotationReplaceCarrier): CfirAnnotationSlotSnapshot? = synchronized(lock) {
        check(state != State.ABORTED) { "Annotation metadata transaction was aborted." }
        carrierSlots[carrier]?.snapshot
    }

    /** 稳定 carrier 表示来源，当前槽位必须从事务中取得；已消费/删除的 annotation 没有当前槽位。 */
    fun currentSlot(carrier: CfirAnnotationReplaceCarrier): CfirAnnotationReplaceCarrier? = synchronized(lock) {
        check(state != State.ABORTED) { "Annotation metadata transaction was aborted." }
        carrierSlots[carrier]?.takeIf { it.active }?.let { slot ->
            check(slotsByOwner[slot.snapshot.owner]?.get(slot.snapshot.annotationIndex) === slot) {
                "Active annotation slot is absent from its canonical owner table."
            }
            slot.snapshot.asCarrier()
        }
    }

    override fun snapshot(annotation: CfirAnnotationCall): CfirAnnotationSlotSnapshot? = synchronized(lock) {
        check(state != State.ABORTED) { "Annotation metadata transaction was aborted." }
        slotsByAnnotation[annotation]?.snapshot
    }

    /** splice 成功后迁移当前 identity；原 carrier 仍可查询同一槽位的原始语法 provenance。 */
    fun migrate(carrier: CfirAnnotationReplaceCarrier, replacement: CfirAnnotationCall) = synchronized(lock) {
        requireOpen()
        val slot = requireNotNull(carrierSlots[carrier]?.takeIf { it.active }) {
            "Cannot migrate an annotation slot absent from this transaction."
        }
        require(slot.snapshot.owner.annotations.getOrNull(slot.snapshot.annotationIndex) === replacement) {
            "Annotation migration must follow the actual slot replacement."
        }
        require(replacement !in slotsByAnnotation) { "Replacement annotation already belongs to a slot." }
        slotsByAnnotation.remove(slot.snapshot.originalAnnotation)
        slot.snapshot = slot.snapshot.copy(originalAnnotation = replacement)
        carrierSlots[slot.snapshot.asCarrier()] = slot
        slotsByAnnotation[replacement] = slot
    }

    /** 实际声明替换后显式转移保留的 annotation 槽位；原 surface carrier 继续引用同一 provenance。 */
    fun transferOwner(original: CfirDeclaration, replacement: CfirDeclaration) = synchronized(lock) {
        requireOpen()
        require(original !== replacement) { "Owner transfer requires a distinct replacement declaration." }
        require(original.moduleData.session === replacement.moduleData.session) { "Annotation owner transfer cannot cross sessions." }
        require(original !in retiredOwners && replacement !in retiredOwners) { "Cannot transfer a replaced annotation owner." }
        require(original.annotations.size == replacement.annotations.size &&
            original.annotations.indices.all { original.annotations[it] === replacement.annotations[it] }) {
            "Owner transfer must preserve the actual annotation identities and slot order."
        }
        require(replacement !in slotsByOwner && carrierSlots.keys.none { it.owner === replacement }) {
            "Replacement owner already has annotation metadata."
        }
        val slots = slotsByOwner[original]
        if (slots == null) {
            retiredOwners.add(original)
            return@synchronized
        }
        require(slots.values.all { original.annotations.getOrNull(it.snapshot.annotationIndex) === it.snapshot.originalAnnotation }) {
            "Original owner slots changed without metadata migration."
        }
        slotsByOwner.remove(original)
        retiredOwners.add(original)
        for (slot in slots.values) {
            slot.snapshot = slot.snapshot.copy(owner = replacement)
            carrierSlots[slot.snapshot.asCarrier()] = slot
        }
        slotsByOwner[replacement] = slots
    }

    /**
     * 条件裁剪或宏合并的显式列表变更：调用者决定列表与新 annotation 的来源绑定。
     * 先验证所有旧槽位、来源和新身份，再同时更新真实列表及索引；不在 seal 时补猜 owner/index。
     */
    fun replaceAnnotationList(
        owner: CfirDeclaration,
        annotations: List<CfirAnnotation>,
        bindings: Map<CfirAnnotationCall, CfirAnnotationReplaceCarrier> = emptyMap(),
    ) = synchronized(lock) {
        requireOpen()
        require(owner !in retiredOwners) { "Cannot change annotations on a replaced owner." }
        val previous = slotsByOwner[owner]?.values?.toList().orEmpty()
        require(previous.all { owner.annotations.getOrNull(it.snapshot.annotationIndex) === it.snapshot.originalAnnotation }) {
            "Annotation list changed without metadata migration."
        }
        val next = LinkedHashMap<Int, Slot>()
        val updates = IdentityHashMap<Slot, CfirAnnotationSlotSnapshot>()
        val seen = Collections.newSetFromMap(IdentityHashMap<CfirAnnotation, Boolean>())
        for ((index, annotation) in annotations.withIndex()) {
            require(seen.add(annotation)) { "Annotation list contains a duplicate object identity." }
            if (annotation !is CfirAnnotationCall) continue
            val binding = bindings[annotation]
            val slot = if (binding != null) {
                requireNotNull(carrierSlots[binding]?.takeIf { it.active }) { "Annotation binding is absent from this transaction." }
            } else {
                slotsByAnnotation[annotation]?.also {
                    require(it.snapshot.owner === owner) { "Moving an annotation requires an explicit source carrier." }
                }
            } ?: continue
            require(slot.active && slotsByOwner[slot.snapshot.owner]?.get(slot.snapshot.annotationIndex) === slot) {
                "Annotation binding is not a current canonical slot."
            }
            require(slot.snapshot.owner.annotations.getOrNull(slot.snapshot.annotationIndex) === slot.snapshot.originalAnnotation) {
                "Source annotation slot changed without metadata migration."
            }
            require(slot.snapshot.owner.moduleData.session === owner.moduleData.session) { "Annotation bindings cannot cross sessions." }
            require(slot !in updates) { "A source annotation slot cannot supply multiple replacement slots." }
            require(slotsByAnnotation[annotation].let { it == null || it === slot }) { "Replacement annotation belongs to another slot." }
            val updated = slot.snapshot.copy(owner = owner, annotationIndex = index, originalAnnotation = annotation)
            require(carrierSlots[updated.asCarrier()].let { it == null || it === slot }) { "New annotation carrier collides with retained provenance." }
            next[index] = slot
            updates[slot] = updated
        }
        require(bindings.keys.all { it in seen }) { "Annotation binding does not occur in the replacement list." }

        owner.replaceAnnotations(annotations.toList())
        for (slot in previous) {
            if (slot !in updates) {
                slot.active = false
                slotsByAnnotation.remove(slot.snapshot.originalAnnotation)
            }
        }
        slotsByOwner.remove(owner)
        for ((slot, updated) in updates) {
            val old = slot.snapshot
            slotsByOwner[old.owner]?.remove(old.annotationIndex)
            slotsByAnnotation.remove(old.originalAnnotation)
            slot.snapshot = updated
            carrierSlots[updated.asCarrier()] = slot
            slotsByAnnotation[updated.originalAnnotation] = slot
        }
        if (next.isNotEmpty()) slotsByOwner[owner] = next
        slotsByOwner.entries.removeIf { it.value.isEmpty() }
    }

    /**
     * 只封闭最终图中仍存在的 owner；未采用的宏片段和被删除声明不进入发布产品。
     * 正式 CFIR visitor 确认成员关系，不按源码位置或名字补造 metadata。
     */
    fun seal(finalRoots: List<CfirDeclaration>): CfirAnnotationMetadataSnapshot = synchronized(lock) {
        requireOpen()
        require(finalRoots.isNotEmpty()) { "Metadata publication requires an actual final graph root." }
        val session = finalRoots.first().moduleData.session
        require(finalRoots.all { it.moduleData.session === session }) { "A metadata product cannot cross sessions." }
        val tables = mutableListOf<CfirDeclarationAnnotationMetadata>()
        val visited = Collections.newSetFromMap(IdentityHashMap<CfirElement, Boolean>())
        val visitor = object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (!visited.add(element)) return
                if (element is CfirDeclaration) {
                    val slots = slotsByOwner[element]
                    if (slots != null) {
                        require(element.moduleData.session === session) { "Annotation owner belongs to another session." }
                        val snapshots = slots.values.map { it.snapshot }
                        for (slot in snapshots) {
                            check(element.annotations.getOrNull(slot.annotationIndex) === slot.originalAnnotation) {
                                "Final annotation slots differ from the recorded construction metadata."
                            }
                        }
                        tables += CfirDeclarationAnnotationMetadata(element, generation, immutableList(snapshots))
                    }
                }
                if (element is org.cangnova.cangjie.cfir.declarations.CfirValueParameter) {
                    element.defaultValueMetadata?.function?.accept(this)
                }
                element.acceptChildren(this)
            }
        }
        finalRoots.forEach { it.accept(visitor) }
        state = State.SEALED
        CfirAnnotationMetadataSnapshot(session, immutableList(tables))
    }

    /** 终止私有构造；不触碰任何已发布文件，清除对本次临时图的索引引用。 */
    fun abort() = synchronized(lock) {
        requireOpen()
        state = State.ABORTED
        slotsByOwner.clear()
        carrierSlots.clear()
        slotsByAnnotation.clear()
        recordingOrder.clear()
        retiredOwners.clear()
    }

    /** 异常退出的清理只终止仍打开的事务；已封闭产品保持不可变，不遮蔽原异常。 */
    internal fun abortIfOpen() = synchronized(lock) {
        if (state == State.OPEN) abort()
    }

    private fun <T> immutableList(values: Collection<T>): List<T> = Collections.unmodifiableList(ArrayList(values))

    private fun CfirAnnotationSlotSnapshot.asCarrier(): CfirAnnotationReplaceCarrier =
        CfirAnnotationReplaceCarrier(owner, annotationIndex, originalAnnotation)
}
