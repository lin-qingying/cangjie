package org.cangnova.cangjie.cfir.analysis.checkers

/**
 * Common 检查在声明所属 session 中执行；Platform 检查使用叶模块 session 检查整个依赖图的源码。
 * LL 在同一次结构元素诊断收集中保留两类 checker。
 */
enum class MppCheckerKind {
    Common, Platform
}

/** checker 的多平台执行归属，由 registry 和 collector 统一选择。 */
interface CfirCheckerWithMppKind {
    val mppKind: MppCheckerKind
}
