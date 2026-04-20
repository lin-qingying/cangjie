package org.cangnova.cangjie.analysis.api.components

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.types.CaSubstitutor
import org.cangnova.cangjie.analysis.api.types.CaType

/**
 * 公开替换器构造入口。
 *
 * 这里直接对齐 Kotlin `KaSubstitutorProvider` 的公开语义：
 * 1. public API 只接受“类型参数符号 -> 类型实参”的结构化映射；
 * 2. 不把内部 map-backed / chained / cache-key 等实现细节暴露给调用方；
 * 3. builder 负责聚合 substitutions，provider 负责把公开映射解释成真正的 substitutor。
 */
interface CaSubstitutorProvider : CaLifetimeOwner {
    fun createSubstitutor(mappings: Map<CaTypeParameterSymbol, CaType>): CaSubstitutor
}

/**
 * 公开 substitutor builder。
 *
 * 该 builder 只承载“要把哪些类型参数替换成哪些类型”这一声明式输入，
 * 具体替换策略仍由 session 组件解释。
 */
@OptIn(ExperimentalContracts::class)
class CaSubstitutorBuilder(
    override val token: CaLifetimeToken,
) : CaLifetimeOwner {
    private val backingMappings = linkedMapOf<CaTypeParameterSymbol, CaType>()

    /**
     * 当前已经累积的 substitutions。
     */
    val mappings: Map<CaTypeParameterSymbol, CaType>
        get() = withValidityAssertion { backingMappings.toMap() }

    /**
     * 追加或覆盖一条类型参数替换规则。
     */
    fun substitution(typeParameter: CaTypeParameterSymbol, type: CaType): Unit = withValidityAssertion {
        backingMappings[typeParameter] = type
    }

    /**
     * 批量追加或覆盖类型参数替换规则。
     */
    fun substitutions(substitutions: Map<CaTypeParameterSymbol, CaType>): Unit = withValidityAssertion {
        backingMappings += substitutions
    }
}

/**
 * 基于 builder DSL 构造公开 substitutor。
 */
@OptIn(ExperimentalContracts::class)
@JvmName("buildSubstitutorExtension")
inline fun CaSession.buildSubstitutor(
    build: CaSubstitutorBuilder.() -> Unit,
): CaSubstitutor {
    contract {
        callsInPlace(build, InvocationKind.EXACTLY_ONCE)
    }
    return createSubstitutor(CaSubstitutorBuilder(token).apply(build).mappings)
}

context(session: CaSession)
@OptIn(ExperimentalContracts::class)
inline fun buildSubstitutor(
    build: CaSubstitutorBuilder.() -> Unit,
): CaSubstitutor {
    contract {
        callsInPlace(build, InvocationKind.EXACTLY_ONCE)
    }
    return session.buildSubstitutor(build)
}

context(session: CaSession)
fun createSubstitutor(mappings: Map<CaTypeParameterSymbol, CaType>): CaSubstitutor {
    return with(session) {
        createSubstitutor(mappings)
    }
}
