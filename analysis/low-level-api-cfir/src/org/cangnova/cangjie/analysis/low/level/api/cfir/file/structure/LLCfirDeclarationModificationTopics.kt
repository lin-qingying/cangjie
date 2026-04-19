/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure

import com.intellij.util.messages.Topic
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.psi.CjElement

/**
 * @see org.cangnova.cangjie.analysis.api.platform.modification.KotlinModificationEvent
 * @see LLCfirDeclarationModificationService
 * */
internal object LLCfirDeclarationModificationTopics {
    /**
     * @see org.cangnova.cangjie.analysis.api.platform.modification.CaSourceModificationLocality.InBlock
     */
    val IN_BLOCK_MODIFICATION: Topic<LLCfirInBlockModificationListener> = Topic(
        /* listenerClass = */ LLCfirInBlockModificationListener::class.java,
        /* broadcastDirection = */ Topic.BroadcastDirection.TO_CHILDREN,
        /* immediateDelivery = */ true,
    )
}

/**
 * @see LLCfirDeclarationModificationTopics.IN_BLOCK_MODIFICATION
 * @see org.cangnova.cangjie.analysis.api.platform.modification.CaSourceModificationLocality.InBlock
 */

interface LLCfirInBlockModificationListener {
    /**
     * @param element the element where the in-block modification happened
     * @param module the module where the modification happened
     */
    fun afterModification(element: CjElement, module: CaModule)
}
