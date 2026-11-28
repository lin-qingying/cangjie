package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.name.Name
import java.util.Collections

/** 实例化检查的来源身份，原声明与进入实例化之前的 structTy 分别保存。 */
data class CfirInstantiationMemberIdentity(
    val declaration: CfirCallableSymbol<*>,
    val structType: ConeCangJieType,
    val lookupProvenance: CfirCallableLookupProvenance,
)

/**
 * 原生 MemberSignature 的函数投影。declaration、当前存储的参数/返回类型与 structType
 * 相互独立；接口归并可以采用 parent 的函数类型，同时保留 child 的声明身份。
 */
data class CfirStoredFunctionMember(
    val declaration: CfirCallableSymbol<*>,
    val parameterTypes: List<ConeCangJieType>,
    val returnType: ConeCangJieType,
    val structType: ConeCangJieType,
    val identity: CfirInstantiationMemberIdentity,
    val baseScope: CfirTypeScope,
    val lookupProvenance: CfirCallableLookupProvenance,
    val upperBounds: Map<CfirTypeParameterSymbol, List<ConeCangJieType>>,
    val inconsistentReturnTypes: List<ConeCangJieType>,
    val replaceOther: Boolean,
    val isInheritedInterface: Boolean,
    val shouldBeImplemented: Boolean,
    val sourceExtend: CfirExtend?,
    val isStatic: Boolean,
    val hasOwnGeneric: Boolean,
    val isAbstract: Boolean,
    val isPrivate: Boolean,
    val isConstructor: Boolean,
    val isDefault: Boolean,
    val returnUpperBounds: List<ConeCangJieType>,
    val isInterfaceOwner: Boolean,
)

/** 统一冻结函数存储载荷；归一化、绑定和表构造均使用同一拥有集合协议。 */
fun CfirStoredFunctionMember.freezeFunctionMember(): CfirStoredFunctionMember = copy(
    parameterTypes = Collections.unmodifiableList(ArrayList(parameterTypes)),
    upperBounds = Collections.unmodifiableMap(upperBounds.mapValues { (_, bounds) -> Collections.unmodifiableList(ArrayList(bounds)) }),
    inconsistentReturnTypes = Collections.unmodifiableList(ArrayList(inconsistentReturnTypes)),
    returnUpperBounds = Collections.unmodifiableList(ArrayList(returnUpperBounds)),
)

/** 真实写入成员表的历史，不把声明 override 关系当作发生替换的证明。 */
data class CfirStoredFunctionUpdate(
    val previous: CfirInstantiationMemberIdentity,
    val incoming: CfirInstantiationMemberIdentity,
    val stored: CfirInstantiationMemberIdentity,
    val replacedDeclaration: Boolean,
)

/** 未实例化成员表；实例化检查补入 private/构造器时只修改副本。 */
class CfirFunctionInstantiationTable(
    members: List<CfirStoredFunctionMember>,
    updates: List<CfirStoredFunctionUpdate>,
    ownDeclarations: Set<CfirCallableSymbol<*>>,
    val kind: CfirFunctionMemberTableKind,
) {
    val members: List<CfirStoredFunctionMember> = Collections.unmodifiableList(members.map { member ->
        member.freezeFunctionMember()
    })
    val updates: List<CfirStoredFunctionUpdate> = Collections.unmodifiableList(ArrayList(updates))
    val ownDeclarations: Set<CfirCallableSymbol<*>> = Collections.unmodifiableSet(LinkedHashSet(ownDeclarations))
}

/** 原生合并容器身份由实际操作给出，不能从候选所属类批量推断接口标志。 */
enum class CfirFunctionMemberTableKind { INSTANCE, INTERFACE, VISIBLE_EXTEND, INSTANTIATION_COPY }

/** 独立于调用 actualizing 与声明 PreCheck 的实例化成员输入。 */
interface CfirFunctionInstantiationScope {
    /** 已移除 private 并清除临时状态的 structInheritedMembers 发布表。 */
    fun publishedFunctionMemberTable(name: Name): CfirFunctionInstantiationTable
    /** 对位 GetInheritedSuperMembers：发布表加上父类型可见的 extend，私有成员不传播。 */
    fun inheritedFunctionMemberTable(name: Name, useSiteOwner: CfirDeclaration): CfirFunctionInstantiationTable
    fun functionInstantiationTable(name: Name): CfirFunctionInstantiationTable
    fun constructorInstantiationTable(): CfirFunctionInstantiationTable
}

/** MergeInheritedMembers 只代换已有 ty/structTy/upperBounds，不从 declaration 重建函数类型。 */
fun CfirFunctionInstantiationTable.projectStoredFunctionTypes(
    substitutor: ConeSubstitutor,
    baseScope: CfirTypeScope,
): CfirFunctionInstantiationTable {
    fun CfirInstantiationMemberIdentity.project(): CfirInstantiationMemberIdentity =
        copy(structType = substitutor.substituteOrSelf(structType))
    return CfirFunctionInstantiationTable(
        members.map { member ->
            val projectedStructType = substitutor.substituteOrSelf(member.structType)
            member.copy(
                parameterTypes = member.parameterTypes.map(substitutor::substituteOrSelf),
                returnType = substitutor.substituteOrSelf(member.returnType),
                structType = projectedStructType,
                identity = member.identity.project(),
                baseScope = baseScope,
                upperBounds = member.upperBounds.mapValues { (_, bounds) -> bounds.map(substitutor::substituteOrSelf) },
                inconsistentReturnTypes = member.inconsistentReturnTypes.map(substitutor::substituteOrSelf),
                returnUpperBounds = member.returnUpperBounds.map(substitutor::substituteOrSelf),
            )
        },
        updates.map { update -> update.copy(
            previous = update.previous.project(), incoming = update.incoming.project(), stored = update.stored.project(),
        ) },
        ownDeclarations,
        kind,
    )
}
