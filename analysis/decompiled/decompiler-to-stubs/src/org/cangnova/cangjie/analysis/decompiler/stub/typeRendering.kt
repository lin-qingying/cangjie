package org.cangnova.cangjie.analysis.decompiler.stub

import org.cangnova.cangjie.cfir.renderer.CfirRenderer
import org.cangnova.cangjie.cfir.types.CfirTypeRef

/**
 * stub 构建阶段只保留类型文本辅助。
 *
 * package 级 decompiled 文本构建属于 decompiler-to-psi/text，
 * 这里不能继续承载整文件文本协议。
 */
private val decompiledTypeRenderer: CfirRenderer = CfirRenderer.withReadability()

internal fun renderDecompiledTypeRef(typeRef: CfirTypeRef): String {
    return decompiledTypeRenderer.renderElementAsString(typeRef)
}
