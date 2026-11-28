package org.cangnova.cangjie.parsing

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.Key
import com.intellij.psi.tree.IElementType
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.CjmpRoleEventKind
import org.cangnova.cangjie.CjmpRoleInputCompleteness
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.messages.CjParsingDiagnosticScope
import org.cangnova.cangjie.parsing.AbstractCangJieParsing.ParsingContext
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.parsedLanguageModuleName
import java.util.Collections
import java.util.IdentityHashMap
import java.util.UUID
import java.security.MessageDigest

/**
 * 独立 parser 根的完整不可变输入。源码种类、编译调用和局部文法是不同维度，分别保留。
 * lazy 节点捕获的是进入该文法时的实际状态，不从 PSI 祖先或 CFIR owner 重新推导。
 */
data class CjParserSeed(
    val sourceKind: CjSourceKind,
    val languageModuleName: String,
    val cjmpContext: CjmpParsingContext,
    val fileRole: CjmpFileRole,
    val quotedSyntaxDepth: Int,
    val context: ParsingContext,
    /** 普通根没有宏入口；宏 code fragment 必须随 chameleon 传递其完整文法描述。 */
    val macroFragment: CjMacroFragmentContext? = null,
    /** 已证明的输入定位与文法 seed 独立；候选重解析由平台输入重新建立本次定位。 */
    val sourceContext: CjParserSourceContext? = null,
    /** 父 parser 为该惰性根登记的真实槽位；独立文件和无父绑定的片段没有此身份。 */
    val roleRegionSlot: CjmpParsedRoleEventId? = null,
    /** 惰性子根复用同一冻结词法输入；新输入的构造器必须重新建立这份身份。 */
    val lexicalInput: CjLexicalSourceInput? = null,
    /** 正文入口在父 producer 时间线中的真实区域；独立新根没有父诊断区域。 */
    val diagnosticScope: CjParsingDiagnosticScope? = null,
    /** 本次编译调用的版本、后端与警告策略；子根不得重新取默认配置。 */
    val settings: CjParserSettings = CjParserSettings(),
) {
    init {
        require(quotedSyntaxDepth >= 0)
        require(lexicalInput == null || sourceContext?.snapshot === lexicalInput.source) {
            "A parser seed must keep its lexical input and source context together."
        }
    }
}

/** 构建钩子使用当前 parser 的局部 token 坐标，发布到 AST 的 seed 本身不包含坐标。 */
internal data class CjSeedToken(val type: IElementType, val start: Int, val end: Int)

/** 事件身份由根事务和该根内的序号组成；独立根不会共享序号空间。 */
data class CjmpParsedRoleEventId(val rootIdentity: UUID, val sequence: Long) {
    init {
        require(sequence >= 0)
    }
}

/** 锚点是所属根内的局部坐标；身份和合并后的文件检查顺序是不同维度。 */
data class CjmpParsedRoleEvent(val id: CjmpParsedRoleEventId, val declarationOffset: Int, val kind: CjmpRoleEventKind)

