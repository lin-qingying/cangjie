package org.cangnova.cangjie.parsing

import com.intellij.lang.ASTNode
import com.intellij.lang.ASTFactory
import com.intellij.lang.Language
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.lang.LighterASTNode
import com.intellij.lang.LighterLazyParseableNode
import com.intellij.lang.ParserDefinition
import com.intellij.lang.PsiBuilder
import com.intellij.lang.impl.PsiBuilderImpl
import com.intellij.lexer.Lexer
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.impl.source.tree.TreeElement
import com.intellij.psi.impl.source.tree.CompositeElement
import com.intellij.psi.impl.source.tree.LeafElement
import com.intellij.psi.impl.source.tree.LazyParseableElement
import com.intellij.psi.impl.source.DummyHolder
import com.intellij.psi.text.BlockSupport
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.IFileElementType
import com.intellij.psi.tree.ILazyParseableElementType
import com.intellij.util.ThreeState
import com.intellij.util.ThreeState.NO
import com.intellij.util.ThreeState.UNSURE
import com.intellij.util.TripleFunction
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNodeTypes

/**
 * SDK 通过 ParserDefinition 的 ASTFactory 能力构造 composite；该对象仅属于当前 parser。
 * 普通建树和 ASTConverter 都经过这里，原 definition 的三类工厂协议完整保留。
 */
private class CangJieSyntaxTreeFactory(private val original: ParserDefinition) : ASTFactory(), ParserDefinition by original {
    private var prepared = false
    private var fileResult: CjParsedFileResult? = null

    fun prepare(result: CjParsedFileResult?) {
        check(!prepared)
        fileResult = result
        prepared = true
    }

    override fun createComposite(type: IElementType): CompositeElement? {
        val element = (original as? ASTFactory)?.createComposite(type)
        if (type != CjNodeTypes.PACKAGE_DIRECTIVE && type != CjNodeTypes.MACRO_FRAGMENT_CONTENTS) return element
        check(prepared) { "File result nodes cannot be constructed before the parser transaction is frozen." }
        val result = checkNotNull(fileResult) { "A parser result carrier requires a complete root parse result." }
        return (element ?: ASTFactory.composite(type)).also { it.putCopyableUserData(PARSED_FILE_RESULT_KEY, result) }
    }

    override fun createLeaf(type: IElementType, text: CharSequence): LeafElement? =
        (original as? ASTFactory)?.createLeaf(type, text)

    override fun createLazy(type: ILazyParseableElementType, text: CharSequence?): LazyParseableElement? =
        (original as? ASTFactory)?.createLazy(type, text)
}

/**
 * 每次解析私有的 SDK builder。保留平台 chameleon/oldTree/CharTable 协议，
 * 在 SDK 真正创建普通、折叠及增量 diff token 时安装本轮冻结的语法事实。
 */
internal class CangJiePsiBuilder : PsiBuilderImpl {
    internal val offsetOrigin: Int
    internal val sourceContext: CjParserSourceContext?
    internal val lexicalInput: CjLexicalSourceInput?
    /** 原生构造器收到的真实文件，不从解析中的 AST 再次查询。 */
    internal val contextFile: CjFile?
    private val syntaxFactory: CangJieSyntaxTreeFactory
    private var preparedData: CjPreparedParsingData? = null
    private var fileResult: CjParsedFileResult? = null
    private var sourceRootIdentity: java.util.UUID? = null
    private val parsingRoot: ASTNode?
    private val parsingProject: Project?

    constructor(project: Project, definition: ParserDefinition, lexer: Lexer, node: ASTNode, text: CharSequence) :
        this(project, CangJieSyntaxTreeFactory(definition), lexer, node, text.toString())

