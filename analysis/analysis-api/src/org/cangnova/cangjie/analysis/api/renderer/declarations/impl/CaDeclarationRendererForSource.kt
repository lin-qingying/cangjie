package org.cangnova.cangjie.analysis.api.renderer.declarations.impl

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.renderer.base.CaKeywordsRenderer
import org.cangnova.cangjie.analysis.api.renderer.base.annotations.CaAnnotationRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaCallableReturnTypeFilter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationNameRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaRecommendedRendererCodeStyle
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaClassifierBodyRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaFunctionLikeBodyRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaParameterDefaultValueRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaPropertyAccessorBodyRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaPropertyAccessorsRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaRendererBodyMemberScopeProvider
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaRendererBodyMemberScopeSorter
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaVariableInitializerRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.modifiers.CaDeclarationModifiersRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaCallableParameterRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaCallableReceiverRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaCallableReturnTypeRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaCallableSignatureRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaConstructorSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaEnumConstructorSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaFieldSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaFunctionLikeKeywordRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaLocalVariableSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaNamedFunctionSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaPropertySymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaScriptSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.callables.CaValueParameterSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers.CaClassLikeSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers.CaExtendSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers.CaTypeAliasSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers.CaTypeParametersFilter
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers.CaTypeParametersRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.declarations.renderers.classifiers.CaTypeParameterSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.superTypes.CaSuperTypeListRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.declarations.superTypes.CaSuperTypeRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.declarations.superTypes.CaSuperTypesFilter
import org.cangnova.cangjie.analysis.api.renderer.types.CaRendererTypeApproximator
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForSource

object CaDeclarationRendererForSource {
    private fun CaDeclarationRenderer.withShortNames(): CaDeclarationRenderer = with {
        typeRenderer = CaTypeRendererForSource.WITH_SHORT_NAMES
        annotationRenderer = CaAnnotationRendererForSource.WITH_SHORT_NAMES
    }

    @OptIn(CaExperimentalApi::class)
    val WITH_QUALIFIED_NAMES: CaDeclarationRenderer = CaDeclarationRenderer {
        nameRenderer = CaDeclarationNameRenderer.QUOTED
        keywordsRenderer = CaKeywordsRenderer.AS_WORD
        codeStyle = CaRecommendedRendererCodeStyle
        typeRenderer = CaTypeRendererForSource.WITH_QUALIFIED_NAMES
        annotationRenderer = CaAnnotationRendererForSource.WITH_QUALIFIED_NAMES
        modifiersRenderer = CaDeclarationModifiersRendererForSource.NO_IMPLICIT_MODIFIERS
        declarationTypeApproximator = CaRendererTypeApproximator.NO_APPROXIMATION
        classifierBodyRenderer = CaClassifierBodyRenderer.NO_BODY

        superTypeRenderer = CaSuperTypeRendererForSource.WITH_OUT_APPROXIMATION
        superTypeListRenderer = CaSuperTypeListRendererForSource.AS_LIST
        superTypesFilter = CaSuperTypesFilter.ALL
        bodyMemberScopeProvider = CaRendererBodyMemberScopeProvider.ALL_DECLARED
        bodyMemberScopeSorter = CaRendererBodyMemberScopeSorter.ENUM_CONSTRUCTORS_AT_BEGINNING
        functionLikeBodyRenderer = CaFunctionLikeBodyRenderer.NO_BODY
        variableInitializerRenderer = CaVariableInitializerRenderer.NO_INITIALIZER
        parameterDefaultValueRenderer = CaParameterDefaultValueRenderer.NO_DEFAULT_VALUE
        propertyAccessorsRenderer = CaPropertyAccessorsRenderer.NO_ACCESSORS
        accessorBodyRenderer = CaPropertyAccessorBodyRenderer.NO_BODY
        returnTypeRenderer = CaCallableReturnTypeRenderer.WITH_OUT_APPROXIMATION
        callableReceiverRenderer = CaCallableReceiverRendererForSource.AS_TYPE_WITH_IN_APPROXIMATION
        valueParametersRenderer = CaCallableParameterRenderer.PARAMETERS_IN_PARENS
        typeParametersRenderer = CaTypeParametersRendererForSource.WITH_BOUNDS_IN_WHERE_CLAUSE
        typeParametersFilter = CaTypeParametersFilter.ALL
        callableSignatureRenderer = CaCallableSignatureRenderer.FOR_SOURCE
        returnTypeFilter = CaCallableReturnTypeFilter.NO_UNIT_FOR_FUNCTIONS
        functionLikeKeywordRenderer = CaFunctionLikeKeywordRenderer.AS_SOURCE
        classLikeRenderer = CaClassLikeSymbolRenderer.AS_SOURCE
        typeAliasRenderer = CaTypeAliasSymbolRenderer.AS_SOURCE
        extendRenderer = CaExtendSymbolRenderer.AS_SOURCE
        namedFunctionRenderer = CaNamedFunctionSymbolRenderer.AS_SOURCE
        constructorRenderer = CaConstructorSymbolRenderer.AS_SOURCE
        propertyRenderer = CaPropertySymbolRenderer.AS_SOURCE
        fieldRenderer = CaFieldSymbolRenderer.AS_SOURCE
        localVariableRenderer = CaLocalVariableSymbolRenderer.AS_SOURCE
        enumConstructorRenderer = CaEnumConstructorSymbolRenderer.AS_SOURCE
        valueParameterRenderer = CaValueParameterSymbolRenderer.AS_SOURCE
        typeParameterRenderer = CaTypeParameterSymbolRenderer.AS_SOURCE
        scriptRenderer = CaScriptSymbolRenderer.AS_SOURCE
    }

