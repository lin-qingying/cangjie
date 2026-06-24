package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.session.services.CfirExtendRuleQueryService
import org.cangnova.cangjie.cfir.session.services.CfirExtendInheritedInterfaceSemantic
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

/**
 * 基于 [CfirExtendIndexStore] 的 extend 规则查询服务实现。
 */
class CfirExtendRuleQueryServiceImpl(
    /**
     * 会话级 extend 索引存储。
     */
    private val indexStore: CfirExtendIndexStore,
) : CfirExtendRuleQueryService {
    /**
     * 返回声明对应的 extend 目标键。
     */
    override fun targetKeyOf(declaration: Any): CfirExtendTargetKey? =
        indexStore.modelForDeclaration(declaration)?.targetKey

    /**
     * 返回声明对应的目标 classId。
     */
    override fun targetClassIdOf(declaration: Any): ClassId? =
        indexStore.modelForDeclaration(declaration)?.targetClassId

    /**
     * 返回声明所在包名。
     */
    override fun packageFqNameOf(declaration: Any): FqName? =
        indexStore.modelForDeclaration(declaration)?.packageFqName

    /**
     * 返回声明继承的 interface 语义项。
     */
    override fun inheritedInterfacesOf(declaration: Any): List<CfirExtendInheritedInterfaceSemantic> =
        indexStore.modelForDeclaration(declaration)?.inheritedInterfaces.orEmpty()

    /**
     * 按目标 classId 查询继承的 interface 语义项。
     */
    override fun inheritedInterfacesForTarget(
        targetClassId: ClassId,
        excludingDeclaration: Any?,
    ): List<CfirExtendInheritedInterfaceSemantic> =
        inheritedInterfacesForTarget(CfirExtendTargetKey.ClassLike(targetClassId), excludingDeclaration)

    /**
     * 按目标键查询继承的 interface 语义项。
     */
    override fun inheritedInterfacesForTarget(
        targetKey: CfirExtendTargetKey,
        excludingDeclaration: Any?,
    ): List<CfirExtendInheritedInterfaceSemantic> {
        return indexStore.modelsForTarget(targetKey)
            .asSequence()
            .filter { excludingDeclaration == null || it.declaration !== excludingDeclaration }
            .flatMap { it.inheritedInterfaces.asSequence() }
            .toList()
    }

    /**
     * 返回声明继承的 interface classId。
     */
    override fun inheritedInterfaceClassIdsOf(declaration: Any): List<ClassId> =
        indexStore.modelForDeclaration(declaration)?.inheritedInterfaceClassIds.orEmpty()

    /**
     * 按目标 classId 查询继承的 interface classId。
     */
    override fun inheritedInterfaceClassIdsForTarget(targetClassId: ClassId, excludingDeclaration: Any?): List<ClassId> =
        inheritedInterfaceClassIdsForTarget(CfirExtendTargetKey.ClassLike(targetClassId), excludingDeclaration)

    /**
     * 按目标键查询继承的 interface classId。
     */
    override fun inheritedInterfaceClassIdsForTarget(targetKey: CfirExtendTargetKey, excludingDeclaration: Any?): List<ClassId> {
        return indexStore.modelsForTarget(targetKey)
            .asSequence()
            .filter { excludingDeclaration == null || it.declaration !== excludingDeclaration }
            .flatMap { it.inheritedInterfaceClassIds.asSequence() }
            .toList()
    }

    /**
     * 返回声明继承 interface 的闭包 classId 集合。
     */
    override fun inheritedInterfaceClosureClassIdsOf(declaration: Any): Set<ClassId> =
        indexStore.inheritedInterfaceClosureClassIdsOf(declaration)

    /**
     * 判断两个 extend 声明是否处于继承关系。
     */
    override fun areExtendsInInheritRelation(firstDeclaration: Any, secondDeclaration: Any): Boolean =
        indexStore.areExtendsInInheritRelation(firstDeclaration, secondDeclaration)

    /**
     * 判断声明是否存在无法判定的 extend 检查序列。
     */
    override fun hasUndecidableExtendCheckSequence(declaration: Any): Boolean =
        indexStore.hasUndecidableExtendCheckSequence(declaration)

    /**
     * 返回声明继承 interface 的语义 key。
     */
    override fun inheritedInterfaceSemanticKeysOf(declaration: Any): List<String> =
        indexStore.modelForDeclaration(declaration)?.inheritedInterfaceSemanticKeys.orEmpty()

    /**
     * 按目标 classId 查询继承 interface 的语义 key。
     */
    override fun inheritedInterfaceSemanticKeysForTarget(targetClassId: ClassId, excludingDeclaration: Any?): List<String> =
        inheritedInterfaceSemanticKeysForTarget(CfirExtendTargetKey.ClassLike(targetClassId), excludingDeclaration)

    /**
     * 按目标键查询继承 interface 的语义 key。
     */
    override fun inheritedInterfaceSemanticKeysForTarget(targetKey: CfirExtendTargetKey, excludingDeclaration: Any?): List<String> {
        return indexStore.modelsForTarget(targetKey)
            .asSequence()
            .filter { excludingDeclaration == null || it.declaration !== excludingDeclaration }
            .flatMap { it.inheritedInterfaceSemanticKeys.asSequence() }
            .toList()
    }

    /**
     * 返回 interface 中默认独立成员名称。
     */
    override fun defaultIndependentMembersOfInterface(interfaceClassId: ClassId): List<Name> =
        indexStore.defaultIndependentMembersOfInterface(interfaceClassId)

    /**
     * 返回目标类型自身已经拥有的 interface 语义项。
     */
    override fun targetOwnInterfacesOf(declaration: Any): List<CfirExtendInheritedInterfaceSemantic> =
        indexStore.modelForDeclaration(declaration)?.targetOwnInterfaces.orEmpty()

    /**
     * 返回目标 class 自身声明的 interface classId 集合。
     */
    override fun targetClassOwnInterfaceClassIds(targetClassId: ClassId): Set<ClassId> =
        indexStore.targetClassOwnInterfaceClassIds(targetClassId)

    /**
     * 返回其他包 extend 注入的 interface classId 集合。
     */
    override fun otherPackageExtendedInterfaceClassIds(targetClassId: ClassId, currentPackage: FqName): Set<ClassId> =
        indexStore.otherPackageExtendedInterfaceClassIds(targetClassId, currentPackage)

    /**
     * 返回其他包针对目标键注入的 interface classId 集合。
     */
    override fun otherPackageExtendedInterfaceClassIds(targetKey: CfirExtendTargetKey, currentPackage: FqName): Set<ClassId> =
        indexStore.otherPackageExtendedInterfaceClassIds(targetKey, currentPackage)

    /**
     * 判断声明是否是目标 classId 上的第一个 extend。
     */
    override fun isFirstExtendForTarget(declaration: Any, targetClassId: ClassId): Boolean =
        indexStore.isFirstExtendForTarget(declaration, targetClassId)

    /**
     * 判断声明是否是目标键上的第一个 extend。
     */
    override fun isFirstExtendForTarget(declaration: Any, targetKey: CfirExtendTargetKey): Boolean =
        indexStore.isFirstExtendForTarget(declaration, targetKey)
}
