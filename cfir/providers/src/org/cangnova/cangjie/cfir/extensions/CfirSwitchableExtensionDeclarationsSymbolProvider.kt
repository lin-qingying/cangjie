package org.cangnova.cangjie.cfir.extensions

import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 可临时禁用的扩展生成声明 provider。
 *
 * 该 wrapper 用于生成声明自身解析期间屏蔽 generated declarations，避免扩展 provider
 * 在构造自身产物时递归看见尚未稳定的生成声明。
 */
open class CfirSwitchableExtensionDeclarationsSymbolProvider protected constructor(
    /**
     * 实际提供扩展生成声明的委托 provider。
     */
    private val delegate: CfirExtensionDeclarationsSymbolProvider,
) : CfirSymbolProvider(delegate.session) {
    /**
     * 可开关扩展声明 provider 工厂。
     */
    companion object {
        /**
         * 若 session 中存在扩展声明 provider，则创建可开关 wrapper。
         */
        fun createIfNeeded(session: CfirSession): CfirSwitchableExtensionDeclarationsSymbolProvider? =
            CfirExtensionDeclarationsSymbolProvider.createIfNeeded(session)?.let(::CfirSwitchableExtensionDeclarationsSymbolProvider)
    }

    /**
     * 当前 wrapper 是否处于禁用状态。
     */
    protected open var disabled: Boolean = false

    /**
     * 根据 [disabled] 状态切换的名称索引。
     *
     * 禁用时返回 `null`，表示调用方不能依赖 generated declarations 的名称过滤结果。
     */
    override val symbolNamesProvider: CfirSymbolNamesProvider = object : CfirSymbolNamesProvider() {
        /**
         * 返回委托 provider 的包名集合；禁用时返回未知。
         */
        override fun getPackageNames(): Set<String>? =
            if (disabled) null else delegate.symbolNamesProvider.getPackageNames()

        /**
         * classifier 包集合计算能力与委托 provider 保持一致。
         */
        override val hasSpecificClassifierPackageNamesComputation: Boolean
            get() = delegate.symbolNamesProvider.hasSpecificClassifierPackageNamesComputation

        /**
         * 返回指定包中的 classifier 名称；禁用时返回未知。
         */
        override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? =
            if (disabled) null else delegate.symbolNamesProvider.getTopLevelClassifierNamesInPackage(packageFqName)

        /**
         * callable 包集合计算能力与委托 provider 保持一致。
         */
        override val hasSpecificCallablePackageNamesComputation: Boolean
            get() = delegate.symbolNamesProvider.hasSpecificCallablePackageNamesComputation

        /**
         * 返回指定包中的 callable 名称；禁用时返回未知。
         */
        override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>? =
            if (disabled) null else delegate.symbolNamesProvider.getTopLevelCallableNamesInPackage(packageFqName)
    }

    /**
     * 返回扩展生成的 class-like symbol；禁用时视为不存在。
     */
    override fun getClassLikeSymbolByClassId(classId: ClassId):  CfirClassLikeSymbol<*>? {
        if (disabled) return null
        return delegate.getClassLikeSymbolByClassId(classId)
    }

    /**
     * 将扩展生成的顶层 callable 追加到 [destination]；禁用时不追加任何结果。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(
        destination: MutableList<CfirCallableSymbol<*>>,
        packageFqName: FqName,
        name: Name,
    ) {
        if (disabled) return
        delegate.getTopLevelCallableSymbolsTo(destination, packageFqName, name)
    }

    /**
     * 将扩展生成的顶层函数追加到 [destination]；禁用时不追加任何结果。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<CfirNamedFunctionSymbol>,
        packageFqName: FqName,
        name: Name,
    ) {
        if (disabled) return
        delegate.getTopLevelFunctionSymbolsTo(destination, packageFqName, name)
    }

    /**
     * 将扩展生成的顶层属性追加到 [destination]；禁用时不追加任何结果。
     */
    @CfirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(
        destination: MutableList<CfirPropertySymbol>,
        packageFqName: FqName,
        name: Name,
    ) {
        if (disabled) return
        delegate.getTopLevelPropertySymbolsTo(destination, packageFqName, name)
    }

    /**
     * 判断委托 provider 是否拥有指定包；禁用时返回 `false`。
     */
    override fun hasPackage(fqName: FqName): Boolean {
        if (disabled) return false
        return delegate.hasPackage(fqName)
    }

    /**
     * 禁用 generated declarations 查询。
     */
    fun disable() {
        require(!disabled) {
            "Attempt to disable already disabled ${CfirSwitchableExtensionDeclarationsSymbolProvider::class}"
        }
        disabled = true
    }

    /**
     * 重新启用 generated declarations 查询。
     */
    fun enable() {
        require(disabled) {
            "Attempt to enable already enabled ${CfirSwitchableExtensionDeclarationsSymbolProvider::class}"
        }
        disabled = false
    }

    /**
     * 返回当前 wrapper 是否已禁用。
     */
    internal fun isDisabled(): Boolean = disabled
}

/**
 * 当前 session 中注册的可开关 generated declarations provider。
 */
val CfirSession.generatedDeclarationsSymbolProvider: CfirSwitchableExtensionDeclarationsSymbolProvider?
    by CfirSession.nullableSessionComponentAccessor()

/**
 * 在 [action] 执行期间临时禁用 generated declarations provider。
 */
fun CfirSession.withGeneratedDeclarationsSymbolProviderDisabled(action: () -> Unit) {
    val enabledProvider = generatedDeclarationsSymbolProvider?.takeUnless { it.isDisabled() }
    enabledProvider?.disable()
    try {
        action()
    } finally {
        enabledProvider?.enable()
    }
}
