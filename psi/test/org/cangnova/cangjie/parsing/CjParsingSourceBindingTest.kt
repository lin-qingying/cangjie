package org.cangnova.cangjie.parsing

import com.intellij.lang.ASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.BlockSupportImpl
import com.intellij.psi.impl.source.tree.LazyParseableElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.lang.CangJieFileType
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.messages.CangJieParsingBundle
import org.cangnova.cangjie.messages.CjCanonicalParsingDiagnosticHint
import org.cangnova.cangjie.messages.CjParsingDiagnosticRange
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.messages.ParsingDiagnosticHint
import org.cangnova.cangjie.psi.CjAnnotation
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjMacroExpression
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.test.testFramework.CjParsingTestCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import java.awt.EventQueue
import kotlin.test.Test
import kotlin.test.assertEquals as assertValueEquals
import kotlin.test.assertFailsWith as assertFailure
import kotlin.test.assertFalse as assertFalseValue
import kotlin.test.assertNotEquals as assertDifferentValues
import kotlin.test.assertNotNull as assertPresent
import kotlin.test.assertNotSame as assertDistinct
import kotlin.test.assertSame as assertIdentical
import kotlin.test.assertTrue as assertTrueValue

/**
 * 官方 v1.1.3 ParseMacro.cpp:579–617 的新宏仅保存前导 annotations，modifiers 只传给 input decl。
 * Parser.cpp:316–345 的 foreign 克隆保留原注解位置；MacroCall.cpp:19–58 的 getter 读取宏自身字段。
 * 通过真实 PSI 来源、SDK 复制和局部 DiffLog 验证当前范围；首次 origin 不随绑定或复制改写。
 */
