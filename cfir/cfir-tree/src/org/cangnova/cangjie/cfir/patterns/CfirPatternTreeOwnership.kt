package org.cangnova.cangjie.cfir.patterns

import org.cangnova.cangjie.cfir.CfirElement

/**
 * 直接拥有一棵模式子树的 CFIR 节点。
 *
 * 根字段名由各宿主决定；生成的实现通过该方法向统一的 owner 协调逻辑暴露根节点。
 */
interface CfirPatternTreeOwner {
    /** 返回当前宿主实际拥有的模式根节点。 */
    fun patternTreeRoot(): CfirElement

    /**
     * 在修改任何 owner 前验证根节点符合该宿主生成字段的静态类型。
     *
     * 实现必须在写入前完成全部可失败校验；通过验证后，写入入口只能执行已验证的字段替换。
     */
    fun validatePatternTreeRoot(newPatternTreeRoot: CfirElement)

    /**
     * 通过宿主的生成式替换入口恢复或替换其模式根节点。
     *
     * 该操作属于多 owner 恢复事务的提交阶段；实现不得在写入时再次校验或抛出异常。
     */
    fun replacePatternTreeRoot(newPatternTreeRoot: CfirElement)
}

private val patternTreeRootRestoreInProgress = ThreadLocal.withInitial { false }

/** 生成的 owner/node setter 通过此标记在多 owner 恢复期间暂缓逐节点协调。 */
internal fun isPatternTreeRootRestoreInProgress(): Boolean = patternTreeRootRestoreInProgress.get()

/** 将根字段和反向 owner 链接作为一笔树所有权事务恢复。 */
internal fun <T> withPatternTreeRootRestore(action: () -> T): T {
    val previous = patternTreeRootRestoreInProgress.get()
    patternTreeRootRestoreInProgress.set(true)
    return try {
        action()
    } finally {
        if (previous) patternTreeRootRestoreInProgress.set(true) else patternTreeRootRestoreInProgress.remove()
    }
}

/**
 * 可以作为模式树组成部分并记录唯一宿主的 CFIR 节点。
 *
 * pattern 节点和其中声明的绑定变量都实现此接口，使复制、替换和变换共用同一套所有权不变量。
 */
interface CfirPatternTreeNode {
    /** 当前节点所属模式树宿主；反向链接不是 CFIR 子节点。 */
    val patternTreeOwner: CfirPatternTreeOwner?

    /**
     * 仅供树协调器在验证后建立或解除反向 owner 链接；实现必须只写入反向链接，不得在提交阶段失败。
     */
    fun replacePatternTreeOwner(newPatternTreeOwner: CfirPatternTreeOwner?)
}
