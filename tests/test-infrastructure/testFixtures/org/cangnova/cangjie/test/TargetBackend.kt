package org.cangnova.cangjie.test

/**
 * 目标后端枚举
 *
 * 对应 Kotlin K2 的 TargetBackend
 */
enum class TargetBackend(
    private val compatibleWithTargetBackend: TargetBackend? = null
) {
    ANY,
    NATIVE,
    ;

    val compatibleWith get() = compatibleWithTargetBackend ?: ANY

    fun isTransitivelyCompatibleWith(backend: TargetBackend): Boolean {
        if (this == backend) return true
        return compatibleWithTargetBackend?.isTransitivelyCompatibleWith(backend) ?: false
    }
}
