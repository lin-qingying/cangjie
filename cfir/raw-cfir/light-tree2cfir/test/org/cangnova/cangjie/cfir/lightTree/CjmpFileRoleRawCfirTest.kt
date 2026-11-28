package org.cangnova.cangjie.cfir.lightTree

import com.intellij.lang.PsiBuilderFactory
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.builder.toParsingContext
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.parsingFileFacts
import org.cangnova.cangjie.cfir.declarations.parsingDiagnosticPublication
import org.cangnova.cangjie.cfir.declarations.parsingSourceContext
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpSettingsComponent
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.parsing.CangJieParser
import org.cangnova.cangjie.parsing.CjParserSettings
import org.cangnova.cangjie.parsing.parsedFileFacts
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.parsing.CjmpFileRole
import org.cangnova.cangjie.parsing.cjmpParsingContext
import org.cangnova.cangjie.parsing.collectParsingDiagnostics
import org.cangnova.cangjie.source.toSourceLinesMapping
import org.cangnova.cangjie.test.JUnit3RunnerWithInners
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertNotSame as differentFrom
import kotlin.test.assertTrue

/** 文件角色检查的有效修饰符必须从 parser 传递到两条 raw CFIR 路径。 */
@RunWith(JUnit3RunnerWithInners::class)
class CjmpFileRoleRawCfirTest : AbstractLightTree2CfirConverterTestCase() {
    fun testPsiCommonModeRejectsSpecific() = checkWrongRole(commonMode = true, lightTree = false)
    fun testPsiSpecificModeRejectsCommon() = checkWrongRole(commonMode = false, lightTree = false)
    fun testLightTreeCommonModeRejectsSpecific() = checkWrongRole(commonMode = true, lightTree = true)
    fun testLightTreeSpecificModeRejectsCommon() = checkWrongRole(commonMode = false, lightTree = true)

    fun testPsiWrongRoleExtendRetainsItsSingleRoleModifier() = checkWrongRoleExtend(lightTree = false)
    fun testLightTreeWrongRoleExtendRetainsItsSingleRoleModifier() = checkWrongRoleExtend(lightTree = true)

    private fun checkWrongRoleExtend(lightTree: Boolean) {
        for (commonMode in listOf(false, true)) {
            val role = if (commonMode) "specific" else "common"
            val file = build("$role extend Int64 {}", LanguageVersion.CANGJIE_1_1_3, commonMode, lightTree)
            val extension = file.declarations.filterIsInstance<CfirExtend>().single()
            assertEquals(!commonMode, extension.status.isCommon, "common=$commonMode lightTree=$lightTree")
            assertEquals(commonMode, extension.status.isSpecific, "common=$commonMode lightTree=$lightTree")
        }
    }

    fun testPsiExtendWithMultipleModifiersPublishesNone() = checkMultipleExtendModifiers(lightTree = false)
    fun testLightTreeExtendWithMultipleModifiersPublishesNone() = checkMultipleExtendModifiers(lightTree = true)

    private fun checkMultipleExtendModifiers(lightTree: Boolean) {
        val file = build("public specific extend Int64 {}", LanguageVersion.CANGJIE_1_1_3, false, lightTree)
        val extension = file.declarations.filterIsInstance<CfirExtend>().single()
        assertEquals(false, extension.status.isSpecific)
        assertEquals(false, extension.status.isVisibilityExplicit)
    }

    fun testExtendRoleModifiersUseVersionedSpellingAndTokenSet() {
        for (lightTree in listOf(false, true)) {
            val repeated = build("specific specific extend Int64 {}", LanguageVersion.CANGJIE_1_1_3, false, lightTree)
                .declarations.filterIsInstance<CfirExtend>().single()
            assertTrue(repeated.status.isSpecific, "repeated token, lightTree=$lightTree")

            val historical = build("platform extend Int64 {}", LanguageVersion.CANGJIE_1_1_0_ALPHA_65, false, lightTree)
                .declarations.filterIsInstance<CfirExtend>().single()
            assertTrue(historical.status.isSpecific, "historical spelling, lightTree=$lightTree")

            val distinct = build("platform specific extend Int64 {}", LanguageVersion.CANGJIE_1_1_0_ALPHA_65, false, lightTree)
                .declarations.filterIsInstance<CfirExtend>().single()
            assertEquals(false, distinct.status.isSpecific, "distinct spellings, lightTree=$lightTree")
        }
    }

