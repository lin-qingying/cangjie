package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityChecker
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendExportSurfaceService
import org.cangnova.cangjie.cfir.resolve.services.CfirImportBindingStore
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider


/** 当前 session 的唯一语言级可见性服务。 */
val CfirSession.accessibilityChecker: CfirAccessibilityChecker by CfirSession.sessionComponentAccessor()

/** 当前 session 的 extend 成员导出面服务。 */
val CfirSession.extendExportSurfaceService: CfirExtendExportSurfaceService by CfirSession.sessionComponentAccessor()








/** 当前 session 的仓颉作用域 provider。 */
val CfirSession.cangjieScopeProvider: CfirCangJieScopeProvider by CfirSession.sessionComponentAccessor()

/** import 绑定缓存。 */
val CfirSession.importBindingStore: CfirImportBindingStore by CfirSession.sessionComponentAccessor()

/** import 绑定缓存，可空访问版本。 */
val CfirSession.importBindingStoreOrNull: CfirImportBindingStore? by CfirSession.nullableSessionComponentAccessor()


