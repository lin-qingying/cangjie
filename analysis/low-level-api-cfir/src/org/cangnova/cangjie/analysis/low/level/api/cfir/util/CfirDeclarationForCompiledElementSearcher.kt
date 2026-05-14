@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.util

import org.cangnova.cangjie.analysis.api.platform.CaDeserializedDeclarationsOrigin
import org.cangnova.cangjie.analysis.api.platform.CaPlatformSettings
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider
import org.cangnova.cangjie.analysis.api.util.withClassEntry
import org.cangnova.cangjie.analysis.api.util.withPsiEntry
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.services.LLCfirElementByPsiElementChooser
import org.cangnova.cangjie.analysis.low.level.api.cfir.element.builder.containingDeclaration
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.llCfirModuleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLModuleWithDependenciesSymbolProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLModuleSpecificSymbolProviderAccess
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.getClassLikeSymbolByClassIdWithoutDependencies
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.getClassLikeSymbolByPsiWithoutDependencies
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.containingTypeStatement
import org.cangnova.cangjie.utils.exceptions.ExceptionAttachmentBuilder
import org.cangnova.cangjie.utils.exceptions.withCfirEntry

/**
 * Allows to search for CFIR declarations by compiled [CjDeclaration]s.
 */
internal class CfirDeclarationForCompiledElementSearcher(private val session: LLCfirSession) {
    private val project get() = session.project

    private val projectStructureProvider by lazy(LazyThreadSafetyMode.PUBLICATION) {
        CangJieProjectStructureProvider.getInstance(project)
    }

