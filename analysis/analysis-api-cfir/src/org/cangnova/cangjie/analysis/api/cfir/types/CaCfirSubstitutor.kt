package org.cangnova.cangjie.analysis.api.cfir.types

import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.impl.base.CaMapBackedSubstitutor
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.types.CaSubstitutor
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutorByMap

/**
 * CFIR substitutor 的统一抽象基类。
 *
 * 对齐 Kotlin `AbstractKaFirSubstitutor` 的职责边界：
 * 1. `CaSubstitutor` 的公开语义只关心替换结果；
 * 2. 底层 `ConeSubstitutor` 与类型构造细节留在 CFIR；
 * 3. 具体映射是否可还原，由实现层 marker 决定。
 */
internal abstract class AbstractCaCfirSubstitutor<T : ConeSubstitutor>(
    /**
     * 底层 CFIR 替换器。
     */
    internal val substitutor: T,
    /**
     * 类型替换结果转为公开类型时使用的 CFIR builder。
     */
    protected val builder: CaSymbolByCfirBuilder,
) : CaSubstitutor {
    /**
     * 替换器所属会话的生命周期令牌。
     */
    final override val token: CaLifetimeToken
        get() = builder.analysisSession.token

    /**
     * 尝试替换公开类型，并在底层替换器无结果时返回 null。
     */
    final override fun substituteOrNull(type: CaType): CaType? = withValidityAssertion {
        require(type is CaCfirType)
        return substitutor.substituteOrNull(type.coneType)?.let { builder.typeBuilder.buildType(it) }
    }
}

/**
 * 通用 CFIR substitutor。
 *
 * 当底层替换器不暴露稳定 map 语义时，公开层只暴露“可替换”能力。
 */
internal class CaCfirGenericSubstitutor(
    substitutor: ConeSubstitutor,
    builder: CaSymbolByCfirBuilder,
) : AbstractCaCfirSubstitutor<ConeSubstitutor>(substitutor, builder)

/**
 * 可还原成“类型参数符号 -> 类型”映射的 CFIR substitutor。
 */
internal class CaCfirMapBackedSubstitutor(
    substitutor: ConeSubstitutorByMap,
    builder: CaSymbolByCfirBuilder,
) : AbstractCaCfirSubstitutor<ConeSubstitutorByMap>(substitutor, builder), CaMapBackedSubstitutor {
    /**
     * 返回保持插入顺序的公开替换映射。
     */
    override fun getAsMap(): Map<CaTypeParameterSymbol, CaType> = withValidityAssertion {
        val result = mutableMapOf<CaTypeParameterSymbol, CaType>()
        for ((typeParameter, type) in substitutor.substitution) {
            val typeParameterSymbol = builder.classifierBuilder.buildTypeParameterSymbol(typeParameter)
            result[typeParameterSymbol] = builder.typeBuilder.buildType(type)
        }
        return result
    }
}
