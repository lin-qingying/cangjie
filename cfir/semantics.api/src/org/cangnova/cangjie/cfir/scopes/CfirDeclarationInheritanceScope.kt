package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.name.Name
import java.util.Collections

/** 声明继承检查输入，不属于普通查找或晚期实例化发布表。 */
interface CfirDeclarationInheritanceScope {
    fun declarationInheritanceInput(owner: CfirExtend): CfirDeclarationInheritanceInput
}

enum class CfirInheritanceMemberKind { FUNCTION, PROPERTY, VARIABLE }

/** 真实声明 header 的不可变标志；不会在诊断期间重新读取成员 status。 */
data class CfirInheritanceMemberFlags(
    val isStatic: Boolean,
    val isOverride: Boolean,
    val isRedef: Boolean,
    val isConst: Boolean,
    val isMut: Boolean,
    val isDefault: Boolean,
    val isAbstract: Boolean,
    val visibility: Visibility,
    val isVar: Boolean,
    val hasGetter: Boolean,
    val hasSetter: Boolean,
    val isOpen: Boolean,
    val isInterfaceOwner: Boolean,
    val isConstructor: Boolean,
    val isOperator: Boolean,
)

/** 准备阶段捕获的完整 header；绑定 base scope 时不再读取活动声明或类型引用。 */
class CfirInheritanceMemberHeader(
    val declaration: CfirCallableSymbol<*>,
    val name: Name,
    val kind: CfirInheritanceMemberKind,
    val physicalOwner: CfirDeclaration?,
    val logicalOwner: CfirDeclaration?,
    val flags: CfirInheritanceMemberFlags,
    val returnType: ConeCangJieType,
    parameterTypes: List<ConeCangJieType>,
    parameterNames: List<Name>,
    namedParameters: List<Boolean>,
    typeParameters: List<CfirTypeParameterSymbol>,
    upperBounds: Map<CfirTypeParameterSymbol, List<ConeCangJieType>>,
    val hasOwnGeneric: Boolean,
    returnUpperBounds: List<ConeCangJieType>,
) {
    val returnUpperBounds = Collections.unmodifiableList(ArrayList(returnUpperBounds))
    val parameterTypes: List<ConeCangJieType> = Collections.unmodifiableList(ArrayList(parameterTypes))
    val parameterNames: List<Name> = Collections.unmodifiableList(ArrayList(parameterNames))
    val namedParameters: List<Boolean> = Collections.unmodifiableList(ArrayList(namedParameters))
    val typeParameters: List<CfirTypeParameterSymbol> = Collections.unmodifiableList(ArrayList(typeParameters))
    val upperBounds: Map<CfirTypeParameterSymbol, List<ConeCangJieType>> = Collections.unmodifiableMap(upperBounds.mapValues {
        Collections.unmodifiableList(ArrayList(it.value))
    })
    override fun equals(other: Any?): Boolean = other is CfirInheritanceMemberHeader && declaration === other.declaration &&
        physicalOwner === other.physicalOwner && logicalOwner === other.logicalOwner && name == other.name && kind == other.kind &&
        flags == other.flags && returnType == other.returnType && parameterTypes == other.parameterTypes &&
        parameterNames == other.parameterNames && namedParameters == other.namedParameters &&
        typeParameters == other.typeParameters && upperBounds == other.upperBounds && hasOwnGeneric == other.hasOwnGeneric &&
        returnUpperBounds == other.returnUpperBounds
    override fun hashCode(): Int = listOf(System.identityHashCode(declaration), System.identityHashCode(physicalOwner),
        System.identityHashCode(logicalOwner), name, kind, flags, returnType, parameterTypes, parameterNames, namedParameters,
        typeParameters, upperBounds, hasOwnGeneric, returnUpperBounds).hashCode()
}

