/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieCompositeDeclarationProvider
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProvider
import org.cangnova.cangjie.analysis.api.platform.packages.CangJieCompositePackageProvider
import org.cangnova.cangjie.analysis.api.platform.packages.createPackageProvider
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.util.withPsiEntry
import org.cangnova.cangjie.analysis.api.util.withVirtualFileEntry
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.llCfirModuleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.caches.LLPsiAwareClassLikeSymbolCache
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.CfirElementFinder
import org.cangnova.cangjie.cfir.caches.CfirCache
import org.cangnova.cangjie.cfir.caches.cfirCachesFactory
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompositeCachedSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.utils.exceptions.ExceptionAttachmentBuilder
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment

/**
 * [LLCangJieSourceSymbolProvider] is a [LLCangJieSymbolProvider] which provides symbols for source-based modules, such as [CaSourceModule][org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule].
 */
@OptIn(CaPlatformInterface::class)
internal class LLCangJieSourceSymbolProvider(
    session: LLCfirSession,
    private val moduleComponents: LLCfirModuleResolveComponents,
    declarationProviderFactory: (GlobalSearchScope) -> CangJieDeclarationProvider?,
) : LLCangJieSymbolProvider(session), LLMultiClassLikeSymbolProvider {
    private val searchScope: GlobalSearchScope
        get() = moduleComponents.module.contentScope

    override val declarationProvider = CangJieCompositeDeclarationProvider.create(
        listOfNotNull(
            declarationProviderFactory(searchScope),
        )
    )

    override val packageProvider = CangJieCompositePackageProvider.create(
        listOfNotNull(
            session.project.createPackageProvider(searchScope),
        )
    )

    override val symbolNamesProvider: CfirSymbolNamesProvider = CfirCompositeCachedSymbolNamesProvider.create(
        session,
        listOfNotNull(
            LLCfirCangJieSymbolNamesProvider(declarationProvider),
        )
    )

    private val classLikeCache =
        LLPsiAwareClassLikeSymbolCache(session, ::computeClassLikeSymbolByClassId) { declaration: CjClassLikeDeclaration, _ ->
            computeClassLikeSymbolByPsi(declaration)
        }

    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? {
        if (!symbolNamesProvider.mayHaveTopLevelClassifier(classId)) return null
        return getClassLikeSymbolByClassIdAndDeclaration(classId, classLikeDeclaration = null)
    }

    @LLModuleSpecificSymbolProviderAccess
    override fun getClassLikeSymbolByClassId(classId: ClassId, classLikeDeclaration: CjClassLikeDeclaration): CfirClassLikeSymbol<*>? =
        getClassLikeSymbolByClassIdAndDeclaration(classId, classLikeDeclaration)

    @OptIn(LLModuleSpecificSymbolProviderAccess::class)
    private fun getClassLikeSymbolByClassIdAndDeclaration(
        classId: ClassId,
        classLikeDeclaration: CjClassLikeDeclaration?,
    ): CfirClassLikeSymbol<*>? {
        return classLikeCache.getSymbolByClassId(
            classId,
            classLikeDeclaration,
            buildAdditionalAttachments = buildAdditionalAttachmentsForClassLikeSymbol,
        )
    }

    @LLModuleSpecificSymbolProviderAccess
    override fun getClassLikeSymbolByPsi(classId: ClassId, declaration: PsiElement): CfirClassLikeSymbol<*>? {
        return classLikeCache.getSymbolByPsi<CjClassLikeDeclaration>(
            classId,
            declaration,
            buildAdditionalAttachments = buildAdditionalAttachmentsForClassLikeSymbol,
        ) { it }
    }

    /**
     * To find out more about KT-62339, we're adding information about whether the declaration for the given class ID can *now* be found by
     * the declaration provider (or is still `null`). And whether the given context element is actually in the scope of the symbol provider.
     */
    private val buildAdditionalAttachmentsForClassLikeSymbol: ExceptionAttachmentBuilder.(ClassId, CjClassLikeDeclaration?) -> Unit =
        { classId, context ->
            val declaration = declarationProvider.getClassLikeDeclarationByClassId(classId)
            withPsiEntry("declarationFromDeclarationProvider", declaration)

            val virtualFile = context?.containingFile?.virtualFile
            withVirtualFileEntry("contextVirtualFile", virtualFile)

            if (virtualFile != null) {
                val isInContentScope = searchScope.contains(virtualFile)
                withEntry("isContextInScope", isInContentScope.toString())
            }
        }

    override fun getAllClassLikeSymbolsByClassId(classId: ClassId): List<CfirClassLikeSymbol<*>> {
        val declarations = declarationProvider.getAllClassesByClassId(classId) + declarationProvider.getAllTypeAliasesByClassId(classId)

        // We're specifically taking the declarations from the declaration provider, so they're guaranteed to be in the symbol provider's
        // module.
        @OptIn(LLModuleSpecificSymbolProviderAccess::class)
        return declarations.mapNotNull { getClassLikeSymbolByPsi(classId, it) }
    }

    private fun computeClassLikeSymbolByClassId(classId: ClassId, context: CjClassLikeDeclaration?): CfirClassLikeSymbol<*>? {
        require(context == null || context.isPhysical)
        val classLikeDeclaration = context ?: declarationProvider.getClassLikeDeclarationByClassId(classId) ?: return null

        if (classLikeDeclaration.getClassId() == null) return null
        return findClassLikeSymbol(classId, classLikeDeclaration) { CfirElementFinder.findClassifierWithClassId(it, classId) }
    }

    private fun computeClassLikeSymbolByPsi(declaration: CjClassLikeDeclaration): CfirClassLikeSymbol<*>? {
        require(declaration.isPhysical)

        val classId = declaration.getClassId() ?: return null
        return findClassLikeSymbol(classId, declaration) { file ->
            CfirElementFinder.findDeclaration(file, declaration) as? CfirClassLikeDeclaration
        }
    }

    private inline fun findClassLikeSymbol(
        classId: ClassId,
        declaration: CjClassLikeDeclaration,
        findCfirElement: (CfirFile) -> CfirClassLikeDeclaration?,
    ): CfirClassLikeSymbol<*> {
        val cfirFile = moduleComponents.cfirFileBuilder.buildRawCfirFileWithCaching(declaration.containingCjFile)
        return findCfirElement(cfirFile)?.symbol
            ?: errorWithAttachment("Classifier was found in CjFile but was not found in CfirFile") {
                withEntry("classifierClassId", classId) { it.asString() }
                withPsiEntry("classifier", declaration, session.llCfirModuleData.caModule)
                withVirtualFileEntry("virtualFile", declaration.containingCjFile.virtualFile)
            }
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<CfirCallableSymbol<*>> {
        if (!symbolNamesProvider.mayHaveTopLevelCallable(packageFqName, name)) return emptyList()
        return getTopLevelCallableSymbols(CallableId(packageFqName, name), callableFiles = null)
    }

    @CfirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<CfirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        if (!symbolNamesProvider.mayHaveTopLevelCallable(packageFqName, name)) return
        destination += getTopLevelCallableSymbols(CallableId(packageFqName, name), callableFiles = null)
    }

    @CfirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(
        destination: MutableList<CfirCallableSymbol<*>>,
        callableId: CallableId,
        callables: Collection<CjCallableDeclaration>,
    ) {
        destination += getTopLevelCallableSymbols(callableId, callables.mapTo(mutableSetOf()) { it.containingCjFile })
    }

    private fun getTopLevelCallableSymbols(callableId: CallableId, callableFiles: Collection<CjFile>?): List<CfirCallableSymbol<*>> {
        val functions = getTopLevelFunctionSymbols(callableId, callableFiles)
        val properties = getTopLevelPropertySymbols(callableId, callableFiles)

        return buildList(functions.size + properties.size) {
            addAll(functions)
            addAll(properties)
        }
    }

    override fun getTopLevelFunctionSymbols(packageFqName: FqName, name: Name): List<CfirNamedFunctionSymbol> {
        if (!symbolNamesProvider.mayHaveTopLevelCallable(packageFqName, name)) return emptyList()
        return getTopLevelFunctionSymbols(CallableId(packageFqName, name), callableFiles = null)
    }

    @CfirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<CfirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        if (!symbolNamesProvider.mayHaveTopLevelCallable(packageFqName, name)) return
        destination += getTopLevelFunctionSymbols(CallableId(packageFqName, name), callableFiles = null)
    }

    @CfirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<CfirNamedFunctionSymbol>,
        callableId: CallableId,
        functions: Collection<CjNamedFunction>,
    ) {
        destination += getTopLevelFunctionSymbols(callableId, functions.mapTo(mutableSetOf()) { it.containingCjFile })
    }

    private val functionCache: CfirCache<CallableId, List<CfirNamedFunctionSymbol>, Collection<CjFile>?> =
        session.cfirCachesFactory.createCache { callableId, context ->
            computeCallableSymbolsByCallableId<CfirNamedFunctionSymbol>(callableId, context)
        }

    private fun getTopLevelFunctionSymbols(callableId: CallableId, callableFiles: Collection<CjFile>?): List<CfirNamedFunctionSymbol> {
        return functionCache.getValue(callableId, callableFiles)
    }

    override fun getTopLevelPropertySymbols(packageFqName: FqName, name: Name): List<CfirPropertySymbol> {
        if (!symbolNamesProvider.mayHaveTopLevelCallable(packageFqName, name)) return emptyList()
        return getTopLevelPropertySymbols(CallableId(packageFqName, name), callableFiles = null)
    }

    @CfirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<CfirPropertySymbol>, packageFqName: FqName, name: Name) {
        if (!symbolNamesProvider.mayHaveTopLevelCallable(packageFqName, name)) return
        destination += getTopLevelPropertySymbols(CallableId(packageFqName, name), callableFiles = null)
    }

    @CfirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(
        destination: MutableList<CfirPropertySymbol>,
        callableId: CallableId,
        properties: Collection<CjProperty>,
    ) {
        destination += getTopLevelPropertySymbols(callableId, properties.mapTo(mutableSetOf()) { it.containingCjFile })
    }

    private val propertyCache: CfirCache<CallableId, List<CfirPropertySymbol>, Collection<CjFile>?> =
        session.cfirCachesFactory.createCache { callableId, context ->
            computeCallableSymbolsByCallableId<CfirPropertySymbol>(callableId, context)
        }

    private fun getTopLevelPropertySymbols(callableId: CallableId, callableFiles: Collection<CjFile>?): List<CfirPropertySymbol> {
        return propertyCache.getValue(callableId, callableFiles)
    }

    /**
     * Locates all the callable symbols of required [TYPE] with the matching [callableId] within a specific set of files.
     * Uses the passed [context] files to avoid index access if available; falls back to the [declarationProvider] otherwise.
     *
     * To work correctly with the [CfirCache], this function has to obey the following contract:
     *
     * It can be called with some [callableId] and a non-null [context] **if and only if** the returned value
     * is going to be the same for the `null` context.
     */
    private inline fun <reified TYPE : CfirCallableSymbol<*>> computeCallableSymbolsByCallableId(
        callableId: CallableId,
        context: Collection<CjFile>?,
    ): List<TYPE> {
        require(context == null || context.all { it.isPhysical })

        // we want to use `getTopLevelCallableFiles` instead of
        // `getTopLevelFunctions/Properties`, because it is highly optimized
        // to retrieve the files in the IDE mode
        val files = context ?: declarationProvider.getTopLevelCallableFiles(callableId)

        if (files.isEmpty()) return emptyList()

        return buildList {
            files.forEach { cjFile ->
                val cfirFile = moduleComponents.cfirFileBuilder.buildRawCfirFileWithCaching(cjFile)
                cfirFile.collectCallableSymbolsOfTypeTo<TYPE>(this, callableId.callableName)
            }
        }
    }

    private inline fun <reified TYPE : CfirCallableSymbol<*>> CfirFile.collectCallableSymbolsOfTypeTo(result: MutableList<TYPE>, name: Name) {
        declarations.mapNotNullTo(result) { declaration ->
            if (declaration is CfirCallableDeclaration && declaration.symbol.name == name) {
                declaration.symbol as? TYPE
            } else null
        }
    }

    override fun hasPackage(fqName: FqName): Boolean {
        return packageProvider.doesPackageExist(fqName)
    }
}
