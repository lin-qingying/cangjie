/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.sessions

import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.api.projectStructure.CaNotUnderContentRootModule
import org.cangnova.cangjie.cfir.BuiltinTypes
import org.cangnova.cangjie.cfir.PrivateSessionConstructor

internal class LLCfirNotUnderContentRootResolvableModuleSession @PrivateSessionConstructor constructor(
    ktModule: CaNotUnderContentRootModule,
    override val moduleComponents: LLCfirModuleResolveComponents,
    builtinTypes: BuiltinTypes,
) : LLCfirResolvableModuleSession(ktModule, builtinTypes)
