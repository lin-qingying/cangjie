package org.cangnova.cangjie.analysis.api.signatures

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.Name

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
