/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.forEachDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClass

/**
 * [LLCfirResolveTarget] representing a class with all callable members (functions and variables).
 */
internal class LLCfirClassWithAllCallablesResolveTarget(designation: CfirDesignation) : LLCfirClassResolveTarget(designation) {
    /**
     * 解析 class 本身，同时继续访问其 callable 成员。
     */
    override val visitClass: Boolean get() = true

    /**
     * 遍历 class 中所有 callable 声明并交给 visitor 执行 resolve 动作。
     */
    override fun visitMembers(visitor: LLCfirResolveTargetVisitor, cfirClass: CfirClass) {
        cfirClass.forEachDeclaration {
            if (it is CfirCallableDeclaration) {
                visitor.performAction(it)
            }
        }
    }
}
