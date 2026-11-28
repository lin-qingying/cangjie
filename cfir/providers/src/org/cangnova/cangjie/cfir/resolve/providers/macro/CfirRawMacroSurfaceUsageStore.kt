package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.declarations.defaultValueMetadata

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirCodeFragment
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.cfir.visitors.CfirDefaultVisitorVoid
import java.util.Collections
import java.util.IdentityHashMap

/** Raw macro 使用事实与 annotation registry 共用所属 session 的锁，提交/恢复期间读者不能看到半个产品。 */
class CfirRawMacroSurfaceUsageStore(private val session: CfirSession) : CfirSessionComponent {
    private class Entry(val file: CfirFile, val slots: MutableMap<CfirRawMacroUsageSlot, List<MacroSurfaceExpr>>)
    private val entries: MutableMap<Any, Entry> = IdentityHashMap()

    fun recordExpressionSurfaces(file: CfirFile, surfaces: List<MacroSurfaceExpr>): Unit = synchronized(session) {
        require(file.moduleData.session === session)
        check(entries[file] == null) { "A raw file generation can register its usage entry only once." }
        entries[file] = createEntry(file, surfaces)
    }

    /** 文件与调用方实际来源身份共享同一份槽位记录。 */
    fun recordExpressionSurfaces(file: CfirFile, sourceFileIdentity: Any, surfaces: List<MacroSurfaceExpr>): Unit =
        synchronized(session) {
            require(file.moduleData.session === session)
            check(entries[file] == null) { "A raw file generation can register its usage entry only once." }
            val entry = createEntry(file, surfaces)
            entries[file] = entry
            entries[sourceFileIdentity] = entry
        }

    fun expressionSurfaces(file: CfirFile): List<MacroSurfaceExpr> = synchronized(session) {
        allSurfaces(entries[file])
    }

    fun expressionSurfacesForSourceFile(sourceFileIdentity: Any): List<MacroSurfaceExpr> = synchronized(session) {
        allSurfaces(entries[sourceFileIdentity])
    }

    /** 原编辑入口和当前target同时属于同一CFIR产品；显式登记身份，不按路径回查或补造记录。 */
    fun bindSourceFileIdentity(file: CfirFile, sourceFileIdentity: Any): Unit = synchronized(session) {
        val entry = checkedEntry(file, emptyList())
        check(entries[sourceFileIdentity].let { it == null || it === entry }) { "A source identity already belongs to another raw file." }
        entries[sourceFileIdentity] = entry
    }

    /** 整树 source 换代只移动真实来源入口；CFIR 文件及实际 owner 槽位身份保持不变。 */
    fun validateSourceFileReplacement(file: CfirFile, previous: Any, replacement: Any): Unit = synchronized(session) {
        val entry = checkedEntry(file, emptyList())
        check(entries[previous] === entry) { "The previous PSI source does not own this raw file generation." }
        check(previous !== replacement && entries[replacement] == null) { "The replacement PSI source already belongs to a raw generation." }
    }

    /** 整代源入口及实际使用事实一起准备；被丢弃正文的旧 carrier 不进入新代次。 */
    fun prepareSourceFileReplacement(
        file: CfirFile,
        previous: Any,
        replacement: Any,
        discardedSlots: Set<CfirRawMacroUsageSlot>,
        mapSurface: (MacroSurfaceExpr) -> MacroSurfaceExpr,
    ): CfirSourceGenerationUpdate = synchronized(session) {
        validateSourceFileReplacement(file, previous, replacement)
        val entry = entries.getValue(file)
        val oldSlots = entry.slots.toMap()
        require(discardedSlots.all { it in oldSlots }) { "Source replacement must discard actual registered body slots." }
        val nextSlots = oldSlots.mapValues { (slot, surfaces) ->
            if (slot in discardedSlots) emptyList() else immutable(surfaces.map(mapSurface))
        }
        object : CfirSourceGenerationUpdate {
            private var committed = false

            override fun validate(): Unit = synchronized(session) {
                check(!committed)
                validateSourceFileReplacement(file, previous, replacement)
                check(entry.slots.size == oldSlots.size && oldSlots.all { (slot, surfaces) -> entry.slots[slot] === surfaces }) {
                    "Raw usage slots changed before source generation adoption."
                }
            }

            override fun commit(): Unit = synchronized(session) {
                validate()
                nextSlots.forEach { (slot, surfaces) -> entry.slots[slot] = surfaces }
                entries.remove(previous)
                entries[replacement] = entry
                committed = true
            }
        }.also { it.validate() }
    }

