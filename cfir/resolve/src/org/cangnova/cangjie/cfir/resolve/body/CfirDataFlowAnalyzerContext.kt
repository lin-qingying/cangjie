package org.cangnova.cangjie.cfir.resolve.body

/**
 * 数据流分析上下文。
 * 持有数据流分析器状态，用于 smart cast、可达性分析和初始化检查等能力。
 * Phase 2 先提供空骨架，Phase 3+ 再逐步补齐：
 * - 变量初始化状态跟踪
 * - 条件分支的类型收窄
 * - 不可达代码检测
 * 参考 K2 `DataFlowAnalyzerContext`。
 */
class CfirDataFlowAnalyzerContext {
    // Phase 2: 仅提供骨架，暂不承载实际状态
}

