package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

/**
 * 仓颉特有 extend 声明查询接口。
 *
 * providers 层必须同时暴露：
 * 1. 按目标类型查询 extend。
 * 2. 按 callable 查询 owner extend。
 *
 * 这样 use-site substitution scope 才能像 Kotlin FIR 一样在 scope 层完成
 * “声明复制 + 类型实参替换”，而不是在解析阶段退化成按 symbol 反查。
 */
interface CfirExtendProvider : CfirSessionComponent {
    fun getExtendsForClass(classId: ClassId): List<CfirExtend>

    fun getExtendsInPackage(packageFqName: FqName): List<CfirExtend>

    fun getExtendsForBuiltinType(kind: PrimitiveTypeKind): List<CfirExtend>

    /**
     * 返回 callable 对应的 owner extend。
     *
     * 默认实现返回 `null`，允许不支持 extend 成员索引的 provider 继续工作；
     * 但 source/library 主路径必须提供真实索引，不能靠运行期遍历补洞。
     */
    fun getContainingExtend(symbol: CfirCallableSymbol<*>): CfirExtend? = null

    fun isExtendAccessible(extend: CfirExtend): Boolean = true
}
