package org.cangnova.cangjie.parsing

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.Key
import com.intellij.psi.impl.source.tree.LazyParseableElement
import org.cangnova.cangjie.CjmpRoleEventKind
import org.cangnova.cangjie.CjmpRoleInputCompleteness
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNodeTypes
import java.util.Collections
import java.util.UUID

/**
 * 一个实际 parser 根的冻结事件及其惰性子槽位。槽位使用父事务中的真实 Unknown 事件身份，
 * 独立子 parser 保留收到的入口 seed；偏移只校验源码切片，不能用于选择或替换槽位。
 */
internal class CjmpParsedRoleRegion(
    val rootIdentity: UUID,
    val entrySeed: CjParserSeed?,
    val parsingContext: CjmpParsingContext,
    val entryFileRole: CjmpFileRole,
    val sourceContext: CjParserSourceContext,
    events: List<CjmpParsedRoleEvent>,
    slots: Map<CjmpParsedRoleEventId, CjParserSeed>,
    val lexicalInput: CjLexicalSourceInput,
    declarationModifiers: Map<Int, CjParsedDeclarationModifiers>,
) {
    val events: List<CjmpParsedRoleEvent> = Collections.unmodifiableList(events.toList())
    val slots: Map<CjmpParsedRoleEventId, CjParserSeed> = Collections.unmodifiableMap(LinkedHashMap(slots))
    private val declarationsByToken = Collections.unmodifiableMap(declarationModifiers.values.associateBy { it.sourceToken })

    /** 同值副本不是本根 producer 发布的声明结果。 */
    internal fun containsDeclarationModifiers(modifiers: CjParsedDeclarationModifiers): Boolean =
        declarationsByToken[modifiers.sourceToken] === modifiers

    init {
        require(lexicalInput.source === sourceContext.snapshot)
        require(declarationsByToken.size == declarationModifiers.size)
        require(declarationsByToken.values.all {
            it.rootIdentity == rootIdentity && it.parsingContext == parsingContext && lexicalInput.contains(it.sourceToken)
        })
        require(this.events.all { it.id.rootIdentity == rootIdentity })
        require(this.events.map { it.id }.toSet().size == this.events.size)
        val unknownIds = this.events.filter { it.kind == CjmpRoleEventKind.Unknown }.mapTo(hashSetOf()) { it.id }
        require(this.slots.all { (id, seed) ->
            id in unknownIds && seed.roleRegionSlot == id && seed.cjmpContext == parsingContext &&
                seed.sourceContext?.snapshot === sourceContext.snapshot && seed.lexicalInput === lexicalInput &&
                checkNotNull(seed.sourceContext).inputStart >= sourceContext.inputStart &&
                checkNotNull(seed.sourceContext).inputEnd <= sourceContext.inputEnd
        })
        require(entrySeed?.roleRegionSlot?.rootIdentity != rootIdentity)
    }
}

internal val PARSED_ROLE_REGION_KEY: Key<CjmpParsedRoleRegion> = Key.create("CANGJIE_PARSED_ROLE_REGION")
private val FILE_ROLE_REGION_KEY: Key<CjmpParsedRoleRegion> = Key.create("CANGJIE_FILE_ROLE_REGION")

/** 文件和它的 AST 副本各自发布快照，冻结的 parser 根本身不被子解析修改。 */
internal var CjFile.parsedRoleRegion: CjmpParsedRoleRegion?
    get() = getCopyableUserData(FILE_ROLE_REGION_KEY)
    set(value) { putCopyableUserData(FILE_ROLE_REGION_KEY, value) }

/** 独立 block/lambda 根的结果只从该根自己的开括号读取，不加载未展开的 chameleon。 */
private fun ASTNode.parsedRoleRegionOrNull(): CjmpParsedRoleRegion? {
    if (this is LazyParseableElement && !isParsed) return null
    val braceOwner = when (elementType) {
        CjNodeTypes.BLOCK -> this
        CjNodeTypes.LAMBDA_EXPRESSION -> findChildByType(CjNodeTypes.FUNCTION_LITERAL) ?: return null
        else -> return null
    }
    return braceOwner.findChildByType(CjTokens.LBRACE)?.getCopyableUserData(PARSED_ROLE_REGION_KEY)
}

/**
 * 发布 source stub 前沿实际祖先寻找所属独立 parser 根，验证真实载体及该根首次结果的对象身份。
 * 范围只校验此树自己的输入位置，不用于从另一棵树选择 owner。
 */
