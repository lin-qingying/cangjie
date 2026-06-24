package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.renderer.CfirDeclarationRendererWithAttributes
import org.cangnova.cangjie.cfir.renderer.CfirRenderer
import org.cangnova.cangjie.cfir.renderer.CfirResolvePhaseRenderer
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.utils.exceptions.ExceptionAttachmentBuilder
import org.cangnova.cangjie.utils.exceptions.withCfirEntry

/**
 * 在异常 attachment 中加入符号绑定的 CFIR 声明。
 */
fun ExceptionAttachmentBuilder.withCfirSymbolEntry(name: String, symbol: CfirBasedSymbol<*>) {
    withCfirEntry("${name}Cfir", symbol.cfir)
}

/**
 * 在异常 attachment 中加入模块数据摘要。
 */
fun ExceptionAttachmentBuilder.withModuleDataEntry(name: String, moduleData: CfirModuleData?) {
    withEntry(name, moduleData) { module ->
        buildString {
            append("Name: ${module.name}, ")
            append("Platform: ${module.platform}")
        }
    }
}

/**
 * 在异常 attachment 中加入 CFIR 元素的渲染结果、元素类型、模块数据和源码片段。
 */
fun ExceptionAttachmentBuilder.withCfirEntry(name: String, fir: CfirElement?) {
    withEntry(name, fir) { fir ->
        CfirRenderer(
            resolvePhaseRenderer = CfirResolvePhaseRenderer(),
            declarationRenderer = CfirDeclarationRendererWithAttributes(),
        ).renderElementAsString(fir)
    }

    if (fir != null) {
        withEntry("${name}ElementKind", fir.source?.kind?.let { it::class.simpleName })
        if (fir is CfirElementWithResolveState) {
            withModuleDataEntry("${name}ModuleData", fir.moduleData)
        }
        withSourceEntry("${name}Source", fir.source)
    }
}


/**
 * 在异常 attachment 中加入源码上下文片段。
 */
fun ExceptionAttachmentBuilder.withSourceEntry(name: String, source: CjSourceElement?) {
    withEntry(name, source) { it.getElementTextInContextForDebug() }
}
