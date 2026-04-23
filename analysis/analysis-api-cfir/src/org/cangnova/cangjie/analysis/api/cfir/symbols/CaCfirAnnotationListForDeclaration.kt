package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.impl.base.annotations.CaBaseEmptyAnnotationList
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol

internal class CaCfirAnnotationListForDeclaration private constructor(
    private val firSymbol: CfirBasedSymbol<*>,
    private val builder: CaSymbolByCfirBuilder,
) : AbstractList<CaAnnotation>(), CaAnnotationList {
    private val backingAnnotations: List<CaAnnotation>
        get() = withValidityAssertion {
            emptyList()
        }

    override val token
        get() = builder.token

    override val size: Int
        get() = withValidityAssertion { backingAnnotations.size }

    override fun get(index: Int): CaAnnotation = withValidityAssertion {
        backingAnnotations[index]
    }

    override fun contains(classId: org.cangnova.cangjie.name.ClassId): Boolean = withValidityAssertion {
        backingAnnotations.any { annotation -> annotation.classId == classId }
    }

    override fun get(classId: org.cangnova.cangjie.name.ClassId): List<CaAnnotation> = withValidityAssertion {
        backingAnnotations.filter { annotation -> annotation.classId == classId }
    }

    override val classIds: Collection<org.cangnova.cangjie.name.ClassId>
        get() = withValidityAssertion { backingAnnotations.mapNotNull { annotation -> annotation.classId } }

    companion object {
        fun create(cfirSymbol: CfirBasedSymbol<*>, builder: CaSymbolByCfirBuilder): CaAnnotationList {
            return when {

                cfirSymbol.annotations.isEmpty() ->
                    CaBaseEmptyAnnotationList(builder.token)
                else ->
                    CaCfirAnnotationListForDeclaration(cfirSymbol, builder)
            }
        }
    }
}
