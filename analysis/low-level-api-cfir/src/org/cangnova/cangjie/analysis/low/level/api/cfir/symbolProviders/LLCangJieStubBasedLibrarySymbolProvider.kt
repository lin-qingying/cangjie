

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.containers.addIfNotNull
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.declarations.createDeclarationProvider
import org.cangnova.cangjie.analysis.api.platform.packages.createPackageProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.util.withPsiEntry
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.LLCfirModuleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.llCfirModuleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.statistics.LLStatisticsOnlyApi
import org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization.*
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.caches.LLPsiAwareClassLikeSymbolCache
import org.cangnova.cangjie.cfir.common.moduleData
import org.cangnova.cangjie.cfir.caches.CfirCache
import org.cangnova.cangjie.cfir.caches.CfirCacheInternals
import org.cangnova.cangjie.cfir.caches.cfirCachesFactory
import org.cangnova.cangjie.cfir.caches.getValue
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.realPsi
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumSymbol
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirStructSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.stubs.impl.CangJieNamedFunctionStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJiePropertyStubImpl
import org.cangnova.cangjie.utils.ifNotEmpty
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment

typealias DeserializedTypeAliasPostProcessor = (CfirTypeAliasSymbol) -> Unit

/**
 * [LLCangJieStubBasedLibrarySymbolProvider] deserializes CFIR symbols from existing stubs, retrieving them by [ClassId]/[CallableId] from a
 * [CangJieDeclarationProvider][org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProvider].
 *
 * The symbol provider is currently only enabled in IDE mode. The Standalone mode uses [LLJvmClassFileBasedSymbolProvider] whose base class
 * [JvmClassFileBasedSymbolProvider][org.cangnova.cangjie.cfir.java.deserialization.JvmClassFileBasedSymbolProvider] is also used by the
 * compiler.
 *
 * Because the symbol provider uses existing stubs, there is no need to keep a huge protobuf in memory, which would be the case for
 * metadata-based deserialization ([JvmClassFileBasedSymbolProvider][org.cangnova.cangjie.cfir.java.deserialization.JvmClassFileBasedSymbolProvider]).
 * At the same time, there is no need to guess sources for CFIR elements anymore, as they are set during deserialization.
 *
 * Like with [JvmClassFileBasedSymbolProvider][org.cangnova.cangjie.cfir.java.deserialization.JvmClassFileBasedSymbolProvider], the resulting
 * deserialized CFIR elements are already fully resolved.
 */
