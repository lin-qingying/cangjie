package org.cangnova.cangjie.cfir.declarations

/**
 * CFIR 中 visibility 与 modality 的来源显式性。
 *
 * 源码路径即使知道修饰符被省略，也记录为 `IMPLICIT`；二进制元数据未携带该事实时记录为
 * `UNKNOWN`，避免把“确定省略”与“无法得知”混为一谈。
 */
enum class CfirModifierExplicitness {
    /** 源码中明确写出了对应修饰符。 */
    EXPLICIT,

    /** 已知源码省略了对应修饰符。 */
    IMPLICIT,

    /** 当前输入不包含足以判断来源显式性的事实。 */
    UNKNOWN,
}
