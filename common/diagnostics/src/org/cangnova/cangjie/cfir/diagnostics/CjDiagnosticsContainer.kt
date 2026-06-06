

package org.cangnova.cangjie.cfir.diagnostics

import org.cangnova.cangjie.cfir.diagnostics.rendering.BaseDiagnosticRendererFactory

abstract class CjDiagnosticsContainer {
    /**
     * !!!! Don't convert this function to property, as it might lead to cyclic initialization problems !!!!
     */
    abstract fun getRendererFactory(): BaseDiagnosticRendererFactory
}

