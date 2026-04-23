package org.cangnova.cangjie.analysis.api.renderer.declarations

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotated
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.types.CaType


@CaExperimentalApi
public object CaRecommendedRendererCodeStyle : CaRendererCodeStyle {
    override fun getIndentSize(analysisSession: CaSession): Int = 4

    override fun getSeparatorAfterContextReceivers(analysisSession: CaSession): String = "\n"

    override fun getSeparatorBetweenAnnotationAndOwner(analysisSession: CaSession, symbol: CaAnnotated): String = when (symbol) {
        is CaType -> " "
        is CaTypeParameterSymbol -> " "
        is CaParameterSymbol -> " "
        else -> "\n"
    }

    override fun getSeparatorBetweenAnnotations(analysisSession: CaSession, symbol: CaAnnotated): String = when (symbol) {
        is CaType -> " "
        is CaTypeParameterSymbol -> " "
        is CaParameterSymbol -> " "
        else -> "\n"
    }

    override fun getSeparatorBetweenModifiers(analysisSession: CaSession): String = " "

    override fun getSeparatorBetweenMembers(analysisSession: CaSession, first: CaDeclarationSymbol, second: CaDeclarationSymbol): String {
        return when {
            first is CaEnumConstructorSymbol && second is CaEnumConstructorSymbol -> ",\n"
            first is CaEnumConstructorSymbol && second !is CaEnumConstructorSymbol -> ";\n\n"
            else -> "\n\n"
        }
    }


}
