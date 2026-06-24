package org.cangnova.cangjie.cfir.resolve.dfa

/**
 * 标记只允许 DFA 框架内部直接使用的 API。
 *
 * 被该注解保护的声明通常依赖控制流图构造顺序或 flow 持久化结构不变量，外部调用方需要通过
 * 上层 resolve/checker 服务访问数据流信息。
 */
@RequiresOptIn
annotation class DfaInternals