    fun testExtendRoleModifierIsInactiveWithoutCjmpCompilation() {
        for (lightTree in listOf(false, true)) {
            val extension = build(
                "specific extend Int64 {}", LanguageVersion.CANGJIE_1_1_3, false, lightTree, CfirCjmpMode.NONE,
            ).declarations.filterIsInstance<CfirExtend>().single()
            assertEquals(false, extension.status.isSpecific, "lightTree=$lightTree")
        }
    }

    private fun checkWrongRole(commonMode: Boolean, lightTree: Boolean) {
        val role = if (commonMode) "specific" else "common"
        val file = build("public unsafe $role func wrong() {}", LanguageVersion.CANGJIE_1_1_3,
            commonMode, lightTree)
        val function = file.declarations.filterIsInstance<CfirNamedFunction>().single()
        assertEquals(
            listOf(false, false, false),
            listOf(function.status.isCommon, function.status.isSpecific, function.status.isUnsafe),
            "common=$commonMode lightTree=$lightTree; all explicit modifiers must be rejected",
        )
    }

    fun testAlpha20EitherModeAcceptsMixedRoles() {
        for (lightTree in listOf(false, true)) {
            for (commonMode in listOf(false, true)) {
                val file = build("common func first() {}\nspecific func second() {}",
                    LanguageVersion.CANGJIE_1_2_0_ALPHA_20, commonMode, lightTree)
                val functions = file.declarations.filterIsInstance<CfirNamedFunction>().associateBy { it.name.asString() }
                assertEquals(setOf("first", "second"), functions.keys)
                assertTrue(functions.getValue("first").status.isCommon)
                assertTrue(functions.getValue("second").status.isSpecific)
            }
        }
    }

