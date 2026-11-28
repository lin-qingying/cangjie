package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.Decl

/**
 * 集中定义 CJO 跨包 FullId 的声明引用键协议。
 *
 * 键由目标包的原始声明拥有：优先取官方 exportId，仅在该字段缺失时使用 identifier。
 */
internal object CjoFullIdReferenceKey {
    /** 返回官方 CJO producer 写入的跨包引用键。 */
    fun fromDeclaration(declaration: Decl): String? =
        declaration.exportId?.takeIf(String::isNotBlank)
            ?: fromIdentifier(declaration.identifier)

    /** 与 FlatBuffers 声明使用同一规则读取 producer 的目标声明。 */
    fun fromDeclarationMetadata(declaration: CjoPackageDeclaration): String? =
        declaration.exportId?.takeIf(String::isNotBlank)
            ?: fromIdentifier(declaration.identifier)

    /** 声明没有 exportId 时返回 identifier 回退键。 */
    fun fromIdentifier(identifier: String?): String? = identifier?.takeIf(String::isNotBlank)

    /** 在目标包索引中查找前验证 FullId 携带的原始键。 */
    fun fromWireValue(value: String?): String? = value?.takeIf(String::isNotBlank)
}
