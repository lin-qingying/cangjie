/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.parsing

import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.TokenType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.messages.CjParsingDiagnosticCollector
import org.cangnova.cangjie.messages.CjParsingDiagnosticReport
import org.cangnova.cangjie.lexer.CangJieLexer

/**
 * 提供 `CangJieLightParser` 单例，集中承载仓颉语法解析的共享状态、工厂或工具行为。
 */
object CangJieLightParser {
    /** 在交给 SDK 之前冻结文本；已有 caller-builder 的原始文本必须是不可变 String。 */
    fun createBuilder(text: CharSequence): PsiBuilder = PsiBuilderFactory.getInstance().createBuilder(
        CangJieParserDefinition(), CangJieLexer(), text.toString(),
    )

    /**
     * 按完整仓颉文件语法构造 LightTree；宏片段可传入源模块，显式 package 会覆盖它。
     *
     * @param sourceKind 被解析代码所属源文件的种类；`.cj.d` 传 [CjSourceKind.DECLARATION]。
     *   默认 [CjSourceKind.SOURCE]，既有调用点行为不变。
     */
    fun parse(
        builder: PsiBuilder,
        errorListener: LightTreeParsingErrorListener? = null,
        languageModuleName: String = "",
        sourceKind: CjSourceKind = CjSourceKind.SOURCE,
        cjmpContext: CjmpParsingContext = CjmpParsingContext.SyntaxOnly,
        sourceFile: CjSourceFile? = null,
        settings: CjParserSettings = CjParserSettings(),
    ): ParsedCangJieLightFile =
        parseWith(builder, errorListener, languageModuleName, sourceKind, cjmpContext, isCompleteFile = true,
            diagnosticOwnership = if (errorListener == null) CangJieLightDiagnosticOwnership.UNOBSERVED else CangJieLightDiagnosticOwnership.EXTERNAL_LISTENER,
            sourceContext = CjParserSourceContext.complete(builder.originalText, sourceFile), settings = settings) { parseFile() }

    /** 构建者取得完整诊断事务，确认最终语境后自行发布。 */
    fun parseBuffered(
        builder: PsiBuilder,
        languageModuleName: String = "",
        sourceKind: CjSourceKind = CjSourceKind.SOURCE,
        cjmpContext: CjmpParsingContext = CjmpParsingContext.SyntaxOnly,
        sourceFile: CjSourceFile? = null,
        lexicalInput: CjLexicalSourceInput? = null,
        settings: CjParserSettings = CjParserSettings(),
    ): CangJieLightParseResult = parseBufferedWith(
        builder, languageModuleName, sourceKind, cjmpContext, true, sourceFile, lexicalInput, settings,
    ) { parseFile() }

    /**
     * 按 annotation-only 语法构造 LightTree。
     *
     * languageModuleName 来自源 package，保证无 package header 的片段保留模块限制。
     * 该入口供 custom annotation 宏展开结果重解析使用，保证参数列表生成标准
     * `ANNOTATION -> VALUE_ARGUMENT_LIST -> VALUE_ARGUMENT` 结构，禁止退化为 macro-expression token 容器。
     *
     * @param sourceKind 该片段**来源文件**的种类。片段不是文件，种类由它的归属文件决定
     *   （宏展开产物属于展开它的那个文件），因此这里不按片段自身内容推断。
     */
    fun parseAnnotationOnly(
        builder: PsiBuilder,
        errorListener: LightTreeParsingErrorListener? = null,
        languageModuleName: String = "",
        sourceKind: CjSourceKind = CjSourceKind.SOURCE,
        cjmpContext: CjmpParsingContext = CjmpParsingContext.SyntaxOnly,
        settings: CjParserSettings = CjParserSettings(),
    ): ParsedCangJieLightFile =
        parseWith(builder, errorListener, languageModuleName, sourceKind, cjmpContext, isCompleteFile = false,
            diagnosticOwnership = if (errorListener == null) CangJieLightDiagnosticOwnership.UNOBSERVED else CangJieLightDiagnosticOwnership.EXTERNAL_LISTENER,
            settings = settings) { parseOnlyAnnotationFile() }

    /** 原生注解文法与完整文件共享冻结输入和诊断事务，构建者取得结果后决定提交时机。 */
    fun parseAnnotationOnlyBuffered(
        builder: PsiBuilder,
        languageModuleName: String = "",
        sourceKind: CjSourceKind = CjSourceKind.SOURCE,
        cjmpContext: CjmpParsingContext = CjmpParsingContext.SyntaxOnly,
        sourceFile: CjSourceFile? = null,
        lexicalInput: CjLexicalSourceInput? = null,
        settings: CjParserSettings = CjParserSettings(),
    ): CangJieLightParseResult = parseBufferedWith(
        builder, languageModuleName, sourceKind, cjmpContext, false, sourceFile, lexicalInput, settings,
    ) { parseOnlyAnnotationFile() }