    private constructor(project: Project, definition: CangJieSyntaxTreeFactory, lexer: Lexer, node: ASTNode, text: String) :
        super(project, definition, lexer, CjNativeFragmentResultHandoff.prepareRoot(project, node), text) {
        syntaxFactory = definition
        parsingRoot = node
        parsingProject = project
        offsetOrigin = 0
        contextFile = node.psi?.containingFile as? CjFile
        sourceContext = sourceContextForChameleon(node, text)
        lexicalInput = sourceContext?.let { context ->
            val configured = if (node.elementType is IFileElementType) contextFile?.psiCompilationInput else null
            val inherited = configured?.seed?.lexicalInput ?: node.getCopyableUserData(PARSER_SEED_KEY)?.lexicalInput
                ?: (node.psi?.containingFile as? CjFile)?.parsedRoleRegion?.lexicalInput
            inherited?.takeIf { it.source === context.snapshot } ?: CjLexicalSourceInput(context.snapshot)
        }
        installComparator()
    }

    constructor(project: Project, definition: ParserDefinition, lexer: Lexer, node: LighterLazyParseableNode, text: CharSequence) :
        this(project, CangJieSyntaxTreeFactory(definition), lexer, node, text.toString())

    private constructor(project: Project, definition: CangJieSyntaxTreeFactory, lexer: Lexer, node: LighterLazyParseableNode, text: String) :
        super(project, definition, lexer, node, text) {
        syntaxFactory = definition
        parsingRoot = null
        parsingProject = project
        offsetOrigin = node.startOffset
        contextFile = null
        // Light lazy 输入没有 PSI 文件；parseSeeded 必须传其冻结的、完整的 source context。
        sourceContext = null
        lexicalInput = null
        installComparator()
    }

    constructor(definition: ParserDefinition, lexer: Lexer, text: CharSequence) :
        this(CangJieSyntaxTreeFactory(definition), lexer, text.toString())

    private constructor(definition: CangJieSyntaxTreeFactory, lexer: Lexer, text: String) :
        super(null, null, definition, lexer, null, text, null, null) {
        syntaxFactory = definition
        parsingRoot = null
        parsingProject = null
        offsetOrigin = 0
        contextFile = null
        // 这个构造器接收独立的无文件文本；命名 Light 根由解析入口显式提供 metadata。
        sourceContext = null
        lexicalInput = null
        installComparator()
    }

    /** 先准备 SDK 范围，再冻结；treeBuilt 随后的 merge 异常是平台正常控制流。 */
    fun finishParsing(journal: MacroParsingJournal, result: CjParsedFileResult?): CjParsedFileResult? {
        check(preparedData == null) { "A parser root can only be published once." }
        lightTree
        preparedData = journal.freeze(offsetOrigin)
        sourceRootIdentity = journal.rootIdentity
        val finalizedResult = result?.copy(
            roleEvents = checkNotNull(preparedData).roleEvents,
            roleRegion = checkNotNull(preparedData).roleRegion,
            diagnosticScope = checkNotNull(preparedData).diagnosticScope,
        )
        fileResult = finalizedResult
        syntaxFactory.prepare(finalizedResult)
        // SDK 整文件替换搬运原生表；按已准备的真实表登记，候选文件与零孩子不会提前更新原文件。
        if (parsingRoot?.elementType is com.intellij.psi.impl.source.tree.ICodeFragmentElementType && finalizedResult != null) {
            CjNativeFragmentResultHandoff.registerFrozenResult(checkNotNull(parsingProject), parsingRoot, finalizedResult)
        }
        return finalizedResult
    }

    override fun createLeaf(type: IElementType, start: Int, end: Int): TreeElement {
        val data = checkNotNull(preparedData) { "Syntax tokens cannot be built before their parser transaction is frozen." }
        val leaf = super.createLeaf(type, start, end)
        publishParsingToken(leaf, type, start, end, data, checkNotNull(sourceRootIdentity), fileResult == null)
        return leaf
    }

