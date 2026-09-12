package org.cangnova.cangjie.type.model

/**
 * 一次类型实参实例化的求解范围，按对象身份区分同一声明的不同调用。
 *
 * 约束系统可合并嵌套调用，但官方局部求解器的 tyVarsToSolve 仍属于各自实例化。
 * 此身份只约束局部依赖图，不隔离跨调用的类型约束及其传播。
 */
class TypeVariableInferenceScope

/** 具有局部类型实参求解范围的变量；lambda/PCLA 占位变量不属于该集合。 */
interface TypeVariableWithInferenceScope : TypeVariableMarker {
    /** 创建此变量的实例化范围。 */
    val inferenceScope: TypeVariableInferenceScope
}