    /**
     * 解析官方宏展开后的声明 token。
     *
     * 与 [parse] 共享完整声明 grammar，但启用 custom annotation 分派，
     * 对齐官方 `ReplaceEachMacro` 的 `EnableCustomAnno` 重解析阶段。
     */
    fun parseMacroExpansion(
        builder: PsiBuilder,
        errorListener: LightTreeParsingErrorListener? = null,
        languageModuleName: String = "",
        sourceKind: CjSourceKind = CjSourceKind.SOURCE,
        cjmpContext: CjmpParsingContext = CjmpParsingContext.SyntaxOnly,
        settings: CjParserSettings = CjParserSettings(),
    ): ParsedCangJieLightFile =
        parseWith(builder, errorListener, languageModuleName, sourceKind, cjmpContext, isCompleteFile = false,
            diagnosticOwnership = if (errorListener == null) CangJieLightDiagnosticOwnership.UNOBSERVED else CangJieLightDiagnosticOwnership.EXTERNAL_LISTENER,
            settings = settings) { parseMacroExpansionFile() }

    /** PSI 与 LightTree 共用原生 scoped ParseNodes，不经过完整文件 preamble。 */
    fun parseMacroCodeFragment(
        builder: PsiBuilder,
        seed: CjParserSeed,
        errorListener: LightTreeParsingErrorListener? = null,
    ): ParsedCangJieMacroFragment = parseMacroCodeFragmentWith(
        builder, seed, errorListener,
        if (errorListener == null) CangJieLightDiagnosticOwnership.UNOBSERVED else CangJieLightDiagnosticOwnership.EXTERNAL_LISTENER,
    )

    /** 保存未发布诊断，供调用方确定最终片段代次后一次性消费。 */
    fun parseMacroCodeFragmentBuffered(builder: PsiBuilder, seed: CjParserSeed): CangJieLightMacroParseResult {
        val diagnostics = mutableListOf<ParsingDiagnostic>()
        val fragment = parseMacroCodeFragmentWith(
            builder, seed, { diagnostics.add(it) }, CangJieLightDiagnosticOwnership.BUFFERED,
        )
        return CangJieLightMacroParseResult(fragment, diagnostics)
    }

    private fun parseMacroCodeFragmentWith(
        builder: PsiBuilder,
        seed: CjParserSeed,
        errorListener: LightTreeParsingErrorListener?,
        diagnosticOwnership: CangJieLightDiagnosticOwnership,
    ): ParsedCangJieMacroFragment {
        val inputText = builder.originalText.toString()
        val ownBuilder = builder as? CangJiePsiBuilder
        val inputSource = ownBuilder?.sourceContext ?: seed.sourceContext
            ?: CjParserSourceContext.complete(builder.originalText)
        // 只有 SDK chameleon 输入构造层能够证明另一份候选输入；普通 seed 不得静默丢失词法来源。
        val lexicalInput = if (ownBuilder?.sourceContext != null) checkNotNull(ownBuilder.lexicalInput)
            else seed.lexicalInput?.also { require(it.source === inputSource.snapshot) }
                ?: CjLexicalSourceInput(inputSource.snapshot)
        val semantic = SemanticWhitespaceAwarePsiBuilderImpl(builder, inputSource, lexicalInput)
        val parser = CangJieParsing.createForTopLevelNonLazy(
            semantic, seed.languageModuleName, seed.sourceKind, seed.cjmpContext, seed.settings,
        )
        parser.restoreSeed(seed)
        with(seed.context) { parser.parseMacroCodeFragment(checkNotNull(seed.macroFragment)) }
        val tree = builder.lightTree
        check(tree.root.startOffset == 0)
        val data = semantic.macroParsingJournal.freeze(0)
        val roots = tree.macroFragmentRoots()
        check(builder.originalText.toString() == inputText) { "Macro parse input changed during parsing." }
        val collector = CjParsingDiagnosticCollector(inputSource, seed.settings.diagnostics)
        data.parsingReports.values.flatten().forEach { collector.restoreReport(it.report) }
        reportErrors(tree.root, tree, collector::restoreReport, inputSource, lexicalInput)
        val diagnostics = collector.finish()
        if (errorListener != null) diagnostics.forEach(errorListener::onError)
        return ParsedCangJieMacroFragment(
            tree, roots, parser.parsedFileFacts, checkNotNull(parser.macroFragmentInfo), data.macroFacts, data.nominalFacts,
            data.declarationRoleChecks, data.roleEvents, seed, inputText, diagnosticOwnership,
            inputSource, data.declarationPrefixes, data.declarationModifiers,
            lexicalInput, collector.publication, data.diagnosticScope,
            verifyTokenStream = { lexicalInput.requireBuilderInput(builder, inputSource, atEntry = false) },
        ).also { it.requireConsistentInput() }
    }

