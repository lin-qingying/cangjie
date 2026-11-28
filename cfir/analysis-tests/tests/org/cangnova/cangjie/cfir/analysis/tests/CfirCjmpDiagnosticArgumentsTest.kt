package org.cangnova.cangjie.cfir.analysis.tests

import org.junit.jupiter.api.AfterEach
import org.cangnova.cangjie.cfir.analysis.checkers.MppCheckerKind
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.CfirExceptionHandler
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.CfirCjmpDiagnosticArgument
import org.cangnova.cangjie.cfir.diagnostics.CfirCjmpDiagnosticLabel
import org.cangnova.cangjie.cfir.diagnostics.CfirCjmpImplementationKind
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnosticWithParameters2
import org.cangnova.cangjie.cfir.diagnostics.CjPsiDiagnosticWithParameters1
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticBaseContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.SourceElementPositioningStrategies
import org.cangnova.cangjie.cfir.diagnostics.impl.BaseDiagnosticsCollector
import org.cangnova.cangjie.cfir.diagnostics.Severity
import org.cangnova.cangjie.cfir.pipeline.runCheckers
import org.cangnova.cangjie.cfir.resolve.CfirDiagnosticCollector
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent
import org.cangnova.cangjie.source.toCjPsiSourceElement

/** 关键 CJMP 诊断的参数必须与官方诊断工厂的实参语义一致。 */
class CfirCjmpDiagnosticArgumentsTest : AbstractCfirAnalysisResolveTest() {
    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    @Test
    fun genericFrozenDiagnosticUsesDeclarationName() {
        val diagnostic = diagnostics(
            mode = CfirCjmpMode.COMMON,
            source = """
                @Frozen
                public common func generic<T>(value: T): Int64
            """.trimIndent(),
        ).single { it.factoryName.endsWith("COMMON_GENERIC_FROZEN_NOT_SUPPORTED") }

        assertEquals(
            "common/specific declaration generic with generics cannot be @Frozen",
            diagnostic.renderMessage(),
        )
    }

    @Test
    fun duplicateExtensionDiagnosticUsesCompleteExtendedType() {
        val diagnostic = diagnostics(
            mode = CfirCjmpMode.SPECIFIC,
            source = """
                package cjmp_p

                public class Box<T> {}
                specific extend Box<Int64> {}
                specific extend Box<Int64> {}
            """.trimIndent(),
        ).single { it.factoryName.endsWith("SPECIFIC_HAS_DUPLICATE_EXTENSIONS") }

        assertEquals(
            "declaration 'specific' extend 'Box<Int64>' has a conflicting extension",
            diagnostic.renderMessage(),
        )
    }

    @Test
    fun duplicatePrivateExtensionDiagnosticUsesCompleteExtendedType() {
        val diagnostic = diagnostics(
            mode = CfirCjmpMode.COMMON,
            source = """
                package cjmp_p
                public class Box<T> {}
                common extend Box<Int64> {
                    private func choose(): Int64 { 1 }
                }
                common extend Box<Int64> {
                    private func choose(): Int64 { 2 }
                }
            """.trimIndent(),
        ).single { it.factoryName.endsWith("COMMON_DIRECT_EXTENSION_HAS_DUPLICATE_PRIVATE_MEMBERS") }

        assertEquals(
            "declaration 'common' extend 'Box<Int64>' has a conflicting private function 'choose'",
            diagnostic.renderMessage(),
        )
    }

    @Test
    fun commonPrivateExtensionDiagnosticKeepsModifierQuotes() {
        val diagnostic = diagnostics(
            mode = CfirCjmpMode.COMMON,
            source = """
                common extend Int64 {
                    common private func choose(): Int64 { 1 }
                }
            """.trimIndent(),
        ).single { it.factoryName.endsWith("COMMON_DIRECT_EXTENSION_HAS_COMMON_PRIVATE_MEMBERS") }

        assertEquals(
            "'common' and 'private' modifier conflict on function 'choose' declaration",
            diagnostic.renderMessage(),
        )
    }

    @Test
    fun unmatchedSpecificDeclarationUsesCheckerFactsInRenderer() {
        val diagnostic = diagnostics(
            mode = CfirCjmpMode.SPECIFIC,
            source = """
                package cjmp_p
                public specific func orphan(): Int64 { 2 }
            """.trimIndent(),
        ).single { it.factoryName.endsWith("NOT_MATCHED") }

        assertEquals(
            "'specific' function 'orphan' can not find 'common' match",
            diagnostic.renderMessage(),
        )
    }

    @Test
    fun unmatchedEnumConstructorRendersItsContainingEnum() {
        val diagnostic = diagnostics(
            mode = CfirCjmpMode.SPECIFIC,
            source = """
                package cjmp_p
                public specific enum E {
                    | A
                }
            """.trimIndent(),
        ).single {
            it.factoryName.endsWith("NOT_MATCHED") && "constructor 'A'" in it.renderMessage()
        }

        assertEquals(
            "'specific' enum 'E' constructor 'A' can not find 'common' match",
            diagnostic.renderMessage(),
        )
    }

    @Test
    fun unmatchedPatternRootRendersItsStructuredKind() {
        val diagnostics = diagnostics(
            mode = CfirCjmpMode.SPECIFIC,
            source = "public specific let (a, b) = (1, 2)",
        ).filter { it.factoryName.endsWith("NOT_MATCHED") }

        assertTrue(diagnostics.any { "variable with pattern" in it.renderMessage() })
    }

