package org.cangnova.cangjie.cfir.session.services

import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * extend 声明继承接口的语义快照。
 *
 * @property classId 可解析到 nominal 接口声明时的接口 [ClassId]。
 * @property semanticKey 经过 extend 类型语义归一化后的接口键，用于区分泛型实参不同的接口实例。
 */
data class CfirExtendInheritedInterfaceSemantic(
    /** 可解析到 nominal 接口声明时的接口 [ClassId]。 */
    val classId: ClassId?,
    /** 经过 extend 类型语义归一化后的接口键，用于区分泛型实参不同的接口实例。 */
    val semanticKey: String,
)

/**
 * extend 规则索引的只读查询服务。
 *
 * 实现由 resolve 阶段构建的 `CfirExtendIndexStore` 提供，checker、provider 和 scope 层
 * 通过该接口查询同一份 extend 语义模型，避免各阶段重复解析 extend 目标、继承接口闭包
 * 或源码顺序关系。
 */
interface CfirExtendRuleQueryService : CfirSessionComponent {
    /**
     * 返回 [declaration] 对应 extend 的目标键。
     */
    fun targetKeyOf(declaration: Any): CfirExtendTargetKey?

    /**
     * 返回与 [declaration] 具有同一完整实例化目标模式的 extend 声明。
     *
     * 该查询用于重复接口等精确同目标检查；与 nominal target 候选召回分离。
     */
    fun extendDeclarationsForSameTarget(declaration: Any): List<Any>

    /**
     * 返回与 [declaration] 共享同一展开后 nominal target 的全部 extend 声明。
     *
     * 结果保留各声明自身的 target pattern，供 specialization checker 做结构化实例化匹配。
     */
    fun extendDeclarationsForNominalTarget(declaration: Any): List<Any>

    /**
     * 返回 [declaration] 对应 extend 的 nominal 目标类标识。
     */
    fun targetClassIdOf(declaration: Any): ClassId?

    /**
     * 返回 [declaration] 所在文件的包名。
     */
    fun packageFqNameOf(declaration: Any): FqName?

    /**
     * 返回 [declaration] 直接声明继承的接口语义列表。
     */
    fun inheritedInterfacesOf(declaration: Any): List<CfirExtendInheritedInterfaceSemantic>

    /**
     * 返回同一 [targetKey] 上所有 extend 继承的接口语义列表。
     *
     * [excludingDeclaration] 非空时会从结果中排除该 extend 声明，供冲突检查避免自比较。
     */
    fun inheritedInterfacesForTarget(targetKey: CfirExtendTargetKey, excludingDeclaration: Any? = null): List<CfirExtendInheritedInterfaceSemantic>

    /**
     * 返回同一 nominal 目标类上所有 extend 继承的接口语义列表。
     */
    fun inheritedInterfacesForTarget(targetClassId: ClassId, excludingDeclaration: Any? = null): List<CfirExtendInheritedInterfaceSemantic>

    /**
     * 返回 [declaration] 直接继承接口中可解析的 [ClassId] 列表。
     */
    fun inheritedInterfaceClassIdsOf(declaration: Any): List<ClassId>

    /**
     * 返回同一 [targetKey] 上所有 extend 的直接接口 [ClassId] 列表。
     */
    fun inheritedInterfaceClassIdsForTarget(targetKey: CfirExtendTargetKey, excludingDeclaration: Any? = null): List<ClassId>

    /**
     * 返回同一 nominal 目标类上所有 extend 的直接接口 [ClassId] 列表。
     */
    fun inheritedInterfaceClassIdsForTarget(targetClassId: ClassId, excludingDeclaration: Any? = null): List<ClassId>

    /**
     * 返回单条 extend 声明直接接口及其传递父接口组成的 [ClassId] 闭包。
     */
    fun inheritedInterfaceClosureClassIdsOf(declaration: Any): Set<ClassId>

    /**
     * 判断两个 extend 声明的直接接口之间是否存在继承关系。
     */
    fun areExtendsInInheritRelation(firstDeclaration: Any, secondDeclaration: Any): Boolean

    /**
     * 判断 [declaration] 与同目标其他 extend 的检查顺序是否存在无法决定的继承关系。
     */
    fun hasUndecidableExtendCheckSequence(declaration: Any): Boolean

    /**
     * 返回 [declaration] 直接继承接口的语义键列表。
     */
    fun inheritedInterfaceSemanticKeysOf(declaration: Any): List<String>

    /**
     * 返回同一 [targetKey] 上所有 extend 直接继承接口的语义键列表。
     */
    fun inheritedInterfaceSemanticKeysForTarget(targetKey: CfirExtendTargetKey, excludingDeclaration: Any? = null): List<String>

    /**
     * 返回同一 nominal 目标类上所有 extend 直接继承接口的语义键列表。
     */
    fun inheritedInterfaceSemanticKeysForTarget(targetClassId: ClassId, excludingDeclaration: Any? = null): List<String>

    /**
     * 返回接口中默认实现成员且不依赖接口类型形参的成员名。
     */
    fun defaultIndependentMembersOfInterface(interfaceClassId: ClassId): List<Name>

    /**
     * 获取当前 extend 目标类型模式代入后，目标类自身已经继承的接口语义集合。
     */
    fun targetOwnInterfacesOf(declaration: Any): List<CfirExtendInheritedInterfaceSemantic>

    /**
     * 获取目标类自身声明中继承的所有接口 [ClassId] 集合。
     */
    fun targetClassOwnInterfaceClassIds(targetClassId: ClassId): Set<ClassId>

    /**
     * 获取其他包中对同一目标类已 extend 过的接口 [ClassId] 集合（含传递父接口）。
     */
    fun otherPackageExtendedInterfaceClassIds(targetClassId: ClassId, currentPackage: FqName): Set<ClassId>

    /**
     * 获取其他包中对同一目标键已 extend 过的接口 [ClassId] 集合（含传递父接口）。
     */
    fun otherPackageExtendedInterfaceClassIds(targetKey: CfirExtendTargetKey, currentPackage: FqName): Set<ClassId>

    /**
     * 判断给定 extend 声明是否在源码序中排在同一目标的所有 extend 的首位。
     *
     * 官方编译器对 specialization conflict / default implementation conflict
     * 只在"后方"的 extend 上报错；排在首位的 extend 不报错。
     */
    fun isFirstExtendForTarget(declaration: Any, targetClassId: ClassId): Boolean

    /**
     * 判断给定 extend 声明是否在源码序中排在同一目标键的所有 extend 的首位。
     */
    fun isFirstExtendForTarget(declaration: Any, targetKey: CfirExtendTargetKey): Boolean
}
