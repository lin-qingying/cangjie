package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.resolve.providers.CfirDirectSupertypeProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirTypeAwareSupertypeProvider
import org.cangnova.cangjie.cfir.resolve.services.CfirImportBindingStore
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider

val CfirSession.symbolProvider: CfirSymbolProvider by CfirSession.sessionComponentAccessor()

val CfirSession.cfirProvider: CfirProvider by CfirSession.sessionComponentAccessor()

val CfirSession.extendProvider: CfirExtendProvider by CfirSession.sessionComponentAccessor()
val CfirSession.extendProviderOrNull: CfirExtendProvider? by CfirSession.nullableSessionComponentAccessor()

val CfirSession.directSupertypeProviderOrNull: CfirDirectSupertypeProvider?
        by CfirSession.nullableSessionComponentAccessor()

val CfirSession.typeAwareSupertypeProviderOrNull: CfirTypeAwareSupertypeProvider?
        by CfirSession.nullableSessionComponentAccessor()

val CfirSession.cangjieScopeProvider: CfirCangJieScopeProvider by CfirSession.sessionComponentAccessor()

/** import 绑定缓存。 */
val CfirSession.importBindingStore: CfirImportBindingStore by CfirSession.sessionComponentAccessor()

/** import 绑定缓存，可空访问版本。 */
val CfirSession.importBindingStoreOrNull: CfirImportBindingStore? by CfirSession.nullableSessionComponentAccessor()

/** 仅包含依赖（不含源码）的符号提供器的注册 key。 */
const val DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY: String =
    "org.cangnova.cangjie.cfir.resolve.providers.CfirDependenciesSymbolProvider"

val CfirSession.dependenciesSymbolProvider: CfirSymbolProvider
    by CfirSession.sessionComponentAccessor<CfirSymbolProvider>(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY)
