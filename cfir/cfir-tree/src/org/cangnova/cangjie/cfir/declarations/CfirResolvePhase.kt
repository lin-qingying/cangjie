package org.cangnova.cangjie.cfir.declarations

/**
 * CFIR 语义解析阶段定义（Resolve Phase Model）。
 *
 * 该枚举描述了 CFIR 从“仅有结构”到“语义可消费”的渐进式解析过程。
 * 其设计目标是同时支持：
 *
 * 1. 全量编译模式（CLI）
 * - 按阶段批处理：先对所有声明执行阶段 A，再执行阶段 B。
 *
 * 2. 按需解析模式（IDE / Analysis）
 * - 对单个声明按需推进到目标阶段（lazy resolve）。
 *
 * -----------------------------------------------------------------------------
 * 一、执行模型
 * -----------------------------------------------------------------------------
 *
 * 全量模式可抽象为：
 * ```kotlin
 * for (phase in CfirResolvePhase.entries) {
 *     for (declaration in allDeclarations) {
 *         runPhase(declaration, phase)
 *     }
 * }
 * ```
 *
 * 按需模式可抽象为：
 * ```kotlin
 * fun resolveTo(target: CfirDeclaration, goal: CfirResolvePhase) {
 *     for (phase in CfirResolvePhase.entries) {
 *         if (phase > goal) break
 *         runPhase(target, phase)
 *     }
 * }
 * ```
 *
 * -----------------------------------------------------------------------------
 * 二、阶段契约（必须满足）
 * -----------------------------------------------------------------------------
 *
 * 1) 顺序性（Sequential Only）
 * - 任何声明只能按阶段顺序推进，不能跳过前置阶段直接进入后置阶段。
 *
 * 2) 单调性（Monotonic）
 * - 阶段只能前进，不能回退。
 *
 * 3) 幂等性（Idempotent）
 * - 对已经达到某阶段的声明再次执行该阶段，不应改变语义结果。
 *
 * 4) 边界可见性（No Higher-Phase Read）
 * - 在阶段 P 中，不得依赖高于 P 的信息。
 * - 例如在 [STATUS] 阶段，不应依赖 [IMPLICIT_TYPES] 或 [BODY_RESOLVE] 才会计算出的信息。
 *
 * 5) 可重现性（Deterministic）
 * - 相同输入与依赖图下，阶段结果应稳定可重现。
 *
 * -----------------------------------------------------------------------------
 * 三、Jumping Phase 约定
 * -----------------------------------------------------------------------------
 *
 * “Jumping phase”指：在处理当前声明时，可以请求其他声明的阶段信息以满足当前阶段推理。
 *
 * 注意：
 * - jumping phase 不等于一定允许“同阶段”请求。
 * - 本枚举不再使用 isJumpingPhase 等结构性标记。
 * - 哪些阶段属于 jumping phase，由文档约定与 resolver 行为共同定义。
 * - 是否允许同阶段请求，由 [isItAllowedToCallLazyResolveToTheSamePhase] 决定。
 *
 * 当前定义中，下列阶段被视为 jumping phase：
 * - [SUPER_TYPES]
 * - [STATUS]
 * - [EXTENSIONS]
 * - [IMPLICIT_TYPES]
 * - [BODY_RESOLVE]
 *
 * 其中当前允许 same-phase lazy resolve 的阶段为：
 * - [SUPER_TYPES]
 * - [STATUS]
 * - [IMPLICIT_TYPES]
 * - [BODY_RESOLVE]
 *
 * [EXTENSIONS] 当前仍视为 jumping phase，但不允许 same-phase lazy resolve，
 * 以避免扩展链递归在尚无稳定收敛策略时引入死循环风险。
 *
 * -----------------------------------------------------------------------------
 * 四、非本枚举职责
 * -----------------------------------------------------------------------------
 *
 * 本枚举仅覆盖 resolve 语义流水线，不覆盖 FINALIZE 侧流程。
 * 以下内容不属于 [CfirResolvePhase]：
 * - 语义后脱糖
 * - 泛型实例化
 * - 溢出策略标注
 *
 * -----------------------------------------------------------------------------
 * 五、终态语义
 * -----------------------------------------------------------------------------
 *
 * 到达 [CHECKERS] 表示“resolve 语义终态”：
 * - resolve 范围内诊断已产出
 * - 声明可被 resolve 消费方视为语义完成
 */
