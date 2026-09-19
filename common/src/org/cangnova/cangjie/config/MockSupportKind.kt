package org.cangnova.cangjie.config

/**
 * 官方仓颉 mock 编译选项的四种状态。
 *
 * 该值是编译调用的配置事实，不从源码路径、测试文件名或 annotation 文本推断。
 * 具体的 session capability 由 CFIR entrypoint 根据 [enableCompileTest] 与此值计算。
 */
public enum class MockSupportKind {
    /** 未显式指定 `--mock`；测试编译中按是否存在 mock 使用决定是否准备包。 */
    DEFAULT,

    /** 显式开启 mock 兼容编译。 */
    ON,

    /** 显式关闭 mock 支持。 */
    OFF,

    /** 允许语义检查，但运行时 mock 创建会被替换为异常路径。 */
    RUNTIME_ERROR,
}
