package org.cangnova.cangjie.cfir.entrypoint.session

import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * 默认会话实现。
 * 对齐 Kotlin 声明：`org.jetbrains.kotlin.fir.java.FirCliSession`（此处仅复用“具体会话”角色，不引入 CLI 语义）。
 */
class CfirDefaultSession(
    /**
     * 会话种类（Source/Library）。
     */
    kind:  Kind,
) : CfirSession(kind)
