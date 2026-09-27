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
 * extend 直接接口的稳定源码 occurrence。
 *
 * 重复接口诊断的 owner 是接口引用 occurrence，而不是整个 extend 声明。
 * [semanticKey] 保留带约束的完整语义键，供既有特化规则使用；
 * [duplicateSemanticKey] 使用不含 where bound 的 alpha-equivalent 键，专门用于
 * 官方 `CheckExtendInterfaces` 的跨 extend 重复接口汇总。
 */
data class CfirExtendInterfaceOccurrence(
    /** 该接口引用在所属 extend 的 supertype 列表中的下标。 */
    val superTypeIndex: Int,
    /** 可解析的接口 ClassId。无效或非接口引用不会进入 occurrence 索引。 */
    val classId: ClassId,
    /** 带完整约束信息的接口语义键。 */
    val semanticKey: String,
    /** 用于重复接口汇总的无 bounds、alpha-equivalent 语义键。 */
    val duplicateSemanticKey: String,
    /** 同一重复目标身份下该接口 occurrence 的总数。 */
    val occurrenceCount: Int,
    /** 当前引用是否为该接口分组的最终 occurrence。 */
    val isLastOccurrence: Boolean,
)

/**
 * extend 目标接口视图。
 *
 * 同一份目标父类型图会被不同规则以不同边界消费：重复接口检查排除当前 extend 自身，
 * orphan rule 则只接受目标声明继承的接口和其他包 extend 已经引入的接口。视图选择由
 * 查询服务统一解释，checker 不得重新遍历 provider 或按 primitive/class 分类补接口。
 */
enum class CfirExtendTargetInterfaceView {
    /**
     * 重复接口检查的目标基线。
     *
     * 目标声明的 nominal 父图按官方 `GetAllSuperTys` 语义展开；同一目标上其它
     * extend 只贡献直接写出的接口，不把该接口的父接口当作另一个 extend occurrence。
     */
    DUPLICATE_BASELINE,

    /**
     * orphan rule 的目标基线。
     *
     * 目标的 nominal 声明父图不混入 extend 边；其他包的 direct/indirect extend 贡献
     * 直接接口后，再展开该接口的 nominal 父接口闭包。
     */
    ORPHAN_BASELINE,
}

/**
 * 当前 extend owner 检查时，另一个同目标 extend 对成员有效图的参与方式。
 */
enum class CfirExtendMemberPeerDisposition {
    /** peer 成员进入当前 owner 的有效成员图。 */
    INCLUDE,

    /** peer 是当前 owner 的严格子接口扩展，当前 owner 按官方顺序先检查并排除 peer。 */
    EXCLUDE,

    /** 接口继承关系无法决定检查顺序；调用方报告顺序诊断并按 nullopt 继续收集 peer。 */
    UNDECIDABLE,
}

/**
 * 已实例化 peer 顺序的结构化结果。
 */
data class CfirExtendMemberPeerDecision(
    /** 当前 owner 检查对 peer 的纳入判定。 */
    val disposition: CfirExtendMemberPeerDisposition,
    /** 顺序冲突的直接接口实例证据；其他 disposition 不携带冲突证据。 */
    val conflictWitness: CfirExtendMemberPeerConflictWitness? = null,
) {
    init {
        require((disposition == CfirExtendMemberPeerDisposition.UNDECIDABLE) == (conflictWitness != null)) {
            "Only undecidable extend peer order carries a conflict witness"
        }
    }
}

/** owner/peer 的直接接口实例，供顺序诊断与测试保留真实泛型替换上下文。 */
data class CfirExtendMemberPeerConflictWitness(
    /** current owner 的直接接口实例列表。 */
    val ownerInterfaces: List<CfirExtendInheritedInterfaceSemantic>,
    /** peer 的直接接口实例列表。 */
    val peerInterfaces: List<CfirExtendInheritedInterfaceSemantic>,
    /** cjc peer note 使用的一个或两个子接口到父接口关系链。 */
    val conflictChains: List<CfirExtendMemberPeerConflictChain>,
) {
    init {
        require(conflictChains.isNotEmpty()) { "Undecidable extend order requires concrete subtype witness chains" }
    }
}

