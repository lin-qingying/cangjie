package org.cangnova.cangjie.cfir.symbols

import org.cangnova.cangjie.cfir.types.ConeAttributes
import org.cangnova.cangjie.cfir.types.ConeLookupTagBasedType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection

/**
 * 类型参数类型的抽象基类，表示源码中直接使用类型参数名的类型，如 `T`、`E`。
 *
 * 与 [org.cangnova.cangjie.cfir.types.ConeClassLikeType] 等具名类型不同，类型参数类型在实例化前没有确定的底层类，
 * 其上界约束通过 [ConeTypeParameterLookupTag] → [CfirTypeParameterSymbol] 链路访问。
 *
 * 类型参数类型**没有类型实参**（自身不是泛型容器），因此子类统一返回空列表。
 */
abstract class ConeTypeParameterType : ConeLookupTagBasedType() {
    /**
     * lookup tag 收窄为 [ConeTypeParameterLookupTag]，可直接访问类型参数符号。
     */
    abstract override val lookupTag: ConeTypeParameterLookupTag
}

/**
 * [ConeTypeParameterType] 的标准实现，用于绝大多数场景。
 *
 * 构造时只需提供 lookup tag 和可选的附加属性，
 * 类型实参固定为空列表（类型参数本身不携带实参）。
 *
 * @param lookupTag 指向对应类型参数符号的 tag。
 * @param attributes 类型附加属性，默认为空。
 */
class ConeTypeParameterTypeImpl(
    /**
     * 指向类型参数符号的 lookup tag。
     */
    override val lookupTag: ConeTypeParameterLookupTag,
    /**
     * 类型参数类型携带的类型属性。
     */
    override val attributes: ConeAttributes = ConeAttributes.Empty,
) : ConeTypeParameterType() {

    /**
     * 类型参数不是泛型容器，始终返回空列表。
     */
    override val typeArguments: List<ConeTypeProjection>
        get() = emptyList()
}
