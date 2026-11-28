package org.cangnova.cangjie.psi

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.AbstractFileViewProvider
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.testFramework.LightVirtualFile
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.parsing.CangJieParser
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.parsing.CjmpFileRole
import org.cangnova.cangjie.parsing.CjmpParsingContext
import org.cangnova.cangjie.parsing.ParsingFileFacts
import org.cangnova.cangjie.parsing.cjmpParsingContext
import org.cangnova.cangjie.parsing.collectParsingDiagnostics
import org.cangnova.cangjie.parsing.parsedFileFacts
import org.cangnova.cangjie.parsing.parserSourceKind
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

/** PSI 复制保留 parser 已发布的不可变事实，同时保留再次解析所需的独立输入。 */
class ParsingFileCopyTest : CjParsingTestCase(
    dataPath = "", fileExt = "cj", fileType = CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    @Test
    fun unparsedFileDoesNotInventPublishedFacts() {
        val file = createPsiFile("unparsed", "common func declaration(): Unit") as CjFile
        assertFalse((file as PsiFileImpl).isContentsLoaded)
        assertNull(file.parsedFileFacts)
        assertNull(file.parsedLanguageModuleName)
        assertNull(file.parserSourceKind)
        assertSame(CjmpParsingContext.SyntaxOnly, file.cjmpParsingContext)
        assertEquals("", file.parserLanguageModuleName)
        assertFalse(file.isContentsLoaded, "读取未发布值不能触发解析或制造 NONE 事实")

        file.parserSourceKind = CjSourceKind.DECLARATION
        file.parserLanguageModuleName = "std"
        file.cjmpParsingContext = CjmpParsingContext.Compilation(true, false, true)
        assertNull(file.parsedFileFacts)
        assertNull(file.parsedLanguageModuleName)
        assertFalse(file.isContentsLoaded, "输入发布不等于 parser 已完成")
    }

    @Test
    fun copiedFilesPreserveEveryRoleSourceKindAndModuleInputAndResult() {
        val context = CjmpParsingContext.Compilation(
            compileCommon = true, compileSpecific = true, enabled = true,
            mixedCommonSpecificDeclarations = true,
            commonMemberInSpecificOuter = true,
            parameterRoleMatching = true,
        )
        val modules = listOf(
            ModuleCase("", "", ""),
            ModuleCase("std", "", "std"),
            ModuleCase("std", "package org::std.copy_contract\n", "org"),
        )
        for (sourceKind in listOf(CjSourceKind.SOURCE, CjSourceKind.DECLARATION)) {
            for (role in CjmpFileRole.entries) {
                for ((moduleIndex, module) in modules.withIndex()) {
                    val ordinaryBody = if (sourceKind == CjSourceKind.DECLARATION) "" else " {}"
                    val declaration = when (role) {
                        CjmpFileRole.NONE -> "func ordinary(): Unit$ordinaryBody"
                        CjmpFileRole.COMMON -> "common func commonValue(): Unit"
                        CjmpFileRole.SPECIFIC -> "specific func specificValue(): Unit$ordinaryBody"
                        CjmpFileRole.BOTH -> "common func commonValue(): Unit\nspecific func specificValue(): Unit$ordinaryBody"
                    }
                    val source = module.header + declaration
                    val file = createPsiFile("copy${sourceKind}${role}$moduleIndex", source) as CjFile
                    file.cjmpParsingContext = context
                    file.parserSourceKind = sourceKind
                    file.parserLanguageModuleName = module.input
                    assertNull(file.parsedFileFacts)
                    assertNull(file.parsedLanguageModuleName)
                    assertEquals(emptyList<ParsingDiagnostic>(), file.collectParsingDiagnostics())
                    val facts = kotlin.test.assertNotNull(file.parsedFileFacts)
                    assertEquals(ParsingFileFacts(sourceKind, role), facts)
                    assertEquals(module.result, file.parsedLanguageModuleName)
                    assertEquals(CjSourceKind.SOURCE, file.sourceKind, "普通 .cj 的 parser 来源可显式为 DECLARATION")

                    val copied = file.copy() as CjFile
                    val copiedAgain = copied.copy() as CjFile
                    for (copy in listOf(copied, copiedAgain)) {
                        assertNotSame(file, copy)
                        assertSame(context, copy.cjmpParsingContext)
                        assertSame(facts, copy.parsedFileFacts, "已解析的 PSI clone 必须保留原不可变事实")
                        assertEquals(sourceKind, copy.parserSourceKind)
                        assertEquals(module.input, copy.parserLanguageModuleName)
                        assertEquals(module.result, copy.parsedLanguageModuleName)
                        assertEquals(module.result, CangJieParser.languageModuleNameForContext(copy))
                        assertEquals(emptyList<ParsingDiagnostic>(), copy.collectParsingDiagnostics())
                        assertSame(facts, copy.parsedFileFacts, "查询复制树不能通过重新解析补造事实")

                        reparse(copy, source)
                        assertEquals(ParsingFileFacts(sourceKind, role), copy.parsedFileFacts)
                        assertNotSame(facts, copy.parsedFileFacts, "真实 parser 再次运行须发布本次结果")
                        assertEquals(module.input, copy.parserLanguageModuleName)
                        assertEquals(module.result, copy.parsedLanguageModuleName)
                        assertSame(context, copy.cjmpParsingContext)
                    }
                    assertSame(facts, file.parsedFileFacts)
                    assertEquals(module.result, file.parsedLanguageModuleName)
                }
            }
        }
    }

    @Test
    fun changingCopiedInputsAndReparsingPublishesOnlyOnThatCopy() {
        val originalContext = CjmpParsingContext.Compilation(true, false, true)
        val file = createPsiFile("independentCopy", "common func original(): Unit") as CjFile
        file.cjmpParsingContext = originalContext
        file.parserSourceKind = CjSourceKind.DECLARATION
        file.parserLanguageModuleName = "std"
        assertEquals(emptyList<ParsingDiagnostic>(), file.collectParsingDiagnostics())
        val originalFacts = kotlin.test.assertNotNull(file.parsedFileFacts)
        assertEquals(ParsingFileFacts(CjSourceKind.DECLARATION, CjmpFileRole.COMMON), originalFacts)
        assertEquals("std", file.parsedLanguageModuleName)

        val copy = file.copy() as CjFile
        val sibling = file.copy() as CjFile
        val replacementContext = CjmpParsingContext.Compilation(
            compileCommon = false, compileSpecific = true, enabled = true,
            initialFileRole = CjmpFileRole.SPECIFIC,
        )
        copy.cjmpParsingContext = replacementContext
        copy.parserSourceKind = CjSourceKind.SOURCE
        copy.parserLanguageModuleName = ""
        assertSame(originalFacts, copy.parsedFileFacts, "修改输入不能提前改写已发布结果")
        assertEquals("std", copy.parsedLanguageModuleName)

        // 无角色 token 的新源码仍继承显式 initialFileRole；空模块 seed 不能复用旧 std 结果。
        reparse(copy, "func replacement(): Unit {}")
        assertEquals(ParsingFileFacts(CjSourceKind.SOURCE, CjmpFileRole.SPECIFIC), copy.parsedFileFacts)
        assertEquals("", copy.parsedLanguageModuleName)
        assertSame(replacementContext, copy.cjmpParsingContext)
        assertEquals(CjSourceKind.SOURCE, copy.parserSourceKind)

        // 可空 copyable 属性清除只作用于本副本，不改写原树或同源副本。
        copy.parsedFileFacts = null
        copy.parsedLanguageModuleName = null
        copy.parserSourceKind = null
        assertNull(copy.parsedFileFacts)
        assertNull(copy.parsedLanguageModuleName)
        assertNull(copy.parserSourceKind)
        for (unchanged in listOf(file, sibling)) {
            assertSame(originalFacts, unchanged.parsedFileFacts)
            assertSame(originalContext, unchanged.cjmpParsingContext)
            assertEquals(CjSourceKind.DECLARATION, unchanged.parserSourceKind)
            assertEquals("std", unchanged.parserLanguageModuleName)
            assertEquals("std", unchanged.parsedLanguageModuleName)
        }
    }

    /** SDK 重载真实副本内容后由正常文件入口重新解析；不手工安装 AST 或关联 PSI。 */
    private fun reparse(file: CjFile, source: String) {
        val provider = file.viewProvider as AbstractFileViewProvider
        val virtualFile = provider.virtualFile as LightVirtualFile
        val oldRoot = file.node
        val oldFacts = file.parsedFileFacts
        assertNull(FileDocumentManager.getInstance().getCachedDocument(virtualFile))
        ApplicationManager.getApplication().runWriteAction {
            virtualFile.setContent(this, source, false)
            provider.onContentReload()
        }
        assertSame(file, provider.getPsi(file.language))
        // 非物理文件的长度一致性检查会同步建根；验证真实重建结果，不要求过渡空树状态。
        assertEquals(source, provider.contents.toString())
        assertEquals(emptyList<ParsingDiagnostic>(), file.collectParsingDiagnostics())
        val newRoot = file.node
        assertNotSame(oldRoot, newRoot)
        assertSame(file, newRoot.psi)
        assertNotSame(oldFacts, file.parsedFileFacts)
        assertEquals(source, newRoot.text)
        assertEquals(source, file.text)
    }

    private data class ModuleCase(val input: String, val header: String, val result: String)
}
