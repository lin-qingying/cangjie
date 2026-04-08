package org.cangnova.cangjie.analysis.api.renderer.declarations.impl

import org.cangnova.cangjie.analysis.api.renderer.base.CaAnnotationRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.base.CaKeywordsRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaCallableParameterRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaCallableReceiverRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaCallableReturnTypeFilter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaCallableReturnTypeRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaCallableSignatureRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaClassifierBodyRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaClassLikeSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaConstructorSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationModifiersRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationNameRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaEnumEntrySymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaExtendSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaFieldSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaFunctionLikeBodyRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaFunctionLikeKeywordRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaLocalVariableSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaNamedFunctionSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaParameterDefaultValueRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaPropertySymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaPropertyAccessorsRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaPropertyAccessorBodyRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaRecommendedRendererCodeStyle
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaRendererBodyMemberScopeProvider
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaRendererBodyMemberScopeSorter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaScriptSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaSuperTypeListRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaSuperTypesFilter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaSuperTypeRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaTypeAliasSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaTypeParametersFilter
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaTypeParameterSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaTypeParametersRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaVariableInitializerRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaValueParameterSymbolRenderer
import org.cangnova.cangjie.analysis.api.renderer.types.CaRendererTypeApproximator
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForSource

object CaDeclarationRendererForSource {
    private fun CaDeclarationRenderer.withShortNames(): CaDeclarationRenderer = with {
        typeRenderer = CaTypeRendererForSource.WITH_SHORT_NAMES
        annotationRenderer = CaAnnotationRendererForSource.WITH_SHORT_NAMES
    }

    val WITH_QUALIFIED_NAMES: CaDeclarationRenderer = CaDeclarationRenderer {
        nameRenderer = CaDeclarationNameRenderer.QUOTED
        keywordsRenderer = CaKeywordsRenderer.AS_WORD
        codeStyle = CaRecommendedRendererCodeStyle
        typeRenderer = CaTypeRendererForSource.WITH_QUALIFIED_NAMES
        annotationRenderer = CaAnnotationRendererForSource.WITH_QUALIFIED_NAMES
        modifiersRenderer = CaDeclarationModifiersRendererForSource.NO_IMPLICIT_MODIFIERS
        declarationTypeApproximator = CaRendererTypeApproximator.TO_DENOTABLE
        classifierBodyRenderer = CaClassifierBodyRendererForSource.NO_BODY
        superTypeRenderer = CaSuperTypeRendererForSource.WITH_OUT_APPROXIMATION
        superTypeListRenderer = CaSuperTypeListRendererForSource.AS_LIST
        superTypesFilter = CaSuperTypesFilter.ALL
        bodyMemberScopeProvider = CaRendererBodyMemberScopeProvider.ALL_DECLARED
        bodyMemberScopeSorter = CaRendererBodyMemberScopeSorter.ENUM_ENTRIES_AT_BEGINNING
        functionLikeBodyRenderer = CaFunctionLikeBodyRenderer.NO_BODY
        variableInitializerRenderer = CaVariableInitializerRenderer.NO_INITIALIZER
        parameterDefaultValueRenderer = CaParameterDefaultValueRenderer.NO_DEFAULT_VALUE
        propertyAccessorsRenderer = CaPropertyAccessorsRenderer.NO_ACCESSORS
        accessorBodyRenderer = CaPropertyAccessorBodyRenderer.NO_BODY
        returnTypeRenderer = CaCallableReturnTypeRendererForSource.WITH_OUT_APPROXIMATION
        callableReceiverRenderer = CaCallableReceiverRendererForSource.AS_TYPE_WITH_IN_APPROXIMATION
        valueParametersRenderer = CaCallableParameterRendererForSource.PARAMETERS_IN_PARENS
        typeParametersRenderer = CaTypeParametersRendererForSource.WITH_BOUNDS_IN_WHERE_CLAUSE
        typeParametersFilter = CaTypeParametersFilter.ALL
        callableSignatureRenderer = CaCallableSignatureRendererForSource.FOR_SOURCE
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
        enumEntryRenderer = CaEnumEntrySymbolRenderer.AS_SOURCE
        valueParameterRenderer = CaValueParameterSymbolRenderer.AS_SOURCE
        typeParameterRenderer = CaTypeParameterSymbolRenderer.AS_SOURCE
        scriptRenderer = CaScriptSymbolRenderer.AS_SOURCE
    }

