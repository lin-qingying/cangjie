

package org.cangnova.cangjie.cfir.diagnostics

import org.cangnova.cangjie.cfir.diagnostics.rendering.BaseDiagnosticRendererFactory

/**
 * 诊断集合容器，负责暴露与该集合匹配的渲染器工厂。
 */
abstract class CjDiagnosticsContainer {
    /**
     * !!!! Don't convert this function to property, as it might lead to cyclic initialization problems !!!!
     */
    abstract fun getRendererFactory(): BaseDiagnosticRendererFactory
}
