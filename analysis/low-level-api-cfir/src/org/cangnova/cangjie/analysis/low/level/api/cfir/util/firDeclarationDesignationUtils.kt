/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.util

import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.ResolveStateAccess
import org.cangnova.cangjie.cfir.expressions.withCfirEntry
import org.cangnova.cangjie.utils.exceptions.checkWithAttachment

internal fun CfirElementWithResolveState.checkPhase(requiredResolvePhase: CfirResolvePhase) {
    @OptIn(ResolveStateAccess::class)
    val declarationResolveState = resolveState
    checkWithAttachment(
        declarationResolveState.resolvePhase >= requiredResolvePhase,
        { "At least $requiredResolvePhase expected but $declarationResolveState found for ${this::class.simpleName}" },
    ) {
        withCfirEntry("firDeclaration", this@checkPhase)
    }
}
