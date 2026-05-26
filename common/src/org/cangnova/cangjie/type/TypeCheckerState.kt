package org.cangnova.cangjie.type

import org.cangnova.cangjie.type.model.CangJieTypeMarker
import org.cangnova.cangjie.type.model.RigidTypeMarker
import org.cangnova.cangjie.type.model.TypeCheckerProviderContext
import org.cangnova.cangjie.type.model.TypeSystemContext
import java.util.ArrayDeque

/**
 * 类型检查状态
 *
 * 在一次类型检查（子类型判断、类型推断约束求解）过程中持有所有上下文状态。
 * 包括：
 * - 错误类型/存根类型的宽松匹配策略
 * - 父类型遍历队列（避免重复访问）
 * - 约束系统的分叉点（forking point）支持
 *
 * 改造说明：
 * - 删除 isDnnTypesEqualToFlexible：仓颉无 DefinitelyNotNull 类型（T & Any 语法）
 * - 删除 SupertypesPolicy.UpperIfFlexible / LowerIfFlexible：仓颉无弹性类型
 *   父类型遍历策略只需 None（跳过）和 DoCustomTransform（自定义）
 * - 新增 SupertypesPolicy.Direct：直接使用父类型（刚性类型唯一合理的默认策略）
 * - 删除 addSubtypeConstraint 的 isFromNullabilityConstraint 参数：仓颉无可空性约束
 */
open class TypeCheckerState(
    /** 错误类型是否与任意类型相等（宽松模式，用于错误恢复阶段） */
    val isErrorTypeEqualsToAnything: Boolean,
    /** 存根类型是否与任意类型相等（推断中间阶段的宽松模式） */
    val isStubTypeEqualsToAnything: Boolean,
    /** 是否允许类型变量参与子类型判断 */
    val allowedTypeVariable: Boolean,
    /** 类型系统上下文，提供所有类型操作 */
    val typeSystemContext: TypeSystemContext,
    /** 类型预处理器，在比较前对类型做规范化处理 */
    val cangjieTypePreparator: AbstractTypePreparator,
    /** 类型精化器，用于将类型精化为更具体的形式（如展开类型别名） */
    val cangjieTypeRefiner: AbstractTypeRefiner,
) {
    /** 使用精化器对类型进行精化 */
    fun refineType(type: CangJieTypeMarker): CangJieTypeMarker =
        cangjieTypeRefiner.refineType(type)

    /** 使用预处理器对类型进行规范化 */
    fun prepareType(type: CangJieTypeMarker): CangJieTypeMarker =
        cangjieTypePreparator.prepareType(type)

    /**
     * 自定义子类型判断钩子
     * 子类可覆盖此方法在标准子类型算法之外添加额外约束
     * 默认返回 true（不附加额外约束）
     */
    open fun customIsSubtypeOf(
        subType: CangJieTypeMarker,
        superType: CangJieTypeMarker,
    ): Boolean = true

    /** 当前泛型实参递归深度，防止无限递归 */
    protected var argumentsDepth = 0

    /**
     * 在泛型实参子类型检查的上下文中执行代码块
     * 超过深度限制时抛出错误，防止无限递归
     */
    internal inline fun <T> runWithArgumentsSettings(
        subArgument: CangJieTypeMarker,
        f: TypeCheckerState.() -> T,
    ): T {
        if (argumentsDepth > 100) {
            error("泛型实参递归深度过大，相关实参类型：$subArgument")
        }
        argumentsDepth++
        val result = f()
        argumentsDepth--
        return result
    }

    /**
     * 向约束系统添加子类型约束
     * 返回 null 表示不处理（由上层决定），返回 Boolean 表示约束是否成立
     * 子类在推断模式下覆盖此方法收集约束
     */
    open fun addSubtypeConstraint(
        subType: CangJieTypeMarker,
        superType: CangJieTypeMarker,
    ): Boolean? = null

    /**
     * 执行分叉点（Forking Point）逻辑
     *
     * 分叉点用于重载解析等需要"尝试多个可能性，有一个成功即可"的场景。
     * 默认实现使用短路求值：第一个成功的分支即为结果。
     */
    open fun runForkingPoint(block: ForkPointContext.() -> Unit): Boolean =
        with(ForkPointContext.Default()) {
            block()
            result
        }

    /** 分叉点上下文，允许注册多个候选分支 */
    interface ForkPointContext {
        /** 注册一个候选分支，若之前的分支已成功则跳过 */
        fun fork(block: () -> Boolean)

        /** 默认实现：短路求值，第一个返回 true 的分支即为最终结果 */
        class Default : ForkPointContext {
            var result: Boolean = false
            override fun fork(block: () -> Boolean) {
                if (result) return
                result = block()
            }
        }
    }

    /**
     * 捕获类型下界的处理策略
     * 控制子类型检查时是否以及如何使用捕获类型的下界
     */
    enum class LowerCapturedTypePolicy {
        /** 仅检查下界 */
        CHECK_ONLY_LOWER,
        /** 同时检查子类型关系和下界 */
        CHECK_SUBTYPE_AND_LOWER,
        /** 跳过下界检查 */
        SKIP_LOWER,
    }

    // =====================================================================
    // 父类型遍历状态（BFS）
    // 使用双端队列 + 已访问集合实现广度优先父类型遍历
    // =====================================================================

    private var supertypesLocked = false

    var supertypesDeque: ArrayDeque<RigidTypeMarker>? = null
        private set

    var supertypesSet: MutableSet<RigidTypeMarker>? = null
        private set

    /** 初始化父类型遍历状态（加锁防止重入） */
    fun initialize() {
        check(!supertypesLocked) { "父类型遍历状态已被锁定：${this::class}" }
        supertypesLocked = true
        if (supertypesDeque == null) supertypesDeque = ArrayDeque(4)
        if (supertypesSet == null) supertypesSet = mutableSetOf()
    }

    /** 清空父类型遍历状态并解锁 */
    fun clear() {
        supertypesDeque!!.clear()
        supertypesSet!!.clear()
        supertypesLocked = false
    }

    /**
     * 广度优先遍历从 [start] 开始的所有父类型，判断是否存在满足 [predicate] 的父类型
     *
     * @param start            起始刚性类型
     * @param predicate        对每个父类型的判断谓词，返回 true 则立即终止并返回 true
     * @param supertypesPolicy 对每个类型决定遍历策略（是否继续、如何变换类型）
     * @return 若存在满足谓词的父类型则返回 true，否则返回 false
     */
    inline fun anySupertype(
        start: RigidTypeMarker,
        predicate: (RigidTypeMarker) -> Boolean,
        supertypesPolicy: (RigidTypeMarker) -> SupertypesPolicy,
    ): Boolean {
        if (predicate(start)) return true

        initialize()

        val deque = supertypesDeque!!
        val visitedSupertypes = supertypesSet!!

        deque.push(start)
        while (deque.isNotEmpty()) {
            val current = deque.pop()
            if (!visitedSupertypes.add(current)) continue

            // 获取遍历策略，None 表示跳过此节点的父类型
            val policy = supertypesPolicy(current)
                .takeIf { it != SupertypesPolicy.None } ?: continue

            val supertypes = with(typeSystemContext) {
                current.typeConstructor().supertypes()
            }
            for (supertype in supertypes) {
                val newType = policy.transformType(this, supertype)
                if (predicate(newType)) {
                    clear()
                    return true
                }
                deque.add(newType)
            }
        }

        clear()
        return false
    }

    /**
     * 父类型遍历策略
     *
     * 仓颉无弹性类型，因此删除了 UpperIfFlexible / LowerIfFlexible。
     * 策略简化为：
     * - [None]：跳过，不继续遍历该节点的父类型
     * - [Direct]：直接使用父类型（刚性类型的标准遍历策略）
     * - [DoCustomTransform]：自定义变换，供子类扩展
     */
    sealed class SupertypesPolicy {

        /**
         * 将父类型变换为用于遍历的刚性类型
         * 仓颉所有类型均为刚性类型，此处直接转型
         */
        abstract fun transformType(
            state: TypeCheckerState,
            type: CangJieTypeMarker,
        ): RigidTypeMarker

        /** 跳过策略：不遍历该节点的父类型 */
        object None : SupertypesPolicy() {
            override fun transformType(
                state: TypeCheckerState,
                type: CangJieTypeMarker,
            ): RigidTypeMarker = error("None 策略不应调用 transformType")
        }

        /**
         * 直接策略：将父类型直接作为刚性类型使用
         * 仓颉所有类型均为刚性类型，这是标准的父类型遍历策略
         */
        object Direct : SupertypesPolicy() {
            override fun transformType(
                state: TypeCheckerState,
                type: CangJieTypeMarker,
            ): RigidTypeMarker = with(state.typeSystemContext) {
                type.asRigidType()
                    ?: error("仓颉类型系统中所有类型均应为刚性类型，遇到意外类型：$type")
            }
        }

        /** 自定义变换策略基类，供需要特殊父类型处理逻辑的场景继承 */
        abstract class DoCustomTransform : SupertypesPolicy()

        /** 仓颉无弹性类型，直接将超类型转为刚性类型 */
        object RigidOnly : SupertypesPolicy() {
            override fun transformType(state: TypeCheckerState, type: CangJieTypeMarker): RigidTypeMarker =
                with(state.typeSystemContext) {
                    type.asRigidType()
                        ?: error("仓颉类型系统中所有超类型都应可规约为刚性类型，遇到意外类型：$type")
                }
        }
    }

    /**
     * 判断给定类型是否是被允许参与推断的类型变量
     */
    fun isAllowedTypeVariable(type: CangJieTypeMarker): Boolean =
        allowedTypeVariable && with(typeSystemContext) { type.isTypeVariableType() }
}

