/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirRegularClass

/**
 * Resolves [members] of [designation]. Ignores the class itself.
 */
internal class LLCfirClassSpecificMembersResolveTarget(
    designation: CfirDesignation,
    val members: List<CfirDeclaration>,
) : LLCfirRegularClassResolveTarget(designation) {
    override val visitClass: Boolean get() = false
    override fun visitMembers(visitor: LLCfirResolveTargetVisitor, firRegularClass: CfirRegularClass) {
        members.forEach(visitor::performAction)
    }

    override fun toStringAdditionalSuffix(): String = members.joinToString(prefix = "[", postfix = "]") { it.symbol.toString() }
}
