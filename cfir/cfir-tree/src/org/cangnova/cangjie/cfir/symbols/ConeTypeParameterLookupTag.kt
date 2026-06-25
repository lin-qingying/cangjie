package org.cangnova.cangjie.cfir.symbols

import org.cangnova.cangjie.cfir.types.ConeClassifierLookupTag
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.type.model.TypeParameterMarker


/**
 * 绑定了固定符号（symbol）的 lookup tag 抽象基类。
 *
 * 相对于 [ConeClassifierLookupTag]，此层额外持有一个 [CfirClassifierSymbol]，
 * 使 tag 可以直接访问对应声明的完整符号信息（上界、variance、注解等），
 * 无需再通过符号表二次查找。
 *
 * "固定"意味着 symbol 在 tag 创建后不再变更，适合已完成解析的声明。
 */
abstract class ConeClassifierLookupTagWithFixedSymbol : ConeClassifierLookupTag() {
    /**
     * 该 tag 所对应的分类器符号，泛型参数表示具体符号子类型。
     */
    abstract val symbol: CfirClassifierSymbol<*>
}

/**
 * 泛型类型参数的查找标签。
 *
 * 将"类型参数身份"直接附着在 lookup tag 上，使得
 * `typeConstructor → classifier → upperBounds` 这条访问链路
 * 无需再引入额外的包装层。
 *
 * 同时实现 [TypeParameterMarker]，允许推断引擎将其作为
 * 类型参数直接参与 variance 和上界约束的计算。
 *
 * 使用 `data class` 是因为类型参数的身份完全由 [typeParameterSymbol] 决定，
 * 基于内容的 equals/hashCode 可以安全地用于集合去重和缓存。
 *
 * @param typeParameterSymbol 该类型参数对应的 CFIR 符号，
 *   携带名称、variance、上界等完整声明信息。
 */
data class ConeTypeParameterLookupTag(
    /**
     * 该 lookup tag 绑定的类型参数符号。
     */
    val typeParameterSymbol: CfirTypeParameterSymbol,
) : ConeClassifierLookupTagWithFixedSymbol(), TypeParameterMarker {

    /**
     * 类型参数的名称，直接委托给符号，如 `T`、`E`、`K` 等。
     */
    override val name: Name get() = typeParameterSymbol.name

    /**
     * 直接返回构造时传入的类型参数符号，满足父类的 symbol 契约。
     */
    override val symbol: CfirTypeParameterSymbol
        get() = typeParameterSymbol
}
