package org.cangnova.cangjie.cfir.types

/**
 * Type 节点是否真正存在由创建 owner 发布，不能由 resolved/source/delegated 的存在反推。
 * SOURCE 属于原始类型语法；SEMANTIC 只是已知或推断类型；COMPILER_ADDED 是原生显式生成节点。
 */
enum class CfirTypeRefOrigin {
    SOURCE,
    SEMANTIC,
    COMPILER_ADDED,
}
