package org.cangnova.cangjie.analysis.api.renderer.declarations.impl

import org.cangnova.cangjie.analysis.api.renderer.base.annotations.CaAnnotationRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer
import org.cangnova.cangjie.analysis.api.renderer.declarations.bodies.CaClassifierBodyRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.declarations.modifiers.CaDeclarationModifiersRendererForSource
import org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForDebug

object CaDeclarationRendererForDebug {
    private fun CaDeclarationRenderer.withDebugQualifiedNames(): CaDeclarationRenderer = with {
        modifiersRenderer = CaDeclarationModifiersRendererForSource.WITH_IMPLICIT_MODIFIERS
        typeRenderer = CaTypeRendererForDebug.WITH_QUALIFIED_NAMES
        annotationRenderer = CaAnnotationRendererForSource.WITH_QUALIFIED_NAMES
    }

    private fun CaDeclarationRenderer.withDebugShortNames(): CaDeclarationRenderer = with {
        typeRenderer = CaTypeRendererForDebug.WITH_SHORT_NAMES
        annotationRenderer = CaAnnotationRendererForSource.WITH_SHORT_NAMES
    }

    val WITH_QUALIFIED_NAMES: CaDeclarationRenderer = CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES.with {
        modifiersRenderer = CaDeclarationModifiersRendererForSource.WITH_IMPLICIT_MODIFIERS
        classifierBodyRenderer = CaClassifierBodyRendererForSource.NO_BODY
        typeRenderer = CaTypeRendererForDebug.WITH_QUALIFIED_NAMES
        annotationRenderer = CaAnnotationRendererForSource.WITH_QUALIFIED_NAMES
    }

    val WITH_QUALIFIED_NAMES_WITH_MEMBERS: CaDeclarationRenderer =
        CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES_WITH_MEMBERS.withDebugQualifiedNames()

    val WITH_QUALIFIED_NAMES_WITH_MEMBERS_OR_EMPTY_BRACES: CaDeclarationRenderer =
        CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES_WITH_MEMBERS_OR_EMPTY_BRACES.withDebugQualifiedNames()

    val WITH_QUALIFIED_NAMES_WITH_BODY: CaDeclarationRenderer =
        CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES_WITH_BODY.withDebugQualifiedNames()

    val WITH_QUALIFIED_NAMES_WITH_MEMBERS_AND_BODY: CaDeclarationRenderer =
        CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES_WITH_MEMBERS_AND_BODY.withDebugQualifiedNames()

    val WITH_QUALIFIED_NAMES_WITH_PLACEHOLDER_BODIES: CaDeclarationRenderer =
        CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES_WITH_PLACEHOLDER_BODIES.withDebugQualifiedNames()

    val WITH_QUALIFIED_NAMES_WITH_INITIALIZERS: CaDeclarationRenderer =
        CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES_WITH_INITIALIZERS.withDebugQualifiedNames()

    val WITH_QUALIFIED_NAMES_WITH_DEFAULT_PARAMETER_VALUES: CaDeclarationRenderer =
        CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES_WITH_DEFAULT_PARAMETER_VALUES.withDebugQualifiedNames()

    val WITH_QUALIFIED_NAMES_WITH_ALL_DETAILS: CaDeclarationRenderer =
        CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES_WITH_ALL_DETAILS.withDebugQualifiedNames()

    val WITH_QUALIFIED_NAMES_WITH_PLACEHOLDER_DETAILS: CaDeclarationRenderer =
        CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES_WITH_PLACEHOLDER_DETAILS.withDebugQualifiedNames()

    val WITH_QUALIFIED_NAMES_RAW_SIGNATURES: CaDeclarationRenderer =
        CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES_RAW_SIGNATURES.withDebugQualifiedNames()

    val WITH_QUALIFIED_NAMES_WITHOUT_SUPER_TYPES: CaDeclarationRenderer =
        CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES_WITHOUT_SUPER_TYPES.withDebugQualifiedNames()

    val WITH_QUALIFIED_NAMES_WITHOUT_TYPE_PARAMETERS: CaDeclarationRenderer =
        CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES_WITHOUT_TYPE_PARAMETERS.withDebugQualifiedNames()

    val WITH_SHORT_NAMES: CaDeclarationRenderer = WITH_QUALIFIED_NAMES.withDebugShortNames()

    val WITH_SHORT_NAMES_WITH_MEMBERS: CaDeclarationRenderer = WITH_QUALIFIED_NAMES_WITH_MEMBERS.withDebugShortNames()

    val WITH_SHORT_NAMES_WITH_MEMBERS_OR_EMPTY_BRACES: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITH_MEMBERS_OR_EMPTY_BRACES.withDebugShortNames()

    val WITH_SHORT_NAMES_WITH_BODY: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITH_BODY.withDebugShortNames()

    val WITH_SHORT_NAMES_WITH_MEMBERS_AND_BODY: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITH_MEMBERS_AND_BODY.withDebugShortNames()

    val WITH_SHORT_NAMES_WITH_PLACEHOLDER_BODIES: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITH_PLACEHOLDER_BODIES.withDebugShortNames()

    val WITH_SHORT_NAMES_WITH_INITIALIZERS: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITH_INITIALIZERS.withDebugShortNames()

    val WITH_SHORT_NAMES_WITH_DEFAULT_PARAMETER_VALUES: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITH_DEFAULT_PARAMETER_VALUES.withDebugShortNames()

    val WITH_SHORT_NAMES_WITH_ALL_DETAILS: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITH_ALL_DETAILS.withDebugShortNames()

    val WITH_SHORT_NAMES_WITH_PLACEHOLDER_DETAILS: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITH_PLACEHOLDER_DETAILS.withDebugShortNames()

    val WITH_SHORT_NAMES_RAW_SIGNATURES: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_RAW_SIGNATURES.withDebugShortNames()

    val WITH_SHORT_NAMES_WITHOUT_SUPER_TYPES: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITHOUT_SUPER_TYPES.withDebugShortNames()

    val WITH_SHORT_NAMES_WITHOUT_TYPE_PARAMETERS: CaDeclarationRenderer =
        WITH_QUALIFIED_NAMES_WITHOUT_TYPE_PARAMETERS.withDebugShortNames()
}
