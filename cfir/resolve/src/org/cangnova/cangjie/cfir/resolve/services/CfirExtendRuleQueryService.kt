package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.session.services.CfirExtendRuleQueryService
import org.cangnova.cangjie.cfir.session.services.CfirExtendInheritedInterfaceSemantic
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

class CfirExtendRuleQueryServiceImpl(
    private val indexStore: CfirExtendIndexStore,
) : CfirExtendRuleQueryService {
    override fun targetClassIdOf(declaration: Any): ClassId? =
        indexStore.modelForDeclaration(declaration)?.targetClassId

    override fun packageFqNameOf(declaration: Any): FqName? =
        indexStore.modelForDeclaration(declaration)?.packageFqName

    override fun inheritedInterfacesOf(declaration: Any): List<CfirExtendInheritedInterfaceSemantic> =
        indexStore.modelForDeclaration(declaration)?.inheritedInterfaces.orEmpty()

    override fun inheritedInterfacesForTarget(
        targetClassId: ClassId,
        excludingDeclaration: Any?,
    ): List<CfirExtendInheritedInterfaceSemantic> {
        return indexStore.modelsForClass(targetClassId)
            .asSequence()
            .filter { excludingDeclaration == null || it.declaration !== excludingDeclaration }
            .flatMap { it.inheritedInterfaces.asSequence() }
            .toList()
    }

    override fun inheritedInterfaceClassIdsOf(declaration: Any): List<ClassId> =
        indexStore.modelForDeclaration(declaration)?.inheritedInterfaceClassIds.orEmpty()

    override fun inheritedInterfaceClassIdsForTarget(targetClassId: ClassId, excludingDeclaration: Any?): List<ClassId> {
        return indexStore.modelsForClass(targetClassId)
            .asSequence()
            .filter { excludingDeclaration == null || it.declaration !== excludingDeclaration }
            .flatMap { it.inheritedInterfaceClassIds.asSequence() }
            .toList()
    }

    override fun inheritedInterfaceSemanticKeysOf(declaration: Any): List<String> =
        indexStore.modelForDeclaration(declaration)?.inheritedInterfaceSemanticKeys.orEmpty()

    override fun inheritedInterfaceSemanticKeysForTarget(targetClassId: ClassId, excludingDeclaration: Any?): List<String> {
        return indexStore.modelsForClass(targetClassId)
            .asSequence()
            .filter { excludingDeclaration == null || it.declaration !== excludingDeclaration }
            .flatMap { it.inheritedInterfaceSemanticKeys.asSequence() }
            .toList()
    }

    override fun defaultIndependentMembersOfInterface(interfaceClassId: ClassId): List<Name> =
        indexStore.defaultIndependentMembersOfInterface(interfaceClassId)

    override fun targetClassOwnInterfaceClassIds(targetClassId: ClassId): Set<ClassId> =
        indexStore.targetClassOwnInterfaceClassIds(targetClassId)

    override fun otherPackageExtendedInterfaceClassIds(targetClassId: ClassId, currentPackage: FqName): Set<ClassId> =
        indexStore.otherPackageExtendedInterfaceClassIds(targetClassId, currentPackage)
}

