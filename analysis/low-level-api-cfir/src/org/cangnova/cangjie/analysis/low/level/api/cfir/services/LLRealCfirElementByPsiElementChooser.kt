/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.services

import org.cangnova.cangjie.analysis.low.level.api.cfir.api.services.LLCfirElementByPsiElementChooser
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.realPsi
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjTypeParameter

/**
 * 基于真实 PSI 身份匹配 CFIR 元素的 chooser。
 *
 * low-level API 可能同时存在原始 PSI 与复制 PSI；该实现只接受 [realPsi] 与传入 PSI 完全相同的 CFIR 元素。
 */
internal class LLRealCfirElementByPsiElementChooser : LLCfirElementByPsiElementChooser() {
    /**
     * 判断值参数 CFIR 是否来自指定 PSI 参数。
     */
    override fun isMatchingValueParameter(psi: CjParameter, fir: CfirValueParameter): Boolean = fir.realPsi === psi

    /**
     * 判断类型参数 CFIR 是否来自指定 PSI 类型参数。
     */
    override fun isMatchingTypeParameter(psi: CjTypeParameter, fir: CfirTypeParameter): Boolean = fir.realPsi === psi

    /**
     * 判断 callable CFIR 是否来自指定 PSI callable 声明。
     */
    override fun isMatchingCallableDeclaration(psi: CjCallableDeclaration, fir: CfirCallableDeclaration): Boolean = fir.realPsi === psi
}
