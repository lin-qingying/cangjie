/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibrarySourceModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.cfir.session.CfirBuiltinTypes

internal class LLCfirLibraryOrLibrarySourceResolvableModuleSession(
    ktModule: CaModule,
    override val moduleComponents: LLCfirModuleResolveComponents,
    builtinTypes: CfirBuiltinTypes,
) : LLCfirResolvableModuleSession(ktModule, builtinTypes) {
    init {
        checkIsValidCjModule(ktModule)
    }

    companion object {
        fun checkIsValidCjModule(module: CaModule) {
            require(module is CaLibraryModule || module is CaLibrarySourceModule || module is CaBuiltinsModule) {
                "Expected ${CaLibraryModule::class.simpleName} or ${CaLibrarySourceModule::class.simpleName}, but ${module::class.simpleName} found"
            }
        }
    }
}
