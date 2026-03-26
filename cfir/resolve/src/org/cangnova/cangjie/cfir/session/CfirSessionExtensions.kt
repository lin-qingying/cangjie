package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.resolve.inference.CfirInferenceLogger
import org.cangnova.cangjie.cfir.resolve.transformers.CfirPhaseResolverRegistry
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticCollector
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticReporter
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticReporterComponent
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolver
import org.cangnova.cangjie.cfir.symbols.CfirLazyDeclarationResolver
import org.cangnova.cangjie.cfir.resolve.services.CfirImportBindingStore
import org.cangnova.cangjie.cfir.resolve.services.CfirSuperTypeGraphStore
import org.cangnova.cangjie.cfir.resolve.services.CfirExtendIndexStore

/** resolve 阶段处理器注册表，对齐 Kotlin `FirPhaseManager` 的阶段调度入口。 */
val CfirSession.phaseResolverRegistry: CfirPhaseResolverRegistry by CfirSession.sessionComponentAccessor()

/** 按需声明解析服务。 */
val CfirSession.lazyDeclarationResolver: CfirLazyDeclarationResolver by CfirSession.sessionComponentAccessor()

/** 按需声明解析服务，可空访问版本。 */
val CfirSession.lazyDeclarationResolverOrNull: CfirLazyDeclarationResolver? by CfirSession.nullableSessionComponentAccessor()

/** 诊断上报器组件。 */
private val CfirSession.diagnosticReporterComponent: CfirDiagnosticReporterComponent by CfirSession.sessionComponentAccessor()
private val CfirSession.nullableDiagnosticReporterComponent: CfirDiagnosticReporterComponent? by CfirSession.nullableSessionComponentAccessor()

/** 诊断上报器，对齐 Kotlin `DiagnosticReporter`。 */
val CfirSession.diagnosticReporter: CfirDiagnosticReporter
    get() = diagnosticReporterComponent.reporter

val CfirSession.diagnosticReporterOrNull: CfirDiagnosticReporter?
    get() = nullableDiagnosticReporterComponent?.reporter

/** 诊断收集器，要求当前 reporter 实际为 `CfirDiagnosticCollector`。 */
val CfirSession.diagnosticCollector: CfirDiagnosticCollector
    get() = diagnosticReporter as? CfirDiagnosticCollector
        ?: error("Current diagnostic reporter is not CfirDiagnosticCollector")

/** 将诊断上报器注册到会话。 */
fun CfirSession.registerDiagnosticReporter(reporter: CfirDiagnosticReporter) {
    register(CfirDiagnosticReporterComponent::class, CfirDiagnosticReporterComponent(reporter))
}

/** import 绑定缓存。 */
val CfirSession.importBindingStore: CfirImportBindingStore by CfirSession.sessionComponentAccessor()

/** import 绑定缓存，可空访问版本。 */
val CfirSession.importBindingStoreOrNull: CfirImportBindingStore? by CfirSession.nullableSessionComponentAccessor()

/** 父类型图缓存。 */
val CfirSession.superTypeGraphStore: CfirSuperTypeGraphStore by CfirSession.sessionComponentAccessor()

/** 父类型图缓存，可空访问版本。 */
val CfirSession.superTypeGraphStoreOrNull: CfirSuperTypeGraphStore? by CfirSession.nullableSessionComponentAccessor()

/** extend 语义索引存储。 */
val CfirSession.extendIndexStore: CfirExtendIndexStore by CfirSession.sessionComponentAccessor()

/** extend 语义索引存储，可空访问版本。 */
val CfirSession.extendIndexStoreOrNull: CfirExtendIndexStore? by CfirSession.nullableSessionComponentAccessor()

/** 类型解析器，对齐 Kotlin `FirTypeResolver`。 */
val CfirSession.typeResolver: CfirTypeResolver by CfirSession.sessionComponentAccessor()

/** 推断日志记录器，可空访问版本。 */
val CfirSession.inferenceLogger: CfirInferenceLogger? by CfirSession.nullableSessionComponentAccessor()
