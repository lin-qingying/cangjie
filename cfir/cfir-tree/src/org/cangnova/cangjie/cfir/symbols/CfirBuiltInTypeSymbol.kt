package org.cangnova.cangjie.cfir.symbols

import org.cangnova.cangjie.cfir.declarations.CfirBuiltInDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirBuiltInTypeKind
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

/**
 * 内建类型声明的 class-like 符号。
 *
 * 对应官方 `AST::BuiltInDecl`：`RawArray` / `VArray` / `CPointer` / `CString` / `CFunc`
 * 在官方是注入 `std.core` 的普通全局声明，因此在 CFIR 中同样以 class-like 符号参与
 * lookup tag、名字解析与 scope 查询，而不另开查询通道。
 *
 * 与 [CfirPrimitiveTypeSymbol] 的分工：primitive 在官方是 lexer 关键字、没有声明，
 * 本仓库为统一 scope 才为其合成 classifier；此处则是官方本就存在的声明的对应物。
 *
 * @property classId 内建声明在 `std.core` 下的稳定 class id。
 * @property kind 内建类型种类。
 */
class CfirBuiltInTypeSymbol(
    /**
     * 内建声明在 `std.core` 下的稳定 class id。
     */
    override val classId: ClassId,
    /**
     * 内建类型的语义种类。
     */
    val kind: CfirBuiltInTypeKind,
) : CfirClassLikeSymbol<CfirBuiltInDeclaration>(classId) {
    /**
     * 内建类型名称；绑定后以声明名为准，未绑定时从 class id 推导。
     */
    override val name: Name
        get() = if (isBound) cfir.name else super.name

    /**
     * 返回调试用符号文本。
     */
    override fun toString(): String =
        if (isBound) "CfirBuiltInTypeSymbol(${cfir.name})" else "CfirBuiltInTypeSymbol(${kind.typeName})"
}
