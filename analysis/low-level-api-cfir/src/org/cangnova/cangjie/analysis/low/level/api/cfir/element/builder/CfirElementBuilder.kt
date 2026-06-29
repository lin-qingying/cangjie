

package org.cangnova.cangjie.analysis.low.level.api.cfir.element.builder

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.low.level.api.cfir.element.builder.parentsOfType
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.FileStructure
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.CfirElementsRecorder
import org.cangnova.cangjie.analysis.low.level.api.cfir.file.structure.CjToCfirMapping
import org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve.elementCanBeLazilyResolved
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.findSourceNonLocalCfirDeclaration
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.isPartialBodyResolvable
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.requireTypeIntersectionWith
import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.correspondingProperty
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.resolve.getContainingClass
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhaseRecursively
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.parentOfType
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
                is CjUserType if unwrappedElement.parent is CjOptionType -> unwrappedElement.parent as CjOptionType
                else -> unwrappedElement
            }
        }

        private fun doCjElementHasCorrespondingCfirElement(cjElement: CjElement): Boolean = when (cjElement) {
            is CjImportList -> false
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

    /**
     * 构建或读取整个仓颉文件的 raw CFIR，并递归解析到 body resolve。
     */
    private fun getOrBuildCfirForCjFile(cjFile: CjFile): CfirFile {
        val cfirFile = moduleComponents.cfirFileBuilder.buildRawCfirFileWithCaching(cjFile)
        cfirFile.lazyResolveToPhaseRecursively(CfirResolvePhase.BODY_RESOLVE)
        return cfirFile
    }

    /**
     * 为非普通文件 PSI 元素恢复对应 CFIR 元素。
     */
    private fun getCfirForNonCjFileElement(element: CjElement): CfirElement? {
        require(element !is CjFile || element is CjCodeFragment)

        if (!doCjElementHasCorrespondingCfirElement(element)) {
            return null
        }

        val nonLocalContainer = element.getNonLocalContainingOrThisElement()
        tryGetCfirWithoutBodyResolve(nonLocalContainer, element)?.let { return it }

        val psi = getPsiAsCfirElementSource(element) ?: return null
        val cjFile = element.containingCjFile
        val fileStructure = moduleComponents.fileStructureCache.getFileStructure(cjFile)

        val structureElement = fileStructure.getStructureElementFor(element, nonLocalContainer)
        val mappings = structureElement.mappings

        val cfirElement = mappings.get(psi)

        if (cfirElement is CfirElementWithResolveState) {
            // Partially resolvable declarations might have unresolved bodies in the mapping.
            // Here we forcibly resolve them to obey to the 'getOrBuildCfirFor()' contract.
            if (cfirElement.isPartialBodyResolvable && cfirElement.resolvePhase < CfirResolvePhase.BODY_RESOLVE) {
                cfirElement.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
            }
        }

        return cfirElement
    }

    /**
     * Provides a fast path for well-known psi-to-fir mappings to avoid [CfirResolvePhase.BODY_RESOLVE] there it is possible.
     *
     * This optimization makes sense only for [nonLocalContainer]s which might have such expensive resolution.
     * For instance, there is no need to avoid [CfirResolvePhase.BODY_RESOLVE] for dangling modifiers as they don't
     * have bodies, so effectively it is the same as [CfirResolvePhase.ANNOTATION_ARGUMENTS].
     *
     * Declaration containers ([CjFile], [CjTypeStatement]) are resolved till [CfirResolvePhase.ANNOTATION_ARGUMENTS]
     * by default in [FileStructure.getStructureElementFor],
     * so there is no need to use optimized search via [getCfirForElementInsideAnnotations]/[getCfirForElementInsideTypes]
     * as this is redundant work and effectively duplicate the logic of [CjToCfirMapping].
     *
     * [CjTypeStatement] is not excluded yet as in some cases it might trigger additional resolution.
     * For instance, currently it resolves generated members (fake constructor, etc.).
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

    /**
     * 在不推进到 body resolve 的情况下，通过 annotation/type/header 锚点恢复 CFIR 元素。
     */
    private inline fun <T : CjElement, E : PsiElement> getCfirForNonBodyElement(
        element: CjElement,
        nonLocalDeclaration: CjDeclaration?,
        anchorElementProvider: (CjElement) -> T?,
        elementOwnerProvider: (T) -> E?,
        resolveAndFindCfirForAnchor: (CfirElementWithResolveState, T) -> CfirElement?,
    ): CfirElement? {
        val anchorElement = anchorElementProvider(element) ?: return null
        val elementOwner = elementOwnerProvider(anchorElement) ?: return null

        val cfirElementContainer = if (elementOwner is CjFile) {
            moduleComponents.cfirFileBuilder.buildRawCfirFileWithCaching(elementOwner)
        } else {
            if (elementOwner != nonLocalDeclaration) return null

            nonLocalDeclaration.findSourceNonLocalCfirDeclaration(
                cfirFileBuilder = moduleComponents.cfirFileBuilder,
                provider = moduleComponents.session.cfirProvider,
            )
        }

        // There is no need for a custom traverse as the resolved element has a cached map
        if (cfirElementContainer.resolvePhase == CfirResolvePhase.BODY_RESOLVE) {
            return null
        }

        val anchorCfir = resolveAndFindCfirForAnchor(cfirElementContainer, anchorElement) ?: return null
        // We use identity comparison here intentionally to check that it is exactly the object we want to find
        if (element === anchorElement) return anchorCfir

        return findElementInside(cfirElement = anchorCfir, element = element)
    }

    /**
     * 返回 annotation 所属的可注解 PSI 声明。
     */
    private fun PsiElement.annotationOwner(): CjAnnotated? {
        val modifierList = when (val parent = parent) {
            is CjModifierList -> parent
            is CjAnnotation -> return parent.annotationOwner()
            else -> null
        }

        return modifierList?.owner as? CjDeclaration
    }

    /**
     * 通过 annotation 锚点恢复元素对应的 CFIR annotation 或其内部元素。
     */
    private fun getCfirForElementInsideAnnotations(
        element: CjElement,
        nonLocalDeclaration: CjDeclaration,
    ): CfirElement? = getCfirForNonBodyElement(
        element = element,
        nonLocalDeclaration = nonLocalDeclaration,
        anchorElementProvider = { it.parentsOfType<CjAnnotation>(nonLocalDeclaration).firstOrNull() },
        elementOwnerProvider = { it.annotationOwner() },
        resolveAndFindCfirForAnchor = { declaration, anchor -> declaration.resolveAndFindAnnotation(anchor, goDeep = true) },
    )

    /**
     * 通过类型引用锚点恢复元素对应的 CFIR type ref 或其内部元素。
     */
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
    )?.let { cfirElement ->
        cfirElement
    }

    /**
     * 在文件头 package/import 区域内恢复 PSI 元素对应的 CFIR 元素。
     */
    private fun getCfirForElementInsideFileHeader(element: CjElement): CfirElement? = getCfirForNonBodyElement<CjElement, PsiElement>(
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

    /**
     * 返回文件头中的 package 或 import 锚点元素。
     */
    private fun CjElement.fileHeaderAnchorElement(): CjElement? {
        return parentsWithSelf.find { it is CjPackageDirective || it is CjImportDirective } as? CjElement
    }

    /**
     * 在已定位的 CFIR 锚点子树内部继续查找具体 PSI 元素对应的 CFIR 元素。
     */
    private fun findElementInside(cfirElement: CfirElement, element: CjElement): CfirElement? {
        val elementToSearch = getPsiAsCfirElementSource(element) ?: return null
        val mapping = CfirElementsRecorder.recordElementsFrom(cfirElement, CfirElementsRecorder())
        return CjToCfirMapping.getCfir(elementToSearch, moduleComponents.session, mapping)
    }

    /**
     * 将声明解析到 TYPES 阶段并查找指定类型引用对应的 CFIR anchor。
     */
    private fun CfirElementWithResolveState.resolveAndFindTypeRefAnchor(typeReference: CjTypeReference): CfirElement? {
        requireTypeIntersectionWith<CfirAnnotationContainer>()

        lazyResolveToPhase(CfirResolvePhase.TYPES)

        when (this) {
            is CfirCallableDeclaration -> {
                returnTypeRef.takeIf { it.psi == typeReference }?.let { return it }
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

    /**
     * 在 type parameter ref 中查找指定 PSI type reference 对应的 bound type ref。
     */
    private fun CfirTypeParameterRef.findTypeRefAnchor(typeReference: CjTypeReference): CfirElement? {
        if (this !is CfirTypeParameter) return null

        for (typeRef in bounds) {
            if (typeRef.psi == typeReference) {
                return typeRef
            }
        }

        return null
    }

    /**
     * 将声明解析到 TYPES 阶段并查找指定 annotation 对应的 CFIR annotation。
     */
    private fun CfirElementWithResolveState.resolveAndFindAnnotation(
        annotationEntry: CjAnnotation,
        goDeep: Boolean = false,
    ): CfirAnnotation? {
        requireTypeIntersectionWith<CfirAnnotationContainer>()

        lazyResolveToPhase(CfirResolvePhase.TYPES)
        findAnnotation(annotationEntry)?.let { return it }

        if (this is CfirProperty) {
            getter?.findAnnotation(annotationEntry)?.let { return it }
            setter?.findAnnotation(annotationEntry)?.let { return it }
            setter?.valueParameters?.first()?.findAnnotation(annotationEntry)?.let { return it }
        }

        return when {
            !goDeep -> null
            this is CfirProperty -> correspondingValueParameterFromPrimaryConstructor?.resolveAndFindAnnotation(annotationEntry)
            this is CfirValueParameter -> correspondingProperty?.resolveAndFindAnnotation(annotationEntry)
            else -> null
        }
    }

    /**
     * Kotlin FIR 这里直接暴露 `correspondingValueParameterFromPrimaryConstructor`。
     * 仓颉主干当前只保留 `CfirValueParameter.correspondingProperty`，因此这里基于主构造参数反查。
     */
    private val CfirProperty.correspondingValueParameterFromPrimaryConstructor: CfirValueParameter?
        get() {
            val containingClass = getContainingClass() ?: return null
            val primaryConstructor = containingClass.declarations
                .firstOrNull { it is CfirConstructor && it.isPrimary } as? CfirConstructor
                ?: return null
            return primaryConstructor.valueParameters.firstOrNull { it.correspondingProperty === this }
        }

    /**
     * 在 annotation container 中按 PSI annotation 查找 CFIR annotation。
     */
    private fun CfirAnnotationContainer.findAnnotation(
        annotationEntry: CjAnnotation ,
    ): CfirAnnotation? = annotations.find { it.psi == annotationEntry }
}

/**
 * 返回类型参数所属的声明。
 */
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


/**
 * 返回包含当前 PSI 的非局部声明；可通过 predicate 进一步筛选声明。
 */
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

/**
 * 沿父链返回指定类型 PSI 节点，遇到 stopDeclaration 时停止。
 */
private inline fun <reified T : CjElement> PsiElement.parentsOfType(stopDeclaration: CjDeclaration?): Sequence<T> {
    return parentsWithSelf.takeWhile { it !== stopDeclaration }.filterIsInstance<T>()
}
