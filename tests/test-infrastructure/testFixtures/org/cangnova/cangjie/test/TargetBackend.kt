package org.cangnova.cangjie.test

/**
 * 目标后端枚举
 *
 * 对应 Kotlin K2 的 TargetBackend
 */
enum class TargetBackend(
    /**
     * 保存 `compatibleWithTargetBackend`，供测试基础设施在测试执行期间读取或传递。
     */
    private val compatibleWithTargetBackend: TargetBackend? = null
) {
    ANY,
    NATIVE,
    ;

    /**
     * 保存 `compatibleWith`，供测试基础设施在测试执行期间读取或传递。
     */
    val compatibleWith get() = compatibleWithTargetBackend ?: ANY

    /**
     * 执行 `isTransitivelyCompatibleWith` 对应的测试基础设施流程，维持测试框架的阶段契约。
     */
    fun isTransitivelyCompatibleWith(backend: TargetBackend): Boolean {
        if (this == backend) return true
        return compatibleWithTargetBackend?.isTransitivelyCompatibleWith(backend) ?: false
    }
}