/** 同一次解析的入口与事件快照；不完整根不得用于推断完整文件角色。 */
class CjmpFileRoleSyntaxSnapshot(
    val parsingContext: CjmpParsingContext,
    val entryFileRole: CjmpFileRole,
    val completeness: CjmpRoleInputCompleteness,
    events: List<CjmpParsedRoleEvent>,
    stubBindings: Map<List<Int>, List<CjmpParsedRoleEventId>> = emptyMap(),
    val sourceFingerprint: String? = null,
    stubDeclarationModifiers: Map<List<Int>, CjParsedDeclarationModifiers> = emptyMap(),
    val lexicalSource: CjLexicalSourceDescriptor? = null,
) {
    val events: List<CjmpParsedRoleEvent> = Collections.unmodifiableList(events.toList())
    /** 文件 stub 的结构路径绑定完整事件身份；空表不代表声明没有角色检查。 */
    val stubBindings: Map<List<Int>, List<CjmpParsedRoleEventId>> = Collections.unmodifiableMap(stubBindings.entries.associate { (path, ids) ->
        Collections.unmodifiableList(path.toList()) to Collections.unmodifiableList(ids.toList())
    })
    /** 返回属性绑定到声明 stub 身份；没有角色事件的声明也可以具有有效空属性。 */
    val stubDeclarationModifiers: Map<List<Int>, CjParsedDeclarationModifiers> = Collections.unmodifiableMap(
        stubDeclarationModifiers.entries.associate { (path, modifiers) -> Collections.unmodifiableList(path.toList()) to modifiers },
    )
    private val eventIdentities: Set<CjmpParsedRoleEvent> =
        Collections.newSetFromMap(IdentityHashMap<CjmpParsedRoleEvent, Boolean>()).apply { addAll(this@CjmpFileRoleSyntaxSnapshot.events) }
    private val eventsById: Map<CjmpParsedRoleEventId, CjmpParsedRoleEvent> = this.events.associateBy { it.id }
    private val eventOrdersById: Map<CjmpParsedRoleEventId, Int> = this.events.mapIndexed { index, event -> event.id to index }.toMap()
    private val eventOrdersByIdentity = IdentityHashMap<CjmpParsedRoleEvent, Int>().apply {
        this@CjmpFileRoleSyntaxSnapshot.events.forEachIndexed { index, event -> put(event, index) }
    }

    internal fun containsEventIdentity(event: CjmpParsedRoleEvent): Boolean = event in eventIdentities
    internal fun eventById(id: CjmpParsedRoleEventId): CjmpParsedRoleEvent? = eventsById[id]
    internal fun eventOrder(event: CjmpParsedRoleEvent): Int? = eventOrdersByIdentity[event]

    init {
        require(sourceFingerprint == null || sourceFingerprint.matches(Regex("[0-9a-f]{64}")))
        require(this.events.all { it.declarationOffset >= 0 })
        require(eventsById.size == this.events.size)
        val previousSequences = hashMapOf<UUID, Long>()
        for (event in this.events) {
            val previous = previousSequences.put(event.id.rootIdentity, event.id.sequence)
            require(previous == null || previous < event.id.sequence)
        }
        val available = this.events.filter { it.kind is CjmpRoleEventKind.Check || it.kind is CjmpRoleEventKind.Extend }
            .mapTo(hashSetOf()) { it.id }
        val bound = hashSetOf<CjmpParsedRoleEventId>()
        require(this.stubBindings.all { (path, ids) ->
            path.isNotEmpty() && path.all { it >= 0 } && ids.isNotEmpty() &&
                ids.all { it in available } &&
                ids.zipWithNext().all { (left, right) -> eventOrdersById.getValue(left) < eventOrdersById.getValue(right) } &&
                ids.all { bound.add(it) }
        })
        require(this.stubDeclarationModifiers.all { (path, modifiers) ->
            path.isNotEmpty() && path.all { it >= 0 } && modifiers.parsingContext == parsingContext
        })
        require(this.stubDeclarationModifiers.isEmpty() || lexicalSource != null)
        require(this.stubDeclarationModifiers.values.all { checkNotNull(lexicalSource).contains(it.sourceToken) })
        // 唯一性只约束本文件的声明属性 stub 表；raw 派生字段和 PSI 副本不受此约束。
        require(this.stubDeclarationModifiers.values.map { it.sourceToken }.toSet().size == this.stubDeclarationModifiers.size)
    }

    /** 在文件 stub 构建完成后附加该树的结构身份，不改变 AST 文件快照。 */
    fun withStubBindings(
        bindings: Map<List<Int>, List<CjmpParsedRoleEventId>>,
        declarationModifiers: Map<List<Int>, CjParsedDeclarationModifiers> = stubDeclarationModifiers,
    ): CjmpFileRoleSyntaxSnapshot =
        CjmpFileRoleSyntaxSnapshot(parsingContext, entryFileRole, completeness, events, bindings, sourceFingerprint, declarationModifiers,
            lexicalSource)

    /** 指纹仅用于内容一致性校验，不证明语境相同或调用期间没有并发修改。 */
    fun matchesSourceContent(text: CharSequence): Boolean? = sourceFingerprint?.let { it == cjmpSourceFingerprint(text) }

    override fun equals(other: Any?): Boolean = other is CjmpFileRoleSyntaxSnapshot &&
        parsingContext == other.parsingContext && entryFileRole == other.entryFileRole &&
        completeness == other.completeness && events == other.events && stubBindings == other.stubBindings &&
        sourceFingerprint == other.sourceFingerprint && stubDeclarationModifiers == other.stubDeclarationModifiers &&
        lexicalSource == other.lexicalSource

    override fun hashCode(): Int {
        var result = parsingContext.hashCode()
        result = 31 * result + entryFileRole.hashCode()
        result = 31 * result + completeness.hashCode()
        result = 31 * result + events.hashCode()
        result = 31 * result + stubBindings.hashCode()
        result = 31 * result + (sourceFingerprint?.hashCode() ?: 0)
        result = 31 * result + stubDeclarationModifiers.hashCode()
        return 31 * result + (lexicalSource?.hashCode() ?: 0)
    }
}

