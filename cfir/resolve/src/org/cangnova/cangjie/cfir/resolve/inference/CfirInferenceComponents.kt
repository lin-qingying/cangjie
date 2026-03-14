package org.cangnova.cangjie.cfir.resolve.inference

import org.cangnova.cangjie.cfir.types.ConeSubtypeChecker

/**
 * 推断组件工厂。
 *
 * 持有推断所需的公共依赖，提供约束系统的创建入口。
 *
 * 对齐 K2 InferenceComponents（去掉 resultTypeResolver 等 Kotlin 特有组件）。
 */
class CfirInferenceComponents(
    /** 子类型检查器引用 */
    val subtypeChecker: ConeSubtypeChecker,
) {
    /**
     * 创建新的约束系统实例。
     *
     * 每次泛型调用推断都应创建独立的约束系统。
     */
    fun createConstraintSystem(): CfirConstraintSystemImpl = CfirConstraintSystemImpl()
}
