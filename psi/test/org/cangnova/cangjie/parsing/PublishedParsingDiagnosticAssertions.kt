package org.cangnova.cangjie.parsing

import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiBuilderFactory
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.psi.CjFile
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * 旧用例继续验收最终 published diagnostics；原 offset、message、origin、category、每个 hint
 * 和显式 source 全部保留。PARSE 是 parser 专测的真实类别期望，不能视为未指定。
 * 旧构造器未指定的新 rangeKind/mainRange 几何契约另行核对，显式 mainRange 仍作全值比较。
 * 规范范围由下面的独立算法核对，绝不调用 collector 或 SourceSnapshot.nativePosition 生成期望。
 */
internal fun assertLegacyParsingDiagnostics(
    expected: List<ParsingDiagnostic>,
    actual: List<ParsingDiagnostic>,
    context: CjParserSourceContext,
    message: String = "",
) {
    assertEquals(expected.size, actual.size, message)
    expected.zip(actual).forEach { (raw, published) ->
        assertEquals(raw.startOffset, published.startOffset, message)
        assertEquals(raw.endOffset, published.endOffset, message)
        assertEquals(raw.message, published.message, message)
        assertEquals(raw.origin, published.origin, message)
        assertSame(raw.source, published.source, message)
        assertEquals(raw.category, published.category, message)
        if (raw.rangeKind != ParsingDiagnostic.RangeKind.RANGE) assertEquals(raw.rangeKind, published.rangeKind, message)
        assertEquals(raw.hints.size, published.hints.size, message)
        raw.hints.zip(published.hints).forEach { (hint, actualHint) ->
            assertEquals(hint.startOffset, actualHint.startOffset, message)
            assertEquals(hint.endOffset, actualHint.endOffset, message)
            assertEquals(hint.label, actualHint.label, message)
            assertSame(hint.source, actualHint.source, message)
            if (hint.rangeKind != ParsingDiagnostic.RangeKind.RANGE) assertEquals(hint.rangeKind, actualHint.rangeKind, message)
        }
        // 新协议的显式完整期望仍执行 data class 全值比较，不剥掉任何规范字段。
        if (raw.mainRange != null) assertEquals(raw, published, message)
        else assertTrue(raw.nativeHints.isEmpty(), "显式规范 hint 期望必须同时提供 mainRange：$message")
    }
    assertCanonicalParsingRanges(actual, context, message)
}

/** 两入口已共享真实输入，完整诊断值必须相等；分别核对坐标，不能只互相证明正确。 */
internal fun assertPublishedParsingDiagnosticParity(
    psi: List<ParsingDiagnostic>, light: List<ParsingDiagnostic>, context: CjParserSourceContext, message: String = "",
) {
    assertCanonicalParsingDiagnostics(psi, context, message)
    assertCanonicalParsingDiagnostics(light, context, message)
    assertEquals(psi, light, message)
}

/**
 * category 选择发布通道，不改变几何；一次 publication 不混合 LEX/PARSE。
 * RANGE 跨行定位、POINT 原生宽一字节列、TOKEN literal 字节宽度分别核对，不能一律按字符裁剪。
 * category 去重与 LEX 门的首报语义由 Collector/ReportOrder/LEX 专测独立覆盖。
 */
internal fun assertCanonicalParsingDiagnostics(
    diagnostics: List<ParsingDiagnostic>, context: CjParserSourceContext, message: String = "",
) {
    assertTrue(diagnostics.map { it.category }.distinct().size <= 1, message)
    assertCanonicalParsingRanges(diagnostics, context, message)
}

/** producer reports 可以同时保存 LEX/PARSE；范围完整性与最终发布通道分别验证。 */
private fun assertCanonicalParsingRanges(
    diagnostics: List<ParsingDiagnostic>, context: CjParserSourceContext, message: String,
) {
    for (diagnostic in diagnostics) {
        val main = assertNotNull(diagnostic.mainRange, message)
        val source = diagnostic.source ?: context.snapshot
        assertSame(source, main.source, message)
        assertEquals(source.identity, main.source.identity, message)
        val coordinates = if (diagnostic.source == null && diagnostic.origin == ParsingDiagnostic.Origin.ORIGINAL_FILE) {
            (1 to 1) to (1 to 2)
        } else {
            val offset = if (diagnostic.source == null) context.inputStart else 0
            nativeCoordinates(source.text, offset + diagnostic.startOffset, offset + diagnostic.endOffset, diagnostic.rangeKind)
        }
        assertEquals(coordinates.first, main.begin, message)
        assertEquals(coordinates.second, main.end, message)
        assertEquals(diagnostic.hints.size, diagnostic.nativeHints.size, message)
        diagnostic.hints.zip(diagnostic.nativeHints).forEach { (hint, native) ->
            val hintSource = hint.source ?: context.snapshot
            assertSame(hintSource, native.range.source, message)
            assertEquals(hintSource.identity, native.range.source.identity, message)
            val offset = if (hint.source == null) context.inputStart else 0
            val hintCoordinates = nativeCoordinates(hintSource.text, offset + hint.startOffset,
                offset + hint.endOffset, hint.rangeKind)
            assertEquals(hintCoordinates.first, native.range.begin, message)
            assertEquals(hintCoordinates.second, native.range.end, message)
            assertEquals(hint.label, native.label, message)
        }
    }
}

