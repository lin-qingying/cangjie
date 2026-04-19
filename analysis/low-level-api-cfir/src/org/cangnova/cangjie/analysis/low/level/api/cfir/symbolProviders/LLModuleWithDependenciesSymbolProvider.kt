/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.deserialization.AbstractCfirDeserializedSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.*
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.utils.addIfNotNull

/**
 * The module-level [CfirSymbolProvider] for an [LLCfirSession], composing multiple kinds of symbol providers for the module's own content
 * ([providers]) and its dependencies ([dependencyProvider]).
 *
 * This class does not implement [LLPsiAwareSymbolProvider] because for specific-module symbol provider access, only own symbol providers
 * should be considered, but not dependencies. [LLModuleWithDependenciesSymbolProvider] must consider dependencies for all its overridden
 * symbol provider functions. As such, the class offers alternative functions like [getClassLikeSymbolByPsiWithoutDependencies].
 *
 * ### Module content inclusion
 *
 * [LLModuleWithDependenciesSymbolProvider] must and must only provide symbols for declarations in the associated module's content scope.
 *
 * In general, the following statements should all be consistent with each other for a given `declaration` and `module`:
 *
 * - `declaration` is included in `module`’s content scope.
 * - The project structure provider provides `module` for `declaration` (or chooses a competing, equally valid candidate based on use-site
 *   module disambiguation).
 * - The symbol provider for `module`'s CFIR session provides a symbol for `declaration`.
 *
 * Content scopes are the source of truth in this matter. As such, the implementation of the symbol provider must be consistent with the
 * content scope. This is not always intuitive. For example, the content scope of a library module may exclude certain files from the
 * library which are nonetheless physically present in an underlying JAR.
 *
 * [CangJieProjectStructureProvider][org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider] has the same
 * responsibility, which is the burden of the Analysis API platform.
 */
