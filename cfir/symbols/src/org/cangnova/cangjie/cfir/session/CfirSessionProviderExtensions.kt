package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider

val CfirSession.symbolProvider: CfirSymbolProvider by CfirSession.sessionComponentAccessor()

val CfirSession.cfirProvider: CfirProvider by CfirSession.sessionComponentAccessor()

val CfirSession.extendProvider: CfirExtendProvider by CfirSession.sessionComponentAccessor()

val CfirSession.kotlinScopeProvider: CfirCangJieScopeProvider by CfirSession.sessionComponentAccessor()

/** 仅包含依赖（不含源码）的符号提供器的注册 key。 */
const val DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY: String =
    "org.cangnova.cangjie.cfir.resolve.providers.CfirDependenciesSymbolProvider"

val CfirSession.dependenciesSymbolProvider: CfirSymbolProvider
    by CfirSession.sessionComponentAccessor<CfirSymbolProvider>(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY)