/** 官方 Position.h 的列为 UTF-8 字节；按输入全文扫描换行，不借用被测 lines mapping。 */
private fun nativePosition(text: String, offset: Int): Pair<Int, Int> {
    require(offset in 0..text.length)
    var line = 1
    var lineStart = 0
    var index = 0
    while (index < offset) {
        if (text[index] == '\r' || text[index] == '\n') {
            val nextLine = index + if (text[index] == '\r' && index + 1 < text.length && text[index + 1] == '\n') 2 else 1
            if (nextLine > offset) break
            line++
            lineStart = nextLine
            index = nextLine
        } else {
            index++
        }
    }
    return line to text.substring(lineStart, offset).toByteArray(Charsets.UTF_8).size + 1
}

private fun nativeCoordinates(
    text: String, start: Int, end: Int, kind: ParsingDiagnostic.RangeKind,
): Pair<Pair<Int, Int>, Pair<Int, Int>> {
    require(start in 0..end && end <= text.length)
    val begin = nativePosition(text, start)
    val finish = when (kind) {
        ParsingDiagnostic.RangeKind.RANGE -> nativePosition(text, end)
        ParsingDiagnostic.RangeKind.POINT -> {
            assertEquals(start, end)
            begin.first to begin.second + 1
        }
        ParsingDiagnostic.RangeKind.TOKEN -> begin.first to
            begin.second + text.substring(start, end).toByteArray(Charsets.UTF_8).size
    }
    return begin to finish
}

/** 展开后只读取真实 ROOT 输入，并验证它与 producer 冻结 region 的全文来源身份一致。 */
internal fun publishedParserSeed(file: CjFile): CjParserSeed {
    file.node
    val seed = assertNotNull(file.getCopyableUserData(ROOT_PARSER_SEED_KEY))
    val context = assertNotNull(seed.sourceContext)
    val input = assertNotNull(seed.lexicalInput)
    val region = assertNotNull(file.parsedRoleRegion)
    assertEquals(file.text, context.snapshot.text.substring(context.inputStart, context.inputEnd))
    assertSame(context.snapshot, input.source)
    assertEquals(context, region.sourceContext)
    assertSame(input, region.lexicalInput)
    assertNotNull(file.parsingDiagnosticScopeOrNull())
    return seed
}

/** freeze 只增加真实 sourceContext/lexicalInput；完整文法、角色、父 scope 等其他输入逐值保留。 */
internal fun assertPublishedParserSeed(original: CjParserSeed, file: CjFile): CjParserSeed {
    val actual = publishedParserSeed(file)
    assertEquals(original.copy(sourceContext = actual.sourceContext, lexicalInput = actual.lexicalInput), actual)
    assertSame(original.diagnosticScope, actual.diagnosticScope)
    if (original.diagnosticScope == null) {
        assertNull(actual.diagnosticScope)
        assertEquals(emptyList<Long>(), assertNotNull(file.parsingDiagnosticScopeOrNull()).path)
    }
    return actual
}

/** 普通 LT 入口共享 PSI 的真实冻结输入；输出 scope 属于新根，与输入的父 scope 分开。 */
internal fun parseLightWithPsiInput(file: CjFile, builder: PsiBuilder): CangJieLightParseResult {
    val seed = assertNotNull(file.getCopyableUserData(ROOT_PARSER_SEED_KEY))
    val view = file.currentPsiSourceView()
    val result = CangJieLightParser.parseBuffered(builder, languageModuleName = seed.languageModuleName,
        sourceKind = seed.sourceKind, cjmpContext = seed.cjmpContext, lexicalInput = view.lexicalInput)
    assertSame(view.lexicalInput, result.file.lexicalInput)
    assertSame(view.sourceContext.snapshot, result.file.sourceContext.snapshot)
    assertEquals(view.sourceContext, result.file.sourceContext)
    assertEquals(emptyList<Long>(), result.file.diagnosticScope.path)
    assertTrue(result.file.diagnosticScope.rootIdentity != assertNotNull(file.parsingDiagnosticScopeOrNull()).rootIdentity)
    view.requireCurrent()
    return result
}

/** 注解使用正式缓冲入口；这里只验证真实输入和新根身份，不复制 parser/collector 产线。 */
internal fun parseAnnotationLightWithPsiInput(file: CjFile): CangJieLightParseResult {
    val seed = publishedParserSeed(file)
    val view = file.currentPsiSourceView()
    val context = view.sourceContext
    val input = view.lexicalInput
    val builder = PsiBuilderFactory.getInstance().createBuilder(CangJieParserDefinition(), CangJieLexer(), file.text)
    builder.setDebugMode(true)
    val result = CangJieLightParser.parseAnnotationOnlyBuffered(builder, languageModuleName = seed.languageModuleName,
        sourceKind = seed.sourceKind, cjmpContext = seed.cjmpContext, lexicalInput = input)
    result.file.requireConsistentInput()
    assertSame(context.snapshot, result.file.sourceContext.snapshot)
    assertSame(input, result.file.lexicalInput)
    assertEquals(context, result.file.sourceContext)
    assertEquals(CangJieLightDiagnosticOwnership.BUFFERED, result.file.diagnosticOwnership)
    assertEquals(emptyList<Long>(), result.file.diagnosticScope.path)
    assertTrue(result.file.diagnosticScope.rootIdentity != assertNotNull(file.parsingDiagnosticScopeOrNull()).rootIdentity)
    view.requireCurrent()
    return result
}
