package org.cangnova.cangjie.parsing

import com.intellij.lang.PsiBuilder
import com.intellij.psi.tree.IElementType
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.CjmpRoleEventKind
import org.cangnova.cangjie.CjSourceKind
import org.cangnova.cangjie.CjmpFileRoleError
import org.cangnova.cangjie.messages.CjParsingDiagnosticOrder
import org.cangnova.cangjie.messages.CjParsingDiagnosticScope
import org.cangnova.cangjie.messages.CjParsingDiagnosticReport
import org.cangnova.cangjie.messages.ParsingDiagnostic
import java.util.UUID
import java.util.Collections

/** 文件输入与解析状态属于同一根事务；包前缀、角色、首个错误和 quote 深度一起撤销。 */
internal data class CjParserFileState(
    val sourceKind: CjSourceKind,
    val cjmpContext: CjmpParsingContext,
    val languageModuleName: String,
    val role: CjmpFileRole = CjmpFileRole.NONE,
    val error: CjPendingFileRoleDiagnostic? = null,
    val quotedSyntaxDepth: Int = 0,
) {
    init {
        require(quotedSyntaxDepth >= 0)
    }
}

/** 文件检查在 producer 处冻结语义和顺序，根末尾仅创建错误节点，不重新分配身份。 */
internal data class CjPendingFileRoleDiagnostic(val roleError: CjmpFileRoleError, val order: CjParsingDiagnosticOrder)

/**
 * 根 token stream 所有 parser 共享的可撤销语法事实事务。
 * registration、后续 scope/逗号 refinement、collapse 删除均记录旧值；不保留池化 marker 身份。
 * 本对象仅在解析期间可变，不能作为 PSI/raw 的发布结果。
 */
class MacroParsingJournal internal constructor() {
    private sealed interface Change {
        data class Facts(val offset: Int, val previous: MacroParsingFacts?) : Change
        data class Nominal(val offset: Int, val previous: CjmpNominalSyntaxFacts?) : Change
        data class RoleCheck(val offset: Int, val previous: CjmpDeclarationRoleCheck?) : Change
        data class RoleEvent(val sequence: Long, val previous: CjmpParsedRoleEvent?) : Change
        data class ModifierSource(val offset: Int, val previous: CjModifierSourceFact?) : Change
        data class AnnotationSource(val offset: Int, val previous: CjAnnotationSourceFact?) : Change
        data class DeclarationPrefix(val offset: Int, val previous: CjMacroDeclarationPrefix?) : Change
        data class DeclarationModifiers(val offset: Int, val previous: CjParsedDeclarationModifiers?) : Change
        data class SourceToken(val offset: Int, val previous: CapturedSourceToken?) : Change
        data class FileState(val previous: CjParserFileState) : Change
        data class Seed(val id: Int, val previous: SeedRecord?) : Change
        data class SyntaxErrorCount(val previous: Long) : Change
        data class DiagnosticSequence(val scope: CjParsingDiagnosticScope, val previous: Long?) : Change
        data class ParsingReport(val order: CjParsingDiagnosticOrder, val previous: CjStoredParsingReport?) : Change
    }

    private data class SeedRecord(val marker: PsiBuilder.Marker, val openingBrace: Int, val seed: CjParserSeed)
    private data class CapturedSourceToken(val type: IElementType, val endOffset: Int)

