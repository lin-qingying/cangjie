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

/**
 * 库或库源码模块的可解析 low-level CFIR session。
 *
 * 该 session 用于需要从库源码或库声明中继续 lazy resolve 的分析路径。
 */
internal class LLCfirLibraryOrLibrarySourceResolvableModuleSession(
    caModule: CaModule,

    /**
     * 当前库可解析 session 的模块解析组件。
     */
    override val moduleComponents: LLCfirModuleResolveComponents,
    builtinTypes: CfirBuiltinTypes,
) : LLCfirResolvableModuleSession(caModule, builtinTypes) {
    init {
        checkIsValidCjModule(caModule)
    }

    companion object {
        /**
         * 校验 [module] 是否可以创建库类可解析 session。
         */
        fun checkIsValidCjModule(module: CaModule) {
            require(module is CaLibraryModule || module is CaLibrarySourceModule || module is CaBuiltinsModule) {
                "Expected ${CaLibraryModule::class.simpleName} or ${CaLibrarySourceModule::class.simpleName}, but ${module::class.simpleName} found"
            }
        }
    }
}
