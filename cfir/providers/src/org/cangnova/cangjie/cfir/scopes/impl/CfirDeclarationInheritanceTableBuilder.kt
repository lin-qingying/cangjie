package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.scopes.*
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.type.AbstractTypeChecker

/** 完整成员表的更新 owner；函数复用既有存储归并，非函数保留 kind 与类型不一致证据。 */
internal class CfirDeclarationInheritanceTableBuilder(private val session: CfirSession, private val rootExtend: CfirExtend,
    private val relatedExtends: Set<CfirExtend>) {
    private val members = ArrayList<CfirMappedInheritedMember>()
    fun snapshot(): List<CfirMappedInheritedMember> = members.toList()
    fun removeAbstractMembers() { members.removeAll { it.flags.isAbstract } }
    fun addAll(values: List<CfirMappedInheritedMember>, interfaces: Boolean = false) { values.forEach { update(it, interfaces) } }

    fun update(child: CfirMappedInheritedMember, interfaces: Boolean = false) {
        for (index in members.indices) {
            val parent = members[index]
            if (parent.name != child.name || parent.kind != child.kind) continue
            if (parent.kind == CfirInheritanceMemberKind.FUNCTION) {
                val builder = CfirFunctionInstantiationTableBuilder(session, rootExtend,
                    extendsRelated = { it in relatedExtends })
                val parentFunction = checkNotNull(parent.function)
                val childFunction = checkNotNull(child.function)
                builder.copyFrom(CfirFunctionInstantiationTable(listOf(parentFunction), emptyList(), emptySet(), CfirFunctionMemberTableKind.INTERFACE))
                builder.update(childFunction, interfaces)
                val merged = builder.finish(CfirFunctionMemberTableKind.INTERFACE)
                if (merged.members.size != 1) continue
                val stored = merged.members.single()
                val selected = if (stored.declaration === child.declaration) child else parent
                members[index] = selected.withStoredFunction(stored)
            } else {
                val selected = if (parent.flags.isAbstract || !child.flags.isAbstract) child else parent
                val needImplement = child.flags.isInterfaceOwner || interfaces
                val inconsistent = if (needImplement && !AbstractTypeChecker.equalTypes(session.typeContext, parent.returnType, child.returnType))
                    (selected.inconsistentTypes + parent.inconsistentTypes + parent.returnType + child.returnType).distinct()
                    else if (needImplement) (selected.inconsistentTypes + parent.inconsistentTypes).distinct() else selected.inconsistentTypes
                val incomingInterface = child.isInheritedInterface && interfaces
                val related = child.sourceExtend == null || child.sourceExtend in relatedExtends
                members[index] = CfirMappedInheritedMember(selected.declaration, selected.name, selected.kind,
                    selected.physicalOwner, selected.logicalOwner, selected.structType, selected.baseScope, selected.flags,
                    selected.returnType, selected.parameterTypes, selected.parameterNames, selected.namedParameters,
                    selected.typeParameters, selected.upperBounds, inconsistent, null, true,
                    incomingInterface, parent.flags.isDefault && child.flags.isDefault && (incomingInterface || !related),
                    if (parent.sourceExtend == null || child.sourceExtend == null) null else selected.sourceExtend,
                    selected.returnUpperBounds)
            }
            return
        }
        members += child
    }
}

internal fun CfirInheritanceMemberHeader.bindInheritanceMember(
    baseScope: CfirTypeScope,
    structType: org.cangnova.cangjie.cfir.types.ConeCangJieType,
    substitution: ConeSubstitutor,
    sourceExtend: org.cangnova.cangjie.cfir.declarations.CfirExtend?,
    lookupProvenance: CfirCallableLookupProvenance,
    interfaceMember: Boolean = false,
): CfirMappedInheritedMember {
    val parameters = parameterTypes.map(substitution::substituteOrSelf)
    val result = substitution.substituteOrSelf(returnType)
    val bounds = upperBounds.mapValues { (_, values) -> values.map(substitution::substituteOrSelf) }
    val provenance = lookupProvenance
    val function = if (kind == CfirInheritanceMemberKind.FUNCTION) CfirStoredFunctionMember(
        declaration, parameters, result, structType, CfirInstantiationMemberIdentity(declaration, structType, provenance),
        baseScope, provenance, bounds, emptyList(), false, interfaceMember, false, sourceExtend,
        flags.isStatic, hasOwnGeneric, flags.isAbstract, flags.visibility == org.cangnova.cangjie.descriptors.Visibilities.Private,
        flags.isConstructor, flags.isDefault,
        returnUpperBounds.map(substitution::substituteOrSelf),
        flags.isInterfaceOwner,
    ).freezeFunctionMember() else null
    return CfirMappedInheritedMember(declaration, name, kind, physicalOwner, logicalOwner, structType, baseScope, flags,
        result, parameters, parameterNames, namedParameters, typeParameters, bounds, emptyList(), function,
        false, interfaceMember, false, sourceExtend, returnUpperBounds.map(substitution::substituteOrSelf))
}

private fun CfirMappedInheritedMember.withStoredFunction(stored: CfirStoredFunctionMember): CfirMappedInheritedMember =
    CfirMappedInheritedMember(declaration, name, kind, physicalOwner, logicalOwner, stored.structType, stored.baseScope, flags,
        stored.returnType, stored.parameterTypes, parameterNames, namedParameters, typeParameters, stored.upperBounds,
        stored.inconsistentReturnTypes, stored, stored.replaceOther, stored.isInheritedInterface, stored.shouldBeImplemented, stored.sourceExtend,
        stored.returnUpperBounds)
