@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.platform.modification.publishCodeFragmentContextModificationEvent
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.psi.CjElement

internal class LLCfirInBlockModificationListenerForCodeFragments(val project: Project) : LLCfirInBlockModificationListener {
    override fun afterModification(element: CjElement, module: CaModule) {
        module.publishCodeFragmentContextModificationEvent()
    }
}
