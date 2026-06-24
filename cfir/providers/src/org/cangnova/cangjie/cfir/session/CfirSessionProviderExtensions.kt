package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.resolve.providers.CfirDirectSupertypeProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirTypeAwareSupertypeProvider
import org.cangnova.cangjie.cfir.resolve.services.CfirImportBindingStore
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider

/** 当前 session 的统一符号 provider。 */
val CfirSession.symbolProvider: CfirSymbolProvider by CfirSession.sessionComponentAccessor()

/** 当前 session 的 CFIR 声明 provider。 */
val CfirSession.cfirProvider: CfirProvider by CfirSession.sessionComponentAccessor()

/** 当前 session 的 extend 查询 provider。 */
val CfirSession.extendProvider: CfirExtendProvider by CfirSession.sessionComponentAccessor()

/** 当前 session 的 extend 查询 provider，可空访问版本。 */
val CfirSession.extendProviderOrNull: CfirExtendProvider? by CfirSession.nullableSessionComponentAccessor()

/** 当前 session 的直接父类型 provider，可空访问版本。 */
val CfirSession.directSupertypeProviderOrNull: CfirDirectSupertypeProvider?
        by CfirSession.nullableSessionComponentAccessor()

/** 当前 session 的类型感知父类型 provider，可空访问版本。 */
val CfirSession.typeAwareSupertypeProviderOrNull: CfirTypeAwareSupertypeProvider?
        by CfirSession.nullableSessionComponentAccessor()

/** 当前 session 的仓颉作用域 provider。 */
val CfirSession.cangjieScopeProvider: CfirCangJieScopeProvider by CfirSession.sessionComponentAccessor()

/** import 绑定缓存。 */
val CfirSession.importBindingStore: CfirImportBindingStore by CfirSession.sessionComponentAccessor()

/** import 绑定缓存，可空访问版本。 */
val CfirSession.importBindingStoreOrNull: CfirImportBindingStore? by CfirSession.nullableSessionComponentAccessor()

/** 仅包含依赖（不含源码）的符号提供器的注册 key。 */
const val DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY: String =
    "org.cangnova.cangjie.cfir.resolve.providers.CfirDependenciesSymbolProvider"

/** 当前 session 仅包含依赖、不包含源码 provider 的符号 provider。 */
val CfirSession.dependenciesSymbolProvider: CfirSymbolProvider
    by CfirSession.sessionComponentAccessor<CfirSymbolProvider>(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY)
