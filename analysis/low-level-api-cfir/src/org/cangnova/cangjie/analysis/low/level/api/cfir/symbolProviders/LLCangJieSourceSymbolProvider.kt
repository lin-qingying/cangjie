

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
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompositeCachedSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals
import org.cangnova.cangjie.cfir.psi
import org.cangnova.cangjie.cfir.symbols.*
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.utils.exceptions.ExceptionAttachmentBuilder
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment

/**
 * 面向源码模块的 [LLCangJieSymbolProvider] 实现。
 *
 * 该提供器从模块内容范围内的仓颉 PSI 构建 raw CFIR，并把 class-like、顶层函数、顶层属性和顶层扩展文件暴露为 CFIR 符号。
 * 典型使用方是 [CaSourceModule][org.cangnova.cangjie.analysis.api.projectStructure.CaSourceModule] 对应的低阶分析会话。
 */
@OptIn(CaPlatformInterface::class)
internal class LLCangJieSourceSymbolProvider(
    session: LLCfirSession,
    /**
     * 当前模块的解析组件集合，提供内容范围、raw CFIR 文件构建器和模块数据。
     */
    private val moduleComponents: LLCfirModuleResolveComponents,
    declarationProviderFactory: (GlobalSearchScope) -> CangJieDeclarationProvider?,
) : LLCangJieSymbolProvider(session), LLMultiClassLikeSymbolProvider {
    /**
     * 当前源码模块的内容搜索范围。
     */
    private val searchScope: GlobalSearchScope
        get() = moduleComponents.module.contentScope

    /**
     * 聚合当前内容范围内可用的仓颉声明索引。
     */
    override val declarationProvider = CangJieCompositeDeclarationProvider.create(
        listOfNotNull(
            declarationProviderFactory(searchScope),
        )
    )

    /**
     * 聚合当前内容范围内可用的仓颉包索引。
     */
    override val packageProvider = CangJieCompositePackageProvider.create(
        listOfNotNull(
            session.project.createPackageProvider(searchScope),
        )
    )

    /**
     * 绑定当前源码声明索引的缓存名称提供器。
     */
    private val physicalSymbolNamesProvider: CfirSymbolNamesProvider = CfirCompositeCachedSymbolNamesProvider.create(
        session,
        listOfNotNull(
            LLCfirCangJieSymbolNamesProvider(declarationProvider),
        )
    )

    /** 物理 PSI 名称与已发布快照名称的实时并集，避免发布前缓存的名称负结果过滤生成声明。 */
    override val symbolNamesProvider: CfirSymbolNamesProvider = LLCfirMacroAwareSymbolNamesProvider(
        delegate = physicalSymbolNamesProvider,
        snapshotProvider = { moduleComponents.macroSemanticSnapshotStore.snapshot },
    )

    /** 只暴露最终宏快照中能由 registry 来源映射确认的生成声明名称。 */
    override val macroGeneratedSourceDeclarationsNamesProvider: CfirSymbolNamesProvider =
        LLCfirMacroGeneratedSourceNamesProvider { moduleComponents.macroSemanticSnapshotStore.snapshot }

    /**
     * class-like 符号缓存，支持按 [ClassId] 与按 PSI 两种命中路径。
     */
    private val classLikeCache =
        LLPsiAwareClassLikeSymbolCache(session, ::computeClassLikeSymbolByClassId) { declaration: CjClassLikeDeclaration, _ ->
            computeClassLikeSymbolByPsi(declaration)
        }

    /**
     * 根据 [classId] 查询当前源码模块中的 class-like 符号。
     */
    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? {
        moduleComponents.macroSemanticSnapshotStore.snapshot?.let { snapshot ->
            return snapshot.classifiers(classId).firstOrNull()?.symbol
        }
        if (!symbolNamesProvider.mayHaveTopLevelClassifier(classId)) return null
        return getClassLikeSymbolByClassIdAndDeclaration(classId, classLikeDeclaration = null)
    }

    /**
     * 根据已知 [classLikeDeclaration] 查询 [classId] 对应的 class-like 符号。
     */
    @LLModuleSpecificSymbolProviderAccess
    override fun getClassLikeSymbolByClassId(classId: ClassId, classLikeDeclaration: CjClassLikeDeclaration): CfirClassLikeSymbol<*>? {
        moduleComponents.macroSemanticSnapshotStore.snapshot?.let { snapshot ->
            val sourceClassifiers = snapshot.classifiersForSource(classLikeDeclaration)
                .filter { it.symbol.classId == classId }
            return sourceClassifiers.singleOrNull()?.symbol
        }
        return getClassLikeSymbolByClassIdAndDeclaration(classId, classLikeDeclaration)
    }

    /**
     * 统一按 [classId] 与可选 PSI 上下文查询 class-like 缓存。
     */
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

    /**
     * 根据 [declaration] PSI 精确查询 [classId] 对应的 class-like 符号。
     */
    @LLModuleSpecificSymbolProviderAccess
    override fun getClassLikeSymbolByPsi(classId: ClassId, declaration: PsiElement): CfirClassLikeSymbol<*>? {
        moduleComponents.macroSemanticSnapshotStore.snapshot?.let { snapshot ->
            val sourceDeclaration = declaration as? CjClassLikeDeclaration ?: return null
            val sourceClassifiers = snapshot.classifiersForSource(sourceDeclaration)
                .filter { it.symbol.classId == classId }
            return sourceClassifiers.singleOrNull()?.symbol
        }
        return classLikeCache.getSymbolByPsi<CjClassLikeDeclaration>(
            classId,
            declaration,
            buildAdditionalAttachments = buildAdditionalAttachmentsForClassLikeSymbol,
        ) { it }
    }

    /**
     * 为 class-like 缓存异常补充声明索引与内容范围诊断信息。
     *
     * 这里会记录指定 class ID 当前是否仍能从 [declarationProvider] 找到声明，以及给定上下文 PSI 是否位于当前符号提供器的
     * [searchScope] 内，用于定位缓存失效或模块归属异常。
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

    /**
     * 查询 [classId] 对应的全部 class-like 符号。
     *
     * 多声明场景下直接使用 [declarationProvider] 提供的声明集合，确保结果只来自当前模块内容范围。
     */
    override fun getAllClassLikeSymbolsByClassId(classId: ClassId): List<CfirClassLikeSymbol<*>> {
        moduleComponents.macroSemanticSnapshotStore.snapshot?.let { snapshot ->
            return snapshot.classifiers(classId).map { it.symbol }
        }
        val declarations = declarationProvider.getAllClassesByClassId(classId) + declarationProvider.getAllTypeAliasesByClassId(classId)

        // We're specifically taking the declarations from the declaration provider, so they're guaranteed to be in the symbol provider's
        // module.
        @OptIn(LLModuleSpecificSymbolProviderAccess::class)
        return declarations.mapNotNull { getClassLikeSymbolByPsi(classId, it) }
    }

    /** 将 PSI 精确多声明查询接入编译器公共候选 API，组织过滤前不选择首项。 */
    override fun getClassLikeSymbolsByClassId(classId: ClassId): List<CfirClassLikeSymbol<*>> =
        getAllClassLikeSymbolsByClassId(classId)

    /**
     * 根据 [classId] 与可选 PSI 上下文计算 class-like 符号。
     */
    private fun computeClassLikeSymbolByClassId(classId: ClassId, context: CjClassLikeDeclaration?): CfirClassLikeSymbol<*>? {
        require(context == null || context.isPhysical)
        val classLikeDeclaration = context ?: declarationProvider.getClassLikeDeclarationByClassId(classId) ?: return null

        if (classLikeDeclaration.getClassId() == null) return null
        return findClassLikeSymbol(classId, classLikeDeclaration) { file ->
            // 这里已经拿到了精确 PSI，优先按同一份声明做映射，
            // 避免仅靠 ClassId 路径搜索时被当前 CFIR 结构差异误伤。
            (CfirElementFinder.findDeclaration(file, classLikeDeclaration) as? CfirClassLikeDeclaration)
                ?: CfirElementFinder.findClassifierWithClassId(file, classId)
        }
    }

    /**
     * 根据物理 [declaration] PSI 计算 class-like 符号。
     */
    private fun computeClassLikeSymbolByPsi(declaration: CjClassLikeDeclaration): CfirClassLikeSymbol<*>? {
        require(declaration.isPhysical)

        val classId = declaration.getClassId() ?: return null
        return findClassLikeSymbol(classId, declaration) { file ->
            CfirElementFinder.findDeclaration(file, declaration) as? CfirClassLikeDeclaration
        }
    }

    /**
     * 构建声明所在文件的 raw CFIR，并在其中定位对应的 class-like CFIR 声明。
     */
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

    /**
     * 查询指定包和名称下的全部顶层 callable 符号。
     */
    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<CfirCallableSymbol<*>> {
        if (!symbolNamesProvider.mayHaveTopLevelCallable(packageFqName, name)) return emptyList()
        return getTopLevelCallableSymbols(CallableId(packageFqName, name), callableFiles = null)
    }

    /**
     * 将指定包和名称下的全部顶层 callable 符号追加到 [destination]。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<CfirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        if (!symbolNamesProvider.mayHaveTopLevelCallable(packageFqName, name)) return
        destination += getTopLevelCallableSymbols(CallableId(packageFqName, name), callableFiles = null)
    }

    /**
     * 根据已知 [callables] 所在文件查询顶层 callable 符号并追加到 [destination]。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(
        destination: MutableList<CfirCallableSymbol<*>>,
        callableId: CallableId,
        callables: Collection<CjCallableDeclaration>,
    ) {
        destination += getTopLevelCallableSymbols(callableId, callables.mapTo(mutableSetOf()) { it.containingCjFile })
    }

    /**
     * 通过 [callableCache] 获取 [callableId] 对应的顶层 callable 符号。
     */
    private fun getTopLevelCallableSymbols(callableId: CallableId, callableFiles: Collection<CjFile>?): List<CfirCallableSymbol<*>> {
        moduleComponents.macroSemanticSnapshotStore.snapshot?.let { snapshot ->
            return snapshot.callables(callableId).map { it.symbol }
        }
        return callableCache.getValue(callableId, callableFiles)
    }

    /**
     * 顶层 callable 缓存，缓存键为 [CallableId]，上下文为可选的已知声明文件集合。
     */
    private val callableCache: CfirCache<CallableId, List<CfirCallableSymbol<*>>, Collection<CjFile>?> =
        session.cfirCachesFactory.createCache { callableId, context ->
            computeCallableSymbolsByCallableId<CfirCallableSymbol<*>>(callableId, context)
        }

    /**
     * 查询指定包和名称下的全部顶层函数符号。
     */
    override fun getTopLevelFunctionSymbols(packageFqName: FqName, name: Name): List<CfirNamedFunctionSymbol> {
        if (!symbolNamesProvider.mayHaveTopLevelCallable(packageFqName, name)) return emptyList()
        return getTopLevelFunctionSymbols(CallableId(packageFqName, name), callableFiles = null)
    }

    /**
     * 将指定包和名称下的全部顶层函数符号追加到 [destination]。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<CfirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        if (!symbolNamesProvider.mayHaveTopLevelCallable(packageFqName, name)) return
        destination += getTopLevelFunctionSymbols(CallableId(packageFqName, name), callableFiles = null)
    }

    /**
     * 根据已知 [functions] 所在文件查询顶层函数符号并追加到 [destination]。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<CfirNamedFunctionSymbol>,
        callableId: CallableId,
        functions: Collection<CjNamedFunction>,
    ) {
        destination += getTopLevelFunctionSymbols(callableId, functions.mapTo(mutableSetOf()) { it.containingCjFile })
    }

    /**
     * 顶层函数缓存，缓存键为 [CallableId]，上下文为可选的已知声明文件集合。
     */
    private val functionCache: CfirCache<CallableId, List<CfirNamedFunctionSymbol>, Collection<CjFile>?> =
        session.cfirCachesFactory.createCache { callableId, context ->
            computeCallableSymbolsByCallableId<CfirNamedFunctionSymbol>(callableId, context)
        }

    /**
     * 通过 [functionCache] 获取 [callableId] 对应的顶层函数符号。
     */
    private fun getTopLevelFunctionSymbols(callableId: CallableId, callableFiles: Collection<CjFile>?): List<CfirNamedFunctionSymbol> {
        moduleComponents.macroSemanticSnapshotStore.snapshot?.let { snapshot ->
            return snapshot.callables(callableId).mapNotNull { it.symbol as? CfirNamedFunctionSymbol }
        }
        return functionCache.getValue(callableId, callableFiles)
    }

    /**
     * 查询指定包和名称下的全部顶层属性符号。
     */
    override fun getTopLevelPropertySymbols(packageFqName: FqName, name: Name): List<CfirPropertySymbol> {
        if (!symbolNamesProvider.mayHaveTopLevelCallable(packageFqName, name)) return emptyList()
        return getTopLevelPropertySymbols(CallableId(packageFqName, name), callableFiles = null)
    }

    /**
     * 将指定包和名称下的全部顶层属性符号追加到 [destination]。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<CfirPropertySymbol>, packageFqName: FqName, name: Name) {
        if (!symbolNamesProvider.mayHaveTopLevelCallable(packageFqName, name)) return
        destination += getTopLevelPropertySymbols(CallableId(packageFqName, name), callableFiles = null)
    }

    /**
     * 根据已知 [properties] 所在文件查询顶层属性符号并追加到 [destination]。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(
        destination: MutableList<CfirPropertySymbol>,
        callableId: CallableId,
        properties: Collection<CjProperty>,
    ) {
        destination += getTopLevelPropertySymbols(callableId, properties.mapTo(mutableSetOf()) { it.containingCjFile })
    }

    /**
     * 顶层属性缓存，缓存键为 [CallableId]，上下文为可选的已知声明文件集合。
     */
    private val propertyCache: CfirCache<CallableId, List<CfirPropertySymbol>, Collection<CjFile>?> =
        session.cfirCachesFactory.createCache { callableId, context ->
            computeCallableSymbolsByCallableId<CfirPropertySymbol>(callableId, context)
        }

    /**
     * 通过 [propertyCache] 获取 [callableId] 对应的顶层属性符号。
     */
    private fun getTopLevelPropertySymbols(callableId: CallableId, callableFiles: Collection<CjFile>?): List<CfirPropertySymbol> {
        moduleComponents.macroSemanticSnapshotStore.snapshot?.let { snapshot ->
            return snapshot.callables(callableId).mapNotNull { it.symbol as? CfirPropertySymbol }
        }
        return propertyCache.getValue(callableId, callableFiles)
    }

    /**
     * 在指定文件集合内定位匹配 [callableId] 的 [TYPE] 类型 callable 符号。
     *
     * 当 [context] 不为空时直接使用已知文件避免访问索引；否则通过 [declarationProvider] 获取包含该 callable 的顶层文件。
     *
     * 为了与 [CfirCache] 正确协作，该函数必须满足以下契约：
     *
     * 只有当非空 [context] 的返回值与 `null` 上下文的返回值一致时，才允许使用同一个 [callableId] 携带非空 [context] 调用。
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

        val result = buildList {
            files.forEach { cjFile ->
                val cfirFile = moduleComponents.cfirFileBuilder.buildRawCfirFileWithCaching(cjFile)
                cfirFile.collectCallableSymbolsOfTypeTo<TYPE>(this, callableId.callableName)
            }
        }
        return result
    }

    /**
     * 从当前 [CfirFile] 的顶层声明中收集指定 [name] 与 [TYPE] 类型匹配的 callable 符号。
     */
    private inline fun <reified TYPE : CfirCallableSymbol<*>> CfirFile.collectCallableSymbolsOfTypeTo(result: MutableList<TYPE>, name: Name) {
        declarations.mapNotNullTo(result) { declaration ->
            if (declaration is CfirCallableDeclaration && declaration.symbol.name == name) {
                declaration.symbol as? TYPE
            } else null
        }
    }

    /**
     * 判断当前源码模块包索引中是否存在 [fqName] 包。
     */
    override fun hasPackage(fqName: FqName): Boolean {
        return packageProvider.doesPackageExist(fqName)
    }

    /**
     * 物化当前源码模块中的顶层扩展文件为 raw CFIR 文件。
     */
    internal override fun materializeTopLevelExtendFiles(): List<CfirFile> {
        moduleComponents.macroSemanticSnapshotStore.snapshot?.let { snapshot ->
            return snapshot.files.flatMap { file -> file.declarations.filterIsInstance<CfirExtend>() }
                .mapNotNull { declaration -> moduleComponents.cache.getContainerCfirFile(declaration) }
                .distinct()
        }
        return declarationProvider.getTopLevelExtendFiles()
            .distinctBy { file -> file.virtualFile ?: file }
            .map(moduleComponents.cfirFileBuilder::buildRawCfirFileWithCaching)
    }
}