    private fun installComparator() {
        putUserData(CUSTOM_COMPARATOR, TripleFunction<ASTNode, LighterASTNode, FlyweightCapableTreeStructure<LighterASTNode>, ThreeState> { old, fresh, _ ->
            val data = checkNotNull(preparedData) { "Incremental comparison requires a frozen parser transaction." }
            val key = CjSeedToken(fresh.tokenType, fresh.startOffset - offsetOrigin, fresh.endOffset - offsetOrigin)
            val newSeed = data.roots[key] ?: data.seeds[key]
            val oldSeed = old.parserSeedOrNull()
            if ((fresh.tokenType == CjNodeTypes.PACKAGE_DIRECTIVE || fresh.tokenType == CjNodeTypes.MACRO_FRAGMENT_CONTENTS) &&
                old.getCopyableUserData(PARSED_FILE_RESULT_KEY) !== fileResult
            ) NO
            else if (oldSeed != newSeed) NO
            else if ((fresh.tokenType == CjTokens.AT || fresh.tokenType == CjTokens.ATEXCL) &&
                old.getCopyableUserData(MACRO_PARSING_FACTS_KEY) != data.macroFacts[key.start]
            ) NO
            else if (fresh.tokenType in CJMP_NOMINAL_KEYWORDS &&
                old.getCopyableUserData(CJMP_NOMINAL_SYNTAX_FACTS_KEY) != data.nominalFacts[key.start]
            ) NO
            else if (old is LeafElement &&
                old.getCopyableUserData(CJMP_DECLARATION_ROLE_CHECK_KEY) != data.declarationRoleChecks[key.start]
            ) NO
            // 事件身份属于单次解析事务，即使文本相同也不能沿用上一代叶节点。
            else if (old is LeafElement &&
                old.getCopyableUserData(CJMP_DECLARATION_ROLE_EVENTS_KEY) !== data.declarationRoleEvents[key.start]
            ) NO
            else if (old is LeafElement && key.start == 0 && fresh.tokenType == CjTokens.LBRACE && fileResult == null &&
                old.getCopyableUserData(PARSED_ROLE_REGION_KEY) !== data.roleRegion
            ) NO
            else if (old is LeafElement && (
                old.getCopyableUserData(MODIFIER_SOURCE_FACT_KEY) !== data.modifierSources[key.start] ||
                old.getCopyableUserData(ANNOTATION_SOURCE_FACT_KEY) !== data.annotationSources[key.start] ||
                old.getCopyableUserData(DECLARATION_PREFIX_KEY) !== data.declarationPrefixes[key.start] ||
                old.getCopyableUserData(DECLARATION_MODIFIERS_KEY) !== data.declarationModifiers[key.start]
            )) NO
            else if (old is LeafElement && old.getCopyableUserData(PARSER_SOURCE_TOKEN_KEY) != data.sourceTokens[key.start]) NO
            else UNSURE
        })
    }
}

/**
 * 输入构造层按 SDK 的真实替换协议建立候选全文；DummyHolder 自身坐标不是源文件坐标。
 * 读取 viewProvider.contents 不会展开当前 chameleon，也不以磁盘内容代替本代文本。
 */
