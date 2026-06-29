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
 * 允许错误类型在成员作用域遍历时继续使用 delegated nominal type 的诊断。
 *
 * 这不是通用错误恢复标记；只有类型构造器已经解析成功、错误只发生在不影响 nominal
 * owner 的附属信息上时，诊断实现才应声明该能力。
 */
interface ConeAllowsDelegatedScopeTraversalDiagnostic : ConeDiagnostic

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
