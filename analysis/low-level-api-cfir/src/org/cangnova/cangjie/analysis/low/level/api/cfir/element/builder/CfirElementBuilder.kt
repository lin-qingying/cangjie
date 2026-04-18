/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.element.builder

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.FileStructure
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.CfirElementsRecorder
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.CjToCfirMapping
import org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve.elementCanBeLazilyResolved
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.findSourceNonLocalCfirDeclaration
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.isPartialBodyResolvable
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.requireTypeIntersectionWith
import org.cangnova.cangjie.analysis.utils.printer.parentOfType
import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.utils.correspondingValueParameterFromPrimaryConstructor
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.resolve.providers.firProvider
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhaseRecursively
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.parentsWithSelf
import org.cangnova.cangjie.utils.ThreadSafe
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withPsiEntry

/**
 * This class is responsible for mapping from [CjElement] to [CfirElement]
 * using [FileStructure][org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.FileStructure].
 *
 * @see getOrBuildCfirFor
 * @see org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.FileStructure
 * @see getNonLocalContainingOrThisElement
 */
@ThreadSafe
internal class CfirElementBuilder(private val moduleComponents: LLCfirModuleResolveComponents) {
    companion object {
        private fun getPsiAsCfirElementSource(element: CjElement): CjElement? {
            val unwrappedElement = if (element is CjExpression) CjPsiUtil.safeDeparenthesize(element) else element
            return when (unwrappedElement) {
                is CjQualifiedExpression if unwrappedElement.selectorExpression is CjCallExpression -> {
                    /*
                     CjQualifiedExpression with CjCallExpression in selector transformed in CFIR to CfirFunctionCall expression
                     Which will have a receiver as qualifier
                     */
                    unwrappedElement.selectorExpression ?: errorWithAttachment("Incomplete code") {
                        withPsiEntry("psi", unwrappedElement)
                    }
                }
                is CjValueArgument -> {
                    // null will be return in case of invalid CjValueArgument
                    unwrappedElement.getArgumentExpression()
                }
                is CjStringTemplateEntryWithExpression -> unwrappedElement.expression
                is CjUserType if unwrappedElement.parent is CjNullableType -> unwrappedElement.parent as CjNullableType
                else -> unwrappedElement
            }
        }

        private fun doCjElementHasCorrespondingCfirElement(ktElement: CjElement): Boolean = when (ktElement) {
            is CjImportList -> false
            is CjFileAnnotationList -> false
            is CjAnnotation -> false
            else -> true
        }
    }

    /**
     * Returns a [CfirElement] in its final resolved state.
     *
     * Note: that it isn't always [BODY_RESOLVE][CfirResolvePhase.BODY_RESOLVE]
     * as not all declarations have types/bodies/etc. to resolve.
     *
     * For instance, [CjPackageDirective] has nothing to resolve,
     * so it will be returned as is ([CfirPackageDirective][org.cangnova.cangjie.cfir.CfirPackageDirective]),
     * with the [RAW_CFIR][CfirResolvePhase.RAW_CFIR] phase.
     *
     * @return associated [CfirElement] in final resolved state if it exists.
     *
     * @see getCfirForElementInsideAnnotations
     * @see getCfirForElementInsideTypes
     * @see getCfirForElementInsideFileHeader
     */
    fun getOrBuildCfirFor(element: CjElement): CfirElement? {
        return if (element is CjFile && element !is CjCodeFragment) {
            getOrBuildCfirForCjFile(element)
        } else {
            getCfirForNonCjFileElement(element)
        }
    }

    private fun getOrBuildCfirForCjFile(ktFile: CjFile): CfirFile {
        val firFile = moduleComponents.firFileBuilder.buildRawCfirFileWithCaching(ktFile)
        firFile.lazyResolveToPhaseRecursively(CfirResolvePhase.BODY_RESOLVE)
        return firFile
    }