/** 对 UTF-16 代码单元逐一编码，保留未配对代理和所有换行形式。 */
private fun cjmpSourceFingerprint(text: CharSequence): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(8192)
    var used = 0
    for (character in text) {
        buffer[used++] = (character.code ushr 8).toByte()
        buffer[used++] = character.code.toByte()
        if (used == buffer.size) {
            digest.update(buffer)
            used = 0
        }
    }
    digest.update(buffer, 0, used)
    return digest.digest().joinToString("") { "%02x".format(it.toInt() and 0xff) }
}

/** prepareLightTree 完成后冻结的节点构建输入，不能继续写入解析事务。 */
internal class CjPreparedParsingData(
    seeds: Map<CjSeedToken, CjParserSeed>,
    roots: Map<CjSeedToken, CjParserSeed>,
    macroFacts: Map<Int, MacroParsingFacts>,
    nominalFacts: Map<Int, CjmpNominalSyntaxFacts>,
    declarationRoleChecks: Map<Int, CjmpDeclarationRoleCheck>,
    roleEvents: List<CjmpParsedRoleEvent>,
    modifierSources: Map<Int, CjModifierSourceFact>,
    annotationSources: Map<Int, CjAnnotationSourceFact>,
    declarationPrefixes: Map<Int, CjMacroDeclarationPrefix>,
    declarationModifiers: Map<Int, CjParsedDeclarationModifiers>,
    val roleRegion: CjmpParsedRoleRegion?,
    val lexicalInput: CjLexicalSourceInput?,
    sourceTokens: Map<Int, CjSourceTokenId>,
    val diagnosticScope: CjParsingDiagnosticScope,
    parsingReports: Map<Int, List<CjStoredParsingReport>>,
) {
    val parsingReports: Map<Int, List<CjStoredParsingReport>> = Collections.unmodifiableMap(
        parsingReports.mapValues { Collections.unmodifiableList(it.value.toList()) },
    )
    val seeds: Map<CjSeedToken, CjParserSeed> = Collections.unmodifiableMap(LinkedHashMap(seeds))
    val roots: Map<CjSeedToken, CjParserSeed> = Collections.unmodifiableMap(LinkedHashMap(roots))
    val macroFacts: Map<Int, MacroParsingFacts> = Collections.unmodifiableMap(LinkedHashMap(macroFacts))
    val nominalFacts: Map<Int, CjmpNominalSyntaxFacts> = Collections.unmodifiableMap(LinkedHashMap(nominalFacts))
    val declarationRoleChecks: Map<Int, CjmpDeclarationRoleCheck> = Collections.unmodifiableMap(LinkedHashMap(declarationRoleChecks))
    val roleEvents: List<CjmpParsedRoleEvent> = Collections.unmodifiableList(roleEvents.toList())
    val modifierSources: Map<Int, CjModifierSourceFact> = Collections.unmodifiableMap(LinkedHashMap(modifierSources))
    val annotationSources: Map<Int, CjAnnotationSourceFact> = Collections.unmodifiableMap(LinkedHashMap(annotationSources))
    val declarationPrefixes: Map<Int, CjMacroDeclarationPrefix> = Collections.unmodifiableMap(LinkedHashMap(declarationPrefixes))
    val declarationModifiers: Map<Int, CjParsedDeclarationModifiers> = Collections.unmodifiableMap(LinkedHashMap(declarationModifiers))
    val sourceTokens: Map<Int, CjSourceTokenId> = Collections.unmodifiableMap(LinkedHashMap(sourceTokens))
    val declarationRoleEvents: Map<Int, List<CjmpParsedRoleEvent>> = Collections.unmodifiableMap(
        this.roleEvents.filter { it.kind is CjmpRoleEventKind.Check || it.kind is CjmpRoleEventKind.Extend }
            .groupBy { it.declarationOffset }
            .mapValues { Collections.unmodifiableList(it.value) },
    )
}

