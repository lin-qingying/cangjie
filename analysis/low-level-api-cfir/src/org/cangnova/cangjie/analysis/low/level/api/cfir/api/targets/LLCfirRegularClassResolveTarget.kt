/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.withCfirDesignationEntry
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.CfirRegularClass
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment

internal sealed class LLCfirRegularClassResolveTarget(designation: CfirDesignation) : LLCfirResolveTarget(designation) {
    init {
        requireWithAttachment(
            target is CfirRegularClass,
            { "Expected type of '${::target.name}' is ${CfirRegularClass::class.simpleName}, but ${target::class.simpleName} is found" },
        ) {
            withCfirDesignationEntry("designation", this@LLCfirRegularClassResolveTarget.designation)
        }
    }

    abstract fun visitMembers(visitor: LLCfirResolveTargetVisitor, firRegularClass: CfirRegularClass)

    abstract val visitClass: Boolean

    final override fun visitTargetElement(
        element: CfirElementWithResolveState,
        visitor: LLCfirResolveTargetVisitor,
    ) {
        if (visitClass) {
            visitor.performAction(element)
        }

        visitor.withRegularClass(element as CfirRegularClass) {
            visitMembers(visitor, element)
        }
    }
}
