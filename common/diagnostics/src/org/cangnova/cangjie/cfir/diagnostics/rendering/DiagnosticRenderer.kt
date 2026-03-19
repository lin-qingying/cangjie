/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.diagnostics.rendering

import org.cangnova.cangjie.cfir.diagnostics.UnboundDiagnostic

interface DiagnosticRenderer<in D : UnboundDiagnostic> {
    fun render(diagnostic: D): String

    fun renderParameters(diagnostic: D): Array<out Any?>
}

