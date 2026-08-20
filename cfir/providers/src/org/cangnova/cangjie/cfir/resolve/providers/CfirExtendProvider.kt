package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
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
    /**
     * 返回与规范化目标 key 匹配的所有 extend 声明。
     */
    fun getExtendsForTarget(targetKey: CfirExtendTargetKey): List<CfirExtend>

    /**
     * 返回显式扩展 [classId] 的所有 extend 声明。
     */
    fun getExtendsForClass(classId: ClassId): List<CfirExtend>

    /**
     * 返回声明在 [packageFqName] 包内的 extend 集合。
     */
    fun getExtendsInPackage(packageFqName: FqName): List<CfirExtend>

    /**
     * 返回扩展 builtin primitive 类型 [kind] 的 extend 声明。
     */
    fun getExtendsForBuiltinType(kind: PrimitiveTypeKind): List<CfirExtend>

    /**
     * 返回 callable 对应的 owner extend。
     *
     * 默认实现返回 `null`，允许不支持 extend 成员索引的 provider 继续工作；
     * 但 source/library 主路径必须提供真实索引，不能靠运行期遍历补洞。
     */
    fun getContainingExtend(symbol: CfirCallableSymbol<*>): CfirExtend? = null

    /**
     * 返回 extend 声明所在包。
     *
     * use-site scope 需要用它模拟 CJO 导出面：跨包时 private extend 成员
     * 不应进入候选集合。
     */
    fun getPackageFqName(extend: CfirExtend): FqName? = null

    /**
     * 返回 extend 的声明文件。
     *
     * 这是 provider 的结构索引事实；它不代表调用方的 use-site，也不执行可见性判断。
     * 反序列化声明没有可复用的源码文件时返回 `null`。
     */
    fun getContainingFile(extend: CfirExtend): CfirFile? = null

}