private fun sourceContextForChameleon(node: ASTNode, input: CharSequence): CjParserSourceContext? {
    val configured = if (node.elementType is IFileElementType) {
        (node.psi?.containingFile as? CjFile)?.psiCompilationInput?.seed?.sourceContext
    } else null
    if (configured != null) {
        configured.requireInput(input)
        return configured
    }
    val replacement = node.getUserData(BlockSupport.TREE_TO_BE_REPARSED)
    if (replacement != null) {
        val oldNode = replacement.first
        val oldText = replacement.second.toString()
        val start = oldNode.startOffset
        val end = start + oldNode.textLength
        require(start in 0..end && end <= oldText.length &&
            oldText.regionMatches(start, oldNode.chars.toString(), 0, oldNode.textLength)) {
            "The platform reparse range must describe the old source node."
        }
        val file = oldNode.psi.containingFile
        val candidate = oldText.substring(0, start) + input + oldText.substring(end)
        return CjParserSourceContext(CjParsingSourceSnapshot(file.name, file.virtualFile?.path, candidate), start, start + input.length)
            .also { it.requireInput(input) }
    }
    val file = node.psi?.containingFile ?: return null
    // SDK 的普通子树复制也用 DummyHolder，可在插入前展开 lazy 副本；没有替换协议时它是独立匿名输入。
    if (file is DummyHolder) return CjParserSourceContext.complete(input)
    val text = file.viewProvider.contents.toString()
    val stored = file.getCopyableUserData(PARSER_SOURCE_SNAPSHOT_KEY)
    // SDK 的整文件 PSI copy 保留 originalFile 与冻结根输入，但关闭事件系统后 virtualFile 为 null。
    // 同一实际解析根的副本继续使用该输入身份；匿名新文件和已修改内容没有这条来源证明。
    val copiedInput = stored != null && file is CjFile && file.originalFile !== file &&
        file.parsedRoleRegion?.sourceContext?.snapshot === stored
    val snapshot = if (stored != null && stored.text == text &&
        (stored.fileName == file.name && stored.filePath == file.virtualFile?.path || copiedInput)) stored
        else CjParsingSourceSnapshot(file.name, file.virtualFile?.path, text)
    return CjParserSourceContext(snapshot, node.startOffset, node.startOffset + input.length).also { it.requireInput(input) }
}

private val CJMP_NOMINAL_KEYWORDS = setOf(
    CjTokens.CLASS_KEYWORD, CjTokens.INTERFACE_KEYWORD, CjTokens.STRUCT_KEYWORD,
    CjTokens.ENUM_KEYWORD, CjTokens.EXTEND_KEYWORD,
)

/** 正式 PSI 根必须经带 chameleon 的 SDK 构造器，不能以无宿主 LT builder 代替。 */
internal fun createCangJiePsiBuilder(project: Project, chameleon: ASTNode, language: Language): CangJiePsiBuilder {
    val definition = checkNotNull(LanguageParserDefinitions.INSTANCE.forLanguage(language)) {
        "No parser definition is registered for $language."
    }
    return CangJiePsiBuilder(project, definition, definition.createLexer(project), chameleon, chameleon.chars)
}

internal class CjFinishedParsing(val tree: ASTNode, val fileResult: CjParsedFileResult?)

private class CjSdkGrammarNode(
    val kind: IElementType,
    val start: Int,
    val end: Int,
    val collapsed: Boolean,
    val children: List<CjSdkGrammarNode>,
)

/**
 * 自有 SDK 构造器在创建叶之前发布事实；普通 SDK builder 则按同次 light→AST 转换的真实
 * 父子结构发布。两者消费同一冻结 journal，后者不通过另一解析或旧树的偏移选择节点。
 */