    val WITH_QUALIFIED_NAMES_WITH_MEMBERS: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        classifierBodyRenderer = CaClassifierBodyRenderer.BODY_WITH_MEMBERS
    }

    val WITH_QUALIFIED_NAMES_WITH_MEMBERS_OR_EMPTY_BRACES: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        classifierBodyRenderer = CaClassifierBodyRenderer.BODY_WITH_MEMBERS_OR_EMPTY_BRACES
    }

    val WITH_QUALIFIED_NAMES_WITH_BODY: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        functionLikeBodyRenderer = CaFunctionLikeBodyRenderer.NO_BODY
        propertyAccessorsRenderer = CaPropertyAccessorsRenderer.NO_ACCESSORS
        accessorBodyRenderer = CaPropertyAccessorBodyRenderer.NO_BODY
    }

    val WITH_QUALIFIED_NAMES_WITH_MEMBERS_AND_BODY: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        classifierBodyRenderer = CaClassifierBodyRenderer.BODY_WITH_MEMBERS
        functionLikeBodyRenderer = CaFunctionLikeBodyRenderer.NO_BODY
        propertyAccessorsRenderer = CaPropertyAccessorsRenderer.NO_ACCESSORS
        accessorBodyRenderer = CaPropertyAccessorBodyRenderer.NO_BODY
    }

    val WITH_QUALIFIED_NAMES_WITH_PLACEHOLDER_BODIES: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        functionLikeBodyRenderer = CaFunctionLikeBodyRenderer.NO_BODY
        propertyAccessorsRenderer = CaPropertyAccessorsRenderer.NO_ACCESSORS
        accessorBodyRenderer = CaPropertyAccessorBodyRenderer.NO_BODY
    }

    val WITH_QUALIFIED_NAMES_WITH_INITIALIZERS: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        variableInitializerRenderer = CaVariableInitializerRenderer.NO_INITIALIZER
    }

    val WITH_QUALIFIED_NAMES_WITH_DEFAULT_PARAMETER_VALUES: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        parameterDefaultValueRenderer = CaParameterDefaultValueRenderer.THREE_DOTS
    }

    val WITH_QUALIFIED_NAMES_WITH_ALL_DETAILS: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        variableInitializerRenderer = CaVariableInitializerRenderer.NO_INITIALIZER
        parameterDefaultValueRenderer = CaParameterDefaultValueRenderer.THREE_DOTS
        functionLikeBodyRenderer = CaFunctionLikeBodyRenderer.NO_BODY
        propertyAccessorsRenderer = CaPropertyAccessorsRenderer.NO_ACCESSORS
        accessorBodyRenderer = CaPropertyAccessorBodyRenderer.NO_BODY
    }

    val WITH_QUALIFIED_NAMES_WITH_PLACEHOLDER_DETAILS: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        variableInitializerRenderer = CaVariableInitializerRenderer.NO_INITIALIZER
        parameterDefaultValueRenderer = CaParameterDefaultValueRenderer.THREE_DOTS
        functionLikeBodyRenderer = CaFunctionLikeBodyRenderer.NO_BODY
        propertyAccessorsRenderer = CaPropertyAccessorsRenderer.NO_ACCESSORS
        accessorBodyRenderer = CaPropertyAccessorBodyRenderer.NO_BODY
    }

    val WITH_QUALIFIED_NAMES_RAW_SIGNATURES: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        namedFunctionRenderer = CaNamedFunctionSymbolRenderer.AS_RAW_SIGNATURE
        constructorRenderer = CaConstructorSymbolRenderer.AS_RAW_SIGNATURE
        propertyRenderer = CaPropertySymbolRenderer.AS_RAW_SIGNATURE
    }

    val WITH_QUALIFIED_NAMES_WITHOUT_SUPER_TYPES: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        superTypesFilter = CaSuperTypesFilter.NONE
    }

    val WITH_QUALIFIED_NAMES_WITHOUT_TYPE_PARAMETERS: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        typeParametersFilter = CaTypeParametersFilter.NONE
    }

    val WITH_SHORT_NAMES: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.withShortNames()

    val WITH_SHORT_NAMES_WITH_MEMBERS: CaDeclarationRenderer = WITH_QUALIFIED_NAMES_WITH_MEMBERS.withShortNames()

    val WITH_SHORT_NAMES_WITH_MEMBERS_OR_EMPTY_BRACES: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITH_MEMBERS_OR_EMPTY_BRACES.withShortNames()

    val WITH_SHORT_NAMES_WITH_BODY: CaDeclarationRenderer = WITH_QUALIFIED_NAMES_WITH_BODY.withShortNames()

    val WITH_SHORT_NAMES_WITH_MEMBERS_AND_BODY: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITH_MEMBERS_AND_BODY.withShortNames()

    val WITH_SHORT_NAMES_WITH_PLACEHOLDER_BODIES: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITH_PLACEHOLDER_BODIES.withShortNames()

    val WITH_SHORT_NAMES_WITH_INITIALIZERS: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITH_INITIALIZERS.withShortNames()

    val WITH_SHORT_NAMES_WITH_DEFAULT_PARAMETER_VALUES: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITH_DEFAULT_PARAMETER_VALUES.withShortNames()

    val WITH_SHORT_NAMES_WITH_ALL_DETAILS: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITH_ALL_DETAILS.withShortNames()

    val WITH_SHORT_NAMES_WITH_PLACEHOLDER_DETAILS: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITH_PLACEHOLDER_DETAILS.withShortNames()

    val WITH_SHORT_NAMES_RAW_SIGNATURES: CaDeclarationRenderer = WITH_QUALIFIED_NAMES_RAW_SIGNATURES.withShortNames()

    val WITH_SHORT_NAMES_WITHOUT_SUPER_TYPES: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITHOUT_SUPER_TYPES.withShortNames()

    val WITH_SHORT_NAMES_WITHOUT_TYPE_PARAMETERS: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITHOUT_TYPE_PARAMETERS.withShortNames()
}
