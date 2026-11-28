package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.cfir.CfirDeclarationDataKey
import org.cangnova.cangjie.parsing.ParsingFileFacts
import org.cangnova.cangjie.parsing.CjParserSourceContext
import org.cangnova.cangjie.messages.CjParsingDiagnosticPublication
import org.cangnova.cangjie.parsing.CjParserSettings

private object ParsingFileFactsKey : CfirDeclarationDataKey()
private object ParsingSourceContextKey : CfirDeclarationDataKey()
private object ParsingDiagnosticsKey : CfirDeclarationDataKey()
private object ParserSettingsKey : CfirDeclarationDataKey()

private var CfirFile.storedParsingFileFacts: ParsingFileFacts?
    by CfirDeclarationDataRegistry.data(ParsingFileFactsKey)

private var CfirFile.storedParsingSourceContext: CjParserSourceContext?
    by CfirDeclarationDataRegistry.data(ParsingSourceContextKey)

private var CfirFile.storedParsingDiagnostics: CjParsingDiagnosticPublication?
    by CfirDeclarationDataRegistry.data(ParsingDiagnosticsKey)
private var CfirFile.storedParserSettings: CjParserSettings?
    by CfirDeclarationDataRegistry.data(ParserSettingsKey)

/** raw 构建一次发布的原始文件文法事实；resolve 和宏事务只能读取。 */
val CfirFile.parsingFileFacts: ParsingFileFacts? get() = storedParsingFileFacts

/** 源码 raw 文件的实际 parser 输入；二进制模型没有这份 source parser 身份。 */
val CfirFile.parsingSourceContext: CjParserSourceContext? get() = storedParsingSourceContext

/** 实际 parser 产品的全部报告保留 producer 顺序，编译单元发布时才能应用跨文件 LEX 门。 */
val CfirFile.parsingDiagnosticPublication: CjParsingDiagnosticPublication? get() = storedParsingDiagnostics
val CfirFile.parserSettings: CjParserSettings? get() = storedParserSettings

/** 与 pre-macro 产品共同冻结，文件复制和裁剪继续持有同一批报告及其调用选项。 */
fun CfirFile.publishParsingDiagnostics(publication: CjParsingDiagnosticPublication, settings: CjParserSettings) {
    check(storedParsingDiagnostics == null || storedParsingDiagnostics === publication) {
        "A raw file cannot substitute another parser diagnostic product"
    }
    check(storedParserSettings == null || storedParserSettings == settings)
    storedParsingDiagnostics = publication
    storedParserSettings = settings
}

/** 文件事实只在 raw 构建结束时发布一次，后续角色变化归宏构造事务所有。 */
fun CfirFile.publishParsingFileFacts(facts: ParsingFileFacts, sourceContext: CjParserSourceContext) {
    check(storedParsingFileFacts == null) { "File syntax facts have already been published." }
    check(storedParsingSourceContext == null) { "File parser source has already been published." }
    storedParsingFileFacts = facts
    storedParsingSourceContext = sourceContext
}

/**
 * LL 保留声明图时只迁移源码代次。语义文件事实须由当前完整 parser 产品证明不变，
 * 不能以重新 publish 绕过原始事实的一次性发布契约。
 */
@org.cangnova.cangjie.cfir.CfirImplementationDetail
fun CfirFile.replaceParsingSourceContext(
    expected: CjParserSourceContext,
    replacement: CjParserSourceContext,
    currentFacts: ParsingFileFacts,
    diagnostics: CjParsingDiagnosticPublication,
    settings: CjParserSettings,
) {
    check(storedParsingSourceContext === expected) { "The parser source generation changed before source replacement." }
    check(storedParsingFileFacts == currentFacts) { "Changed file-role semantics require a new CFIR declaration generation." }
    check(storedParserSettings == settings) { "Changed parser invocation settings require a new source generation." }
    storedParsingSourceContext = replacement
    storedParsingDiagnostics = diagnostics
}
