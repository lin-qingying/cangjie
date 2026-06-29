

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.serialization.provider.AbstractCfirDeserializedSymbolProvider
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
 * [LLCfirSession] 的模块级 [CfirSymbolProvider]。
 *
 * 该提供器组合模块自身内容的多个 [providers] 与依赖侧 [dependencyProvider]，对外呈现“当前模块加依赖”的完整符号视图。
 *
 * 该类不会实现 [LLPsiAwareSymbolProvider]。按特定模块进行 PSI 精确访问时，只能考虑模块自身符号提供器，不能把依赖混入；
 * 而 [LLModuleWithDependenciesSymbolProvider] 覆写的常规符号查询必须包含依赖。因此需要通过
 * [getClassLikeSymbolByPsiWithoutDependencies] 这类入口显式表达“仅当前模块”的查询语义。
 *
 * ### 模块内容范围
 *
 * [LLModuleWithDependenciesSymbolProvider] 必须且只能为关联模块内容范围内的声明提供符号。对同一个 `declaration` 与 `module`，
 * 下列事实应保持一致：
 *
 * - `declaration` 位于 `module` 的内容范围内。
 * - 工程结构提供器会把 `declaration` 归属到 `module`，或在使用点消歧时选择一个等价候选模块。
 * - `module` 对应 CFIR 会话的符号提供器能够为 `declaration` 提供符号。
 *
 * 内容范围是这里的事实来源。符号提供器实现必须与内容范围一致，即使底层 JAR 或文件系统中实际存在更多文件。例如库模块的
 * 内容范围可能排除某些物理存在于 JAR 中的文件。
 *
 * [CangJieProjectStructureProvider][org.cangnova.cangjie.analysis.api.platform.projectStructure.CangJieProjectStructureProvider]
 * 在平台层承担同样的归属一致性责任。
 */
internal class LLModuleWithDependenciesSymbolProvider(
    session: LLCfirSession,
    /**
     * 当前模块自身内容使用的符号提供器列表。
     *
     * 列表中的提供器共同覆盖源码、反序列化库、stub 库等模块内符号来源，不应包含模块依赖聚合器。
     */
    val providers: List<CfirSymbolProvider>,
    /**
     * 当前模块依赖符号的扁平化聚合提供器。
     *
     * 常规查询会在 [providers] 之后查询该提供器，确保模块自身声明优先于依赖声明。
     */
    val dependencyProvider: LLDependenciesSymbolProvider,
) : CfirSymbolProvider(session) {
    /**
     * 组合当前模块与依赖的缓存名称提供器。
     *
     * [LLModuleWithDependenciesSymbolProvider] 本身不会直接依赖该对象进行查询，因为 IDE 中 Java 符号提供器目前通常无法提供名称集合，
     * 多数情况下结果会是 `null`。但独立模式需要通过名称提供器计算包作用域中的 classifier/callable 名称集合；fallback 声明提供器
     * 无法覆盖二进制库符号，因此这里必须组合所有模块内提供器与依赖提供器的名称索引。
     *
     * 该属性必须延迟初始化，以避免提前访问 [LLDependenciesSymbolProvider.providers] 并触发循环依赖模块的会话构造。
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

    /**
     * 先在当前模块自身内容中查找 [classId]，未命中时再查找依赖。
     */
    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? =
        getClassLikeSymbolByClassIdWithoutDependencies(classId)
            ?: dependencyProvider.getClassLikeSymbolByClassId(classId)

    /**
     * 仅在当前模块自身 [providers] 中查找 [classId]，不读取依赖符号。
     */
    fun getClassLikeSymbolByClassIdWithoutDependencies(classId: ClassId): CfirClassLikeSymbol<*>? =
        providers.firstNotNullOfOrNull { it.getClassLikeSymbolByClassId(classId) }

    /**
     * 仅在当前模块自身 [providers] 中按 [declaration] PSI 精确查找 [classId]。
     */
    @LLModuleSpecificSymbolProviderAccess
    fun getClassLikeSymbolByPsiWithoutDependencies(
        classId: ClassId,
        declaration: PsiElement,
    ): CfirClassLikeSymbol<*>? =
        providers.firstNotNullOfOrNull { it.getClassLikeSymbolMatchingPsi(classId, declaration) }

    /**
     * 将当前模块和依赖中匹配 [packageFqName]/[name] 的顶层 callable 符号追加到 [destination]。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<CfirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        providers.forEach { it.getTopLevelCallableSymbolsTo(destination, packageFqName, name) }
        dependencyProvider.getTopLevelCallableSymbolsTo(destination, packageFqName, name)
    }

    /**
     * 多文件类 part callable 的兜底提供器。
     *
     * 仅当 stub 库常规查询无法找到 callable 时使用，避免把多文件类 part 的兜底路径提前混入主查询。
     */
    private val multifileClassPartCallableSymbolProvider by lazy(LazyThreadSafetyMode.PUBLICATION) {
        LLCangJieStubBasedLibraryMultifileClassPartCallableSymbolProvider(session)
    }

    /**
     * 仅在当前模块自身提供器中查找反序列化顶层 callable 符号。
     *
     * 该入口用于已经持有 [callableDeclaration] 的库声明场景。它优先使用 stub 库提供器的精确查询，其次读取反序列化提供器；
     * 当常规路径没有结果且模块内存在 stub 库提供器时，再通过多文件类 part 提供器尝试补齐 callable。
     */
    @OptIn(CfirSymbolProviderInternals::class)
    fun getTopLevelDeserializedCallableSymbolsWithoutDependencies(
        packageFqName: FqName,
        shortName: Name,
        callableDeclaration: CjCallableDeclaration,
    ): List<CfirCallableSymbol<*>> = buildList {
        providers.forEach { provider ->
            when (provider) {
                is LLCangJieStubBasedLibrarySymbolProvider ->
                    addIfNotNull(provider.getTopLevelCallableSymbol(packageFqName, shortName, callableDeclaration))

                is AbstractCfirDeserializedSymbolProvider ->
                    provider.getTopLevelCallableSymbolsTo(this, packageFqName, shortName)

                else -> {}
            }
        }

        // Must be called after the original search as this is only a fallback solution
        if (isEmpty() && providers.any { it is LLCangJieStubBasedLibrarySymbolProvider }) {
            multifileClassPartCallableSymbolProvider.addCallableIfNeeded(this, packageFqName, shortName, callableDeclaration)
        }
    }

    /**
     * 将当前模块和依赖中匹配 [packageFqName]/[name] 的顶层函数符号追加到 [destination]。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<CfirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        getTopLevelFunctionSymbolsToWithoutDependencies(destination, packageFqName, name)
        dependencyProvider.getTopLevelFunctionSymbolsTo(destination, packageFqName, name)
    }

    /**
     * 将当前模块和依赖中匹配 [packageFqName]/[name] 的顶层属性符号追加到 [destination]。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<CfirPropertySymbol>, packageFqName: FqName, name: Name) {
        getTopLevelPropertySymbolsToWithoutDependencies(destination, packageFqName, name)
        dependencyProvider.getTopLevelPropertySymbolsTo(destination, packageFqName, name)
    }

    /**
     * 仅把当前模块自身 [providers] 中匹配 [packageFqName]/[name] 的顶层函数符号追加到 [destination]。
     */
    @CfirSymbolProviderInternals
    fun getTopLevelFunctionSymbolsToWithoutDependencies(
        destination: MutableList<CfirNamedFunctionSymbol>,
        packageFqName: FqName,
        name: Name
    ) {
        providers.forEach { it.getTopLevelFunctionSymbolsTo(destination, packageFqName, name) }
    }

    /**
     * 仅把当前模块自身 [providers] 中匹配 [packageFqName]/[name] 的顶层属性符号追加到 [destination]。
     */
    @CfirSymbolProviderInternals
    fun getTopLevelPropertySymbolsToWithoutDependencies(destination: MutableList<CfirPropertySymbol>, packageFqName: FqName, name: Name) {
        providers.forEach { it.getTopLevelPropertySymbolsTo(destination, packageFqName, name) }
    }

    /**
     * 判断当前模块或任一依赖中是否存在 [fqName] 包。
     */
    override fun hasPackage(fqName: FqName): Boolean =
        hasPackageWithoutDependencies(fqName)
                || dependencyProvider.hasPackage(fqName)

    /**
     * 仅判断当前模块自身 [providers] 中是否存在 [fqName] 包。
     */
    fun hasPackageWithoutDependencies(fqName: FqName): Boolean =
        providers.any { it.hasPackage(fqName) }
}

