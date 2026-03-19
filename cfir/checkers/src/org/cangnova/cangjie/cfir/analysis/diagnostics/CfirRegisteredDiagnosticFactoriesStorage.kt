package org.cangnova.cangjie.cfir.analysis.diagnostics

import org.cangnova.cangjie.cfir.diagnostics.CjRegisteredDiagnosticFactoriesStorage
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent

class CfirRegisteredDiagnosticFactoriesStorage(
    val storage: CjRegisteredDiagnosticFactoriesStorage,
) : CfirSessionComponent

private val CfirSession.cfirRegisteredDiagnosticFactoriesStorage: CfirRegisteredDiagnosticFactoriesStorage
        by CfirSession.sessionComponentAccessor()

private val CfirSession.cfirRegisteredDiagnosticFactoriesStorageOrNull: CfirRegisteredDiagnosticFactoriesStorage?
        by CfirSession.nullableSessionComponentAccessor()

val CfirSession.registeredDiagnosticFactoriesStorage: CjRegisteredDiagnosticFactoriesStorage
    get() = cfirRegisteredDiagnosticFactoriesStorage.storage

val CfirSession.registeredDiagnosticFactoriesStorageOrNull: CjRegisteredDiagnosticFactoriesStorage?
    get() = cfirRegisteredDiagnosticFactoriesStorageOrNull?.storage

fun CfirSession.registerDiagnosticFactoriesStorage(storage: CjRegisteredDiagnosticFactoriesStorage) {
    register(CfirRegisteredDiagnosticFactoriesStorage::class, CfirRegisteredDiagnosticFactoriesStorage(storage))
}

