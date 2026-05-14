package org.cangnova.cangjie.analysis.api.renderer.declarations.superTypes

/**
 * 面向源码风格的超类型 renderer 预设。
 *
 * 对齐 Kotlin Analysis API 的 `KaSuperTypeRendererForSource`。
 */
object CaSuperTypeRendererForSource {
    /**
     * 预设: 先经过 [CaDeclarationRenderer.declarationTypeApproximator] 近似化处理,
     * 再交由 typeRenderer 写出。
     *
     * 适合需要把内部类型(如反向投影)近似为可在源码中书写的类型的场景。
     */
    val WITH_OUT_APPROXIMATION: CaSuperTypeRenderer = CaSuperTypeRenderer { analysisSession, superType, declarationRenderer, printer ->
        printer {
            declarationRenderer.typeRenderer.renderType(
                analysisSession,
                declarationRenderer.declarationTypeApproximator.approximateType(
                    analysisSession,
                    superType,
                ),
                this,
            )
        }
    }
}
