package org.cangnova.cangjie.parsing

import com.intellij.lang.PsiBuilderFactory
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.messages.ParsingDiagnosticHint
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjClass
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjFinalizer
import org.cangnova.cangjie.psi.CjForeignDirective
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjSecondaryConstructor
import org.cangnova.cangjie.psi.psiUtil.parsedDeclarationModifiersOrNull
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals as assertValueEquals
import kotlin.test.assertNotNull as assertValuePresent
import kotlin.test.assertTrue as assertValueTrue

/** 文件 driver 的集合语法与实际声明属性必须分别发布，不能从外层形状补授。 */
class CjmpForeignCollectionScopeParsingTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun nestedForeignIsADeclarationRecoveryInput() {
        val source = "foreign { foreign { func nativeFun(): Unit } }"
        val file = psi(source, COMMON)
        assertValueEquals(1, PsiTreeUtil.findChildrenOfType(file, CjForeignDirective::class.java).size)
        val diagnostics = file.collectParsingDiagnostics()
        val duplicate = diagnostics.single { it.message == "duplicated modifier: 'foreign'" }
        assertValueEquals(10, duplicate.startOffset)
        assertValueEquals(17, duplicate.endOffset)
        assertValueEquals(listOf(ParsingDiagnosticHint(0, 7, "previous one is here")), duplicate.hints)
        // 原生主错误位于集合内 ParseDecl 已消费 foreign 后的 '{'，没有内层集合 owner。
        assertValueTrue(diagnostics.any { it.startOffset == source.indexOf('{', 10) })
    }

    @Test
    fun commonCollectionPublishesClassAndMemberAttributesSeparately() {
        val source = "common foreign { class C { common func member(): Unit {} } }"
        val file = psi(source, COMMON)
        val owner = assertValuePresent(PsiTreeUtil.findChildOfType(file, CjClass::class.java))
        val member = assertValuePresent(PsiTreeUtil.findChildOfType(owner, CjNamedFunction::class.java))
        assertValueEquals(setOf(CjModifierKind.COMMON, CjModifierKind.FOREIGN), assertValuePresent(owner.parsedDeclarationModifiersOrNull()).attributes)
        assertValueEquals(setOf(CjModifierKind.COMMON), assertValuePresent(member.parsedDeclarationModifiersOrNull()).attributes)
        val light = light(source, COMMON)
        assertValueEquals(setOf(CjModifierKind.COMMON, CjModifierKind.FOREIGN), light.declarationModifiers.getValue(source.indexOf("class")).attributes)
        assertValueEquals(setOf(CjModifierKind.COMMON), light.declarationModifiers.getValue(source.indexOf("func")).attributes)
    }

    @Test
    fun rejectedFunctionKeepsItsSourcePrefixAndPublishesEmptyAttributes() {
        val source = "specific foreign { public unsafe func wrong(): Unit }"
        val file = psi(source, COMMON)
        val function = assertValuePresent(PsiTreeUtil.findChildOfType(file, CjNamedFunction::class.java))
        val keyword = assertValuePresent(function.node.findChildByType(CjTokens.FUNC_KEYWORD))
        assertValueEquals(CjmpDeclarationRoleCheck.REJECTED, keyword.cjmpDeclarationRoleCheckOrNull())
        assertValueEquals(emptySet(), assertValuePresent(function.parsedDeclarationModifiersOrNull()).attributes)
        val expectedPrefix = setOf(CjModifierKind.SPECIFIC, CjModifierKind.FOREIGN, CjModifierKind.PUBLIC, CjModifierKind.UNSAFE)
        assertValueEquals(expectedPrefix, assertValuePresent(keyword.getCopyableUserData(DECLARATION_PREFIX_KEY)).modifiers)
        val light = light(source, COMMON)
        assertValueEquals(emptySet(), light.declarationModifiers.getValue(source.indexOf("func")).attributes)
        assertValueEquals(expectedPrefix, light.declarationPrefixes.getValue(source.indexOf("func")).modifiers)
    }

    @Test
    fun secondaryPredefinedCommonSurvivesRejectedModifierCheck() {
        val source = "class C { common init() {} }"
        val file = psi(source, SPECIFIC)
        val constructor = assertValuePresent(PsiTreeUtil.findChildOfType(file, CjSecondaryConstructor::class.java))
        assertValueEquals(setOf(CjModifierKind.COMMON), assertValuePresent(constructor.parsedDeclarationModifiersOrNull()).attributes)
        val light = light(source, SPECIFIC)
        assertValueEquals(CjmpDeclarationRoleCheck.REJECTED, light.declarationRoleChecks.getValue(source.indexOf("init")))
        assertValueEquals(setOf(CjModifierKind.COMMON), light.declarationModifiers.getValue(source.indexOf("init")).attributes)
    }

    @Test
    fun notCheckedDoesNotInferEffectiveAttributes() {
        val source = "class C { public ~init() {} }"
        val file = psi(source, COMMON)
        val finalizer = assertValuePresent(PsiTreeUtil.findChildOfType(file, CjFinalizer::class.java))
        assertValueEquals(emptySet(), assertValuePresent(finalizer.parsedDeclarationModifiersOrNull()).attributes)
        val light = light(source, COMMON)
        assertValueEquals(CjmpDeclarationRoleCheck.NOT_CHECKED, light.declarationRoleChecks.getValue(source.indexOf("init")))
        assertValueEquals(emptySet(), light.declarationModifiers.getValue(source.indexOf("init")).attributes)

        val text = "public func generated(): Unit {}"
        val entry = CjMacroFragmentContext.Nodes(MacroParsingScope.Declaration(CjScopeKind.UNKNOWN_SCOPE),
            CjMacroFragmentParent.OTHER, null, CjMacroDeclarationPrefix.EMPTY)
        val seed = createMacroFragmentParserSeed(ParsingFileFacts(CjSourceKind.SOURCE, CjmpFileRole.NONE), COMMON, entry)
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), text)
        val parsed = CangJieLightParser.parseMacroCodeFragment(builder, seed)
        assertValueEquals(CjmpDeclarationRoleCheck.NOT_CHECKED, parsed.declarationRoleChecks.getValue(text.indexOf("func")))
        assertValueEquals(setOf(CjModifierKind.PUBLIC), parsed.declarationModifiers.getValue(text.indexOf("func")).attributes)
    }

    private fun psi(source: String, context: CjmpParsingContext): CjFile {
        val file = createPsiFile("foreignScope.cj", source) as CjFile
        file.cjmpParsingContext = context
        file.node.firstChildNode
        assertValueEquals(context, assertValuePresent(file.parsedRoleSnapshot).parsingContext)
        return file
    }

    private fun light(source: String, context: CjmpParsingContext): ParsedCangJieLightFile {
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        return CangJieLightParser.parse(builder, cjmpContext = context,
            sourceFile = CjInMemoryTextSourceFile("foreignScope.cj", null, source))
    }

    companion object {
        private val COMMON = CjmpParsingContext.Compilation(true, false, true)
        private val SPECIFIC = CjmpParsingContext.Compilation(false, true, true)
    }
}