    private val cfirElementByPsiElementChooser by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LLCfirElementByPsiElementChooser.getInstance(project)
    }

    fun findNonLocalDeclaration(cjDeclaration: CjDeclaration): CfirDeclaration = when (cjDeclaration) {
        is CjClassLikeDeclaration -> findNonLocalClassLikeDeclaration(cjDeclaration)
        is CjConstructor<*> -> findConstructorOfNonLocalClass(cjDeclaration)
        is CjNamedFunction -> findNonLocalFunction(cjDeclaration)
        is CjProperty -> findNonLocalProperty(cjDeclaration)
        is CjParameter -> findParameter(cjDeclaration)
        is CjPropertyAccessor -> findNonLocalPropertyAccessor(cjDeclaration)
        is CjTypeParameter -> findNonLocalTypeParameter(cjDeclaration)

        else -> errorWithCfirSpecificEntries("Unsupported compiled declaration of type", psi = cjDeclaration)
    }

    private fun findFunctionCandidates(function: CjNamedFunction): List<CfirFunctionSymbol<*>> =
        findCallableCandidates(function, function.parent is CjFile).filterIsInstance<CfirFunctionSymbol<*>>()

    private fun findPropertyCandidates(property: CjProperty): List<CfirPropertySymbol> =
        findCallableCandidates(property, property.parent is CjFile).filterIsInstance<CfirPropertySymbol>()

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

        val containingClass = declaration.containingTypeStatement?.let(::findNonLocalClassLikeDeclaration)
            ?: errorWithCfirSpecificEntries("No containing non-local declaration found for", psi = declaration)

        return when (declaration) {
            is CjProperty -> containingClass.declarations
                .filterIsInstance<CfirProperty>()
                .filter { it.name == shortName }
                .mapTo(mutableListOf()) { it.symbol }
            is CjNamedFunction -> containingClass.declarations
                .filterIsInstance<CfirFunction>()
                .filter { it.symbol.name == shortName }
                .mapTo(mutableListOf()) { it.symbol }
            else -> errorWithCfirSpecificEntries("Unexpected callable ${declaration::class.simpleName}") {
                withEntry("isTopLevel", isTopLevel.toString())
                withPsiEntry("declaration", declaration)
            }
        }
    }

    private fun findNonLocalTypeParameter(param: CjTypeParameter): CfirDeclaration {
        val owner = param.containingDeclaration ?: errorWithCfirSpecificEntries("Unsupported compiled type parameter", psi = param)
        val cfirDeclaration = findNonLocalDeclaration(owner)
        val cfirTypeParameterRefOwner = cfirDeclaration as? CfirTypeParameterRefsOwner ?: errorWithCfirSpecificEntries(
            "No cfir found by $owner",
            psi = owner,
            cfir = cfirDeclaration,
        )

        return cfirTypeParameterRefOwner.typeParameters.find { typeParameterRef ->
            cfirElementByPsiElementChooser.isMatchingTypeParameter(param, typeParameterRef.symbol.cfir)
        } as CfirDeclaration
    }

    private fun findParameter(param: CjParameter): CfirDeclaration {
        val ownerDeclaration = param.ownerFunction ?: errorWithCfirSpecificEntries("Unsupported compiled parameter", psi = param)
        val cfirDeclaration = findNonLocalDeclaration(ownerDeclaration)
        val cfirFunction = cfirDeclaration as? CfirFunction ?: errorWithCfirSpecificEntries(
            "${CfirFunction::class.simpleName} expected but ${cfirDeclaration::class.simpleName} found",
            psi = ownerDeclaration,
            cfir = cfirDeclaration,
        )

        return cfirFunction.valueParameters.find { cfirElementByPsiElementChooser.isMatchingValueParameter(param, it) }
            ?: errorWithCfirSpecificEntries("No cfir value parameter found", psi = param, cfir = cfirFunction)
    }

    private fun findNonLocalClassLikeDeclaration(declaration: CjClassLikeDeclaration): CfirClassLikeDeclaration {
        val classId = declaration.getClassId() ?: errorWithCfirSpecificEntries("Non-local class should have classId", psi = declaration)

        // With the `BINARIES` origin, deserialized CFIR declarations don't have associated PSI elements. Hence, we cannot use `*ByPsi*`
        // functions, as they check the candidate's associated PSI.
        val classLikeSymbol = when (CaPlatformSettings.getInstance(project).deserializedDeclarationsOrigin) {
            CaDeserializedDeclarationsOrigin.BINARIES -> findBinaryClassLikeSymbol(classId)
            CaDeserializedDeclarationsOrigin.STUBS -> findStubClassLikeSymbol(classId, declaration)
            else -> null
        }

        classLikeSymbol?.let { return it.cfir }

        errorWithCfirSpecificEntries(
            "We should be able to find a symbol for class-like declaration",
            psi = declaration,
        ) {
            withEntry("classId", classId) { it.asString() }

            val contextualModule = session.llCfirModuleData.caModule
            val moduleForFile = projectStructureProvider.getModule(declaration, contextualModule)
            withEntry("caModule", moduleForFile) { it.moduleDescription }
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
        val containingClass = declaration.containingTypeStatement
            ?: errorWithCfirSpecificEntries("Constructor must have outer class", psi = declaration)

        val containingCfirClass = findNonLocalClassLikeDeclaration(containingClass)
        val constructorCandidate = containingCfirClass.declarations
            .filterIsInstance<CfirConstructor>()
            .singleOrNull { cfirElementByPsiElementChooser.isMatchingCallableDeclaration(declaration, it) }
            ?: errorWithCfirSpecificEntries("We should be able to find a constructor", psi = declaration, cfir = containingCfirClass)

        return constructorCandidate
    }

    private fun findNonLocalFunction(declaration: CjNamedFunction): CfirFunction {
        require(!declaration.isLocal)

        val candidates = findFunctionCandidates(declaration)
        val functionCandidate = candidates.firstOrNull { cfirElementByPsiElementChooser.isMatchingCallableDeclaration(declaration, it.cfir) }
            ?: errorWithCfirSpecificEntries("We should be able to find a symbol for function", psi = declaration) {
                withCandidates(candidates)
            }

        return functionCandidate.cfir
    }

    private fun findNonLocalProperty(declaration: CjProperty): CfirProperty {
        require(!declaration.isLocal)

        val candidates = findPropertyCandidates(declaration)
        val propertyCandidate = candidates.firstOrNull { cfirElementByPsiElementChooser.isMatchingCallableDeclaration(declaration, it.cfir) }
            ?: errorWithCfirSpecificEntries("We should be able to find a symbol for property", psi = declaration) {
                withCandidates(candidates)
            }

        return propertyCandidate.cfir
    }

    private fun findNonLocalPropertyAccessor(declaration: CjPropertyAccessor): CfirPropertyAccessor {
        val cfirProperty = findNonLocalProperty(declaration.property)

        return (if (declaration.isGetter) cfirProperty.getter else cfirProperty.setter)
            ?: errorWithCfirSpecificEntries("We should be able to find a symbol for property accessor", psi = declaration)
    }
}

private fun ExceptionAttachmentBuilder.withCandidates(candidates: List<CfirBasedSymbol<*>>) {
    withEntry("Candidates count", candidates.size.toString())
    for ((index, candidate) in candidates.withIndex()) {
        val caModule = candidate.llCfirModuleData.caModule
        withEntryGroup(index.toString()) {
            withClassEntry("candidateClass", candidate)
            withEntry("module", caModule) { it.moduleDescription }
            withEntry("origin", candidate.origin.toString())
            withCfirEntry("candidateCfir", candidate.cfir)

        }
    }
}
