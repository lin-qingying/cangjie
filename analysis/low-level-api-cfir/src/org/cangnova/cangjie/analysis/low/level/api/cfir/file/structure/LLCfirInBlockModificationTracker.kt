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

    /**
     * 订阅块内修改事件并递增工程级修改计数的监听器。
     *
     * @param project 当前监听器所属工程，用于按需取得已创建的 [LLCfirInBlockModificationTracker] 服务。
     */
    internal class Listener(val project: Project) : LLCfirInBlockModificationListener {
        /**
         * 在块内修改完成后递增低阶 CFIR 的全局块内修改计数。
         *
         * 如果 tracker 服务尚未创建，则不主动创建服务，避免单纯的 PSI 修改事件拉起不必要的工程级状态。
         */
        override fun afterModification(element: CjElement, module: CaModule) {
            project.serviceIfCreated<LLCfirInBlockModificationTracker>()?.incModificationCount()
        }
    }
}