@OptIn(CaPlatformInterface::class)
internal open class LLCangJieStubBasedLibrarySymbolProvider(
    session: LLCfirSession,
    private val deserializedContainerSourceProvider: DeserializedContainerSourceProvider,
    scope: GlobalSearchScope,
) : LLCangJieSymbolProvider(session) {
    private val cangjieScopeProvider: CfirCangJieScopeProvider get() = session.cangjieScopeProvider
    private val moduleData: LLCfirModuleData get() = session.llCfirModuleData

    private val module: CaModule
        get() = moduleData.caModule

    final override val declarationProvider = session.project.createDeclarationProvider(
        scope,
        contextualModule = session.caModule,
    )

    @OptIn(CaPlatformInterface::class)
    override val symbolNamesProvider: CfirSymbolNamesProvider =
        LLCfirCangJieSymbolNamesProvider.cached(session, declarationProvider)

    private val typeAliasCache = LLPsiAwareClassLikeSymbolCache(
        createTypeAliasCache(::findAndDeserializeTypeAlias),
        createTypeAliasCache { declaration: CjClassLikeDeclaration, context ->
            val classId = declaration.getClassId() ?: return@createTypeAliasCache Pair(null, null)
            findAndDeserializeTypeAlias(classId, declaration, context)
        },
    )

    private inline fun <K : Any> createTypeAliasCache(
        crossinline deserialize: (K, StubBasedCfirDeserializationContext?) -> Pair<CfirTypeAliasSymbol?, DeserializedTypeAliasPostProcessor?>,
    ): CfirCache<K, CfirTypeAliasSymbol?, StubBasedCfirDeserializationContext?> =
        session.cfirCachesFactory.createCacheWithPostCompute(
            createValue = { key, context ->
                deserialize(key, context)
            },
            postCompute = { _, symbol, postProcessor ->
                if (postProcessor != null && symbol != null) {
                    postProcessor.invoke(symbol)
                }
            },
        )

    private val classCache = LLPsiAwareClassLikeSymbolCache(
        session,
        ::findAndDeserializeClass,
    ) { declaration: CjClassLikeDeclaration, context ->
        val classId = declaration.getClassId() ?: return@LLPsiAwareClassLikeSymbolCache null
        findAndDeserializeClass(classId, declaration, context)
    }

    private val functionCache = session.cfirCachesFactory.createCache(::loadFunctionsByCallableId)
    private val propertyCache = session.cfirCachesFactory.createCache(::loadPropertiesByCallableId)

    final override val packageProvider = session.project.createPackageProvider(scope)

    /**
     * Computes the origin for the declarations coming from [file].
     *
     * We assume that a stub Kotlin declaration might come only from Library or from BuiltIns.
     * We do the decision based upon the extension of the [file].
     *
     * This method is left open so the inheritors can provide more optimal/strict implementations.
     */
    protected open fun getDeclarationOriginFor(file: CjFile): CfirDeclarationOrigin {
        val virtualFile = file.virtualFile

        return if (virtualFile.extension == STUB_BUILTINS_FILE_EXTENSION) {
            CfirDeclarationOrigin.Library
        } else {
            CfirDeclarationOrigin.Library
        }
    }

    @OptIn(CaPlatformInterface::class)
    private fun findAndDeserializeTypeAlias(
        classId: ClassId,
        context: StubBasedCfirDeserializationContext?,
    ): Pair<CfirTypeAliasSymbol?, DeserializedTypeAliasPostProcessor?> {
        val declaration = context?.classLikeDeclaration
            ?: declarationProvider.getClassLikeDeclarationByClassId(classId)
            ?: return Pair(null, null)

        return findAndDeserializeTypeAlias(classId, declaration, context)
    }

    private fun findAndDeserializeTypeAlias(
        classId: ClassId,
        declaration: CjClassLikeDeclaration,
        context: StubBasedCfirDeserializationContext?,
    ): Pair<CfirTypeAliasSymbol?, DeserializedTypeAliasPostProcessor?> {
        if (declaration !is CjTypeAlias) return Pair(null, null)

        checkDeclarationAndContextConsistency(declaration, context)

        val symbol = CfirTypeAliasSymbol(classId)
        val postProcessor: DeserializedTypeAliasPostProcessor = {
            val rootContext = context ?: StubBasedCfirDeserializationContext.createRootContext(
                moduleData,
                StubBasedAnnotationDeserializer(session),
                classId.packageFqName,
                classId.relativeClassName,
                declaration,
                null, null, symbol,
                initialOrigin = getDeclarationOriginFor(declaration.containingCjFile)
            )
            rootContext.memberDeserializer.loadTypeAlias(declaration, symbol, cangjieScopeProvider)
        }
        return symbol to postProcessor
    }

    private fun findAndDeserializeClass(
        classId: ClassId,
        parentContext: StubBasedCfirDeserializationContext?,
    ): CfirClassLikeSymbol<*>? {
        val declaration = parentContext?.classLikeDeclaration
            ?: declarationProvider.getClassLikeDeclarationByClassId(classId)
            ?: return null

        return findAndDeserializeClass(classId, declaration, parentContext)
    }

    private fun findAndDeserializeClass(
        classId: ClassId,
        declaration: CjClassLikeDeclaration,
        parentContext: StubBasedCfirDeserializationContext?,
    ): CfirClassLikeSymbol<*>? {
        if (declaration !is CjTypeStatement) return null

        checkDeclarationAndContextConsistency(declaration, parentContext)

        val symbol = createClassLikeSymbol(classId, declaration)
        deserializeClassToSymbol(
            classId,
            declaration,
            symbol,
            session,
            moduleData,
            StubBasedAnnotationDeserializer(session),
            cangjieScopeProvider,
            parentContext = parentContext,
            containerSource = deserializedContainerSourceProvider.getClassContainerSource(classId),
            initialOrigin = parentContext?.initialOrigin ?: getDeclarationOriginFor(declaration.containingCjFile)
        )

        return symbol
    }

    private fun checkDeclarationAndContextConsistency(
        declaration: CjClassLikeDeclaration,
        context: StubBasedCfirDeserializationContext?,
    ) {
        requireWithAttachment(
            context?.classLikeDeclaration == null || declaration === context.classLikeDeclaration,
            { "The declaration to deserialize should be the same as the context's declaration." },
        ) {
            withPsiEntry("declaration", declaration, module)
            withPsiEntry("context.classLikeDeclaration", context?.classLikeDeclaration, module)
        }
    }

    private fun loadFunctionsByCallableId(
        callableId: CallableId,
        foundFunctions: Collection<CjNamedFunction>?,
    ): List<CfirNamedFunctionSymbol> {
        val topLevelFunctions = foundFunctions ?: declarationProvider.getTopLevelFunctions(callableId)

        return ArrayList<CfirNamedFunctionSymbol>(topLevelFunctions.size).apply {
            for (function in topLevelFunctions) {
                val symbol = loadFunction(
                    function = function,
                    callableId = callableId,
                    functionOrigin = getDeclarationOriginFor(function.containingCjFile),
                    deserializedContainerSourceProvider = deserializedContainerSourceProvider,
                    session = session,
                )
                add(symbol)
            }
        }
    }

    private fun loadPropertiesByCallableId(callableId: CallableId, foundProperties: Collection<CjProperty>?): List<CfirPropertySymbol> {
        val topLevelProperties = foundProperties ?: declarationProvider.getTopLevelProperties(callableId)

        return ArrayList<CfirPropertySymbol>(topLevelProperties.size).apply {
            for (property in topLevelProperties) {
                val symbol = loadProperty(
                    property = property,
                    callableId = callableId,
                    propertyOrigin = getDeclarationOriginFor(property.containingCjFile),
                    deserializedContainerSourceProvider = deserializedContainerSourceProvider,
                    session = session,
                )
                add(symbol)
            }
        }
    }

    @CfirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<CfirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        val callableId = CallableId(packageFqName, name)
        destination += functionCache.getCallablesWithoutContext(callableId)
        destination += propertyCache.getCallablesWithoutContext(callableId)
    }

    private fun <C : CfirCallableSymbol<*>, CONTEXT> CfirCache<CallableId, List<C>, CONTEXT?>.getCallablesWithoutContext(
        id: CallableId,
    ): List<C> {
        if (!symbolNamesProvider.mayHaveTopLevelCallable(id.packageName, id.callableName)) return emptyList()
        return getValue(id, null)
    }

    @CfirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(
        destination: MutableList<CfirCallableSymbol<*>>,
        callableId: CallableId,
        callables: Collection<CjCallableDeclaration>,
    ) {
        callables.filterIsInstance<CjNamedFunction>().ifNotEmpty {
            destination += functionCache.getValue(callableId, this)
        }

        callables.filterIsInstance<CjProperty>().ifNotEmpty {
            destination += propertyCache.getValue(callableId, this)
        }
    }

    @CfirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<CfirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        destination += functionCache.getCallablesWithoutContext(CallableId(packageFqName, name))
    }

    @CfirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<CfirNamedFunctionSymbol>,
        callableId: CallableId,
        functions: Collection<CjNamedFunction>,
    ) {
        destination += functionCache.getValue(callableId, functions)
    }

    @CfirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<CfirPropertySymbol>, packageFqName: FqName, name: Name) {
        destination += propertyCache.getCallablesWithoutContext(CallableId(packageFqName, name))
    }

    @CfirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(
        destination: MutableList<CfirPropertySymbol>,
        callableId: CallableId,
        properties: Collection<CjProperty>,
    ) {
        destination += propertyCache.getValue(callableId, properties)
    }

    override fun hasPackage(fqName: FqName): Boolean =
        packageProvider.doesPackageExist(fqName)

    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? {
        getCachedClassLikeSymbol(classId)?.let { return it }

        if (!symbolNamesProvider.mayHaveTopLevelClassifier(classId)) return null

        return getClass(classId) ?: getTypeAlias(classId)
    }

    private fun getCachedClassLikeSymbol(classId: ClassId): CfirClassLikeSymbol<*>? {
        return classCache.getCachedSymbolByClassId(classId)
            ?: typeAliasCache.getCachedSymbolByClassId(classId)
    }

    private fun getClass(classId: ClassId): CfirClassLikeSymbol<*>? {
        @OptIn(LLModuleSpecificSymbolProviderAccess::class)
        return classCache.getSymbolByClassId(classId, context = null)
    }

    private fun getTypeAlias(classId: ClassId): CfirTypeAliasSymbol? {
        @OptIn(LLModuleSpecificSymbolProviderAccess::class)
        return typeAliasCache.getSymbolByClassId(classId, context = null)
    }

    @LLModuleSpecificSymbolProviderAccess
    override fun getClassLikeSymbolByClassId(classId: ClassId, classLikeDeclaration: CjClassLikeDeclaration): CfirClassLikeSymbol<*>? {
        val cache = if (classLikeDeclaration is CjTypeStatement) classCache else typeAliasCache
        cache.getCachedSymbolByClassId(classId)?.let { return it }

        return cache.getSymbolByClassId(classId, createClassLikeDeserializationContext(classId, classLikeDeclaration))
    }

    @LLModuleSpecificSymbolProviderAccess
    override fun getClassLikeSymbolByPsi(classId: ClassId, declaration: PsiElement): CfirClassLikeSymbol<*>? {
        if (declaration !is CjClassLikeDeclaration) return null

        val cache = if (declaration is CjTypeStatement) classCache else typeAliasCache
        cache.getCachedSymbolByPsi(classId, declaration)?.let { return it }

        return cache.getSymbolByPsi(classId, declaration, createClassLikeDeserializationContext(classId, declaration))
    }

    private fun createClassLikeDeserializationContext(
        classId: ClassId,
        classLikeDeclaration: CjClassLikeDeclaration,
    ): StubBasedCfirDeserializationContext {
        val annotationDeserializer = StubBasedAnnotationDeserializer(session)
        val classOrigin = getDeclarationOriginFor(classLikeDeclaration.containingCjFile)
        return StubBasedCfirDeserializationContext(
            moduleData,
            classId.packageFqName,
            classId.relativeClassName,
            StubBasedCfirTypeDeserializer(
                moduleData,
                annotationDeserializer,
                parent = null,
                containingSymbol = null,
                owner = null,
                classOrigin
            ),
            annotationDeserializer,
            containerSource = null,
            outerClassSymbol = null,
            outerTypeParameters = emptyList<CfirTypeParameterSymbol>(),
            classOrigin,
            classLikeDeclaration,
        )
    }

    fun getTopLevelCallableSymbol(
        packageFqName: FqName,
        shortName: Name,
        callableDeclaration: CjCallableDeclaration,
    ): CfirCallableSymbol<*>? {
        //possible overloads spoils here
        //we can't use only this callable instead of index access to fill the cache
        //names check is redundant though as we already have existing callable in scope
        val callableId = CallableId(packageFqName, shortName)
        val callableSymbols = when (callableDeclaration) {
            is CjNamedFunction -> functionCache.getValue(callableId)
            is CjProperty -> propertyCache.getValue(callableId)
            else -> null
        }

        return callableSymbols?.singleOrNull { it.cfir.realPsi == callableDeclaration }
    }

    @OptIn(CfirCacheInternals::class)
    @LLStatisticsOnlyApi
    internal val cachedDeclarations: List<CfirDeclaration>
        get() = buildList {
            typeAliasCache.cachedValues.forEach { addIfNotNull(it?.cfir) }
            classCache.cachedValues.forEach { addIfNotNull(it?.cfir) }
            functionCache.cachedValues.forEach { functions ->
                functions.forEach { add(it.cfir) }
            }
            propertyCache.cachedValues.forEach { properties ->
                properties.forEach { add(it.cfir) }
            }
        }

    companion object {
        fun loadProperty(
            property: CjProperty,
            callableId: CallableId,
            propertyOrigin: CfirDeclarationOrigin,
            deserializedContainerSourceProvider: DeserializedContainerSourceProvider,
            session: CfirSession,
        ): CfirPropertySymbol {
            val propertyStub: CangJiePropertyStubImpl = property.compiledStub
            val containerSource = deserializedContainerSourceProvider.getFacadeContainerSource(
                file = property.containingCjFile,
                stubOrigin = propertyStub.origin,
                declarationOrigin = propertyOrigin,
            )

            val symbol = CfirPropertySymbol(callableId)
            val rootContext = StubBasedCfirDeserializationContext.createRootContext(
                session = session,
                moduleData = session.moduleData,
                callableId = callableId,
                parameterListOwner = property,
                symbol = symbol,
                initialOrigin = propertyOrigin,
                containerSource = containerSource,
            )

            return rootContext.memberDeserializer.loadProperty(
                property = property,
                classSymbol = null,
                existingSymbol = symbol,
            ).symbol
        }

        fun loadFunction(
            function: CjNamedFunction,
            callableId: CallableId,
            functionOrigin: CfirDeclarationOrigin,
            deserializedContainerSourceProvider: DeserializedContainerSourceProvider,
            session: CfirSession,
        ): CfirNamedFunctionSymbol {
            val functionStub: CangJieNamedFunctionStubImpl = function.compiledStub
            val containerSource = deserializedContainerSourceProvider.getFacadeContainerSource(
                file = function.containingCjFile,
                stubOrigin = functionStub.origin,
                declarationOrigin = functionOrigin,
            )

            val symbol = CfirNamedFunctionSymbol(callableId)
            val rootContext = StubBasedCfirDeserializationContext.createRootContext(
                session = session,
                moduleData = session.moduleData,
                callableId = callableId,
                parameterListOwner = function,
                symbol = symbol,
                initialOrigin = functionOrigin,
                containerSource = containerSource,
            )

            return rootContext.memberDeserializer.loadFunction(
                function = function,
                classSymbol = null,
                session = session,
                existingSymbol = symbol,
            ).symbol
        }
    }

    private fun createClassLikeSymbol(classId: ClassId, declaration: CjTypeStatement): CfirClassLikeSymbol<*> {
        return when {
            declaration.isInterface() -> CfirInterfaceSymbol(classId)
            declaration.isStruct() -> CfirStructSymbol(classId)
            declaration.isEnum() -> CfirEnumSymbol(classId)
            else -> CfirClassSymbol(classId)
        }
    }
}
