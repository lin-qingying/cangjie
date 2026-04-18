/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.util

import org.cangnova.cangjie.analysis.api.platform.KotlinDeserializedDeclarationsOrigin
import org.cangnova.cangjie.analysis.api.platform.KotlinPlatformSettings
import org.cangnova.cangjie.analysis.api.platform.projectStructure.KotlinProjectStructureProvider
import org.cangnova.cangjie.analysis.api.utils.errors.withClassEntry
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.services.LLCfirElementByPsiElementChooser
import org.cangnova.cangjie.analysis.low.level.api.cfir.element.builder.containingDeclaration
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.llCfirModuleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLModuleWithDependenciesSymbolProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLModuleSpecificSymbolProviderAccess
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.getClassLikeSymbolByClassIdWithoutDependencies
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.getClassLikeSymbolByPsiWithoutDependencies
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.resolve.providers.symbolProvider
import org.cangnova.cangjie.cfir.scopes.getFunctions
import org.cangnova.cangjie.cfir.scopes.getProperties
import org.cangnova.cangjie.cfir.scopes.impl.declaredMemberScope
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.impl.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.impl.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.impl.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.impl.CfirPropertySymbol
import org.cangnova.cangjie.cfir.utils.exceptions.withCfirEntry
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.containingClassOrObject
import org.cangnova.cangjie.utils.exceptions.ExceptionAttachmentBuilder
import org.cangnova.cangjie.utils.exceptions.withPsiEntry

/**
 * Allows to search for CFIR declarations by compiled [CjDeclaration]s.
 */
internal class CfirDeclarationForCompiledElementSearcher(private val session: LLCfirSession) {
    private val project get() = session.project

    private val projectStructureProvider by lazy(LazyThreadSafetyMode.PUBLICATION) {
        KotlinProjectStructureProvider.getInstance(project)
    }