    private fun getCfirForNonCjFileElement(element: CjElement): CfirElement? {
        require(element !is CjFile || element is CjCodeFragment)

        if (!doCjElementHasCorrespondingCfirElement(element)) {
            return null
        }

        val nonLocalContainer = element.getNonLocalContainingOrThisElement()
        tryGetCfirWithoutBodyResolve(nonLocalContainer, element)?.let { return it }

        val psi = getPsiAsCfirElementSource(element) ?: return null
        val ktFile = element.containingCjFile
        val fileStructure = moduleComponents.fileStructureCache.getFileStructure(ktFile)

        val structureElement = fileStructure.getStructureElementFor(element, nonLocalContainer)
        val mappings = structureElement.mappings

        val firElement = mappings.get(psi)

        if (firElement is CfirElementWithResolveState) {
            // Partially resolvable declarations might have unresolved bodies in the mapping.
            // Here we forcibly resolve them to obey to the 'getOrBuildCfirFor()' contract.
            if (firElement.isPartialBodyResolvable && firElement.resolvePhase < CfirResolvePhase.BODY_RESOLVE) {
                firElement.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
            }
        }

        return firElement
    }

    /**
     * Provides a fast path for well-known psi-to-fir mappings to avoid [CfirResolvePhase.BODY_RESOLVE] there it is possible.
     *
     * This optimization makes sense only for [nonLocalContainer]s which might have such expensive resolution.
     * For instance, there is no need to avoid [CfirResolvePhase.BODY_RESOLVE] for dangling modifiers as they don't
     * have bodies, so effectively it is the same as [CfirResolvePhase.ANNOTATION_ARGUMENTS].
     *
     * Declaration containers ([CjFile], [CjClassOrObject]) are resolved till [CfirResolvePhase.ANNOTATION_ARGUMENTS]
     * by default in [FileStructure.getStructureElementFor],
     * so there is no need to use optimized search via [getCfirForElementInsideAnnotations]/[getCfirForElementInsideTypes]
     * as this is redundant work and effectively duplicate the logic of [CjToCfirMapping].
     *
     * [CjClassOrObject] is not excluded yet as in some cases it might trigger additional resolution.
     * For instance, currently it resolves generated members (fake constructor, data class members, enum members, etc.).
     *
     * [CjEnumEntry] is not a declaration container as it is treated as callable in K2.
     *
     * @see getCfirForElementInsideFileHeader
     * @see getCfirForElementInsideAnnotations
     * @see getCfirForElementInsideTypes
     */
    private fun tryGetCfirWithoutBodyResolve(nonLocalContainer: CjElement?, element: CjElement): CfirElement? = when (nonLocalContainer) {
        is CjFile -> getCfirForElementInsideFileHeader(element)

        is CjDeclaration -> getCfirForElementInsideAnnotations(element, nonLocalContainer)
            ?: getCfirForElementInsideTypes(element, nonLocalContainer)

        else -> null
    }

    private inline fun <T : CjElement, E : PsiElement> getCfirForNonBodyElement(
        element: CjElement,
        nonLocalDeclaration: CjDeclaration?,
        anchorElementProvider: (CjElement) -> T?,
        elementOwnerProvider: (T) -> E?,
        resolveAndFindCfirForAnchor: (CfirElementWithResolveState, T) -> CfirElement?,
    ): CfirElement? {
        val anchorElement = anchorElementProvider(element) ?: return null
        val elementOwner = elementOwnerProvider(anchorElement) ?: return null

        val firElementContainer = if (elementOwner is CjFile) {
            moduleComponents.firFileBuilder.buildRawCfirFileWithCaching(elementOwner)
        } else {
            if (elementOwner != nonLocalDeclaration) return null

            nonLocalDeclaration.findSourceNonLocalCfirDeclaration(
                firFileBuilder = moduleComponents.firFileBuilder,
                provider = moduleComponents.session.firProvider,
            )
        }

        // There is no need for a custom traverse as the resolved element has a cached map
        if (firElementContainer.resolvePhase == CfirResolvePhase.BODY_RESOLVE) {
            return null
        }

        val anchorCfir = resolveAndFindCfirForAnchor(firElementContainer, anchorElement) ?: return null
        // We use identity comparison here intentionally to check that it is exactly the object we want to find
        if (element === anchorElement) return anchorCfir

        return findElementInside(firElement = anchorCfir, element = element)
    }

