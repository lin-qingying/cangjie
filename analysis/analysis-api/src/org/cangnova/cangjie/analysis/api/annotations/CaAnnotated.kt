package org.cangnova.cangjie.analysis.api.annotations

/**
 * 拥有注解的实体协议。
 *
 * 类型、符号、签名等可被注解修饰的元素统一实现该接口,
 * 通过 [annotations] 暴露稳定的注解列表视图。
 *
 * 对齐 Kotlin Analysis API 的 `KaAnnotated`。
 */
interface CaAnnotated {
    /** 当前实体上的注解列表。 */
    val annotations: CaAnnotationList
}
