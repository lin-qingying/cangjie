package org.cangnova.cangjie.parsing

import com.intellij.lang.ASTNode
import com.intellij.lang.FileASTNode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.impl.BlockSupportImpl
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.psi.CjAnnotation
import org.cangnova.cangjie.psi.CjBlockExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjIfExpression
import org.cangnova.cangjie.psi.CjLambdaExpression
import org.cangnova.cangjie.psi.CjMacroExpression
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.psi.CjWhileExpression
import org.cangnova.cangjie.psi.parsedLanguageModuleName
import org.cangnova.cangjie.psi.parserLanguageModuleName
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
import kotlin.test.assertSame as assertIdentical
import kotlin.test.assertTrue as assertTrueValue

/**
 * 官方 v1.1.3 ParseAnnotations.cpp 的 enableCustomAnno 决定同一前缀是注解还是宏。
 * 独立 block/lambda parser、PSI clone 和 SDK 增量重解析都必须保留这个输入。
 * 语义由真实节点、参数、诊断和模块来源验证；入口对象身份另用于检查 diff 提交前的发布隔离。
 */
class ParserSeedLifecycleTest : CjParsingTestCase(
    dataPath = "", fileExt = "cj", fileType = CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach
    fun setUpFixture() = setUp()

    @AfterEach
    fun tearDownFixture() = tearDown()

    @Test
    fun annotationOnlyModeSurvivesNestedFunctionLambdaAndControlBodies() {
        val file = createFile(DEEP_BODIES, annotationOnly = true)
        val body = hostBody(file)
        assertFalseValue(body.isParsed, "必须从真实未展开的函数正文进入独立 parser")
        assertDeepBodies(file, annotationOnly = true)
        assertTrueValue(body.isParsed)
    }

    @Test
    fun ordinarySourceKeepsMacrosInTheSameNestedBodies() {
        val file = createFile(DEEP_BODIES, annotationOnly = false)
        val body = hostBody(file)
        assertFalseValue(body.isParsed)
        assertDeepBodies(file, annotationOnly = false)
        assertTrueValue(body.isParsed)
    }

    /** SDK 对非物理文件的 copy 会先加载源根；复制后的 collapsed 正文仍应彼此独立。 */
    @Test
    fun copyingUnparsedNonphysicalFilesKeepsIndependentBodyModes() {
        for (annotationOnly in listOf(true, false)) {
            val original = createFile(DEEP_BODIES, annotationOnly)
            assertFalseValue(original.isContentsLoaded)
            val copy = original.copy() as CjFile
            assertTrueValue(original.isContentsLoaded, "PsiFileImpl.clone 的 getTreeElement 会加载非物理源文件")
            assertTrueValue(copy.isContentsLoaded)
            val originalBody = hostBody(original)
            val copyBody = hostBody(copy)
            assertFalseValue(originalBody.isParsed)
            assertFalseValue(copyBody.isParsed)
            val secondCopy = copy.copy() as CjFile
            assertDistinct(original, copy)
            assertDistinct(copy, secondCopy)
            assertTrueValue(secondCopy.isContentsLoaded)
            assertFalseValue(hostBody(secondCopy).isParsed)

            assertDeepBodies(secondCopy, annotationOnly)
            assertFalseValue(originalBody.isParsed, "展开二次副本不得展开原文件的正文")
            assertFalseValue(copyBody.isParsed, "展开二次副本不得展开中间副本的正文")
            assertDeepBodies(copy, annotationOnly)
            assertFalseValue(originalBody.isParsed)
            assertDeepBodies(original, annotationOnly)
        }
    }

    /** SDK CompositeElement.clone 复制已有子树而不展开 collapsed 正文。 */
    @Test
    fun parsedFileCopiesKeepUnparsedBodyEntriesIndependent() {
        for (annotationOnly in listOf(true, false)) {
            val original = createFile(DEEP_BODIES, annotationOnly)
            val originalBody = hostBody(original)
            assertTrueValue(original.isContentsLoaded)
            assertFalseValue(originalBody.isParsed)

            val copy = original.copy() as CjFile
            val copyBody = hostBody(copy)
            assertDistinct(original.node, copy.node)
            assertDistinct(originalBody, copyBody)
            assertFalseValue(originalBody.isParsed)
            assertFalseValue(copyBody.isParsed)
            assertDeepBodies(copy, annotationOnly)
            assertFalseValue(originalBody.isParsed, "副本的 lazy 解析不得消费原节点的正文")
            assertDeepBodies(original, annotationOnly)
        }
    }

    @Test
    fun expandedFileCopiesKeepDistinctSyntaxNodesAndTheSameMode() {
        for (annotationOnly in listOf(true, false)) {
            val original = createFile(DEEP_BODIES, annotationOnly)
            assertDeepBodies(original, annotationOnly)
            val originalPrefixes = prefixNodes(original, annotationOnly)
            val copy = original.copy() as CjFile
            val copyBody = hostBody(copy)
            assertTrueValue(copyBody.isParsed, "已展开的树应直接 clone，不能靠重新解析补造结果")
            assertDeepBodies(copy, annotationOnly)
            val copiedPrefixes = prefixNodes(copy, annotationOnly)
            assertValueEquals(originalPrefixes.map { it.text }, copiedPrefixes.map { it.text })
            for ((oldPrefix, newPrefix) in originalPrefixes.zip(copiedPrefixes)) {
                assertDistinct(oldPrefix, newPrefix)
                assertDistinct(oldPrefix.node, newPrefix.node)
                assertIdentical(original, oldPrefix.containingFile)
                assertIdentical(copy, newPrefix.containingFile)
            }
            assertDeepBodies(original, annotationOnly)
        }
    }

    /** 同文本的两种解析入口交错展开，不能共享“当前模式”或复用另一根的事务。 */
    @Test
    fun interleavedLazyParsingDoesNotLeakModesBetweenFiles() {
        val annotationFile = createFile(DEEP_BODIES, annotationOnly = true)
        val sourceFile = createFile(DEEP_BODIES, annotationOnly = false)
        val annotationBody = hostBody(annotationFile)
        val sourceBody = hostBody(sourceFile)
        assertFalseValue(annotationBody.isParsed)
        assertFalseValue(sourceBody.isParsed)

        val sourceNested = sourceBody.statements.filterIsInstance<CjNamedFunction>().single()
        val annotationNested = annotationBody.statements.filterIsInstance<CjNamedFunction>().single()
        val annotationDeep = assertPresent(annotationNested.bodyBlockExpression)
            .statements.filterIsInstance<CjNamedFunction>().single()
        val sourceDeep = assertPresent(sourceNested.bodyBlockExpression)
            .statements.filterIsInstance<CjNamedFunction>().single()
        assertPrefixForms(assertPresent(sourceDeep.bodyBlockExpression), listOf(FUNCTION_PREFIX), annotationOnly = false)
        assertPrefixForms(assertPresent(annotationDeep.bodyBlockExpression), listOf(FUNCTION_PREFIX), annotationOnly = true)
        assertDeepBodies(sourceFile, annotationOnly = false)
        assertDeepBodies(annotationFile, annotationOnly = true)
    }

    @Test
    fun sdkBlockReparsePreservesAnnotationOnlyMode() = assertSdkBodyReparse(annotationOnly = true, lambda = false)

    @Test
    fun sdkBlockReparsePreservesOrdinaryMacroMode() = assertSdkBodyReparse(annotationOnly = false, lambda = false)

    @Test
    fun sdkLambdaReparsePreservesAnnotationOnlyMode() = assertSdkBodyReparse(annotationOnly = true, lambda = true)

    @Test
    fun sdkLambdaReparsePreservesOrdinaryMacroMode() = assertSdkBodyReparse(annotationOnly = false, lambda = true)

    @Test
    fun sdkFullFileReparsePreservesAnnotationOnlyMode() = assertSdkFileReparse(annotationOnly = true)

    @Test
    fun sdkFullFileReparsePreservesOrdinaryMacroMode() = assertSdkFileReparse(annotationOnly = false)

    @Test
    fun sdkFullFileReparseParsesNewNestedBodiesInAnnotationOnlyMode() =
        assertSdkFileReparseWithNewBody(annotationOnly = true)

    @Test
    fun sdkFullFileReparseParsesNewNestedBodiesInOrdinaryMacroMode() =
        assertSdkFileReparseWithNewBody(annotationOnly = false)

    @Test
    fun sdkFullFileReparseToEmptySourcePublishesTheEmptyPackageResult() {
        for (annotationOnly in listOf(true, false)) {
            val source = "package std.seed_lifecycle\n@Tag[1] func item() {}"
            val file = createFile(source, annotationOnly)
            assertPrefixForms(file, listOf(Prefix("Tag", "item", "1")), annotationOnly)
            val before = publicationSnapshot(file)
            assertValueEquals("std", before.moduleName)
            val expectedKind = if (annotationOnly) CjSourceKind.MACRO_CALL else CjSourceKind.SOURCE
            assertValueEquals(ParsingFileFacts(expectedKind, CjmpFileRole.NONE), before.facts)

            val result = BlockSupportImpl.reparse(
                file, before.root, TextRange(0, source.length), "", EmptyProgressIndicator(), source,
            )
            assertIdentical(before.root, result.oldRoot)
            assertPublicationUnchanged(file, before)
            applyPsiChangesOnEdt { result.log.doActualPsiChange(file) }

            assertValueEquals("", file.text)
            assertTrueValue(file.declarations.isEmpty())
            assertValueEquals(TextRange(0, 0), directPackageNode(file).textRange)
            assertValueEquals("", CangJieParser.languageModuleNameForContext(file))
            assertValueEquals("", file.parsedLanguageModuleName)
            assertValueEquals(ParsingFileFacts(expectedKind, CjmpFileRole.NONE), file.parsedFileFacts)
            assertTrueValue(PsiTreeUtil.findChildrenOfType(file, CjAnnotation::class.java).isEmpty())
            assertTrueValue(PsiTreeUtil.findChildrenOfType(file, CjMacroExpression::class.java).isEmpty())
            assertNoErrors(file)
        }
    }

    @Test
    fun sdkFullFileReparseFromEmptySourceKeepsTheOriginalAnnotationOrMacroMode() {
        for (annotationOnly in listOf(true, false)) {
            val file = createFile("", annotationOnly)
            val before = publicationSnapshot(file)
            assertValueEquals(TextRange(0, 0), before.packageNode.textRange)
            assertValueEquals("", before.moduleName)
            val source = "package std.seed_lifecycle\n@Tag[2] func populated() {}"
            val result = BlockSupportImpl.reparse(
                file, before.root, TextRange(0, 0), source, EmptyProgressIndicator(), "",
            )
            assertIdentical(before.root, result.oldRoot)
            assertPublicationUnchanged(file, before)
            applyPsiChangesOnEdt { result.log.doActualPsiChange(file) }

            assertValueEquals(source, file.text)
            assertTrueValue(directPackageNode(file).textLength > 0)
            assertValueEquals("std", CangJieParser.languageModuleNameForContext(file))
            assertValueEquals("std", file.parsedLanguageModuleName)
            assertValueEquals(before.facts, file.parsedFileFacts)
            assertPrefixForms(file, listOf(Prefix("Tag", "populated", "2")), annotationOnly)
            assertNoErrors(file)
        }
    }

    /** 新候选同时改变模块、语义来源和 CJMP 初始角色，不能在实际提交之前污染原文件结果。 */
    @Test
    fun sdkFullFileDiffGenerationLeavesAllPublishedHostFactsUntouched() {
        for (annotationOnly in listOf(true, false)) {
            val source = "package std.seed_lifecycle\n@Tag[1] func item() {}"
            val file = createFile(source, annotationOnly)
            assertPrefixForms(file, listOf(Prefix("Tag", "item", "1")), annotationOnly)
            val before = publicationSnapshot(file)
            file.parserSourceKind = CjSourceKind.DECLARATION
            file.cjmpParsingContext = CjmpParsingContext.Compilation(
                compileCommon = false, compileSpecific = true, enabled = true,
                initialFileRole = CjmpFileRole.SPECIFIC,
            )
            assertPublicationUnchanged(file, before)
            val replacement = "package application.seed_lifecycle\n@Tag[2] func changed(): Unit"
            val result = BlockSupportImpl.reparse(
                file, before.root, TextRange(0, source.length), replacement, EmptyProgressIndicator(), source,
            )
            assertIdentical(before.root, result.oldRoot)
            assertDistinct(result.oldRoot, result.newRoot)
            // 不应用这个候选；检查仍在读取原节点、原入口对象和原不可变结果。
            assertPublicationUnchanged(file, before)
            assertPrefixForms(file, listOf(Prefix("Tag", "item", "1")), annotationOnly)
            assertNoErrors(file)
        }
    }

    /** 空文本没有首 token；只有零宽 PACKAGE_DIRECTIVE 能承载此次不同的完整文件结果。 */
    @Test
    fun emptyFileReparseCommitsConfigurationChangesThroughItsZeroWidthPackageNode() {
        for (annotationOnly in listOf(true, false)) {
            val file = createFile("", annotationOnly)
            val before = publicationSnapshot(file)
            assertValueEquals(TextRange(0, 0), before.packageNode.textRange)
            val compilation = CjmpParsingContext.Compilation(
                compileCommon = false, compileSpecific = true, enabled = true,
                initialFileRole = CjmpFileRole.SPECIFIC,
            )
            file.parserSourceKind = CjSourceKind.DECLARATION
            file.parserLanguageModuleName = "std"
            file.cjmpParsingContext = compilation
            assertPublicationUnchanged(file, before)
            val result = BlockSupportImpl.reparse(
                file, before.root, TextRange(0, 0), "", EmptyProgressIndicator(), "",
            )
            assertIdentical(before.root, result.oldRoot)
            assertPublicationUnchanged(file, before)
            applyPsiChangesOnEdt { result.log.doActualPsiChange(file) }

            assertValueEquals("", file.text)
            assertTrueValue(file.declarations.isEmpty())
            assertValueEquals(TextRange(0, 0), directPackageNode(file).textRange)
            assertValueEquals("std", CangJieParser.languageModuleNameForContext(file))
            assertValueEquals("std", file.parsedLanguageModuleName)
            assertValueEquals(ParsingFileFacts(CjSourceKind.DECLARATION, CjmpFileRole.SPECIFIC), file.parsedFileFacts)
            val committedEntry = assertPresent(file.getCopyableUserData(ROOT_PARSER_SEED_KEY))
            assertDistinct(before.entry, committedEntry)
            assertValueEquals(CjSourceKind.DECLARATION, committedEntry.sourceKind)
            assertValueEquals("std", committedEntry.languageModuleName)
            assertIdentical(compilation, committedEntry.cjmpContext)
            assertNoErrors(file)
        }
    }

    /**
     * 使用 SDK 自行选取节点、创建新 chameleon、调用 isValidReparse 并生成 DiffLog 的完整路径。
     * 当前 fixture 的 commit processor 是 mock，因此这里不能用 commitDocument 代替真实 reparse。
     */
    private fun assertSdkBodyReparse(annotationOnly: Boolean, lambda: Boolean) {
        val input = "@Tag[1] func inside() {}"
        val bodyText = if (lambda) "let callback = { =>\n$input\n}" else input
        val oldText = "func host() {\n$bodyText\nfunc afterInside() {}\n}\nfunc following() {}"
        val file = createFile(oldText, annotationOnly)
        val host = topLevelFunctions(file).first()
        val body = assertPresent(host.bodyBlockExpression)
        val reparseRoot: ASTNode = if (lambda) {
            val variable = body.statements.filterIsInstance<CjPatternVariable>().single()
            (assertPresent(variable.initializer) as CjLambdaExpression).node
        } else body.node
        val fileRoot = file.node
        val following = followingFunction(file)
        val afterInside = body.statements.filterIsInstance<CjNamedFunction>().single { it.name == "afterInside" }
        assertPrefixForms(file, listOf(Prefix("Tag", "inside", "1")), annotationOnly)
        val oldPrefix = prefixNodes(file, annotationOnly).single()
        val oldPrefixText = oldPrefix.text
        val offset = oldText.indexOf("[1]") + 1
        val range = TextRange(offset, offset + 1)
        val newText = oldText.replaceRange(range.startOffset, range.endOffset, "42")

        val result = BlockSupportImpl.reparse(file, fileRoot, range, newText, EmptyProgressIndicator(), oldText)
        assertIdentical(reparseRoot, result.oldRoot, "必须命中原来的 block/lambda，不能退到整文件重建")
        assertValueEquals(if (lambda) CjNodeTypes.LAMBDA_EXPRESSION else CjNodeTypes.BLOCK, result.oldRoot.elementType)
        assertDistinct(result.oldRoot, result.newRoot, "SDK 必须为新文本创建新的 chameleon")
        assertValueEquals(oldText, file.text, "生成 diff 不能提前改写原 PSI")
        assertIdentical(oldPrefix, prefixNodes(file, annotationOnly).single())
        assertValueEquals(oldPrefixText, oldPrefix.text)
        applyPsiChangesOnEdt {
            result.log.doActualPsiChange(file)
        }

        assertIdentical(fileRoot, file.node)
        assertIdentical(host, topLevelFunctions(file).first())
        assertIdentical(following, followingFunction(file))
        assertIdentical(afterInside, body.statements.filterIsInstance<CjNamedFunction>().single { it.name == "afterInside" })
        assertValueEquals(newText, file.text)
        assertPrefixForms(file, listOf(Prefix("Tag", "inside", "42")), annotationOnly)
        assertNoErrors(file)
    }

    /** 声明头部修改强制经过 SDK 文件副本入口，正文之后仍要用同一种模式进行 lazy 解析。 */
    private fun assertSdkFileReparse(annotationOnly: Boolean) {
        val file = createFile(DEEP_BODIES, annotationOnly)
        assertDeepBodies(file, annotationOnly)
        val oldRoot = file.node
        val following = followingFunction(file)
        val offset = DEEP_BODIES.indexOf("host")
        val range = TextRange(offset, offset + "host".length)
        val newText = DEEP_BODIES.replaceRange(range.startOffset, range.endOffset, "renamedHost")
        val result = BlockSupportImpl.reparse(file, oldRoot, range, newText, EmptyProgressIndicator(), DEEP_BODIES)
        assertIdentical(oldRoot, result.oldRoot, "函数名不在正文中，必须经过完整文件 parser")
        assertDistinct(result.oldRoot, result.newRoot)
        assertValueEquals(DEEP_BODIES, file.text)
        applyPsiChangesOnEdt {
            result.log.doActualPsiChange(file)
        }

        assertValueEquals(newText, file.text)
        assertValueEquals(listOf("renamedHost", "following"), topLevelFunctions(file).map { it.name })
        assertIdentical(following, followingFunction(file), "完整 diff 仍应保留未修改的后续声明")
        assertPrefixForms(file, DEEP_PREFIXES, annotationOnly)
        assertNoErrors(file)
    }

    /**
     * 同时修改声明头和 lambda 正文，强制整文件解析并生成过去不存在的函数正文。
     * 新正文里的前缀不能靠复用旧 PSI 通过；原有 LambdaTag 参数也须得到本轮的新值。
     */
    private fun assertSdkFileReparseWithNewBody(annotationOnly: Boolean) {
        val file = createFile(DEEP_BODIES, annotationOnly)
        assertDeepBodies(file, annotationOnly)
        val oldRoot = file.node
        val following = followingFunction(file)
        val oldPrefixes = prefixNodes(file, annotationOnly)
        val oldPrefixTexts = oldPrefixes.map { it.text }
        val oldFunctions = PsiTreeUtil.findChildrenOfType(file, CjNamedFunction::class.java)
        val insertionAnchor = "@LambdaTag[2] func lambdaLeaf() {}"
        val range = TextRange(
            DEEP_BODIES.indexOf("host"),
            DEEP_BODIES.indexOf(insertionAnchor) + insertionAnchor.length,
        )
        val insertedBody = """

                        func addedContainer() {
                            @AddedTag[4] func addedLeaf() {}
                        }
        """.trimIndent()
        val replacement = DEEP_BODIES.substring(range.startOffset, range.endOffset)
            .replaceFirst("host", "renamedHost")
            .replace("@LambdaTag[2]", "@LambdaTag[42]") + "\n" + insertedBody
        val newText = DEEP_BODIES.replaceRange(range.startOffset, range.endOffset, replacement)
        val result = BlockSupportImpl.reparse(file, oldRoot, range, newText, EmptyProgressIndicator(), DEEP_BODIES)
        assertIdentical(oldRoot, result.oldRoot, "头部与正文的跨界修改必须经过完整文件 parser")
        assertDistinct(result.oldRoot, result.newRoot)
        assertValueEquals(DEEP_BODIES, file.text)
        assertValueEquals(oldPrefixTexts, oldPrefixes.map { it.text }, "生成 diff 不得提前改变旧前缀")
        applyPsiChangesOnEdt {
            result.log.doActualPsiChange(file)
        }

        assertValueEquals(newText, file.text)
        assertValueEquals(listOf("renamedHost", "following"), topLevelFunctions(file).map { it.name })
        assertIdentical(following, followingFunction(file))
        val hostBody = assertPresent(topLevelFunctions(file).first().bodyBlockExpression)
        val outerVariable = hostBody.statements.filterIsInstance<CjPatternVariable>().single()
        val outerLambda = assertPresent(outerVariable.initializer) as CjLambdaExpression
        val innerVariable = assertPresent(outerLambda.bodyExpression).statements.filterIsInstance<CjPatternVariable>().single()
        val innerLambda = assertPresent(innerVariable.initializer) as CjLambdaExpression
        val addedContainer = assertPresent(innerLambda.bodyExpression).statements
            .filterIsInstance<CjNamedFunction>().single { it.name == "addedContainer" }
        assertTrueValue(oldFunctions.none { it === addedContainer }, "新增的容器不能来自旧函数节点复用")
        assertIdentical(file, addedContainer.containingFile)
        val addedPrefix = Prefix("AddedTag", "addedLeaf", "4")
        assertPrefixForms(assertPresent(addedContainer.bodyBlockExpression), listOf(addedPrefix), annotationOnly)
        assertPrefixForms(
            file,
            listOf(FUNCTION_PREFIX, Prefix("LambdaTag", "lambdaLeaf", "42"), addedPrefix, DEEP_PREFIXES.last()),
            annotationOnly,
        )
        assertNoErrors(file)
    }

    private fun assertDeepBodies(file: CjFile, annotationOnly: Boolean) {
        assertValueEquals(DEEP_BODIES, file.text)
        assertValueEquals(listOf("host", "following"), topLevelFunctions(file).map { it.name })
        assertPrefixForms(file, DEEP_PREFIXES, annotationOnly)
        assertValueEquals(
            listOf("host", "nested", "deep", "functionLeaf", "lambdaLeaf", "controlLeaf", "following"),
            PsiTreeUtil.findChildrenOfType(file, CjNamedFunction::class.java).map { it.name },
        )
        assertValueEquals(2, PsiTreeUtil.findChildrenOfType(file, CjLambdaExpression::class.java).size)
        assertValueEquals(1, PsiTreeUtil.findChildrenOfType(file, CjIfExpression::class.java).size)
        assertValueEquals(1, PsiTreeUtil.findChildrenOfType(file, CjWhileExpression::class.java).size)
        assertNoErrors(file)
    }

    private fun assertPrefixForms(root: PsiElement, expected: List<Prefix>, annotationOnly: Boolean) {
        val functions = PsiTreeUtil.findChildrenOfType(root, CjNamedFunction::class.java)
        val annotations = PsiTreeUtil.findChildrenOfType(root, CjAnnotation::class.java)
        val macros = PsiTreeUtil.findChildrenOfType(root, CjMacroExpression::class.java)
        if (annotationOnly) {
            assertValueEquals(expected.map { it.annotationName }, annotations.map { it.shortName?.asString() })
            assertTrueValue(macros.isEmpty(), "annotation-only 的局部入口不能重新启用宏解析")
            for (prefix in expected) {
                val function = functions.single { it.name == prefix.functionName }
                val annotation = function.annotationEntries.single()
                assertValueEquals("@${prefix.annotationName}[${prefix.argument}]", annotation.text)
                assertValueEquals(listOf(prefix.argument), annotation.valueArguments.map { it.getArgumentExpression()?.text })
                assertFalseValue(annotation.isCompileTimeVisible)
            }
        } else {
            assertTrueValue(annotations.isEmpty(), "普通源码的裸自定义前缀必须保留宏身份")
            assertValueEquals(expected.map { it.annotationName }, macros.map { it.referenceExpression?.text })
            for (prefix in expected) {
                val function = functions.single { it.name == prefix.functionName }
                val macro = macros.single { it.referenceExpression?.text == prefix.annotationName }
                assertTrueValue(function.annotationEntries.isEmpty())
                assertIdentical(function, macro.unwrappedDeclaration)
                assertValueEquals("[${prefix.argument}]", macro.attr?.text)
            }
        }
        assertNoErrors(root)
    }

    private fun prefixNodes(root: PsiElement, annotationOnly: Boolean): List<PsiElement> =
        if (annotationOnly) PsiTreeUtil.findChildrenOfType(root, CjAnnotation::class.java).toList()
        else PsiTreeUtil.findChildrenOfType(root, CjMacroExpression::class.java).toList()

    private fun createFile(source: String, annotationOnly: Boolean): CjFile {
        val factory = CjPsiFactory(project, markGenerated = false)
        return if (annotationOnly) factory.createAnnotationOnlyFile(source) else factory.createFile(source)
    }

    private fun hostBody(file: CjFile): CjBlockExpression =
        assertPresent(topLevelFunctions(file).single { it.name == "host" }.bodyBlockExpression)

    private fun followingFunction(file: CjFile): CjNamedFunction =
        topLevelFunctions(file).single { it.name == "following" }

    private fun topLevelFunctions(file: CjFile): List<CjNamedFunction> = file.declarations.filterIsInstance<CjNamedFunction>()

    private fun assertNoErrors(root: PsiElement) {
        val errors = PsiTreeUtil.findChildrenOfType(root, PsiErrorElement::class.java)
        assertTrueValue(errors.isEmpty(), "${errors.joinToString { it.errorDescription }}\n${root.text}")
    }

    /** file.node 只取得 lazy 根；读取直接 package 子节点完成文件解析后，才能采集本次发布结果。 */
    private fun publicationSnapshot(file: CjFile): FilePublicationSnapshot {
        val root = file.node
        val packageNode = directPackageNode(file)
        return FilePublicationSnapshot(
            root = root,
            entry = assertPresent(file.getCopyableUserData(ROOT_PARSER_SEED_KEY)),
            facts = assertPresent(file.parsedFileFacts),
            moduleName = CangJieParser.languageModuleNameForContext(file),
            parsedModuleName = file.parsedLanguageModuleName,
            packageNode = packageNode,
            text = file.text,
        )
    }

    private fun assertPublicationUnchanged(file: CjFile, before: FilePublicationSnapshot) {
        assertIdentical(before.root, file.node)
        assertIdentical(before.entry, file.getCopyableUserData(ROOT_PARSER_SEED_KEY))
        assertIdentical(before.facts, file.parsedFileFacts)
        assertValueEquals(before.moduleName, CangJieParser.languageModuleNameForContext(file))
        assertValueEquals(before.parsedModuleName, file.parsedLanguageModuleName)
        assertIdentical(before.packageNode, directPackageNode(file))
        assertValueEquals(before.text, file.text)
    }

    /** 只检查根的直接 package 节点；空源码也必须保留唯一的零宽节点。 */
    private fun directPackageNode(file: CjFile): ASTNode {
        val packages = file.node.getChildren(TokenSet.create(CjNodeTypes.PACKAGE_DIRECTIVE))
        assertValueEquals(1, packages.size)
        return packages.single()
    }

    private data class FilePublicationSnapshot(
        val root: FileASTNode,
        val entry: CjParserSeed,
        val facts: ParsingFileFacts,
        val moduleName: String,
        val parsedModuleName: String?,
        val packageNode: ASTNode,
        val text: String,
    )

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

    private data class Prefix(val annotationName: String, val functionName: String, val argument: String)

    companion object {
        private val FUNCTION_PREFIX = Prefix("FunctionTag", "functionLeaf", "1")
        private val DEEP_PREFIXES = listOf(
            FUNCTION_PREFIX,
            Prefix("LambdaTag", "lambdaLeaf", "2"),
            Prefix("ControlTag", "controlLeaf", "3"),
        )
        private val DEEP_BODIES = """
            func host() {
                func nested() {
                    func deep() {
                        @FunctionTag[1] func functionLeaf() {}
                    }
                }
                let callback = { =>
                    let nestedCallback = { =>
                        @LambdaTag[2] func lambdaLeaf() {}
                    }
                }
                if (true) {
                    while (false) {
                        @ControlTag[3] func controlLeaf() {}
                    }
                }
            }
            func following() {}
        """.trimIndent()
    }
}
