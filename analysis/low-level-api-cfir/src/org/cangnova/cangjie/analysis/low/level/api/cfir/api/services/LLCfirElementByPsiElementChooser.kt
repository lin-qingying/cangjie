/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.api.services

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnumEntry
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjEnumEntry
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjTypeParameter

/**
 * [LLCfirElementByPsiElementChooser] helps with choosing a [org.cangnova.cangjie.cfir.CfirElement] from multiple *sibling* CFIR elements that
 * matches a given [org.cangnova.cangjie.psi.CjElement].
 *
 * The chooser requires the CFIR elements to belong to the same conceptual parent (such as top-level scope, class, parameter list, etc.), as
 * the algorithm is only required to consider structural aspects of the given elements, but not their parents.
 *
 * Using this service only makes sense when a CFIR element may not have an underlying PSI, which may be the case for deserialized elements.
 * When elements are deserialized from stubs, this issue does not occur because the PSI is provided during deserialization. However, in
 * Standalone mode, deserialized symbols do not have sources and require more sophisticated choosing logic.
 */
abstract class LLCfirElementByPsiElementChooser {
    abstract fun isMatchingValueParameter(psi: CjParameter, fir: CfirValueParameter): Boolean

    abstract fun isMatchingTypeParameter(psi: CjTypeParameter, fir: CfirTypeParameter): Boolean

    abstract fun isMatchingEnumEntry(psi: CjEnumEntry, fir: CfirEnumEntry): Boolean

    abstract fun isMatchingCallableDeclaration(psi: CjCallableDeclaration, fir: CfirCallableDeclaration): Boolean

    companion object {
        fun getInstance(project: Project): LLCfirElementByPsiElementChooser =
            project.getService(LLCfirElementByPsiElementChooser::class.java)
    }
}
