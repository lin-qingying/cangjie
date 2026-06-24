package org.cangnova.cangjie.cfir.render

import org.cangnova.cangjie.cfir.types.ConeCangJieType

/**
 * 面向 [ConeCangJieType.renderForDebugging] 的轻量调试渲染器。
 *
 * 与 debug-info 渲染器相比，它使用更短的 ID 文本，并且只保留非编译器内部属性。
 */
class ConeTypeRendererForDebugging() : ConeTypeRenderer() {

    /**
     * 使用既有 [builder] 初始化调试渲染器。
     */
    constructor(builder: StringBuilder) : this() {
        this.builder = builder
        this.idRenderer = ConeIdRendererForDebugging()
        idRenderer.builder = builder
    }

    /**
     * 调试文本不渲染编译器内部属性。
     */
    override fun ConeCangJieType.renderAttributes() {
        renderNonCompilerAttributes()
    }
}
