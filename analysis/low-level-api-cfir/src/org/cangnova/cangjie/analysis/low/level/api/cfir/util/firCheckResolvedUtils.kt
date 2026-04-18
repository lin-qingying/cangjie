/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(UnresolvedExpressionTypeAccess::class)

package org.cangnova.cangjie.analysis.low.level.api.cfir.util

import org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve.NonLocalAnnotationVisitor
import org.cangnova.cangjie.cfir.CfirAnnotationContainer
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.CfirElementWithResolveState
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirResolvable
import org.cangnova.cangjie.cfir.expressions.UnresolvedExpressionTypeAccess
import org.cangnova.cangjie.cfir.expressions.impl.CfirResolvedArgumentList
import org.cangnova.cangjie.cfir.references.CfirErrorNamedReference
import org.cangnova.cangjie.cfir.references.CfirNamedReference
import org.cangnova.cangjie.cfir.references.CfirReference
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.resolve.transformers.body.resolve.CfirAbstractBodyResolveTransformerDispatcher
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.utils.exceptions.withCfirEntry
import org.cangnova.cangjie.cfir.utils.exceptions.withCfirSymbolEntry
import org.cangnova.cangjie.utils.exceptions.ExceptionAttachmentBuilder
import org.cangnova.cangjie.utils.exceptions.checkWithAttachment
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment

internal inline fun checkTypeRefIsResolved(
    typeRef: CfirTypeRef,
    typeRefName: String,
    owner: CfirElementWithResolveState,
    acceptImplicitTypeRef: Boolean = false,
    extraAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
) {
    checkWithAttachment(
        condition = typeRef is CfirResolvedTypeRef || acceptImplicitTypeRef && typeRef is CfirImplicitTypeRef,
        message = {
            buildString {
                append("Expected ${CfirResolvedTypeRef::class.simpleName}")
                if (acceptImplicitTypeRef) {
                    append(" or ${CfirImplicitTypeRef::class.simpleName}")
                }
                append(" for $typeRefName of ${owner::class.simpleName}(${(owner as? CfirDeclaration)?.origin}) but ${typeRef::class.simpleName} found")
            }
        }
    ) {
        withCfirEntry("typeRef", typeRef)
        withCfirEntry("firDeclaration", owner)
        extraAttachment()
    }
}

internal inline fun checkExpressionTypeIsResolved(
    type: ConeKotlinType?,
    typeName: String,
    owner: CfirElement,
    extraAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
) {
    checkWithAttachment(
        condition = type != null,
        message = {
            buildString {
                append("Expected resolved expression type")
                append(" for $typeName of ${owner::class.simpleName}(${(owner as? CfirDeclaration)?.origin})")
            }
        }
    ) {
        withCfirEntry("firDeclaration", owner)
        extraAttachment()
    }
}

internal fun <T> checkAnnotationTypeIsResolved(annotationContainer: T) where T : CfirAnnotationContainer, T : CfirElementWithResolveState {
    annotationContainer.annotations.forEach { annotation ->
        checkTypeRefIsResolved(annotation.annotationTypeRef, "annotation type", owner = annotationContainer) {
            withCfirEntry("firAnnotation", annotation)
        }

        annotation.typeArguments.forEach {
            if (it is CfirTypeProjectionWithVariance) {
                checkTypeRefIsResolved(it.typeRef, "annotation type argument", owner = annotationContainer) {
                    withCfirEntry("typeProjection", it)
                }
            }
        }
    }
}

internal fun checkBodyIsResolved(function: CfirFunction) {
    val block = function.body ?: return
    checkExpressionTypeIsResolved(block.coneTypeOrNull, "block type", function) {
        withCfirEntry("block", block)
    }
}

internal fun checkDelegatedConstructorIsResolved(constructor: CfirConstructor) {
    val delegatedConstructorCall = constructor.delegatedConstructor ?: return
    val calleeReference = delegatedConstructorCall.calleeReference
    checkReferenceIsResolved(reference = calleeReference, owner = delegatedConstructorCall) {
        withCfirEntry("constructor", constructor)
    }
}

internal fun checkReferenceIsResolved(
    reference: CfirReference,
    owner: CfirResolvable,
    extraAttachment: ExceptionAttachmentBuilder.() -> Unit = {},
) {
    checkWithAttachment(
        condition = reference is CfirResolvedNamedReference || reference is CfirErrorNamedReference,
        message = {
            "Expected ${CfirNamedReference::class.simpleName} or " +
                    "${CfirErrorNamedReference::class.simpleName} " +
                    "but ${reference::class.simpleName} found"
        }
    ) {
        withCfirEntry("referenceOwner", owner)
        extraAttachment()
    }
}

internal fun checkInitializerIsResolved(variable: CfirVariable) {
    val initializer = variable.initializer ?: return
    checkExpressionTypeIsResolved(initializer.coneTypeOrNull, "initializer type", variable) {
        withCfirEntry("initializer", initializer)
    }
}

internal fun checkDefaultValueIsResolved(parameter: CfirValueParameter) {
    val defaultValue = parameter.defaultValue ?: return
    checkExpressionTypeIsResolved(defaultValue.coneTypeOrNull, "default value type", parameter) {
        withCfirEntry("defaultValue", defaultValue)
    }
}