    /** 在任何 lazy 槽位采用前确认文件、槽位及唯一性；不补建缺失记录。 */
    fun validateAdoptionSlots(file: CfirFile, slots: List<CfirRawMacroUsageSlot>): Unit = synchronized(session) {
        checkedEntry(file, slots)
        Unit
    }

    /** 根据 postProcess 后的实际图选择旧事实与 fresh 事实，准备阶段不改写索引。 */
    fun prepareExpressionSurfacePublication(
        file: CfirFile,
        slots: List<CfirRawMacroUsageSlot>,
        surfaces: List<MacroSurfaceExpr>,
    ): CfirRawMacroUsagePublication = synchronized(session) {
        val entry = checkedEntry(file, slots)
        val previous = slots.associateWith { entry.slots.getValue(it) }
        val roots = slots.associateWith { it.currentRootOrNull() }
        val next = assignSurfaces(slots, previous.values.flatten() + surfaces)
        Publication(file, entry, previous, next, roots).also { it.validate() }
    }

    /** 单个显式提交仍通过准备/提交协议；空更新撤销旧 carrier，复用节点的事实保持。 */
    fun replaceAdoptedExpressionSurfaces(file: CfirFile, slots: List<CfirRawMacroUsageSlot>, surfaces: List<MacroSurfaceExpr>) {
        val publication = prepareExpressionSurfacePublication(file, slots, surfaces)
        try {
            publication.commit()
        } catch (failure: Throwable) {
            publication.restore()
            throw failure
        }
    }

    private fun checkedEntry(file: CfirFile, slots: List<CfirRawMacroUsageSlot>): Entry {
        require(file.moduleData.session === session) { "A raw usage entry belongs to its file's source session." }
        val entry = checkNotNull(entries[file]) { "The raw file must register its usage entry before lazy adoption." }
        check(entry.file === file)
        require(slots.toSet().size == slots.size) { "An adoption cannot submit the same actual slot twice." }
        require(slots.none { it.kind == CfirRawMacroUsageSlot.Kind.HEADER }) { "Body adoption cannot replace a file header." }
        require(slots.all { entry.slots.containsKey(it) }) { "A lazy adoption must use actual registered owner slots." }
        return entry
    }

    private enum class PublicationState { PREPARED, COMMITTING, COMMITTED, RESTORED }

