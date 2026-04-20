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

fun ExceptionAttachmentBuilder.withCfirSymbolEntry(name: String, symbol: CfirBasedSymbol<*>) {
    withCfirEntry("${name}Cfir", symbol.cfir)
}

fun ExceptionAttachmentBuilder.withModuleDataEntry(name: String, moduleData: CfirModuleData?) {
    withEntry(name, moduleData) { module ->
        buildString {
            append("Name: ${module.name}, ")
            append("Platform: ${module.platform}")
        }
    }
}

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


fun ExceptionAttachmentBuilder.withSourceEntry(name: String, source: CjSourceElement?) {
    withEntry(name, source) { it.getElementTextInContextForDebug() }
}
