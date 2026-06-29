/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration

/**
 * Resolves [members] of [designation]. Ignores the class itself.
 */
internal class LLCfirClassSpecificMembersResolveTarget(
    designation: CfirDesignation,
    /**
     * 本次 resolve 需要访问的 class 成员声明列表。
     */
    val members: List<CfirDeclaration>,
) : LLCfirClassResolveTarget(designation) {
    /**
     * 只解析指定成员，不对 class 本身执行目标动作。
     */
    override val visitClass: Boolean get() = false

    /**
     * 将 visitor 动作应用到本 target 持有的指定成员列表。
     */
    override fun visitMembers(visitor: LLCfirResolveTargetVisitor, cfirClass: CfirClass) {
        members.forEach(visitor::performAction)
    }

    /**
     * 输出指定成员 symbol 列表，辅助区分同一个 class 上的不同成员 target。
     */
    override fun toStringAdditionalSuffix(): String = members.joinToString(prefix = "[", postfix = "]") { it.symbol.toString() }
}
