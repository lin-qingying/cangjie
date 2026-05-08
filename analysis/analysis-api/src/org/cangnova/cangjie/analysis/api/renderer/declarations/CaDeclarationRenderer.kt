package org.cangnova.cangjie.analysis.api.renderer.declarations

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.renderer.base.CaKeywordsRenderer
import org.cangnova.cangjie.analysis.api.renderer.base.PrettyPrinter
import org.cangnova.cangjie.analysis.api.renderer.base.prettyPrint
import org.cangnova.cangjie.analysis.api.renderer.base.annotations.CaAnnotationRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaClassifierBodyRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaFunctionLikeBodyRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaParameterDefaultValueRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaPropertyAccessorBodyRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaPropertyAccessorsRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaRendererBodyMemberScopeProvider
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaRendererBodyMemberScopeSorter
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaVariableInitializerRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.modifiers.CaDeclarationModifiersRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaCallableParameterRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaCallableReceiverRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaCallableReturnTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaCallableSignatureRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaConstructorSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaEnumConstructorSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaFieldSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaFunctionLikeKeywordRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaLocalVariableSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaNamedFunctionSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaPropertyGetterSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaPropertySymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaPropertySetterSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaValueParameterSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers.CaClassLikeSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers.CaExtendSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers.CaTypeParametersFilter
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers.CaTypeAliasSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers.CaTypeParametersRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers.CaTypeParameterSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.superTypes.CaSuperTypeListRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.superTypes.CaSuperTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.superTypes.CaSuperTypesFilter
import org.cangnova.cangjie.analysis.api.renderer.types.CaRendererTypeApproximator
import org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer
import org.cangnova.cangjie.analysis.api.symbols.CaAnonymousFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassKind
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFieldSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFinalizerSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertyGetterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySetterSymbol
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
    val getterRenderer: CaPropertyGetterSymbolRenderer,
    val setterRenderer: CaPropertySetterSymbolRenderer,
    val fieldRenderer: CaFieldSymbolRenderer,
    val localVariableRenderer: CaLocalVariableSymbolRenderer,
    val enumConstructorRenderer: CaEnumConstructorSymbolRenderer,
    val valueParameterRenderer: CaValueParameterSymbolRenderer,
    val typeParameterRenderer: CaTypeParameterSymbolRenderer,
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
        printer: PrettyPrinter,
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
            is CaEnumConstructorSymbol -> enumConstructorRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaPropertySymbol -> propertyRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaPropertyGetterSymbol -> getterRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaPropertySetterSymbol -> setterRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaFieldSymbol -> fieldRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaValueParameterSymbol -> valueParameterRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaVariableSymbol -> localVariableRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaTypeParameterSymbol -> typeParameterRenderer.renderSymbol(analysisSession, symbol, this, printer)
            is CaFunctionSymbol -> functionLikeKeywordRenderer.renderFunctionLike(analysisSession, symbol, "func", this, printer)
            else -> {
                modifiersRenderer.renderDeclarationModifiers(analysisSession, symbol, printer)
                symbol.name?.let { name ->
                    nameRenderer.renderName(analysisSession, name, null, this, printer)
                } ?: printer.append("<unnamed>")
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
            getterRenderer = current.getterRenderer
            setterRenderer = current.setterRenderer
            fieldRenderer = current.fieldRenderer
            localVariableRenderer = current.localVariableRenderer
            enumConstructorRenderer = current.enumConstructorRenderer
            valueParameterRenderer = current.valueParameterRenderer
            typeParameterRenderer = current.typeParameterRenderer
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
        lateinit var getterRenderer: CaPropertyGetterSymbolRenderer
        lateinit var setterRenderer: CaPropertySetterSymbolRenderer
        lateinit var fieldRenderer: CaFieldSymbolRenderer
        lateinit var localVariableRenderer: CaLocalVariableSymbolRenderer
        lateinit var enumConstructorRenderer: CaEnumConstructorSymbolRenderer
        lateinit var valueParameterRenderer: CaValueParameterSymbolRenderer
        lateinit var typeParameterRenderer: CaTypeParameterSymbolRenderer

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
            getterRenderer = getterRenderer,
            setterRenderer = setterRenderer,
            fieldRenderer = fieldRenderer,
            localVariableRenderer = localVariableRenderer,
            enumConstructorRenderer = enumConstructorRenderer,
            valueParameterRenderer = valueParameterRenderer,
            typeParameterRenderer = typeParameterRenderer,
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
