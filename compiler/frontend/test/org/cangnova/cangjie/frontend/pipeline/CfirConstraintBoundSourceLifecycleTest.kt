package org.cangnova.cangjie.frontend.pipeline

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirInterface
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.parsingDiagnosticPublication
import org.cangnova.cangjie.cfir.declarations.parsingFileFacts
import org.cangnova.cangjie.cfir.declarations.parsingSourceContext
import org.cangnova.cangjie.cfir.declarations.parserSettings
import org.cangnova.cangjie.cfir.declarations.replaceParsingSourceContext
import org.cangnova.cangjie.cfir.declarations.resolvePhase
import org.cangnova.cangjie.cfir.diagnostics.impl.DiagnosticsCollectorImpl
import org.cangnova.cangjie.cfir.resolve.CfirGenericConstraintBound
import org.cangnova.cangjie.cfir.resolve.CfirGenericConstraintBounds
import org.cangnova.cangjie.cfir.resolve.CfirGenericConstraintCompatibility
import org.cangnova.cangjie.cfir.resolve.CfirGenericConstraintParent
import org.cangnova.cangjie.cfir.resolve.providers.cfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompositeProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirEmptyProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirLibrarySessionProvider
import org.cangnova.cangjie.cfir.resolve.providers.getContainingFile
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.addCangJieSourceRoot
import org.cangnova.cangjie.config.diagnosticsCollector
import org.cangnova.cangjie.config.messageCollector
import org.cangnova.cangjie.config.moduleName
import org.cangnova.cangjie.config.useLightTree
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.CompilerMessageSourceLocation
import org.cangnova.cangjie.messages.MessageCollector
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

/**
 * Source bounds 必须消费真实 frontend 的 TYPES、owner 文件和 parser 产品。
 * 文件槽替换只回归原 LL seam 的输入失效边界，不代表完整 LL 缓存换代测试。
 */
