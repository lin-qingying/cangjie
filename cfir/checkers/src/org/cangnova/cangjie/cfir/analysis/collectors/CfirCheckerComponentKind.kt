package org.cangnova.cangjie.cfir.analysis.collectors

/**
 * 诊断收集组件的种类：一次诊断遍历里由它承担的那一类检查。
 *
 * 一次遍历会在每个 CFIR 元素上依次执行若干个 [org.cangnova.cangjie.cfir.analysis.collectors.components
 * .AbstractDiagnosticCollectorComponent]，耗时差异极大——声明检查器要跑几十个子检查器，
 * 而错误节点组件基本是空转。只看"整个遍历多慢"无法回答"是哪一类检查慢"，本枚举就是
 * 这层归因的维度。
 *
 * 粒度说明：这里到**组件**为止，不到单个 checker。单个 checker 分散在生成代码
 * （`cfir:checkers/gen/`）里且没有稳定 id，要下沉到那一层需要改代码生成器并为每个 checker
 * 造标识、指标基数也会上涨一个数量级。当前组件级已经能回答"声明 / 表达式 / 类型 / 宏 /
 * CFA 哪一类最耗时"，更细的拆分留待确有需要时再做。
 *
 * @property metricSuffix 指标名中的组件段。
 */
enum class CfirCheckerComponentKind(val metricSuffix: String) {
    /** 声明检查器组件。 */
    DECLARATION("declaration"),

    /** 表达式检查器组件。 */
    EXPRESSION("expression"),

    /** 类型检查器组件。 */
    TYPE("type"),

    /** 宏构造诊断组件。 */
    MACRO_CONSTRUCTION("macroConstruction"),

    /** 错误节点诊断组件：只处理已经报错的 CFIR 节点。 */
    ERROR_NODE("errorNode"),

    /** 语言版本设置检查组件。 */
    LANGUAGE_VERSION_SETTINGS("languageVersionSettings"),

    /** 控制流分析组件；只在文件无错误的 POST_SEMA 阶段运行。 */
    CONTROL_FLOW_ANALYSIS("controlFlowAnalysis"),

    /** CHIR 常量/算术检查组件；只在文件无错误的 POST_SEMA 阶段运行。 */
    CHIR_ARITHMETIC("chirArithmetic"),
}