internal fun checkDeprecationProviderIsResolved(declaration: CfirDeclaration, provider: DeprecationsProvider) {
    checkWithAttachment(
        condition = provider !is UnresolvedDeprecationProvider,
        message = { "Unresolved deprecation provider found for ${declaration::class.simpleName}" }
    ) {
        withCfirEntry("declaration", declaration)
    }
}

internal fun checkReturnTypeRefIsResolved(declaration: CfirCallableDeclaration, acceptImplicitTypeRef: Boolean = false) {
    checkTypeRefIsResolved(declaration.returnTypeRef, typeRefName = "return type", declaration, acceptImplicitTypeRef)
}

internal fun checkDeclarationStatusIsResolved(declaration: CfirMemberDeclaration) {
    val status = declaration.status
    checkWithAttachment(
        condition = status is CfirResolvedDeclarationStatus,
        message = { "Expected ${CfirResolvedDeclarationStatus::class.simpleName} but ${status::class.simpleName} found for ${declaration::class.simpleName}" }
    ) {
        withCfirEntry("declaration", declaration)
    }
}

internal fun checkAnnotationsAreResolved(owner: CfirAnnotationContainer, typeRef: CfirTypeRef) {
    checkWithAttachment(typeRef is CfirResolvedTypeRef, { "Unexpected type: ${typeRef::class.simpleName}" }) {
        withCfirEntry("owner", owner)
        withCfirEntry("type", typeRef)
    }

    typeRef.accept(AnnotationChecker, owner)
}

internal fun CfirAbstractBodyResolveTransformerDispatcher.checkAnnotationCallIsResolved(
    symbol: CfirBasedSymbol<*>,
    annotationCall: CfirAnnotationCall,
) {
    val annotationContainer = context.containerIfAny ?: errorWithAttachment("Container cannot be found") {
        withCfirSymbolEntry("symbol", symbol)
        withCfirEntry("annotation", annotationCall)
    }

    checkAnnotationIsResolved(annotationCall, annotationContainer)
}

private object AnnotationChecker : NonLocalAnnotationVisitor<CfirAnnotationContainer>() {
    override fun processAnnotation(annotation: CfirAnnotation, data: CfirAnnotationContainer) {
        checkAnnotationIsResolved(annotation, data)
    }
}

internal fun checkAnnotationsAreResolved(annotationContainer: CfirAnnotationContainer) {
    for (annotation in annotationContainer.annotations) {
        checkAnnotationIsResolved(annotation, annotationContainer)
    }
}

internal fun checkAnnotationIsResolved(annotation: CfirAnnotation, annotationContainer: CfirAnnotationContainer) {
    if (annotation is CfirAnnotationCall) {
        checkWithAttachment(
            condition = annotation.argumentList is CfirResolvedArgumentList,
            message = {
                buildString {
                    append("Expected ${CfirResolvedArgumentList::class.simpleName}")
                    append(" for ${annotation::class.simpleName} of ${annotationContainer::class.simpleName}(${(annotationContainer as? CfirDeclaration)?.origin})")
                    append(" but ${annotation.argumentList::class.simpleName} found")
                }
            }
        ) {
            withCfirEntry("firAnnotation", annotation)
            withCfirEntry("firDeclaration", annotationContainer)
        }
    }

    for (argument in annotation.argumentMapping.mapping.values) {
        checkExpressionTypeIsResolved(argument.coneTypeOrNull, "annotation argument", annotationContainer) {
            withCfirEntry("firAnnotation", annotation)
            withCfirEntry("firArgument", argument)
        }
    }
}

/**
 * Checks whether the given [target] is resolved at the [requestedPhase].
 * If resolution is already complete, a [LLReadyPhaseEvent] is sent.
 *
 * @param target The declaration being analyzed.
 * @param containingDeclarations The list of declarations enclosing [target] starting from the [CfirFile], if available.
 * @param requestedPhase The phase the declaration is being analyzed to.
 *
 * [containingDeclarations] are passed as an optimization.
 * If the argument value is `null`, the list will be computed before the [LLReadyPhaseEvent] submission.
 *
 * @return `true` if the [target] is resolved at the [requestedPhase], `false` otherwise.
 */
internal fun checkAnalysisReadiness(
    target: CfirElementWithResolveState,
    containingDeclarations: List<CfirDeclaration>?,
    requestedPhase: CfirResolvePhase,
    currentPhase: CfirResolvePhase = target.resolvePhase
): Boolean {
    if (currentPhase >= requestedPhase) {
        if (shouldRecordReadyPhaseEvent(requestedPhase)) {
            if (containingDeclarations != null) {
                LLFlightRecorder.readyPhase(target, containingDeclarations, requestedPhase)
            } else {
                LLFlightRecorder.readyPhase(target, requestedPhase)
            }
        }
        return true
    }

    return false
}

private fun shouldRecordReadyPhaseEvent(requestedPhase: CfirResolvePhase): Boolean {
    return when (requestedPhase) {
        CfirResolvePhase.RAW_CFIR -> false

        CfirResolvePhase.IMPORTS -> {
            /**
             * Technically, we should record here [LLFlightRecorder.readyPhase] events here. However, imports for files are requested
             * too often. Moreover, import resolution is quite fast, so we won't be able to get any useful information from these events.
             */
            false
        }

        CfirResolvePhase.SEALED_CLASS_INHERITORS -> {
            /**
             * The phase is no-op in LL CFIR.
             * @see [org.cangnova.cangjie.analysis.low.level.api.cfir.providers.LLSealedInheritorsProvider]
             */
            false
        }

        else -> true
    }
}
