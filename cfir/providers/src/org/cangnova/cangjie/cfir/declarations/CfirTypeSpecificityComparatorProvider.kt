package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.SessionConfiguration
import org.cangnova.cangjie.cfir.session.CfirComposableSessionComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.resolve.calls.results.TypeSpecificityComparator

/**
 * 为 session 提供调用候选类型特异性比较器的可组合组件。
 */
sealed class CfirTypeSpecificityComparatorProvider :
    CfirComposableSessionComponent<CfirTypeSpecificityComparatorProvider> {
    /**
     * 当前 provider 暴露的类型特异性比较器。
     */
    abstract val typeSpecificityComparator: TypeSpecificityComparator

    /**
     * 单一比较器 provider。
     */
    class Simple(override val typeSpecificityComparator: TypeSpecificityComparator) : CfirTypeSpecificityComparatorProvider()

    /**
     * 多个比较器 provider 的组合实现。
     *
     * @property components 参与组合的 provider 列表。
     */
    class Composed(
        /**
         * 参与当前 session 组合的所有类型特异性比较器 provider。
         */
        override val components: List<CfirTypeSpecificityComparatorProvider>,
    ) : CfirTypeSpecificityComparatorProvider(), CfirComposableSessionComponent.Composed<CfirTypeSpecificityComparatorProvider> {
        /**
         * 由所有子 provider 比较器组合出的最终比较器。
         */
        override val typeSpecificityComparator: TypeSpecificityComparator =
            TypeSpecificityComparator.Composed(components.map { it.typeSpecificityComparator })
    }

    /**
     * 创建可组合 session component 的组合实例。
     */
    @SessionConfiguration
    override fun createComposed(components: List<CfirTypeSpecificityComparatorProvider>): Composed {
        return Composed(components)
    }

    /**
     * 类型特异性比较器 provider 工厂。
     */
    companion object {
        /**
         * 将单个 [typeSpecificityComparator] 包装为 provider。
         */
        fun of(typeSpecificityComparator: TypeSpecificityComparator): CfirTypeSpecificityComparatorProvider {
            return Simple(typeSpecificityComparator)
        }
    }
}

/**
 * 当前 session 中可选注册的类型特异性比较器 provider。
 */
val CfirSession.typeSpecificityComparatorProvider: CfirTypeSpecificityComparatorProvider? by CfirSession.nullableSessionComponentAccessor()