internal fun ASTNode.declarationModifiersFromActualRegion(file: CjFile): CjParsedDeclarationModifiers? {
    val binding = declarationModifierBindingForOwnerOrNull() ?: return null
    val fileRegion = checkNotNull(file.parsedRoleRegion) { "A source declaration requires its actual file parser region." }
    val fileNode = file.node
    var regionOwner = this
    var region = regionOwner.parsedRoleRegionOrNull()
    while (region == null && regionOwner !== fileNode) {
        regionOwner = checkNotNull(regionOwner.treeParent) { "The declaration owner does not belong to its actual source file." }
        region = if (regionOwner === fileNode) fileRegion else regionOwner.parsedRoleRegionOrNull()
    }
    val actual = checkNotNull(region)
    val modifiers = binding.modifiers
    check(actual.lexicalInput === fileRegion.lexicalInput)
    check(actual.containsDeclarationModifiers(modifiers)) { "A declaration carrier must retain its actual parser result." }
    check(binding.carrier.getCopyableUserData(PARSER_SOURCE_ROOT_KEY) == actual.rootIdentity)
    val tokenId = checkNotNull(binding.carrier.parsedSourceTokenOrNull())
    check(tokenId === modifiers.sourceToken && actual.lexicalInput.contains(tokenId))
    actual.sourceContext.requireInput(regionOwner.chars)
    val token = actual.lexicalInput.tokens[tokenId.ordinal]
    val carrierStart = actual.sourceContext.inputStart + binding.carrier.startOffset - regionOwner.startOffset
    check(carrierStart == token.startOffset && carrierStart + binding.carrier.textLength == token.endOffset)
    return modifiers
}

/**
 * 只收集已提交、已展开的真实 AST。子结果须由实际父子结构、入口 seed 身份和同份源码快照
 * 共同证明属于父槽位；缺少绑定时保留 Unknown，不授予完整文件结论，也不展开任何正文。
 */
fun CjFile.collectParsedRoleSnapshot(): CjmpFileRoleSyntaxSnapshot? {
    val current = parsedRoleSnapshot ?: return null
    val rootRegion = parsedRoleRegion ?: return null
    if (current.matchesSourceContent(viewProvider.contents) != true) return null

    fun collect(region: CjmpParsedRoleRegion, owner: ASTNode): List<CjmpParsedRoleEvent> {
        val bindings = mutableListOf<Pair<CjmpParsedRoleEventId, ASTNode>>()
        val encountered = mutableListOf<CjmpParsedRoleEventId>()
        fun visit(node: ASTNode) {
            val unparsed = node is LazyParseableElement && !node.isParsed
            val slot = (if (unparsed) node.getCopyableUserData(PARSER_SEED_KEY) else node.parserSeedOrNull())?.roleRegionSlot
            if (slot in region.slots) {
                val id = checkNotNull(slot)
                encountered += id
                bindings += id to node
                return
            }
            if (unparsed) return
            var child = node.firstChildNode
            while (child != null) {
                visit(child)
                child = child.treeNext
            }
        }
        var child = owner.firstChildNode
        while (child != null) {
            visit(child)
            child = child.treeNext
        }
        // 使用真实树中的槽位顺序验证结构；排序偏移或选择第一个重名节点都不构成身份证明。
        if (encountered != region.slots.keys.toList()) return region.events
        val provenBindings = bindings.toMap()
        return buildList {
            for (event in region.events) {
                val seed = region.slots[event.id]
                if (seed == null) {
                    add(event)
                    continue
                }
                val node = provenBindings.getValue(event.id)
                val expanded = node.parsedRoleRegionOrNull()
                if (expanded == null || expanded.entrySeed !== seed ||
                    expanded.parsingContext != region.parsingContext ||
                    expanded.sourceContext != seed.sourceContext || expanded.lexicalInput !== region.lexicalInput
                ) {
                    add(event)
                } else {
                    addAll(collect(expanded, node))
                }
            }
        }
    }

    val events = collect(rootRegion, node)
    val completeness = if (events.any { it.kind == CjmpRoleEventKind.Unknown }) CjmpRoleInputCompleteness.PARTIAL
        else CjmpRoleInputCompleteness.COMPLETE
    if (current.completeness == completeness && current.events.size == events.size &&
        current.events.zip(events).all { (old, fresh) -> old === fresh }
    ) return current
    return CjmpFileRoleSyntaxSnapshot(
        rootRegion.parsingContext, rootRegion.entryFileRole, completeness, events,
        sourceFingerprint = current.sourceFingerprint,
        lexicalSource = rootRegion.lexicalInput.descriptor,
    ).also { parsedRoleSnapshot = it }
}