// =====================================================================
// 扩展函数：便捷创建 TypeCheckerState
// =====================================================================

/**
 * 创建新的类型检查状态
 *
 * 要求实现类同时实现 [TypeSystemContext]，否则抛出错误。
 *
 * @param errorTypesEqualToAnything 错误类型是否与任意类型相等
 * @param stubTypesEqualToAnything  存根类型是否与任意类型相等
 * @param allowedTypeVariable       是否允许类型变量参与子类型判断
 * @param cangjieTypePreparator     类型预处理器（默认为无操作）
 * @param cangjieTypeRefiner        类型精化器（默认为无操作）
 */
fun TypeCheckerProviderContext.newTypeCheckerState(
    errorTypesEqualToAnything: Boolean,
    stubTypesEqualToAnything: Boolean,
    allowedTypeVariable: Boolean = false,
    cangjieTypePreparator: AbstractTypePreparator = AbstractTypePreparator.Default,
    cangjieTypeRefiner: AbstractTypeRefiner = AbstractTypeRefiner.Default,
): TypeCheckerState = TypeCheckerState(
    isErrorTypeEqualsToAnything = errorTypesEqualToAnything,
    isStubTypeEqualsToAnything = stubTypesEqualToAnything,
    allowedTypeVariable = allowedTypeVariable,
    typeSystemContext = (this as? TypeSystemContext)
        ?: error("TypeCheckerProviderContext 的实现类必须同时实现 TypeSystemContext"),
    cangjieTypePreparator = cangjieTypePreparator,
    cangjieTypeRefiner = cangjieTypeRefiner,
)