    private fun PsiElement.annotationOwner(): CjAnnotated? {
        val modifierList = when (val parent = parent) {
            is CjModifierList -> parent
            is CjAnnotation -> return parent.annotationOwner()
            is CjFileAnnotationList -> return parent.parent as? CjFile
            else -> null
        }

        return modifierList?.owner as? CjDeclaration
    }

    private fun getCfirForElementInsideAnnotations(
        element: CjElement,
        nonLocalDeclaration: CjDeclaration,
    ): CfirElement? = getCfirForNonBodyElement(
        element = element,
        nonLocalDeclaration = nonLocalDeclaration,
        anchorElementProvider = { it.parentsOfType<CjAnnotationEntry>(nonLocalDeclaration).firstOrNull() },
        elementOwnerProvider = { it.annotationOwner() },
        resolveAndFindCfirForAnchor = { declaration, anchor -> declaration.resolveAndFindAnnotation(anchor, goDeep = true) },
    )

    private fun getCfirForElementInsideTypes(
        element: CjElement,
        nonLocalDeclaration: CjDeclaration,
    ): CfirElement? = getCfirForNonBodyElement(
        element = element,
        nonLocalDeclaration = nonLocalDeclaration,
        anchorElementProvider = { it.parentsOfType<CjTypeReference>(nonLocalDeclaration).lastOrNull() },
        elementOwnerProvider = {
            when (val parent = it.parent) {
                is CjDeclaration -> parent
                is CjSuperTypeListEntry, is CjConstructorCalleeExpression, is CjTypeConstraint -> parent.parentOfType<CjDeclaration>()
                else -> null
            }
        },
        resolveAndFindCfirForAnchor = { declaration, anchor -> declaration.resolveAndFindTypeRefAnchor(anchor) },
    )?.let { firElement ->
        if (firElement is CfirReceiverParameter) {
            firElement.typeRef
        } else {
            firElement
        }
    }

    private fun getCfirForElementInsideFileHeader(element: CjElement): CfirElement? = getCfirForNonBodyElement<CjElement, CjAnnotated>(
        element = element,
        nonLocalDeclaration = null,
        anchorElementProvider = { it.fileHeaderAnchorElement() },
        elementOwnerProvider = { it.containingCjFile },
        resolveAndFindCfirForAnchor = { declaration, anchor ->
            declaration.requireTypeIntersectionWith<CfirFile>()

            when (anchor) {
                is CjPackageDirective -> declaration.packageDirective
                is CjImportDirective -> {
                    declaration.lazyResolveToPhase(CfirResolvePhase.IMPORTS)
                    declaration.imports.find { it.psi == anchor }
                }
                else -> errorWithAttachment("Unexpected element type: ${anchor::class.simpleName}") {
                    withPsiEntry("anchor", anchor)
                }
            }
        },
    )

    private fun CjElement.fileHeaderAnchorElement(): CjElement? {
        return parentsWithSelf.find { it is CjPackageDirective || it is CjImportDirective } as? CjElement
    }

    private fun findElementInside(firElement: CfirElement, element: CjElement): CfirElement? {
        val elementToSearch = getPsiAsCfirElementSource(element) ?: return null
        val mapping = CfirElementsRecorder.recordElementsFrom(firElement, CfirElementsRecorder())
        return CjToCfirMapping.getCfir(elementToSearch, moduleComponents.session, mapping)
    }

