package org.cangnova.cangjie.cfir.resolve.body

/**
 * 数据流分析上下文。
 *
 * 持有数据流分析器的状态，用于智能转换（smart cast）、
 * 可达性分析、初始化检查等。
 *
 * Phase 2 提供空实现骨架，Phase 3+ 将逐步添加：
 * - 变量初始化状态跟踪
 * - 条件分支的类型窄化
 * - 不可达代码检测
 *
 * 参考 K2 DataFlowAnalyzerContext。
 */
class CfirDataFlowAnalyzerContext {
    // Phase 2: 骨架，无实际状态
}
