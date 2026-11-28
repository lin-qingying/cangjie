package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.expressions.CfirBlock
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirFunctionCall
import org.cangnova.cangjie.cfir.references.CfirControlFlowGraphReference
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirThisOwnerSymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.unwrapSubstitutionOverrides
import java.util.Collections
import java.util.IdentityHashMap

/** 完成路径上的 definite/possible 状态；它不是表达式访问过程中的最后写入位。 */
data class CfirFieldInitializationState(val definitelyInitialized: Boolean, val possiblyInitialized: Boolean) {
    init { require(!definitelyInitialized || possiblyInitialized) }
}

enum class CfirConstructorInitializationAvailability { COMPLETE, HEADER_ONLY, INVALID_DELEGATION, DELEGATION_CYCLE }
enum class CfirClassInitializationOrigin { SOURCE, IMPORTED_FIELDS, NO_IMPLEMENTATION }

/** 静态诊断边界的来源与可用性，不能以空候选集冒充已完成分析。 */
enum class CfirNativeInitializationDiagnosticAvailability { COMPLETE, IMPORTED_FIELDS, NOT_APPLICABLE, UNAVAILABLE }

/** 字段义务属于当前 owner，或由已完成的实际父边界转交；不是 reporter 的错误去重表。 */
class CfirNativeFieldInitializationObligation(
    val field: CfirFieldVariable,
    val checkingOwner: CfirClassLikeDeclaration,
    val parentBoundary: CfirClassInitializationInfo?,
)

/** 独立静态执行的结果；首次 return 的原始义务只保存来源，不替换经 finally 的构造事实。 */
class CfirNativeConstructorInitializationDiagnostics(
    val constructor: CfirConstructor,
    val ordinal: Int,
    val availability: CfirConstructorInitializationAvailability,
    val flow: CfirConstructorInitializationFlow,
    endFields: Map<CfirBasedSymbol<*>, CfirFieldInitializationState>,
    firstReturnUninitialized: Map<CfirBasedSymbol<*>, CfirExpression>,
) {
    val endFields = freezeFields(endFields)
    val firstReturnUninitialized: Map<CfirBasedSymbol<*>, CfirExpression> =
        Collections.unmodifiableMap(IdentityHashMap(firstReturnUninitialized))
}

/**
 * Native 静态检查的有序末边界与继承残余义务。它和全正常构造出口的交集是两个产品；
 * 本数据没有 INITIALIZED 发布权限，不能用末次 ctor 的静态 true 覆盖真实构造 false。
 */
class CfirNativeClassInitializationDiagnostics(
    val availability: CfirNativeInitializationDiagnosticAvailability,
    fields: Map<CfirBasedSymbol<*>, CfirFieldInitializationState>,
    constructors: Map<CfirConstructorSymbol, CfirNativeConstructorInitializationDiagnostics>,
    obligations: List<CfirNativeFieldInitializationObligation>,
) {
    val fields = freezeFields(fields)
    val constructors: Map<CfirConstructorSymbol, CfirNativeConstructorInitializationDiagnostics> =
        Collections.unmodifiableMap(IdentityHashMap(constructors))
    val obligations: List<CfirNativeFieldInitializationObligation> = Collections.unmodifiableList(ArrayList(obligations))
}

/**
 * 构造执行的分阶段输入：实参在 beforeDelegation 中求值；只有调用正常返回后才应用
 * afterDelegation 并执行本 owner 初始化器。this 委托不会重复执行初始化器。
 */
/** 初始化 owner 的角色与本 purpose 执行初始化器的位置分开；静态准备不是“无 owner 初始化”。 */
enum class CfirConstructorInitializerStage {
    EXECUTE_AFTER_DELEGATION,
    STATIC_PREPARED_BEFORE_DELEGATION,
    NONE_THIS,
    UNAVAILABLE,
}

class CfirConstructorInitializationFlow(
    val delegationCall: CfirFunctionCall?,
    beforeDelegation: Map<CfirBasedSymbol<*>, CfirFieldInitializationState>,
    afterDelegation: Map<CfirBasedSymbol<*>, CfirFieldInitializationState>,
    val delegationReturnsNormally: Boolean,
    val initializesOwner: Boolean,
    initializerFields: List<CfirFieldVariable>,
    parameterFields: List<CfirFieldVariable>,
    val initializerStage: CfirConstructorInitializerStage,
) {
    init {
        when (initializerStage) {
            CfirConstructorInitializerStage.EXECUTE_AFTER_DELEGATION,
            CfirConstructorInitializerStage.STATIC_PREPARED_BEFORE_DELEGATION -> require(initializesOwner)
            CfirConstructorInitializerStage.NONE_THIS -> require(!initializesOwner)
            CfirConstructorInitializerStage.UNAVAILABLE -> require(!delegationReturnsNormally)
        }
    }
    val beforeDelegation = freezeFields(beforeDelegation)
    val afterDelegation = freezeFields(afterDelegation)
    val initializerFields: List<CfirFieldVariable> = Collections.unmodifiableList(ArrayList(initializerFields))
    val parameterFields: List<CfirFieldVariable> = Collections.unmodifiableList(ArrayList(parameterFields))
}

/** 原构造器的入口和全部正常出口；throw 路径不产生正常构造出口。 */
class CfirConstructorInitializationInfo(
    val constructor: CfirConstructor,
    val availability: CfirConstructorInitializationAvailability,
    val delegationTarget: CfirConstructorSymbol?,
    val receiverOwner: CfirThisOwnerSymbol<*>,
    val flow: CfirConstructorInitializationFlow,
    normalExitFields: List<Map<CfirBasedSymbol<*>, CfirFieldInitializationState>>,
    terminalFields: Map<CfirBasedSymbol<*>, CfirFieldInitializationState>,
) {
    val normalExitFields = Collections.unmodifiableList(normalExitFields.map(::freezeFields))
    /** 完成 body 的状态独立保存；它不把 throw/不可用路径伪装为正常构造出口。 */
    val terminalFields = freezeFields(terminalFields)
    val hasNormalExit: Boolean get() = normalExitFields.isNotEmpty()
}

