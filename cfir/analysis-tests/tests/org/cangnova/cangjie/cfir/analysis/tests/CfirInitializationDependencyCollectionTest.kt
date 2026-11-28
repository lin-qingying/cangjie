package org.cangnova.cangjie.cfir.analysis.tests

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.analysis.checkers.MppCheckerKind
import org.cangnova.cangjie.cfir.CfirExceptionHandler
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.analysis.CheckersComponent
import org.cangnova.cangjie.cfir.analysis.checkers.CommonDeclarationCheckers
import org.cangnova.cangjie.cfir.analysis.checkers.CommonExpressionCheckers
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.initializationDependencies
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.impl.BaseDiagnosticsCollector
import org.cangnova.cangjie.cfir.pipeline.CfirTotalResolveProcessor
import org.cangnova.cangjie.cfir.pipeline.runCheckers
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticCollector
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSessionExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompositeSymbolProvider
import org.cangnova.cangjie.cfir.session.CfirBuiltinTypes
import org.cangnova.cangjie.cfir.session.extendIndexStore
import org.cangnova.cangjie.cfir.session.registerDiagnosticReporter
import org.cangnova.cangjie.cfir.session.registerResolveComponents
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpBatchFamilyAggregate
import org.cangnova.cangjie.cfir.session.CfirCjmpFamilyAggregate
import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent
import org.cangnova.cangjie.cfir.resolve.providers.DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY
import org.cangnova.cangjie.cfir.diagnostics.CjRegisteredDiagnosticFactoriesStorage
import org.cangnova.cangjie.cfir.resolve.cjmp.CfirCjmpMatchRunner
import org.cangnova.cangjie.resolve.calls.mpp.CjmpInitializationDependencies
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** 初始化依赖从解析后的真实 CFIR 表达式目标收集，并只附着在已初始化的实例字段上。 */
class CfirInitializationDependencyCollectionTest : AbstractCfirAnalysisResolveTest() {
    @BeforeEach
    fun setUpFixture() {
        setUp()
    }

    @AfterEach
    fun tearDownFixture() {
        tearDown()
    }

    @Test
    fun initializedInstanceFieldsCollectResolvedTargetsAcrossReferencesMembersAndLambdas() {
        val session = cjmpSession()
        val cfirFile = createCjFile("initializationDependencies", SOURCE).toCfirFile(session)
        resolveToPhase(cfirFile, session, CfirResolvePhase.BODY_RESOLVE, CfirDiagnosticCollector())
        for (mppCheckerKind in MppCheckerKind.entries) {
            session.runCheckers(ScopeSession(), listOf(cfirFile), RecordingDiagnosticsCollector(), mppCheckerKind)
        }

        val holder = fieldsOf(cfirFile, "Holder")
        val box = fieldsOf(cfirFile, "Box")
        val uninitialized = fieldsOf(cfirFile, "Uninitialized")

        assertCollected(box.getValue("direct"), box.getValue("source"))
        assertCollected(box.getValue("member"), box.getValue("source"))
        assertCollected(box.getValue("deduplicated"), box.getValue("source"))
        assertCollected(box.getValue("captured"), holder.getValue("value"))
        assertCollected(box.getValue("externalMember"), holder.getValue("value"))
        assertCollected(box.getValue("readsStatic"))
        assertCollected(box.getValue("readsPackage"))
        assertCollected(box.getValue("source"))
        assertNotCollected(box.getValue("staticSource"))
        assertNotCollected(uninitialized.getValue("value"))
    }

    @Test
    fun initializedInstanceFieldsRetainCrossPackageDeclarationIdentity() {
        val session = cjmpSession()
        val files = listOf(
            createCjFile("externalHolder", EXTERNAL_HOLDER_SOURCE),
            createCjFile("crossPackageBox", CROSS_PACKAGE_BOX_SOURCE),
        ).toCfirFiles(session)
        resolveFiles(files, session)
        for (mppCheckerKind in MppCheckerKind.entries) {
            session.runCheckers(ScopeSession(), files, RecordingDiagnosticsCollector(), mppCheckerKind)
        }

        val holder = fieldsOf(files[0], "Holder")
        val box = fieldsOf(files[1], "Box")
        assertCollected(box.getValue("externalMember"), holder.getValue("value"))
    }

