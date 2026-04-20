package org.cangnova.cangjie.analysis.api.projectStructure

/**
 * 控制游离文件中非局部声明引用的解析策略。
 */
enum class CaDanglingFileResolutionMode {
    /**
     * 优先解析到游离文件自身中的声明，必要时再回退到原始文件或上下文模块。
     */
    PREFER_SELF,

    /**
     * 默认忽略游离文件中的非局部声明，直接解析到原始文件或上下文模块。
     */
    IGNORE_SELF,
}