    private fun CfirElementWithResolveState.resolveAndFindTypeRefAnchor(typeReference: CjTypeReference): CfirElement? {
        requireTypeIntersectionWith<CfirAnnotationContainer>()

        lazyResolveToPhase(CfirResolvePhase.ANNOTATION_ARGUMENTS)

        when (this) {
            is CfirCallableDeclaration -> {
                returnTypeRef.takeIf { it.psi == typeReference }?.let { return it }
                receiverParameter?.takeIf { it.typeRef.psi == typeReference }?.let { return it }
            }

            is CfirTypeParameter -> {
                findTypeRefAnchor(typeReference)?.let { return it }
            }

            is CfirClass -> {
                for (typeRef in superTypeRefs) {
                    if (typeRef.psi == typeReference) {
                        return typeRef
                    }
                }
            }

            is CfirTypeAlias -> {
                expandedTypeRef.takeIf { it.psi == typeReference }?.let { return it }
            }
        }

        if (this is CfirTypeParameterRefsOwner) {
            for (typeParameterRef in typeParameters) {
                typeParameterRef.findTypeRefAnchor(typeReference)?.let { return it }
            }
        }

        return null
    }

    private fun CfirTypeParameterRef.findTypeRefAnchor(typeReference: CjTypeReference): CfirElement? {
        if (this !is CfirTypeParameter) return null

        for (typeRef in bounds) {
            if (typeRef.psi == typeReference) {
                return typeRef
            }
        }

        return null
    }

    private fun CfirElementWithResolveState.resolveAndFindAnnotation(
        annotationEntry: CjAnnotationEntry,
        goDeep: Boolean = false,
    ): CfirAnnotation? {
        requireTypeIntersectionWith<CfirAnnotationContainer>()

        lazyResolveToPhase(CfirResolvePhase.ANNOTATION_ARGUMENTS)
        findAnnotation(annotationEntry)?.let { return it }

        if (this is CfirProperty) {
            backingField?.findAnnotation(annotationEntry)?.let { return it }
            getter?.findAnnotation(annotationEntry)?.let { return it }
            setter?.findAnnotation(annotationEntry)?.let { return it }
            setter?.valueParameters?.first()?.findAnnotation(annotationEntry)?.let { return it }
        }

        return when {
            !goDeep -> null
            this is CfirProperty -> correspondingValueParameterFromPrimaryConstructor?.fir?.resolveAndFindAnnotation(annotationEntry)
            this is CfirValueParameter -> correspondingProperty?.resolveAndFindAnnotation(annotationEntry)
            else -> null
        }
    }

    private fun CfirAnnotationContainer.findAnnotation(
        annotationEntry: CjAnnotationEntry,
    ): CfirAnnotation? = annotations.find { it.psi == annotationEntry }
}

internal val CjTypeParameter.containingDeclaration: CjDeclaration?
    get() = (parent as? CjTypeParameterList)?.parent as? CjDeclaration

/**
 * Returns **true** if [this] element is a unit of resolution and can be treated as non-local.
 * The property is supposed to be used only in the pair with
 * [getNonLocalContainingOrThisElement] or [getNonLocalContainingOrThisDeclaration].
 *
 * @see getNonLocalContainingOrThisElement
 */
internal val CjElement.isAutonomousElement: Boolean
    get() = when (this) {
        is CjPropertyAccessor, is CjParameter, is CjTypeParameter -> false
        else -> true
    }

// TODO change predicate (KT-76271)
@CaImplementationDetail
fun PsiElement.getNonLocalContainingOrThisDeclaration(predicate: (CjDeclaration) -> Boolean = { true }): CjDeclaration? {
    return getNonLocalContainingOrThisElement { it is CjDeclaration && predicate(it) } as? CjDeclaration
}

/**
 * Returns the first non-local element from [parentsWithSelf] that contains the given elements, based on the specified predicate.
 *
 * The resulting element can be considered reachable at [RAW_CFIR][CfirResolvePhase.RAW_CFIR] phase.
 *
 * @see org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.FileStructure
 */
internal fun PsiElement.getNonLocalContainingOrThisElement(predicate: (CjElement) -> Boolean = { true }): CjElement? {
    for (parent in parentsWithSelf) {
        if (parent is CjElement && elementCanBeLazilyResolved(parent) && predicate(parent)) {
            return parent
        }
    }

    return null
}

private inline fun <reified T : CjElement> PsiElement.parentsOfType(stopDeclaration: CjDeclaration?): Sequence<T> {
    return parentsWithSelf.takeWhile { it !== stopDeclaration }.filterIsInstance<T>()
}
