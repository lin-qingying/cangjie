package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.Decl
import PackageFormat.DeclKind

/**
 * 集中定义 CJO 跨包 FullId 的声明引用键协议。
 *
 * 键由目标包的原始声明拥有：优先取官方 exportId，仅在该字段缺失时使用 identifier。
 */
internal object CjoFullIdReferenceKey {
    /** 返回官方 CJO producer 写入的跨包引用键。 */
    fun fromDeclaration(declaration: Decl): String? =
        declarationKey(declaration.kind, declaration.exportId, declaration.identifier)

    /** 与 FlatBuffers 声明使用同一规则读取 producer 的目标声明。 */
    fun fromDeclarationMetadata(declaration: CjoPackageDeclaration): String? =
        declarationKey(declaration.kind, declaration.exportId, declaration.identifier)

    private fun declarationKey(kind: UShort, exportId: String?, identifier: String?): String? {
        if (!exportId.isNullOrBlank()) return exportId
        // Native LoadGenericParamDecl 只向跨包映射登记具有实际 exportId 的类型参数。
        if (kind == DeclKind.GenericParamDecl) return null
        return fromIdentifier(identifier)
    }

    /** 声明没有 exportId 时返回 identifier 回退键。 */
    fun fromIdentifier(identifier: String?): String? = identifier?.takeIf(String::isNotBlank)

    /** 在目标包索引中查找前验证 FullId 携带的原始键。 */
    fun fromWireValue(value: String?): String? = value?.takeIf(String::isNotBlank)
}
