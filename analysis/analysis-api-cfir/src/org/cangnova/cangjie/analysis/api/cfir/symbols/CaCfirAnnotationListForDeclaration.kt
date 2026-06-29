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

/**
 * 基于 CFIR 声明符号的公开注解列表。
 */
internal class CaCfirAnnotationListForDeclaration private constructor(
    /**
     * 提供 annotation call 的底层 CFIR 符号。
     */
    private val firSymbol: CfirBasedSymbol<*>,
    /**
     * 将 CFIR 注解转换为公开注解对象的符号构建器。
     */
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

    /**
     * 当前生命周期内按需构建的公开注解对象列表。
     */
    private val backingAnnotations: List<CaAnnotation>
        get() = withValidityAssertion {
            resolvedAnnotationCalls.map { annotation -> annotation.asPublicAnnotation(builder, builder.token) }
        }

    /**
     * 注解列表绑定的生命周期 token。
     */
    override val token
        get() = builder.token

    /**
     * 注解列表中的注解数量。
     */
    override val size: Int
        get() = withValidityAssertion { backingAnnotations.size }

    /**
     * 按下标取得公开注解对象。
     */
    override fun get(index: Int): CaAnnotation = withValidityAssertion {
        backingAnnotations[index]
    }

    /**
     * 判断声明上是否存在指定 classId 的注解。
     */
    override fun contains(classId: org.cangnova.cangjie.name.ClassId): Boolean = withValidityAssertion {
        resolvedAnnotationCalls.any { annotation -> annotation.typeRef.annotationClassIdOrNull() == classId }
    }

    /**
     * 返回声明上所有匹配指定 classId 的注解。
     */
    override fun get(classId: org.cangnova.cangjie.name.ClassId): List<CaAnnotation> = withValidityAssertion {
        resolvedAnnotationCalls
            .filter { annotation -> annotation.typeRef.annotationClassIdOrNull() == classId }
            .map { annotation -> annotation.asPublicAnnotation(builder, builder.token) }
    }

    /**
     * 返回声明上所有可解析注解的 classId。
     */
    override val classIds: Collection<org.cangnova.cangjie.name.ClassId>
        get() = withValidityAssertion { resolvedAnnotationCalls.mapNotNull { annotation -> annotation.typeRef.annotationClassIdOrNull() } }

    companion object {
        /**
         * 按 CFIR 符号注解数量创建空列表或 CFIR-backed 注解列表。
         */
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
