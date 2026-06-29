package org.cangnova.cangjie.analysis.api.types

/**
 * 错误类型。
 *
 * 表示一处未能成功解析的类型位置——可能是名字不在作用域内、参数个数不匹配、依赖缺失等。
 * 该类型保留必要信息以便 IDE 仍可在不可解析的上下文中提供诊断、提示与补全。
 *
 * 对于 class-like 错误,优先使用更具体的 [CaClassErrorType],它会额外暴露限定段与候选符号。
 *
 * 对齐 Kotlin Analysis API 的 `KaErrorType`。
 */
interface CaErrorType : CaType {
    /**
     * 解析失败时产生的内部错误消息,主要用于诊断/调试,不保证国际化或对用户友好。
     */
    val errorMessage: String

    /**
     * 适合直接展示给用户的文本形式,若解析层无法产出友好文本则为 `null`。
     */
    val presentableText: String?

    /**
     * 创建可恢复该错误类型的类型指针。
     */
    override fun createPointer(): CaTypePointer<CaErrorType>
}
