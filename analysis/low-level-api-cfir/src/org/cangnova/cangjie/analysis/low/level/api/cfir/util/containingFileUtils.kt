/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.util

import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.llCfirResolvableSession
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.psi.CjFile

fun CfirElementWithResolveState.getContainingFile(): CfirFile? {
    val provider = moduleData.session.cfirProvider
    return when (this) {
        is CfirFile -> this
        is CfirTypeParameter -> containingDeclarationSymbol.cfir.getContainingFile()
        is CfirPropertyAccessor -> propertySymbol.cfir.getContainingFile()
        is CfirValueParameter -> containingDeclarationSymbol.cfir.getContainingFile()
        is CfirCallableDeclaration -> provider.getCfirCallableContainerFile(symbol)
        is CfirClassLikeDeclaration -> provider.getCfirClassifierContainerFileIfAny(symbol)
        is CfirExtend -> {
            // extend 在仓颉框架里是文件级声明，不属于 Kotlin FIR 现成的 callable/class-like 分支。
            // low-level API 必须直接复用模块缓存里的“声明 -> CfirFile”归属关系，不能把它落回 unsupported。
            llCfirResolvableSession?.moduleComponents?.cache?.getContainerCfirFile(this)
        }
        is CfirCodeFragment -> {
            val cjFile = psi?.containingFile as? CjFile
                ?: error("File for code fragment cannot be null")
            val moduleComponents = llCfirResolvableSession?.moduleComponents
                ?: error("LLCfirResolvableModuleSession for code fragment cannot be null")
            moduleComponents.cache.getCachedCfirFile(cjFile)
                ?: error("Cfir file for code fragment cannot be null")
        }
        else -> errorWithCfirSpecificEntries("Unsupported declaration ${this::class}", cfir = this)
    }
}