internal class LLModuleWithDependenciesSymbolProvider(
    session: LLCfirSession,
    val providers: List<CfirSymbolProvider>,
    val dependencyProvider: LLDependenciesSymbolProvider,
) : CfirSymbolProvider(session) {
    /**
     * This symbol names provider is not used directly by [LLModuleWithDependenciesSymbolProvider], because in the IDE, Java symbol
     * providers currently cannot provide name sets (see KTIJ-24642). So in most cases, name sets would be `null` anyway.
     *
     * However, in Standalone mode, we rely on the symbol names provider to compute classifier/callable name sets for package scopes (see
     * `DeclarationsInPackageProvider`). The fallback declaration provider doesn't work for symbols from binary libraries.
     *
     * [symbolNamesProvider] needs to be lazy to avoid eager initialization of [LLDependenciesSymbolProvider.providers].
     */
    override val symbolNamesProvider: CfirSymbolNamesProvider by lazy {
        CfirCompositeCachedSymbolNamesProvider(
            session,
            buildList {
                providers.mapTo(this) { it.symbolNamesProvider }
                dependencyProvider.providers.mapTo(this) { it.symbolNamesProvider }
            },
        )
    }

    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? =
        getClassLikeSymbolByClassIdWithoutDependencies(classId)
            ?: dependencyProvider.getClassLikeSymbolByClassId(classId)

    fun getClassLikeSymbolByClassIdWithoutDependencies(classId: ClassId): CfirClassLikeSymbol<*>? =
        providers.firstNotNullOfOrNull { it.getClassLikeSymbolByClassId(classId) }

    @LLModuleSpecificSymbolProviderAccess
    fun getClassLikeSymbolByPsiWithoutDependencies(
        classId: ClassId,
        declaration: PsiElement,
    ): CfirClassLikeSymbol<*>? =
        providers.firstNotNullOfOrNull { it.getClassLikeSymbolMatchingPsi(classId, declaration) }

    @CfirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<CfirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        providers.forEach { it.getTopLevelCallableSymbolsTo(destination, packageFqName, name) }
        dependencyProvider.getTopLevelCallableSymbolsTo(destination, packageFqName, name)
    }

    private val multifileClassPartCallableSymbolProvider by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LLKotlinStubBasedLibraryMultifileClassPartCallableSymbolProvider(session)
    }

    @OptIn(CfirSymbolProviderInternals::class)
    fun getTopLevelDeserializedCallableSymbolsWithoutDependencies(
        packageFqName: FqName,
        shortName: Name,
        callableDeclaration: CjCallableDeclaration,
    ): List<CfirCallableSymbol<*>> = buildList {
        providers.forEach { provider ->
            when (provider) {
                is LLKotlinStubBasedLibrarySymbolProvider ->
                    addIfNotNull(provider.getTopLevelCallableSymbol(packageFqName, shortName, callableDeclaration))

                is AbstractCfirDeserializedSymbolProvider ->
                    provider.getTopLevelCallableSymbolsTo(this, packageFqName, shortName)

                else -> {}
            }
        }

        // Must be called after the original search as this is only a fallback solution
        if (isEmpty() && providers.any { it is LLKotlinStubBasedLibrarySymbolProvider }) {
            multifileClassPartCallableSymbolProvider.addCallableIfNeeded(this, packageFqName, shortName, callableDeclaration)
        }
    }

    @CfirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<CfirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        getTopLevelFunctionSymbolsToWithoutDependencies(destination, packageFqName, name)
        dependencyProvider.getTopLevelFunctionSymbolsTo(destination, packageFqName, name)
    }

    @CfirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<CfirPropertySymbol>, packageFqName: FqName, name: Name) {
        getTopLevelPropertySymbolsToWithoutDependencies(destination, packageFqName, name)
        dependencyProvider.getTopLevelPropertySymbolsTo(destination, packageFqName, name)
    }

    @CfirSymbolProviderInternals
    fun getTopLevelFunctionSymbolsToWithoutDependencies(
        destination: MutableList<CfirNamedFunctionSymbol>,
        packageFqName: FqName,
        name: Name
    ) {
        providers.forEach { it.getTopLevelFunctionSymbolsTo(destination, packageFqName, name) }
    }

    @CfirSymbolProviderInternals
    fun getTopLevelPropertySymbolsToWithoutDependencies(destination: MutableList<CfirPropertySymbol>, packageFqName: FqName, name: Name) {
        providers.forEach { it.getTopLevelPropertySymbolsTo(destination, packageFqName, name) }
    }

    override fun hasPackage(fqName: FqName): Boolean =
        hasPackageWithoutDependencies(fqName)
                || dependencyProvider.hasPackage(fqName)

    fun hasPackageWithoutDependencies(fqName: FqName): Boolean =
        providers.any { it.hasPackage(fqName) }
}

internal class LLDependenciesSymbolProvider(
    session: CfirSession,
    computeProviders: () -> List<CfirSymbolProvider>,
) : CfirSymbolProvider(session) {
    /**
     * Dependency symbol providers are lazy to support cyclic dependencies between modules. If a module A and a module B depend on each
     * other and session creation tries to access dependency symbol providers eagerly, the creation of session A would try to create session
     * B (to get its symbol providers), which in turn would try to create session A, and so on.
     */
    val providers: List<CfirSymbolProvider> by lazy {
        computeProviders().also { providers ->
            require(providers.all { it !is LLModuleWithDependenciesSymbolProvider }) {
                "${LLDependenciesSymbolProvider::class.simpleName} may not contain ${LLModuleWithDependenciesSymbolProvider::class.simpleName}:" +
                        " dependency providers must be flattened during session creation."
            }
        }
    }

    override val symbolNamesProvider: CfirSymbolNamesProvider = CfirNullSymbolNamesProvider

    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? =
        providers.firstNotNullOfOrNull { it.getClassLikeSymbolByClassId(classId) }

    @CfirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<CfirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        for (provider in providers) {
            provider.getTopLevelCallableSymbolsTo(destination, packageFqName, name)
        }
    }

    @CfirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<CfirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        for (provider in providers) {
            provider.getTopLevelFunctionSymbolsTo(destination, packageFqName, name)
        }
    }

    @CfirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<CfirPropertySymbol>, packageFqName: FqName, name: Name) {
        for (provider in providers) {
            provider.getTopLevelPropertySymbolsTo(destination, packageFqName, name)
        }
    }

    override fun hasPackage(fqName: FqName): Boolean = providers.any { it.hasPackage(fqName) }
}