/**
 * 一个实际 class-like owner 的完成初始化产品。原 fields/constructors 与实例化 scope
 * 的声明身份分开保存；输入指纹不含输出 INITIALIZED 位，避免发布自身导致反复退役。
 * 数据按 checker 的实际会话保存，普通源/依赖会话不能混用同名 owner 的结果。
 */
class CfirClassInitializationInfo(
    val owner: CfirClassLikeDeclaration,
    val useSiteSession: CfirSession,
    val origin: CfirClassInitializationOrigin,
    members: List<CfirDeclaration>,
    parents: List<CfirClassInitializationInfo>,
    fields: Map<CfirBasedSymbol<*>, CfirFieldInitializationState>,
    initialFields: Map<CfirBasedSymbol<*>, CfirFieldInitializationState>,
    constructors: Map<CfirConstructorSymbol, CfirConstructorInitializationInfo>,
    fieldAliases: Map<CfirBasedSymbol<*>, CfirBasedSymbol<*>>,
    val nativeStaticDiagnostics: CfirNativeClassInitializationDiagnostics,
) {
    val members: List<CfirDeclaration> = Collections.unmodifiableList(ArrayList(members))
    val parents: List<CfirClassInitializationInfo> = Collections.unmodifiableList(ArrayList(parents))
    val fields = freezeFields(fields)
    val initialFields = freezeFields(initialFields)
    val constructors: Map<CfirConstructorSymbol, CfirConstructorInitializationInfo> =
        Collections.unmodifiableMap(IdentityHashMap(constructors))
    /** 已接受的 common→specific 字段关系；它不通过名称匹配声明。 */
    val fieldAliases: Map<CfirBasedSymbol<*>, CfirBasedSymbol<*>> = Collections.unmodifiableMap(IdentityHashMap(fieldAliases))
    val hasNormalConstructionExit: Boolean get() = constructors.values.any { it.hasNormalExit }
    val hasUnavailableConstructors: Boolean get() = constructors.values.any {
        it.availability != CfirConstructorInitializationAvailability.COMPLETE
    }

    /** 声明使用点先归回实际 storage，再应用已接受的字段实现关系。 */
    fun fieldState(symbol: CfirBasedSymbol<*>): CfirFieldInitializationState {
        val original = if (symbol is CfirVariableSymbol<*> && symbol.isBound) symbol.unwrapSubstitutionOverrides() else symbol
        return fields.getValue(fieldAliases[original] ?: original)
    }

    private val physicalMembers = owner.declarations.toList()
    private val graph = (owner as? CfirControlFlowGraphOwner)?.controlFlowGraphReference
    private val constructorBodies: Map<CfirConstructor, CfirBlock?> = members.filterIsInstance<CfirConstructor>().associateWith { it.body }
    private val constructorGraphs: Map<CfirConstructor, CfirControlFlowGraphReference?> =
        members.filterIsInstance<CfirConstructor>().associateWith { it.controlFlowGraphReference }
    private val initializers: Map<CfirFieldVariable, CfirExpression?> =
        members.filterIsInstance<CfirFieldVariable>().associateWith { it.initializer }

    /** 只检查已捕获的输入身份；不进入 provider、别名解析或正文材料化。 */
    fun isCurrent(): Boolean = physicalMembers == owner.declarations &&
        graph === (owner as? CfirControlFlowGraphOwner)?.controlFlowGraphReference &&
        constructorBodies.all { (constructor, body) -> constructor.body === body } &&
        constructorGraphs.all { (constructor, reference) -> constructor.controlFlowGraphReference === reference } &&
        initializers.all { (field, initializer) -> field.initializer === initializer } &&
        parents.all { it.owner.classInitializationInfo(useSiteSession) === it && it.isCurrent() }

    fun matches(session: CfirSession, members: List<CfirDeclaration>, parents: List<CfirClassInitializationInfo>,
        fieldAliases: Map<CfirBasedSymbol<*>, CfirBasedSymbol<*>>): Boolean =
        useSiteSession === session && this.members == members && this.parents == parents && this.fieldAliases == fieldAliases && isCurrent()
}

private fun freezeFields(values: Map<CfirBasedSymbol<*>, CfirFieldInitializationState>): Map<CfirBasedSymbol<*>, CfirFieldInitializationState> =
    Collections.unmodifiableMap(IdentityHashMap(values))

private var CfirClassLikeDeclaration.classInitializationInfos: Map<CfirSession, CfirClassInitializationInfo>?
    by CfirDeclarationDataRegistry.data(CfirClassInitializationInfoKey)

fun CfirClassLikeDeclaration.classInitializationInfo(session: CfirSession): CfirClassInitializationInfo? =
    classInitializationInfos?.get(session)

/** 调用方在实际 owner 的完成事务中发布；旧会话产品保留自己的身份和输入边界。 */
fun CfirClassLikeDeclaration.publishClassInitializationInfo(info: CfirClassInitializationInfo) {
    require(info.owner === this)
    synchronized(this) {
        classInitializationInfos = Collections.unmodifiableMap(IdentityHashMap<CfirSession, CfirClassInitializationInfo>().apply {
            classInitializationInfos?.let(::putAll)
            put(info.useSiteSession, info)
        })
    }
}
