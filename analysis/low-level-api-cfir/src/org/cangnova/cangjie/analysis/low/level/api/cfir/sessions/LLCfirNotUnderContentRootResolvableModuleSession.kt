/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.api.projectStructure.CaNotUnderContentRootModule
import org.cangnova.cangjie.cfir.PrivateSessionConstructor
import org.cangnova.cangjie.cfir.session.CfirBuiltinTypes

/**
 * 不在内容根内的源码模块可解析 session。
 *
 * 这类 session 仍按源码 session 方式进行 lazy resolve，但模块来源不是常规 source root。
 */
internal class LLCfirNotUnderContentRootResolvableModuleSession @PrivateSessionConstructor constructor(
    caModule: CaNotUnderContentRootModule,

    /**
     * 当前 session 的模块解析组件。
     */
    override val moduleComponents: LLCfirModuleResolveComponents,
    builtinTypes: CfirBuiltinTypes,
) : LLCfirResolvableModuleSession(caModule, builtinTypes)
