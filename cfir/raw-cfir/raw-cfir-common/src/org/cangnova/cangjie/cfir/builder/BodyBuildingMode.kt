package org.cangnova.cangjie.cfir.builder

/**
 * Raw CFIR 构建时的函数体构建策略。
 *
 * 该模式用于统一 PSI 与 LightTree builder 对函数体、属性初始化器等 body
 * 的处理时机，避免两条 raw 构建路径在 lazy body 策略上分叉。
 */
enum class BodyBuildingMode {
    /** 立即构建所有可见 body。 */
    NORMAL,

    /** 延迟构建 body，仅保留后续阶段可恢复 body 的入口信息。 */
    LAZY_BODIES,

    ;

    /** [BodyBuildingMode] 的布尔开关转换工具。 */
    companion object {
        /** 根据 [lazyBodies] 开关选择 [LAZY_BODIES] 或 [NORMAL]。 */
        fun lazyBodies(lazyBodies: Boolean): BodyBuildingMode = if (lazyBodies) LAZY_BODIES else NORMAL
    }
}
