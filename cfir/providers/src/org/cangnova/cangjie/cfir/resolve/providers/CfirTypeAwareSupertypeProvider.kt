package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

/**
 * 实例化父类型边的声明来源。
 *
 * 来源属于成员图身份，而不是类型身份。同一个接口类型可以由声明继承、多个 extend，
 * 或 superclass 链上的 extend 分别贡献；类型系统可以把它们投影成同一个类型，成员 scope
 * 必须保留这些独立边，才能正确计算 override、shadow 与访问控制。
 */
sealed interface CfirInstantiatedSupertypeOrigin {
    /** class-like 声明头中直接写出的父类型。 */
    data class Declared(
        /** 声明树中的原始父类型引用，用作稳定边身份。 */
        val sourceTypeRef: CfirTypeRef,
    ) : CfirInstantiatedSupertypeOrigin

    /** 官方为没有 concrete superclass 的 class 补入的隐式 Object 父边。 */
    data class ImplicitObject(
        /** 产生该隐式边的声明 owner。 */
        val ownerClassId: ClassId,
    ) : CfirInstantiatedSupertypeOrigin

    /**
     * extend 注入的接口父边。
     *
     * @property predicateVisible 该边是否参与子类型谓词视图。
     * 官方 `TypeManager::HasExtendInterfaceTyHelper` 为赋值/子类型判定构建映射时，
     * 只把 extendedType 实参中"直接的类型参数"纳入代换；若接口类型中仍残留无法由
     * 直接映射实例化的 extend 类型形参（如 `extend<X> A<B<X>> <: I<X>` 的 `X`），
     * 该边在谓词视图下不成立。成员查找、可达性遍历等结构化消费方不受此标记影响。
     */
    data class Extend(
        /** 真正贡献接口实现的 extend 声明。 */
        val sourceExtend: CfirExtend,
        /** extend 声明自身所属包；在父边建立时由声明 session 固化。 */
        val declarationPackage: FqName,
        /** extend 声明中写出的原始接口类型引用。 */
        val sourceTypeRef: CfirTypeRef,
        /** 从当前 receiver 的 superclass 链传播到 source extend 的声明父边路径。 */
        val propagationPath: List<CfirTypeRef> = emptyList(),
        /** 见上；默认 true 以保持非泛型/直接形状 extend 的既有行为。 */
        val predicateVisible: Boolean = true,
    ) : CfirInstantiatedSupertypeOrigin
}

/**
 * 面向成员图的实例化直接父边。
 *
 * [type] 是当前具体 receiver 上实际使用的父类型；[provenanceType] 是进入本次具体实例化
 * 之前的父类型形态，用于区分 `I<X>` 与 `I<Y>` 等独立继承输入；[origin] 保留真实声明边。
 */
data class CfirInstantiatedSupertypeDescriptor(
    val type: ConeCangJieType,
    val provenanceType: ConeCangJieType = type,
    val origin: CfirInstantiatedSupertypeOrigin,
)

/**
 * 面向具体 use-site 类型的父类型提供器。
 *
 * 和仅按 [org.cangnova.cangjie.name.ClassId] 查询的 [CfirDirectSupertypeProvider] 不同，
 * 这里返回的是“已经按当前具体类型实参完成实例化后的父类型”。同一计算同时提供：
 *
 * 1. 保留声明来源的 descriptor 视图，供成员 scope、override 和 extend 导出检查使用；
 * 2. 只保留类型的去重投影视图，供子类型关系等纯类型系统消费者使用。
 */
interface CfirTypeAwareSupertypeProvider : CfirSessionComponent {
    /**
     * 返回 [type] 的实例化直接父边，保留每条声明/extend 来源。
     *
     * 禁止按父类型或 ClassId 去重；同类型不同来源是成员图中的独立输入。
     */
    fun getDirectSupertypeDescriptors(type: ConeCangJieType): List<CfirInstantiatedSupertypeDescriptor>

    /**
     * 返回 [type] 在当前 session 中可见的直接父类型。
     *
     * 返回值已经应用 [type] 的类型实参，并可包含 extend 语义补充的接口父类型。
     * 这是纯类型投影，因此相同类型在这里有意去重。
     */
    fun getDirectSupertypes(type: ConeCangJieType): List<ConeCangJieType> =
        getDirectSupertypeDescriptors(type).map(CfirInstantiatedSupertypeDescriptor::type).distinct()

    /**
     * 返回 [type] 在官方赋值/子类型谓词语义下的直接父类型。
     *
     * 与 [getDirectSupertypes] 的差异仅在 extend 边：按照官方
     * `TypeManager::HasExtendInterfaceTyHelper` 的 direct-TyVar-only 映射规则，
     * 接口类型中仍残留 extend 类型形参的边（如 `extend<X> A<B<X>> <: I<X>`
     * 在 use-site `A<B<Int64>>` 上产生的 `I<X>` 边）不参与本视图。
     * 仅子类型判定路径（AbstractTypeChecker 的 supertypes 查询）应消费本方法；
     * 成员查找、可达性遍历与 match 绑定继续使用 [getDirectSupertypes]。
     */
    fun getPredicateSupertypes(type: ConeCangJieType): List<ConeCangJieType> = getDirectSupertypes(type)
}
