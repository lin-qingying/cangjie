package org.cangnova.cangjie.cfir.analysis.diagnostics

import org.cangnova.cangjie.cfir.diagnostics.CjRegisteredDiagnosticFactoriesStorage
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent

/** 挂载在 session 上的 CFIR 诊断工厂注册表组件。 */
class CfirRegisteredDiagnosticFactoriesStorage(
    /** 当前 session 使用的诊断工厂注册表。 */
    val storage: CjRegisteredDiagnosticFactoriesStorage,
) : CfirSessionComponent

/** 必须存在的 session 诊断工厂注册表组件 accessor。 */
private val CfirSession.cfirRegisteredDiagnosticFactoriesStorage: CfirRegisteredDiagnosticFactoriesStorage
        by CfirSession.sessionComponentAccessor()

/** 可空的 session 诊断工厂注册表组件 accessor。 */
private val CfirSession.cfirRegisteredDiagnosticFactoriesStorageOrNull: CfirRegisteredDiagnosticFactoriesStorage?
        by CfirSession.nullableSessionComponentAccessor()

/** 当前 session 中已经注册的诊断工厂注册表。 */
val CfirSession.registeredDiagnosticFactoriesStorage: CjRegisteredDiagnosticFactoriesStorage
    get() = cfirRegisteredDiagnosticFactoriesStorage.storage

/** 当前 session 中可选的诊断工厂注册表。 */
val CfirSession.registeredDiagnosticFactoriesStorageOrNull: CjRegisteredDiagnosticFactoriesStorage?
    get() = cfirRegisteredDiagnosticFactoriesStorageOrNull?.storage

/** 将诊断工厂注册表组件注册到当前 session。 */
fun CfirSession.registerDiagnosticFactoriesStorage(storage: CjRegisteredDiagnosticFactoriesStorage) {
    register(CfirRegisteredDiagnosticFactoriesStorage::class, CfirRegisteredDiagnosticFactoriesStorage(storage))
}
