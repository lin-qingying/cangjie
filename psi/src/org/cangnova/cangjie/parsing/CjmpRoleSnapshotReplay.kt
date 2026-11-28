package org.cangnova.cangjie.parsing

import org.cangnova.cangjie.CjmpFileRoleMode
import org.cangnova.cangjie.CjmpFileRoleReplay
import org.cangnova.cangjie.CjmpRoleEvent
import org.cangnova.cangjie.CjmpRoleEventId
import org.cangnova.cangjie.CjmpRoleEventResult
import org.cangnova.cangjie.CjmpRoleInputCompleteness
import org.cangnova.cangjie.CjmpRoleReplayResult
import org.cangnova.cangjie.CjmpRoleSyntaxIdentity
import org.cangnova.cangjie.LanguageVersionSettings

/** 单次快照在目标语境中的重放结果；不能与另一解析代次的事件混用。 */
class CjmpRoleSnapshotReplay private constructor(
    private val snapshot: CjmpFileRoleSyntaxSnapshot,
    val result: CjmpRoleReplayResult<CjmpParsedRoleEventId>,
) {
    private val resultsById = result.events.associateBy { it.event.id.value }

    /** 仅对属于该快照且保持检查顺序的非空事件返回结果；没有身份时返回未知。 */
    fun resultsFor(events: List<CjmpParsedRoleEvent>): List<CjmpRoleEventResult<CjmpParsedRoleEventId>>? {
        if (events.isEmpty()) return null
        if (events.any { !snapshot.containsEventIdentity(it) }) return null
        if (events.zipWithNext().any { (left, right) ->
                checkNotNull(snapshot.eventOrder(left)) >= checkNotNull(snapshot.eventOrder(right))
            }) return null
        return events.map { checkNotNull(resultsById[it.id]) }
    }

    companion object {
        /** 语法是否等价由拥有两次解析语境的调用方证明，不能从文本或事件偏移推断。 */
        fun replay(
            snapshot: CjmpFileRoleSyntaxSnapshot,
            mode: CjmpFileRoleMode,
            languageVersionSettings: LanguageVersionSettings,
            syntaxIdentity: CjmpRoleSyntaxIdentity,
        ): CjmpRoleSnapshotReplay {
            val events = snapshot.events.mapIndexed { index, event ->
                CjmpRoleEvent(CjmpRoleEventId(event.id), index.toLong(), event.kind)
            }
            val initial = snapshot.entryFileRole.takeUnless { snapshot.completeness == CjmpRoleInputCompleteness.PARTIAL }
            return CjmpRoleSnapshotReplay(snapshot, CjmpFileRoleReplay.replay(
                mode, initial, snapshot.completeness, events, languageVersionSettings, syntaxIdentity,
            ))
        }
    }
}
