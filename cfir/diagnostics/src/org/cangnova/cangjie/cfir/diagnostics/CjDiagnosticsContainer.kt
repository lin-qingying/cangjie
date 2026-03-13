/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.cfir.diagnostics

import org.cangnova.cangjie.cfir.diagnostics.rendering.BaseDiagnosticRendererFactory

abstract class CjDiagnosticsContainer {
    /**
     * !!!! Don't convert this function to property, as it might lead to cyclic initialization problems !!!!
     */
    abstract fun getRendererFactory(): BaseDiagnosticRendererFactory
}