    private val firElementByPsiElementChooser by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LLCfirElementByPsiElementChooser.getInstance(project)
    }

    fun findNonLocalDeclaration(ktDeclaration: CjDeclaration): CfirDeclaration = when (ktDeclaration) {
        is CjEnumEntry -> findNonLocalEnumEntry(ktDeclaration)
        is CjClassLikeDeclaration -> findNonLocalClassLikeDeclaration(ktDeclaration)
        is CjConstructor<*> -> findConstructorOfNonLocalClass(ktDeclaration)
        is CjNamedFunction -> findNonLocalFunction(ktDeclaration)
        is CjProperty -> findNonLocalProperty(ktDeclaration)
        is CjParameter -> findParameter(ktDeclaration)
        is CjPropertyAccessor -> findNonLocalPropertyAccessor(ktDeclaration)
        is CjTypeParameter -> findNonLocalTypeParameter(ktDeclaration)

        else -> errorWithCfirSpecificEntries("Unsupported compiled declaration of type", psi = ktDeclaration)
    }

    private fun findFunctionCandidates(function: CjNamedFunction): List<CfirFunctionSymbol<*>> =
        findCallableCandidates(function, function.isTopLevel).filterIsInstance<CfirFunctionSymbol<*>>()

    private fun findPropertyCandidates(property: CjProperty): List<CfirPropertySymbol> =
        findCallableCandidates(property, property.isTopLevel).filterIsInstance<CfirPropertySymbol>()

    private fun findCallableCandidates(
        declaration: CjCallableDeclaration,
        isTopLevel: Boolean,
    ): List<CfirCallableSymbol<*>> {
        val shortName = declaration.nameAsSafeName

        if (isTopLevel) {
            val packageFqName = declaration.containingCjFile.packageFqName

            return when (val symbolProvider = session.symbolProvider) {
                is LLModuleWithDependenciesSymbolProvider ->
                    symbolProvider.getTopLevelDeserializedCallableSymbolsWithoutDependencies(packageFqName, shortName, declaration)

                else -> symbolProvider.getTopLevelCallableSymbols(packageFqName, shortName)
            }
        }

        val containingClass = declaration.containingClassOrObject?.let(::findNonLocalClassLikeDeclaration)
            ?: errorWithCfirSpecificEntries("No containing non-local declaration found for", psi = declaration)

        val scope = session.declaredMemberScope(containingClass as CfirClass, memberRequiredPhase = null)
        return when (declaration) {
            is CjProperty -> scope.getProperties(shortName)
            is CjNamedFunction -> scope.getFunctions(shortName)
            else -> errorWithCfirSpecificEntries("Unexpected callable ${declaration::class.simpleName}") {
                withEntry("isTopLevel", isTopLevel.toString())
                withPsiEntry("declaration", declaration)
            }
        }
    }

    private fun findNonLocalTypeParameter(param: CjTypeParameter): CfirDeclaration {
        val owner = param.containingDeclaration ?: errorWithCfirSpecificEntries("Unsupported compiled type parameter", psi = param)
        val firDeclaration = findNonLocalDeclaration(owner)
        val firTypeParameterRefOwner = firDeclaration as? CfirTypeParameterRefsOwner ?: errorWithCfirSpecificEntries(
            "No fir found by $owner",
            psi = owner,
            fir = firDeclaration,
        )

        return firTypeParameterRefOwner.typeParameters.find { typeParameterRef ->
            firElementByPsiElementChooser.isMatchingTypeParameter(param, typeParameterRef.symbol.fir)
        } as CfirDeclaration
    }

    private fun findParameter(param: CjParameter): CfirDeclaration {
        val ownerDeclaration = param.ownerDeclaration ?: errorWithCfirSpecificEntries("Unsupported compiled parameter", psi = param)
        val firDeclaration = findNonLocalDeclaration(ownerDeclaration)
        val firFunction = firDeclaration as? CfirFunction ?: errorWithCfirSpecificEntries(
            "${CfirFunction::class.simpleName} expected but ${firDeclaration::class.simpleName} found",
            psi = ownerDeclaration,
            fir = firDeclaration,
        )

        return firFunction.valueParameters.find { firElementByPsiElementChooser.isMatchingValueParameter(param, it) }
            ?: errorWithCfirSpecificEntries("No fir value parameter found", psi = param, fir = firFunction)
    }

    private fun findNonLocalEnumEntry(declaration: CjEnumEntry): CfirEnumEntry {
        val classCandidate = declaration.containingClassOrObject?.let(::findNonLocalClassLikeDeclaration)
            ?: errorWithCfirSpecificEntries("Enum entry must have containing class", psi = declaration)

        return (classCandidate as CfirRegularClass).declarations.first {
            it is CfirEnumEntry && firElementByPsiElementChooser.isMatchingEnumEntry(declaration, it)
        } as CfirEnumEntry
    }

    private fun findNonLocalClassLikeDeclaration(declaration: CjClassLikeDeclaration): CfirClassLikeDeclaration {
        val classId = declaration.getClassId() ?: errorWithCfirSpecificEntries("Non-local class should have classId", psi = declaration)

        // With the `BINARIES` origin, deserialized CFIR declarations don't have associated PSI elements. Hence, we cannot use `*ByPsi*
        // functions, as they check the candidate's associated PSI.
        val classLikeSymbol = when (KotlinPlatformSettings.getInstance(project).deserializedDeclarationsOrigin) {
            KotlinDeserializedDeclarationsOrigin.BINARIES -> findBinaryClassLikeSymbol(classId)
            KotlinDeserializedDeclarationsOrigin.STUBS -> findStubClassLikeSymbol(classId, declaration)
        }

        classLikeSymbol?.let { return it.fir }

        errorWithCfirSpecificEntries(
            "We should be able to find a symbol for class-like declaration",
            psi = declaration,
        ) {
            withEntry("classId", classId) { it.asString() }

            val contextualModule = session.llCfirModuleData.ktModule
            val moduleForFile = projectStructureProvider.getModule(declaration, contextualModule)
            withEntry("ktModule", moduleForFile) { it.moduleDescription }
        }
    }

    private fun findBinaryClassLikeSymbol(classId: ClassId): CfirClassLikeSymbol<*>? =
        session.symbolProvider.getClassLikeSymbolByClassIdWithoutDependencies(classId)

    /**
     * Note regarding [LLModuleSpecificSymbolProviderAccess]: [CfirDeclarationForCompiledElementSearcher] must be queried with PSI elements
     * that are contained in the compiled element searcher's module. As such, it's also legal to call module-specific symbol provider
     * functions on that module's symbol provider.
     */
    @OptIn(LLModuleSpecificSymbolProviderAccess::class)
    private fun findStubClassLikeSymbol(classId: ClassId, declaration: CjClassLikeDeclaration): CfirClassLikeSymbol<*>? =
        session.symbolProvider.getClassLikeSymbolByPsiWithoutDependencies(classId, declaration)

    private fun findConstructorOfNonLocalClass(declaration: CjConstructor<*>): CfirConstructor {
        val containingClass = declaration.containingClassOrObject
            ?: errorWithCfirSpecificEntries("Constructor must have outer class", psi = declaration)

        val containingCfirClass = findNonLocalClassLikeDeclaration(containingClass) as CfirClass
        val constructorCandidate = containingCfirClass.constructors(session)
            .singleOrNull { firElementByPsiElementChooser.isMatchingCallableDeclaration(declaration, it.fir) }
            ?: errorWithCfirSpecificEntries("We should be able to find a constructor", psi = declaration, fir = containingCfirClass)

        return constructorCandidate.fir
    }

    private fun findNonLocalFunction(declaration: CjNamedFunction): CfirFunction {
        require(!declaration.isLocal)

        val candidates = findFunctionCandidates(declaration)
        val functionCandidate = candidates.firstOrNull { firElementByPsiElementChooser.isMatchingCallableDeclaration(declaration, it.fir) }
            ?: errorWithCfirSpecificEntries("We should be able to find a symbol for function", psi = declaration) {
                withCandidates(candidates)
            }

        return functionCandidate.fir
    }

    private fun findNonLocalProperty(declaration: CjProperty): CfirProperty {
        require(!declaration.isLocal)

        val candidates = findPropertyCandidates(declaration)
        val propertyCandidate = candidates.firstOrNull { firElementByPsiElementChooser.isMatchingCallableDeclaration(declaration, it.fir) }
            ?: errorWithCfirSpecificEntries("We should be able to find a symbol for property", psi = declaration) {
                withCandidates(candidates)
            }

        return propertyCandidate.fir
    }

    private fun findNonLocalPropertyAccessor(declaration: CjPropertyAccessor): CfirPropertyAccessor {
        val firProperty = findNonLocalProperty(declaration.property)

        return (if (declaration.isGetter) firProperty.getter else firProperty.setter)
            ?: errorWithCfirSpecificEntries("We should be able to find a symbol for property accessor", psi = declaration)
    }
}

private fun ExceptionAttachmentBuilder.withCandidates(candidates: List<CfirBasedSymbol<*>>) {
    withEntry("Candidates count", candidates.size.toString())
    for ((index, candidate) in candidates.withIndex()) {
        val ktModule = candidate.llCfirModuleData.ktModule
        withEntryGroup(index.toString()) {
            withClassEntry("candidateClass", candidate)
            withEntry("module", ktModule) { it.moduleDescription }
            withEntry("origin", candidate.origin.toString())
            withCfirEntry("candidateCfir", candidate.fir)

        }
    }
}
