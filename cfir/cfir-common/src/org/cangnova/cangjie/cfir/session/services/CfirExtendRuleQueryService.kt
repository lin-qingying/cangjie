package org.cangnova.cangjie.cfir.session.services

import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

data class CfirExtendInheritedInterfaceSemantic(
    val classId: ClassId?,
    val semanticKey: String,
)

interface CfirExtendRuleQueryService : CfirSessionComponent {
    fun targetClassIdOf(declaration: Any): ClassId?
    fun packageFqNameOf(declaration: Any): FqName?
    fun inheritedInterfacesOf(declaration: Any): List<CfirExtendInheritedInterfaceSemantic>
    fun inheritedInterfacesForTarget(targetClassId: ClassId, excludingDeclaration: Any? = null): List<CfirExtendInheritedInterfaceSemantic>
    fun inheritedInterfaceClassIdsOf(declaration: Any): List<ClassId>
    fun inheritedInterfaceClassIdsForTarget(targetClassId: ClassId, excludingDeclaration: Any? = null): List<ClassId>
    fun inheritedInterfaceSemanticKeysOf(declaration: Any): List<String>
    fun inheritedInterfaceSemanticKeysForTarget(targetClassId: ClassId, excludingDeclaration: Any? = null): List<String>
    fun defaultIndependentMembersOfInterface(interfaceClassId: ClassId): List<Name>
}
