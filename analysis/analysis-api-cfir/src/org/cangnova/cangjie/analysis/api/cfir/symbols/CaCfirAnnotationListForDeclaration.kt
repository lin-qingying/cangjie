package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.cfir.components.annotationClassIdOrNull
import org.cangnova.cangjie.analysis.api.cfir.components.asPublicAnnotation
import org.cangnova.cangjie.analysis.api.impl.base.annotations.CaBaseEmptyAnnotationList
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase

internal class CaCfirAnnotationListForDeclaration private constructor(
    private val firSymbol: CfirBasedSymbol<*>,
    private val builder: CaSymbolByCfirBuilder,
) : AbstractList<CaAnnotation>(), CaAnnotationList {
    /**
     * 对齐 Kotlin FIR “注解列表负责提供稳定的 classId / 参数映射” 的职责边界：
     * 仓颉当前还没有独立的 `ANNOTATION_ARGUMENTS` phase，因此这里统一把声明推进到
     * `BODY_RESOLVE`，保证：
     * 1. 自定义注解的 annotation typeRef 已绑定到稳定的 ClassId；
     * 2. 具名注解参数已经回写为 resolved argument mapping。
     */
    private val resolvedAnnotationCalls: List<CfirAnnotationCall> by lazy(LazyThreadSafetyMode.NONE) {
        firSymbol.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
        firSymbol.annotations.filterIsInstance<CfirAnnotationCall>()
    }

    private val backingAnnotations: List<CaAnnotation>
        get() = withValidityAssertion {
            resolvedAnnotationCalls.map { annotation -> annotation.asPublicAnnotation(builder, builder.token) }
        }

    override val token
        get() = builder.token

    override val size: Int
        get() = withValidityAssertion { backingAnnotations.size }

    override fun get(index: Int): CaAnnotation = withValidityAssertion {
        backingAnnotations[index]
    }

    override fun contains(classId: org.cangnova.cangjie.name.ClassId): Boolean = withValidityAssertion {
        resolvedAnnotationCalls.any { annotation -> annotation.typeRef.annotationClassIdOrNull() == classId }
    }

    override fun get(classId: org.cangnova.cangjie.name.ClassId): List<CaAnnotation> = withValidityAssertion {
        resolvedAnnotationCalls
            .filter { annotation -> annotation.typeRef.annotationClassIdOrNull() == classId }
            .map { annotation -> annotation.asPublicAnnotation(builder, builder.token) }
    }

    override val classIds: Collection<org.cangnova.cangjie.name.ClassId>
        get() = withValidityAssertion { resolvedAnnotationCalls.mapNotNull { annotation -> annotation.typeRef.annotationClassIdOrNull() } }

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
