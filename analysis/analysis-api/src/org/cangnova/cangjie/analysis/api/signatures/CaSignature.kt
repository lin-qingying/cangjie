package org.cangnova.cangjie.analysis.api.signatures

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.Name

/**
 * 值参数签名的公开语义快照。
 *
 * 这里只保留稳定语义信息，不再缓存源码文本。
 * 需要源码级呈现时，应由 renderer 直接基于 PSI 或 source snapshot 取值。
 */
interface CaValueParameterSignature : CaLifetimeOwner {
    /**
     * 参数名。
     */
    val name: Name?

    /**
     * 参数的语义类型。
     *
     * 当后端当前无法稳定恢复该参数类型时返回 `null`，
     * 但不会再退化为原始文本字段。
     */
    val type: CaType?

    /**
     * 参数上直接声明的注解。
     */
    val annotations: List<CaAnnotation>
}

/**
 * callable 声明签名的公开语义快照。
 *
 * 该结构只表达稳定、可比较、可替换的语义内容：
 * 声明名、类型参数、值参数、返回类型和注解。
 */
interface CaSignature : CaLifetimeOwner {
    /**
     * 声明名。
     */
    val declarationName: Name?

    /**
     * 类型参数名列表，顺序与声明顺序一致。
     */
    val typeParameters: List<Name>

    /**
     * 值参数签名列表，顺序与声明顺序一致。
     */
    val valueParameters: List<CaValueParameterSignature>

    /**
     * 返回类型的语义对象。
     */
    val returnType: CaType?

    /**
     * 声明本身直接携带的注解。
     */
    val annotations: List<CaAnnotation>
}
