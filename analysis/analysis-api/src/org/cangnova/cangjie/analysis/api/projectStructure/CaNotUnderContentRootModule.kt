package org.cangnova.cangjie.analysis.api.projectStructure

/**
 * 不属于工程内容根的源码模块。
 *
 * - 表示一类"游离"但仍可分析的源码,例如 IDE 打开的外部源文件、
 *   测试数据文件、另一个工程的源码片段等;
 * - 与 [CaDanglingFileModule] 的差别:[CaNotUnderContentRootModule] 通常对应
 *   持久化的 PSI 文件,只是没有被纳入任何 content root,而非临时 in-memory 文件;
 * - 依赖图由具体平台决定,常见做法是依赖 SDK / 标准库。
 *
 * 对齐 Kotlin Analysis API 的 `KaNotUnderContentRootModule`。
 */
interface CaNotUnderContentRootModule : CaModule {
    /**
     * 人类可读的模块名,通常派生自文件路径或显式标识。
     */
    val name: String

    /**
     * 若该模块是从某个真实模块"派生"而来(例如 IDE 复制场景),
     * 可在此返回原始模块,便于调用方做映射。
     */
    val originalModule: CaModule?
        get() = null

    /**
     * 人类可读的模块描述。
     */
    override val moduleDescription: String
        get() = "Not-under-content-root module $name"
}
