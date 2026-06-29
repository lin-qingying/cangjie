

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

/**
 * typealias 反序列化后处理函数。
 *
 * 缓存创建阶段需要先返回 [CfirTypeAliasSymbol] 以打破递归，随后再通过该函数把 typealias CFIR 内容加载到符号上。
 */
typealias DeserializedTypeAliasPostProcessor = (CfirTypeAliasSymbol) -> Unit

/**
 * 基于已有 stub 反序列化库 CFIR 符号的 [LLCangJieSymbolProvider] 实现。
 *
 * 该提供器通过平台声明索引按 [ClassId] 或 [CallableId] 找到库 stub PSI，再把 stub 反序列化为已解析完成的 CFIR 符号。
 * 它当前只用于 IDE 模式；独立模式会使用基于 class 文件的符号提供器。
 *
 * 由于直接复用已有 stub，这里不需要像 metadata 反序列化那样在内存中保留大型 protobuf；同时 CFIR 元素来源会在反序列化期间设置，
 * 不再需要额外推断 source。
 *
 * 与编译器侧 class 文件符号提供器一致，该提供器产出的反序列化 CFIR 元素已经处于完成解析状态。
 */
@OptIn(CaPlatformInterface::class)
internal open class LLCangJieStubBasedLibrarySymbolProvider(
    session: LLCfirSession,
    /**
     * 根据声明类型和 stub 来源创建反序列化 container source 的提供器。
     */
    private val deserializedContainerSourceProvider: DeserializedContainerSourceProvider,
    scope: GlobalSearchScope,
) : LLCangJieSymbolProvider(session) {
    /**
     * 当前会话的仓颉作用域提供器，用于类和成员反序列化。
     */
    private val cangjieScopeProvider: CfirCangJieScopeProvider get() = session.cangjieScopeProvider
    /**
     * 当前低阶 CFIR 模块数据。
     */
    private val moduleData: LLCfirModuleData get() = session.llCfirModuleData

    /**
     * 当前低阶会话对应的 Analysis API 模块。
     */
    private val module: CaModule
        get() = moduleData.caModule

    /**
     * 当前库搜索范围内的仓颉声明索引。
     */
    final override val declarationProvider = session.project.createDeclarationProvider(
        scope,
        contextualModule = session.caModule,
    )

    /**
     * 基于 [declarationProvider] 的缓存名称索引。
     */
    @OptIn(CaPlatformInterface::class)
    override val symbolNamesProvider: CfirSymbolNamesProvider =
        LLCfirCangJieSymbolNamesProvider.cached(session, declarationProvider)

    /**
     * typealias 符号缓存，支持按 class ID 和按 PSI 精确反序列化。
     */
    private val typeAliasCache = LLPsiAwareClassLikeSymbolCache(
        createTypeAliasCache(::findAndDeserializeTypeAlias),
        createTypeAliasCache { declaration: CjClassLikeDeclaration, context ->
            val classId = declaration.getClassId() ?: return@createTypeAliasCache Pair(null, null)
            findAndDeserializeTypeAlias(classId, declaration, context)
        },
    )

    /**
     * 创建带反序列化后处理阶段的 typealias 缓存。
     */
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

    /**
     * 类、接口、结构体和枚举的 class-like 符号缓存。
     */
    private val classCache = LLPsiAwareClassLikeSymbolCache(
        session,
        ::findAndDeserializeClass,
    ) { declaration: CjClassLikeDeclaration, context ->
        val classId = declaration.getClassId() ?: return@LLPsiAwareClassLikeSymbolCache null
        findAndDeserializeClass(classId, declaration, context)
    }

    /**
     * 顶层函数符号缓存。
     */
    private val functionCache = session.cfirCachesFactory.createCache(::loadFunctionsByCallableId)
    /**
     * 顶层属性符号缓存。
     */
    private val propertyCache = session.cfirCachesFactory.createCache(::loadPropertiesByCallableId)

    /**
     * 当前库搜索范围内的包索引。
     */
    final override val packageProvider = session.project.createPackageProvider(scope)

    /**
     * 计算 [file] 中反序列化声明的 CFIR 来源。
     *
     * 当前实现把 stub 声明视为库声明。方法保持 `open`，允许子类在已知来源更严格的场景中提供更精确的判定。
     *
     * @return [file] 中声明应使用的 [CfirDeclarationOrigin]。
     */
    protected open fun getDeclarationOriginFor(file: CjFile): CfirDeclarationOrigin {
        val virtualFile = file.virtualFile

        return if (virtualFile.extension == STUB_BUILTINS_FILE_EXTENSION) {
            CfirDeclarationOrigin.Library
        } else {
            CfirDeclarationOrigin.Library
        }
    }

    /**
     * 按 [classId] 查找 typealias stub 并创建可后处理的反序列化结果。
     */
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

    /**
     * 根据已知 [declaration] 创建 typealias 符号和延迟反序列化后处理函数。
     */
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

    /**
     * 按 [classId] 查找 class-like stub 并反序列化为类符号。
     */
    private fun findAndDeserializeClass(
        classId: ClassId,
        parentContext: StubBasedCfirDeserializationContext?,
    ): CfirClassLikeSymbol<*>? {
        val declaration = parentContext?.classLikeDeclaration
            ?: declarationProvider.getClassLikeDeclarationByClassId(classId)
            ?: return null

        return findAndDeserializeClass(classId, declaration, parentContext)
    }

    /**
     * 根据已知 [declaration] 反序列化类、接口、结构体或枚举符号。
     */
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

    /**
     * 校验反序列化 [declaration] 与可选 [context] 中记录的 class-like 声明一致。
     */
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

    /**
     * 按 [callableId] 加载顶层函数符号列表。
     */
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

    /**
     * 按 [callableId] 加载顶层属性符号列表。
     */
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

    /**
     * 将指定包和名称下的顶层函数与属性符号追加到 [destination]。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<CfirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        val callableId = CallableId(packageFqName, name)
        destination += functionCache.getCallablesWithoutContext(callableId)
        destination += propertyCache.getCallablesWithoutContext(callableId)
    }

    /**
     * 在名称索引确认可能存在 callable 后，从当前缓存中读取无 PSI 上下文的 callable 符号。
     */
    private fun <C : CfirCallableSymbol<*>, CONTEXT> CfirCache<CallableId, List<C>, CONTEXT?>.getCallablesWithoutContext(
        id: CallableId,
    ): List<C> {
        if (!symbolNamesProvider.mayHaveTopLevelCallable(id.packageName, id.callableName)) return emptyList()
        return getValue(id, null)
    }

    /**
     * 根据已知 [callables] 精确加载顶层 callable 符号并追加到 [destination]。
     */
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

    /**
     * 将指定包和名称下的顶层函数符号追加到 [destination]。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<CfirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        destination += functionCache.getCallablesWithoutContext(CallableId(packageFqName, name))
    }

    /**
     * 根据已知 [functions] 精确加载顶层函数符号并追加到 [destination]。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<CfirNamedFunctionSymbol>,
        callableId: CallableId,
        functions: Collection<CjNamedFunction>,
    ) {
        destination += functionCache.getValue(callableId, functions)
    }

    /**
     * 将指定包和名称下的顶层属性符号追加到 [destination]。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<CfirPropertySymbol>, packageFqName: FqName, name: Name) {
        destination += propertyCache.getCallablesWithoutContext(CallableId(packageFqName, name))
    }

    /**
     * 根据已知 [properties] 精确加载顶层属性符号并追加到 [destination]。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(
        destination: MutableList<CfirPropertySymbol>,
        callableId: CallableId,
        properties: Collection<CjProperty>,
    ) {
        destination += propertyCache.getValue(callableId, properties)
    }

    /**
     * 判断当前库包索引中是否存在 [fqName] 包。
     */
    override fun hasPackage(fqName: FqName): Boolean =
        packageProvider.doesPackageExist(fqName)

    /**
     * 按 [classId] 查询库 class-like 符号，优先读取缓存，再按名称索引触发类或 typealias 反序列化。
     */
    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? {
        getCachedClassLikeSymbol(classId)?.let { return it }

        if (!symbolNamesProvider.mayHaveTopLevelClassifier(classId)) return null

        return getClass(classId) ?: getTypeAlias(classId)
    }

    /**
     * 从类缓存和 typealias 缓存中读取 [classId] 对应的已缓存 class-like 符号。
     */
    private fun getCachedClassLikeSymbol(classId: ClassId): CfirClassLikeSymbol<*>? {
        return classCache.getCachedSymbolByClassId(classId)
            ?: typeAliasCache.getCachedSymbolByClassId(classId)
    }

    /**
     * 按 [classId] 触发普通 class-like 符号反序列化。
     */
    private fun getClass(classId: ClassId): CfirClassLikeSymbol<*>? {
        @OptIn(LLModuleSpecificSymbolProviderAccess::class)
        return classCache.getSymbolByClassId(classId, context = null)
    }

    /**
     * 按 [classId] 触发 typealias 符号反序列化。
     */
    private fun getTypeAlias(classId: ClassId): CfirTypeAliasSymbol? {
        @OptIn(LLModuleSpecificSymbolProviderAccess::class)
        return typeAliasCache.getSymbolByClassId(classId, context = null)
    }

    /**
     * 根据已知 [classLikeDeclaration] 精确查询或反序列化 [classId] 对应的 class-like 符号。
     */
    @LLModuleSpecificSymbolProviderAccess
    override fun getClassLikeSymbolByClassId(classId: ClassId, classLikeDeclaration: CjClassLikeDeclaration): CfirClassLikeSymbol<*>? {
        val cache = if (classLikeDeclaration is CjTypeStatement) classCache else typeAliasCache
        cache.getCachedSymbolByClassId(classId)?.let { return it }

        return cache.getSymbolByClassId(classId, createClassLikeDeserializationContext(classId, classLikeDeclaration))
    }

    /**
     * 根据 [declaration] PSI 精确查询或反序列化 [classId] 对应的 class-like 符号。
     */
    @LLModuleSpecificSymbolProviderAccess
    override fun getClassLikeSymbolByPsi(classId: ClassId, declaration: PsiElement): CfirClassLikeSymbol<*>? {
        if (declaration !is CjClassLikeDeclaration) return null

        val cache = if (declaration is CjTypeStatement) classCache else typeAliasCache
        cache.getCachedSymbolByPsi(classId, declaration)?.let { return it }

        return cache.getSymbolByPsi(classId, declaration, createClassLikeDeserializationContext(classId, declaration))
    }

    /**
     * 为已知 class-like PSI 声明创建 stub 反序列化上下文。
     */
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

    /**
     * 按已知 [callableDeclaration] 查找单个顶层 callable 符号。
     *
     * 该入口用于调用方已经拥有 PSI 声明、但仍需要从当前缓存中找到真实反序列化符号的场景。缓存填充仍基于名字索引，
     * 最后再通过 PSI 精确匹配目标声明。
     */
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

    /**
     * 当前提供器已经缓存的 CFIR 声明列表。
     *
     * 该属性仅供低阶分析统计使用，不参与符号解析语义。
     */
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

    /**
     * stub 库符号反序列化的静态辅助入口集合。
     */
    companion object {
        /**
         * 从 [property] 的已编译 stub 加载顶层属性符号。
         */
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

        /**
         * 从 [function] 的已编译 stub 加载顶层函数符号。
         */
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

    /**
     * 根据 PSI 声明形态创建对应类型的 class-like 符号。
     */
    private fun createClassLikeSymbol(classId: ClassId, declaration: CjTypeStatement): CfirClassLikeSymbol<*> {
        return when {
            declaration.isInterface() -> CfirInterfaceSymbol(classId)
            declaration.isStruct() -> CfirStructSymbol(classId)
            declaration.isEnum() -> CfirEnumSymbol(classId)
            else -> CfirClassSymbol(classId)
        }
    }
}
