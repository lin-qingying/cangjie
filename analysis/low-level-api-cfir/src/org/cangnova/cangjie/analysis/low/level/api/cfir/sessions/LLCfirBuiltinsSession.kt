/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.cfir.PrivateSessionConstructor
import org.cangnova.cangjie.cfir.session.CfirBuiltinTypes

/**
 * Builtins 模块使用的 low-level CFIR session。
 *
 * Builtins session 按库类 session 处理，不参与源码 lazy resolve，只承载内建类型和 builtins symbol provider。
 */
class LLCfirBuiltinsSession @PrivateSessionConstructor constructor(
    caModule: CaModule,
    builtinTypes: CfirBuiltinTypes,
) : LLCfirLibraryLikeSession(caModule, builtinTypes)