enum class CfirResolvePhase(
    /**
     * 是否为“无处理器阶段”。
     *
     * `true` 表示该阶段是状态锚点（state marker），通常不期望有独立 processor。
     */
    val noProcessor: Boolean = false,
) {
    /**
     * 原始 CFIR 已构建（仅结构）。
     *
     * 输入：
     * - PSI / LightTree 到 CFIR 的转换完成。
     *
     * 输出：
     * - 声明结构、符号壳、基本语法信息可访问。
     * - 尚不承诺导入绑定、类型解析、函数体语义。
     */
    RAW_CFIR(noProcessor = true),

    /**
     * 导入绑定阶段。
     *
     * 输入：
     * - [RAW_CFIR] 完成。
     *
     * 输出：
     * - import 对应的包/符号可定位，为后续名称解析提供候选空间。
     */
    IMPORTS,

    /**
     * 父类型图准备阶段。
     *
     * 输入：
     * - [IMPORTS] 完成。
     *
     * 输出：
     * - 父类/父接口关系可用于继承层次检查与后续类型解析。
     *
     * 说明：
     * - 该阶段被视为 jumping phase。
     * - 该阶段通常需要沿继承链递归推进其他声明的 [SUPER_TYPES]。
     */
    SUPER_TYPES,

    /**
     * 显式类型解析阶段。
     *
     * 输入：
     * - [SUPER_TYPES] 完成。
     *
     * 输出：
     * - 声明头中的显式类型（参数、返回、字段、类型引用）完成解析或产生诊断。
     */
    TYPES,

    /**
     * 声明状态与修饰符规范化阶段。
     *
     * 输入：
     * - [TYPES] 完成。
     *
     * 输出：
     * - 可见性、修饰符、状态约束完成校验并标准化。
     *
     * 说明：
     * - 该阶段被视为 jumping phase。
     * - 当成员需要对其父声明进行 override / abstract / final 等判定时，
     *   当前阶段可能需要请求父声明的 [STATUS]。
     */
    STATUS,

    /**
     * 仓颉扩展声明解析阶段。
     *
     * 输入：
     * - [STATUS] 完成。
     *
     * 输出：
     * - `extend Type <: Interface` 约束完成绑定，可供后续推断与体解析使用。
     *
     * 说明：
     * - 该阶段被视为 jumping phase。
     * - 该阶段可能需要跳转查询被扩展目标或相关扩展声明。
     * - 当前实现默认不开放 same-phase 请求，避免在扩展链尚未建立完整收敛策略前引入递归复杂度。
     */
    EXTENSIONS,

    /**
     * 声明边界隐式类型推断阶段。
     *
     * 输入：
     * - [EXTENSIONS] 完成。
     *
     * 输出：
     * - 省略的声明级类型完成推断（或诊断）。
     * - 为 [BODY_RESOLVE] 提供稳定的声明边界类型。
     *
     * 说明：
     * - 该阶段被视为 jumping phase。
     */
    IMPLICIT_TYPES,

    /**
     * 函数体语义解析阶段。
     *
     * 输入：
     * - [IMPLICIT_TYPES] 完成。
     *
     * 输出：
     * - 表达式类型、调用绑定、控制流相关语义信息可用。
     *
     * 说明：
     * - 该阶段被视为 jumping phase。
     */
    BODY_RESOLVE;


    /**
     * 返回下一个阶段；若当前已是末阶段，则返回自身。
     */
    val next: CfirResolvePhase
        get() {
            val values = entries
            val nextOrdinal = ordinal + 1
            return if (nextOrdinal < values.size) values[nextOrdinal] else this
        }

    /**
     * 返回上一个阶段；若当前已是首阶段，则返回自身。
     */
    val previous: CfirResolvePhase
        get() {
            val prevOrdinal = ordinal - 1
            return if (prevOrdinal >= 0) entries[prevOrdinal] else this
        }
}

/**
 * 判定当前阶段是否允许请求到 [requestedPhase]。
 *
 * 规则：
 * - 允许请求更低阶段
 * - 同阶段是否允许由 [isItAllowedToCallLazyResolveToTheSamePhase] 决定
 * - 禁止请求更高阶段
 */
fun CfirResolvePhase.isItAllowedToCallLazyResolveTo(requestedPhase: CfirResolvePhase): Boolean = when {
    this > requestedPhase -> true
    this == requestedPhase -> isItAllowedToCallLazyResolveToTheSamePhase
    else -> false
}

/**
 * 是否允许“同阶段”请求（same-phase lazy resolve）。
 *
 * 说明：
 * - 并非所有 jumping phase 都允许同阶段请求。
 * - [SUPER_TYPES] 允许同阶段请求，以支持继承链递归展开与父类型图构建。
 * - [STATUS] 允许同阶段请求，以支持 override / modality / visibility 在父声明上的联动判定。
 * - [IMPLICIT_TYPES] 与 [BODY_RESOLVE] 允许同阶段请求，
 *   以支持隐式类型与函数体推断中的相互依赖场景。
 * - [EXTENSIONS] 当前仍不开放 same-phase 请求，避免扩展链递归在尚无稳定收敛策略时引入死循环风险。
 */
val CfirResolvePhase.isItAllowedToCallLazyResolveToTheSamePhase: Boolean
    get() = when (this) {
        CfirResolvePhase.SUPER_TYPES,
        CfirResolvePhase.STATUS,
        CfirResolvePhase.IMPLICIT_TYPES,
        CfirResolvePhase.BODY_RESOLVE -> true
        else -> false
    }