/**
 * 在物理声明名称索引上叠加完整 expanded CFIR 中的顶层名称。
 *
 * 该 provider 不自行缓存快照，名称查询总是观察当前原子发布结果；物理索引无法枚举时继续返回 null，
 * 由基类保守策略允许符号查询继续进行。
 */
private class LLCfirMacroAwareSymbolNamesProvider(
    private val delegate: CfirSymbolNamesProvider,
    private val snapshotProvider: () -> org.cangnova.cangjie.analysis.low.level.api.cfir.macro.LLCfirMacroSemanticSnapshot?,
) : CfirSymbolNamesProvider() {
    override val hasSpecificClassifierPackageNamesComputation: Boolean
        get() = delegate.hasSpecificClassifierPackageNamesComputation

    override val hasSpecificCallablePackageNamesComputation: Boolean
        get() = delegate.hasSpecificCallablePackageNamesComputation

    override val mayHaveSyntheticFunctionTypes: Boolean
        get() = delegate.mayHaveSyntheticFunctionTypes

    override fun getPackageNames(): Set<String>? =
        merge(delegate.getPackageNames(), snapshotProvider()?.filesByPackage?.keys?.mapTo(linkedSetOf()) { it.asString() }.orEmpty())

    override fun getPackageNamesWithTopLevelClassifiers(): Set<String>? =
        merge(
            delegate.getPackageNamesWithTopLevelClassifiers(),
            snapshotProvider()?.filesByPackage?.keys
                ?.filterTo(linkedSetOf()) { snapshotProvider()?.classifierNames(it)?.isNotEmpty() == true }
                ?.mapTo(linkedSetOf()) { it.asString() }
                .orEmpty(),
        )

    override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? =
        merge(delegate.getTopLevelClassifierNamesInPackage(packageFqName), snapshotProvider()?.classifierNames(packageFqName).orEmpty())

    override fun getPackageNamesWithTopLevelCallables(): Set<String>? =
        merge(
            delegate.getPackageNamesWithTopLevelCallables(),
            snapshotProvider()?.filesByPackage?.keys
                ?.filterTo(linkedSetOf()) { snapshotProvider()?.callableNames(it)?.isNotEmpty() == true }
                ?.mapTo(linkedSetOf()) { it.asString() }
                .orEmpty(),
        )

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>? =
        merge(delegate.getTopLevelCallableNamesInPackage(packageFqName), snapshotProvider()?.callableNames(packageFqName).orEmpty())

    override fun mayHaveSyntheticFunctionType(classId: ClassId): Boolean = delegate.mayHaveSyntheticFunctionType(classId)

    private fun <T> merge(base: Set<T>?, additional: Set<T>): Set<T>? = base?.plus(additional)
}