    private val occurrences = linkedMapOf<Int, MacroParsingFacts>()
    private val nominals = linkedMapOf<Int, CjmpNominalSyntaxFacts>()
    private val roleChecks = linkedMapOf<Int, CjmpDeclarationRoleCheck>()
    // sequence 由实际检查产生，rollback 重新插入多个旧值也必须恢复原检查顺序。
    private val roleEvents = sortedMapOf<Long, CjmpParsedRoleEvent>()
    private val modifierSources = linkedMapOf<Int, CjModifierSourceFact>()
    private val annotationSources = linkedMapOf<Int, CjAnnotationSourceFact>()
    private val declarationPrefixes = linkedMapOf<Int, CjMacroDeclarationPrefix>()
    private val declarationModifiers = linkedMapOf<Int, CjParsedDeclarationModifiers>()
    private val capturedSourceTokens = linkedMapOf<Int, CapturedSourceToken>()
    private val seeds = sortedMapOf<Int, SeedRecord>()
    private val changes = arrayListOf<Change>()
    private val parsingReports = linkedMapOf<CjParsingDiagnosticOrder, CjStoredParsingReport>()
    private var nextSeedId = 0
    private var nextRoleEventSequence = 0L
    internal val rootIdentity: UUID = UUID.randomUUID()
    private var currentSourceContext: CjParserSourceContext? = null
    private var currentLexicalInput: CjLexicalSourceInput? = null
    private var verifySourceInput: (() -> Unit)? = null
    private var inputLength: Int = 0
    internal var markerOffsetOrigin: Int = 0
        private set
    internal val sourceContext: CjParserSourceContext
        get() = checkNotNull(currentSourceContext) { "Source capture requires the actual parser input context." }
    internal val lexicalInput: CjLexicalSourceInput
        get() = checkNotNull(currentLexicalInput) { "Token identity requires the actual frozen lexical input." }
    private var frozen = false
    private var currentFileState: CjParserFileState? = null
    private var rootEntrySeed: CjParserSeed? = null
    /** 诊断与声明角色使用独立时钟；子区域路径在 grammar 进入时分配。 */
    private var entryDiagnosticScope = CjParsingDiagnosticScope(UUID.randomUUID())
    private var currentDiagnosticScope = entryDiagnosticScope
    private val diagnosticSequences = hashMapOf(entryDiagnosticScope to 0L)
    internal val diagnosticScope: CjParsingDiagnosticScope get() = currentDiagnosticScope
    internal val fileState: CjParserFileState
        get() = checkNotNull(currentFileState) { "File state requires a parser root context." }
    internal var parserSettings: CjParserSettings = CjParserSettings()
        private set

    val checkpoint: Int get() = changes.size

    /** 仅实际错误载体完成时递增；warning、角色候选和诊断槽位分配不改变语法破损事实。 */
    internal var syntaxErrorCount: Long = 0
        private set

    internal fun recordSyntaxError(type: IElementType) {
        if (type !is CjParsingErrorElementType) return
        checkOpen()
        changes.add(Change.SyntaxErrorCount(syntaxErrorCount))
        syntaxErrorCount++
    }

    /** 非错误诊断不制造语法节点；报告和词元锚点在 producer 时共同注册并随 marker 撤销。 */
    internal fun reportParsingWarning(anchorOffset: Int, diagnostic: ParsingDiagnostic) {
        checkOpen()
        require(!diagnostic.isError && diagnostic.mainRange == null)
        val order = nextDiagnosticOrder()
        val frozenDiagnostic = diagnostic.copy(
            hints = Collections.unmodifiableList(diagnostic.hints.toList()),
            notes = Collections.unmodifiableList(diagnostic.notes.toList()),
        )
        val hintAnchors = diagnostic.hints.map { hint ->
            if (hint.source != null) null else {
                val offset = checkNotNull(capturedSourceTokens.keys.filter { it <= hint.startOffset }.maxOrNull()) {
                    "A parser hint requires an actual captured token"
                }
                CjStoredParsingHintAnchor(offset, sourceTokenAt(offset))
            }
        }
        val stored = CjStoredParsingReport(anchorOffset, sourceTokenAt(anchorOffset),
            CjParsingDiagnosticReport(frozenDiagnostic, order), Collections.unmodifiableList(hintAnchors))
        check(parsingReports.put(order, stored) == null)
        changes.add(Change.ParsingReport(order, null))
    }