    private inner class Publication(
        private val file: CfirFile,
        private val entry: Entry,
        private val previous: Map<CfirRawMacroUsageSlot, List<MacroSurfaceExpr>>,
        private val next: Map<CfirRawMacroUsageSlot, List<MacroSurfaceExpr>>,
        private val roots: Map<CfirRawMacroUsageSlot, CfirElement?>,
    ) : CfirRawMacroUsagePublication {
        private var state = PublicationState.PREPARED

        override fun validate(): Unit = synchronized(session) {
            check(state == PublicationState.PREPARED) { "Raw usage publication is $state." }
            check(checkedEntry(file, previous.keys.toList()) === entry) { "Raw file generation changed before publication." }
            for ((slot, original) in previous) {
                check(entry.slots.getValue(slot) === original) { "Raw usage slot changed before publication." }
                check(slot.currentRootOrNull() === roots.getValue(slot)) { "Adopted root changed before publication." }
            }
        }

        override fun commit(): Unit = synchronized(session) {
            validate()
            state = PublicationState.COMMITTING
            for ((slot, surfaces) in next) entry.slots[slot] = surfaces
            state = PublicationState.COMMITTED
        }

        override fun restore(): Unit = synchronized(session) {
            if (state == PublicationState.RESTORED) return@synchronized
            if (state != PublicationState.PREPARED) {
                check(entries[file] === entry) { "Raw file generation changed during publication." }
                for ((slot, original) in previous) {
                    val current = entry.slots.getValue(slot)
                    check(current === original || current === next.getValue(slot)) { "Raw usage slot changed outside its publication." }
                }
                for ((slot, original) in previous) entry.slots[slot] = original
            }
            state = PublicationState.RESTORED
        }
    }
    private fun createEntry(file: CfirFile, surfaces: List<MacroSurfaceExpr>): Entry {
        val slots = mutableListOf<CfirRawMacroUsageSlot>()
        // 需要按语义父类分派：扁平 VisitorVoid 的 visitNamedFunction 不调用 visitFunction。
        val visitor = object : CfirDefaultVisitorVoid() {
            override fun visitElement(element: CfirElement) = element.acceptChildren(this)

            override fun visitFunction(function: CfirFunction) {
                slots += CfirRawMacroUsageSlot(function, CfirRawMacroUsageSlot.Kind.BODY)
                function.valueParameters.forEach {
                    slots += CfirRawMacroUsageSlot(it, CfirRawMacroUsageSlot.Kind.PARAMETER_DEFAULT)
                    it.defaultValueMetadata?.function?.let { helper ->
                        slots += CfirRawMacroUsageSlot(helper, CfirRawMacroUsageSlot.Kind.BODY)
                    }
                }
                // 局部函数属于该实际正文/default槽位，不另用局部名字重新划分使用事实。
            }

            override fun visitVariable(variable: CfirVariable) {
                slots += CfirRawMacroUsageSlot(variable, CfirRawMacroUsageSlot.Kind.INITIALIZER)
            }

            override fun visitCodeFragment(codeFragment: CfirCodeFragment) {
                slots += CfirRawMacroUsageSlot(codeFragment, CfirRawMacroUsageSlot.Kind.CODE_FRAGMENT)
            }
        }
        file.accept(visitor)
        val assigned = assignSurfaces(slots, surfaces)
        val assignedIdentities = Collections.newSetFromMap(IdentityHashMap<MacroSurfaceExpr, Boolean>())
        assigned.values.forEach(assignedIdentities::addAll)
        // 文件header不是body替换槽位，采用正文时不撤销其中的原始使用事实。
        assigned[CfirRawMacroUsageSlot(file, CfirRawMacroUsageSlot.Kind.HEADER)] =
            immutable(surfaces.filter { it !in assignedIdentities })
        return Entry(file, assigned)
    }

    private fun assignSurfaces(
        slots: List<CfirRawMacroUsageSlot>,
        surfaces: List<MacroSurfaceExpr>,
    ): MutableMap<CfirRawMacroUsageSlot, List<MacroSurfaceExpr>> {
        val roots = slots.associateWith { reachable(it.currentRootOrNull()) }
        val result = slots.associateWithTo(LinkedHashMap()) { mutableListOf<MacroSurfaceExpr>() }
        val carriers = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
        for (surface in surfaces) {
            val carrier = surface.replaceHandle.carrier
            val owners = roots.filterValues { carrier != null && carrier in it }.keys
            require(owners.size <= 1) { "A macro carrier must belong to one actual replacement slot." }
            if (owners.isEmpty()) continue // 私有重建中未被采用的head/参数carrier不进入提交槽位。
            checkNotNull(carrier)
            require(carriers.add(carrier)) { "A macro carrier must have one usage fact in the adopted graph." }
            result.getValue(owners.single()) += surface
        }
        return result.mapValuesTo(LinkedHashMap()) { immutable(it.value) }
    }

    private fun reachable(root: CfirElement?): Set<Any> {
        val visited = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
        val visitor = object : CfirVisitorVoid() {
            override fun visitElement(element: CfirElement) {
                if (!visited.add(element)) return
                element.acceptChildren(this)
            }
        }
        root?.accept(visitor)
        return visited
    }

    private fun allSurfaces(entry: Entry?): List<MacroSurfaceExpr> =
        immutable(entry?.slots?.values?.flatten().orEmpty())

    private fun <T> immutable(values: Collection<T>): List<T> = Collections.unmodifiableList(ArrayList(values))
}

/** 实际文件槽位的准备、提交和撤销；调用方须在同一 session 锁内组合全部产品。 */
interface CfirRawMacroUsagePublication {
    fun validate(): Unit
    fun commit(): Unit
    fun restore(): Unit
}
