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

/**
 * flow 表达式的解糖调用失败。
 *
 * 官方 `ChkFlowExpr` 分两阶段：先综合操作数，再解糖成普通调用并检查。第二阶段的调用检查
 * 在抑制器内进行（`external/cangjie_compiler/src/Sema/TypeCheckExpr/BinaryExpr.cpp:1057-1070`）：
 * 调用失败时**丢弃调用自身的全部诊断**，用 `RecoverToBinaryExpr` 把表达式恢复成二元节点，
 * 再经 `DiagnoseForBinaryExpr` 在操作符上报 `sema_invalid_binary_expr`。
 *
 * 本仓合成调用失败时若被留在树里，其内部的 `operator ()` 包装会把「两侧类型可解析但操作符
 * 无重载」这一事实重述成操作数上的 `UNRESOLVED_REFERENCE` / `NO_MATCHING_OPERATOR_INVOKE` /
 * `ARGUMENT_TYPE_MISMATCH`，这些都是官方不存在的诊断（例如 `x ~> g` 中 `x: Int64`、
 * `g: (Int64) -> Int64`）。因此 resolve 失败时同样**丢弃整棵合成调用**，把本标记挂在二元
 * 节点上并由 checker 在操作符上报二元运算符诊断——合成的 `this |> g` 实参失配也就不会被
 * 单独报出（官方实测只报 `sema_invalid_binary_expr`）。
 *
 * 操作数自身的名字解析失败不走本路径：官方在综合操作数阶段就已把诊断报出（`~>` 直接返回
 * 失败、`|>` 清空操作数继续），实测 `missing |> f` / `1 |> undeclared` 只报
 * `sema_undeclared_identifier`。因此本标记只在**两侧操作数均无根错误**时产生。
 *
 * @property reason 解糖调用失败的原因，仅用于标记自述；最终文案由二元运算符诊断渲染。
 * @property leftOperandType 解糖**前**综合得到的左操作数类型。官方 `DiagnoseForBinaryExpr`
 *   渲染的是综合阶段的类型；`|>` 的左操作数随后会作为解糖调用的实参被重新检查，失败时可能
 *   被写成错误类型，因此必须在解糖前取快照。
 * @property rightOperandType 解糖前综合得到的右操作数类型。
 */
class ConeInvalidFlowBinaryExpr(
    override val reason: String,
    val leftOperandType: ConeCangJieType?,
    val rightOperandType: ConeCangJieType?,
) : ConeDiagnostic
