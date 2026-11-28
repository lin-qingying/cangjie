package org.cangnova.cangjie.psi

import com.intellij.lang.PsiBuilderFactory
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.messages.CompilerMessageSeverity
import org.cangnova.cangjie.messages.CompilerMessageLocationWithRelatedLocations
import org.cangnova.cangjie.messages.CompilerMessageSourceLocation
import org.cangnova.cangjie.messages.MessageCollector
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.parsing.CjmpParsingContext
import org.cangnova.cangjie.parsing.CjLexicalSourceInput
import org.cangnova.cangjie.parsing.ROOT_PARSER_SEED_KEY
import org.cangnova.cangjie.parsing.cjmpParsingContext
import org.cangnova.cangjie.parsing.parsedFileFacts
import org.cangnova.cangjie.parsing.reportSyntaxErrors
import org.cangnova.cangjie.parsing.collectParsingDiagnostics
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.parsing.toCangJieParsingErrorListener
import org.cangnova.cangjie.source.readSourceFileWithMapping
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** CJMP 签名语法由共享 parser 负责，PSI 和 light-tree 必须报告相同错误。 */
class CjmpFunctionParsingTest : CjParsingTestCase(
    dataPath = "",
    fileExt = "cj",
    fileType = CangJieFileType.INSTANCE,
    CangJieParserDefinition(),
) {
    /** alpha.20 的文件角色门接受任一 CJMP 模式，并沿用宏宿主已冻结的混合角色。 */
    @Test
    fun testMixedFileRolesUseEitherCompileRoleInBothParsers() {
        val declarations = listOf(
            "common func c(): Unit {}\nspecific func s(): Unit {}",
            "specific func s(): Unit {}\ncommon func c(): Unit {}",
        )
        for (mixed in listOf(false, true)) for (compileCommon in listOf(false, true)) {
            for (compileSpecific in listOf(false, true)) for ((index, declaration) in declarations.withIndex()) {
                val text = "\n\n// role boundary\n$declaration"
                val context = CjmpParsingContext.Compilation(
                    compileCommon, compileSpecific, true, mixedCommonSpecificDeclarations = mixed,
                )
                val psi = createPsiFile("mixed-$mixed-$compileCommon-$compileSpecific-$index", text) as CjFile
                psi.cjmpParsingContext = context
                val psiErrors = psi.collectParsingDiagnostics()
                val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
                val seed = requireNotNull(psi.getCopyableUserData(ROOT_PARSER_SEED_KEY))
                val light = CangJieLightParser.parseBuffered(builder, cjmpContext = context,
                    lexicalInput = requireNotNull(seed.lexicalInput))
                val lightFacts = light.file.fileFacts
                assertEquals(psiErrors, light.diagnostics)
                assertEquals(psi.parsedFileFacts, lightFacts)
                if (mixed && (compileCommon || compileSpecific)) {
                    assertTrue(psiErrors.isEmpty())
                    assertEquals(org.cangnova.cangjie.parsing.CjmpFileRole.BOTH, lightFacts.cjmpRole)
                } else {
                    assertEquals(1, psiErrors.size)
                    val error = psiErrors.single()
                    assertEquals(ParsingDiagnostic.Origin.ORIGINAL_FILE, error.origin)
                    assertEquals(0, error.startOffset)
                    assertEquals(0, error.endOffset)
                    if (mixed) {
                        assertEquals(CangJieParsingBundle.message("parsing.error.cjmp.unexpected.declaration"), error.message)
                    }
                }
            }
        }
        val inherited = CjmpParsingContext.Compilation(
            true, false, true, org.cangnova.cangjie.parsing.CjmpFileRole.BOTH,
            mixedCommonSpecificDeclarations = true,
        )
        assertTrue(lightDiagnostics("common func c(): Unit {}", cjmpContext = inherited).isEmpty())
    }

    /** 类型化的文件错误必须仍是 IntelliJ ERROR_ELEMENT，普通错误不能被当作文件角色错误。 */
    @Test
    fun testFileRoleDiagnosticsKeepExplicitOriginInPsiAndLightTree() {
        val cases = listOf(
            "// host header\ncommon func f(): Unit {}" to CjmpParsingContext.Compilation(false, false, true),
            "// host header\nspecific func f(): Unit {}" to CjmpParsingContext.Compilation(false, false, true),
            "\n\ncommon func f(): Unit {}" to CjmpParsingContext.Compilation(false, false, true),
            "common func f(): Unit {}\nspecific func g(): Unit {}" to CjmpParsingContext.Compilation(true, true, true),
            "specific func f(): Unit {}\ncommon func g(): Unit {}" to CjmpParsingContext.Compilation(true, true, true),
        )
        for ((index, case) in cases.withIndex()) {
            val (text, context) = case
            val file = createPsiFile("fileRoleOrigin$index", text) as CjFile
            file.cjmpParsingContext = context
            val errors = file.collectParsingDiagnostics()
            assertEquals(1, errors.size, text)
            assertEquals(ParsingDiagnostic.Origin.ORIGINAL_FILE, errors.single().origin, text)
            assertEquals(0, errors.single().startOffset)
            assertEquals(0, errors.single().endOffset)
            val seed = requireNotNull(file.getCopyableUserData(ROOT_PARSER_SEED_KEY))
            assertEquals(errors, lightDiagnostics(text, cjmpContext = context,
                lexicalInput = requireNotNull(seed.lexicalInput)))
            assertEquals(com.intellij.psi.TokenType.ERROR_ELEMENT,
                PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java).single().node.elementType)
            val collector = RecordingCollector()
            assertTrue(collector.reportSyntaxErrors(file))
            val location = requireNotNull(collector.locations.single())
            assertEquals(1, location.line)
            assertEquals(1, location.column)
            assertEquals(1, location.lineEnd)
            assertEquals(2, location.columnEnd)
        }
        for (text in listOf("?", "common let _ = value")) {
            val file = createPsiFile("inputOrigin${text.length}", text) as CjFile
            val errors = file.collectParsingDiagnostics()
            assertTrue(errors.isNotEmpty(), text)
            assertTrue(errors.all { it.origin == ParsingDiagnostic.Origin.INPUT }, text)
            val seed = requireNotNull(file.getCopyableUserData(ROOT_PARSER_SEED_KEY))
            assertEquals(errors, lightDiagnostics(text, lexicalInput = requireNotNull(seed.lexicalInput)))
        }
    }

    /** common 禁止的模式种类由共享文法辨认，错误覆盖完整根模式，后续声明仍可解析。 */
    @Test
    fun testCommonPatternDeclarationsAreParserErrorsWithExactRootRanges() {
        for ((pattern, kind) in listOf("(a, b)" to "tuple", "Some(x)" to "enum", "pkg.Some(x)" to "enum", "_" to "wildcard")) {
            val text = "common let $pattern = value\nfunc following(): Unit {}"
            val file = createPsiFile("commonPattern", text) as CjFile
            val expected = CangJieParsingBundle.message("parsing.error.cjmp.pattern.declaration", kind, "common")
            val start = text.indexOf(pattern)
            val expectedErrors = listOf(Triple(start, start + pattern.length, expected))
            val actual = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
                .map { Triple(it.textRange.startOffset, it.textRange.endOffset, it.errorDescription) }
            assertEquals(expectedErrors, actual)
            assertEquals(expectedErrors, lightErrors(text))
            val variable = requireNotNull(PsiTreeUtil.findChildOfType(file, CjPatternVariable::class.java))
            assertNotNull(variable.pattern)
            assertEquals(pattern, variable.pattern?.text)
            assertEquals(listOf("following"), PsiTreeUtil.findChildrenOfType(file, CjNamedFunction::class.java).map { it.name })
        }
        for (text in listOf("common let x: Int64 = 1", "specific let (a, b) = (1, 2)",
            "func f(): Unit { let tokens = quote(@M common let (a, b) = (1, 2)) }")) {
            assertTrue(lightErrors(text).isEmpty(), text)
            val file = createPsiFile("validPatternRole", text) as CjFile
            assertTrue(PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java).isEmpty(), text)
        }
        val versionDisabled = CjmpParsingContext.Compilation(true, false, false)
        assertTrue(lightErrors("common let (a, b) = (1, 2)", cjmpContext = versionDisabled).isEmpty())
        val ordinaryCompilation = CjmpParsingContext.Compilation(false, false, true)
        val expectedRole = CangJieParsingBundle.message("parsing.error.cjmp.common.in.non.common.file")
        assertEquals(listOf(Triple(0, 0, expectedRole)), lightErrors("common let (a, b) = (1, 2)", cjmpContext = ordinaryCompilation))
    }

    /** 角色来自 parser，SyntaxOnly 也保留真实声明事实；宏入口可以显式继承宿主角色。 */
    @Test
    fun testParserPublishesFrozenFileFactsAndChecksInheritedRole() {
        val cases = listOf(
            "func f(): Unit {}" to org.cangnova.cangjie.parsing.CjmpFileRole.NONE,
            "func f(): Unit { let tokens = quote(@M common func g(): Unit {}) }" to org.cangnova.cangjie.parsing.CjmpFileRole.NONE,
            "@Outer(@Inner common class Quoted {})\nfunc f(): Unit {}" to org.cangnova.cangjie.parsing.CjmpFileRole.NONE,
            "func f(): Unit { let tokens = quote(@M common func g(): Unit {}) }\ncommon func real(): Unit" to org.cangnova.cangjie.parsing.CjmpFileRole.COMMON,
            "common func f(): Unit" to org.cangnova.cangjie.parsing.CjmpFileRole.COMMON,
            "specific func f(): Unit" to org.cangnova.cangjie.parsing.CjmpFileRole.SPECIFIC,
            "@M common func f(): Unit" to org.cangnova.cangjie.parsing.CjmpFileRole.COMMON,
            "@M specific func f(): Unit" to org.cangnova.cangjie.parsing.CjmpFileRole.SPECIFIC,
            "common func f(): Unit\nspecific func g(): Unit" to org.cangnova.cangjie.parsing.CjmpFileRole.BOTH,
        )
        for ((text, role) in cases) {
            val file = createPsiFile("fileFacts", text) as CjFile
            assertTrue(file.children.isNotEmpty())
            assertEquals(org.cangnova.cangjie.parsing.ParsingFileFacts(CjSourceKind.SOURCE, role), file.parsedFileFacts)
            val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
            val facts = CangJieLightParser.parse(builder).fileFacts
            assertEquals(file.parsedFileFacts, facts)
        }
        val context = CjmpParsingContext.Compilation(true, true, true, org.cangnova.cangjie.parsing.CjmpFileRole.COMMON)
        val text = "specific func generated(): Unit {}"
        val expected = CangJieParsingBundle.message("parsing.error.cjmp.common.and.specific.in.same.file")
        assertEquals(listOf(Triple(0, 0, expected)), lightErrors(text, cjmpContext = context))
        val file = createPsiFile("inheritedRole", text) as CjFile
        file.cjmpParsingContext = context
        assertTrue(file.children.isNotEmpty())
        assertEquals(org.cangnova.cangjie.parsing.CjmpFileRole.COMMON, file.parsedFileFacts?.cjmpRole)
        assertTrue(PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java).any { it.errorDescription == expected })
    }

    /** 声明宏包装层不能丢失 common 次构造器的修饰符，也不能改变允许无体的文法。 */
    @Test
    fun testMacroWrappedCommonSecondaryConstructorUsesSharedSyntaxMode() {
        val text = "common class C { @M common init() }"
        val file = createPsiFile("macroConstructor", text) as CjFile
        assertEquals(emptyList<String>(), PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java).map { it.errorDescription })
        assertEquals(emptyList<Triple<Int, Int, String?>>(), lightErrors(text))
        assertEquals(org.cangnova.cangjie.parsing.CjmpFileRole.COMMON, file.parsedFileFacts?.cjmpRole)
    }

    @Test
    fun testCommonModifierIsRejectedOnPrimaryConstructor() {
        val text = "common class C { common C() }"
        val expected = "modifier 'common' is not allowed on this declaration"
        val file = createPsiFile("primaryConstructorRole", text) as CjFile
        val psiErrors = file.collectParsingDiagnostics()
            .map { Triple(it.startOffset, it.endOffset, it.message) }
        val modifierStart = text.lastIndexOf("common")
        assertEquals(listOf(Triple(modifierStart, modifierStart + "common".length, expected)), psiErrors)
        assertEquals(listOf(Triple(modifierStart, modifierStart + "common".length, expected)), lightErrors(text))
    }

    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    @Test
    fun testBodylessCjmpFunctionRequiresReturnTypeAtFuncKeyword() {
        for (role in listOf("common", "specific")) {
            val text = "$role interface I { $role func missing() }"
            val file = createPsiFile("missingReturnType", text) as CjFile
            val expected = requiredReturnTypeMessage(role)
            val errors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
            assertEquals(listOf(expected), errors.map { it.errorDescription })
            assertEquals(text.indexOf("func"), errors.single().textOffset)
            assertEquals(0, errors.single().textLength)
            val function = requireNotNull(PsiTreeUtil.findChildOfType(file, CjNamedFunction::class.java))
            assertEquals("missing", function.name)
            assertNotNull(function.node.findChildByType(CjTokens.FUNC_KEYWORD))
            assertEquals(listOf(Triple(text.indexOf("func"), text.indexOf("func"), expected)), lightErrors(text))
        }
    }

    @Test
    fun testExplicitReturnTypesAndBodiesAreAccepted() {
        for (role in listOf("common", "specific")) {
            for (text in listOf(
                "$role interface I { $role func typed(): Unit }",
                "$role func inferred() { 1 }",
                "common func declared(): Unit",
                "common class C { common init() }",
                "interface I { func ordinary() }",
            )) {
                val file = createPsiFile("validCjmpSignature", text) as CjFile
                assertTrue(PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java).isEmpty(), text)
                assertTrue(lightErrors(text).isEmpty(), text)
            }
        }
    }

    @Test
    fun testMissingReturnTypeDoesNotConsumeFollowingDeclaration() {
        for (role in listOf("common", "specific")) {
            val text = "$role func missing()\nfunc next(): Int64 { 1 }"
            val file = createPsiFile("followingFunction", text) as CjFile
            assertEquals(listOf("missing", "next"),
                PsiTreeUtil.findChildrenOfType(file, CjNamedFunction::class.java).map { it.name })
            val psiErrors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
                .map { Triple(it.textRange.startOffset, it.textRange.endOffset, it.errorDescription) }
            assertEquals(psiErrors, lightErrors(text))
            assertEquals(1, psiErrors.count { it.third == requiredReturnTypeMessage(role) })
        }
    }

    @Test
    fun testDeclarationSourceStillRequiresCjmpReturnType() {
        for (role in listOf("common", "specific")) {
            val text = "$role func missing()"
            assertEquals(listOf(Triple(text.indexOf("func"), text.indexOf("func"), requiredReturnTypeMessage(role))),
                lightErrors(text, CjSourceKind.DECLARATION))
            assertTrue(lightErrors("$role func typed(): Unit", CjSourceKind.DECLARATION).isEmpty())
        }
    }

    private fun requiredReturnTypeMessage(role: String): String =
        CangJieParsingBundle.message("parsing.error.cjmp.function.return.type.required", role)

    /** 文件角色以编译选项为准；只报告源码顺序中的第一个错位，保留所有声明。 */
    @Test
    fun testFileRoleErrorsAreSharedByPsiAndLightTree() {
        val cases = listOf(
            Triple(CjmpParsingContext.Compilation(false, false, true), "common", "common.in.non.common.file"),
            Triple(CjmpParsingContext.Compilation(false, false, true), "specific", "specific.in.non.specific.file"),
            Triple(CjmpParsingContext.Compilation(true, false, true), "specific", "specific.in.non.specific.file"),
            Triple(CjmpParsingContext.Compilation(false, true, true), "common", "common.in.non.common.file"),
        )
        for ((options, role, key) in cases) {
            val otherRole = if (role == "common") "specific" else "common"
            val text = "\n\npackage demo\n$role func first(): Unit {}\n$otherRole func next(): Unit {}"
            val file = createPsiFile("fileRole", text) as CjFile
            file.cjmpParsingContext = options
            val expected = CangJieParsingBundle.message("parsing.error.cjmp.$key")
            val psiErrors = PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java)
                .map { Triple(it.textRange.startOffset, it.textRange.endOffset, it.errorDescription) }
            assertEquals(listOf(Triple(0, 0, expected)), psiErrors)
            assertEquals(psiErrors, lightErrors(text, cjmpContext = options))
            assertEquals(listOf("first", "next"), file.declarations.filterIsInstance<CjNamedFunction>().map { it.name })
            assertEquals("demo", file.packageFqName.asString())
        }
    }

    @Test
    fun testIndependentCompilationFlagsRejectMixedFileInBothOrders() {
        val options = CjmpParsingContext.Compilation(true, true, true)
        for (first in listOf("common", "specific")) {
            val second = if (first == "common") "specific" else "common"
            assertTrue(lightErrors("$first func f(): Unit {}", cjmpContext = options).isEmpty())
            val text = "$first func f(): Unit {}\n$second func g(): Unit {}"
            val file = createPsiFile("mixedRoles", text) as CjFile
            file.cjmpParsingContext = options
            val message = CangJieParsingBundle.message("parsing.error.cjmp.common.and.specific.in.same.file")
            assertEquals(listOf(message), PsiTreeUtil.findChildrenOfType(file, PsiErrorElement::class.java).map { it.errorDescription })
            assertEquals(listOf(Triple(0, 0, message)), lightErrors(text, cjmpContext = options))
        }
    }

    @Test
    fun testFileRoleCheckIncludesMembersAndRespectsVersionAndSyntaxOnlyContexts() {
        val text = "class C { specific func f(): Unit {} }"
        val message = CangJieParsingBundle.message("parsing.error.cjmp.specific.in.non.specific.file")
        assertEquals(listOf(Triple(0, 0, message)), lightErrors(text,
            cjmpContext = CjmpParsingContext.Compilation(true, false, true)))
        assertTrue(lightErrors(text, cjmpContext = CjmpParsingContext.Compilation(false, false, false)).isEmpty())
        assertTrue(lightErrors(text).isEmpty())
    }

    /** CLI 的两种入口必须保留解析器文案；CRLF 读取归一化后仍使用同一次读取的行映射。 */
    @Test
    fun testParsingMessagesReachCompilerCollector() {
        val text = "package demo\r\ncommon interface I { common func missing() }"
        val source = CjInMemoryTextSourceFile("input.cj", "C:/syntax/input.cj", text)
        val (code, mapping) = source.getContentsAsStream().reader(Charsets.UTF_8).use { it.readSourceFileWithMapping() }
        val collector = RecordingCollector()
        val builder = CangJieLightParser.createBuilder(code)
        CangJieLightParser.parse(builder, collector.toCangJieParsingErrorListener(), sourceFile = source)
        assertEquals(listOf(requiredReturnTypeMessage("common")), collector.messages)
        val location = requireNotNull(collector.locations.single())
        assertEquals("C:/syntax/input.cj", location.path)
        assertEquals(2, location.line)
        assertEquals(text.substringAfter("\r\n").indexOf("func") + 1, location.column)
        assertEquals(location.line, location.lineEnd)
        assertEquals(location.column, location.columnEnd)
        assertEquals(text.substringAfter("\r\n"), location.lineContent)
        assertTrue(collector.hasErrors())

        val psiCollector = RecordingCollector()
        val file = createPsiFile("collectorPsi", code.toString()) as CjFile
        assertTrue(psiCollector.reportSyntaxErrors(file))
        assertEquals(collector.messages, psiCollector.messages)
    }

    @Test
    fun testParserRelatedRangesReachCompilerCollector() {
        val text = "package demo\r\nspecific interface I { prop value: Int64 { @Frozen get() { 1 } } }"
        val source = CjInMemoryTextSourceFile("input.cj", "C:/syntax/input.cj", text)
        val (code, mapping) = source.getContentsAsStream().reader(Charsets.UTF_8).use { it.readSourceFileWithMapping() }
        val collector = RecordingCollector()
        val builder = CangJieLightParser.createBuilder(code)
        CangJieLightParser.parse(builder, collector.toCangJieParsingErrorListener(), sourceFile = source)

        assertEquals(listOf("unexpected annotation '@Frozen' on get"), collector.messages)
        val mainLocation = requireNotNull(collector.locations.single()) as CompilerMessageLocationWithRelatedLocations
        val relatedLocation = mainLocation.relatedLocations.single()
        assertEquals("on get", relatedLocation.message)
        val hintStart = code.indexOf("get") + "get".length
        val hintEnd = hintStart + "get".length
        val (hintLine, hintColumn) = mapping.getLineAndColumnByOffset(hintStart)
        val (hintLineEnd, hintColumnEnd) = mapping.getLineAndColumnByOffset(hintEnd)
        assertEquals(hintLine + 1, relatedLocation.location.line)
        assertEquals(hintColumn + 1, relatedLocation.location.column)
        assertEquals(hintLineEnd + 1, relatedLocation.location.lineEnd)
        assertEquals(hintColumnEnd + 1, relatedLocation.location.columnEnd)

        val psiCollector = RecordingCollector()
        val file = createPsiFile("collectorRelatedPsi", code.toString()) as CjFile
        assertTrue(psiCollector.reportSyntaxErrors(file))
        assertEquals(collector.messages, psiCollector.messages)
        val psiLocation = requireNotNull(psiCollector.locations.single()) as CompilerMessageLocationWithRelatedLocations
        val psiRelatedLocation = psiLocation.relatedLocations.single()
        assertEquals(psiLocation.path, psiRelatedLocation.location.path)
        assertEquals(relatedLocation.message, psiRelatedLocation.message)
        assertEquals(relatedLocation.location.line, psiRelatedLocation.location.line)
        assertEquals(relatedLocation.location.column, psiRelatedLocation.location.column)
        assertEquals(relatedLocation.location.lineEnd, psiRelatedLocation.location.lineEnd)
        assertEquals(relatedLocation.location.columnEnd, psiRelatedLocation.location.columnEnd)
    }

    private class RecordingCollector : MessageCollector {
        val messages = mutableListOf<String>()
        val locations = mutableListOf<CompilerMessageSourceLocation?>()
        override fun clear() { messages.clear(); locations.clear() }
        override fun hasErrors(): Boolean = messages.isNotEmpty()
        override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
            assertEquals(CompilerMessageSeverity.ERROR, severity)
            messages += message
            locations += location
        }
    }

    private fun lightErrors(
        text: String,
        sourceKind: CjSourceKind = CjSourceKind.SOURCE,
        cjmpContext: CjmpParsingContext = CjmpParsingContext.SyntaxOnly,
    ): List<Triple<Int, Int, String?>> = lightDiagnostics(text, sourceKind, cjmpContext).map {
        Triple(it.startOffset, it.endOffset, it.message)
    }

    private fun lightDiagnostics(
        text: String,
        sourceKind: CjSourceKind = CjSourceKind.SOURCE,
        cjmpContext: CjmpParsingContext = CjmpParsingContext.SyntaxOnly,
        lexicalInput: CjLexicalSourceInput? = null,
    ): List<ParsingDiagnostic> {
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
        return CangJieLightParser.parseBuffered(builder,
            sourceKind = sourceKind,
            cjmpContext = cjmpContext,
            lexicalInput = lexicalInput,
        ).diagnostics
    }
}