/**
 * 依赖模块符号提供器的惰性扁平化聚合器。
 *
 * 该提供器只代表依赖集合，不包含当前模块自身内容，也不允许嵌套另一个 [LLModuleWithDependenciesSymbolProvider]。
 * 会话创建阶段会把依赖符号提供器展开为扁平列表，避免查询时重复走“模块加依赖”的组合逻辑。
 */
internal class LLDependenciesSymbolProvider(
    session: CfirSession,
    /**
     * 延迟计算依赖符号提供器列表的函数。
     */
    computeProviders: () -> List<CfirSymbolProvider>,
) : CfirSymbolProvider(session) {
    /**
     * 依赖符号提供器列表。
     *
     * 该属性必须惰性计算以支持模块间循环依赖。如果模块 A 与模块 B 相互依赖，而会话创建阶段急切访问依赖符号提供器，
     * 创建 A 会尝试创建 B，创建 B 又会尝试创建 A，最终形成递归初始化。
     */
    val providers: List<CfirSymbolProvider> by lazy {
        computeProviders().also { providers ->
            require(providers.all { it !is LLModuleWithDependenciesSymbolProvider }) {
                "${LLDependenciesSymbolProvider::class.simpleName} may not contain ${LLModuleWithDependenciesSymbolProvider::class.simpleName}:" +
                        " dependency providers must be flattened during session creation."
            }
        }
    }

    /**
     * 依赖聚合器自身不提供可缓存名称集合，名称组合由外层模块级提供器统一完成。
     */
    override val symbolNamesProvider: CfirSymbolNamesProvider = CfirNullSymbolNamesProvider

    /**
     * 按依赖顺序查找 [classId] 对应的第一个 class-like 符号。
     */
    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? =
        providers.firstNotNullOfOrNull { it.getClassLikeSymbolByClassId(classId) }

    /**
     * 按依赖顺序追加匹配 [packageFqName]/[name] 的顶层 callable 符号。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<CfirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        for (provider in providers) {
            provider.getTopLevelCallableSymbolsTo(destination, packageFqName, name)
        }
    }

    /**
     * 按依赖顺序追加匹配 [packageFqName]/[name] 的顶层函数符号。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<CfirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        for (provider in providers) {
            provider.getTopLevelFunctionSymbolsTo(destination, packageFqName, name)
        }
    }

    /**
     * 按依赖顺序追加匹配 [packageFqName]/[name] 的顶层属性符号。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<CfirPropertySymbol>, packageFqName: FqName, name: Name) {
        for (provider in providers) {
            provider.getTopLevelPropertySymbolsTo(destination, packageFqName, name)
        }
    }

    /**
     * 判断任一依赖提供器中是否存在 [fqName] 包。
     */
    override fun hasPackage(fqName: FqName): Boolean = providers.any { it.hasPackage(fqName) }
}
