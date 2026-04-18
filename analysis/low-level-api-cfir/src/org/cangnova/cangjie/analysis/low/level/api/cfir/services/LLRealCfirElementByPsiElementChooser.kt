/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.services

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.services.LLCfirElementByPsiElementChooser
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnumEntry
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.realPsi
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjEnumEntry
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjTypeParameter

internal class LLRealCfirElementByPsiElementChooser : LLCfirElementByPsiElementChooser() {
    override fun isMatchingValueParameter(psi: CjParameter, fir: CfirValueParameter): Boolean = fir.realPsi === psi

    override fun isMatchingTypeParameter(psi: CjTypeParameter, fir: CfirTypeParameter): Boolean = fir.realPsi === psi

    override fun isMatchingEnumEntry(psi: CjEnumEntry, fir: CfirEnumEntry): Boolean = fir.realPsi === psi

    override fun isMatchingCallableDeclaration(psi: CjCallableDeclaration, fir: CfirCallableDeclaration): Boolean = fir.realPsi === psi
}
