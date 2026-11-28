package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.renderer.CfirRenderer
import org.cangnova.cangjie.cfir.types.CfirTypeRef

/**
 * `extend` 声明的稳定结构签名。
 *
 * 库 `extend` 没有源码文件与 ClassId 身份，跨 session 的符号实例也不保证同一个对象；
 * 「接收者 + 父类型」的结构签名是唯一可跨 provider/session 对齐的键。该键同时用于：
 * - `extend` 声明的 PSI 回链（在反编译 PSI 中定位同名结构）；
 * - extend provider 的快速索引（避免每次线性扫描全部库 extend）。
 */
fun CfirExtend.extensionSignatureKey(): String {
    val receiver = cfirExtendTypeName(extendedTypeRef)
    val superTypes = superTypeRefs
        .map(::cfirExtendTypeName)
        .filter(String::isNotEmpty)
        .sorted()
        .joinToString(",")
    return "$receiver<$superTypes>"
}

/**
 * 把 CFIR 类型引用渲染成用于 extend 结构匹配的短类型名。
 */
private fun cfirExtendTypeName(typeRef: CfirTypeRef): String =
    normalizeCfirExtendTypeText(CfirRenderer.withReadability().renderElementAsString(typeRef))

/**
 * 规范化 extend 类型文本：去掉可读性渲染标记、包前缀与泛型实参。
 *
 * `?T` 与 `Option<T>` 在仓颉里是同一语义，统一归一为 `Option`，保证调用点文本
 * 与反编译 PSI 文本可以比较。
 */
fun normalizeCfirExtendTypeText(text: String): String {
    val trimmed = text.removePrefix("R|").removeSuffix("|").trim()
    if (trimmed.startsWith("?")) return "Option"
    return trimmed.substringAfterLast('.').substringBefore('<').trim()
}