    /** 来源由输入构造层在首次 capture 前安装；截断视图使用原根的上下文与局部坐标。 */
    internal fun initializeSource(
        context: CjParserSourceContext,
        input: CharSequence,
        markerOffsetOrigin: Int = 0,
        lexicalInput: CjLexicalSourceInput = CjLexicalSourceInput(context.snapshot),
        verifyInput: () -> Unit,
    ) {
        checkOpen()
        check(currentSourceContext == null && changes.isEmpty())
        require(input is String) { "A parser builder must own an immutable String input before parsing." }
        context.requireInput(input)
        require(lexicalInput.source === context.snapshot)
        lexicalInput.boundaryAt(context.inputStart)
        lexicalInput.boundaryAt(context.inputEnd)
        currentSourceContext = context
        currentLexicalInput = lexicalInput
        verifySourceInput = verifyInput
        inputLength = input.length
        this.markerOffsetOrigin = markerOffsetOrigin
    }

    internal fun sourceOrigin(start: Int, end: Int): CjParsedSourceOrigin {
        require(start in 0..end && end <= inputLength)
        val context = sourceContext
        return CjParsedSourceOrigin(rootIdentity, CjParsingSourceRange(
            context.snapshot, context.inputStart + start, context.inputStart + end,
        ))
    }

    /** 消费前记录真实 SDK 词元；延后检查与冻结只读取这份可回滚的 capture。 */
    internal fun captureSourceToken(offset: Int, endOffset: Int, type: IElementType) {
        checkOpen()
        require(offset in 0 until endOffset && endOffset <= inputLength)
        val capture = CapturedSourceToken(type, endOffset)
        if (capturedSourceTokens[offset] == capture) return
        changes.add(Change.SourceToken(offset, capturedSourceTokens.put(offset, capture)))
    }

    internal fun sourceTokenAt(offset: Int): CjSourceTokenId {
        val capture = checkNotNull(capturedSourceTokens[offset]) { "A source token identity requires an actual parser capture." }
        val token = lexicalInput.tokenAt(sourceContext.inputStart + offset)
        check(sourceContext.inputStart + capture.endOffset == token.endOffset) {
            "A source token capture must retain its actual lexical extent."
        }
        check(lexicalInput.matchesParserKind(token, capture.type)) {
            "A parser token capture must retain its lexical kind or a legal soft-keyword remap."
        }
        return token.id
    }

    internal fun registerModifierSource(offset: Int, fact: CjModifierSourceFact) {
        checkOpen()
        check(offset !in modifierSources)
        changes.add(Change.ModifierSource(offset, null))
        modifierSources[offset] = fact
    }

    internal fun modifierSourceAt(offset: Int): CjModifierSourceFact? = modifierSources[offset]

    internal fun registerAnnotationSource(offset: Int, fact: CjAnnotationSourceFact) {
        checkOpen()
        check(offset !in annotationSources)
        changes.add(Change.AnnotationSource(offset, null))
        annotationSources[offset] = fact
    }

    internal fun registerDeclarationPrefix(offset: Int, prefix: CjMacroDeclarationPrefix) {
        checkOpen()
        check(offset !in declarationPrefixes)
        changes.add(Change.DeclarationPrefix(offset, null))
        declarationPrefixes[offset] = prefix
    }

    /** 属性在文法检查完成后冻结，不由 raw 消费者重新执行 modifier 规则。 */
    internal fun registerDeclarationModifiers(offset: Int, modifiers: CjParsedDeclarationModifiers) {
        checkOpen()
        check(offset !in declarationModifiers && modifiers.rootIdentity == rootIdentity)
        check(modifiers.sourceToken === sourceTokenAt(offset))
        changes.add(Change.DeclarationModifiers(offset, null))
        declarationModifiers[offset] = modifiers
    }