@OptIn(CompilerConfiguration.Internals::class, CfirImplementationDetail::class)
class CfirConstraintBoundSourceLifecycleTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun psiCapturesAnalyzedSourceOwnerAndBoundDomain() = assertAnalyzedSourceStructure(false)

    @Test
    fun lightTreeCapturesAnalyzedSourceOwnerAndBoundDomain() = assertAnalyzedSourceStructure(true)

    @Test
    fun psiProviderRequiresExactSymbolAcrossFrontendSessions() = assertExactProviderBoundary(false)

    @Test
    fun lightTreeProviderRequiresExactSymbolAcrossFrontendSessions() = assertExactProviderBoundary(true)

    @Test
    fun psiCompositeProviderSeparatesNamespaceFromExactSymbolOwnership() = assertCompositeProviderBoundary(false)

    @Test
    fun lightTreeCompositeProviderSeparatesNamespaceFromExactSymbolOwnership() = assertCompositeProviderBoundary(true)

    @Test
    fun psiCompositeBoundCaptureConsumesOnlyTheActualOwnerFile() = assertCompositeBoundOwner(false)

    @Test
    fun lightTreeCompositeBoundCaptureConsumesOnlyTheActualOwnerFile() = assertCompositeBoundOwner(true)

    @Test
    fun psiSourceFileReplacementInvalidatesCapturedAndMappedInputs() = assertReplacementRejected(false, ProductSlot.SOURCE_FILE)

    @Test
    fun lightTreeSourceFileReplacementInvalidatesCapturedAndMappedInputs() = assertReplacementRejected(true, ProductSlot.SOURCE_FILE)

    @Test
    fun psiLinesReplacementInvalidatesCapturedAndMappedInputs() = assertReplacementRejected(false, ProductSlot.LINES)

    @Test
    fun lightTreeLinesReplacementInvalidatesCapturedAndMappedInputs() = assertReplacementRejected(true, ProductSlot.LINES)

    @Test
    fun psiParserSourceReplacementInvalidatesCapturedAndMappedInputs() = assertReplacementRejected(false, ProductSlot.PARSER_SOURCE)

    @Test
    fun lightTreeParserSourceReplacementInvalidatesCapturedAndMappedInputs() = assertReplacementRejected(true, ProductSlot.PARSER_SOURCE)

    @Test
    fun psiFileProductCommitInvalidatesCapturedAndMappedInputs() = assertReplacementRejected(false, ProductSlot.ALL)

    @Test
    fun lightTreeFileProductCommitInvalidatesCapturedAndMappedInputs() = assertReplacementRejected(true, ProductSlot.ALL)

    @Test
    fun psiSourceOriginAndRegisteredFileRequireActualSourceFile() = assertMissingSourceFileRejected(false)

    @Test
    fun lightTreeSourceOriginAndRegisteredFileRequireActualSourceFile() = assertMissingSourceFileRejected(true)

    private fun assertAnalyzedSourceStructure(useLightTree: Boolean) = withFrontendPair(useLightTree) { first, second ->
        assertSourceProduct(first)
        assertSourceProduct(second)
        val captured = CfirGenericConstraintBounds.capture(first.box.typeParameters)
        val original = captured.closedBounds(first.parameter.symbol).single()
        assertBoundSource(first, original)
        val derived = captured.mapTypes { second.reference.coneType }
        val mapped = derived.closedBounds(first.parameter.symbol).single()
        assertNotSame(original, mapped)
        assertBoundSource(first, mapped)
        assertSame(second.reference.coneType, mapped.type)
        assertSame(original.originalType, mapped.originalType)
        captured.requireParameters(first.box.typeParameters)
        derived.requireParameters(first.box.typeParameters)
        assertEquals(emptyList<Int>(), compare(first, captured, first, captured))
        val secondGraph = CfirGenericConstraintBounds.capture(second.box.typeParameters)
        assertEquals(emptyList<Int>(), compare(second, secondGraph, second, secondGraph))
    }

    /** namespace 查找允许命中同 ClassId；symbol 文件归属只能由各自原 provider 的精确登记回答。 */
    private fun assertExactProviderBoundary(useLightTree: Boolean) = withFrontendPair(useLightTree) { first, second ->
        assertSourceProduct(first)
        assertSourceProduct(second)
        val providerA = first.session.cfirProvider
        val providerB = second.session.cfirProvider
        assertNotSame(first.session, second.session)
        assertNotSame(providerA, providerB)
        assertNotSame(first.box.symbol, second.box.symbol)
        assertEquals(first.box.symbol.classId, second.box.symbol.classId)
        assertSame(first.file, providerA.getCfirClassifierContainerFileIfAny(second.box.symbol.classId))
        assertSame(first.file, providerA.getCfirClassifierContainerFile(second.box.symbol.classId))
        assertNull(providerA.getCfirClassifierContainerFileIfAny(second.box.symbol))
        assertNull(providerA.getContainingFile(second.box.symbol))
        assertThrows(IllegalStateException::class.java) { providerA.getCfirClassifierContainerFile(second.box.symbol) }
        assertSame(second.file, providerB.getCfirClassifierContainerFileIfAny(second.box.symbol))
        assertSame(second.file, providerB.getCfirClassifierContainerFile(second.box.symbol))
        assertSame(second.file, second.parameter.containingDeclarationSymbol.getContainingFile())
        val graphA = CfirGenericConstraintBounds.capture(first.box.typeParameters)
        val graphB = CfirGenericConstraintBounds.capture(second.box.typeParameters)
        assertBoundSource(first, graphA.closedBounds(first.parameter.symbol).single())
        assertBoundSource(second, graphB.closedBounds(second.parameter.symbol).single())
        graphA.validateOriginalInput()
        graphB.validateOriginalInput()
    }

    /** 名称首命中保持原顺序；symbol 归属在嵌套聚合及来源反序下都保持原文件。 */
    private fun assertCompositeProviderBoundary(useLightTree: Boolean) = withFrontendPair(useLightTree) { first, second ->
        assertSourceProduct(first)
        assertSourceProduct(second)
        val providerA = first.session.cfirProvider
        val providerB = second.session.cfirProvider
        val classId = first.box.symbol.classId
        for ((leading, trailing) in listOf(first to second, second to first)) {
            val composite = CfirCompositeProvider(leading.session, listOf(
                CfirCompositeProvider(leading.session, listOf(leading.session.cfirProvider)),
                trailing.session.cfirProvider,
            ))
            assertSame(leading.box, composite.getCfirClassifierByFqName(classId))
            assertSame(leading.box.symbol, composite.symbolProvider.getClassLikeSymbolByClassId(classId))
            assertSame(leading.file, composite.getCfirClassifierContainerFileIfAny(classId))
            assertSame(leading.file, composite.getCfirClassifierContainerFile(classId))
            for (product in listOf(first, second)) {
                assertSame(product.file, composite.getCfirClassifierContainerFileIfAny(product.box.symbol))
                assertSame(product.file, composite.getCfirClassifierContainerFile(product.box.symbol))
                assertSame(product.file, composite.getContainingFile(product.box.symbol))
            }
        }
        for ((known, unknown) in listOf(first to second, second to first)) {
            val composite = CfirCompositeProvider(unknown.session, listOf(known.session.cfirProvider))
            assertSame(known.file, composite.getCfirClassifierContainerFileIfAny(unknown.box.symbol.classId))
            assertNull(composite.getCfirClassifierContainerFileIfAny(unknown.box.symbol))
            assertNull(composite.getContainingFile(unknown.box.symbol))
            assertThrows(IllegalStateException::class.java) { composite.getCfirClassifierContainerFile(unknown.box.symbol) }
        }
        for (noSource in listOf(CfirEmptyProvider(providerA.symbolProvider), CfirLibrarySessionProvider(providerB.symbolProvider))) {
            assertNull(noSource.getCfirClassifierContainerFileIfAny(second.box.symbol))
            assertThrows(IllegalStateException::class.java) { noSource.getCfirClassifierContainerFile(second.box.symbol) }
        }
        assertSame(providerA, first.session.cfirProvider)
        assertSame(providerB, second.session.cfirProvider)
    }

    /** bounds 消费真实 owner session 的聚合来源，不借 namespace 首命中文件的初始化产品。 */
    private fun assertCompositeBoundOwner(useLightTree: Boolean) = withFrontendPair(useLightTree) { first, second ->
        assertSourceProduct(first)
        assertSourceProduct(second)
        val providerA = first.session.cfirProvider
        val providerB = second.session.cfirProvider
        val sourceFileA = checkNotNull(first.file.sourceFile)
        val sourceFileB = checkNotNull(second.file.sourceFile)
        val providers = listOf(providerA, providerB)
        try {
            first.session.register(CfirProvider::class, CfirCompositeProvider(first.session, providers))
            second.session.register(CfirProvider::class, CfirCompositeProvider(second.session, providers))
            assertSame(first.file, second.session.cfirProvider.getCfirClassifierContainerFileIfAny(second.box.symbol.classId))
            assertSame(second.file, second.session.cfirProvider.getCfirClassifierContainerFileIfAny(second.box.symbol))
            val graphA = CfirGenericConstraintBounds.capture(first.box.typeParameters)
            val graphB = CfirGenericConstraintBounds.capture(second.box.typeParameters)
            val mappedB = graphB.mapTypes { first.reference.coneType }
            assertBoundSource(first, graphA.closedBounds(first.parameter.symbol).single())
            assertBoundSource(second, graphB.closedBounds(second.parameter.symbol).single())
            val mapped = mappedB.closedBounds(second.parameter.symbol).single()
            assertBoundSource(second, mapped)
            assertSame(first.reference.coneType, mapped.type)

            first.file.replaceSourceFile(null)
            for (input in listOf(graphB, mappedB)) {
                input.validateOriginalInput()
                input.requireParameters(second.box.typeParameters)
                assertBoundSource(second, input.closedBounds(second.parameter.symbol).single())
            }
            assertBoundSource(second, CfirGenericConstraintBounds.capture(second.box.typeParameters).closedBounds(second.parameter.symbol).single())
            val missingA = assertThrows(IllegalStateException::class.java) { CfirGenericConstraintBounds.capture(first.box.typeParameters) }
            assertEquals("Source bounds require the original source file", missingA.message)
            assertThrows(IllegalStateException::class.java) { graphA.validateOriginalInput() }
            first.file.replaceSourceFile(sourceFileA)

            second.file.replaceSourceFile(null)
            val missingB = assertThrows(IllegalStateException::class.java) { CfirGenericConstraintBounds.capture(second.box.typeParameters) }
            assertEquals("Source bounds require the original source file", missingB.message)
            for (input in listOf(graphB, mappedB)) {
                val failure = assertThrows(IllegalStateException::class.java) { input.validateOriginalInput() }
                assertEquals("Original source product changed after bound capture", failure.message)
                assertThrows(IllegalStateException::class.java) { input.closedBounds(second.parameter.symbol) }
                var conversions = 0
                assertThrows(IllegalStateException::class.java) { input.mapTypes { conversions++; it.type } }
                assertEquals(0, conversions)
                assertThrows(IllegalStateException::class.java) { compare(second, input, first, graphA) }
                assertThrows(IllegalStateException::class.java) { compare(first, graphA, second, input) }
            }
            graphA.validateOriginalInput()
            assertEquals(emptyList<Int>(), compare(first, graphA, first, graphA))
            second.file.replaceSourceFile(sourceFileB)

            val withoutOwner = CfirCompositeProvider(second.session, listOf(providerA))
            second.session.register(CfirProvider::class, withoutOwner)
            assertSame(first.file, withoutOwner.getCfirClassifierContainerFileIfAny(second.box.symbol.classId))
            assertNull(withoutOwner.getContainingFile(second.box.symbol))
            val missingOwner = assertThrows(IllegalStateException::class.java) { CfirGenericConstraintBounds.capture(second.box.typeParameters) }
            assertEquals("Source bounds require their original containing file product", missingOwner.message)
        } finally {
            first.file.replaceSourceFile(sourceFileA)
            second.file.replaceSourceFile(sourceFileB)
            first.session.register(CfirProvider::class, providerA)
            second.session.register(CfirProvider::class, providerB)
        }
        assertSourceProduct(first)
        assertSourceProduct(second)
    }

    /** 三个新槽值均取自第二次同语义 frontend 产品；不构造 source、parser context 或 line mapping。 */
    private fun assertReplacementRejected(useLightTree: Boolean, slot: ProductSlot) = withFrontendPair(useLightTree) { first, second ->
        assertSourceProduct(first)
        assertSourceProduct(second)
        val file = first.file
        val replacement = second.file
        val facts = checkNotNull(file.parsingFileFacts)
        val replacementFacts = checkNotNull(replacement.parsingFileFacts)
        val parser = checkNotNull(file.parsingSourceContext)
        val replacementParser = checkNotNull(replacement.parsingSourceContext)
        val diagnostics = checkNotNull(replacement.parsingDiagnosticPublication)
        val settings = checkNotNull(replacement.parserSettings)
        assertEquals(facts, replacementFacts)
        assertEquals(file.parserSettings, settings)
        assertEquals(parser.snapshot.text, replacementParser.snapshot.text)
        assertNotSame(parser, replacementParser)
        assertNotSame(file.sourceFile, replacement.sourceFile)
        assertNotSame(file.sourceFileLinesMapping, replacement.sourceFileLinesMapping)

        val originalFileSource = file.source
        val originalOwnerSource = first.box.source
        val originalParameterSource = first.parameter.source
        val originalReferenceSource = first.reference.source
        val captured = CfirGenericConstraintBounds.capture(first.box.typeParameters)
        val original = captured.closedBounds(first.parameter.symbol).single()
        val mapped = captured.mapTypes { second.reference.coneType }
        val derived = mapped.closedBounds(first.parameter.symbol).single()
        assertBoundSource(first, original)
        assertBoundSource(first, derived)
        assertSame(second.reference.coneType, derived.type)
        val valid = CfirGenericConstraintBounds.capture(second.box.typeParameters)
        assertEquals(emptyList<Int>(), compare(first, captured, first, captured))
        assertEquals(emptyList<Int>(), compare(second, valid, second, valid))

        synchronized(first.session) {
            if (slot == ProductSlot.SOURCE_FILE || slot == ProductSlot.ALL) {
                file.replaceSourceFile(checkNotNull(replacement.sourceFile))
            }
            if (slot == ProductSlot.LINES || slot == ProductSlot.ALL) {
                file.replaceSourceFileLinesMapping(checkNotNull(replacement.sourceFileLinesMapping))
            }
            if (slot == ProductSlot.PARSER_SOURCE || slot == ProductSlot.ALL) {
                file.replaceParsingSourceContext(parser, replacementParser, replacementFacts, diagnostics, settings)
            }
        }

        assertSame(file, file.symbol.cfir)
        assertSame(first.box, first.box.symbol.cfir)
        assertSame(first.parameter, first.parameter.symbol.cfir)
        assertSame(first.box.symbol, first.parameter.containingDeclarationSymbol)
        assertSame(first.reference, first.parameter.bounds.single())
        assertSame(original.originalRootType, first.reference.coneType)
        assertSame(first.session, file.moduleData.session)
        assertSame(first.file.moduleData, first.box.moduleData)
        assertSame(first.file.moduleData, first.parameter.moduleData)
        assertSame(file, first.parameter.containingDeclarationSymbol.getContainingFile())
        assertSame(originalFileSource, file.source)
        assertSame(originalOwnerSource, first.box.source)
        assertSame(originalParameterSource, first.parameter.source)
        assertSame(originalReferenceSource, first.reference.source)
        assertSame(facts, file.parsingFileFacts)
        if (slot == ProductSlot.SOURCE_FILE || slot == ProductSlot.ALL) assertSame(replacement.sourceFile, file.sourceFile)
        if (slot == ProductSlot.LINES || slot == ProductSlot.ALL) assertSame(replacement.sourceFileLinesMapping, file.sourceFileLinesMapping)
        if (slot == ProductSlot.PARSER_SOURCE || slot == ProductSlot.ALL) {
            assertSame(replacementParser, file.parsingSourceContext)
            assertSame(diagnostics, file.parsingDiagnosticPublication)
        }

        for (input in listOf(captured, mapped)) {
            val failure = assertThrows(IllegalStateException::class.java) { input.validateOriginalInput() }
            assertEquals("Original source product changed after bound capture", failure.message)
            assertThrows(IllegalStateException::class.java) { input.requireParameters(first.box.typeParameters) }
            assertThrows(IllegalStateException::class.java) { input.closedBounds(first.parameter.symbol) }
            var conversions = 0
            assertThrows(IllegalStateException::class.java) { input.mapTypes { conversions++; it.type } }
            assertEquals(0, conversions)
            assertThrows(IllegalStateException::class.java) { compare(first, input, second, valid) }
            assertThrows(IllegalStateException::class.java) { compare(second, valid, first, input) }
        }
        valid.validateOriginalInput()
        assertBoundSource(second, valid.closedBounds(second.parameter.symbol).single())
    }

    /** 即使 origin 为 Source 且 owner 已登记，缺少真实 sourceFile 产品也不能只凭 marker 捕获。 */
    private fun assertMissingSourceFileRejected(useLightTree: Boolean) = withFrontendPair(useLightTree) { first, _ ->
        assertSourceProduct(first)
        val sourceFile = checkNotNull(first.file.sourceFile)
        try {
            first.file.replaceSourceFile(null)
            assertTrue(first.parameter.origin.fromSource)
            assertSame(first.file, first.parameter.containingDeclarationSymbol.getContainingFile())
            val failure = assertThrows(IllegalStateException::class.java) {
                CfirGenericConstraintBounds.capture(first.box.typeParameters)
            }
            assertEquals("Source bounds require the original source file", failure.message)
        } finally {
            first.file.replaceSourceFile(sourceFile)
        }
        CfirGenericConstraintBounds.capture(first.box.typeParameters).validateOriginalInput()
    }

    private fun compare(
        child: AnalyzedSource,
        childBounds: CfirGenericConstraintBounds,
        parent: AnalyzedSource,
        parentBounds: CfirGenericConstraintBounds,
    ): List<Int> = CfirGenericConstraintCompatibility.findIncompatibleTypeParameterIndices(
        child.box.typeParameters,
        listOf(CfirGenericConstraintParent(parent.box.typeParameters, ConeSubstitutor.Empty, parentBounds)),
        child.session.typeContext,
        ConeSubstitutor.Empty,
        childBounds,
    )

    private fun assertBoundSource(product: AnalyzedSource, bound: CfirGenericConstraintBound) {
        assertSame(product.parameter.symbol, bound.owner)
        assertSame(product.parameter, bound.declaration)
        assertSame(product.box.symbol, bound.containingDeclarationSymbol)
        assertSame(product.reference, bound.reference)
        assertSame(product.reference.coneType, bound.originalRootType)
        assertSame(product.reference.coneType, bound.originalType)
        assertSame(product.session, bound.domain)
        assertEquals(CfirSession.Kind.Source, bound.domain.kind)
        assertEquals(0, bound.boundOrdinal)
        assertEquals(emptyList<Int>(), bound.componentPath)
        assertSame(product.file, bound.containingDeclarationSymbol.getContainingFile())
        assertSame(product.bound.symbol, checkNotNull(bound.originalType as? ConeClassLikeType).toSymbol(bound.domain))
    }

    /** phase 仅断言，不写 resolveState；文件、声明及 parser 事实均由正常管线产出。 */
    private fun assertSourceProduct(product: AnalyzedSource) {
        assertEquals(CfirSession.Kind.Source, product.session.kind)
        assertTrue(product.file.origin.fromSource)
        assertTrue(product.box.origin.fromSource)
        assertTrue(product.parameter.origin.fromSource)
        assertTrue(product.box.resolvePhase >= CfirResolvePhase.TYPES)
        assertTrue(product.parameter.resolvePhase >= CfirResolvePhase.TYPES)
        assertSame(product.file, product.file.symbol.cfir)
        assertSame(product.box, product.box.symbol.cfir)
        assertSame(product.parameter, product.parameter.symbol.cfir)
        assertSame(product.box.symbol, product.parameter.containingDeclarationSymbol)
        assertSame(product.session, product.parameter.moduleData.session)
        assertSame(product.file.moduleData, product.box.moduleData)
        assertSame(product.file.moduleData, product.parameter.moduleData)
        assertSame(product.reference, product.parameter.bounds.single())
        assertSame(product.file, product.parameter.containingDeclarationSymbol.getContainingFile())
        assertNotNull(product.file.source)
        assertNotNull(product.box.source)
        assertNotNull(product.parameter.source)
        assertNotNull(product.reference.source)
        val source = checkNotNull(product.file.sourceFile)
        assertEquals(product.path.toAbsolutePath().normalize(), Path.of(checkNotNull(source.path)).toAbsolutePath().normalize())
        assertNotNull(product.file.sourceFileLinesMapping)
        assertNotNull(product.file.parsingFileFacts)
        assertNotNull(product.file.parserSettings)
        assertTrue(checkNotNull(product.file.parsingDiagnosticPublication).diagnostics.none { it.isError })
        val parser = checkNotNull(product.file.parsingSourceContext)
        assertEquals(SOURCE, parser.snapshot.text)
        parser.requireInput(SOURCE)
    }

    /** 每个测试保留两次真实输出的环境直到断言结束，由 JUnit 清理运行时 UTF-8 源文件。 */
    private fun withFrontendPair(useLightTree: Boolean, action: (AnalyzedSource, AnalyzedSource) -> Unit) {
        val disposable = Disposer.newDisposable("constraint-bound-source-lifecycle")
        try {
            val first = analyze(tempDir.resolve("a.cj"), useLightTree, disposable)
            val second = analyze(tempDir.resolve("b.cj"), useLightTree, disposable)
            assertNotSame(first.file, second.file)
            assertNotSame(first.parameter, second.parameter)
            assertNotSame(first.reference, second.reference)
            assertNotSame(first.session, second.session)
            action(first, second)
        } finally {
            Disposer.dispose(disposable)
        }
    }

    private fun analyze(path: Path, lightTree: Boolean, disposable: Disposable): AnalyzedSource {
        Files.writeString(path, SOURCE, StandardCharsets.UTF_8)
        val messages = RecordingMessageCollector()
        val diagnostics = DiagnosticsCollectorImpl()
        val configuration = CompilerConfiguration().apply {
            messageCollector = messages
            diagnosticsCollector = diagnostics
            moduleName = "constraint_bound_source"
            useLightTree = lightTree
            addCangJieSourceRoot(path.toString())
        }
        val artifact = CfirFrontendPipelinePhase.executePhase(ConfigurationPipelineArtifact(configuration, disposable))
        assertFalse(messages.hasErrors(), messages.messages.toString())
        assertTrue(diagnostics.diagnostics.none { it.severity.isError }, diagnostics.diagnostics.toString())
        assertNotNull(artifact, "实际 Source 前端必须完成：${messages.messages}")
        val output = checkNotNull(artifact).frontendOutput.outputs.single()
        val file = output.fir.single()
        val box = file.declarations.filterIsInstance<CfirClass>().single { it.name.asString() == "Box" }
        val bound = file.declarations.filterIsInstance<CfirInterface>().single { it.name.asString() == "Bound" }
        val parameter = box.typeParameters.single()
        assertEquals("T", parameter.name.asString())
        assertTrue(parameter.bounds.single() is CfirResolvedTypeRef)
        return AnalyzedSource(path, output.session, file, box, bound, parameter, parameter.bounds.single() as CfirResolvedTypeRef)
    }

    private data class AnalyzedSource(
        val path: Path,
        val session: CfirSession,
        val file: CfirFile,
        val box: CfirClass,
        val bound: CfirInterface,
        val parameter: CfirTypeParameter,
        val reference: CfirResolvedTypeRef,
    )

    private enum class ProductSlot { SOURCE_FILE, LINES, PARSER_SOURCE, ALL }

    private class RecordingMessageCollector : MessageCollector {
        val messages = mutableListOf<Pair<CompilerMessageSeverity, String>>()
        override fun clear() = messages.clear()
        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
            messages += severity to message
        }
        override fun hasErrors(): Boolean = messages.any { it.first.isError }
    }

    companion object {
        // 官方 generic_constraint / generic_class：约束位于声明体前，空 init 使用真实仓颉文法。
        private val SOURCE = """
            package constraint_bound_source
            public interface Bound {}
            public class Box<T> where T <: Bound {
                public init() {}
            }
        """.trimIndent() + "\n"
    }
}
