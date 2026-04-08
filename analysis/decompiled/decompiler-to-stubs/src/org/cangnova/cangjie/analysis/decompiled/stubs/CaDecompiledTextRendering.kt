package org.cangnova.cangjie.analysis.decompiled.stubs

import org.cangnova.cangjie.analysis.decompiled.filestubs.CaLoadedCjoPackage
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.renderer.CfirRenderer
import org.cangnova.cangjie.cfir.types.CfirTypeRef

/**
 * decompiled 文本渲染统一入口。
 *
 * `analysis/decompiled` 现在同时服务：
 * 1. project 级 decompiled PSI / text
 * 2. standalone binary decompiler
 * 3. 基于 compiled stub 的调试与导出
 *
 * 这些入口必须共享同一份文本协议，避免在多个模块里各自拼接“看起来一样”的反编译文本。
 */
object CaDecompiledTextRendering {
    private val readableRenderer: CfirRenderer = CfirRenderer.withReadability()

    fun renderPackageText(
        loadedPackage: CaLoadedCjoPackage,
        declarations: List<CfirDeclaration>,
    ): String {
        return buildString {
            appendLine("// Decompiled from .cjo package: ${loadedPackage.header.fullPkgName}")
            appendLine("package ${loadedPackage.header.fullPkgName}")
            if (loadedPackage.header.decompiledImportTexts.isNotEmpty()) {
                appendLine()
                loadedPackage.header.decompiledImportTexts.forEach { renderedImport ->
                    appendLine("import $renderedImport")
                }
            }
            declarations.forEach { declaration ->
                appendLine()
                appendLine(readableRenderer.renderElementAsString(declaration))
            }
        }.trimEnd() + System.lineSeparator()
    }

    fun renderTypeRef(typeRef: CfirTypeRef): String {
        return readableRenderer.renderElementAsString(typeRef)
    }
}
