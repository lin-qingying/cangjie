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
    fun chooseMaximallySpecificCandidates(
        candidates: Collection<Candidate>,
    ): Set<Candidate> = chooseMaximallySpecificCandidates(candidates.toSet())

    abstract fun chooseMaximallySpecificCandidates(
        candidates: Set<Candidate>,
    ): Set<Candidate>
}

abstract class ConeCallConflictResolverFactory : CfirComposableSessionComponent<ConeCallConflictResolverFactory> {
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

    abstract fun createAdditionalResolvers(session: CfirSession): List< ConeCallConflictResolver>

    object Default : ConeCallConflictResolverFactory() {
        override fun createAdditionalResolvers(session: CfirSession): List< ConeCallConflictResolver> {
            return emptyList()
        }
    }

    class Composed(
        override val components: List<ConeCallConflictResolverFactory>
    ) : ConeCallConflictResolverFactory(),CfirComposableSessionComponent.Composed<ConeCallConflictResolverFactory> {
        override fun createAdditionalResolvers(session: CfirSession): List< ConeCallConflictResolver> {
            return components.flatMap { it.createAdditionalResolvers(session) }
        }
    }

    @SessionConfiguration
    override fun createComposed(components: List<ConeCallConflictResolverFactory>): Composed {
        return Composed(components)
    }
}


val CfirSession.callConflictResolverFactory: ConeCallConflictResolverFactory
        by CfirSession.sessionComponentAccessorWithDefault(ConeCallConflictResolverFactory.Default)
