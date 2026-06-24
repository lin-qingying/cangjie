package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.SessionConfiguration

import org.cangnova.cangjie.cfir.session.CfirComposableSessionComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.resolve.DefaultImportsProvider


/**
 * 默认导入 provider 的可组合 session component。
 */
sealed class CfirDefaultImportsProviderHolder : CfirComposableSessionComponent<CfirDefaultImportsProviderHolder> {

    /**
     * 默认导入 provider holder 工厂。
     */
    companion object {
        /**
         * 将单个 [provider] 包装为 holder。
         */
        fun of(provider: DefaultImportsProvider): CfirDefaultImportsProviderHolder {
            return Single(provider)
        }
    }

    /**
     * 单个默认导入 provider holder。
     */
    class Single(override val provider: DefaultImportsProvider) : CfirDefaultImportsProviderHolder()

    /**
     * 多个默认导入 provider holder 的组合。
     *
     * @property components 参与组合的 holder 列表。
     */
    class Composed(
        override val components: List<CfirDefaultImportsProviderHolder>
    ) : CfirDefaultImportsProviderHolder(), CfirComposableSessionComponent.Composed<CfirDefaultImportsProviderHolder> {
        /**
         * 组合后的默认导入 provider。
         */
        override val provider: DefaultImportsProvider = DefaultImportsProvider.Composed(components.map { it.provider })
    }

    /**
     * 当前 holder 暴露的默认导入 provider。
     */
    abstract val provider: DefaultImportsProvider

    /**
     * 创建默认导入 provider holder 的组合实例。
     */
    @SessionConfiguration
    override fun createComposed(components: List<CfirDefaultImportsProviderHolder>): Composed {
        return Composed(components)
    }


}

/**
 * 当前 session 注册的默认导入 provider holder。
 */
private val CfirSession.defaultImportsProviderHolder: CfirDefaultImportsProviderHolder by CfirSession.sessionComponentAccessor()

/**
 * 当前 session 使用的默认导入 provider。
 */
val CfirSession.defaultImportsProvider: DefaultImportsProvider get() = defaultImportsProviderHolder.provider
