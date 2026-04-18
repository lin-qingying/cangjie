/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.util

import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.llCfirResolvableSession
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.utils.isLocal
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.resolve.getContainingClassSymbol
import org.cangnova.cangjie.cfir.resolve.providers.firProvider
import org.cangnova.cangjie.psi.CjFile

fun CfirElementWithResolveState.getContainingFile(): CfirFile? {
    val provider = moduleData.session.firProvider
    return when (this) {
        is CfirFile -> this
        is CfirScript -> provider.getCfirScriptContainerFile(symbol)
        is CfirTypeParameter -> containingDeclarationSymbol.cfir.getContainingFile()
        is CfirPropertyAccessor -> propertySymbol.cfir.getContainingFile()
        is CfirValueParameter -> containingDeclarationSymbol.cfir.getContainingFile()
        is CfirBackingField -> propertySymbol.cfir.getContainingFile()
        is CfirCallableDeclaration -> provider.getCfirCallableContainerFile(symbol)
        is CfirClassLikeDeclaration -> provider.getCfirClassifierContainerFileIfAny(symbol)
        is CfirAnonymousInitializer -> {
            if (getContainingClassSymbol()?.isLocal == true) {
                containingCjFileIfAny?.let {
                    val moduleComponents = llCfirResolvableSession?.moduleComponents
                    moduleComponents?.cache?.getCachedCfirFile(it)
                }
            } else {
                containingDeclarationSymbol.cfir.getContainingFile()
            }
        }
        is CfirDanglingModifierList, is CfirCodeFragment -> {
            val ktFile = psi?.containingFile as? CjFile
                ?: error("File for dangling modifier list cannot be null")
            val moduleComponents = llCfirResolvableSession?.moduleComponents
                ?: error("LLCfirResolvableModuleSession for dangling modifier list cannot be null")
            moduleComponents.cache.getCachedCfirFile(ktFile)
                ?: error("Cfir file for dandling modifier list cannot be null")
        }
        is CfirReceiverParameter -> containingDeclarationSymbol.cfir.getContainingFile()
        else -> errorWithCfirSpecificEntries("Unsupported declaration ${this::class}", fir = this)
    }
}
