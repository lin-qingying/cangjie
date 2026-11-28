@file:OptIn(org.cangnova.cangjie.analysis.api.CaNonPublicApi::class)

package org.cangnova.cangjie.analysis.api.renderer.declarations

import org.cangnova.cangjie.analysis.api.CaNonPublicApi
import org.cangnova.cangjie.analysis.api.symbols.CaClassKind
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFieldSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaLocalVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPatternBindingSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaTypeAliasSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.analysis.api.renderer.types.renderers.CaTypeNameRenderer
import org.cangnova.cangjie.analysis.api.types.CaClassLikeType
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaTypeParameterType
import org.cangnova.cangjie.rendering.CangJieSemanticKind

/**
 * 让通用声明 renderer 在写出名字和类型名时记录语言语义类别。
 *
 * 该 renderer 不选择颜色、不生成 HTML，也不区分输出宿主；调用方可在一次文本渲染中取得语义范围。
 */
@CaNonPublicApi
fun CaDeclarationRenderer.withSemanticAnnotations(): CaDeclarationRenderer {
    val originalNameRenderer = nameRenderer
    val originalTypeNameRenderer = typeRenderer.typeNameRenderer

    return with {
        nameRenderer = CaDeclarationNameRenderer { analysisSession, name, symbol, declarationRenderer, printer ->
            val semanticKind = symbol?.semanticKind()
            if (semanticKind == null) {
                originalNameRenderer.renderName(analysisSession, name, symbol, declarationRenderer, printer)
            } else {
                printer.withSemanticAnnotation(semanticKind) {
                    originalNameRenderer.renderName(analysisSession, name, symbol, declarationRenderer, this)
                }
            }
        }
        typeRenderer = typeRenderer.with {
            typeNameRenderer = CaTypeNameRenderer { analysisSession, name, owner, renderer, printer ->
                printer.withSemanticAnnotation(owner.semanticKind()) {
                    originalTypeNameRenderer.renderName(analysisSession, name, owner, renderer, this)
                }
            }
        }
    }
}

private fun CaNamedSymbol.semanticKind(): CangJieSemanticKind = when (this) {
    is CaClassSymbol -> classKind.semanticKind()
    is CaTypeAliasSymbol -> CangJieSemanticKind.TYPE_ALIAS
    is CaTypeParameterSymbol -> CangJieSemanticKind.TYPE_PARAMETER
    is CaNamedFunctionSymbol -> CangJieSemanticKind.FUNCTION
    is CaValueParameterSymbol -> CangJieSemanticKind.PARAMETER
    is CaPropertySymbol -> CangJieSemanticKind.PROPERTY
    is CaFieldSymbol -> CangJieSemanticKind.FIELD
    // 单名与解构叶子都由真实绑定符号提供名字语义；容器不制造变量名。
    is CaPatternBindingSymbol -> if (location == CaSymbolLocation.TOP_LEVEL) {
        CangJieSemanticKind.PACKAGE_VARIABLE
    } else {
        CangJieSemanticKind.LOCAL_VARIABLE
    }
    is CaLocalVariableSymbol -> CangJieSemanticKind.LOCAL_VARIABLE
    is CaPackageSymbol -> CangJieSemanticKind.PACKAGE
    else -> CangJieSemanticKind.OTHER
}

private fun CaType.semanticKind(): CangJieSemanticKind = when (this) {
    is CaTypeParameterType -> CangJieSemanticKind.TYPE_PARAMETER
    is CaClassLikeType -> symbol?.semanticKind() ?: CangJieSemanticKind.CLASS
    else -> CangJieSemanticKind.CLASS
}

private fun CaClassKind.semanticKind(): CangJieSemanticKind = when (this) {
    CaClassKind.CLASS, CaClassKind.PRIMITIVE -> CangJieSemanticKind.CLASS
    CaClassKind.INTERFACE -> CangJieSemanticKind.INTERFACE
    CaClassKind.STRUCT -> CangJieSemanticKind.STRUCT
    CaClassKind.ENUM -> CangJieSemanticKind.ENUM
}
