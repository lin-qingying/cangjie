package org.cangnova.cangjie.analysis.api.renderer.declarations

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.CaAnnotationRenderer
import org.cangnova.cangjie.analysis.api.renderer.base.CaKeywordsRenderer
import org.cangnova.cangjie.analysis.api.renderer.base.CaPrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.base.prettyPrint
import org.cangnova.cangjie.analysis.api.renderer.types.CaRendererTypeApproximator
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaAnonymousFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassKind
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumEntrySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFieldSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFinalizerSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaScriptSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeAliasSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.name

class CaDeclarationRenderer private constructor(
    val nameRenderer: CaDeclarationNameRenderer,
    val keywordsRenderer: CaKeywordsRenderer,
    val codeStyle: CaRendererCodeStyle,
    val typeRenderer: CaTypeRenderer,
    val annotationRenderer: CaAnnotationRenderer,
    val modifiersRenderer: CaDeclarationModifiersRenderer,
    val declarationTypeApproximator: CaRendererTypeApproximator,
    val classifierBodyRenderer: CaClassifierBodyRenderer,
    val superTypeRenderer: CaSuperTypeRenderer,
    val superTypeListRenderer: CaSuperTypeListRenderer,
    val superTypesFilter: CaSuperTypesFilter,
    val bodyMemberScopeProvider: CaRendererBodyMemberScopeProvider,
    val bodyMemberScopeSorter: CaRendererBodyMemberScopeSorter,
    val functionLikeBodyRenderer: CaFunctionLikeBodyRenderer,
    val variableInitializerRenderer: CaVariableInitializerRenderer,
    val parameterDefaultValueRenderer: CaParameterDefaultValueRenderer,
    val propertyAccessorsRenderer: CaPropertyAccessorsRenderer,
    val accessorBodyRenderer: CaPropertyAccessorBodyRenderer,
    val returnTypeRenderer: CaCallableReturnTypeRenderer,
    val callableReceiverRenderer: CaCallableReceiverRenderer,
    val valueParametersRenderer: CaCallableParameterRenderer,
    val typeParametersRenderer: CaTypeParametersRenderer,
    val typeParametersFilter: CaTypeParametersFilter,
    val callableSignatureRenderer: CaCallableSignatureRenderer,
    val returnTypeFilter: CaCallableReturnTypeFilter,
    val functionLikeKeywordRenderer: CaFunctionLikeKeywordRenderer,
    val classLikeRenderer: CaClassLikeSymbolRenderer,
    val typeAliasRenderer: CaTypeAliasSymbolRenderer,
    val extendRenderer: CaExtendSymbolRenderer,
    val namedFunctionRenderer: CaNamedFunctionSymbolRenderer,
    val constructorRenderer: CaConstructorSymbolRenderer,
    val propertyRenderer: CaPropertySymbolRenderer,
    val fieldRenderer: CaFieldSymbolRenderer,
    val localVariableRenderer: CaLocalVariableSymbolRenderer,
    val enumEntryRenderer: CaEnumEntrySymbolRenderer,
    val valueParameterRenderer: CaValueParameterSymbolRenderer,
    val typeParameterRenderer: CaTypeParameterSymbolRenderer,
    val scriptRenderer: CaScriptSymbolRenderer,
) {
    fun renderDeclaration(
        analysisSession: CaSession,
        symbol: CaDeclarationSymbol,
    ): String = prettyPrint {
        renderDeclaration(analysisSession, symbol, this)
    }

    fun renderDeclaration(
        analysisSession: CaSession,
        symbol: CaDeclarationSymbol,
        printer: CaPrettyPrinter,
    ) {
        when (symbol) {
            is CaClassSymbol -> classLikeRenderer.renderSymbol(
                analysisSession,
                symbol,
                symbol.classKind.keyword(),
                this,
                printer,
            )

            is CaTypeAliasSymbol -> typeAliasRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaExtendSymbol -> extendRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaNamedFunctionSymbol -> namedFunctionRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaConstructorSymbol -> constructorRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaFinalizerSymbol -> functionLikeKeywordRenderer.renderFunctionLike(analysisSession, symbol, "finalizer", this, printer)
            is CaAnonymousFunctionSymbol -> functionLikeKeywordRenderer.renderFunctionLike(analysisSession, symbol, "func", this, printer)
            is CaEnumEntrySymbol -> enumEntryRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaPropertySymbol -> propertyRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaFieldSymbol -> fieldRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaValueParameterSymbol -> valueParameterRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaVariableSymbol -> localVariableRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaTypeParameterSymbol -> typeParameterRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaScriptSymbol -> scriptRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaFunctionSymbol -> functionLikeKeywordRenderer.renderFunctionLike(analysisSession, symbol, "func", this, printer)
            else -> {
                modifiersRenderer.renderModifiers(analysisSession, symbol, this, printer)
                nameRenderer.renderName(symbol.name?.asString() ?: "<unnamed>", printer)
            }
        }
    }

    fun with(action: Builder.() -> Unit): CaDeclarationRenderer {
        val current = this
        return Builder().apply {
            nameRenderer = current.nameRenderer
            keywordsRenderer = current.keywordsRenderer
            codeStyle = current.codeStyle
            typeRenderer = current.typeRenderer
            annotationRenderer = current.annotationRenderer
            modifiersRenderer = current.modifiersRenderer
            declarationTypeApproximator = current.declarationTypeApproximator
            classifierBodyRenderer = current.classifierBodyRenderer
            superTypeRenderer = current.superTypeRenderer
            superTypeListRenderer = current.superTypeListRenderer
            superTypesFilter = current.superTypesFilter
            bodyMemberScopeProvider = current.bodyMemberScopeProvider
            bodyMemberScopeSorter = current.bodyMemberScopeSorter
            functionLikeBodyRenderer = current.functionLikeBodyRenderer
            variableInitializerRenderer = current.variableInitializerRenderer
            parameterDefaultValueRenderer = current.parameterDefaultValueRenderer
            propertyAccessorsRenderer = current.propertyAccessorsRenderer
            accessorBodyRenderer = current.accessorBodyRenderer
            returnTypeRenderer = current.returnTypeRenderer
            callableReceiverRenderer = current.callableReceiverRenderer
            valueParametersRenderer = current.valueParametersRenderer
            typeParametersRenderer = current.typeParametersRenderer
            typeParametersFilter = current.typeParametersFilter
            callableSignatureRenderer = current.callableSignatureRenderer
            returnTypeFilter = current.returnTypeFilter
            functionLikeKeywordRenderer = current.functionLikeKeywordRenderer
            classLikeRenderer = current.classLikeRenderer
            typeAliasRenderer = current.typeAliasRenderer
            extendRenderer = current.extendRenderer
            namedFunctionRenderer = current.namedFunctionRenderer
            constructorRenderer = current.constructorRenderer
            propertyRenderer = current.propertyRenderer
            fieldRenderer = current.fieldRenderer
            localVariableRenderer = current.localVariableRenderer
            enumEntryRenderer = current.enumEntryRenderer
            valueParameterRenderer = current.valueParameterRenderer
            typeParameterRenderer = current.typeParameterRenderer
            scriptRenderer = current.scriptRenderer
            action()
        }.build()
    }

    class Builder {
        lateinit var nameRenderer: CaDeclarationNameRenderer
        lateinit var keywordsRenderer: CaKeywordsRenderer
        lateinit var codeStyle: CaRendererCodeStyle
        lateinit var typeRenderer: CaTypeRenderer
        lateinit var annotationRenderer: CaAnnotationRenderer
        lateinit var modifiersRenderer: CaDeclarationModifiersRenderer
        lateinit var declarationTypeApproximator: CaRendererTypeApproximator
        lateinit var classifierBodyRenderer: CaClassifierBodyRenderer
        lateinit var superTypeRenderer: CaSuperTypeRenderer
        lateinit var superTypeListRenderer: CaSuperTypeListRenderer
        lateinit var superTypesFilter: CaSuperTypesFilter
        lateinit var bodyMemberScopeProvider: CaRendererBodyMemberScopeProvider
        lateinit var bodyMemberScopeSorter: CaRendererBodyMemberScopeSorter
        lateinit var functionLikeBodyRenderer: CaFunctionLikeBodyRenderer
        lateinit var variableInitializerRenderer: CaVariableInitializerRenderer
        lateinit var parameterDefaultValueRenderer: CaParameterDefaultValueRenderer
        lateinit var propertyAccessorsRenderer: CaPropertyAccessorsRenderer
        lateinit var accessorBodyRenderer: CaPropertyAccessorBodyRenderer
        lateinit var returnTypeRenderer: CaCallableReturnTypeRenderer
        lateinit var callableReceiverRenderer: CaCallableReceiverRenderer
        lateinit var valueParametersRenderer: CaCallableParameterRenderer
        lateinit var typeParametersRenderer: CaTypeParametersRenderer
        lateinit var typeParametersFilter: CaTypeParametersFilter
        lateinit var callableSignatureRenderer: CaCallableSignatureRenderer
        lateinit var returnTypeFilter: CaCallableReturnTypeFilter
        lateinit var functionLikeKeywordRenderer: CaFunctionLikeKeywordRenderer
        lateinit var classLikeRenderer: CaClassLikeSymbolRenderer
        lateinit var typeAliasRenderer: CaTypeAliasSymbolRenderer
        lateinit var extendRenderer: CaExtendSymbolRenderer
        lateinit var namedFunctionRenderer: CaNamedFunctionSymbolRenderer
        lateinit var constructorRenderer: CaConstructorSymbolRenderer
        lateinit var propertyRenderer: CaPropertySymbolRenderer
        lateinit var fieldRenderer: CaFieldSymbolRenderer
        lateinit var localVariableRenderer: CaLocalVariableSymbolRenderer
        lateinit var enumEntryRenderer: CaEnumEntrySymbolRenderer
        lateinit var valueParameterRenderer: CaValueParameterSymbolRenderer
        lateinit var typeParameterRenderer: CaTypeParameterSymbolRenderer
        lateinit var scriptRenderer: CaScriptSymbolRenderer

        fun build(): CaDeclarationRenderer = CaDeclarationRenderer(
            nameRenderer = nameRenderer,
            keywordsRenderer = keywordsRenderer,
            codeStyle = codeStyle,
            typeRenderer = typeRenderer,
            annotationRenderer = annotationRenderer,
            modifiersRenderer = modifiersRenderer,
            declarationTypeApproximator = declarationTypeApproximator,
            classifierBodyRenderer = classifierBodyRenderer,
            superTypeRenderer = superTypeRenderer,
            superTypeListRenderer = superTypeListRenderer,
            superTypesFilter = superTypesFilter,
            bodyMemberScopeProvider = bodyMemberScopeProvider,
            bodyMemberScopeSorter = bodyMemberScopeSorter,
            functionLikeBodyRenderer = functionLikeBodyRenderer,
            variableInitializerRenderer = variableInitializerRenderer,
            parameterDefaultValueRenderer = parameterDefaultValueRenderer,
            propertyAccessorsRenderer = propertyAccessorsRenderer,
            accessorBodyRenderer = accessorBodyRenderer,
            returnTypeRenderer = returnTypeRenderer,
            callableReceiverRenderer = callableReceiverRenderer,
            valueParametersRenderer = valueParametersRenderer,
            typeParametersRenderer = typeParametersRenderer,
            typeParametersFilter = typeParametersFilter,
            callableSignatureRenderer = callableSignatureRenderer,
            returnTypeFilter = returnTypeFilter,
            functionLikeKeywordRenderer = functionLikeKeywordRenderer,
            classLikeRenderer = classLikeRenderer,
            typeAliasRenderer = typeAliasRenderer,
            extendRenderer = extendRenderer,
            namedFunctionRenderer = namedFunctionRenderer,
            constructorRenderer = constructorRenderer,
            propertyRenderer = propertyRenderer,
            fieldRenderer = fieldRenderer,
            localVariableRenderer = localVariableRenderer,
            enumEntryRenderer = enumEntryRenderer,
            valueParameterRenderer = valueParameterRenderer,
            typeParameterRenderer = typeParameterRenderer,
            scriptRenderer = scriptRenderer,
        )
    }

    companion object {
        operator fun invoke(action: Builder.() -> Unit): CaDeclarationRenderer =
            Builder().apply(action).build()
    }
}

private fun CaClassKind.keyword(): String = when (this) {
    CaClassKind.CLASS -> "class"
    CaClassKind.INTERFACE -> "interface"
    CaClassKind.STRUCT -> "struct"
    CaClassKind.ENUM -> "enum"
}