internal fun PsiBuilder.finishCangJieParsing(
    journal: MacroParsingJournal,
    result: CjParsedFileResult?,
): CjFinishedParsing {
    if (this is CangJiePsiBuilder) {
        val finalized = finishParsing(journal, result)
        return CjFinishedParsing(treeBuilt, finalized)
    }
    val structure = lightTree
    val origin = structure.root.startOffset
    val data = journal.freeze(origin)
    val finalized = result?.copy(roleEvents = data.roleEvents, roleRegion = data.roleRegion, diagnosticScope = data.diagnosticScope)
    fun freeze(light: LighterASTNode): CjSdkGrammarNode {
        if (light is LighterLazyParseableNode) return CjSdkGrammarNode(light.tokenType, light.startOffset, light.endOffset, true, emptyList())
        val reference = Ref<Array<LighterASTNode>>()
        val count = structure.getChildren(light, reference)
        val children = reference.get()
        return try {
            CjSdkGrammarNode(light.tokenType, light.startOffset, light.endOffset, false,
                if (count == 0) emptyList() else (0 until count).map { freeze(checkNotNull(children)[it]) })
        } finally {
            if (children != null) structure.disposeChildren(children, count)
        }
    }
    val grammar = freeze(structure.root)
    val tree = treeBuilt
    fun publish(light: CjSdkGrammarNode, actual: ASTNode) {
        check(light.kind.actualAstElementType() == actual.elementType && light.end - light.start == actual.textLength) {
            "A grammar publication must retain the actual SDK light-to-AST conversion structure."
        }
        val start = light.start - origin
        val end = light.end - origin
        if (light.kind == CjNodeTypes.PACKAGE_DIRECTIVE || light.kind == CjNodeTypes.MACRO_FRAGMENT_CONTENTS) {
            actual.putCopyableUserData(PARSED_FILE_RESULT_KEY, checkNotNull(finalized))
        }
        if (light.collapsed || light.children.isEmpty()) {
            // 空 composite 是诊断或结果容器，不是声明关键字的实际 token 载体。
            if (light.collapsed || actual is LeafElement || actual is LazyParseableElement) {
                publishParsingToken(actual, light.kind, start, end, data, journal.rootIdentity, finalized == null)
            }
            return
        }
        var child = actual.firstChildNode
        for (next in light.children) {
            publish(next, checkNotNull(child))
            child = child.treeNext
        }
        check(child == null)
    }
    publish(grammar, tree)
    return CjFinishedParsing(tree, finalized)
}

private fun publishParsingToken(
    token: ASTNode,
    type: IElementType,
    start: Int,
    end: Int,
    data: CjPreparedParsingData,
    root: java.util.UUID,
    independentBody: Boolean,
) {
    data.seeds[CjSeedToken(type, start, end)]?.let { token.putCopyableUserData(PARSER_SEED_KEY, it) }
    if (type == CjTokens.AT || type == CjTokens.ATEXCL) data.macroFacts[start]?.let { token.putCopyableUserData(MACRO_PARSING_FACTS_KEY, it) }
    data.nominalFacts[start]?.let { token.putCopyableUserData(CJMP_NOMINAL_SYNTAX_FACTS_KEY, it) }
    data.declarationRoleChecks[start]?.let { token.putCopyableUserData(CJMP_DECLARATION_ROLE_CHECK_KEY, it) }
    data.declarationRoleEvents[start]?.let { token.putCopyableUserData(CJMP_DECLARATION_ROLE_EVENTS_KEY, it) }
    data.modifierSources[start]?.let { token.putCopyableUserData(MODIFIER_SOURCE_FACT_KEY, it) }
    data.annotationSources[start]?.let { token.putCopyableUserData(ANNOTATION_SOURCE_FACT_KEY, it) }
    data.declarationPrefixes[start]?.let { token.putCopyableUserData(DECLARATION_PREFIX_KEY, it) }
    data.declarationModifiers[start]?.let { token.putCopyableUserData(DECLARATION_MODIFIERS_KEY, it) }
    data.sourceTokens[start]?.let { token.putCopyableUserData(PARSER_SOURCE_TOKEN_KEY, it) }
    data.parsingReports[start]?.let { token.putCopyableUserData(PARSER_NON_ERROR_REPORTS_KEY, it) }
    if (type == CjTokens.UNKNOWN_TOKEN_START) {
        val id = checkNotNull(data.sourceTokens[start]) { "An unknown leaf requires its actual frozen token identity." }
        val origin = checkNotNull(checkNotNull(data.lexicalInput).diagnosticOriginForToken(id))
        token.putCopyableUserData(PARSER_LEXICAL_DIAGNOSTIC_KEY, origin)
    }
    if (independentBody && start == 0 && type == CjTokens.LBRACE) data.roleRegion?.let {
        token.putCopyableUserData(PARSED_ROLE_REGION_KEY, it)
    }
    if (start in data.modifierSources || start in data.annotationSources || start in data.declarationPrefixes ||
        start in data.declarationModifiers || start in data.macroFacts) token.putCopyableUserData(PARSER_SOURCE_ROOT_KEY, root)
}
