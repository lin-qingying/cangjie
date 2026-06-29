package org.jetbrains.kotlin.test

/**
 * 测试生成器沿用的目标后端枚举。
 *
 * 旧版 JUnit4 生成器会把该枚举写入 `runTest` 调用，用于区分 JVM、JS、Wasm 等后端测试配置；
 * 仓颉前端测试当前主要复用其兼容关系建模能力。
 */
enum class TargetBackend(
    /**
     * 当前后端直接兼容的父级后端；为 null 时按 [ANY] 处理。
     */
    private val compatibleWithTargetBackend: TargetBackend? = null,
) {
    ANY,
    JVM,
    JVM_IR(JVM),
    JVM_IR_SERIALIZE(JVM_IR),
    JS_IR,
    JS_IR_ES6(JS_IR),
    WASM,
    WASM_JS(WASM),
    WASM_WASI(WASM),
    ANDROID(JVM),
    NATIVE,
    ;

    /**
     * 当前后端声明的直接兼容目标。
     */
    val compatibleWith get() = compatibleWithTargetBackend ?: ANY

    /**
     * 判断当前后端是否通过兼容链条兼容指定后端。
     */
    fun isTransitivelyCompatibleWith(backend: TargetBackend): Boolean {
        if (this == backend) return true
        return compatibleWithTargetBackend?.isTransitivelyCompatibleWith(backend) ?: false
    }
}
