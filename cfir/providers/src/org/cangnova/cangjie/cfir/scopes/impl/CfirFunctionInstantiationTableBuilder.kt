package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.symbols.constructType

import org.cangnova.cangjie.cfir.resolve.substitution.substitutorByMap

import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.scopes.*
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.extendRuleQueryService
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.*
import java.util.Collections

/**
 * 官方 UpdateInheritedMemberIfNeeded 的存储更新 owner。
 * 名字、STATIC 与声明自身 GENERIC 相同后比较当前存储的参数类型，只更新首个命中；
 * COMMON/SPECIFIC、导出过滤及 implementation 指针不参与更新条件。
 */
internal class CfirFunctionInstantiationTableBuilder(
    private val session: CfirSession,
    private val rootExtend: CfirExtend? = null,
    private val extendsRelated: (CfirExtend) -> Boolean = { rootExtend == null || session.extendRuleQueryService.areExtendsInInheritRelation(rootExtend, it) },
) {
    private val members = ArrayList<CfirStoredFunctionMember>()
    private val updates = ArrayList<CfirStoredFunctionUpdate>()
    private val ownDeclarations = LinkedHashSet<CfirCallableSymbol<*>>()

    fun addOwnDeclaration(symbol: CfirCallableSymbol<*>) {
        ownDeclarations += symbol
    }

    /** 原生实例化先复制发布表，复制过程不得再次执行继承更新。 */
    fun copyFrom(table: CfirFunctionInstantiationTable) {
        check(members.isEmpty() && updates.isEmpty() && ownDeclarations.isEmpty())
        members += table.members
        updates += table.updates
        ownDeclarations += table.ownDeclarations
    }

    fun update(child: CfirStoredFunctionMember, inheritedInterfaces: Boolean = false) {
        for (index in members.indices) {
            val originalParent = members[index]
            if (originalParent.declaration.name != child.declaration.name) continue
            if (originalParent.isStatic != child.isStatic || originalParent.hasOwnGeneric != child.hasOwnGeneric) continue
            val parent = originalParent.mapOwnHeaderTo(child)
            if (!areFunctionParameterTypesIdentical(
                    parent.parameterTypes, child.parameterTypes,
                    ConeSubstitutor.Empty, ConeSubstitutor.Empty, session.typeContext,
                )
            ) continue

            val replaceDeclaration = parent.isAbstract || !child.isAbstract
            val bothInterfaceMembers = inheritedInterfaces && parent.isInterfaceOwner && child.isInterfaceOwner
            val adoptParentType = bothInterfaceMembers &&
                    areStoredFunctionReturnTypesCompatible(child, parent, session)
            val consistentReturn = (replaceDeclaration &&
                    areStoredFunctionReturnTypesCompatible(parent, child, session)) || adoptParentType
            val selected = if (replaceDeclaration) {
                if (adoptParentType) child.copy(parameterTypes = parent.parameterTypes, returnType = parent.returnType,
                    returnUpperBounds = parent.returnUpperBounds) else child
            } else {
                originalParent
            }
            val incomingInterface = child.isInheritedInterface && inheritedInterfaces
            val needImplement = child.isInterfaceOwner || inheritedInterfaces
            val childSourceExtend = child.sourceExtend
            val related = childSourceExtend == null || extendsRelated(childSourceExtend)
            val inconsistentType = needImplement &&
                    (originalParent.parameterTypes != child.parameterTypes || originalParent.returnType != child.returnType) &&
                    !consistentReturn
            val inconsistent = if (needImplement && (parent.inconsistentReturnTypes.isNotEmpty() || inconsistentType)) {
                (selected.inconsistentReturnTypes + parent.inconsistentReturnTypes +
                        if (inconsistentType) listOf(parent.returnType, child.returnType) else emptyList()).distinct()
            } else {
                selected.inconsistentReturnTypes
            }
            val boundsWithParent = mergeUpperBounds(selected, parent, inheritedInterfaces)
            val mergedBounds = mergeUpperBounds(selected.copy(upperBounds = boundsWithParent), child, inheritedInterfaces)
            members[index] = selected.copy(
                replaceOther = true,
                upperBounds = mergedBounds,
                inconsistentReturnTypes = inconsistent,
                isInheritedInterface = incomingInterface,
                shouldBeImplemented = (incomingInterface || !related) && parent.isDefault && child.isDefault,
                sourceExtend = if (parent.sourceExtend == null || child.sourceExtend == null) null else selected.sourceExtend,
            )
            updates += CfirStoredFunctionUpdate(originalParent.identity, child.identity, members[index].identity, replaceDeclaration)
            return
        }
        members += child
    }

    fun addTable(table: CfirFunctionInstantiationTable, inheritedInterfaces: Boolean = false) {
        updates += table.updates
        for (member in table.members) update(
            if (inheritedInterfaces) member.copy(isInheritedInterface = true) else member,
            inheritedInterfaces,
        )
    }

    fun removePrivateMembers() {
        members.removeAll { it.isPrivate }
    }

    /** visibleExtendMembers 只保留能提供实现的成员。 */
    fun removeAbstractMembers() {
        members.removeAll { it.isAbstract }
    }

    fun clearPublishedTransientState() {
        for (index in members.indices) members[index] = members[index].copy(isInheritedInterface = false, sourceExtend = null)
    }

    fun finish(kind: CfirFunctionMemberTableKind): CfirFunctionInstantiationTable =
        CfirFunctionInstantiationTable(members, updates, ownDeclarations, kind)

    private fun CfirStoredFunctionMember.mapOwnHeaderTo(child: CfirStoredFunctionMember): CfirStoredFunctionMember {
        if (!hasOwnGeneric || !child.hasOwnGeneric) return this
        val parentParameters = upperBounds.keys.toList()
        val childParameters = child.upperBounds.keys.toList()
        if (parentParameters.size != childParameters.size) return this
        val substitutor = substitutorByMap(parentParameters.zip(childParameters).associate { (origin, target) -> origin to target.constructType() }, session)
        return copy(
            parameterTypes = parameterTypes.map(substitutor::substituteOrSelf),
            returnType = substitutor.substituteOrSelf(returnType),
            returnUpperBounds = returnUpperBounds.map(substitutor::substituteOrSelf),
        )
    }

    private fun mergeUpperBounds(
        selected: CfirStoredFunctionMember,
        other: CfirStoredFunctionMember,
        shouldMerge: Boolean,
    ): Map<CfirTypeParameterSymbol, List<ConeCangJieType>> {
        if (!shouldMerge || selected.declaration === other.declaration || selected.upperBounds.isEmpty() ||
            selected.upperBounds.size != other.upperBounds.size
        ) return selected.upperBounds
        val selectedHeader = selected.upperBounds.keys.toList()
        val otherHeader = other.upperBounds.keys.toList()
        val mapping = if (selectedHeader.size == otherHeader.size) otherHeader.zip(selectedHeader).toMap() else emptyMap()
        val substitutor = substitutorByMap(mapping.entries.associate { (origin, target) -> origin to target.constructType() }, session)
        return Collections.unmodifiableMap(selected.upperBounds.entries.zip(other.upperBounds.values).associate { (entry, bounds) ->
            entry.key to (entry.value + bounds.map(substitutor::substituteOrSelf)).distinct()
        })
    }
}


