package org.cangnova.cangjie.test.services
/**
 * 表示 `CompilationStage`，承载测试服务中的配置数据、测试产物或处理步骤。
 */
enum class CompilationStage {
    /**
     * The first compilation stage for source translation and intermediate artifact production.
     */
    FIRST,

    /**
     * The second compilation stage for executable artifact production.
     */
    SECOND,
}
