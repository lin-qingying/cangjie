package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.resolve.providers.CfirDirectSupertypeProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirTypeAwareSupertypeProvider

/** 当前 session 的 extend 查询 provider。 */
val CfirSession.extendProvider: CfirExtendProvider by CfirSession.sessionComponentAccessor()

/**
 * 当前源码模块自身的 extend 索引；依赖装配只能读取此入口，不能递归嵌套依赖的完整查询视图。
 * 对位 symbol provider 的 own/dependency 分层，保留源码依赖环与菱形图中的声明身份。
 */
val CfirSession.ownExtendProvider: CfirExtendProvider
    by CfirSession.sessionComponentAccessor<CfirExtendProvider>(OWN_EXTEND_PROVIDER_QUALIFIED_KEY)

/** 模块自身的 extend provider 与对外依赖聚合视图分别注册。 */
const val OWN_EXTEND_PROVIDER_QUALIFIED_KEY: String =
    "org.cangnova.cangjie.cfir.resolve.providers.CfirOwnExtendProvider"

/** 当前 session 的 extend 查询 provider，可空访问版本。 */
val CfirSession.extendProviderOrNull: CfirExtendProvider? by CfirSession.nullableSessionComponentAccessor()

/** 当前 session 的直接父类型 provider，可空访问版本。 */
val CfirSession.directSupertypeProviderOrNull: CfirDirectSupertypeProvider?
        by CfirSession.nullableSessionComponentAccessor()

/** 当前 session 的类型感知父类型 provider，可空访问版本。 */
val CfirSession.typeAwareSupertypeProviderOrNull: CfirTypeAwareSupertypeProvider?
        by CfirSession.nullableSessionComponentAccessor()
