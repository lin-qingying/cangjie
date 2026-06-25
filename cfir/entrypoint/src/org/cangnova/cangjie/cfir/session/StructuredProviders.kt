package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider

/**
 * session 中按来源分组的符号 provider 集合。
 *
 * @property sourceProviders 当前 session 自身声明的 provider，包括真实源码声明、
 * 当前模块插件生成声明，以及增量编译产出的当前模块预编译声明。
 *
 * @property dependencyProviders 当前 session 依赖声明的 provider，包括二进制依赖和依赖模块源码 provider。
 *
 * @property sharedProvider 跨 session 共享的 provider，用于 fallback builtins、synthetic provider 等共享声明。
 */
class StructuredProviders(
    /**
     * 当前 session 自身声明与生成声明对应的 provider 列表。
     */
    val sourceProviders: List<CfirSymbolProvider>,
    /**
     * 当前 session 依赖模块对应的 provider 列表。
     */
    val dependencyProviders: List<CfirSymbolProvider>,
    /**
     * 跨 session 共享的 fallback provider。
     */
    val sharedProvider: CfirSymbolProvider
) : CfirSessionComponent

/**
 * 当前 session 的结构化 provider 集合。
 */
val CfirSession.structuredProviders: StructuredProviders by CfirSession.sessionComponentAccessor()