/** 已应用 physical→logical→receiver 投影的完整成员；保存类型与声明身份独立。 */
class CfirMappedInheritedMember(
    val declaration: CfirCallableSymbol<*>,
    val name: Name,
    val kind: CfirInheritanceMemberKind,
    val physicalOwner: CfirDeclaration?,
    val logicalOwner: CfirDeclaration?,
    val structType: ConeCangJieType,
    val baseScope: CfirTypeScope,
    val flags: CfirInheritanceMemberFlags,
    val returnType: ConeCangJieType,
    parameterTypes: List<ConeCangJieType>,
    parameterNames: List<Name>,
    namedParameters: List<Boolean>,
    typeParameters: List<CfirTypeParameterSymbol>,
    upperBounds: Map<CfirTypeParameterSymbol, List<ConeCangJieType>>,
    inconsistentTypes: List<ConeCangJieType>,
    val function: CfirStoredFunctionMember?,
    val replaceOther: Boolean,
    val isInheritedInterface: Boolean,
    val shouldBeImplemented: Boolean,
    val sourceExtend: CfirExtend?,
    returnUpperBounds: List<ConeCangJieType>,
) {
    val returnUpperBounds = Collections.unmodifiableList(ArrayList(returnUpperBounds))
    val parameterTypes: List<ConeCangJieType> = Collections.unmodifiableList(ArrayList(parameterTypes))
    val parameterNames: List<Name> = Collections.unmodifiableList(ArrayList(parameterNames))
    val namedParameters: List<Boolean> = Collections.unmodifiableList(ArrayList(namedParameters))
    val typeParameters: List<CfirTypeParameterSymbol> = Collections.unmodifiableList(ArrayList(typeParameters))
    val upperBounds: Map<CfirTypeParameterSymbol, List<ConeCangJieType>> = Collections.unmodifiableMap(upperBounds.mapValues {
        Collections.unmodifiableList(ArrayList(it.value))
    })
    val inconsistentTypes: List<ConeCangJieType> = Collections.unmodifiableList(ArrayList(inconsistentTypes))
    init { require(parameterTypes.size == parameterNames.size && parameterTypes.size == namedParameters.size) }
    private fun valueKey(): List<Any?> = listOf(declaration, name, kind, physicalOwner, logicalOwner, structType, baseScope,
        flags, returnType, parameterTypes, parameterNames, namedParameters, typeParameters, upperBounds, inconsistentTypes,
        function, replaceOther, isInheritedInterface, shouldBeImplemented, sourceExtend, returnUpperBounds)
    override fun equals(other: Any?): Boolean = other is CfirMappedInheritedMember && valueKey() == other.valueKey()
    override fun hashCode(): Int = valueKey().hashCode()
}

/** 当前成员写入 visible 之前的两个独立同名 parent 快照。 */
class CfirDeclarationInheritanceStep(
    val member: CfirMappedInheritedMember,
    visibleParents: List<CfirMappedInheritedMember>,
    invisibleParents: List<CfirMappedInheritedMember>,
) {
    val visibleParents: List<CfirMappedInheritedMember> = Collections.unmodifiableList(ArrayList(visibleParents))
    val invisibleParents: List<CfirMappedInheritedMember> = Collections.unmodifiableList(ArrayList(invisibleParents))
}

/** 按原声明顺序保存诊断步骤；invisible 永不进入实现候选或最终发布。 */
class CfirDeclarationInheritanceInput(
    val owner: CfirExtend,
    steps: List<CfirDeclarationInheritanceStep>,
    interfaceRequirements: List<CfirMappedInheritedMember>,
    visibleImplementations: List<CfirMappedInheritedMember>,
    invisibleMembers: List<CfirMappedInheritedMember>,
    requirementSources: Map<CfirMappedInheritedMember, org.cangnova.cangjie.cfir.types.CfirTypeRef>,
    initialVisibleImplementations: List<CfirMappedInheritedMember>,
    finalMembers: List<CfirMappedInheritedMember>,
) {
    val steps: List<CfirDeclarationInheritanceStep> = Collections.unmodifiableList(ArrayList(steps))
    val interfaceRequirements: List<CfirMappedInheritedMember> = Collections.unmodifiableList(ArrayList(interfaceRequirements))
    val visibleImplementations: List<CfirMappedInheritedMember> = Collections.unmodifiableList(ArrayList(visibleImplementations))
    val invisibleMembers: List<CfirMappedInheritedMember> = Collections.unmodifiableList(ArrayList(invisibleMembers))
    val requirementSources = Collections.unmodifiableMap(LinkedHashMap(requirementSources))
    /** nominal 已加入、own 尚未写入的 export dependence 输入。 */
    val initialVisibleImplementations = Collections.unmodifiableList(ArrayList(initialVisibleImplementations))
    /** requirements←visible 合并后、清理临时状态前的最终诊断输入。 */
    val finalMembers = Collections.unmodifiableList(ArrayList(finalMembers))
}
