package org.cangnova.cangjie.cfir.symbols

import org.cangnova.cangjie.cfir.declarations.CfirPrimitiveTypeDeclaration
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

/**
 * 内建 primitive 类型的 class-like 符号。
 *
 * primitive 类型虽然没有源码声明，但在 CFIR 类型系统中仍以 class-like 符号参与 lookup tag、
 * 类型构造和 scope 查询。
 *
 * @property classId primitive 类型的稳定 class id。
 * @property kind primitive 类型种类。
 */
class CfirPrimitiveTypeSymbol(
    override val classId: ClassId,
    val kind: PrimitiveTypeKind,
) : CfirClassLikeSymbol<CfirPrimitiveTypeDeclaration>(classId) {
    /**
     * primitive 类型名称；绑定后以声明名为准，未绑定时从 class id 推导。
     */
    override val name: Name
        get() = if (isBound) cfir.name else super.name

    /**
     * 返回调试用符号文本。
     */
    override fun toString(): String =
        if (isBound) "CfirPrimitiveTypeSymbol(${cfir.name})" else "CfirPrimitiveTypeSymbol(${kind.typeName})"
}
