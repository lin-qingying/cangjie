/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.lazy.resolve

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.CjSourceElement
import org.cangnova.cangjie.analysis.api.utils.errors.withPsiEntry
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.CfirDesignation
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.llCfirModuleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.codeFragment
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.errorWithCfirSpecificEntries
import org.cangnova.cangjie.cfir.*
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.utils.isInner
import org.cangnova.cangjie.cfir.expressions.CfirMultiDelegatedConstructorCall
import org.cangnova.cangjie.cfir.references.CfirSuperReference
import org.cangnova.cangjie.cfir.scopes.CfirScopeProvider
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.psi
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.containingClassOrObject
import org.cangnova.cangjie.util.PrivateForInline
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withPsiEntry

internal class RawCfirNonLocalDeclarationBuilder private constructor(
    session: CfirSession,
    baseScopeProvider: CfirScopeProvider,
    private val originalDeclaration: CfirDeclaration,
    private val declarationToBuild: CjElement,
    private val functionsToRebind: Set<CfirFunction>,
) : PsiRawCfirBuilder(session, baseScopeProvider, bodyBuildingMode = BodyBuildingMode.NORMAL) {
    companion object {
        fun buildWithFunctionSymbolRebind(
            session: CfirSession,
            scopeProvider: CfirScopeProvider,
            designation: CfirDesignation,
            rootNonLocalDeclaration: CjAnnotated,
        ): CfirDeclaration {
            val functionsToRebind = when (val originalDeclaration = designation.target) {
                is CfirFunction -> setOf(originalDeclaration)
                is CfirProperty -> setOfNotNull(originalDeclaration.getter, originalDeclaration.setter)
                else -> emptySet()
            }

            return build(session, scopeProvider, designation, rootNonLocalDeclaration, functionsToRebind)
        }

        private fun build(
            session: CfirSession,
            scopeProvider: CfirScopeProvider,
            designation: CfirDesignation,
            rootNonLocalDeclaration: CjElement,
            functionsToRebind: Set<CfirFunction>,
        ): CfirDeclaration {
            check(rootNonLocalDeclaration is CjDeclaration || rootNonLocalDeclaration is CjCodeFragment)

            val builder = RawCfirNonLocalDeclarationBuilder(
                session = session,
                baseScopeProvider = scopeProvider,
                originalDeclaration = designation.target as CfirDeclaration,
                declarationToBuild = rootNonLocalDeclaration,
                functionsToRebind = functionsToRebind,
            )

            builder.context.packageFqName = rootNonLocalDeclaration.containingCjFile.packageFqName
            @OptIn(PrivateForInline::class)
            builder.context.forcedContainerSymbol = designation.target.symbol

            return builder.moveNext(designation.path.iterator(), containingDeclaration = null)
        }
    }

    override fun bindFunctionTarget(target: CfirFunctionTarget, function: CfirFunction) {
        super.bindFunctionTarget(target, computeRebindTarget(function) ?: function)
    }

    /**
     * @return [CfirFunction] if another function should be used instead of [function] for [CfirFunctionTarget]
     *
     * @see bindFunctionTarget
     * @see functionsToRebind
     */
    private fun computeRebindTarget(function: CfirFunction): CfirFunction? {
        if (functionsToRebind.isNullOrEmpty()) return null
        val realPsi = function.realPsi
        if (realPsi != null) {
            return functionsToRebind.firstOrNull { it.realPsi == realPsi }
        }

        val accessor = function as? CfirPropertyAccessor ?: return null
        val accessorPsi = accessor.psi ?: return null

        return functionsToRebind.firstOrNull { it is CfirPropertyAccessor && it.isGetter == accessor.isGetter && it.psi == accessorPsi }
    }

    override fun addCapturedTypeParameters(
        status: Boolean,
        declarationSource: CjSourceElement?,
        currentCfirTypeParameters: List<CfirTypeParameterRef>,
    ) {
        if (originalDeclaration is CfirTypeParameterRefsOwner && declarationSource?.psi == originalDeclaration.psi) {
            super.addCapturedTypeParameters(status, declarationSource, originalDeclaration.typeParameters)
        } else {
            super.addCapturedTypeParameters(status, declarationSource, currentCfirTypeParameters)
        }
    }

    private inner class VisitorWithReplacement(private val containingClass: CfirRegularClass?) : Visitor() {
        private fun extractContructorConversionParams(
            classOrObject: CjClassOrObject,
            constructor: CjConstructor<*>?,
        ): ConstructorConversionParams {
            val typeParameters = mutableListOf<CfirTypeParameterRef>()
            context.appendOuterTypeParameters(ignoreLastLevel = false, typeParameters)
            val containingClass = this.containingClass ?: errorWithAttachment("Constructor outside of class") {
                withPsiEntry("constructor", constructor, baseSession.llCfirModuleData.ktModule)
            }
            val selfType = classOrObject.toDelegatedSelfType(typeParameters, containingClass.symbol)
            val allSuperTypeCallEntries = classOrObject.superTypeListEntries.filterIsInstance<CjSuperTypeCallEntry>()
            val superTypeCallEntry = allSuperTypeCallEntries.lastOrNull()
            return ConstructorConversionParams(superTypeCallEntry, selfType, typeParameters, allSuperTypeCallEntries)
        }

        override fun visitSecondaryConstructor(constructor: CjSecondaryConstructor, data: CfirElement?): CfirElement {
            val classOrObject = constructor.getContainingClassOrObject()
            val params = extractContructorConversionParams(classOrObject, constructor)
            val delegatedTypeRef = (originalDeclaration as CfirConstructor).delegatedConstructor?.constructedTypeRef ?: params.selfType
            return constructor.toCfirConstructor(
                delegatedTypeRef,
                params.selfType,
                classOrObject,
                params.typeParameters,
            )
        }

        fun processPrimaryConstructor(classOrObject: CjClassOrObject, constructor: CjPrimaryConstructor?): CfirElement {
            val params = extractContructorConversionParams(classOrObject, constructor)
            val firConstructor = originalDeclaration as CfirConstructor
            val allSuperTypeCallEntries = if (params.allSuperTypeCallEntries.size <= 1) {
                params.allSuperTypeCallEntries.map { it to firConstructor.delegatedConstructor!!.constructedTypeRef }
            } else {
                params.allSuperTypeCallEntries.zip((firConstructor.delegatedConstructor as CfirMultiDelegatedConstructorCall).delegatedConstructorCalls.map { it.constructedTypeRef })
            }
            val newConstructor = constructor.toCfirConstructor(
                params.superTypeCallEntry,
                firConstructor.delegatedConstructor?.constructedTypeRef,
                params.selfType,
                classOrObject,
                params.typeParameters,
                allSuperTypeCallEntries,
                firConstructor.delegatedConstructor == null,
                copyConstructedTypeRefWithImplicitSource = false,
            )
            val delegatedConstructor = firConstructor.delegatedConstructor
            if (delegatedConstructor is CfirMultiDelegatedConstructorCall) {
                for ((oldExcessiveDelegate, newExcessiveDelegate) in delegatedConstructor.delegatedConstructorCalls
                    .zip((newConstructor.delegatedConstructor as CfirMultiDelegatedConstructorCall).delegatedConstructorCalls)) {
                    val calleeReferenceForExessiveDelegate = oldExcessiveDelegate.calleeReference
                    if (calleeReferenceForExessiveDelegate is CfirSuperReference) {
                        (newExcessiveDelegate.calleeReference as? CfirSuperReference)
                            ?.replaceSuperTypeRef(calleeReferenceForExessiveDelegate.superTypeRef)
                    }
                }
            } else {
                val calleeReference = delegatedConstructor?.calleeReference
                if (calleeReference is CfirSuperReference) {
                    (newConstructor.delegatedConstructor?.calleeReference as? CfirSuperReference)?.replaceSuperTypeRef(calleeReference.superTypeRef)
                }
            }
            return newConstructor
        }

        override fun visitPrimaryConstructor(constructor: CjPrimaryConstructor, data: CfirElement?): CfirElement =
            processPrimaryConstructor(constructor.getContainingClassOrObject(), constructor)

    }

    private fun moveNext(iterator: Iterator<CfirDeclaration>, containingDeclaration: CfirDeclaration?): CfirDeclaration {
        if (!iterator.hasNext()) {
            val containingClass = containingDeclaration as? CfirRegularClass
            val visitor = VisitorWithReplacement(containingClass)
            return when (declarationToBuild) {
                is CjProperty -> {
                    val ownerSymbol = containingClass?.symbol
                    visitor.convertProperty(declarationToBuild, ownerSymbol)
                }
                is CjConstructor<*> -> {
                    if (containingClass == null) {
                        // Constructor outside of class, syntax error, we should not do anything
                        originalDeclaration
                    } else {
                        visitor.convertElement(declarationToBuild, originalDeclaration)
                    }
                }
                is CjClassOrObject -> {
                    when {
                        originalDeclaration is CfirConstructor -> visitor.processPrimaryConstructor(declarationToBuild, null)
                        else -> visitor.convertElement(declarationToBuild, originalDeclaration)
                    }
                }
                is CjCodeFragment -> {
                    val firFile = visitor.convertElement(declarationToBuild, originalDeclaration) as CfirFile
                    firFile.codeFragment
                }
                else -> visitor.convertElement(declarationToBuild, originalDeclaration)
            } as CfirDeclaration
        }

        val parent = iterator.next()
        if (parent !is CfirRegularClass) return moveNext(iterator, containingDeclaration = parent)

        val classOrObject = parent.psi
        if (classOrObject !is CjClassOrObject) {
            errorWithCfirSpecificEntries("Expected CjClassOrObject is not found", fir = parent, psi = classOrObject)
        }

        withChildClassName(classOrObject.nameAsSafeName) {
            withCapturedTypeParameters(
                parent.isInner,
                declarationSource = null,
                parent.typeParameters.subList(0, classOrObject.typeParameters.size)
            ) {
                registerSelfType(classOrObject.toDelegatedSelfType(parent))
                return moveNext(iterator, parent)
            }
        }
    }

    private fun PsiElement.toDelegatedSelfType(firClass: CfirRegularClass): CfirResolvedTypeRef =
        toDelegatedSelfType(firClass.typeParameters, firClass.symbol)

    private data class ConstructorConversionParams(
        val superTypeCallEntry: CjSuperTypeCallEntry?,
        val selfType: CfirTypeRef,
        val typeParameters: List<CfirTypeParameterRef>,
        val allSuperTypeCallEntries: List<CjSuperTypeCallEntry>,
    )
}
