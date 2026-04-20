/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure

import com.intellij.openapi.components.service
import com.intellij.openapi.components.serviceIfCreated
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.util.SimpleModificationTracker
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.psi.CjElement

/**
 * Global in-block modification tracker.
 *
 * This tracker increments each time in-block modification happens somewhere in the code.
 *
 * @see LLCfirDeclarationModificationService
 */

class LLCfirInBlockModificationTracker : SimpleModificationTracker() {
    companion object {
        fun getInstance(project: Project): ModificationTracker = project.service<LLCfirInBlockModificationTracker>()
    }

    internal class Listener(val project: Project) : LLCfirInBlockModificationListener {
        override fun afterModification(element: CjElement, module: CaModule) {
            project.serviceIfCreated<LLCfirInBlockModificationTracker>()?.incModificationCount()
        }
    }
}
