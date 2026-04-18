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

    val lazyDeclarationResolver = session.moduleComponents.firModuleLazyDeclarationResolver
    lazyDeclarationResolver.lazyResolveTarget(this, phase)
}

internal val LLCfirResolveTarget.session: LLCfirSession get() = target.llCfirSession
