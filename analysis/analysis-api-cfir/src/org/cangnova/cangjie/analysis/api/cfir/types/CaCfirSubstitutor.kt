package org.cangnova.cangjie.analysis.api.cfir.types

import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.cfir.utils.asCaType
import org.cangnova.cangjie.analysis.api.impl.base.CaMapBackedSubstitutor
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.types.CaSubstitutor
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.types.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor

/**
 * CFIR substitutor 的统一抽象基类。
 *
 * 对齐 Kotlin `AbstractKaFirSubstitutor` 的职责边界：
 * 1. `CaSubstitutor` 的公开语义只关心替换结果；
 * 2. 底层 `ConeSubstitutor` 与类型构造细节留在 CFIR；
 * 3. 具体映射是否可还原，由实现层 marker 决定。
 */
internal abstract class AbstractCaCfirSubstitutor<T : ConeSubstitutor>(
    internal val substitutor: T,
    protected val builder: CaSymbolByCfirBuilder,
) : CaSubstitutor {
    final override val token: CaLifetimeToken
        get() = builder.analysisSession.token

    final override fun substituteOrNull(type: CaType): CaType? {
        val cfirType = type as? CaCfirType
            ?: error("仅支持对 CFIR Analysis API 类型执行替换：${type::class.simpleName}")
        return substitutor.substituteOrNull(cfirType.coneType)?.asCaType(builder.analysisSession)
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
    internal val mappings: List<Pair<CaTypeParameterSymbol, CaType>>,
    substitutor: CfirTypeSubstitutorByMap,
    builder: CaSymbolByCfirBuilder,
) : AbstractCaCfirSubstitutor<CfirTypeSubstitutorByMap>(substitutor, builder), CaMapBackedSubstitutor {
    override fun getAsMap(): Map<CaTypeParameterSymbol, CaType> = mappings.toMap(linkedMapOf())
}

/**
 * CFIR substitutor 缓存键。
 *
 * 相同类型映射必须在同一 session 内复用同一个公开 substitutor 实例。
 */
internal data class CaCfirSubstitutorCacheKey(
    val mappings: List<Pair<org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol, ConeCangJieType>>,
)
