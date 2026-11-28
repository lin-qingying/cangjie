package org.cangnova.cangjie.parsing

import com.intellij.lang.LighterASTNode
import com.intellij.lang.LighterASTTokenNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.CjmpRoleEventKind
import org.cangnova.cangjie.CjmpRoleModifiers
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.messages.ParsingDiagnosticHint
import org.cangnova.cangjie.psi.CjMacroCodeFragment
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals as assertValueEquals
import kotlin.test.assertNotNull as assertValuePresent
import kotlin.test.assertSame as assertValueSame
import kotlin.test.assertTrue as assertValueTrue

/**
 * 继承前缀与片段内前缀合并后的 scoped ParseNodes 回归。
 * 官方 v1.1.3 ParseNodes 只给首个声明传入 Modifier 集合，ParseDecl 在其上追加本地修饰符；
 * ParseDecl.cpp:1732–1750 对非法 extend 前缀选集合首项，TokenKind 中 PUBLIC 排在 COMMON 前。
 * ParserDiag.cpp:692–702 将主范围放在原 Modifier 位置，hint 放在当前 extend 关键字。
 * 原调用前缀由独立原始文件捕获；PSI/LT 分别与同一份完整诊断期望比较，不能虚构片段内 public 范围。
 */
class CjmpInheritedPrefixOriginParsingTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun inheritedPublicOnBareExtendReportsPublicWithoutAcceptingTheOuterRole() {
        val source = "extend Int64 {}"
        val prefix = capturePrefix(PUBLIC_ORIGIN)
        val parsed = parseBuffered(source, prefix)

