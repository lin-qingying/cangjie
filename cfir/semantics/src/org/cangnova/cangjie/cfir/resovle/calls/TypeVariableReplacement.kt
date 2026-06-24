package org.cangnova.cangjie.cfir.resovle.calls

/**
 * 约束系统输出中类型变量的替换策略。
 */
enum class TypeVariableReplacement {
    /** 保留为声明侧类型参数。 */
    TypeParameter,

    /** 替换为错误类型，阻断后续依赖该变量的精确推断。 */
    ErrorType,
}