    /** 子 parser 使用根的当前模块和恒定输入，同一 token stream 不重新安装初始状态。 */
    internal fun initializeFileState(sourceKind: CjSourceKind, languageModuleName: String, context: CjmpParsingContext,
        settings: CjParserSettings = CjParserSettings()) {
        checkOpen()
        val previous = currentFileState
        if (previous != null) {
            check(previous.sourceKind == sourceKind && previous.cjmpContext == context &&
                previous.languageModuleName == languageModuleName && parserSettings == settings) {
                "Child parsers must use their root's source and compilation context."
            }
            return
        }
        currentFileState = CjParserFileState(
            sourceKind, context, languageModuleName,
            role = (context as? CjmpParsingContext.Compilation)?.initialFileRole ?: CjmpFileRole.NONE,
        )
        parserSettings = settings
    }

    /** 独立 lazy/fragment 根在登记语法事实之前安装完整入口状态。 */
    internal fun initializeFileSeed(seed: CjParserSeed) {
        checkOpen()
        val state = fileState
        check(changes.isEmpty()) { "A parser seed must be installed before parsing starts." }
        check(seed.sourceKind == state.sourceKind && seed.cjmpContext == state.cjmpContext &&
            seed.languageModuleName == state.languageModuleName) {
            "A parser seed must describe its actual root input."
        }
        currentFileState = state.copy(role = seed.fileRole, quotedSyntaxDepth = seed.quotedSyntaxDepth)
        rootEntrySeed = seed
        check(parserSettings == seed.settings) { "A parser seed must retain its invocation settings." }
        seed.diagnosticScope?.let { scope ->
            entryDiagnosticScope = scope
            currentDiagnosticScope = scope
            diagnosticSequences.clear()
            diagnosticSequences[scope] = 0L
        }
    }

    /** 仅真实报告点调用；visitor、冻结和源码映射不得分配诊断序号。 */
    internal fun nextDiagnosticOrder(): CjParsingDiagnosticOrder =
        CjParsingDiagnosticOrder(currentDiagnosticScope, nextDiagnosticSequence(currentDiagnosticScope))

    /** seeded block/lambda 复用父槽位；其他正文在消费左括号前保留独立逻辑区域。 */
    internal fun reserveBodyDiagnosticScope(openingBrace: Int): CjParsingDiagnosticScope {
        checkOpen()
        if (openingBrace == 0 && rootEntrySeed?.diagnosticScope != null && rootEntrySeed?.macroFragment == null &&
            currentDiagnosticScope == entryDiagnosticScope
        ) return entryDiagnosticScope
        val child = currentDiagnosticScope.child(nextDiagnosticSequence(currentDiagnosticScope))
        check(child !in diagnosticSequences) { "A diagnostic child region can only be allocated once." }
        changes.add(Change.DiagnosticSequence(child, null))
        diagnosticSequences[child] = 0L
        return child
    }

    internal fun <T> withDiagnosticScope(scope: CjParsingDiagnosticScope, action: () -> T): T {
        checkOpen()
        check(scope in diagnosticSequences && scope.rootIdentity == entryDiagnosticScope.rootIdentity)
        val previous = currentDiagnosticScope
        currentDiagnosticScope = scope
        return try { action() } finally { currentDiagnosticScope = previous }
    }

    private fun nextDiagnosticSequence(scope: CjParsingDiagnosticScope): Long {
        checkOpen()
        val previous = diagnosticSequences.getValue(scope)
        changes.add(Change.DiagnosticSequence(scope, previous))
        diagnosticSequences[scope] = previous + 1
        return previous
    }

    /** 所有字段一起登记旧值；rollback 与角色事件使用同一个 checkpoint。 */
    internal fun updateFileState(next: CjParserFileState) {
        checkOpen()
        val previous = fileState
        check(next.sourceKind == previous.sourceKind && next.cjmpContext == previous.cjmpContext) {
            "The parser root input cannot change during parsing."
        }
        if (next == previous) return
        changes.add(Change.FileState(previous))
        currentFileState = next
    }

