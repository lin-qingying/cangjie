package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.SessionConfiguration
import org.cangnova.cangjie.cfir.session.CfirComposableSessionComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.resolve.calls.results.TypeSpecificityComparator

sealed class CfirTypeSpecificityComparatorProvider :
    CfirComposableSessionComponent<CfirTypeSpecificityComparatorProvider> {
    abstract val typeSpecificityComparator: TypeSpecificityComparator

    class Simple(override val typeSpecificityComparator: TypeSpecificityComparator) : CfirTypeSpecificityComparatorProvider()

    class Composed(
        override val components: List<CfirTypeSpecificityComparatorProvider>,
    ) : CfirTypeSpecificityComparatorProvider(), CfirComposableSessionComponent.Composed<CfirTypeSpecificityComparatorProvider> {
        override val typeSpecificityComparator: TypeSpecificityComparator =
            TypeSpecificityComparator.Composed(components.map { it.typeSpecificityComparator })
    }

    @SessionConfiguration
    override fun createComposed(components: List<CfirTypeSpecificityComparatorProvider>): Composed {
        return Composed(components)
    }

    companion object {
        fun of(typeSpecificityComparator: TypeSpecificityComparator): CfirTypeSpecificityComparatorProvider {
            return Simple(typeSpecificityComparator)
        }
    }
}

val CfirSession.typeSpecificityComparatorProvider: CfirTypeSpecificityComparatorProvider? by CfirSession.nullableSessionComponentAccessor()
