package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.nameConflictsTracker
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirEnum
import org.cangnova.cangjie.cfir.declarations.CfirEnumConstructor
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirMacroDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.callableNameOrNull
import org.cangnova.cangjie.cfir.patterns.bindingVariables
import org.cangnova.cangjie.cfir.resolve.providers.macro.RecordableRawCfirFiles
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirMacroDeclarationSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumSymbol
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.CfirStructSymbol
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * Source-side provider aligned with Kotlin's `FirProviderImpl`.
 *
 * It owns source indexes and exposes them through both:
 * - [CfirProvider] APIs for declaration lookup.
 * - [symbolProvider] for class/callable symbol resolution.
 *
 * @property session 当前 source provider 绑定的 CFIR session。
 * @property cangjieScopeProvider 为源码声明创建成员作用域时使用的 scope provider。
 */
class CfirProviderImpl(
    val session: CfirSession,
    val cangjieScopeProvider: CfirCangJieScopeProvider = CfirCangJieScopeProvider(),
) : CfirProvider() {

    /**
     * 源码 provider 对外暴露的 symbol lookup 入口。
     */
    override val symbolProvider: CfirSymbolProvider = SourceSymbolProvider()

    /**
     * 源码声明索引的可变状态。
     */
    private val state = State()

    /**
     * 按包缓存 reexport 后可见的顶层名称视图。
     */
    private val exportedTopLevelNamesCache: MutableMap<FqName, SourceExportedTopLevelNames> = hashMapOf()

    /**
     * Source provider 注册状态机（baseline 第 5 节）。
     *
     * - [EMPTY]：尚未开始 macro construction；`getAllFiles()` 必为空。
     * - [OPEN_FOR_EXPANDED_RECORD]：`recordExpandedFilesOnce` 正在写入展开后文件。
     * - [FINALIZED]：已进入 ordinary resolve 阶段，禁止再写入。
     *
     * 唯一规范的进入路径：
     * `recordExpandedRawFilesOnce(provider, files, registry)`。
     */
    private enum class RecordingState { EMPTY, OPEN_FOR_EXPANDED_RECORD, FINALIZED }

    /**
     * 当前源码文件注册状态。
     */
    @Volatile
    private var recordingState: RecordingState = RecordingState.EMPTY

    /**
     * 保护源码文件一次性注册状态机的锁。
     */
    private val recordingLock = Any()

    /** Provider 是否已经被 finalized，禁止后续注册。 */
    val isFinalized: Boolean get() = recordingState == RecordingState.FINALIZED

    /** Provider 是否尚未开始任何注册（construction 前必须为 true）。 */
    val isEmpty: Boolean get() = recordingState == RecordingState.EMPTY

    /** 返回所有已注册的源码文件。 */
    fun getAllFiles(): List<CfirFile> = state.fileMap.values.flatten()

    /**
     * 由 [org.cangnova.cangjie.cfir.resolve.providers.macro.recordExpandedRawFilesOnce] 调用的
     * 唯一规范注册入口：
     * - 强制 `EMPTY → OPEN_FOR_EXPANDED_RECORD → FINALIZED` 单调推进；
     * - 一次性写入所有由 macro construction 产出的可注册文件；
     * - 写入完成立即 finalize，禁止再次进入。
     */
    internal fun recordExpandedFilesOnce(files: RecordableRawCfirFiles) {
        synchronized(recordingLock) {
            check(recordingState == RecordingState.EMPTY) {
                "Source CfirProviderImpl is not empty (recordingState=$recordingState); " +
                    "construction must run on a fresh source provider."
            }
            recordingState = RecordingState.OPEN_FOR_EXPANDED_RECORD
            try {
                for (file in files.files) {
                    recordFileInternal(file)
                }
                recordingState = RecordingState.FINALIZED
            } catch (t: Throwable) {
                // 即使中途失败也强制进入 FINALIZED，避免后续混入残留 mutation
                recordingState = RecordingState.FINALIZED
                throw t
            }
        }
    }

    /**
     * 将单个源码文件写入包、声明、callable 与 reexport 索引。
     */
    private fun recordFileInternal(file: CfirFile) {
        val packageName = file.packageDirective.packageFqName
        state.fileMap.getOrPut(packageName, ::mutableListOf).add(file)
        recordPackageAndParents(packageName)

        for (declaration in file.declarations) {
            recordDeclaration(
                declaration = declaration,
                packageFqName = packageName,
                containingClass = null,
                containingFile = file,
                isTopLevel = true,
            )
        }

        for (import in file.imports) {
            import.reexportInfoOrNull()?.let { reexport ->
                state.exportedImportsInPackage.getOrPut(packageName, ::mutableListOf).add(reexport)
            }
        }
    }

    /**
     * 返回指定包下所有已注册源码文件。
     */
    override fun getCfirFilesByPackage(fqName: FqName): List<CfirFile> =
        state.fileMap[fqName].orEmpty()

    /**
     * 从 source 索引中按 [classId] 查找 class-like 声明。
     */
    override fun getCfirClassifierByFqName(classId: ClassId): CfirClassLikeDeclaration? {
        val symbol = state.classifierMap[classId]
            ?: symbolProvider.getClassLikeSymbolByClassId(classId)
            ?: return null
        return if (symbol.isBound) symbol.cfir as? CfirClassLikeDeclaration else null
    }

    /**
     * 返回 source classifier 对应的容器文件。
     */
    override fun getCfirClassifierContainerFile(fqName: ClassId): CfirFile =
        state.classifierContainerFileMap[fqName]
            ?: error("No containing file recorded for classifier $fqName")

    /**
     * 尝试返回 source classifier 对应的容器文件。
     */
    override fun getCfirClassifierContainerFileIfAny(fqName: ClassId): CfirFile? =
        state.classifierContainerFileMap[fqName]

    /**
     * 返回 source callable 对应的容器文件。
     */
    override fun getCfirCallableContainerFile(symbol: CfirCallableSymbol<*>): CfirFile? =
        state.callableContainerFileMap[symbol.unwrapCallableForDeclarationMetadataLookup()]

    /**
     * 返回 pattern binding 所属的外层 pattern variable。
     */
    override fun getCfirPatternVariableForBinding(symbol: CfirPatternBindingSymbol): CfirPatternVariable? {
        val ownerSymbol = state.patternBindingOwnerMap[symbol] ?: return null
        return ownerSymbol.takeIf { it.isBound }?.cfir
    }

    /**
     * 返回指定包下 reexport 后可见的顶层 classifier 名称。
     */
    override fun getClassNamesInPackage(fqName: FqName): Set<Name> =
        resolveSourcePackageTopLevelNames(fqName).classifierNames

    /**
     * 返回 source symbol 所属的 class-like 宿主。
     */
    override fun getContainingClass(symbol: org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol<*>): CfirClassLikeSymbol<*>? {
        val normalizedSymbol = symbol.unwrapForDeclarationMetadataLookup()
        if (normalizedSymbol !is CfirCallableSymbol<*>) {
            return super.getContainingClass(normalizedSymbol)
        }

        val ownerClassId = normalizedSymbol.callableId.classId ?: state.callableOwnerClassIdMap[normalizedSymbol]
        return ownerClassId?.let(state.classifierMap::get) ?: super.getContainingClass(normalizedSymbol)
    }

    /**
     * source provider 的 symbol lookup 实现。
     */
    private inner class SourceSymbolProvider : CfirSymbolProvider(session) {
        /**
         * 源码名称索引，包含本包声明和 reexport 后可见的顶层声明。
         */
        override val symbolNamesProvider: CfirSymbolNamesProvider = object : CfirSymbolNamesProvider() {
            /**
             * 返回源码 provider 已知的所有包及其父包。
             */
            override fun getPackageNames(): Set<String> = state.allSubPackages.mapTo(linkedSetOf()) { it.asString() }

            /**
             * source provider 的 classifier 包集合复用通用包集合。
             */
            override val hasSpecificClassifierPackageNamesComputation: Boolean
                get() = false

            /**
             * 返回指定包下可见的顶层 classifier 名称。
             */
            override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name> =
                resolveSourcePackageTopLevelNames(packageFqName).classifierNames

            /**
             * source provider 的 callable 包集合复用通用包集合。
             */
            override val hasSpecificCallablePackageNamesComputation: Boolean
                get() = false

            /**
             * 返回指定包下可见的顶层 callable 名称。
             */
            override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> =
                resolveSourcePackageTopLevelNames(packageFqName).callableNames
        }

        /**
         * 按 [classId] 加载 source 或 reexport 后可见的 class-like symbol。
         */
        override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? =
            resolveSourcePackageTopLevelClassSymbol(classId)

        /**
         * 将 source 或 reexport 后可见的顶层 callable symbol 追加到 [destination]。
         */
        @CfirSymbolProviderInternals
        override fun getTopLevelCallableSymbolsTo(
            destination: MutableList<CfirCallableSymbol<*>>,
            packageFqName: FqName,
            name: Name,
        ) {
            destination += resolveSourcePackageTopLevelCallableSymbols(packageFqName, name)
        }

        /**
         * 将 source 或 reexport 后可见的顶层函数 symbol 追加到 [destination]。
         */
        @CfirSymbolProviderInternals
        override fun getTopLevelFunctionSymbolsTo(
            destination: MutableList<CfirNamedFunctionSymbol>,
            packageFqName: FqName,
            name: Name,
        ) {
            destination += resolveSourcePackageTopLevelFunctionSymbols(packageFqName, name)
        }

        /**
         * 将 source 或 reexport 后可见的顶层属性 symbol 追加到 [destination]。
         */
        @CfirSymbolProviderInternals
        override fun getTopLevelPropertySymbolsTo(
            destination: MutableList<CfirPropertySymbol>,
            packageFqName: FqName,
            name: Name,
        ) {
            destination += resolveSourcePackageTopLevelPropertySymbols(packageFqName, name)
        }

        /**
         * 判断 source 索引中是否存在指定包或其子包路径。
         */
        override fun hasPackage(fqName: FqName): Boolean =
            fqName in state.allSubPackages
    }

    /**
     * source package 侧的导出顶层名视图。
     *
     * 与 `.cjo` 读取侧保持同一策略：
     * 先放入本包物理声明，再递归合并 reexport import，
     * 同名时保持先到先得，让本包物理声明优先。
     */
    private fun resolveSourcePackageTopLevelNames(packageFqName: FqName): SourceExportedTopLevelNames {
        if (packageFqName !in state.allSubPackages) {
            return EMPTY_EXPORTED_TOP_LEVEL_NAMES
        }
        return resolveAvailableTopLevelNames(packageFqName, linkedSetOf())
    }

    /**
     * 解析指定包经过 reexport 后可见的顶层名称。
     *
     * [visiting] 用于切断包之间的循环 reexport，保证递归解析不会无限展开。
     */
    private fun resolveAvailableTopLevelNames(
        packageFqName: FqName,
        visiting: LinkedHashSet<FqName>,
    ): SourceExportedTopLevelNames {
        exportedTopLevelNamesCache[packageFqName]?.let { return it }
        if (packageFqName !in state.allSubPackages) {
            return resolveDelegatedTopLevelNames(packageFqName)
        }
        if (!visiting.add(packageFqName)) return EMPTY_EXPORTED_TOP_LEVEL_NAMES

        val callableNames = linkedSetOf<Name>().apply {
            addAll(state.callableNamesInPackage[packageFqName].orEmpty())
        }
        val classifierNames = linkedSetOf<Name>().apply {
            addAll(state.classifierInPackage[packageFqName].orEmpty())
        }
        val callableTargets = linkedMapOf<Name, SourceExportedTopLevelTarget>().apply {
            for (name in callableNames) {
                put(name, SourceExportedTopLevelTarget(packageFqName, name))
            }
        }
        val classifierTargets = linkedMapOf<Name, SourceExportedTopLevelTarget>().apply {
            for (name in classifierNames) {
                put(name, SourceExportedTopLevelTarget(packageFqName, name))
            }
        }

        for (reexport in state.exportedImportsInPackage[packageFqName].orEmpty()) {
            val importedNames = resolveAvailableTopLevelNames(reexport.importedPackageFqName, visiting)

            if (reexport.isAllUnder) {
                callableNames += importedNames.callableNames
                classifierNames += importedNames.classifierNames
                mergeExportTargets(callableTargets, importedNames.callableTargets)
                mergeExportTargets(classifierTargets, importedNames.classifierTargets)
                continue
            }

            val importedName = reexport.importedName ?: continue
            val exportedName = reexport.exportedName ?: continue

            if (importedName in importedNames.callableNames) {
                callableNames += exportedName
                importedNames.callableTargets[importedName]?.let { target ->
                    callableTargets.putIfAbsent(exportedName, target)
                }
            }
            if (importedName in importedNames.classifierNames) {
                classifierNames += exportedName
                importedNames.classifierTargets[importedName]?.let { target ->
                    classifierTargets.putIfAbsent(exportedName, target)
                }
            }
        }

        visiting.remove(packageFqName)
        val resolved = SourceExportedTopLevelNames(
            callableNames = callableNames,
            classifierNames = classifierNames,
            callableTargets = callableTargets,
            classifierTargets = classifierTargets,
        )
        exportedTopLevelNamesCache.putIfAbsent(packageFqName, resolved)
        return exportedTopLevelNamesCache[packageFqName] ?: resolved
    }

    /**
     * 从当前 session 的委托 symbol provider 中解析非 source 包的顶层名称。
     *
     * 该路径用于 source reexport 指向 library/builtin 包时的名称桥接。
     */
    private fun resolveDelegatedTopLevelNames(packageFqName: FqName): SourceExportedTopLevelNames {
        exportedTopLevelNamesCache[packageFqName]?.let { return it }

        val callableNames = linkedSetOf<Name>()
        val classifierNames = linkedSetOf<Name>()
        for (provider in delegatedSymbolProviders()) {
            callableNames += provider.symbolNamesProvider.getTopLevelCallableNamesInPackage(packageFqName).orEmpty()
            classifierNames += provider.symbolNamesProvider.getTopLevelClassifierNamesInPackage(packageFqName).orEmpty()
        }

        val resolved = SourceExportedTopLevelNames(
            callableNames = callableNames,
            classifierNames = classifierNames,
            callableTargets = callableNames.associateWithTo(linkedMapOf()) { name ->
                SourceExportedTopLevelTarget(packageFqName, name)
            },
            classifierTargets = classifierNames.associateWithTo(linkedMapOf()) { name ->
                SourceExportedTopLevelTarget(packageFqName, name)
            },
        )
        exportedTopLevelNamesCache.putIfAbsent(packageFqName, resolved)
        return exportedTopLevelNamesCache[packageFqName] ?: resolved
    }

    /**
     * 返回当前 session 中除本 source symbol provider 以外的委托 symbol provider。
     */
    private fun delegatedSymbolProviders(): List<CfirSymbolProvider> {
        val composite = session.symbolProvider as? CfirCompositeSymbolProvider ?: return emptyList()
        return composite.providers.filterNot { it === symbolProvider }
    }

    /**
     * 解析 source 包中顶层 class-like 短名对应的 symbol。
     */
    private fun resolveSourcePackageTopLevelClassSymbol(classId: ClassId): CfirClassLikeSymbol<*>? {
        if (classId.packageFqName !in state.allSubPackages) return null
        val target = resolveSourcePackageTopLevelNames(classId.packageFqName)
            .classifierTargets[classId.shortClassName]
            ?: return null
        return loadTargetClassLikeSymbol(target)
    }

    /**
     * 解析 source 包中顶层 callable 短名对应的 symbol 集合。
     */
    private fun resolveSourcePackageTopLevelCallableSymbols(
        packageFqName: FqName,
        name: Name,
    ): List<CfirCallableSymbol<*>> {
        if (packageFqName !in state.allSubPackages) return emptyList()
        val target = resolveSourcePackageTopLevelNames(packageFqName).callableTargets[name] ?: return emptyList()
        return loadTargetCallableSymbols(target)
    }

    /**
     * 解析 source 包中顶层函数短名对应的 symbol 集合。
     */
    private fun resolveSourcePackageTopLevelFunctionSymbols(
        packageFqName: FqName,
        name: Name,
    ): List<CfirNamedFunctionSymbol> {
        if (packageFqName !in state.allSubPackages) return emptyList()
        val target = resolveSourcePackageTopLevelNames(packageFqName).callableTargets[name] ?: return emptyList()
        return loadTargetFunctionSymbols(target)
    }

    /**
     * 解析 source 包中顶层属性短名对应的 symbol 集合。
     */
    private fun resolveSourcePackageTopLevelPropertySymbols(
        packageFqName: FqName,
        name: Name,
    ): List<CfirPropertySymbol> {
        if (packageFqName !in state.allSubPackages) return emptyList()
        val target = resolveSourcePackageTopLevelNames(packageFqName).callableTargets[name] ?: return emptyList()
        return loadTargetPropertySymbols(target)
    }

    /**
     * 根据导出目标加载 class-like symbol。
     *
     * 先查当前 source 索引，再查委托 provider，以支持 reexport 到非源码包。
     */
    private fun loadTargetClassLikeSymbol(target: SourceExportedTopLevelTarget): CfirClassLikeSymbol<*>? {
        val classId = ClassId(target.packageFqName, target.name)
        state.classifierMap[classId]?.let { return it as? CfirClassLikeSymbol<*> }
        for (provider in delegatedSymbolProviders()) {
            provider.getClassLikeSymbolByClassId(classId)?.let { return it }
        }
        return null
    }

    /**
     * 根据导出目标加载 callable symbol。
     */
    @OptIn(CfirSymbolProviderInternals::class)
    private fun loadTargetCallableSymbols(target: SourceExportedTopLevelTarget): List<CfirCallableSymbol<*>> {
        val callableId = CallableId(target.packageFqName, target.name)
        return buildList {
            addAll(state.callableMap[callableId].orEmpty())
            for (provider in delegatedSymbolProviders()) {
                provider.getTopLevelCallableSymbolsTo(this, target.packageFqName, target.name)
            }
        }.distinct()
    }

    /**
     * 根据导出目标加载函数 symbol。
     */
    @OptIn(CfirSymbolProviderInternals::class)
    private fun loadTargetFunctionSymbols(target: SourceExportedTopLevelTarget): List<CfirNamedFunctionSymbol> {
        val callableId = CallableId(target.packageFqName, target.name)
        return buildList {
            addAll(state.functionMap[callableId].orEmpty())
            for (provider in delegatedSymbolProviders()) {
                provider.getTopLevelFunctionSymbolsTo(this, target.packageFqName, target.name)
            }
        }.distinct()
    }

    /**
     * 根据导出目标加载属性 symbol。
     */
    @OptIn(CfirSymbolProviderInternals::class)
    private fun loadTargetPropertySymbols(target: SourceExportedTopLevelTarget): List<CfirPropertySymbol> {
        val callableId = CallableId(target.packageFqName, target.name)
        return buildList {
            addAll(state.propertyMap[callableId].orEmpty())
            for (provider in delegatedSymbolProviders()) {
                provider.getTopLevelPropertySymbolsTo(this, target.packageFqName, target.name)
            }
        }.distinct()
    }

    /**
     * 将 reexport 来源的目标映射合并到目标表。
     *
     * 已存在的 key 不会被覆盖，从而保持本包声明优先于 reexport 声明。
     */
    private fun mergeExportTargets(
        destination: MutableMap<Name, SourceExportedTopLevelTarget>,
        source: Map<Name, SourceExportedTopLevelTarget>,
    ) {
        for ((visibleName, target) in source) {
            destination.putIfAbsent(visibleName, target)
        }
    }

    /**
     * 记录包及所有父包，保证包存在性查询可以识别中间包路径。
     */
    private fun recordPackageAndParents(packageName: FqName) {
        var current = packageName
        while (true) {
            state.allSubPackages.add(current)
            if (current.isRoot) break
            current = current.parent()
        }
    }

    /**
     * 将一个 CFIR 声明写入 source provider 的分类器、callable、文件归属和宿主索引。
     *
     * [isTopLevel] 控制声明是否进入包级名称索引；成员声明只记录元数据，不暴露为顶层符号。
     */
    private fun recordDeclaration(
        declaration: CfirDeclaration,
        packageFqName: FqName,
        containingClass: ClassId?,
        containingFile: CfirFile,
        isTopLevel: Boolean,
    ) {
        when (declaration) {
            // ---- 具名分类器类型 ----

            is CfirClass -> {
                if (!isTopLevel) return
                val classId = computeClassId(packageFqName, declaration.name)
                recordClassLikeClassifier(
                    symbol = declaration.symbol as? CfirClassSymbol,
                    packageFqName = packageFqName,
                    shortName = declaration.name,
                    containingFile = containingFile,
                    isTopLevel = true,
                )
                recordMemberDeclarations(
                    declarations = declaration.declarations,
                    packageFqName = packageFqName,
                    ownerClassId = classId,
                    containingFile = containingFile,
                )
            }

            is CfirInterface -> {
                if (!isTopLevel) return
                val classId = computeClassId(packageFqName, declaration.name)
                recordClassLikeClassifier(
                    symbol = declaration.symbol as? CfirInterfaceSymbol,
                    packageFqName = packageFqName,
                    shortName = declaration.name,
                    containingFile = containingFile,
                    isTopLevel = true,
                )
                // 接口成员只采集直接 callable，不采集任何 class-like。
                recordMemberDeclarations(
                    declarations = declaration.declarations ,
                    packageFqName = packageFqName,
                    ownerClassId = classId,
                    containingFile = containingFile,
                )
            }

            is CfirStruct -> {
                if (!isTopLevel) return
                val classId = computeClassId(packageFqName, declaration.name)
                recordClassLikeClassifier(
                    symbol = declaration.symbol as? CfirStructSymbol,
                    packageFqName = packageFqName,
                    shortName = declaration.name,
                    containingFile = containingFile,
                    isTopLevel = true,
                )
                recordMemberDeclarations(
                    declarations = declaration.declarations,
                    packageFqName = packageFqName,
                    ownerClassId = classId,
                    containingFile = containingFile,
                )
            }

            is CfirEnum -> {
                if (!isTopLevel) return
                val classId = computeClassId(packageFqName, declaration.name)
                recordClassLikeClassifier(
                    symbol = declaration.symbol as? CfirEnumSymbol,
                    packageFqName = packageFqName,
                    shortName = declaration.name,
                    containingFile = containingFile,
                    isTopLevel = true,
                )
                // 枚举构造器在包级可见，但 enum 内部 class-like 仍不进入公开索引。
                recordTopLevelEnumConstructors(
                    declaration = declaration,
                    ownerClassId = classId,
                    containingFile = containingFile,
                )
                recordMemberDeclarations(
                    declarations = declaration.declarations,
                    packageFqName = packageFqName,
                    ownerClassId = classId,
                    containingFile = containingFile,
                )
            }

            is CfirTypeAlias -> {
                if (!isTopLevel) return
                recordClassLikeClassifier(
                    symbol = declaration.symbol as? CfirClassLikeSymbol<*>,
                    packageFqName = packageFqName,
                    shortName = declaration.name,
                    containingFile = containingFile,
                    isTopLevel = true,
                )
            }

            is CfirExtend -> {
                if (!isTopLevel) return
                recordExtendMemberDeclarations(
                    declarations = declaration.declarations,
                    packageFqName = packageFqName,
                    containingFile = containingFile,
                )
            }

            // ---- 可调用声明（仅顶层注册）----


            is CfirProperty -> {
                val symbol = declaration.symbol as? CfirPropertySymbol ?: return
                state.callableContainerFileMap[symbol] = containingFile
                state.callableOwnerClassIdMap[symbol] = containingClass
                if (!isTopLevel) return
                val callableId = CallableId(packageFqName, declaration.name)
                state.propertyMap.getOrPut(callableId, ::mutableListOf).add(symbol)
                state.callableMap.getOrPut(callableId, ::mutableListOf).add(symbol)
                state.callableNamesInPackage.getOrPut(packageFqName, ::mutableSetOf).add(declaration.name)
            }

            is CfirPatternVariable -> {
                val symbol = declaration.symbol as? CfirPatternVariableSymbol ?: return
                state.callableContainerFileMap[symbol] = containingFile
                state.callableOwnerClassIdMap[symbol] = containingClass
                if (!isTopLevel) return
                for (bindingVariable in declaration.pattern.bindingVariables()) {
                    val bindingSymbol = bindingVariable.symbol as? CfirPatternBindingSymbol ?: continue
                    state.callableContainerFileMap[bindingSymbol] = containingFile
                    state.callableOwnerClassIdMap[bindingSymbol] = containingClass
                    state.patternBindingOwnerMap[bindingSymbol] = symbol
                    val callableId = CallableId(packageFqName, bindingVariable.name)
                    state.callableMap.getOrPut(callableId, ::mutableListOf).add(bindingSymbol)
                    state.callableNamesInPackage.getOrPut(packageFqName, ::mutableSetOf).add(bindingVariable.name)
                }
            }

            is CfirFieldVariable -> {
                val symbol = declaration.symbol as? CfirFieldVariableSymbol ?: return
                state.callableContainerFileMap[symbol] = containingFile
                state.callableOwnerClassIdMap[symbol] = containingClass
                if (!isTopLevel) return
                val callableId = CallableId(packageFqName, declaration.name)
                state.callableMap.getOrPut(callableId, ::mutableListOf).add(symbol)
                state.callableNamesInPackage.getOrPut(packageFqName, ::mutableSetOf).add(declaration.name)
            }

            is CfirMacroDeclaration -> {
                val symbol = declaration.symbol as? CfirMacroDeclarationSymbol ?: return
                state.callableContainerFileMap[symbol] = containingFile
                state.callableOwnerClassIdMap[symbol] = containingClass
                if (!isTopLevel) return
                val callableId = CallableId(packageFqName, declaration.name)
                state.callableMap.getOrPut(callableId, ::mutableListOf).add(symbol)
                state.callableNamesInPackage.getOrPut(packageFqName, ::mutableSetOf).add(declaration.name)
            }

            is CfirConstructor -> {
                val symbol = declaration.symbol as? CfirConstructorSymbol ?: return
                state.callableContainerFileMap[symbol] = containingFile
                state.callableOwnerClassIdMap[symbol] = containingClass
            }

            is CfirFunction -> {
                val symbol = declaration.symbol as? CfirNamedFunctionSymbol ?: return
                state.callableContainerFileMap[symbol] = containingFile
                state.callableOwnerClassIdMap[symbol] = containingClass
                if (!isTopLevel) return
                val callableName = declaration.callableNameOrNull() ?: return
                val callableId = CallableId(packageFqName, callableName)
                state.functionMap.getOrPut(callableId, ::mutableListOf).add(symbol)
                state.callableMap.getOrPut(callableId, ::mutableListOf).add(symbol)
                state.callableNamesInPackage.getOrPut(packageFqName, ::mutableSetOf).add(callableName)
            }
            else -> Unit
        }
    }

    /**
     * extend 成员不是目标类型的物理 class member，目标类型语义归属由
     * [CfirExtendProvider] / extend index 负责；provider 这里只登记声明所在文件，
     * 保持 visibility、diagnostic、metadata 查询与 Kotlin FirProvider 的职责一致。
     */
    private fun recordExtendMemberDeclarations(
        declarations: Collection<CfirDeclaration>,
        packageFqName: FqName,
        containingFile: CfirFile,
    ) {
        for (member in declarations) {
            recordDeclaration(
                declaration = member,
                packageFqName = packageFqName,
                containingClass = null,
                containingFile = containingFile,
                isTopLevel = false,
            )
        }
    }

    /**
     * 记录顶层 class-like classifier 及其容器文件。
     *
     * 重复声明会登记到 [nameConflictsTracker]，实际 symbol map 保持首个声明优先。
     */
    private fun recordClassLikeClassifier(
        symbol: CfirClassLikeSymbol<*>?,
        packageFqName: FqName,
        shortName: Name,
        containingFile: CfirFile,
        isTopLevel: Boolean,
    ) {
        val classId = computeClassId(packageFqName, shortName)
        if (symbol != null) {
            val previousSymbol = state.classifierMap[classId]
            if (previousSymbol == null) {
                state.classifierMap[classId] = symbol
                state.classifierContainerFileMap[classId] = containingFile
            } else if (previousSymbol != symbol) {
                session.nameConflictsTracker?.registerClassifierRedeclaration(
                    classId = classId,
                    newSymbol = symbol,
                    newSymbolFile = containingFile,
                    prevSymbol = previousSymbol,
                    prevSymbolFile = state.classifierContainerFileMap[classId],
                )
            }
        }

        if (isTopLevel) {
            state.classifierInPackage.getOrPut(packageFqName, ::mutableSetOf).add(classId.shortClassName)
            state.classesInPackage.getOrPut(packageFqName, ::mutableSetOf).add(classId.shortClassName)
        }
    }

    /**
     * 根据包名与短名构造仓颉当前顶层 class-like 的 [ClassId]。
     */
    private fun computeClassId(
        packageFqName: FqName,
        shortName: Name,
    ): ClassId {
        return ClassId(packageFqName, shortName)
    }

    /**
     * 成员作用域只递归直接 callable。
     *
     * class-like 声明不会继续向成员索引层展开，
     * 从而保证源码符号索引与顶层类型标识规则保持一致。
     */
    private fun recordMemberDeclarations(
        declarations: Collection<CfirDeclaration>,
        packageFqName: FqName,
        ownerClassId: ClassId,
        containingFile: CfirFile,
    ) {
        for (member in declarations) {
            if (member is CfirClassLikeDeclaration) continue
            recordDeclaration(
                declaration = member,
                packageFqName = packageFqName,
                containingClass = ownerClassId,
                containingFile = containingFile,
                isTopLevel = false,
            )
        }
    }

    /**
     * 将 enum constructor 作为包级 callable 记录到 source 索引。
     *
     * 仓颉 enum constructor 在调用解析中需要按包级 callable 暴露，同时仍记录其 enum owner。
     */
    private fun recordTopLevelEnumConstructors(
        declaration: CfirEnum,
        ownerClassId: ClassId,
        containingFile: CfirFile,
    ) {
        declaration.declarations.asSequence()
            .filterIsInstance<CfirEnumConstructor>()
            .forEach { enumConstructor ->
                val symbol = enumConstructor.symbol ?: return@forEach
                val callableId = CallableId(ownerClassId.packageFqName, enumConstructor.name)
                state.callableMap.getOrPut(callableId, ::mutableListOf).add(symbol)
                state.callableNamesInPackage.getOrPut(ownerClassId.packageFqName, ::mutableSetOf)
                    .add(enumConstructor.name)
                state.callableContainerFileMap[symbol] = containingFile
                state.callableOwnerClassIdMap[symbol] = ownerClassId
            }
    }

    /**
     * source provider 的全部可变索引。
     */
    private class State {
        /**
         * 包名到源码文件列表的索引。
         */
        val fileMap: MutableMap<FqName, MutableList<CfirFile>> = hashMapOf()

        /**
         * 已知包及其父包集合。
         */
        val allSubPackages: MutableSet<FqName> = hashSetOf()

        /**
         * ClassId 到 class-like symbol 的索引。
         */
        val classifierMap: MutableMap<ClassId, CfirClassLikeSymbol<*>> = hashMapOf()

        /**
         * ClassId 到声明所在文件的索引。
         */
        val classifierContainerFileMap: MutableMap<ClassId, CfirFile> = hashMapOf()

        /**
         * 包名到顶层 classifier 短名集合的索引。
         */
        val classifierInPackage: MutableMap<FqName, MutableSet<Name>> = hashMapOf()

        /**
         * 包名到顶层 class 短名集合的索引。
         */
        val classesInPackage: MutableMap<FqName, MutableSet<Name>> = hashMapOf()

        /**
         * callable symbol 到声明所在文件的索引。
         */
        val callableContainerFileMap: MutableMap<CfirCallableSymbol<*>, CfirFile> = hashMapOf()

        /**
         * callable symbol 到所属 class id 的索引。
         */
        val callableOwnerClassIdMap: MutableMap<CfirCallableSymbol<*>, ClassId?> = hashMapOf()

        /**
         * pattern binding symbol 到外层 pattern variable symbol 的索引。
         */
        val patternBindingOwnerMap: MutableMap<CfirPatternBindingSymbol, CfirPatternVariableSymbol> = hashMapOf()

        /**
         * CallableId 到顶层 callable symbol 列表的索引。
         */
        val callableMap: MutableMap<CallableId, MutableList<CfirCallableSymbol<*>>> = hashMapOf()

        /**
         * CallableId 到顶层函数 symbol 列表的索引。
         */
        val functionMap: MutableMap<CallableId, MutableList<CfirNamedFunctionSymbol>> = hashMapOf()

        /**
         * CallableId 到顶层属性 symbol 列表的索引。
         */
        val propertyMap: MutableMap<CallableId, MutableList<CfirPropertySymbol>> = hashMapOf()

        /**
         * 包名到顶层 callable 短名集合的索引。
         */
        val callableNamesInPackage: MutableMap<FqName, MutableSet<Name>> = hashMapOf()

        /**
         * 包名到该包中 reexport import 的索引。
         */
        val exportedImportsInPackage: MutableMap<FqName, MutableList<CfirReexportImportInfo>> = hashMapOf()
    }

    /**
     * reexport 后实际需要加载的顶层声明目标。
     *
     * @property packageFqName 目标声明真实所在包名。
     * @property name 目标声明真实短名。
     */
    private data class SourceExportedTopLevelTarget(
        val packageFqName: FqName,
        val name: Name,
    )

    /**
     * 一个包经过 reexport 合并后的顶层名称视图。
     *
     * @property callableNames 对调用方可见的 callable 短名集合。
     * @property classifierNames 对调用方可见的 classifier 短名集合。
     * @property callableTargets 可见 callable 短名到真实声明目标的映射。
     * @property classifierTargets 可见 classifier 短名到真实声明目标的映射。
     */
    private data class SourceExportedTopLevelNames(
        val callableNames: Set<Name>,
        val classifierNames: Set<Name>,
        val callableTargets: Map<Name, SourceExportedTopLevelTarget> = emptyMap(),
        val classifierTargets: Map<Name, SourceExportedTopLevelTarget> = emptyMap(),
    )

    /**
     * source provider 的常量与空视图。
     */
    private companion object {
        /**
         * 不存在或不可见包的空顶层名称视图。
         */
        val EMPTY_EXPORTED_TOP_LEVEL_NAMES = SourceExportedTopLevelNames(
            callableNames = emptySet(),
            classifierNames = emptySet(),
        )
    }
}