internal val PARSER_SEED_KEY: Key<CjParserSeed> = Key.create("CANGJIE_PARSER_SEED")
internal val ROOT_PARSER_SEED_KEY: Key<CjParserSeed> = Key.create("CANGJIE_ROOT_PARSER_SEED")
internal val MACRO_PARSING_FACTS_KEY: Key<MacroParsingFacts> = Key.create("CANGJIE_MACRO_PARSING_FACTS")
internal val CJMP_NOMINAL_SYNTAX_FACTS_KEY: Key<CjmpNominalSyntaxFacts> = Key.create("CANGJIE_CJMP_NOMINAL_SYNTAX_FACTS")
internal val CJMP_DECLARATION_ROLE_CHECK_KEY: Key<CjmpDeclarationRoleCheck> = Key.create("CANGJIE_CJMP_DECLARATION_ROLE_CHECK")
internal val CJMP_DECLARATION_ROLE_EVENTS_KEY: Key<List<CjmpParsedRoleEvent>> = Key.create("CANGJIE_CJMP_DECLARATION_ROLE_EVENTS")
internal val MODIFIER_SOURCE_FACT_KEY: Key<CjModifierSourceFact> = Key.create("CANGJIE_MODIFIER_SOURCE_FACT")
internal val ANNOTATION_SOURCE_FACT_KEY: Key<CjAnnotationSourceFact> = Key.create("CANGJIE_ANNOTATION_SOURCE_FACT")
internal val DECLARATION_PREFIX_KEY: Key<CjMacroDeclarationPrefix> = Key.create("CANGJIE_DECLARATION_PREFIX")
internal val DECLARATION_MODIFIERS_KEY: Key<CjParsedDeclarationModifiers> = Key.create("CANGJIE_DECLARATION_MODIFIERS")
internal val PARSER_SOURCE_SNAPSHOT_KEY: Key<CjParsingSourceSnapshot> = Key.create("CANGJIE_PARSER_SOURCE_SNAPSHOT")
internal val PARSER_SOURCE_ROOT_KEY: Key<UUID> = Key.create("CANGJIE_PARSER_SOURCE_ROOT")
internal val PARSER_SOURCE_TOKEN_KEY: Key<CjSourceTokenId> = Key.create("CANGJIE_PARSER_SOURCE_TOKEN")
internal val PARSER_LEXICAL_DIAGNOSTIC_KEY: Key<CjLexicalDiagnosticOrigin> = Key.create("CANGJIE_PARSER_LEXICAL_DIAGNOSTIC")
internal val PARSER_DIAGNOSTIC_SCOPE_KEY: Key<CjParsingDiagnosticScope> = Key.create("CANGJIE_PARSER_DIAGNOSTIC_SCOPE")
internal val PARSER_NON_ERROR_REPORTS_KEY: Key<List<CjStoredParsingReport>> = Key.create("CANGJIE_PARSER_NON_ERROR_REPORTS")

