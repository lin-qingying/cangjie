/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.platform.declarations.KotlinCompositeDeclarationProvider
import org.cangnova.cangjie.analysis.api.platform.declarations.KotlinDeclarationProvider
import org.cangnova.cangjie.analysis.api.platform.packages.KotlinCompositePackageProvider
import org.cangnova.cangjie.analysis.api.platform.packages.createPackageProvider
import org.cangnova.cangjie.analysis.api.projectStructure.analysisContextModule
import org.cangnova.cangjie.analysis.api.utils.errors.withCaModuleEntry
import org.cangnova.cangjie.analysis.api.utils.errors.withPsiEntry
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirModuleResolveComponents
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.llCfirModuleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.caches.LLPsiAwareClassLikeSymbolCache
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.CfirElementFinder
import org.cangnova.cangjie.config.AnalysisFlags
import org.cangnova.cangjie.cfir.caches.CfirCache
import org.cangnova.cangjie.cfir.caches.firCachesFactory
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.languageVersionSettings
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompositeCachedSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals
import org.cangnova.cangjie.cfir.smartPlus
import org.cangnova.cangjie.cfir.symbols.impl.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.impl.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.impl.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.impl.CfirPropertySymbol
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.ClassIdBasedLocality
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.utils.exceptions.ExceptionAttachmentBuilder
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.withPsiEntry
import org.cangnova.cangjie.utils.exceptions.withVirtualFileEntry

/**
 * [LLKotlinSourceSymbolProvider] is a [LLKotlinSymbolProvider] which provides symbols for source-based modules, such as [CaSourceModule][org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule].
 */
internal class LLKotlinSourceSymbolProvider private constructor(
    session: LLCfirSession,
    private val moduleComponents: LLCfirModuleResolveComponents,
    canContainKotlinPackage: Boolean,
    declarationProviderFactory: (GlobalSearchScope) -> KotlinDeclarationProvider?,
) : LLKotlinSymbolProvider(session), LLMultiClassLikeSymbolProvider {
    constructor(
        session: LLCfirSession,
        moduleComponents: LLCfirModuleResolveComponents,
        canContainKotlinPackage: Boolean,
        declarationProviderFactory: (GlobalSearchScope) -> KotlinDeclarationProvider?,
    ) : this(session, moduleComponents, canContainKotlinPackage, declarationProviderFactory)

    private val searchScope: GlobalSearchScope
        get() = moduleComponents.module.contentScope

    override val declarationProvider = KotlinCompositeDeclarationProvider.create(
        listOfNotNull(
            declarationProviderFactory(searchScope),
        )
    )

    override val packageProvider = KotlinCompositePackageProvider.create(
        listOfNotNull(
            session.project.createPackageProvider(searchScope),
        )
    )

    override val allowKotlinPackage: Boolean =
        canContainKotlinPackage || session.languageVersionSettings.getFlag(AnalysisFlags.allowKotlinPackage)

    override val symbolNamesProvider: CfirSymbolNamesProvider = CfirCompositeCachedSymbolNamesProvider.create(
        session,
        listOfNotNull(
            LLCfirKotlinSymbolNamesProvider(declarationProvider, allowKotlinPackage),
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

    @OptIn(LLModuleSpecificSymbolProviderAccess::class, ClassIdBasedLocality::class)
    private fun getClassLikeSymbolByClassIdAndDeclaration(
        classId: ClassId,
        classLikeDeclaration: CjClassLikeDeclaration?,
    ): CfirClassLikeSymbol<*>? {
        if (!classId.isAccepted()) return null
        return classLikeCache.getSymbolByClassId(
            classId,
            classLikeDeclaration,
            buildAdditionalAttachments = buildAdditionalAttachmentsForClassLikeSymbol,
        )
    }

    @LLModuleSpecificSymbolProviderAccess
    @OptIn(ClassIdBasedLocality::class)
    override fun getClassLikeSymbolByPsi(classId: ClassId, declaration: PsiElement): CfirClassLikeSymbol<*>? {
        if (!classId.isAccepted()) return null
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

                @Suppress("DEPRECATION")
                val analysisContextModule = virtualFile.analysisContextModule
                withCaModuleEntry("analysisContextModule", analysisContextModule)
            }
        }

    override fun getAllClassLikeSymbolsByClassId(classId: ClassId): List<CfirClassLikeSymbol<*>> {
        val declarations = declarationProvider.getAllClassesByClassId(classId) + declarationProvider.getAllTypeAliasesByClassId(classId)

        // We're specifically taking the declarations from the declaration provider, so they're guaranteed to be in the symbol provider's
        // module.
        @OptIn(LLModuleSpecificSymbolProviderAccess::class)
        return declarations.mapNotNull { getClassLikeSymbolByPsi(classId, it) }
    }

    @ClassIdBasedLocality
    private fun ClassId.isAccepted(): Boolean = !isLocal && (allowKotlinPackage || !isKotlinPackage())

    private fun computeClassLikeSymbolByClassId(classId: ClassId, context: CjClassLikeDeclaration?): CfirClassLikeSymbol<*>? {
        require(context == null || context.isPhysical)
        val ktClass = context ?: declarationProvider.getClassLikeDeclarationByClassId(classId) ?: return null

        if (ktClass.getClassId() == null) return null
        return findClassLikeSymbol(classId, ktClass) { CfirElementFinder.findClassifierWithClassId(it, classId) }
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
        val firFile = moduleComponents.firFileBuilder.buildRawCfirFileWithCaching(declaration.containingCjFile)
        return findCfirElement(firFile)?.symbol
            ?: errorWithAttachment("Classifier was found in CjFile but was not found in CfirFile") {
                withEntry("classifierClassId", classId) { it.asString() }
                withPsiEntry("classifier", declaration, session.llCfirModuleData.ktModule)
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
        if (!allowKotlinPackage && callableId.packageName.isKotlinPackage()) return emptyList()

        val functions = getTopLevelFunctionSymbols(callableId, callableFiles)
        val properties = getTopLevelPropertySymbols(callableId, callableFiles)

        return functions.smartPlus(properties)
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
        session.firCachesFactory.createCache { callableId, context ->
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
        session.firCachesFactory.createCache { callableId, context ->
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
            files.forEach { ktFile ->
                val firFile = moduleComponents.firFileBuilder.buildRawCfirFileWithCaching(ktFile)
                firFile.collectCallableSymbolsOfTypeTo<TYPE>(this, callableId.callableName)
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
        if (!allowKotlinPackage && fqName.isKotlinPackage()) return false
        return packageProvider.doesKotlinOnlyPackageExist(fqName)
    }
}
