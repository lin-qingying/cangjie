package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.ScopeSessionKey
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase

/**
 * 已缓存 scope 所保障的最高成员阶段。
 *
 * 首次构建由 owner 解析和父 scope 构建保障层次；低阶段 scope 升级时必须先完成整个父层次。
 */
class CfirRequiredMembersPhaseStamp {
    @PublishedApi
    internal var isBuilt: Boolean = false

    @PublishedApi
    internal var phase: CfirResolvePhase? = null

    /** 已完成的最高阶段，供完成 scope 保持后续 session 重建的成员保障要求。 */
    val highestRequiredPhase: CfirResolvePhase?
        get() = phase
}

fun <ID : Any> requiredMembersPhaseStampKey(): ScopeSessionKey<ID, CfirRequiredMembersPhaseStamp> =
    object : ScopeSessionKey<ID, CfirRequiredMembersPhaseStamp>() {}

/** 在相同身份的 scope 缓存查询前，完成首次 owner 或升级后的层次成员阶段。 */
inline fun <reified ID : Any> ScopeSession.ensureRequiredMembersPhase(
    id: ID,
    key: ScopeSessionKey<ID, CfirRequiredMembersPhaseStamp>,
    requiredPhase: CfirResolvePhase?,
    resolveOwner: (CfirResolvePhase) -> Unit,
    resolveHierarchy: (CfirResolvePhase) -> Unit,
) {
    val stamp = getOrBuild(id, key) { CfirRequiredMembersPhaseStamp() }
    if (requiredPhase == null) {
        stamp.isBuilt = true
        return
    }

    val stampPhase = stamp.phase
    if (stampPhase != null && stampPhase >= requiredPhase) return

    if (stamp.isBuilt) {
        resolveHierarchy(requiredPhase)
    } else {
        resolveOwner(requiredPhase)
    }

    stamp.isBuilt = true
    stamp.phase = requiredPhase
}
