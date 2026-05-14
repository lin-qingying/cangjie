package org.cangnova.cangjie.analysis.api.projectStructure

/**
 * 控制游离文件中**非局部声明**引用的解析策略。
 *
 * 对齐 Kotlin Analysis API 的 `KaDanglingFileResolutionMode`。
 */
enum class CaDanglingFileResolutionMode {
    /**
     * 优先解析到游离文件自身中的声明,必要时再回退到原始文件或上下文模块。
     */
    PREFER_SELF,

    /**
     * 默认忽略游离文件中的非局部声明,直接解析到原始文件或上下文模块。
     *
     * 该模式主要用于性能优化:当游离文件只是被复制出来做临时修改、
     * 而调用方并不关心新增的顶层声明时,可避免重新分析游离声明。
     */
    IGNORE_SELF,
}
