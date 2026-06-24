package org.cangnova.cangjie.cfir.resolve.calls.overloads

import org.cangnova.cangjie.cfir.SessionConfiguration
import org.cangnova.cangjie.cfir.declarations.typeSpecificityComparatorProvider
import org.cangnova.cangjie.cfir.resolve.BodyResolveComponents
import org.cangnova.cangjie.cfir.resolve.calls.candidate.Candidate
import org.cangnova.cangjie.cfir.resolve.inference.InferenceComponents
import org.cangnova.cangjie.cfir.session.CfirComposableSessionComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.resolve.calls.results.TypeSpecificityComparator

/**
 * 调用冲突解析器的抽象基类。
 * 它从一组已经通过验证的候选中选出“最特定”的候选集合。
 * 理想情况下返回单个候选；若返回多个，则表示仍然存在歧义。
 * 对齐 K2 `ConeCallConflictResolver`。
 */
abstract class ConeCallConflictResolver {
    /**
     * 从候选集合中选择最特定候选，并把输入集合规范化为 set。
     */
    fun chooseMaximallySpecificCandidates(
        candidates: Collection<Candidate>,
    ): Set<Candidate> = chooseMaximallySpecificCandidates(candidates.toSet())

    /**
     * 从已经完成基本检查的候选集合中选择最特定候选集合。
     */
    abstract fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
    ): Set<Candidate>
}

/**
 * 调用冲突解析器工厂。
 *
 * 会话组件通过组合工厂扩展冲突解析策略，同时保持基础重载冲突解析器位于固定顺序中。
 */
abstract class ConeCallConflictResolverFactory : CfirComposableSessionComponent<ConeCallConflictResolverFactory> {
    /**
     * 基于推断组件和 body resolve 组件构造当前会话的冲突解析器。
     */
    fun create(
        components: InferenceComponents,
        transformerComponents: BodyResolveComponents
    ):  ConeCallConflictResolver {
        val session = components.session
        val specificityComparator = session.typeSpecificityComparatorProvider?.typeSpecificityComparator
            ?: TypeSpecificityComparator.NONE
        // NB: Adding new resolvers is strongly discouraged because the results are order-dependent.
        return ConeCompositeConflictResolver(
//            ConeEquivalentCallConflictResolver(session),
            *createAdditionalResolvers(session).toTypedArray(),
//            ConeIntegerOperatorConflictResolver,
            ConeOverloadConflictResolver(specificityComparator, components, transformerComponents),
        )
    }

    /**
     * 返回插件或平台追加的冲突解析器。
     */
    abstract fun createAdditionalResolvers(session: CfirSession): List< ConeCallConflictResolver>

    /**
     * 默认工厂，不追加额外冲突解析器。
     */
    object Default : ConeCallConflictResolverFactory() {
        /**
         * 默认实现没有额外解析器。
         */
        override fun createAdditionalResolvers(session: CfirSession): List< ConeCallConflictResolver> {
            return emptyList()
        }
    }

    /**
     * 由多个工厂组合形成的冲突解析器工厂。
     */
    class Composed(
        /**
         * 参与组合的工厂列表。
         */
        override val components: List<ConeCallConflictResolverFactory>
    ) : ConeCallConflictResolverFactory(),CfirComposableSessionComponent.Composed<ConeCallConflictResolverFactory> {
        /**
         * 按组合顺序合并所有额外解析器。
         */
        override fun createAdditionalResolvers(session: CfirSession): List< ConeCallConflictResolver> {
            return components.flatMap { it.createAdditionalResolvers(session) }
        }
    }

    /**
     * 将多个会话组件实例组合为一个工厂实例。
     */
    @SessionConfiguration
    override fun createComposed(components: List<ConeCallConflictResolverFactory>): Composed {
        return Composed(components)
    }
}


/**
 * 当前会话注册的调用冲突解析器工厂。
 */
val CfirSession.callConflictResolverFactory: ConeCallConflictResolverFactory
        by CfirSession.sessionComponentAccessorWithDefault(ConeCallConflictResolverFactory.Default)