        assertDiagnosticMessages(parsed, INVALID_PUBLIC_PREFIX)
        assertDiagnostics(parsed, invalidPublicPrefix(prefix, source))
        assertEvents(parsed, source.indexOf("extend") to CjmpRoleEventKind.Extend(NO_ROLE_MODIFIERS, 1))
        assertChecks(parsed, source.indexOf("extend") to CjmpDeclarationRoleCheck.NOT_CHECKED)
        assertValueEquals(CjmpDeclarationRoleCheck.NOT_CHECKED, parsed.fragment.parsingInfo.inheritedPrefixRoleCheck)
        assertValueEquals(CjmpFileRole.NONE, parsed.fragment.fileFacts.cjmpRole)
    }

    @Test
    fun inheritedPublicPrecedesLocalCommonWhenSelectingTheInvalidExtendModifier() {
        val source = "common extend Int64 {}"
        val prefix = capturePrefix(PUBLIC_ORIGIN)
        val parsed = parseBuffered(source, prefix)

        // 官方按 Modifier 身份排序，必须报原调用的 public，不能改报片段中的 common。
        assertDiagnosticMessages(parsed, INVALID_PUBLIC_PREFIX)
        assertDiagnostics(parsed, invalidPublicPrefix(prefix, source))
        assertEvents(parsed, source.indexOf("extend") to CjmpRoleEventKind.Extend(COMMON_MODIFIERS, 2))
        assertChecks(parsed, source.indexOf("extend") to CjmpDeclarationRoleCheck.NOT_CHECKED)
        assertValueEquals(CjmpDeclarationRoleCheck.NOT_CHECKED, parsed.fragment.parsingInfo.inheritedPrefixRoleCheck)
        assertValueEquals(CjmpFileRole.NONE, parsed.fragment.fileFacts.cjmpRole)
    }

    @Test
    fun inheritedCommonOnBareExtendIsAcceptedInCommonCompilation() {
        val source = "extend Int64 {}"
        val parsed = parseBuffered(source, capturePrefix("common func origin(): Unit {}"))

        assertDiagnosticMessages(parsed)
        assertDiagnostics(parsed)
        assertEvents(parsed, source.indexOf("extend") to CjmpRoleEventKind.Extend(COMMON_MODIFIERS, 1))
        assertChecks(parsed, source.indexOf("extend") to CjmpDeclarationRoleCheck.ACCEPTED)
        assertValueEquals(CjmpDeclarationRoleCheck.ACCEPTED, parsed.fragment.parsingInfo.inheritedPrefixRoleCheck)
        assertValueEquals(CjmpFileRole.COMMON, parsed.fragment.fileFacts.cjmpRole)
    }

    /** 官方 ParseDecl::ParseModifiers 的重复项始终以新读到的 local modifier 为主范围，保留原集合对象。 */
    @Test
    fun duplicateLocalPublicPointsToTheLocalTokenAndHintsAtTheInheritedPublic() {
        val source = "public extend Int64 {}"
        val prefix = capturePrefix(PUBLIC_ORIGIN)
        val parsed = parseBuffered(source, prefix)
        val inherited = prefix.sourceModifiers.single().sourceRange
        val localPublic = source.indexOf("public")

        assertDiagnosticMessages(parsed, DUPLICATE_PUBLIC, INVALID_PUBLIC_PREFIX)
        assertDiagnostics(parsed,
            ParsingDiagnostic(
                localPublic, localPublic + "public".length, DUPLICATE_PUBLIC,
                hints = listOf(ParsingDiagnosticHint(
                    inherited.startOffset, inherited.endOffset,
                    message("parsing.error.hint.previous.one.is.here"), inherited.source,
                )),
            ),
            invalidPublicPrefix(prefix, source),
        )
        assertEvents(parsed, source.indexOf("extend") to CjmpRoleEventKind.Extend(NO_ROLE_MODIFIERS, 1))
        assertChecks(parsed, source.indexOf("extend") to CjmpDeclarationRoleCheck.NOT_CHECKED)
        assertValueEquals(CjmpDeclarationRoleCheck.NOT_CHECKED, parsed.fragment.parsingInfo.inheritedPrefixRoleCheck)
        assertValueEquals(CjmpFileRole.NONE, parsed.fragment.fileFacts.cjmpRole)
    }

    /** 原生 Position 比行号：第二行的继承 SPECIFIC 晚于第一行的 local PRIVATE，主范围必须在原始来源。 */
    @Test
    fun conflictOrderingUsesOriginLineInsteadOfOffsetsFromDifferentRoots() {
        val prefix = capturePrefix("\nspecific func origin(): Unit {}")
        val source = "/*abcdefghijkl*/ private func origin(): Unit {}"
        val inherited = prefix.sourceModifiers.single().origin.range
        val local = capturePrefix(source, "line-local-origin.cj").sourceModifiers.single().origin.range
        assertValueEquals(2 to 1, inherited.source.nativePosition(inherited.startOffset))
        assertValueEquals(1 to 18, local.source.nativePosition(local.startOffset))
        assertValueEquals(17, local.startOffset)
        assertValueTrue(inherited.startOffset < local.startOffset)
        val parsed = parseBuffered(source, prefix, rootType = CjNodeTypes.FUNC,
            context = CjmpParsingContext.SyntaxOnly, expectedRootStart = 17)

        assertDiagnostics(parsed, ParsingDiagnostic(
            inherited.startOffset, inherited.endOffset,
            message("parsing.error.cjmp.conflicting.modifiers", "specific", "private"),
            hints = listOf(
                ParsingDiagnosticHint(local.startOffset, local.endOffset, CONFLICT_HINT),
                functionHint(source),
            ),
            source = inherited.source,
        ))
        val functionCheck = source.indexOf("func origin") to CjmpRoleEventKind.Check(SPECIFIC_MODIFIERS)
        assertEvents(parsed, functionCheck, expectedPsi = listOf(functionCheck,
            source.lastIndexOf('{') to CjmpRoleEventKind.Unknown))
        assertChecks(parsed, source.indexOf("func origin") to CjmpDeclarationRoleCheck.NOT_CHECKED)
        assertValueEquals(CjmpDeclarationRoleCheck.NOT_CHECKED, parsed.fragment.parsingInfo.inheritedPrefixRoleCheck)
        // SyntaxOnly 不做编译模式验收，但仍按真实 modifier 记录文件角色；NOT_CHECKED 不是角色缺失。
        assertValueEquals(CjmpFileRole.SPECIFIC, parsed.fragment.fileFacts.cjmpRole)
    }

    /** 官方 TOPLEVEL_FUNCDECL_MODIFIERS 是有向规则：PRIVATE 晚于 SPECIFIC 时不产生反向冲突。 */
    @Test
    fun inheritedPrivateAfterLocalSpecificDoesNotConflictAtOriginLines() {
        val prefix = capturePrefix("\nprivate func origin(): Unit {}")
        val source = "/*abcdefghijkl*/ specific func origin(): Unit {}"
        val inherited = prefix.sourceModifiers.single().origin.range
        val local = capturePrefix(source, "line-local-origin.cj").sourceModifiers.single().origin.range
        assertValueEquals(2 to 1, inherited.source.nativePosition(inherited.startOffset))
        assertValueEquals(1 to 18, local.source.nativePosition(local.startOffset))
        assertValueEquals(17, local.startOffset)
        assertValueTrue(inherited.startOffset < local.startOffset)
        val parsed = parseBuffered(source, prefix, rootType = CjNodeTypes.FUNC,
            context = CjmpParsingContext.SyntaxOnly, expectedRootStart = 17)

        assertDiagnostics(parsed)
        val functionCheck = source.indexOf("func origin") to CjmpRoleEventKind.Check(SPECIFIC_MODIFIERS)
        assertEvents(parsed, functionCheck, expectedPsi = listOf(functionCheck,
            source.lastIndexOf('{') to CjmpRoleEventKind.Unknown))
        assertChecks(parsed, source.indexOf("func origin") to CjmpDeclarationRoleCheck.NOT_CHECKED)
        assertValueEquals(CjmpDeclarationRoleCheck.NOT_CHECKED, parsed.fragment.parsingInfo.inheritedPrefixRoleCheck)
        assertValueEquals(CjmpFileRole.SPECIFIC, parsed.fragment.fileFacts.cjmpRole)
    }

    /** 中文注释令 local PRIVATE 的 UTF-16 offset 更小、原生字节列更靠后；SPECIFIC 在前时无冲突。 */
    @Test
    fun privateAfterSpecificDoesNotConflictAtOriginUtf8ByteColumns() {
        val prefix = capturePrefix("/*abcdefghij*/ specific func origin(): Unit {}")
        val source = "/*中中中中*/ private func origin(): Unit {}"
        val inherited = prefix.sourceModifiers.single().origin.range
        val local = capturePrefix(source, "column-local-origin.cj").sourceModifiers.single().origin.range
        assertValueEquals(1 to 16, inherited.source.nativePosition(inherited.startOffset))
        assertValueEquals(1 to 18, local.source.nativePosition(local.startOffset))
        assertValueEquals(9, local.startOffset)
        assertValueTrue(inherited.startOffset > local.startOffset)
        val parsed = parseBuffered(source, prefix, rootType = CjNodeTypes.FUNC,
            context = CjmpParsingContext.SyntaxOnly, expectedRootStart = 9)

        assertDiagnostics(parsed)
        val functionCheck = source.indexOf("func origin") to CjmpRoleEventKind.Check(SPECIFIC_MODIFIERS)
        assertEvents(parsed, functionCheck, expectedPsi = listOf(functionCheck,
            source.lastIndexOf('{') to CjmpRoleEventKind.Unknown))
        assertChecks(parsed, source.indexOf("func origin") to CjmpDeclarationRoleCheck.NOT_CHECKED)
        assertValueEquals(CjmpDeclarationRoleCheck.NOT_CHECKED, parsed.fragment.parsingInfo.inheritedPrefixRoleCheck)
        assertValueEquals(CjmpFileRole.SPECIFIC, parsed.fragment.fileFacts.cjmpRole)
    }

    /** 交换同一几何输入的身份：local SPECIFIC 在第 18 字节列，必须报告并提示第 16 列的继承 PRIVATE。 */
    @Test
    fun localSpecificAfterInheritedPrivateReportsConflictAtOriginUtf8ByteColumns() {
        val prefix = capturePrefix("/*abcdefghij*/ private func origin(): Unit {}")
        val source = "/*中中中中*/ specific func origin(): Unit {}"
        val inherited = prefix.sourceModifiers.single().origin.range
        val local = capturePrefix(source, "column-local-origin.cj").sourceModifiers.single().origin.range
        assertValueEquals(1 to 16, inherited.source.nativePosition(inherited.startOffset))
        assertValueEquals(1 to 18, local.source.nativePosition(local.startOffset))
        assertValueEquals(9, local.startOffset)
        assertValueTrue(inherited.startOffset > local.startOffset)
        val parsed = parseBuffered(source, prefix, rootType = CjNodeTypes.FUNC,
            context = CjmpParsingContext.SyntaxOnly, expectedRootStart = 9)

        assertDiagnostics(parsed, ParsingDiagnostic(
            local.startOffset, local.endOffset,
            message("parsing.error.cjmp.conflicting.modifiers", "specific", "private"),
            hints = listOf(
                ParsingDiagnosticHint(inherited.startOffset, inherited.endOffset, CONFLICT_HINT, inherited.source),
                functionHint(source),
            ),
        ))
        val functionCheck = source.indexOf("func origin") to CjmpRoleEventKind.Check(SPECIFIC_MODIFIERS)
        assertEvents(parsed, functionCheck, expectedPsi = listOf(functionCheck,
            source.lastIndexOf('{') to CjmpRoleEventKind.Unknown))
        assertChecks(parsed, source.indexOf("func origin") to CjmpDeclarationRoleCheck.NOT_CHECKED)
        assertValueEquals(CjmpDeclarationRoleCheck.NOT_CHECKED, parsed.fragment.parsingInfo.inheritedPrefixRoleCheck)
        assertValueEquals(CjmpFileRole.SPECIFIC, parsed.fragment.fileFacts.cjmpRole)
    }

    /** 前缀必须来自独立原始文件的真实解析；位置、快照身份及 journal 身份均由 parser 捕获。 */
    private fun capturePrefix(text: String, name: String = "inherited-prefix-origin.cj"): CjMacroDeclarationPrefix {
        val sourceFile = CjInMemoryTextSourceFile(name, "test-origins/$name", text)
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
        val parsed = CangJieLightParser.parseBuffered(builder, sourceFile = sourceFile)
        assertValueEquals(emptyList<ParsingDiagnostic>(), parsed.diagnostics)
        val prefix = parsed.file.declarationPrefixes.getValue(text.indexOf("func origin"))
        val modifier = prefix.sourceModifiers.single()
        val range = modifier.sourceRange
        assertValueEquals(parsed.file.roleEvents.single().id.rootIdentity, modifier.origin.rootIdentity)
        assertValueEquals(modifier.origin.range, range)
        assertValueSame(parsed.file.sourceContext.snapshot, range.source)
        assertValueEquals(parsed.file.sourceContext.snapshot.identity, range.source.identity)
        assertValueEquals(name, range.source.fileName)
        assertValueEquals(sourceFile.path, range.source.filePath)
        assertValueEquals(text, range.source.text)
        assertValueEquals(text.indexOf(modifier.kind.name.lowercase()), range.startOffset)
        assertValueEquals(range.startOffset + modifier.kind.name.length, range.endOffset)
        return prefix
    }

    /** 使用真实、非惰性的 scoped buffered 入口，同时构造实际 PSI 片段，核对完整输入及直接 root。 */
    private fun parseBuffered(
        source: String,
        prefix: CjMacroDeclarationPrefix,
        rootType: IElementType = CjNodeTypes.EXTEND,
        context: CjmpParsingContext = COMMON_MODE,
        expectedRootStart: Int = 0,
    ): Parsed {
        val entry = CjMacroFragmentContext.Nodes(
            MacroParsingScope.Declaration(CjScopeKind.TOPLEVEL), CjMacroFragmentParent.OTHER, null, prefix,
        )
        val seed = createMacroFragmentParserSeed(
            ParsingFileFacts(CjSourceKind.SOURCE, CjmpFileRole.NONE), context, entry,
        )
        val psi = CjMacroCodeFragment(project, "fragment.cj", source, context = null, seed = seed)
        val contents = psi.getContentElement()
        val actualSeed = assertPublishedParserSeed(seed, psi)
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        builder.setDebugMode(true)
        return CangJieLightParser.parseMacroCodeFragmentBuffered(builder, actualSeed).consume { fragment, diagnostics ->
            assertValueSame(actualSeed, fragment.inputSeed)
            assertValueSame(actualSeed.lexicalInput, fragment.lexicalInput)
            assertValueEquals(actualSeed.sourceContext, fragment.sourceContext)
            assertValueEquals(actualSeed, fragment.actualInputSeed)
            assertValueEquals(null, fragment.actualInputSeed.diagnosticScope)
            assertValueEquals(emptyList<Long>(), fragment.diagnosticScope.path)
            assertValueTrue(fragment.diagnosticScope.rootIdentity != checkNotNull(psi.parsingDiagnosticScopeOrNull()).rootIdentity)
            assertValueEquals(context, fragment.inputSeed.cjmpContext)
            assertValueEquals(source, fragment.inputText)
            assertValueEquals(CjSourceKind.SOURCE, fragment.fileFacts.sourceKind)
            assertValueEquals(CangJieLightDiagnosticOwnership.BUFFERED, fragment.diagnosticOwnership)
            assertValueEquals(0, fragment.tree.root.startOffset)
            assertValueEquals(source.length, fragment.tree.root.endOffset)
            assertValueEquals(listOf(rootType), fragment.roots.map { it.tokenType })
            val root = fragment.roots.single()
            // 用例独立指定直接声明 span；前置注释保留在完整输入中，不属于这些声明 root。
            assertValueEquals(expectedRootStart, root.startOffset)
            assertValueEquals(source.length, root.endOffset)
            val expectedRootText = source.substring(expectedRootStart)
            assertValueEquals(expectedRootText, lightText(root, fragment.tree))
            assertValueEquals(0, fragment.parsingInfo.inheritedPrefixRootIndex)
            assertValueEquals(CjmpFileRole.NONE, fragment.parsingInfo.entryFileRole)
            assertValueEquals(prefix, fragment.parsingInfo.inheritedPrefix)
            val inheritedModifier = prefix.sourceModifiers.single()
            val declarationKeyword = source.indexOf(if (rootType == CjNodeTypes.EXTEND) "extend" else "func origin")
            val mergedPrefix = fragment.declarationPrefixes.getValue(declarationKeyword)
            assertValueSame(inheritedModifier, mergedPrefix.sourceModifiers.single { it.kind == inheritedModifier.kind })
            assertValueSame(actualSeed, psi.getCopyableUserData(ROOT_PARSER_SEED_KEY))
            assertValueEquals(source, psi.text)
            assertValueEquals(0, psi.node.startOffset)
            assertValueEquals(source.length, psi.node.textRange.endOffset)
            assertValueEquals(listOf(rootType), contents.roots.map { it.node.elementType })
            assertValueTrue(contents.roots.all { it.parent === contents })
            assertValueEquals(expectedRootStart, contents.roots.single().textRange.startOffset)
            assertValueEquals(source.length, contents.roots.single().textRange.endOffset)
            assertValueEquals(expectedRootText, contents.roots.single().text)
            assertValueEquals(fragment.parsingInfo, contents.parsingInfo)
            assertValueEquals(fragment.fileFacts, contents.fileFacts)
            assertValueSame(inheritedModifier, checkNotNull(contents.parsingInfo.inheritedPrefix).sourceModifiers.single())
            Parsed(fragment, diagnostics, psi)
        }
    }

    /** 使用真实 LT token 文本核对已选直接 root；按 SDK 协议归还每次借用的子节点数组。 */
    private fun lightText(node: LighterASTNode, tree: FlyweightCapableTreeStructure<LighterASTNode>): String {
        if (node is LighterASTTokenNode) return node.text.toString()
        val ref = Ref<Array<LighterASTNode?>>()
        val count = tree.getChildren(node, ref)
        val children = ref.get()
        try {
            return buildString {
                for (index in 0 until count) append(lightText(assertValuePresent(children?.get(index)), tree))
            }
        } finally {
            if (children != null) tree.disposeChildren(children, count)
        }
    }

    /** 比较全部诊断的消息多重集，漏报、重复报错和错选 common 都不能被过滤掉。 */
    private fun assertDiagnosticMessages(parsed: Parsed, vararg expected: String) {
        for (actual in listOf(parsed.diagnostics, parsed.psi.collectParsingDiagnostics())) {
            assertValueEquals(expected.toList().groupingBy { it }.eachCount(), actual.groupingBy { it.message }.eachCount())
        }
    }

    /** 每条入口独立核对全部主范围、来源快照、归属、hint 顺序及重复次数，本地位置明确保留 null source。 */
    private fun assertDiagnostics(parsed: Parsed, vararg expected: ParsingDiagnostic) {
        val order = compareBy<ParsingDiagnostic>({ it.startOffset }, { it.endOffset }, { it.message })
        val context = checkNotNull(publishedParserSeed(parsed.psi).sourceContext)
        val psiPublication = parsed.psi.collectParsingDiagnosticPublication()
        val lightPublication = parsed.fragment.diagnosticPublication
        for (publication in listOf(lightPublication, psiPublication)) {
            val actual = publication.diagnostics
            assertLegacyParsingDiagnostics(expected.sortedWith(order), actual.sortedWith(order), context, actual.toString())
            assertCanonicalParsingDiagnostics(publication.diagnostics, context, publication.diagnostics.toString())
        }
        assertPublishedParsingDiagnosticParity(psiPublication.diagnostics.sortedWith(order),
            lightPublication.diagnostics.sortedWith(order), context)
    }

    private fun invalidPublicPrefix(prefix: CjMacroDeclarationPrefix, localText: String): ParsingDiagnostic {
        val range = prefix.sourceModifiers.single().sourceRange
        val keyword = localText.indexOf("extend")
        return ParsingDiagnostic(
            range.startOffset, range.endOffset, INVALID_PUBLIC_PREFIX,
            hints = listOf(ParsingDiagnosticHint(
                keyword, keyword + "extend".length, message("parsing.error.hint.before", "extend declaration"),
            )),
            source = range.source,
        )
    }

    private fun functionHint(source: String): ParsingDiagnosticHint {
        val keyword = source.indexOf("func origin")
        return ParsingDiagnosticHint(keyword, keyword + "func".length, message("parsing.error.hint.on", "function declaration"))
    }

    /** 完整候选序列必须保留继承修饰符数量，且不得额外执行外层 Check。 */
    private fun assertEvents(
        parsed: Parsed,
        vararg expected: Pair<Int, CjmpRoleEventKind>,
        expectedPsi: List<Pair<Int, CjmpRoleEventKind>> = expected.toList(),
    ) {
        assertValueEquals(expected.toList(), parsed.fragment.roleEvents.map { it.declarationOffset to it.kind })
        val result = checkNotNull(parsed.psi.getContentElement().node.getCopyableUserData(PARSED_FILE_RESULT_KEY))
        // PSI 正文没有展开，必须完整保留 brace 上的 Unknown；非惰性 LT 的同一正文已完整呈现。
        assertValueEquals(expectedPsi, result.roleEvents.map { it.declarationOffset to it.kind })
    }

    private fun assertChecks(parsed: Parsed, vararg expected: Pair<Int, CjmpDeclarationRoleCheck>) {
        assertValueEquals(expected.toMap(), parsed.fragment.declarationRoleChecks)
        val (keywordOffset, roleCheck) = expected.single()
        // 角色 key 挂在真正的声明关键字 leaf 上，而非声明容器；不搜索或展开函数体。
        val keyword = checkNotNull(parsed.psi.findElementAt(keywordOffset)).node
        assertValueSame(parsed.psi.getContentElement().roots.single().node, keyword.treeParent)
        assertValueEquals(keywordOffset, keyword.startOffset)
        assertValueTrue(keyword.elementType == CjTokens.FUNC_KEYWORD || keyword.elementType == CjTokens.EXTEND_KEYWORD)
        assertValueEquals(roleCheck, keyword.cjmpDeclarationRoleCheckOrNull())
    }

    private fun message(key: String, vararg arguments: Any): String = CangJieParsingBundle.message(key, *arguments)

    private data class Parsed(
        val fragment: ParsedCangJieMacroFragment,
        val diagnostics: List<ParsingDiagnostic>,
        val psi: CjMacroCodeFragment,
    )

    private companion object {
        val INVALID_PUBLIC_PREFIX = CangJieParsingBundle.message(
            "parsing.error.expected.no.modifier.before", "extend declaration", "public",
        )
        val DUPLICATE_PUBLIC = CangJieParsingBundle.message("parsing.error.duplicate.modifier", "public")
        const val CONFLICT_HINT = "because it is conflicted with this"
        const val PUBLIC_ORIGIN = "// 原始调用\npublic func origin(): Unit {}"
        val NO_ROLE_MODIFIERS = CjmpRoleModifiers(false, false, false)
        val COMMON_MODIFIERS = CjmpRoleModifiers(true, false, false)
        val SPECIFIC_MODIFIERS = CjmpRoleModifiers(false, true, false)
        val COMMON_MODE = CjmpParsingContext.Compilation(compileCommon = true, compileSpecific = false, enabled = true)
    }
}
