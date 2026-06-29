/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.withCfirDesignationEntry
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment

/**
 * class 类 resolve target 的公共基类，负责校验目标类型并在 class 作用域下访问成员。
 */
internal sealed class LLCfirClassResolveTarget(designation: CfirDesignation) : LLCfirResolveTarget(designation) {
    init {
        requireWithAttachment(
            target is CfirClass,
            { "Expected type of '${::target.name}' is ${CfirClass::class.simpleName}, but ${target::class.simpleName} is found" },
        ) {
            withCfirDesignationEntry("designation", this@LLCfirClassResolveTarget.designation)
        }
    }

    /**
     * 在已经进入 class 上下文后访问该 target 需要解析的成员集合。
     */
    abstract fun visitMembers(visitor: LLCfirResolveTargetVisitor, cfirClass: CfirClass)

    /**
     * 当前 target 是否需要对 class 声明本身执行 resolve 动作。
     */
    abstract val visitClass: Boolean

    /**
     * 进入 class 上下文，按 [visitClass] 与 [visitMembers] 的策略访问具体目标。
     */
    final override fun visitTargetElement(
        element: CfirElementWithResolveState,
        visitor: LLCfirResolveTargetVisitor,
    ) {
        if (visitClass) {
            visitor.performAction(element)
        }

        visitor.withClass(element as CfirClass) {
            visitMembers(visitor, element)
        }
    }
}
