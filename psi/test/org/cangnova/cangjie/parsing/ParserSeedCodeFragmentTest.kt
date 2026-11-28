package org.cangnova.cangjie.parsing

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.impl.BlockSupportImpl
import com.intellij.psi.tree.IElementType
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.psi.CjAnnotation
import org.cangnova.cangjie.psi.CjBinaryExpression
import org.cangnova.cangjie.psi.CjBlockCodeFragment
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjExpressionCodeFragment
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjLambdaExpression
import org.cangnova.cangjie.psi.CjMacroExpression
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.psi.CjTypeCodeFragment
import org.cangnova.cangjie.psi.CjUserType
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.awt.EventQueue
import java.lang.reflect.InvocationTargetException
import kotlin.test.Test
import kotlin.test.assertEquals as assertValueEquals
import kotlin.test.assertFalse as assertFalseValue
import kotlin.test.assertNotNull as assertPresent
import kotlin.test.assertNotSame as assertDistinct
import kotlin.test.assertNull as assertAbsent
import kotlin.test.assertSame as assertIdentical
import kotlin.test.assertTrue as assertTrueValue

/**
 * CodeFragment 使用真实构造入口捕获解析输入，不能在首次展开或 SDK diff 期间重新读取宿主配置。
 * 语义检查采用普通宏/自定义注解、std 专属 ConstSafe 及声明文件缺体规则；不人工构造 parser seed。
 */
