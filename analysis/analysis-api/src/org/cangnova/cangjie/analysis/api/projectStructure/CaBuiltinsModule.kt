package org.cangnova.cangjie.analysis.api.projectStructure

/**
 * 仓颉内置类型所在的 [CaModule]。
 *
 * - 提供平台无关的内置符号(基础类型、`core` 中的内建声明等);
 * - 作为兜底依赖出现:模块没有显式依赖标准库时,内置模块仍会被解析引擎隐式注入;
 * - 通常不需要被业务代码显式声明为依赖,具体注入由 Analysis API 内部完成。
 *
 * 对齐 Kotlin Analysis API 的 `KaBuiltinsModule`。
 */
interface CaBuiltinsModule : CaModule {
    /**
     * 内置模块的稳定名称,默认实现使用占位字符串 `<builtins>`,可被具体平台覆盖。
     */
    val builtinsName: String
        get() = "<builtins>"

    /**
     * 人类可读的模块描述,用于诊断与调试输出。
     */
    override val moduleDescription: String
        get() = "Builtins module $builtinsName"

    /**
     * 内置模块的稳定二进制名称,默认与 [builtinsName] 一致。
     */
    override val stableModuleName: String?
        get() = builtinsName
}