/** 只读取实际词元的冻结身份，不以当前偏移重新定位另一解析树。 */
fun ASTNode.parsedSourceTokenOrNull(): CjSourceTokenId? = getCopyableUserData(PARSER_SOURCE_TOKEN_KEY)

/** 读取解析器记录的声明角色检查结果；没有来源与未执行检查独立表示。 */
fun ASTNode.cjmpDeclarationRoleCheckOrNull(): CjmpDeclarationRoleCheck? =
    getCopyableUserData(CJMP_DECLARATION_ROLE_CHECK_KEY)

/** 关键字携带其实际声明的返回属性，不查询祖先或另一棵树的偏移。 */
fun ASTNode.parsedDeclarationModifiersOrNull(): CjParsedDeclarationModifiers? =
    getCopyableUserData(DECLARATION_MODIFIERS_KEY)

/** 只穿透同层错误包装；不进入参数列表、正文或任何嵌套声明。 */
fun ASTNode.declarationModifiersForOwnerOrNull(): CjParsedDeclarationModifiers? =
    declarationModifierBindingForOwnerOrNull()?.modifiers

/** 真实 owner 的直接词元载体与其首次发布结果，结构查找与来源验证分开。 */
internal data class CjDeclarationModifierBinding(val carrier: ASTNode, val modifiers: CjParsedDeclarationModifiers)

internal fun ASTNode.declarationModifierBindingForOwnerOrNull(): CjDeclarationModifierBinding? {
    val results = mutableListOf<CjDeclarationModifierBinding>()
    fun collect(owner: ASTNode) {
        var child = owner.firstChildNode
        while (child != null) {
            child.parsedDeclarationModifiersOrNull()?.let { results += CjDeclarationModifierBinding(child, it) }
            if (child.elementType == com.intellij.psi.TokenType.ERROR_ELEMENT) collect(child)
            child = child.treeNext
        }
    }
    collect(this)
    check(results.size <= 1) { "A declaration must have one actual modifier result owner." }
    return results.singleOrNull()
}

/** 仅当关键字事件属于当前文件快照时返回；局部重解析不能复用旧代次事件。 */
fun ASTNode.cjmpDeclarationRoleEventsIn(snapshot: CjmpFileRoleSyntaxSnapshot): List<CjmpParsedRoleEvent>? {
    val events = getCopyableUserData(CJMP_DECLARATION_ROLE_EVENTS_KEY) ?: return null
    return events.takeIf { it.all(snapshot::containsEventIdentity) }
}

/** 完整文件的解析结果随真实 package 节点进入 AST；空文件也具有这个语法节点。 */
internal data class CjParsedFileResult(
    val input: CjParserSeed,
    val languageModuleName: String,
    val fileFacts: ParsingFileFacts,
    val macroFragmentInfo: CjMacroFragmentParseInfo? = null,
    val roleEvents: List<CjmpParsedRoleEvent> = emptyList(),
    /** 与本次解析结果绑定的精确文本，用于识别局部修改后遗留的旧 package 载体。 */
    val sourceText: String,
    val sourceContext: CjParserSourceContext,
    val roleRegion: CjmpParsedRoleRegion? = null,
    /** 本次根 producer 的输出；父区域仅属于 input，不得在发布时反写。 */
    val diagnosticScope: CjParsingDiagnosticScope? = null,
) {
    /** 本次实际输入与调用方原 seed 分开，供同文本重解析及新子根继承。 */
    val actualInput: CjParserSeed
        get() = input.copy(sourceContext = sourceContext, lexicalInput = checkNotNull(roleRegion).lexicalInput)
    /** 指纹仅由本次事务文本派生；复制或替换文本时不能携带上一代的值。 */
    val sourceFingerprint: String by lazy(LazyThreadSafetyMode.PUBLICATION) { cjmpSourceFingerprint(sourceText) }
}

