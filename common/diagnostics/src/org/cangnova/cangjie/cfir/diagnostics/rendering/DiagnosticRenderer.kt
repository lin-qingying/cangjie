/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.diagnostics.rendering

import org.cangnova.cangjie.cfir.diagnostics.UnboundDiagnostic

/**
 * 将未绑定诊断渲染为用户可读消息的接口。
 */
interface DiagnosticRenderer<in D : UnboundDiagnostic> {
    /**
     * 渲染完整诊断消息文本。
     */
    fun render(diagnostic: D): String

    /**
     * 渲染诊断消息模板所需的参数数组。
     */
    fun renderParameters(diagnostic: D): Array<out Any?>
}