    private fun fieldsOf(file: CfirFile, className: String): Map<String, CfirFieldVariable> =
        file.declarations
            .filterIsInstance<CfirClass>()
            .single { it.name.asString() == className }
            .declarations
            .filterIsInstance<CfirFieldVariable>()
            .associateBy { it.name.asString() }

    private fun cjmpSession() = createTestSession().apply {
        register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent(explicitMode = CfirCjmpMode.COMMON))
        register(CfirCjmpFamilyAggregate::class, CfirCjmpBatchFamilyAggregate(this) { declaration, ownerFacts, matchingMode ->
            CfirCjmpMatchRunner.prepareDeclaration(declaration, this, ownerFacts, matchingMode = matchingMode)
        })
        register(DEPENDENCIES_SYMBOL_PROVIDER_QUALIFIED_KEY, CfirCompositeSymbolProvider(this, emptyList()))
        register(
            CfirExceptionHandler::class,
            object : CfirExceptionHandler() {
                override fun handleExceptionOnElementAnalysis(element: CfirElement, throwable: Throwable): Nothing =
                    throw throwable

                override fun handleExceptionOnFileAnalysis(file: CfirFile, throwable: Throwable): Nothing =
                    throw throwable
            },
        )
    }

    private fun resolveFiles(files: List<CfirFile>, session: org.cangnova.cangjie.cfir.session.CfirSession) {
        session.register(CfirBuiltinTypes::class, CfirBuiltinTypes())
        session.register(
            CheckersComponent::class,
            CheckersComponent().apply {
                register(CommonDeclarationCheckers)
                register(CommonExpressionCheckers)
            },
        )
        session.registerDiagnosticReporter(CfirDiagnosticCollector())
        session.registerResolveComponents(CjRegisteredDiagnosticFactoriesStorage())
        session.register(CfirExtendProvider::class, CfirSessionExtendProvider(session, session.extendIndexStore))
        CfirTotalResolveProcessor(session).process(files)
    }

    private fun assertCollected(field: CfirFieldVariable, vararg expected: CfirFieldVariable) {
        val collected = field.initializationDependencies as? CjmpInitializationDependencies.Collected
            ?: error("${field.name} must publish a collected dependency set")
        assertEquals(expected.size, collected.declarations.size, "${field.name} dependency count")
        expected.forEachIndexed { index, declaration ->
            assertSame(declaration.symbol, collected.declarations[index], "${field.name} dependency[$index]")
        }
    }

    private fun assertNotCollected(field: CfirFieldVariable) {
        assertSame(CjmpInitializationDependencies.NotCollected, field.initializationDependencies, field.name.asString())
    }

    private class RecordingDiagnosticsCollector : BaseDiagnosticsCollector() {
        override val diagnostics: List<CjDiagnostic> = emptyList()
        override val diagnosticsByFilePath: Map<String?, List<CjDiagnostic>> = emptyMap()
        override val hasErrors: Boolean = false
        override val hasWarningsForWError: Boolean = false

        override fun report(diagnostic: CjDiagnostic?, context: DiagnosticContext) = Unit
    }

    private companion object {
        val SOURCE = """
            public common let packageValue: Int64 = 1

            public common class Holder {
                public common let value: Int64 = 1
                public common init()
            }

            public common class Box {
                public common let source: Int64 = 1
                public let direct: Int64 = source
                public let member: Int64 = this.source
                public let deduplicated: Int64 = source + this.source
                public let readsPackage: Int64 = packageValue
                public static let staticSource: Int64 = 2
                public let readsStatic: Int64 = staticSource
                public static let holder: Holder = Holder()
                public let externalMember: Int64 = holder.value
                public let captured: () -> Int64 = { => holder.value }
                public common init()
            }

            public class Uninitialized {
                public var value: Int64
                public init() {
                    this.value = 1
                }
            }
        """.trimIndent()

        val EXTERNAL_HOLDER_SOURCE = """
            package initialization.dependencies.external

            public common class Holder {
                public common let value: Int64 = 1
                public common init()
            }
        """.trimIndent()

        val CROSS_PACKAGE_BOX_SOURCE = """
            package initialization.dependencies.consumer
            import initialization.dependencies.external.Holder

            public common class Box {
                public static let holder: Holder = Holder()
                public let externalMember: Int64 = holder.value
                public common init()
            }
        """.trimIndent()
    }
}