internal val PARSED_FILE_RESULT_KEY: Key<CjParsedFileResult> = Key.create("CANGJIE_PARSED_FILE_RESULT")

/**
 * 已发布的真实文件 parser 输入，供 raw/source 消费方持有不可变来源。
 * 该查询不展开 AST；源解析尚未发布或二进制模型没有输入时明确返回缺失。
 */
fun CjFile.parsingSourceContextOrNull(): CjParserSourceContext? =
    getCopyableUserData(ROOT_PARSER_SEED_KEY)?.sourceContext

/** 读取实际根的不可变调用选项，不从当前 CFIR 模式或语言特性重新推导警告策略。 */
fun CjFile.parsingSettingsOrNull(): CjParserSettings? = getCopyableUserData(ROOT_PARSER_SEED_KEY)?.settings

/** 读取本次实际根的诊断输出，不展开 AST，也不将新根时钟伪装成父区域输入。 */
fun CjFile.parsingDiagnosticScopeOrNull(): CjParsingDiagnosticScope? =
    getCopyableUserData(PARSER_DIAGNOSTIC_SCOPE_KEY)

/** 初次建树成功或实际 AST 修改后发布；构建候选 diff 不得更新原文件。 */
internal fun CjFile.publishParsedFileResult(result: CjParsedFileResult) {
    putCopyableUserData(PUBLISHED_FILE_RESULT_KEY, result)
    putCopyableUserData(PARSER_SOURCE_SNAPSHOT_KEY, result.sourceContext.snapshot)
    val actualInput = result.actualInput
    putCopyableUserData(ROOT_PARSER_SEED_KEY, actualInput)
    publishPsiSourceBasis(actualInput)
    putCopyableUserData(PARSER_DIAGNOSTIC_SCOPE_KEY, checkNotNull(result.diagnosticScope))
    parsedLanguageModuleName = result.languageModuleName
    parsedFileFacts = result.fileFacts
    parsedRoleRegion = checkNotNull(result.roleRegion) { "A file result requires its frozen parser region." }
    parsedRoleSnapshot = CjmpFileRoleSyntaxSnapshot(
        result.input.cjmpContext, result.input.fileRole, CjmpRoleInputCompleteness.PARTIAL, result.roleEvents,
        sourceFingerprint = result.sourceFingerprint,
        lexicalSource = checkNotNull(result.roleRegion).lexicalInput.descriptor,
    )
}

/**
 * collapsed 节点直接携带 seed；已展开的 brace-backed 节点由自己的开括号携带。
 * Lambda 的开括号属于直接 FUNCTION_LITERAL 子节点；只读取这个固定文法路径。
 */
internal fun ASTNode.parserSeedOrNull(): CjParserSeed? {
    getCopyableUserData(PARSER_SEED_KEY)?.let { return it }
    if (elementType != CjNodeTypes.BLOCK && elementType != CjNodeTypes.LAMBDA_EXPRESSION) return null
    val braceOwner = if (elementType == CjNodeTypes.LAMBDA_EXPRESSION) {
        findChildByType(CjNodeTypes.FUNCTION_LITERAL) ?: return null
    } else this
    return braceOwner.findChildByType(CjTokens.LBRACE)?.getCopyableUserData(PARSER_SEED_KEY)
}

internal fun ASTNode.requireParserSeed(): CjParserSeed =
    checkNotNull(parserSeedOrNull()) {
        "The parser root $elementType did not publish its entry seed."
    }

/** 平台先创建未解析的新 chameleon，再调用此钩子；只传不可变入口，不传旧坐标或可变事务。 */
internal fun copyParserSeedForReparse(oldNode: ASTNode, newNode: ASTNode): Boolean {
    newNode.putCopyableUserData(PARSER_SEED_KEY, oldNode.requireParserSeed())
    return true
}