class ParserSeedCodeFragmentTest : CjParsingTestCase(
    dataPath = "", fileExt = "cj", fileType = CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    @Test
    fun blockFragmentsCaptureTheirHostsAnnotationOrMacroModeAtConstruction() {
        for (annotationOnly in listOf(true, false)) {
            val host = createHost(annotationOnly)
            val fragment = block(TAGGED_FUNCTION, host)
            val captured = capturedBeforeParsing(fragment)
            assertIdentical(host, fragment.getOriginalContext())
            assertCustomPrefix(fragment.getContentElement(), annotationOnly)
            assertPublishedParserSeed(captured, fragment)
            assertNoErrors(fragment)
        }
    }

    @Test
    fun expressionFragmentsKeepTheCapturedModeInsideTheirLambda() {
        for (annotationOnly in listOf(true, false)) {
            val host = createHost(annotationOnly)
            val fragment = expression("{ =>\n$TAGGED_FUNCTION\n}", host)
            val captured = capturedBeforeParsing(fragment)
            val lambda = assertPresent(fragment.getContentElement()) as CjLambdaExpression
            assertCustomPrefix(assertPresent(lambda.bodyExpression), annotationOnly)
            assertPublishedParserSeed(captured, fragment)
            assertIdentical(host, fragment.getOriginalContext())
            assertNoErrors(fragment)
        }
    }

    @Test
    fun typeFragmentsAndClonesUseTheTypeRootWithOrWithoutAHost() {
        for (host in listOf(createHost(true), createHost(false), null)) {
            val fragment = type("Array<Int64>", host)
            val captured = capturedBeforeParsing(fragment)
            assertArrayType(fragment)
            val originalContent = assertPresent(fragment.getContentElement())
            val copy = fragment.copy() as CjTypeCodeFragment
            assertArrayType(copy)
            assertDistinct(fragment.node, copy.node)
            assertDistinct(originalContent, copy.getContentElement())
            assertPublishedParserSeed(captured, fragment)
            assertPublishedParserSeed(captured, copy)
            assertIdentical(host, copy.getOriginalContext())
        }
    }

    @Test
    fun fragmentsWithoutAHostHaveIndependentOrdinarySourceEntries() {
        val blockFragment = block(TAGGED_FUNCTION, null)
        val expressionFragment = expression("{ =>\n$TAGGED_FUNCTION\n}", null)
        for (fragment in listOf(blockFragment, expressionFragment)) {
            val captured = capturedBeforeParsing(fragment)
            assertValueEquals(CjSourceKind.SOURCE, captured.sourceKind)
            assertIdentical(CjmpParsingContext.SyntaxOnly, captured.cjmpContext)
            assertAbsent(fragment.getOriginalContext())
            assertCustomPrefix(assertPresent(fragment.getContentElement()), annotationOnly = false)
            assertNoErrors(fragment)
            val copy = fragment.copy() as CjCodeFragment
            assertDistinct(fragment.node, copy.node)
            assertPublishedParserSeed(captured, fragment)
            assertPublishedParserSeed(captured, copy)
            assertAbsent(copy.getOriginalContext())
            assertCustomPrefix(assertPresent(copy.getContentElement()), annotationOnly = false)
            assertNoErrors(copy)
        }
    }

    @Test
    fun blockFragmentDiffGenerationDoesNotChangeTheOriginalEntryOrPsi() {
        for (annotationOnly in listOf(true, false)) {
            val fragment = block(TAGGED_FUNCTION, createHost(annotationOnly))
            capturedBeforeParsing(fragment)
            assertCustomPrefix(fragment.getContentElement(), annotationOnly)
            val offset = TAGGED_FUNCTION.indexOf("[1]") + 1
            assertDiffDoesNotPublish(
                fragment, TextRange(offset, offset + 1), TAGGED_FUNCTION.replace("[1]", "[42]"),
                CjNodeTypes.BLOCK_CODE_FRAGMENT,
            )
            assertCustomPrefix(fragment.getContentElement(), annotationOnly)
        }
    }

    @Test
    fun expressionFragmentDiffGenerationDoesNotChangeTheOriginalEntryOrPsi() {
        for (host in listOf(createHost(true), createHost(false), null)) {
            val fragment = expression("1 + 2", host)
            capturedBeforeParsing(fragment)
            val binary = assertPresent(fragment.getContentElement()) as CjBinaryExpression
            assertValueEquals("1", binary.left?.text)
            assertValueEquals(CjTokens.PLUS, binary.operationToken)
            assertValueEquals("2", binary.right?.text)
            assertDiffDoesNotPublish(fragment, TextRange(0, 1), "42 + 2", CjNodeTypes.EXPRESSION_CODE_FRAGMENT)
            assertIdentical(binary, fragment.getContentElement())
            assertValueEquals("1", binary.left?.text)
        }
    }

    @Test
    fun typeFragmentDiffGenerationDoesNotChangeTheOriginalEntryOrPsi() {
        for (host in listOf(createHost(true), createHost(false), null)) {
            val fragment = type("Array<Int64>", host)
            capturedBeforeParsing(fragment)
            assertArrayType(fragment)
            assertDiffDoesNotPublish(fragment, TextRange(6, 11), "Array<UInt64>", CjNodeTypes.TYPE_CODE_FRAGMENT)
            assertArrayType(fragment)
        }
    }

    /**
     * 工厂明确提供 DECLARATION 语义来源；普通临时文件和 annotation-only 临时文件的物理种类不同。
     * 两者产生的 block/lambda 片段及 clone 都必须继续接受完整的无体函数签名。
     */
    @Test
    fun macroExpansionDeclarationOriginSurvivesFragmentsAndClones() {
        for (annotationOnly in listOf(true, false)) {
            val factory = CjPsiFactory.forMacroExpansion(
                project, markGenerated = false, sourceKind = CjSourceKind.DECLARATION,
            )
            val host = if (annotationOnly) factory.createAnnotationOnlyFile("func host(): Unit")
            else factory.createFile("func host(): Unit")
            val bodyless = "@Tag[1] func item(): Unit"
            val fragments = listOf<CjCodeFragment>(block(bodyless, host), expression("{ =>\n$bodyless\n}", host))
            assertValueEquals(if (annotationOnly) CjSourceKind.MACRO_CALL else CjSourceKind.SOURCE, host.sourceKind)
            for (fragment in fragments) {
                val captured = capturedBeforeParsing(fragment)
                assertValueEquals(CjSourceKind.DECLARATION, captured.sourceKind)
                assertCustomPrefix(assertPresent(fragment.getContentElement()), annotationOnly)
                assertBodylessFunction(fragment, "item", expectsMissingBody = false)
                val copy = fragment.copy() as CjCodeFragment
                assertCustomPrefix(assertPresent(copy.getContentElement()), annotationOnly)
                assertBodylessFunction(copy, "item", expectsMissingBody = false)
                assertPublishedParserSeed(captured, fragment)
                assertPublishedParserSeed(captured, copy)
            }
            assertNoErrors(host)
        }
    }

    /** 构造后尚未展开的片段，不能因为宿主重新编译成 SOURCE 而改变缺体诊断或编译输入。 */
    @Test
    fun hostConfigurationChangesDoNotAlterPreviouslyCreatedFragmentEntries() {
        val originalCompilation = CjmpParsingContext.Compilation(
            compileCommon = true, compileSpecific = true, enabled = true,
            mixedCommonSpecificDeclarations = true,
        )
        val host = CjPsiFactory.forMacroExpansion(
            project, markGenerated = false, cjmpContext = originalCompilation, sourceKind = CjSourceKind.DECLARATION,
        ).createFile("func host(): Unit")
        val blockFragment = block("func declaration(): Unit", host)
        val expressionFragment = expression("{ => func declaration(): Unit }", host)
        val typeFragment = type("Array<Int64>", host)
        val fragments = listOf<CjCodeFragment>(blockFragment, expressionFragment, typeFragment)
        val captures = fragments.map(::capturedBeforeParsing)
        for (capture in captures) {
            assertValueEquals(CjSourceKind.DECLARATION, capture.sourceKind)
            assertIdentical(originalCompilation, capture.cjmpContext)
        }

        val replacementCompilation = CjmpParsingContext.Compilation(false, false, false)
        host.parserSourceKind = CjSourceKind.SOURCE
        host.cjmpParsingContext = replacementCompilation
        reparseHost(host, "func changedHost(): Unit {}")
        assertValueEquals(CjSourceKind.SOURCE, assertPresent(host.getCopyableUserData(ROOT_PARSER_SEED_KEY)).sourceKind)
        for ((fragment, capture) in fragments.zip(captures)) {
            assertFalseValue(fragment.isContentsLoaded, "宿主重新解析不能展开已有片段")
            assertIdentical(capture, fragment.getCopyableUserData(ROOT_PARSER_SEED_KEY))
        }
        assertBodylessFunction(blockFragment, "declaration", expectsMissingBody = false)
        assertBodylessFunction(expressionFragment, "declaration", expectsMissingBody = false)
        assertArrayType(typeFragment)

        val newlyCreated = block("func declaration(): Unit", host)
        assertBodylessFunction(newlyCreated, "declaration", expectsMissingBody = true)
        val freshCapture = assertPresent(newlyCreated.getCopyableUserData(ROOT_PARSER_SEED_KEY))
        assertValueEquals(CjSourceKind.SOURCE, freshCapture.sourceKind)
        assertIdentical(replacementCompilation, freshCapture.cjmpContext)
        for ((fragment, capture) in fragments.zip(captures)) {
            assertPublishedParserSeed(capture, fragment)
            assertIdentical(originalCompilation, capture.cjmpContext)
        }
    }

    /** bare ConstSafe 的内置身份来自捕获时的 std 模块，文件名与后来的宿主文本均不能覆盖它。 */
    @Test
    fun aHostModuleChangeDoesNotAlterAnUnparsedFragmentOrItsClone() {
        val host = CjPsiFactory(project, markGenerated = false)
            .createFile("package std.seed_fragment\nfunc host() {}")
        val fragment = block(CONST_SAFE_VARIABLE, host)
        val captured = capturedBeforeParsing(fragment)
        assertValueEquals("std", captured.languageModuleName)

        reparseHost(host, "package application.seed_fragment\nfunc host() {}")
        assertValueEquals("application", CangJieParser.languageModuleNameForContext(host))
        assertFalseValue(fragment.isContentsLoaded)
        assertIdentical(captured, fragment.getCopyableUserData(ROOT_PARSER_SEED_KEY))
        // CodeFragment.clone 按 SDK/Kotlin 合同会展开自己的源根；此时也不得重新读取宿主模块。
        val copy = fragment.copy() as CjBlockCodeFragment
        assertConstSafeForm(fragment, isBuiltIn = true)
        assertConstSafeForm(copy, isBuiltIn = true)
        assertPublishedParserSeed(captured, fragment)
        assertPublishedParserSeed(captured, copy)
        assertDistinct(fragment.getContentElement(), copy.getContentElement())
        assertConstSafeForm(block(CONST_SAFE_VARIABLE, host), isBuiltIn = false)
    }

    /** 片段的语法来源必须沿实际片段入口继承，不能用 getOriginalContext 绕回已变化的宿主。 */
    @Test
    fun aChildFragmentUsesItsParentFragmentsCapturedModule() {
        val host = CjPsiFactory(project, markGenerated = false)
            .createFile("package std.seed_fragment\nfunc host() {}")
        val parent = block(CONST_SAFE_VARIABLE, host)
        capturedBeforeParsing(parent)
        assertConstSafeForm(parent, isBuiltIn = true)
        reparseHost(host, "package application.seed_fragment\nfunc host() {}")

        val child = block(CONST_SAFE_VARIABLE, parent.getContentElement())
        val childCapture = capturedBeforeParsing(child)
        assertValueEquals("std", childCapture.languageModuleName)
        assertIdentical(host, child.getOriginalContext())
        assertConstSafeForm(child, isBuiltIn = true)
        assertConstSafeForm(block(CONST_SAFE_VARIABLE, host), isBuiltIn = false)
    }

    @Test
    fun childFragmentsKeepTheirCommonParentsCompleteEntryAfterHostChanges() =
        assertCompleteParentEntryInheritance(CjmpFileRole.COMMON)

    @Test
    fun childFragmentsKeepTheirSpecificParentsCompleteEntryAfterHostChanges() =
        assertCompleteParentEntryInheritance(CjmpFileRole.SPECIFIC)

    /** 角色来自宿主的合法声明；父、子局部片段不添加 common/specific token 来补造角色。 */
    private fun assertCompleteParentEntryInheritance(role: CjmpFileRole) {
        val isCommon = role == CjmpFileRole.COMMON
        val compilation = CjmpParsingContext.Compilation(
            compileCommon = isCommon, compileSpecific = !isCommon, enabled = true,
        )
        val roleKeyword = if (isCommon) "common" else "specific"
        val host = CjPsiFactory.forMacroExpansion(
            project, markGenerated = false, cjmpContext = compilation, sourceKind = CjSourceKind.DECLARATION,
        ).createAnnotationOnlyFile("package std.seed_parent\n$roleKeyword func host(): Unit")
        assertNoErrors(host)
        assertValueEquals(ParsingFileFacts(CjSourceKind.DECLARATION, role), host.parsedFileFacts)
        val parent = block("func parentValue(): Unit", host)
        val parentEntry = capturedBeforeParsing(parent)
        assertValueEquals(role, parentEntry.fileRole)
        assertValueEquals(CjSourceKind.DECLARATION, parentEntry.sourceKind)
        assertValueEquals("std", parentEntry.languageModuleName)
        assertIdentical(compilation, parentEntry.cjmpContext)
        val parentContent = parent.getContentElement()
        val parentRoot = parent.node
        assertBodylessFunction(parent, "parentValue", expectsMissingBody = false)

        val childrenBeforeChange = inheritanceChildren(parentContent)
        val entriesBeforeChange = childrenBeforeChange.map(::capturedBeforeParsing)
        for (entry in entriesBeforeChange) {
            assertValueEquals(parentEntry, entry, "子片段必须继承父片段的完整入口，不能只补一个角色字段")
            assertIdentical(compilation, entry.cjmpContext)
        }

        val replacementCompilation = CjmpParsingContext.Compilation(false, false, false)
        host.parserSourceKind = CjSourceKind.SOURCE
        host.cjmpParsingContext = replacementCompilation
        reparseHost(host, "package application.seed_parent\nfunc changedHost(): Unit {}")
        assertValueEquals("application", CangJieParser.languageModuleNameForContext(host))
        assertValueEquals(ParsingFileFacts(CjSourceKind.SOURCE, CjmpFileRole.NONE), host.parsedFileFacts)
        val publishedParent = assertPublishedParserSeed(parentEntry, parent)
        assertIdentical(parentRoot, parent.node)
        assertIdentical(parentContent, parent.getContentElement())
        assertValueEquals("func parentValue(): Unit", parent.text)
        for ((child, entry) in childrenBeforeChange.zip(entriesBeforeChange)) {
            assertFalseValue(child.isContentsLoaded, "宿主更新不得展开已经创建的子片段")
            assertIdentical(entry, child.getCopyableUserData(ROOT_PARSER_SEED_KEY))
        }

        val childrenAfterChange = inheritanceChildren(parentContent)
        val entriesAfterChange = childrenAfterChange.map(::capturedBeforeParsing)
        for ((child, entry) in childrenBeforeChange.zip(entriesBeforeChange) + childrenAfterChange.zip(entriesAfterChange)) {
            assertValueEquals(parentEntry, entry)
            assertIdentical(compilation, entry.cjmpContext)
            assertIdentical(host, child.getOriginalContext())
            assertValueEquals("std", CangJieParser.languageModuleNameForContext(child))
            assertConstSafeForm(child, isBuiltIn = true)
            assertBodylessFunction(child, "item", expectsMissingBody = false)
            val function = PsiTreeUtil.findChildrenOfType(child, CjNamedFunction::class.java).single()
            val annotation = function.annotationEntries.single()
            assertValueEquals("@Tag[1]", annotation.text)
            assertValueEquals(listOf("1"), annotation.valueArguments.map { it.getArgumentExpression()?.text })
            assertPublishedParserSeed(entry, child)
        }
        assertIdentical(publishedParent, parent.getCopyableUserData(ROOT_PARSER_SEED_KEY))

        // 从已改变宿主直接创建的对照片段必须采用新 SOURCE 语义，不能继续享有父片段的缺体豁免。
        val directChild = block("func item(): Unit", host)
        val directEntry = capturedBeforeParsing(directChild)
        assertValueEquals(CjSourceKind.SOURCE, directEntry.sourceKind)
        assertValueEquals(CjmpFileRole.NONE, directEntry.fileRole)
        assertValueEquals("application", CangJieParser.languageModuleNameForContext(directChild))
        assertIdentical(replacementCompilation, directEntry.cjmpContext)
        assertBodylessFunction(directChild, "item", expectsMissingBody = true)
    }

    private fun inheritanceChildren(parentContent: PsiElement): List<CjCodeFragment> {
        val body = "$CONST_SAFE_VARIABLE\n@Tag[1] func item(): Unit"
        return listOf(block(body, parentContent), expression("{ =>\n$body\n}", parentContent))
    }

    /** SDK codefragment 的 full-parse 分支只安排替换；这里故意不提交 DiffLog。 */
    private fun assertDiffDoesNotPublish(
        fragment: CjCodeFragment,
        changedRange: TextRange,
        newText: String,
        contentType: IElementType,
    ) {
        // 候选内容仍接收构造入口，原文件 ROOT 已发布实际冻结输入；两者各自保持身份。
        val captured = fragment.fragmentParserSeed
        val published = assertPublishedParserSeed(captured, fragment)
        val oldRoot = fragment.node
        val oldText = fragment.text
        val oldContent = assertPresent(fragment.getContentElement())
        val oldContentText = oldContent.text
        val result = BlockSupportImpl.reparse(fragment, oldRoot, changedRange, newText, EmptyProgressIndicator(), oldText)
        assertIdentical(oldRoot, result.oldRoot, "没有可局部重解析的正文，必须经过 SDK codefragment full-parse 分支")
        assertDistinct(oldRoot, result.newRoot)
        val newContent = assertPresent(result.newRoot.firstChildNode)
        assertValueEquals(contentType, newContent.elementType)
        assertIdentical(captured, newContent.getCopyableUserData(PARSER_SEED_KEY))
        assertIdentical(published, fragment.getCopyableUserData(ROOT_PARSER_SEED_KEY), "生成候选 diff 不能重新发布原片段入口")
        assertIdentical(oldRoot, fragment.node)
        assertIdentical(oldContent, fragment.getContentElement())
        assertValueEquals(oldText, fragment.text)
        assertValueEquals(oldContentText, oldContent.text)
        assertNoErrors(fragment)
    }

    private fun capturedBeforeParsing(fragment: CjCodeFragment): CjParserSeed {
        assertFalseValue(fragment.isContentsLoaded)
        val captured = assertPresent(fragment.getCopyableUserData(ROOT_PARSER_SEED_KEY), "构造片段时就应捕获入口")
        assertFalseValue(fragment.isContentsLoaded, "查询不可变入口不能展开片段树")
        return captured
    }

    private fun assertCustomPrefix(root: PsiElement, annotationOnly: Boolean) {
        val function = PsiTreeUtil.findChildrenOfType(root, CjNamedFunction::class.java).single()
        assertValueEquals("item", function.name)
        val macros = PsiTreeUtil.findChildrenOfType(root, CjMacroExpression::class.java)
        val annotations = PsiTreeUtil.findChildrenOfType(root, CjAnnotation::class.java)
        if (annotationOnly) {
            assertTrueValue(macros.isEmpty())
            val annotation = function.annotationEntries.single()
            assertIdentical(annotation, annotations.single())
            assertValueEquals("@Tag[1]", annotation.text)
            assertValueEquals(listOf("1"), annotation.valueArguments.map { it.getArgumentExpression()?.text })
        } else {
            assertTrueValue(annotations.isEmpty())
            assertTrueValue(function.annotationEntries.isEmpty())
            val macro = macros.single()
            assertValueEquals("Tag", macro.referenceExpression?.text)
            assertValueEquals("[1]", macro.attr?.text)
            assertIdentical(function, macro.unwrappedDeclaration)
        }
    }

    private fun assertBodylessFunction(root: PsiElement, name: String, expectsMissingBody: Boolean) {
        val function = PsiTreeUtil.findChildrenOfType(root, CjNamedFunction::class.java).single()
        assertValueEquals(name, function.name)
        assertValueEquals("Unit", function.typeReference?.text)
        assertAbsent(function.bodyExpression)
        val errors = errors(root)
        if (expectsMissingBody) {
            // 固定 N parse_missing_body 与 SDK 1.1.3 probe 都保留真实函数名；入口继承检查不依赖旧泛化消息。
            val missingBody = "body of function '$name' is missing"
            assertTrueValue(errors.any { it.errorDescription == missingBody }, "应报告缺少函数体，实际为 ${errors.map { it.errorDescription }}")
        } else {
            assertTrueValue(errors.isEmpty(), errors.joinToString { it.errorDescription })
        }
    }

    private fun assertConstSafeForm(root: PsiElement, isBuiltIn: Boolean) {
        val variable = PsiTreeUtil.findChildrenOfType(root, CjPatternVariable::class.java).single()
        assertValueEquals("value", variable.pattern?.text)
        val macros = PsiTreeUtil.findChildrenOfType(root, CjMacroExpression::class.java)
        if (isBuiltIn) {
            val annotation = variable.annotationEntries.single()
            assertValueEquals("ConstSafe", annotation.shortName?.asString())
            assertTrueValue(annotation.isBuiltInAnnotation)
            assertTrueValue(macros.isEmpty())
        } else {
            assertTrueValue(variable.annotationEntries.isEmpty())
            val macro = macros.single()
            assertValueEquals("ConstSafe", macro.referenceExpression?.text)
            assertIdentical(variable, macro.unwrappedDeclaration)
        }
        assertNoErrors(root)
    }

    private fun assertArrayType(fragment: CjTypeCodeFragment) {
        val reference = assertPresent(fragment.getContentElement())
        assertValueEquals("Array<Int64>", reference.text)
        val type = assertPresent(reference.typeElement) as CjUserType
        assertValueEquals("Array", type.referencedName)
        assertValueEquals(listOf("Int64"), type.typeArguments.map { it.typeReference?.text })
        assertNoErrors(fragment)
    }

    private fun reparseHost(host: CjFile, newText: String) {
        val oldText = host.text
        val oldRoot = host.node
        val result = BlockSupportImpl.reparse(
            host, oldRoot, TextRange(0, oldText.length), newText, EmptyProgressIndicator(), oldText,
        )
        assertIdentical(oldRoot, result.oldRoot)
        applyPsiChangesOnEdt {
            result.log.doActualPsiChange(host)
        }
        assertValueEquals(newText, host.text)
        assertNoErrors(host)
    }

    private fun createHost(annotationOnly: Boolean): CjFile {
        val factory = CjPsiFactory(project, markGenerated = false)
        return if (annotationOnly) factory.createAnnotationOnlyFile("func host() {}") else factory.createFile("func host() {}")
    }

    private fun block(text: String, context: PsiElement?): CjBlockCodeFragment =
        CjPsiFactory(project, markGenerated = false).createBlockCodeFragment(text, context)

    private fun expression(text: String, context: PsiElement?): CjExpressionCodeFragment =
        CjPsiFactory(project, markGenerated = false).createExpressionCodeFragment(text, context)

    private fun type(text: String, context: PsiElement?): CjTypeCodeFragment = CjTypeCodeFragment(project, "fragment.cj", text, context)

    private fun errors(root: PsiElement): Collection<PsiErrorElement> = PsiTreeUtil.findChildrenOfType(root, PsiErrorElement::class.java)

    private fun assertNoErrors(root: PsiElement) {
        val errors = errors(root)
        assertTrueValue(errors.isEmpty(), "${errors.joinToString { it.errorDescription }}\n${root.text}")
    }

    /** PSI 到文档的同步要求真实 AWT EDT、命令范围与 write action，三者分别由对应平台 API 建立。 */
    private fun applyPsiChangesOnEdt(change: () -> Unit) {
        try {
            EventQueue.invokeAndWait {
                CommandProcessor.getInstance().executeCommand(
                    project,
                    { ApplicationManager.getApplication().runWriteAction { change() } },
                    "应用解析结果",
                    null,
                )
            }
        } catch (failure: InvocationTargetException) {
            throw failure.targetException
        }
    }

    companion object {
        private const val TAGGED_FUNCTION = "@Tag[1] func item() {}"
        private const val CONST_SAFE_VARIABLE = "@ConstSafe\nconst value: Int64 = 1"
    }
}
