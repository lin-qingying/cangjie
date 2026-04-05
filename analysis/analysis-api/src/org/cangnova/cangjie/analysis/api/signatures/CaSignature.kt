package org.cangnova.cangjie.analysis.api.signatures

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.Name

/**
 * 值参数签名的公开语义视图。
 *
 * 该结构稳定表达一个参数在声明层面的信息：
 * - 参数名
 * - 语义类型
 * - 源码中显式写出的类型文本
 * - 参数自身的注解
 */
interface CaValueParameterSignature : CaLifetimeOwner {
    /**
     * 参数名。
     */
    val name: Name?

    /**
     * 参数的语义类型。
     *
     * 当当前 Analysis API 无法稳定恢复参数类型对象时返回 `null`，
     * 但 [typeText] 仍可保留源码层信息。
     */
    val type: CaType?

    /**
     * 参数类型在源码中的显式文本。
     */
    val typeText: String?

    /**
     * 参数上直接声明的注解。
     */
    val annotations: List<CaAnnotation>
}

/**
 * 可调用声明签名的公开语义视图。
 *
 * Analysis API 不应该要求上层工具反复从 PSI 手工拼接签名文本，
 * 因此这里以结构化模型统一暴露 callable 的声明级签名信息。
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
     * 返回类型在源码中的显式文本。
     */
    val returnTypeText: String?

    /**
     * 声明本身直接携带的注解。
     */
    val annotations: List<CaAnnotation>
}