    val WITH_QUALIFIED_NAMES_WITH_MEMBERS: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        classifierBodyRenderer = CaClassifierBodyRendererForSource.BODY_WITH_MEMBERS
    }

    val WITH_QUALIFIED_NAMES_WITH_MEMBERS_OR_EMPTY_BRACES: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        classifierBodyRenderer = CaClassifierBodyRendererForSource.BODY_WITH_MEMBERS_OR_EMPTY_BRACES
    }

    val WITH_QUALIFIED_NAMES_WITH_BODY: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        functionLikeBodyRenderer = CaFunctionLikeBodyRenderer.AS_SOURCE
        propertyAccessorsRenderer = CaPropertyAccessorsRenderer.AS_RENDERED_ACCESSORS
        accessorBodyRenderer = CaPropertyAccessorBodyRenderer.AS_SOURCE
    }

    val WITH_QUALIFIED_NAMES_WITH_MEMBERS_AND_BODY: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        classifierBodyRenderer = CaClassifierBodyRendererForSource.BODY_WITH_MEMBERS
        functionLikeBodyRenderer = CaFunctionLikeBodyRenderer.AS_SOURCE
        propertyAccessorsRenderer = CaPropertyAccessorsRenderer.AS_RENDERED_ACCESSORS
        accessorBodyRenderer = CaPropertyAccessorBodyRenderer.AS_SOURCE
    }

    val WITH_QUALIFIED_NAMES_WITH_PLACEHOLDER_BODIES: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        functionLikeBodyRenderer = CaFunctionLikeBodyRenderer.WITH_PLACEHOLDER
        propertyAccessorsRenderer = CaPropertyAccessorsRenderer.AS_RENDERED_ACCESSORS
        accessorBodyRenderer = CaPropertyAccessorBodyRenderer.WITH_PLACEHOLDER
    }

    val WITH_QUALIFIED_NAMES_WITH_INITIALIZERS: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        variableInitializerRenderer = CaVariableInitializerRenderer.AS_SOURCE
    }

    val WITH_QUALIFIED_NAMES_WITH_DEFAULT_PARAMETER_VALUES: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        parameterDefaultValueRenderer = CaParameterDefaultValueRenderer.AS_SOURCE
    }

    val WITH_QUALIFIED_NAMES_WITH_ALL_DETAILS: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        variableInitializerRenderer = CaVariableInitializerRenderer.AS_SOURCE
        parameterDefaultValueRenderer = CaParameterDefaultValueRenderer.AS_SOURCE
        functionLikeBodyRenderer = CaFunctionLikeBodyRenderer.AS_SOURCE
        propertyAccessorsRenderer = CaPropertyAccessorsRenderer.AS_RENDERED_ACCESSORS
        accessorBodyRenderer = CaPropertyAccessorBodyRenderer.AS_SOURCE
    }

    val WITH_QUALIFIED_NAMES_WITH_PLACEHOLDER_DETAILS: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.with {
        variableInitializerRenderer = CaVariableInitializerRenderer.WITH_PLACEHOLDER
        parameterDefaultValueRenderer = CaParameterDefaultValueRenderer.WITH_PLACEHOLDER
        functionLikeBodyRenderer = CaFunctionLikeBodyRenderer.WITH_PLACEHOLDER
        propertyAccessorsRenderer = CaPropertyAccessorsRenderer.AS_RENDERED_ACCESSORS
        accessorBodyRenderer = CaPropertyAccessorBodyRenderer.WITH_PLACEHOLDER
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
