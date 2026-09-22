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