    @Test
    fun unmatchedSpecificExtendRendersItsResolvedTypeArguments() {
        val diagnostic = diagnostics(
            mode = CfirCjmpMode.SPECIFIC,
            source = """
                package cjmp_p
                public class Box<T> {}
                public specific extend Box<Int64> {}
            """.trimIndent(),
        ).single { it.factoryName.endsWith("NOT_MATCHED") }

        assertTrue(diagnostic.renderMessage().contains("Box<Int64>"))
    }

    @Test
    fun unmatchedSpecificExtendWithUnresolvedTypeStillRenders() {
        val diagnostic = diagnostics(
            mode = CfirCjmpMode.SPECIFIC,
            source = "package cjmp_p\npublic specific extend MissingTarget {}",
        ).single { it.factoryName.endsWith("NOT_MATCHED") }

        assertEquals(
            "'specific' extend 'MissingTarget' can not find 'common' match",
            diagnostic.renderMessage(),
        )
    }

    @Test
    fun multipleImplementationKindUsesStructuredRenderer() {
        val declaration = createCjFile("cjmpMultipleImplementationRendering", "func sample() {}").declarations.single()
        val diagnostic = CjPsiDiagnosticWithParameters1(
            element = declaration.toCjPsiSourceElement(),
            a = CfirCjmpDiagnosticArgument.ImplementationKind(
                CfirCjmpImplementationKind.Declaration(CfirCjmpDiagnosticLabel.FUNCTION),
            ),
            severity = Severity.ERROR,
            factory = CfirErrors.MULTIPLE_COMMON_IMPLEMENTATIONS,
            positioningStrategy = SourceElementPositioningStrategies.DEFAULT,
            context = diagnosticContext(),
        )

        assertEquals("'common' function has several specific implementations", diagnostic.renderMessage())

        val distinctSemanticKinds = listOf(
            CfirCjmpImplementationKind.Declaration(CfirCjmpDiagnosticLabel.FUNCTION),
            CfirCjmpImplementationKind.Declaration(CfirCjmpDiagnosticLabel.FUNCTION),
            CfirCjmpImplementationKind.Property(org.cangnova.cangjie.name.Name.identifier("value")),
        ).distinct()
        assertEquals(2, distinctSemanticKinds.size)
    }

    @Test
    fun differentKindMessageKeepsQuotedDeclarationRoles() {
        val declaration = createCjFile("cjmpDiagnosticRendering", "class Sample {}").declarations.single()
        val diagnostic = CjPsiDiagnosticWithParameters2(
            element = declaration.toCjPsiSourceElement(),
            a = CfirCjmpDiagnosticArgument.DeclarationKind(CfirCjmpDiagnosticLabel.CLASS),
            b = CfirCjmpDiagnosticArgument.DeclarationKind(CfirCjmpDiagnosticLabel.INTERFACE),
            severity = Severity.ERROR,
            factory = CfirErrors.SPECIFIC_HAS_DIFFERENT_KIND,
            positioningStrategy = SourceElementPositioningStrategies.DEFAULT,
            context = diagnosticContext(),
        )

        assertEquals(
            "'specific' decl kind(class) is not equal to 'common'(interface)",
            diagnostic.renderMessage(),
        )
    }

    private fun diagnostics(mode: CfirCjmpMode, source: String): List<CjDiagnostic> {
        val session = createTestSession()
        session.register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent(explicitMode = mode))
        session.register(
            CfirExceptionHandler::class,
            object : CfirExceptionHandler() {
                override fun handleExceptionOnElementAnalysis(
                    element: org.cangnova.cangjie.cfir.CfirElement,
                    throwable: Throwable,
                ): Nothing = throw throwable

                override fun handleExceptionOnFileAnalysis(file: CfirFile, throwable: Throwable): Nothing =
                    throw throwable
            },
        )

        val cfirFile = createCjFile("cjmpDiagnosticArguments", source).toCfirFile(session = session)
        val resolveCollector = CfirDiagnosticCollector()
        resolveToPhase(cfirFile, session, targetPhase, resolveCollector)

        val checkerCollector = RecordingDiagnosticsCollector()
        for (mppCheckerKind in MppCheckerKind.entries) {
            session.runCheckers(ScopeSession(), listOf(cfirFile), checkerCollector, mppCheckerKind)
        }
        return resolveCollector.rawDiagnostics + checkerCollector.diagnostics
    }

    private fun diagnosticContext() = object : DiagnosticBaseContext {
        override val languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT
    }

    private class RecordingDiagnosticsCollector : BaseDiagnosticsCollector() {
        private val storage = mutableListOf<CjDiagnostic>()

        override val diagnostics: List<CjDiagnostic>
            get() = storage

        override val diagnosticsByFilePath: Map<String?, List<CjDiagnostic>>
            get() = storage.groupBy { null }

        override val hasErrors: Boolean
            get() = storage.any { it.severity.isError }

        override val hasWarningsForWError: Boolean
            get() = false

        override fun report(diagnostic: CjDiagnostic?, context: DiagnosticContext) {
            if (diagnostic != null) storage += diagnostic
        }
    }
}
