/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.api.targets

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.llCfirResolvableSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.llCfirSession
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment

/**
 * 将 designation 包装为单目标 lazy resolve target。
 */
internal fun CfirDesignation.asResolveTarget(): LLCfirSingleResolveTarget = LLCfirSingleResolveTarget(this)

/**
 * Resolves the target to the specified [phase].
 * The owning session must be a resolvable one.
 */
internal fun LLCfirResolveTarget.resolve(phase: CfirResolvePhase) {
    val session = target.llCfirResolvableSession
        ?: errorWithAttachment("Resolvable session expected, got '${target.llCfirSession::class.java}'") {
            withEntry("firSession", target.llCfirSession) { it.toString() }
        }

    val lazyDeclarationResolver = session.moduleComponents.cfirModuleLazyDeclarationResolver
    lazyDeclarationResolver.lazyResolveTarget(this, phase)
}

/**
 * 取得 resolve target 所属的 low-level session。
 */
internal val LLCfirResolveTarget.session: LLCfirSession get() = target.llCfirSession
