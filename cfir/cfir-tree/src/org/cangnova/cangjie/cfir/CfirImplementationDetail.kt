package org.cangnova.cangjie.cfir

/**
 * 标记 CFIR 内部实现细节 API。
 *
 * 这些 API 不应作为跨模块稳定契约直接使用，调用方需要显式 opt-in。
 */
@RequiresOptIn(level = RequiresOptIn.Level.ERROR)
annotation class CfirImplementationDetail