    /** 文件文法的缓冲入口统一领取真实冻结输入，不能在调用侧复制 parser 或诊断收集流程。 */
    private inline fun parseBufferedWith(
        builder: PsiBuilder,
        languageModuleName: String,
        sourceKind: CjSourceKind,
        cjmpContext: CjmpParsingContext,
        isCompleteFile: Boolean,
        sourceFile: CjSourceFile?,
        lexicalInput: CjLexicalSourceInput?,
        settings: CjParserSettings,
        parseAction: CangJieParsing.() -> Unit,
    ): CangJieLightParseResult {
        val diagnostics = mutableListOf<ParsingDiagnostic>()
        val sourceContext = lexicalInput?.let {
            require(it.source.text == builder.originalText.toString()) { "A reparse must retain its actual lexical input." }
            CjParserSourceContext(it.source, 0)
        } ?: CjParserSourceContext.complete(builder.originalText, sourceFile)
        val file = parseWith(builder, { diagnostics.add(it) }, languageModuleName, sourceKind, cjmpContext,
            isCompleteFile, CangJieLightDiagnosticOwnership.BUFFERED, sourceContext,
            lexicalInput ?: CjLexicalSourceInput(sourceContext.snapshot), settings, parseAction)
        return CangJieLightParseResult(file, diagnostics)
    }

    /** 统一创建 parser、执行指定语法入口并完成 LightTree 错误上报。 */
    private inline fun parseWith(
        builder: PsiBuilder,
        errorListener: LightTreeParsingErrorListener?,
        languageModuleName: String,
        sourceKind: CjSourceKind,
        cjmpContext: CjmpParsingContext,
        isCompleteFile: Boolean,
        diagnosticOwnership: CangJieLightDiagnosticOwnership,
        sourceContext: CjParserSourceContext = CjParserSourceContext.complete(builder.originalText),
        lexicalInput: CjLexicalSourceInput = CjLexicalSourceInput(sourceContext.snapshot),
        settings: CjParserSettings = CjParserSettings(),
        parseAction: CangJieParsing.() -> Unit,
    ): ParsedCangJieLightFile {
        val semantic = SemanticWhitespaceAwarePsiBuilderImpl(builder, sourceContext, lexicalInput)
        val cjParsing: CangJieParsing = CangJieParsing.createForTopLevelNonLazy(
            semantic,
            languageModuleName,
            sourceKind,
            cjmpContext,
            settings,
        )
        cjParsing.parseAction()
        val tree = builder.lightTree
        check(tree.root.startOffset == 0) { "A complete file parser requires a full file token stream." }
        val data = semantic.macroParsingJournal.freeze(0)
        val collector = CjParsingDiagnosticCollector(sourceContext, settings.diagnostics)
        data.parsingReports.values.flatten().forEach { collector.restoreReport(it.report) }
        reportErrors(tree.root, tree, collector::restoreReport, sourceContext, lexicalInput)
        val diagnostics = collector.finish()
        if (errorListener != null) diagnostics.forEach(errorListener::onError)
        return ParsedCangJieLightFile(
            tree, cjParsing.parsedFileFacts, data.macroFacts, data.nominalFacts, data.declarationRoleChecks,
            data.roleEvents, cjmpContext, isCompleteFile, languageModuleName, diagnosticOwnership,
            sourceContext, data.declarationPrefixes, data.declarationModifiers,
            checkNotNull(data.lexicalInput), data.sourceTokens, collector.publication, data.diagnosticScope, settings,
            verifyTokenStream = { lexicalInput.requireBuilderInput(builder, sourceContext, atEntry = false) },
        ).also { it.requireConsistentInput() }
    }

    /**
     * 提供 `interface` 操作，封装仓颉语法解析节点的访问、构造或判断逻辑。
     */
    fun interface LightTreeParsingErrorListener {
        fun onError(diagnostic: ParsingDiagnostic)
    }

    /**
     * 执行 `reportErrors` 内部辅助逻辑，支撑仓颉语法解析节点的结构解析与访问。
     */
    private fun reportErrors(
        node: LighterASTNode,
        tree: FlyweightCapableTreeStructure<LighterASTNode>,
        onReport: (CjParsingDiagnosticReport) -> Unit,
        sourceContext: CjParserSourceContext,
        lexicalInput: CjLexicalSourceInput,
    ) {
        val ref = Ref<Array<LighterASTNode?>>()
        val count = tree.getChildren(node, ref)
        val children = ref.get() ?: return
        try {
            for (index in 0 until count) {
                val child = checkNotNull(children[index])
                check(child.tokenType != TokenType.ERROR_ELEMENT) {
                    "A parser error production must retain its actual report identity."
                }
                val errorType = child.tokenType as? CjParsingErrorElementType
                if (errorType != null) {
                    onReport(CjParsingDiagnosticReport(errorType.diagnostic(child.startOffset, child.endOffset), errorType.diagnosticOrder))
                }
                if (child.tokenType === org.cangnova.cangjie.lexer.CjTokens.UNKNOWN_TOKEN_START) {
                    val token = lexicalInput.tokenAt(sourceContext.inputStart + child.startOffset)
                    check(token.type == child.tokenType && token.endOffset == sourceContext.inputStart + child.endOffset)
                    onReport(checkNotNull(lexicalInput.diagnosticForToken(token.id, sourceContext)))
                }

                reportErrors(child, tree, onReport, sourceContext, lexicalInput)
            }
        } finally {
            tree.disposeChildren(children, count)
        }
    }
}