    fun register(offset: Int, facts: MacroParsingFacts) {
        checkOpen()
        require(offset >= 0)
        check(offset !in occurrences) { "The same AT occurrence was registered twice without rollback." }
        write(offset, facts)
    }

    fun refine(offset: Int, transform: (MacroParsingFacts) -> MacroParsingFacts) {
        checkOpen()
        val previous = checkNotNull(occurrences[offset]) { "Cannot refine an unregistered AT occurrence." }
        val next = transform(previous)
        if (next != previous) write(offset, next)
    }

    fun factsAt(offset: Int): MacroParsingFacts? = occurrences[offset]

    /** 容器关键字在当前解析根内唯一；记录参与外层 marker 的回滚。 */
    internal fun registerNominal(offset: Int, facts: CjmpNominalSyntaxFacts) {
        checkOpen()
        require(offset >= 0)
        check(offset !in nominals) { "The same nominal declaration was registered twice without rollback." }
        writeNominal(offset, facts)
    }

    /** 以声明起始偏移记录本次检查事实，参与外围 marker 的事务。 */
    internal fun registerDeclarationRoleCheck(offset: Int, result: CjmpDeclarationRoleCheck) {
        checkOpen()
        require(offset >= 0)
        check(offset !in roleChecks) { "The same declaration role check was registered twice without rollback." }
        writeRoleCheck(offset, result)
    }

    internal fun declarationRoleCheckAt(offset: Int): CjmpDeclarationRoleCheck? = roleChecks[offset]

    /** 在实际检查发生时登记，不能使用声明完成后的注册顺序替代检查顺序。 */
    internal fun registerRoleEvent(offset: Int, kind: CjmpRoleEventKind): CjmpParsedRoleEventId {
        checkOpen()
        require(offset >= 0)
        val sequence = nextRoleEventSequence++
        val id = CjmpParsedRoleEventId(rootIdentity, sequence)
        writeRoleEvent(sequence, CjmpParsedRoleEvent(id, offset, kind))
        return id
    }

    /** marker 仅在本次解析准备完成前保存；冻结结果只含最终局部坐标和不可变 seed。 */
    internal fun recordSeed(marker: PsiBuilder.Marker, openingBrace: Int, seed: CjParserSeed) {
        checkOpen()
        require(openingBrace >= 0)
        val id = nextSeedId++
        changes.add(Change.Seed(id, null))
        // 独立展开根保持收到的父槽位；重新捕获其 brace seed 不能抹掉这条真实关联。
        val boundSeed = if (openingBrace == 0 && rootEntrySeed?.roleRegionSlot != null) {
            seed.copy(roleRegionSlot = checkNotNull(rootEntrySeed).roleRegionSlot)
        } else seed
        seeds[id] = SeedRecord(marker, openingBrace, boundSeed)
    }