/** 为 Analysis API 名称发现单独枚举宏生成源码声明，不扫描物理源码名称。 */
private class LLCfirMacroGeneratedSourceNamesProvider(
    private val snapshotProvider: () -> org.cangnova.cangjie.analysis.low.level.api.cfir.macro.LLCfirMacroSemanticSnapshot?,
) : CfirSymbolNamesProvider() {
    override fun getPackageNames(): Set<String>? {
        val snapshot = snapshotProvider() ?: return emptySet()
        return snapshot.filesByPackage.keys
            .filterTo(linkedSetOf()) { packageFqName ->
                snapshot.macroGeneratedClassifierNames(packageFqName).isNotEmpty() ||
                    snapshot.macroGeneratedCallableNames(packageFqName).isNotEmpty()
            }
            .mapTo(linkedSetOf()) { it.asString() }
    }

    override val hasSpecificClassifierPackageNamesComputation: Boolean
        get() = true

    override fun getPackageNamesWithTopLevelClassifiers(): Set<String>? {
        val snapshot = snapshotProvider() ?: return emptySet()
        return snapshot.filesByPackage.keys
            .filterTo(linkedSetOf()) { packageFqName ->
                snapshot.macroGeneratedClassifierNames(packageFqName).isNotEmpty()
            }
            .mapTo(linkedSetOf()) { it.asString() }
    }

    override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? =
        snapshotProvider()?.macroGeneratedClassifierNames(packageFqName).orEmpty()

    override val hasSpecificCallablePackageNamesComputation: Boolean
        get() = true

    override fun getPackageNamesWithTopLevelCallables(): Set<String>? {
        val snapshot = snapshotProvider() ?: return emptySet()
        return snapshot.filesByPackage.keys
            .filterTo(linkedSetOf()) { packageFqName ->
                snapshot.macroGeneratedCallableNames(packageFqName).isNotEmpty()
            }
            .mapTo(linkedSetOf()) { it.asString() }
    }

    override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>? =
        snapshotProvider()?.macroGeneratedCallableNames(packageFqName).orEmpty()
}
