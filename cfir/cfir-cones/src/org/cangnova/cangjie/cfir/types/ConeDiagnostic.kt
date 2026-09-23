package org.cangnova.cangjie.cfir.types

/**
 * [ConeErrorType] 携带的结构化诊断。
 *
 * resolve/checker 可以直接模式匹配诊断对象，不需要解析错误字符串。
 */
interface ConeDiagnostic {
    /**
     * 面向调试和兜底展示的诊断原因文本。
     */
    val reason: String

    /**
     * 将诊断作为类型构造器展示时使用的可读描述。
     */
    val readableDescriptionAsTypeConstructor: String get() = reason

}

/**
 * 错误类型仍能可靠识别 nominal owner 的诊断。
 *
 * 该标记只表示检查器可以读取 delegated nominal type 来抑制派生诊断，或执行
 * final class 等独立规则；它不表示错误父类型可以进入继承图或成员作用域。
 */
interface ConeRecoverableNominalDiagnostic : ConeDiagnostic

/**
 * classifier 类型使用已经解析到多个同层声明，不能选择任一候选作为声明父类型。
 *
 * 该标记位于 cones 公共层，使 providers 能结构化识别父类型 classifier 歧义，
 * 而不依赖 resolve/semantics 中的具体诊断类，也不解析诊断文本。
 */
interface ConeClassifierAmbiguityDiagnostic : ConeDiagnostic

/**
 * 类型 Join 失败：公共父类型候选集合中不存在唯一最小候选。
 *
 * 对齐官方 cjc `FindSmallestTy` 返回 InvalidTy 的语义（对应诊断
 * `sema_incompatible_func_body_and_return_type`，消息为 "The types 'X' and 'Y'
 * do not have the smallest common supertype"）。该标记位于 cones 公共层，
 * 使 checkers 能结构化识别 Join 失败诊断，而不依赖解析诊断文本。
 */
interface ConeNoSmallestCommonSupertypeDiagnostic : ConeDiagnostic

/**
 * 不会被重复上报的诊断包装。
 *
 * 当 type ref、reference 等多个 CFIR 节点都携带同一错误时，用该包装保留错误类型语义，
 * 同时避免诊断收集阶段重复报告同一个问题。
 *
 * @property original 被包装的原始诊断。
 */
class ConeUnreportedDuplicateDiagnostic(val original: ConeDiagnostic) :
    ConeDiagnostic {
    /**
     * 复用原始诊断原因。
     */
    override val reason: String get() = original.reason
}

/**
 * `??` 的右操作数未满足 `realTgtTy`（官方 `ChkCoalescingExpr` 的 InvalidTy 分支，
 * `Sema/TypeCheckExpr/BinaryExpr.cpp:1136-1140`）。
 *
 * resolve 用它把整个 `??` 表达式置为错误类型并携带 [targetType]（即官方的 `realTgtTy`），
 * 使外层运算符不再派生诊断；`realTgtTy` 只在 resolve 上下文里可得，因此由 resolve 判定、
 * checker 依据该标记把诊断锚到右操作数上——cone -> CFIR 映射对它不产出诊断，
 * 避免在整条 `??` 上重复报告右操作数的失败。
 *
 * @property reason 面向调试的原因文本。
 * @property targetType 官方 `realTgtTy`：右操作数被检查所用的目标类型。
 */
class ConeCoalescingRightOperandMismatch(
    override val reason: String,
    val targetType: ConeCangJieType,
) : ConeDiagnostic

/**
 * `??` 的左操作数不是 `Option` 类型（官方 `ChkCoalescingExpr` 的
 * `sema_invalid_coalescing` 分支，`Sema/TypeCheckExpr/BinaryExpr.cpp:1105-1118`）。
 *
 * resolve 把整个 `??` 表达式置为错误类型并携带本标记；checker 依据该标记把
 * 诊断锚到左操作数上（官方锚 `*be.leftExpr`）。左操作数为错误类型或尚未定型
 * （PCLA placeholder）时不携带本标记，保持纯毒化——对应官方 `CanSkipDiag` 语义。
 *
 * @property reason 面向调试的原因文本。
 */
class ConeCoalescingLeftOperandInvalid(
    override val reason: String,
) : ConeDiagnostic

/**
 * flow 表达式的函数部分是裸 `this` / `super`（官方 `ChkFlowExpr`，
 * `external/cangjie_compiler/src/Sema/TypeCheckExpr/BinaryExpr.cpp:1044-1049`）。
 *
 * 官方在该判据命中时**不再解糖**，直接把整个 flow 表达式置为 InvalidTy 并返回失败。
 * 本仓解糖会构造合成的 invoke 调用或 `operator ()` 值访问，会把裸 `this`/`super` 变成
 * 调用接收者，从而产生 `NOT_MEMBER_OF`、`UNRESOLVED_REFERENCE` 一类官方不存在的诊断。
 *
 * 因此 resolve 命中判据时携带本标记并返回**未解糖**的二元节点，checker 据此映射诊断：
 * `this` 报 flow 专属诊断；`super` 报二元运算符诊断，而裸 `super` 自身的诊断由既有的
 * `ILLEGAL_SUPER_ALONE` 规则在未被包装的操作数上自然产生。
 *
 * @property keyword 命中的关键字。
 * @property isLeftOperand 命中是否位于左操作数。官方 `|>` 只检查函数部分（右侧），
 *   `~>` 两侧都检查。
 */
class ConeFlowInvalidFunctionOperand(
    override val reason: String,
    val keyword: FlowInvalidOperandKeyword,
    val isLeftOperand: Boolean,
) : ConeDiagnostic

/**
 * [ConeFlowInvalidFunctionOperand] 命中的关键字分类。
 */
enum class FlowInvalidOperandKeyword(
    /** 关键字在源码中的文本。 */
    val sourceText: String,
) {
    /** 裸 `this`。 */
    THIS("this"),

    /** 裸 `super`。 */
    SUPER("super"),
}