    /** 折叠的子树不进入本根；删除同样可被外层 marker 回滚撤销。 */
    fun discardCollapsedChildren(start: Int, end: Int) {
        checkOpen()
        require(start <= end)
        val discarded = occurrences.keys.filter { it >= start && it < end }
        for (offset in discarded) write(offset, null)
        for (offset in nominals.keys.filter { it >= start && it < end }) writeNominal(offset, null)
        for (offset in roleChecks.keys.filter { it >= start && it < end }) writeRoleCheck(offset, null)
        for (offset in modifierSources.keys.filter { it >= start && it < end }) {
            changes.add(Change.ModifierSource(offset, modifierSources.remove(offset)))
        }
        for (offset in annotationSources.keys.filter { it >= start && it < end }) {
            changes.add(Change.AnnotationSource(offset, annotationSources.remove(offset)))
        }
        for (offset in declarationPrefixes.keys.filter { it >= start && it < end }) {
            changes.add(Change.DeclarationPrefix(offset, declarationPrefixes.remove(offset)))
        }
        for (offset in declarationModifiers.keys.filter { it >= start && it < end }) {
            changes.add(Change.DeclarationModifiers(offset, declarationModifiers.remove(offset)))
        }
        for ((order, report) in parsingReports.toMap()) {
            if (report.anchorOffset in start until end) {
                changes.add(Change.ParsingReport(order, parsingReports.remove(order)))
            }
        }
        val regionEvents = roleEvents.values.filter { it.declarationOffset in start until end }
        val slot = regionEvents.lastOrNull { it.declarationOffset == start && it.kind == CjmpRoleEventKind.Unknown }
        for (event in regionEvents) {
            if (event !== slot) writeRoleEvent(event.id.sequence, null)
        }
        // 没有声明 parser seed 的通用 collapse 仍保留一次实际未知观察，不复用已删除声明的身份。
        if (slot == null && regionEvents.isNotEmpty()) registerRoleEvent(start, CjmpRoleEventKind.Unknown)
        for ((id, record) in seeds.toMap()) {
            if (record.openingBrace in start until end) {
                changes.add(Change.Seed(id, record))
                seeds.remove(id)
            }
        }
    }

    fun rollback(checkpoint: Int) {
        checkOpen()
        require(checkpoint in 0..changes.size)
        for (index in changes.lastIndex downTo checkpoint) {
            when (val change = changes.removeAt(index)) {
                is Change.Facts -> if (change.previous == null) occurrences.remove(change.offset)
                    else occurrences[change.offset] = change.previous
                is Change.Nominal -> if (change.previous == null) nominals.remove(change.offset)
                    else nominals[change.offset] = change.previous
                is Change.RoleCheck -> if (change.previous == null) roleChecks.remove(change.offset)
                    else roleChecks[change.offset] = change.previous
                is Change.RoleEvent -> if (change.previous == null) roleEvents.remove(change.sequence)
                    else roleEvents[change.sequence] = change.previous
                is Change.ModifierSource -> if (change.previous == null) modifierSources.remove(change.offset)
                    else modifierSources[change.offset] = change.previous
                is Change.AnnotationSource -> if (change.previous == null) annotationSources.remove(change.offset)
                    else annotationSources[change.offset] = change.previous
                is Change.DeclarationPrefix -> if (change.previous == null) declarationPrefixes.remove(change.offset)
                    else declarationPrefixes[change.offset] = change.previous
                is Change.DeclarationModifiers -> if (change.previous == null) declarationModifiers.remove(change.offset)
                    else declarationModifiers[change.offset] = change.previous
                is Change.SourceToken -> if (change.previous == null) capturedSourceTokens.remove(change.offset)
                    else capturedSourceTokens[change.offset] = change.previous
                is Change.SyntaxErrorCount -> syntaxErrorCount = change.previous
                is Change.FileState -> currentFileState = change.previous
                is Change.Seed -> if (change.previous == null) seeds.remove(change.id)
                    else seeds[change.id] = change.previous
                is Change.DiagnosticSequence -> if (change.previous == null) diagnosticSequences.remove(change.scope)
                    else diagnosticSequences[change.scope] = change.previous
                is Change.ParsingReport -> if (change.previous == null) parsingReports.remove(change.order)
                    else parsingReports[change.order] = change.previous
            }
        }
    }

