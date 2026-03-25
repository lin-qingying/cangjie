package org.cangnova.cangjie.cfir.render

import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType

/**
 * 调试输出专用类型渲染器。
 *
 * 与默认渲染器相比：
 * 1. 默认使用全限定名渲染 class/alias ID；
 * 2. 保留可读性属性渲染；
 * 3. typealias 会附带展开结果，方便定位“别名壳”和“真实类型”。
 */
open class ConeTypeRendererForDebugInfo protected constructor(
    renderCapturedDetails: Boolean = false,
    coneAttributeRendererForReadability: ConeAttributeRenderer = ConeAttributeRenderer.ForReadability,
) : ConeTypeRenderer(coneAttributeRendererForReadability, renderCapturedDetails) {
    constructor(builder: StringBuilder, renderCapturedDetails: Boolean = false) : this(renderCapturedDetails) {
        this.builder = builder
        idRenderer = ConeFullyQualifiedIdRenderer().also { it.builder = builder }
    }

    override fun ConeCangJieType.renderAttributes() {
        renderNonCompilerAttributes()
    }

    override fun renderTypeAliasType(type: ConeTypeAliasType) {
        super.renderTypeAliasType(type)
        type.expandedType?.let { expanded ->
            builder.append(" /*= ")
            render(expanded)
            builder.append(" */")
        }
    }
}