class CjParsingSourceBindingTest : CjParsingTestCase(
    "", "cj", CangJieFileType.INSTANCE, CangJieParserDefinition(),
) {
    @BeforeEach fun setUpFixture() = setUp()
    @AfterEach fun tearDownFixture() = tearDown()

    @Test
    fun leadingBuiltinAnnotationBelongsToTheMacroGetter() {
        val source = "@C @M func item(): Unit {}"
        val file = createPsiFile("leadingMacroAnnotation", source) as CjFile
        val macro = directMacros(file).single()
        val function = inputFunction(macro)
        val annotation = macro.annotationEntries.single()
        val prefix = macroPrefix(macro)
        val body = assertPresent(function.bodyBlockExpression).node as LazyParseableElement

        assertPrefix(prefix, listOf("C"), listOf(BuiltInAnnotationKind.C))
        assertCapturedAnnotation(prefix.annotations.single(), annotation, file)
        assertPrefix(inputPrefix(function), emptyList(), emptyList())
        assertTrueValue(function.annotationEntries.isEmpty())
        assertFalseValue(body.isParsed, "来源绑定前不预展开宏输入声明体")
        val bound = CjParsingSourceBindingScope(file).bind(prefix, macro)
        assertBoundAnnotation(prefix.annotations.single(), bound.annotations.single(), annotation, file)
        assertFalseValue(body.isParsed, "来源绑定不能预展开 opaque/lazy 声明体")

        val light = parseLight(source)
        assertValueEquals(emptyList(), light.diagnostics)
        assertValueEquals(setOf(source.indexOf("@M")), light.file.macroFacts.keys)
        assertLightPrefix(light, source.indexOf("@M"), listOf("C"), listOf(BuiltInAnnotationKind.C))
        assertPrefix(assertPresent(light.file.declarationPrefixes[source.indexOf("func")]), emptyList(), emptyList())
    }

    /** 宏名字之后的 @C 归输入声明，不得借 input annotation 或 modifier 冒充宏 getter 来源。 */
    @Test
    fun inputBuiltinAnnotationDoesNotBecomeAMacroGetterPrefix() {
        val source = "@M @C func item(): Unit {}"
        val file = createPsiFile("inputDeclarationAnnotation", source) as CjFile
        val macro = directMacros(file).single()
        val function = inputFunction(macro)
        val annotation = function.annotationEntries.single()
        val prefix = inputPrefix(function)

        assertPrefix(macroPrefix(macro), emptyList(), emptyList())
        assertTrueValue(macro.annotationEntries.isEmpty())
        assertPrefix(prefix, listOf("C"), listOf(BuiltInAnnotationKind.C))
        assertCapturedAnnotation(prefix.annotations.single(), annotation, file)
        val bound = CjParsingSourceBindingScope(file).bind(prefix, function)
        assertBoundAnnotation(prefix.annotations.single(), bound.annotations.single(), annotation, file)

        val light = parseLight(source)
        assertValueEquals(emptyList(), light.diagnostics)
        assertLightPrefix(light, source.indexOf("@M"), emptyList(), emptyList())
        val input = assertPresent(light.file.declarationPrefixes[source.indexOf("func")])
        assertPrefix(input, listOf("C"), listOf(BuiltInAnnotationKind.C))
        assertInitialLightAnnotation(input.annotations.single(), light, "@C")
    }

    /** 两层宏的前导 annotation 向量独立；外层 C 不向内层 Deprecated 展平。 */
    @Test
    fun nestedMacrosKeepSeparateAnnotationGetterOrigins() {
        val source = "@C @Outer @Deprecated @Inner func item(): Unit {}"
        val file = createPsiFile("nestedMacroAnnotationOwners", source) as CjFile
        val outer = directMacros(file).single()
        val inner = assertPresent(outer.input).children.filterIsInstance<CjMacroExpression>().single()
        val outerPrefix = macroPrefix(outer)
        val innerPrefix = macroPrefix(inner)
        val scope = CjParsingSourceBindingScope(file)

        assertPrefix(outerPrefix, listOf("C"), listOf(BuiltInAnnotationKind.C))
        assertPrefix(innerPrefix, listOf("Deprecated"), listOf(BuiltInAnnotationKind.DEPRECATED))
        assertCapturedAnnotation(outerPrefix.annotations.single(), outer.annotationEntries.single(), file)
        assertCapturedAnnotation(innerPrefix.annotations.single(), inner.annotationEntries.single(), file)
        assertDistinct(outerPrefix.annotations.single().origin, innerPrefix.annotations.single().origin)
        assertBoundAnnotation(outerPrefix.annotations.single(), scope.bind(outerPrefix, outer).annotations.single(),
            outer.annotationEntries.single(), file)
        assertBoundAnnotation(innerPrefix.annotations.single(), scope.bind(innerPrefix, inner).annotations.single(),
            inner.annotationEntries.single(), file)
        assertPrefix(inputPrefix(inputFunction(inner)), emptyList(), emptyList())

        val light = parseLight(source)
        assertValueEquals(emptyList(), light.diagnostics)
        assertValueEquals(setOf(source.indexOf("@Outer"), source.indexOf("@Inner")), light.file.macroFacts.keys)
        assertLightPrefix(light, source.indexOf("@Outer"), listOf("C"), listOf(BuiltInAnnotationKind.C))
        assertLightPrefix(light, source.indexOf("@Inner"), listOf("Deprecated"), listOf(BuiltInAnnotationKind.DEPRECATED))
    }

    /** foreign 前缀的 C 保留原节点来源；原 FOREIGN 只传入 input 并产生 before-macro 诊断。 */
    @Test
    fun foreignMacroKeepsClonedAnnotationOriginAndTheOriginalModifierDiagnostic() {
        val source = "@C foreign { @M func nativeValue(): Unit }"
        val file = createPsiFile("foreignMacroAnnotationOrigin", source) as CjFile
        val macro = PsiTreeUtil.findChildrenOfType(file, CjMacroExpression::class.java).single()
        val annotation = PsiTreeUtil.findChildrenOfType(file, CjAnnotation::class.java).single()
        val prefix = macroPrefix(macro)
        val function = inputFunction(macro)
        val input = inputPrefix(function)
        val foreign = assertPresent(file.findElementAt(source.indexOf("foreign")))
        val scope = CjParsingSourceBindingScope(file)

        assertPrefix(prefix, listOf("C"), listOf(BuiltInAnnotationKind.C))
        assertCapturedAnnotation(prefix.annotations.single(), annotation, file)
        assertBoundAnnotation(prefix.annotations.single(), scope.bind(prefix, macro).annotations.single(), annotation, file)
        assertPrefix(input, emptyList(), emptyList(), listOf(CjModifierKind.FOREIGN))
        assertIdentical(assertPresent(foreign.node.getCopyableUserData(MODIFIER_SOURCE_FACT_KEY)).origin,
            input.sourceModifiers.single().origin)
        assertBoundModifier(input.sourceModifiers.single(), scope.bind(input, function).sourceModifiers.single(), foreign, file)

        val modifierStart = source.indexOf("foreign")
        val macroStart = source.indexOf("@M")
        val lexicalInput = assertPresent(assertPresent(file.getCopyableUserData(ROOT_PARSER_SEED_KEY)).lexicalInput)
        val snapshot = lexicalInput.source
        val hintLabel = CangJieParsingBundle.message("parsing.error.hint.before", "macro call")
        val expected = listOf(ParsingDiagnostic(
            modifierStart, modifierStart + "foreign".length,
            CangJieParsingBundle.message("parsing.error.expected.no.modifier.before", "macro call", "foreign"),
            hints = listOf(ParsingDiagnosticHint(macroStart, macroStart + "@M".length,
                hintLabel)),
            mainRange = CjParsingDiagnosticRange(snapshot, snapshot.nativePosition(modifierStart),
                snapshot.nativePosition(modifierStart + "foreign".length)),
            nativeHints = listOf(CjCanonicalParsingDiagnosticHint(
                CjParsingDiagnosticRange(snapshot, snapshot.nativePosition(macroStart),
                    snapshot.nativePosition(macroStart + "@M".length)), hintLabel)),
        ))
        assertValueEquals(expected, file.collectParsingDiagnostics())
        val light = parseLight(source, lexicalInput)
        assertValueEquals(expected, light.diagnostics, "完整比较原 modifier 主范围和 @M hint，不过滤诊断")
        assertLightPrefix(light, macroStart, listOf("C"), listOf(BuiltInAnnotationKind.C))
        val lightInput = assertPresent(light.file.declarationPrefixes[source.indexOf("func")])
        assertPrefix(lightInput, emptyList(), emptyList(), listOf(CjModifierKind.FOREIGN))
        assertValueEquals(TextRange(modifierStart, modifierStart + "foreign".length), range(lightInput.sourceModifiers.single().origin.range))
    }

    /** SDK 只重解析前方函数体；后方原 annotation/modifier 节点移动后绑定当前全文。 */
    @Test
    fun earlierBlockDiffMovesUnreparsedSourceNodesWithoutChangingTheirOrigins() {
        val source = "$beforeFunction\n$copyableMacro\n"
        val file = createPsiFile("movedUnreparsedSource", source) as CjFile
        val macro = directMacros(file).single()
        val function = inputFunction(macro)
        val annotation = macro.annotationEntries.single()
        val modifier = publicModifier(function)
        val annotationNode = annotation.node
        val modifierNode = modifier.node
        val prefix = macroPrefix(macro)
        val input = inputPrefix(function)
        val oldRange = annotation.textRange
        assertCopyableOwnerBindings(CjParsingSourceBindingScope(file), file, macro, prefix, input)

        val updated = applyEarlierBlockDiff(file)
        assertIdentical(annotationNode, directMacros(file).single().annotationEntries.single().node)
        assertIdentical(modifierNode, publicModifier(inputFunction(directMacros(file).single())).node)
        assertTrueValue(annotation.textRange.startOffset > oldRange.startOffset)
        val bound = assertCopyableOwnerBindings(CjParsingSourceBindingScope(file), file, macro, prefix, input)
        assertValueEquals(updated, bound.macro.annotations.single().sourceRange.source.text)
        assertValueEquals(source, prefix.annotations.single().origin.range.source.text)
        assertDifferentValues(prefix.annotations.single().origin.range.source.identity,
            bound.macro.annotations.single().sourceRange.source.identity)
        assertValueEquals(emptyList(), parseLight(updated).diagnostics)
    }

    @Test
    fun copiedPsiFileBindsToItsOwnPhysicalSourcesAndPreservesTheOriginalOrigins() {
        val source = "$beforeFunction\n$copyableMacro\n"
        val file = createPsiFile("copiedFileSourceBinding", source) as CjFile
        val macro = directMacros(file).single()
        val prefix = macroPrefix(macro)
        val input = inputPrefix(inputFunction(macro))
        val copy = file.copy() as CjFile
        val copiedMacro = directMacros(copy).single()

        assertDistinct(file.node, copy.node)
        assertDistinct(macro.node, copiedMacro.node)
        assertIdentical(prefix, macroPrefix(copiedMacro))
        assertIdentical(input, inputPrefix(inputFunction(copiedMacro)))
        assertCopyableOwnerBindings(CjParsingSourceBindingScope(copy), copy, copiedMacro, prefix, input)
        assertCopyableOwnerBindings(CjParsingSourceBindingScope(file), file, macro, prefix, input)
        assertValueEquals(source, file.text)
        assertValueEquals(source, copy.text)
    }

    /** 同一 origin 对应两个真实实例；必须由 sourceOwner 分支区分，不能覆盖或选表中首项。 */
    @Test
    fun sameFileMacroCloneBindsEachOwnerToItsOwnAnnotationAndModifierInstances() {
        val copies = createSameFileMacroCopies("sameFileOwnerBranches")
        val prefix = macroPrefix(copies.original)
        val input = inputPrefix(inputFunction(copies.original))
        val scope = CjParsingSourceBindingScope(copies.file)

        assertIdentical(prefix, macroPrefix(copies.inserted))
        assertIdentical(input, inputPrefix(inputFunction(copies.inserted)))
        val inserted = assertCopyableOwnerBindings(scope, copies.file, copies.inserted, prefix, input)
        val original = assertCopyableOwnerBindings(scope, copies.file, copies.original, prefix, input)
        assertIdentical(original.macro.annotations.single().origin, inserted.macro.annotations.single().origin)
        assertIdentical(original.input.sourceModifiers.single().origin, inserted.input.sourceModifiers.single().origin)
        assertDifferentValues(range(original.macro.annotations.single().sourceRange), range(inserted.macro.annotations.single().sourceRange))
        assertDifferentValues(range(original.input.sourceModifiers.single().sourceRange), range(inserted.input.sourceModifiers.single().sourceRange))
        assertFailure<IllegalStateException>("整个文件无法唯一证明共享 origin 的实例") { scope.bind(prefix, copies.file) }
        assertValueEquals(emptyList(), parseLight(copies.file.text).diagnostics)
    }

    /** 复制后再由前方局部 diff 移动两个实例，仍各自绑定当前物理 AST，不改首次 parser origin。 */
    @Test
    fun sameFileClonedSourcesRemainOwnerBoundAfterAnEarlierBlockDiff() {
        val copies = createSameFileMacroCopies("clonedSourcesThenMoved")
        val prefix = macroPrefix(copies.original)
        val input = inputPrefix(inputFunction(copies.original))
        val originalAnnotation = copies.original.annotationEntries.single().node
        val insertedAnnotation = copies.inserted.annotationEntries.single().node
        val originalModifier = publicModifier(inputFunction(copies.original)).node
        val insertedModifier = publicModifier(inputFunction(copies.inserted)).node
        val oldText = copies.file.text
        val initialScope = CjParsingSourceBindingScope(copies.file)
        assertCopyableOwnerBindings(initialScope, copies.file, copies.original, prefix, input)
        assertCopyableOwnerBindings(initialScope, copies.file, copies.inserted, prefix, input)

        val updated = applyEarlierBlockDiff(copies.file)
        val macros = directMacros(copies.file)
        assertValueEquals(2, macros.size)
        assertIdentical(originalAnnotation, macros[0].annotationEntries.single().node)
        assertIdentical(insertedAnnotation, macros[1].annotationEntries.single().node)
        assertIdentical(originalModifier, publicModifier(inputFunction(macros[0])).node)
        assertIdentical(insertedModifier, publicModifier(inputFunction(macros[1])).node)
        val scope = CjParsingSourceBindingScope(copies.file)
        val original = assertCopyableOwnerBindings(scope, copies.file, macros[0], prefix, input)
        val inserted = assertCopyableOwnerBindings(scope, copies.file, macros[1], prefix, input)
        assertValueEquals(updated, original.macro.annotations.single().sourceRange.source.text)
        assertIdentical(original.macro.annotations.single().sourceRange.source, inserted.macro.annotations.single().sourceRange.source)
        assertDifferentValues(oldText, updated)
        assertValueEquals(emptyList(), parseLight(updated).diagnostics)
    }

    /** 跨文件插入标准 PSI copy；来源 origin 仍属原文件，当前 sourceRange 则必须属目标文件。 */
    @Test
    fun aMacroSubtreeCopiedIntoAnotherFileBindsToTheDestinationOwner() {
        val originalText = "$copyableMacro\n"
        val originalFile = createPsiFile("originalMacroSource", originalText) as CjFile
        val original = directMacros(originalFile).single()
        val prefix = macroPrefix(original)
        val input = inputPrefix(inputFunction(original))
        val destination = createPsiFile("destinationMacroSource", "$beforeFunction\n") as CjFile
        val anchor = destination.children.filterIsInstance<CjNamedFunction>().single()
        val inserted = insertMacroCopy(destination, original, anchor)

        // 41028 的既定 SDK 插入操作保留两个分隔换行，副本末尾没有终端换行。
        assertValueEquals("$beforeFunction\n\n$copyableMacro", destination.text)
        assertIdentical(prefix, macroPrefix(inserted))
        assertIdentical(input, inputPrefix(inputFunction(inserted)))
        val bound = assertCopyableOwnerBindings(CjParsingSourceBindingScope(destination), destination, inserted, prefix, input)
        assertValueEquals(originalFile.name, bound.macro.annotations.single().origin.range.source.fileName)
        assertValueEquals(destination.name, bound.macro.annotations.single().sourceRange.source.fileName)
        assertValueEquals(originalText, originalFile.text)
        assertCopyableOwnerBindings(CjParsingSourceBindingScope(originalFile), originalFile, original, prefix, input)
        assertValueEquals(emptyList(), parseLight(destination.text).diagnostics)
    }

    private val beforeFunction = "func before() { let value = 1 }"
    private val copyableMacro = "@C @M @Deprecated public func item(): Unit {}"

    /** 只读实际直接子节点，避免为寻找来源而遍历/预展开无关函数体。 */
    private fun directMacros(file: CjFile): List<CjMacroExpression> = file.children.filterIsInstance<CjMacroExpression>()
    private fun inputFunction(macro: CjMacroExpression): CjNamedFunction =
        assertPresent(macro.input).children.filterIsInstance<CjNamedFunction>().single()
    private fun macroPrefix(macro: CjMacroExpression): CjMacroDeclarationPrefix = assertPresent(macro.parsingFacts).declarationPrefix
    private fun inputPrefix(function: CjNamedFunction): CjMacroDeclarationPrefix =
        assertPresent(assertPresent(function.node.findChildByType(CjTokens.FUNC_KEYWORD)).getCopyableUserData(DECLARATION_PREFIX_KEY))
    private fun publicModifier(function: CjNamedFunction): PsiElement =
        assertPresent(assertPresent(function.modifierList).getModifier(CjTokens.PUBLIC_KEYWORD))
    private fun range(source: CjParsingSourceRange): TextRange = TextRange(source.startOffset, source.endOffset)

    private fun assertPrefix(
        prefix: CjMacroDeclarationPrefix,
        names: List<String>,
        kinds: List<BuiltInAnnotationKind>,
        modifiers: List<CjModifierKind> = emptyList(),
    ) {
        assertValueEquals(names, prefix.annotations.map { it.sourceName })
        assertValueEquals(kinds, prefix.annotations.map { it.kind })
        assertValueEquals(kinds.toSet(), prefix.annotationKinds)
        assertValueEquals(modifiers, prefix.sourceModifiers.map { it.kind })
        assertValueEquals(modifiers.toSet(), prefix.modifiers)
    }

    private fun assertCapturedAnnotation(fact: CjAnnotationSourceFact, annotation: CjAnnotation, file: CjFile) {
        val at = assertPresent(annotation.atSymbol).node
        assertIdentical(fact, at.getCopyableUserData(ANNOTATION_SOURCE_FACT_KEY))
        assertValueEquals(fact.origin.rootIdentity, at.getCopyableUserData(PARSER_SOURCE_ROOT_KEY))
        assertIdentical(fact.origin.range, fact.sourceRange)
        assertSourceRange(fact.sourceRange, annotation, file)
    }

    private fun assertBoundAnnotation(
        captured: CjAnnotationSourceFact, bound: CjAnnotationSourceFact, annotation: CjAnnotation, file: CjFile,
    ) {
        assertIdentical(captured.origin, bound.origin)
        assertValueEquals(captured.sourceName, bound.sourceName)
        assertValueEquals(captured.kind, bound.kind)
        assertSourceRange(bound.sourceRange, annotation, file)
    }

    private fun assertBoundModifier(
        captured: CjModifierSourceFact, bound: CjModifierSourceFact, modifier: PsiElement, file: CjFile,
    ) {
        assertIdentical(captured.origin, bound.origin)
        assertValueEquals(captured.kind, bound.kind)
        assertSourceRange(bound.sourceRange, modifier, file)
    }

    /** 当前位置直接对照实际 PSI textRange；原 origin 的偏移不参与计算当前范围。 */
    private fun assertSourceRange(source: CjParsingSourceRange, element: PsiElement, file: CjFile) {
        assertValueEquals(element.textRange, range(source))
        assertValueEquals(element.text, source.source.text.substring(source.startOffset, source.endOffset))
        assertValueEquals(file.node.chars.toString(), source.source.text)
        assertValueEquals(file.name, source.source.fileName)
        assertValueEquals(file.virtualFile?.path, source.source.filePath)
    }

    private data class OwnerBindings(val macro: CjMacroDeclarationPrefix, val input: CjMacroDeclarationPrefix)

    private fun assertCopyableOwnerBindings(
        scope: CjParsingSourceBindingScope, file: CjFile, macro: CjMacroExpression,
        prefix: CjMacroDeclarationPrefix, input: CjMacroDeclarationPrefix,
    ): OwnerBindings {
        val function = inputFunction(macro)
        val body = assertPresent(function.bodyBlockExpression).node as LazyParseableElement
        val wasParsed = body.isParsed
        assertPrefix(prefix, listOf("C"), listOf(BuiltInAnnotationKind.C))
        assertPrefix(input, listOf("Deprecated"), listOf(BuiltInAnnotationKind.DEPRECATED), listOf(CjModifierKind.PUBLIC))
        val boundMacro = scope.bind(prefix, macro)
        val boundInput = scope.bind(input, function)
        assertBoundAnnotation(prefix.annotations.single(), boundMacro.annotations.single(), macro.annotationEntries.single(), file)
        assertBoundAnnotation(input.annotations.single(), boundInput.annotations.single(), function.annotationEntries.single(), file)
        assertBoundModifier(input.sourceModifiers.single(), boundInput.sourceModifiers.single(), publicModifier(function), file)
        assertValueEquals(wasParsed, body.isParsed, "绑定不得改变 SDK copy 留下的 lazy 状态")
        return OwnerBindings(boundMacro, boundInput)
    }

    private fun parseLight(source: String, lexicalInput: CjLexicalSourceInput? = null): CangJieLightParseResult {
        val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), source)
        builder.setDebugMode(true)
        return CangJieLightParser.parseBuffered(builder, lexicalInput = lexicalInput)
    }

    private fun assertLightPrefix(
        result: CangJieLightParseResult, offset: Int, names: List<String>, kinds: List<BuiltInAnnotationKind>,
    ) {
        val prefix = assertPresent(result.file.macroFacts[offset]).declarationPrefix
        assertPrefix(prefix, names, kinds)
        for (fact in prefix.annotations) assertInitialLightAnnotation(fact, result, "@${fact.sourceName}")
    }

    private fun assertInitialLightAnnotation(fact: CjAnnotationSourceFact, result: CangJieLightParseResult, text: String) {
        val snapshot = result.file.sourceContext.snapshot
        val start = snapshot.text.indexOf(text)
        assertTrueValue(start >= 0)
        assertValueEquals(TextRange(start, start + text.length), range(fact.sourceRange))
        assertIdentical(snapshot, fact.sourceRange.source)
        assertIdentical(fact.origin.range, fact.sourceRange)
    }

    private data class MacroCopies(val file: CjFile, val original: CjMacroExpression, val inserted: CjMacroExpression)

    private fun createSameFileMacroCopies(name: String): MacroCopies {
        val file = createPsiFile(name, "$beforeFunction\n$copyableMacro\n") as CjFile
        val original = directMacros(file).single()
        val inserted = insertMacroCopy(file, original, original)
        // 同文件 copy/addAfter 的实际完整文本在两宏之间保留两个换行，末尾没有换行。
        assertValueEquals("$beforeFunction\n$copyableMacro\n\n$copyableMacro", file.text)
        assertDistinct(original.annotationEntries.single().node, inserted.annotationEntries.single().node)
        assertDistinct(publicModifier(inputFunction(original)).node, publicModifier(inputFunction(inserted)).node)
        return MacroCopies(file, original, inserted)
    }

    /** 使用标准 PSI copy/addAfter 插入真实 AST；不手工安装来源 key，也不制造新的 parser origin。 */
    private fun insertMacroCopy(file: CjFile, original: CjMacroExpression, anchor: PsiElement): CjMacroExpression {
        val detached = original.copy() as CjMacroExpression
        lateinit var inserted: CjMacroExpression
        applyPsiChangesOnEdt {
            val separator = file.addAfter(CjPsiFactory(project, markGenerated = false).createWhiteSpace("\n"), anchor)
            inserted = file.addAfter(detached, separator) as CjMacroExpression
        }
        assertDistinct(original.node, inserted.node)
        assertIdentical(file, inserted.containingFile)
        return inserted
    }

    /** 与既有 RoleFileStubTimingTest 相同的 BlockSupportImpl/DiffLog 协议，严格要求局部 body 根。 */
    private fun applyEarlierBlockDiff(file: CjFile): String {
        val oldText = file.text
        val before = file.children.filterIsInstance<CjNamedFunction>().single { it.name == "before" }
        val body: ASTNode = assertPresent(before.bodyBlockExpression).node
        val start = oldText.indexOf("= 1") + "= ".length
        assertValueEquals("1", oldText.substring(start, start + 1))
        val updated = oldText.replaceRange(start, start + 1, "12345")
        val result = BlockSupportImpl.reparse(
            file, file.node, TextRange(start, start + 1), updated, EmptyProgressIndicator(), oldText,
        )
        assertIdentical(body, result.oldRoot, "SDK 必须只重解析前方函数体，不能整文件重建来源")
        assertValueEquals(oldText, file.text)
        applyPsiChangesOnEdt { result.log.doActualPsiChange(file) }
        assertValueEquals(updated, file.text)
        return updated
    }

    private fun applyPsiChangesOnEdt(change: () -> Unit) {
        EventQueue.invokeAndWait {
            CommandProcessor.getInstance().executeCommand(project, {
                ApplicationManager.getApplication().runWriteAction { change() }
            }, "更新真实来源 AST", null)
        }
    }
}