    /** 必须在底层 lightTree 完成边界平衡后调用；Production 范围此时才是最终范围。 */
    internal fun freeze(offsetOrigin: Int): CjPreparedParsingData {
        checkOpen()
        if (currentSourceContext != null) checkNotNull(verifySourceInput).invoke()
        val tokens = linkedMapOf<CjSeedToken, CjParserSeed>()
        val roots = linkedMapOf<CjSeedToken, CjParserSeed>()
        val slotSeeds = hashMapOf<CjmpParsedRoleEventId, CjParserSeed>()
        for (record in seeds.values) {
            val marker = record.marker
            val start = marker.startOffset - offsetOrigin
            val end = marker.endOffset - offsetOrigin
            check(start in 0..end && end <= inputLength) { "A parser child slot must remain inside its actual input interval." }
            check(record.openingBrace in start until end) { "The prepared syntax range does not contain its recorded opening brace." }
            val root = CjSeedToken(marker.tokenType, start, end)
            val seed = record.seed.copy(sourceContext = currentSourceContext?.let {
                CjParserSourceContext(it.snapshot, it.inputStart + start, it.inputStart + end)
            })
            check(roots.put(root, seed) == null) { "A syntax root was registered twice." }
            val token = if (marker.isCollapsed) root
                else CjSeedToken(CjTokens.LBRACE, record.openingBrace, record.openingBrace + 1)
            check(tokens.put(token, seed) == null) { "A syntax seed carrier was registered twice." }
            seed.roleRegionSlot?.takeIf { it.rootIdentity == rootIdentity }?.let { slot ->
                check(slotSeeds.put(slot, seed) == null) { "A lazy parser slot has more than one actual carrier." }
            }
        }
        val orderedSlots = linkedMapOf<CjmpParsedRoleEventId, CjParserSeed>()
        for (event in roleEvents.values) slotSeeds.remove(event.id)?.let { orderedSlots[event.id] = it }
        check(slotSeeds.isEmpty()) { "A lazy parser seed has no surviving role slot." }
        val region = currentFileState?.let { state ->
            CjmpParsedRoleRegion(rootIdentity, rootEntrySeed, state.cjmpContext,
                rootEntrySeed?.fileRole ?: (state.cjmpContext as? CjmpParsingContext.Compilation)?.initialFileRole ?: CjmpFileRole.NONE,
                sourceContext, roleEvents.values.toList(), orderedSlots, lexicalInput, declarationModifiers)
        }
        val result = CjPreparedParsingData(tokens, roots, occurrences, nominals, roleChecks, roleEvents.values.toList(),
            modifierSources, annotationSources, declarationPrefixes, declarationModifiers, region, currentLexicalInput,
            (occurrences.keys + nominals.keys + declarationModifiers.keys + parsingReports.values.map { it.anchorOffset } +
                parsingReports.values.flatMap { report -> report.hintAnchors.mapNotNull { it?.offset } } +
                capturedSourceTokens.filterValues { it.type == CjTokens.UNKNOWN_TOKEN_START || it.type == com.intellij.psi.TokenType.BAD_CHARACTER }.keys)
                .associateWith(::sourceTokenAt),
            entryDiagnosticScope, parsingReports.values.groupBy { it.anchorOffset })
        frozen = true
        seeds.clear()
        changes.clear()
        capturedSourceTokens.clear()
        diagnosticSequences.clear()
        return result
    }

    private fun checkOpen() = check(!frozen) { "A published parsing transaction cannot be changed." }

    private fun write(offset: Int, facts: MacroParsingFacts?) {
        changes.add(Change.Facts(offset, occurrences[offset]))
        if (facts == null) occurrences.remove(offset) else occurrences[offset] = facts
    }

    private fun writeNominal(offset: Int, facts: CjmpNominalSyntaxFacts?) {
        changes.add(Change.Nominal(offset, nominals[offset]))
        if (facts == null) nominals.remove(offset) else nominals[offset] = facts
    }

    private fun writeRoleCheck(offset: Int, result: CjmpDeclarationRoleCheck?) {
        changes.add(Change.RoleCheck(offset, roleChecks[offset]))
        if (result == null) roleChecks.remove(offset) else roleChecks[offset] = result
    }

    private fun writeRoleEvent(sequence: Long, event: CjmpParsedRoleEvent?) {
        changes.add(Change.RoleEvent(sequence, roleEvents[sequence]))
        if (event == null) roleEvents.remove(sequence) else roleEvents[sequence] = event
    }
}
