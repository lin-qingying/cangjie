package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.resolve.transformers.CfirPhaseResolverRegistry
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticCollector
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticReporter
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticReporterComponent
import org.cangnova.cangjie.cfir.resolve.CfirExplicitTypeRefResolver
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolver
import org.cangnova.cangjie.cfir.resolve.services.CfirLazyDeclarationResolver
import org.cangnova.cangjie.cfir.resolve.services.CfirImportBindingStore
import org.cangnova.cangjie.cfir.resolve.services.CfirSuperTypeGraphStore
import org.cangnova.cangjie.cfir.resolve.services.CfirExtendIndexStore

/** resolve 闃舵澶勭悊鍣ㄦ敞鍐岃〃銆傚榻?Kotlin: `FirPhaseManager` 浣撶郴涓嬬殑闃舵璋冨害鍏ュ彛銆?*/
val CfirSession.phaseResolverRegistry: CfirPhaseResolverRegistry by CfirSession.sessionComponentAccessor()

/** 鎸夐渶澹版槑瑙ｆ瀽鏈嶅姟銆?*/
val CfirSession.lazyDeclarationResolver: CfirLazyDeclarationResolver by CfirSession.sessionComponentAccessor()

/** 鎸夐渶澹版槑瑙ｆ瀽鏈嶅姟锛堝彲绌鸿闂級銆?*/
val CfirSession.lazyDeclarationResolverOrNull: CfirLazyDeclarationResolver? by CfirSession.nullableSessionComponentAccessor()

/** 璇婃柇涓婃姤鍣ㄧ粍浠躲€?*/
private val CfirSession.diagnosticReporterComponent: CfirDiagnosticReporterComponent by CfirSession.sessionComponentAccessor()
private val CfirSession.nullableDiagnosticReporterComponent: CfirDiagnosticReporterComponent? by CfirSession.nullableSessionComponentAccessor()

/** 璇婃柇涓婃姤鍣ㄣ€傚榻?Kotlin: `DiagnosticReporter`銆?*/
val CfirSession.diagnosticReporter: CfirDiagnosticReporter
    get() = diagnosticReporterComponent.reporter

val CfirSession.diagnosticReporterOrNull: CfirDiagnosticReporter?
    get() = nullableDiagnosticReporterComponent?.reporter

/** 璇婃柇鏀堕泦鍣紙瑕佹眰褰撳墠 reporter 瀹為檯涓?`CfirDiagnosticCollector`锛夈€?*/
val CfirSession.diagnosticCollector: CfirDiagnosticCollector
    get() = diagnosticReporter as? CfirDiagnosticCollector
        ?: error("Current diagnostic reporter is not CfirDiagnosticCollector")

/** 娉ㄥ唽璇婃柇涓婃姤鍣ㄥ埌浼氳瘽銆?*/
fun CfirSession.registerDiagnosticReporter(reporter: CfirDiagnosticReporter) {
    register(CfirDiagnosticReporterComponent::class, CfirDiagnosticReporterComponent(reporter))
}

/** import 缁戝畾缂撳瓨銆?*/
val CfirSession.importBindingStore: CfirImportBindingStore by CfirSession.sessionComponentAccessor()

/** import 缁戝畾缂撳瓨锛堝彲绌鸿闂級銆?*/
val CfirSession.importBindingStoreOrNull: CfirImportBindingStore? by CfirSession.nullableSessionComponentAccessor()

/** 鐖剁被鍨嬪浘缂撳瓨銆?*/
val CfirSession.superTypeGraphStore: CfirSuperTypeGraphStore by CfirSession.sessionComponentAccessor()

/** 鐖剁被鍨嬪浘缂撳瓨锛堝彲绌鸿闂級銆?*/
val CfirSession.superTypeGraphStoreOrNull: CfirSuperTypeGraphStore? by CfirSession.nullableSessionComponentAccessor()

/** extend 璇箟绱㈠紩瀛樺偍銆?*/
val CfirSession.extendIndexStore: CfirExtendIndexStore by CfirSession.sessionComponentAccessor()

/** extend 璇箟绱㈠紩瀛樺偍锛堝彲绌鸿闂級銆?*/
val CfirSession.extendIndexStoreOrNull: CfirExtendIndexStore? by CfirSession.nullableSessionComponentAccessor()

/** 绫诲瀷瑙ｆ瀽鍣ㄣ€傚榻?Kotlin: `FirTypeResolver`銆?*/
val CfirSession.typeResolver: CfirTypeResolver by CfirSession.sessionComponentAccessor()

/** 鏄惧紡绫诲瀷寮曠敤瑙ｆ瀽鍣ㄣ€傚榻?Kotlin: `FirSpecificTypeResolverTransformer` 鎵€渚濊禆鐨勮В鏋愯亴璐ｃ€?*/
internal val CfirSession.explicitTypeRefResolver: CfirExplicitTypeRefResolver by CfirSession.sessionComponentAccessor()