/** 一个 owner/peer 直接接口之间的子类型关系。 */
data class CfirExtendMemberPeerConflictRelation(
    /** 子接口实例。 */
    val subInterface: CfirExtendInheritedInterfaceSemantic,
    /** 父接口实例。 */
    val superInterface: CfirExtendInheritedInterfaceSemantic,
)

/** cjc AddNote 中的一条接口冲突链；跨 owner 接口顺序冲突分别携带两条单边链。 */
data class CfirExtendMemberPeerConflictChain(
    /** 依次相连的子接口到父接口关系。 */
    val relations: List<CfirExtendMemberPeerConflictRelation>,
) {
    init {
        require(relations.isNotEmpty()) { "An interface conflict chain must contain a subtype relation" }
    }
}

/** source-owner 顺序诊断的 peer 声明及其接口证据。 */
data class CfirExtendMemberPeerSequenceConflict(
    /** peer declaration 用 Any 保持 cfir-common 不依赖具体声明实现模块。 */
    val peerDeclaration: Any,
    /** owner 与 peer 的直接接口实例冲突证据。 */
    val witness: CfirExtendMemberPeerConflictWitness,
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
     * 返回 [declaration] 指定直接接口的重复检查 occurrence 信息。
     *
     * 索引层已经按 extend 源码稳定顺序、再按 supertype 出现顺序完成分组和 owner 选择；
     * checker 不得重新按 declaration 粒度推断诊断位置。
     */
    fun duplicateInterfaceOccurrenceOf(
        declaration: Any,
        superTypeIndex: Int,
    ): CfirExtendInterfaceOccurrence?

    /**
     * 返回 [declaration] 目标在指定 [view] 下已经具备的实例化接口闭包。
     *
     * 结果来自统一的类型感知父类型图，包含声明继承、superclass 传播和依赖库 extend，
     * 并使用当前 extend 的类型参数语义空间生成稳定 key。
     */
    fun targetAvailableInterfacesOf(
        declaration: Any,
        view: CfirExtendTargetInterfaceView,
    ): List<CfirExtendInheritedInterfaceSemantic>

    /**
     * 返回 [declaration] 对应 extend 的 nominal 目标类标识。
     */
    fun targetClassIdOf(declaration: Any): ClassId?

    /**
     * 返回 [declaration] 所在文件的包名。
     */
    fun packageFqNameOf(declaration: Any): FqName

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
     * 判断 [childDeclaration] 的直接接口中是否存在继承自 [parentDeclaration] 直接接口的接口。
     */
    fun doesExtendInheritFrom(childDeclaration: Any, parentDeclaration: Any): Boolean

    /**
     * 返回官方 `DeterminingSkipExtendByInheritanceRelationship` 对一对 owner/peer 的结果。
     *
     * 该查询比较实例化后的直接接口类型，保留同一接口与同一 ClassId 下不同类型实参的区别。
     * `UNDECIDABLE` 对应官方 `nullopt`：source-owner 或 concrete-use checker 报告顺序诊断，
     * 成员图仍纳入 peer。
     */
    fun extendMemberPeerDecision(
        ownerDeclaration: Any,
        peerDeclaration: Any,
    ): CfirExtendMemberPeerDecision

    /**
     * 判断 [declaration] 与同目标其他 extend 的检查顺序是否存在无法决定的继承关系。
     */
    fun hasUndecidableExtendCheckSequence(declaration: Any): Boolean

    /** 返回 source-owner 顺序诊断所需的 peer 声明和接口证据。 */
    fun undecidableExtendCheckSequenceConflicts(declaration: Any): List<CfirExtendMemberPeerSequenceConflict>

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
     * 获取 extend 继承接口连同其传递父接口的语义列表（实例化后）。
     */
    fun inheritedInterfaceClosureOf(declaration: Any): List<CfirExtendInheritedInterfaceSemantic>

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