    fun testSyntaxOnlyTreeMustNotBypassCommonSessionRoleChecks() {
        val source = "public unsafe specific func wrong() {}"
        val settings = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_3)
        val session = createTestSession(settings)
        session.register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent(explicitMode = CfirCjmpMode.COMMON))
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        val parsed = CangJieLightParser.parseBuffered(builder)
        val sourceFile = CjInMemoryTextSourceFile("roles.cj", null, source)
        val file = LightTree2Cfir(session, session.cangjieScopeProvider, BodyBuildingMode.NORMAL)
            .buildPreMacroCfirFile(parsed, sourceFile, source.toSourceLinesMapping()).cfirFile
        val function = file.declarations.filterIsInstance<CfirNamedFunction>().single()
        assertEquals(listOf(false, false), listOf(function.status.isSpecific, function.status.isUnsafe))
    }

    fun testSyntaxOnlyPsiUsesEachOwnerSessionForRoleChecks() {
        val source = "public unsafe specific func wrong() {}"
        val psiFile = createCjFile("sharedRoles", source)
        // 保留共享 SyntaxOnly 来源；每个 owner 的解析产品都由正式 factory 单独创建。
        val originalRoot = psiFile.node
        val originalDiagnostics = psiFile.collectParsingDiagnostics()
        val originalContext = psiFile.cjmpParsingContext
        val originalFacts = psiFile.parsedFileFacts
        val settings = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_3)
        fun buildFor(mode: CfirCjmpMode): CfirNamedFunction {
            val session = createTestSession(settings)
            val cjmpSettings = CfirCjmpSettingsComponent(explicitMode = mode)
            session.register(CfirCjmpSettingsComponent::class, cjmpSettings)
            val context = cjmpSettings.toParsingContext(settings)
            val product = CangJieParser.parseCompilationFile(psiFile, context, CjParserSettings(settings.languageVersion))
            assertSame(psiFile, product.originFile)
            differentFrom(psiFile, product.file)
            differentFrom(originalRoot, product.file.node)
            assertEquals(source, product.file.text)
            assertEquals(context, product.file.cjmpParsingContext)
            val roleError = CangJieParsingBundle.message("parsing.error.cjmp.specific.in.non.specific.file")
            assertEquals(mode == CfirCjmpMode.COMMON, product.diagnostics.any { it.message == roleError })
            val targetDeclaration = product.file.declarations.filterIsInstance<CjNamedFunction>().single()
            val rawBuilder = PsiRawCfirBuilder(session, BodyBuildingMode.NORMAL, CfirAnnotationMetadataBuilder())
            val file = product.withTargetFile { rawBuilder.buildCfirFile(it) }
            val function = file.declarations.filterIsInstance<CfirNamedFunction>().single()
            val range = checkNotNull(function.source)
            assertEquals(targetDeclaration.textRange.startOffset, range.startOffset)
            assertEquals(targetDeclaration.textRange.endOffset, range.endOffset)
            assertEquals(targetDeclaration.text, source.substring(range.startOffset, range.endOffset))
            product.requireConsistentInput()
            return function
        }
        val rejected = buildFor(CfirCjmpMode.COMMON)
        val accepted = buildFor(CfirCjmpMode.SPECIFIC)
        assertEquals(listOf(false, false), listOf(rejected.status.isSpecific, rejected.status.isUnsafe))
        assertEquals(listOf(true, true), listOf(accepted.status.isSpecific, accepted.status.isUnsafe))
        assertSame(originalRoot, psiFile.node)
        assertEquals(source, psiFile.text)
        assertEquals(originalContext, psiFile.cjmpParsingContext)
        assertEquals(originalFacts, psiFile.parsedFileFacts)
        assertEquals(originalDiagnostics, psiFile.collectParsingDiagnostics())
    }

    fun testSyntaxOnlyFileRoleMustReflectOwnerSession() {
        val source = "specific func wrong() {}"
        val settings = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_3)
        val session = createTestSession(settings)
        session.register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent(explicitMode = CfirCjmpMode.COMMON))
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        val parsed = CangJieLightParser.parseBuffered(builder)
        val sourceFile = CjInMemoryTextSourceFile("roles.cj", null, source)
        val file = LightTree2Cfir(session, session.cangjieScopeProvider, BodyBuildingMode.NORMAL)
            .buildPreMacroCfirFile(parsed, sourceFile, source.toSourceLinesMapping()).cfirFile
        assertEquals(CjmpFileRole.NONE, checkNotNull(file.parsingFileFacts).cjmpRole)
    }

    fun testBufferedFileIsReparsedForItsOwnerSession() {
        val source = "specific func wrong() {}"
        val settings = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_3)
        val session = createTestSession(settings)
        session.register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent(explicitMode = CfirCjmpMode.COMMON))
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        val parsed = CangJieLightParser.parseBuffered(builder)
        val sourceFile = CjInMemoryTextSourceFile("roles.cj", null, source)
        val file = LightTree2Cfir(session, session.cangjieScopeProvider, BodyBuildingMode.NORMAL)
            .buildPreMacroCfirFile(parsed, sourceFile, source.toSourceLinesMapping()).cfirFile
        assertEquals(CjmpFileRole.NONE, checkNotNull(file.parsingFileFacts).cjmpRole)
        assertEquals(false, file.declarations.filterIsInstance<CfirNamedFunction>().single().status.isSpecific)
    }

    fun testCannotReuseExternallyReportedDiagnosticsUnderAnotherSession() {
        val source = "specific func wrong() {}"
        val settings = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_3)
        val session = createTestSession(settings)
        session.register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent(explicitMode = CfirCjmpMode.COMMON))
        fun builder() = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        val external = CangJieLightParser.parse(builder(), errorListener = {})
        val buffered = CangJieLightParser.parseBuffered(builder())
        val converter = LightTree2Cfir(session, session.cangjieScopeProvider, BodyBuildingMode.NORMAL)
        val file = CjInMemoryTextSourceFile("roles.cj", null, source)
        assertFailsWith<IllegalArgumentException> { converter.buildPreMacroCfirFile(external, file, source.toSourceLinesMapping()) }
        assertFailsWith<IllegalArgumentException> { converter.buildPreMacroCfirFile(buffered.file, file, source.toSourceLinesMapping()) }
    }

    fun testBufferedReparsePublishesOnlyFinalContextDiagnostics() {
        val source = "specific func wrong() {}"
        val settings = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_3)
        val session = createTestSession(settings)
        session.register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent(explicitMode = CfirCjmpMode.COMMON))
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        val parsed = CangJieLightParser.parseBuffered(builder)
        val roleMessage = CangJieParsingBundle.message("parsing.error.cjmp.specific.in.non.specific.file")
        val originalPublication = parsed.file.diagnosticPublication
        assertTrue(parsed.diagnostics.none { it.message == roleMessage })
        val converter = LightTree2Cfir(session, session.cangjieScopeProvider, BodyBuildingMode.NORMAL)
        val sourceFile = CjInMemoryTextSourceFile("roles.cj", null, source)
        val preFile = converter.buildPreMacroCfirFile(parsed, sourceFile, source.toSourceLinesMapping())
        val publication = preFile.parsingDiagnosticPublication
        assertSame(publication, preFile.cfirFile.parsingDiagnosticPublication)
        assertSame(originalPublication, parsed.file.diagnosticPublication)
        assertEquals(source, checkNotNull(preFile.cfirFile.parsingSourceContext).snapshot.text)
        assertEquals(CjmpFileRole.NONE, checkNotNull(preFile.cfirFile.parsingFileFacts).cjmpRole)
        assertEquals(1, publication.diagnostics.count { it.message == roleMessage })
        assertEquals(1, publication.reports.count { it.diagnostic.message == roleMessage })
        val roleError = publication.diagnostics.single { it.message == roleMessage }
        assertTrue(roleError.isError)
        assertEquals(ParsingDiagnostic.Origin.ORIGINAL_FILE, roleError.origin)
        assertEquals(1 to 1, checkNotNull(roleError.mainRange).begin)
        assertEquals(source, checkNotNull(roleError.mainRange).source.text)
        assertFailsWith<IllegalStateException> { converter.buildPreMacroCfirFile(parsed, sourceFile, source.toSourceLinesMapping()) }
        assertSame(publication, preFile.cfirFile.parsingDiagnosticPublication)
        assertEquals(1, publication.diagnostics.count { it.message == roleMessage })
        assertEquals(1, publication.reports.count { it.diagnostic.message == roleMessage })
    }

    fun testBufferedReparseDiscardsPreviousContextDiagnostics() {
        val source = "common let (a, b) = value"
        val settings = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_3)
        val session = createTestSession(settings)
        session.register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent(explicitMode = CfirCjmpMode.NONE))
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        val parsed = CangJieLightParser.parseBuffered(builder)
        val originalPublication = parsed.file.diagnosticPublication
        val oldDiagnostic = CangJieParsingBundle.message("parsing.error.cjmp.pattern.declaration", "tuple", "common")
        assertTrue(parsed.diagnostics.any { it.message == oldDiagnostic })
        val preFile = LightTree2Cfir(session, session.cangjieScopeProvider, BodyBuildingMode.NORMAL)
            .buildPreMacroCfirFile(parsed, CjInMemoryTextSourceFile("roles.cj", null, source), source.toSourceLinesMapping())
        val publication = preFile.parsingDiagnosticPublication
        assertSame(publication, preFile.cfirFile.parsingDiagnosticPublication)
        assertSame(originalPublication, parsed.file.diagnosticPublication)
        assertTrue(originalPublication.diagnostics.any { it.message == oldDiagnostic })
        assertTrue(publication.diagnostics.none { it.message == oldDiagnostic })
        assertTrue(publication.reports.none { it.diagnostic.message == oldDiagnostic })
        assertEquals(source, checkNotNull(preFile.cfirFile.parsingSourceContext).snapshot.text)
    }

    fun testBufferedReparseRetainsDeclarationKindAndCrlfPositions() {
        val source = "\r\nspecific func wrong(): Unit\r\n"
        val settings = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_3)
        val session = createTestSession(settings)
        session.register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent(explicitMode = CfirCjmpMode.COMMON))
        val sourceFile = CjInMemoryTextSourceFile("roles.cj.d", "roles.cj.d", source, explicitKind = CjSourceKind.DECLARATION)
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        val parsed = CangJieLightParser.parseBuffered(builder, sourceKind = CjSourceKind.DECLARATION, sourceFile = sourceFile)
        val snapshot = parsed.file.sourceContext.snapshot
        assertEquals(sourceFile.name, snapshot.fileName)
        assertEquals(sourceFile.path, snapshot.filePath)
        assertEquals(source, snapshot.text)
        assertSame(snapshot, parsed.file.lexicalInput.source)
        val preFile = LightTree2Cfir(session, session.cangjieScopeProvider, BodyBuildingMode.NORMAL)
            .buildPreMacroCfirFile(parsed, sourceFile, source.toSourceLinesMapping())
        val file = preFile.cfirFile
        val publication = preFile.parsingDiagnosticPublication
        assertSame(publication, file.parsingDiagnosticPublication)
        assertEquals(CjSourceKind.DECLARATION, checkNotNull(file.parsingFileFacts).sourceKind)
        val targetInput = checkNotNull(file.parsingSourceContext)
        assertSame(snapshot, targetInput.snapshot)
        assertEquals(parsed.file.sourceContext, targetInput)
        targetInput.requireInput(source)
        val roleError = publication.diagnostics.single { it.message == CangJieParsingBundle.message("parsing.error.cjmp.specific.in.non.specific.file") }
        val range = checkNotNull(roleError.mainRange)
        assertTrue(roleError.isError)
        assertEquals(ParsingDiagnostic.Origin.ORIGINAL_FILE, roleError.origin)
        assertSame(snapshot, range.source)
        assertEquals(sourceFile.path, range.source.filePath)
        assertEquals(sourceFile.name, range.source.fileName)
        assertEquals(source, range.source.text)
        assertEquals(1, range.begin.first)
        assertEquals(1, range.begin.second)
        assertEquals(1 to 2, range.end)
        val sourceRange = checkNotNull(range.sourceRange)
        assertSame(snapshot, sourceRange.source)
        assertEquals(0, sourceRange.startOffset)
        assertEquals(0, sourceRange.endOffset)
    }

    fun testBufferedReparseRejectsMappingFromDifferentTextBeforeReporting() {
        val source = "specific func wrong() {}"
        val settings = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_3)
        val session = createTestSession(settings)
        session.register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent(explicitMode = CfirCjmpMode.COMMON))
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        val parsed = CangJieLightParser.parseBuffered(builder)
        val originalPublication = parsed.file.diagnosticPublication
        val originalContext = parsed.file.sourceContext
        val sourceFile = CjInMemoryTextSourceFile("roles.cj", "roles.cj", source)
        val converter = LightTree2Cfir(session, session.cangjieScopeProvider, BodyBuildingMode.NORMAL)
        assertFailsWith<IllegalArgumentException> {
            converter.buildPreMacroCfirFile(parsed, sourceFile, ("\n" + source).toSourceLinesMapping())
        }
        // 无 raw 产品时没有新的 publication；错误映射不能消费或重写原事务，正确映射仍可重试。
        assertSame(originalPublication, parsed.file.diagnosticPublication)
        assertSame(originalContext, parsed.file.sourceContext)
        assertEquals(source, originalContext.snapshot.text)
        val roleMessage = CangJieParsingBundle.message("parsing.error.cjmp.specific.in.non.specific.file")
        assertTrue(originalPublication.diagnostics.none { it.message == roleMessage })
        val preFile = converter.buildPreMacroCfirFile(parsed, sourceFile, source.toSourceLinesMapping())
        val publication = preFile.parsingDiagnosticPublication
        assertSame(publication, preFile.cfirFile.parsingDiagnosticPublication)
        assertEquals(1, publication.diagnostics.count { it.message == roleMessage })
        assertEquals(1, publication.reports.count { it.diagnostic.message == roleMessage })
    }

    fun testSyntaxOnlyTreePreservesHistoricalPlatformRoleForSpecificSession() {
        val source = "platform func target() {}"
        val settings = LanguageVersionSettingsImpl(LanguageVersion.CANGJIE_1_1_0_ALPHA_65)
        val session = createTestSession(settings)
        session.register(CfirCjmpSettingsComponent::class, CfirCjmpSettingsComponent(explicitMode = CfirCjmpMode.SPECIFIC))
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        val parsed = CangJieLightParser.parseBuffered(builder)
        val sourceFile = CjInMemoryTextSourceFile("historical-role.cj", null, source)
        val file = LightTree2Cfir(session, session.cangjieScopeProvider, BodyBuildingMode.NORMAL)
            .buildPreMacroCfirFile(parsed, sourceFile, source.toSourceLinesMapping()).cfirFile
        val function = file.declarations.filterIsInstance<CfirNamedFunction>().single()
        assertTrue(function.status.isSpecific)
    }

    private fun build(
        text: String, version: LanguageVersion, common: Boolean, lightTree: Boolean, modeOverride: CfirCjmpMode? = null,
    ): CfirFile {
        val settings = LanguageVersionSettingsImpl(version)
        val session = createTestSession(settings)
        val mode = modeOverride ?: if (common) CfirCjmpMode.COMMON else CfirCjmpMode.SPECIFIC
        val cjmpSettings = CfirCjmpSettingsComponent(explicitMode = mode)
        session.register(CfirCjmpSettingsComponent::class, cjmpSettings)
        val context = cjmpSettings.toParsingContext(settings)
        val expectedRoleError = if (!text.contains("wrong")) null else CangJieParsingBundle.message(
            if (common) "parsing.error.cjmp.specific.in.non.specific.file"
            else "parsing.error.cjmp.common.in.non.common.file",
        )
        return if (lightTree) {
            val source = CjInMemoryTextSourceFile("roles.cj", null, text)
            val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
            val parserSettings = CjParserSettings(version)
            val parsed = CangJieLightParser.parseBuffered(
                builder, sourceKind = source.sourceKind, cjmpContext = context,
                sourceFile = source, settings = parserSettings,
            )
            if (expectedRoleError != null) assertTrue(parsed.diagnostics.any { it.message == expectedRoleError }, parsed.diagnostics.toString())
            val raw = LightTree2Cfir(session, session.cangjieScopeProvider, BodyBuildingMode.NORMAL, parserSettings)
                .buildPreMacroCfirFile(parsed, source, text.toSourceLinesMapping())
            assertEquals(context, parsed.file.cjmpContext)
            assertEquals(parserSettings, raw.parserSettings)
            assertSame(raw.parsingDiagnosticPublication, raw.cfirFile.parsingDiagnosticPublication)
            assertEquals(text, checkNotNull(raw.cfirFile.parsingSourceContext).snapshot.text)
            if (expectedRoleError != null) assertTrue(raw.parsingDiagnosticPublication.diagnostics.any { it.message == expectedRoleError })
            raw.cfirFile
        } else {
            val file = createCjFile("roles", text)
            file.cjmpParsingContext = context
            if (expectedRoleError != null) assertTrue(file.collectParsingDiagnostics().any { it.message == expectedRoleError })
            PsiRawCfirBuilder(session, BodyBuildingMode.NORMAL, CfirAnnotationMetadataBuilder())
                .buildPreMacroCfirFile(file).cfirFile
        }
    }
}
