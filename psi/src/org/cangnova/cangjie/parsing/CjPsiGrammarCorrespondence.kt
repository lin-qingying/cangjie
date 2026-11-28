package org.cangnova.cangjie.parsing

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.cangnova.cangjie.lexer.CjSingleValueToken
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNodeTypes
import java.util.Collections
import java.util.IdentityHashMap
import java.util.UUID

/**
 * 两个真实 parser 图的语法对应。匹配只能在已经匹配的实际父节点下发生；
 * 声明头和正文中的每个子节点分别验证，不把相同词元身份升级为声明或签名等价。
 */
internal class CjPsiGrammarCorrespondence(
    origin: CjFile,
    target: CjFile,
    context: CjParserSourceContext,
    lexicalInput: CjLexicalSourceInput,
    originSourceView: CjPsiSourceView,
) {
    private val targets = IdentityHashMap<ASTNode, ASTNode>()
    private val origins = IdentityHashMap<ASTNode, ASTNode>()

    init {
        val source = GrammarCapture(origin, context, lexicalInput, GrammarTreeRole.SOURCE, originSourceView).capture()
        val destination = GrammarCapture(target, context, lexicalInput, GrammarTreeRole.COMPILED, originSourceView).capture()
        check(source.span == destination.span && source.kind == destination.kind)
        match(source, destination)
    }

    fun targetFor(element: PsiElement): PsiElement? = targets[element.node]?.psi
    fun originFor(element: PsiElement): PsiElement? = origins[element.node]?.psi

    fun captureOriginalElements(): Map<PsiElement, PsiElement> = Collections.unmodifiableMap(
        IdentityHashMap<PsiElement, PsiElement>().apply {
            origins.forEach { (target, origin) -> put(target.psi, origin.psi) }
        },
    )

    private fun match(source: GrammarNode, target: GrammarNode) {
        check(source.kind == target.kind && source.span == target.span)
        if (source.psiClass != target.psiClass) return
        targets[source.node] = target.node
        origins[target.node] = source.node
        val sources = source.semanticChildren.groupBy { it.signature }
        val destinations = target.semanticChildren.groupBy { it.signature }
        for ((signature, candidates) in sources) {
            val successors = destinations[signature] ?: continue
            if (candidates.size == 1 && successors.size == 1) match(candidates.single(), successors.single())
        }
    }
}

private data class TokenPosition(val ordinal: Int, val codeUnit: Int)
private data class TokenSpan(val start: TokenPosition, val end: TokenPosition)
private data class OwnerGrammar(
    val token: CjSourceTokenId,
    val kind: CjmpModifierDeclarationKind,
    val scope: CjScopeKind,
    val parameterProjection: CjVariadicParameterRules.Result?,
)
private data class GrammarSignature(
    val kind: IElementType,
    val span: TokenSpan,
    val owner: OwnerGrammar?,
    val children: List<GrammarSignature>,
)

private class GrammarNode(
    val node: ASTNode,
    val kind: IElementType,
    rawSpan: TokenSpan,
    val owner: OwnerGrammar?,
    children: List<GrammarNode>,
) {
    val psiClass: Class<out PsiElement> = node.psi.javaClass
    private val errorWrapper = node.psi is PsiErrorElement
    val semanticChildren: List<GrammarNode> = children.flatMap { child ->
        if (child.errorWrapper && child.semanticChildren.isNotEmpty()) child.semanticChildren
        else if (child.errorWrapper && child.span.start == child.span.end) emptyList()
        else if (child.kind in CjTokens.ALL_WHITESPACE_TOKENS || child.kind in CjTokens.COMMENTS) emptyList()
        else listOf(child)
    }
    /**
     * 逐叶输入验证仍消费全部 trivia。签名区间由本树实际非空语法子节点确定，
     * 零宽诊断前的空白在 SDK 中改变父归属时，不改变声明头或正文的语法身份。
     */
    val span: TokenSpan = semanticChildren.filter { it.span.start != it.span.end }.let { actual ->
        if (actual.isEmpty()) rawSpan else TokenSpan(actual.first().span.start, actual.last().span.end)
    }
    val signature: GrammarSignature = GrammarSignature(
        // 来源块可由独立 factory/fragment 生产后插入另一真实父节点；历史 entry context 不成为
        // 当前文法身份。两树仍在已匹配父节点下逐叶核验，owner/projection 保持；目标语义由完整新产品证明。
        kind, span, owner,
        if (kind in BODY_CONTAINERS) emptyList() else semanticChildren.map { it.signature },
    )
}

/** 词元位置来自实际 AST 叶的顺序消费；它是本树的来源校验，不是另一树的 owner 查找表。 */
private class GrammarCapture(
    private val file: CjFile,
    private val context: CjParserSourceContext,
    private val input: CjLexicalSourceInput,
    private val role: GrammarTreeRole,
    private val sourceView: CjPsiSourceView,
) {
    private var cursor = context.inputStart
    private var ordinal = input.boundaryAt(context.inputStart)
    private val starts = IdentityHashMap<ASTNode, TokenPosition>()
    private val sourceRegions: Map<UUID, CjmpParsedRoleRegion> =
        if (role == GrammarTreeRole.SOURCE) collectSourceRegions() else emptyMap()

    fun capture(): GrammarNode {
        sourceView.requireCurrent()
        context.requireInput(file.node.chars)
        if (role == GrammarTreeRole.COMPILED) check(file.parsedRoleRegion?.lexicalInput === input)
        else check(sourceView.lexicalInput === input && sourceView.sourceContext == context)
        val result = node(file.node)
        check(cursor == context.inputEnd && ordinal == input.boundaryAt(context.inputEnd))
        sourceView.requireCurrent()
        return result
    }

    private fun position(): TokenPosition {
        while (ordinal < input.tokens.size && input.tokens[ordinal].endOffset <= cursor) ordinal++
        val offset = if (ordinal == input.tokens.size) 0 else cursor - input.tokens[ordinal].startOffset
        check(offset >= 0)
        return TokenPosition(ordinal, offset)
    }

    private fun node(actual: ASTNode): GrammarNode {
        val start = position()
        starts[actual] = start
        val beginning = cursor
        val children = mutableListOf<GrammarNode>()
        var child = actual.firstChildNode
        if (child == null) {
            val text = actual.chars.toString()
            check(cursor + text.length <= context.inputEnd &&
                input.source.text.regionMatches(cursor, text, 0, text.length)) {
                "A grammar leaf must consume its actual frozen source text."
            }
            cursor += text.length
        } else {
            while (child != null) {
                children += node(child)
                child = child.treeNext
            }
        }
        val end = position()
        check(cursor - beginning == actual.textLength)
        if (children.isEmpty() && actual.textLength != 0) requireLeafKind(actual, start, end)
        val binding = actual.declarationModifierBindingForOwnerOrNull()
        val owner = binding?.let { binding ->
            val it = binding.modifiers
            if (role == GrammarTreeRole.COMPILED) check(actual.declarationModifiersFromActualRegion(file) === it)
            else requireSourceModifierProducer(binding)
            val parameterLists = children.filter { child -> child.kind == CjNodeTypes.VALUE_PARAMETER_LIST }
            val parameters = parameterLists.flatMap { list ->
                val owner = list.node
                generateSequence(owner.firstChildNode) { child -> child.treeNext }
                    .filter { child -> child.elementType == CjNodeTypes.VALUE_PARAMETER }
                    .map { child -> child.findChildByType(CjTokens.ELLIPSIS) != null }.toList()
            }
            val projection = if (parameterLists.isEmpty()) null else CjVariadicParameterRules.select(
                it.declarationKind, parameterLists.size, it.hasModifier(CjModifierKind.FOREIGN), parameters,
            )
            // 两棵真实树都消费本代冻结输入；历史keyword origin只证明源载体，不成为新输入的词元ID。
            val carrierStart = checkNotNull(starts[binding.carrier])
            check(carrierStart.codeUnit == 0)
            val token = input.tokens[carrierStart.ordinal]
            check(input.matchesParserKind(token, binding.carrier.elementType))
            OwnerGrammar(token.id, it.declarationKind, it.scope, projection)
        }
        return GrammarNode(actual, actual.elementType, TokenSpan(start, end), owner, children)
    }

    /**
     * 来源树可以是混合代次；仅按真实native载体的producer身份收集来源证明。
     * 历史文件producer和已展开子根均保留原对象，不重放角色、不构造新的完整文件结果。
     */
    private fun collectSourceRegions(): Map<UUID, CjmpParsedRoleRegion> {
        val regions = linkedMapOf<UUID, CjmpParsedRoleRegion>()
        fun register(region: CjmpParsedRoleRegion) {
            val previous = regions.putIfAbsent(region.rootIdentity, region)
            check(previous == null || previous === region) { "One native producer identity must retain one frozen region." }
        }
        register(checkNotNull(file.getCopyableUserData(PUBLISHED_FILE_RESULT_KEY)?.roleRegion))
        fun visit(node: ASTNode) {
            node.getCopyableUserData(PARSED_ROLE_REGION_KEY)?.let(::register)
            var child = node.firstChildNode
            while (child != null) {
                visit(child)
                child = child.treeNext
            }
        }
        visit(file.node)
        return regions
    }

    /** 旧来源属性只按原producer、原词元对象和真实载体拼写验证，不授予当前编译语境的属性。 */
    private fun requireSourceModifierProducer(binding: CjDeclarationModifierBinding) {
        val region = checkNotNull(sourceRegions[binding.carrier.getCopyableUserData(PARSER_SOURCE_ROOT_KEY)])
        val modifiers = binding.modifiers
        check(region.containsDeclarationModifiers(modifiers))
        val tokenId = checkNotNull(binding.carrier.parsedSourceTokenOrNull())
        check(tokenId === modifiers.sourceToken && region.lexicalInput.contains(tokenId))
        val token = region.lexicalInput.tokens[tokenId.ordinal]
        check(token.id === tokenId && region.lexicalInput.matchesParserKind(token, binding.carrier.elementType))
        check(region.lexicalInput.source.text.substring(token.startOffset, token.endOffset) == binding.carrier.chars.toString())
    }

    private fun requireLeafKind(node: ASTNode, start: TokenPosition, end: TokenPosition) {
        val last = end.ordinal + if (end.codeUnit == 0) 0 else 1
        val tokens = input.tokens.subList(start.ordinal, last)
        val kind = node.elementType
        if (kind == TokenType.WHITE_SPACE || kind == CjTokens.NL) {
            check(tokens.all { it.type in CjTokens.ALL_WHITESPACE_TOKENS })
        } else if (kind in CjTokens.COMMENTS) {
            check(tokens.all { it.type in CjTokens.COMMENTS })
        } else if (kind == CjTokens.IDENTIFIER) {
            check(tokens.size == 1 && input.matchesParserKind(tokens.single(), kind))
        } else if (tokens.size == 1 && input.matchesParserKind(tokens.single(), kind)) {
            return
        } else if (kind is CjSingleValueToken) {
            check(node.chars.toString() == kind.value)
        }
        // 原生文法也会折叠多词元的 grammar 节点；类型与完整子结构继续参与两树匹配。
    }
}

/** 来源语法用于导航证明；目标语法必须由本代完整parser产品证明。 */
private enum class GrammarTreeRole { SOURCE, COMPILED }

/** 这些真实文法容器的头部归其实际父 owner；内部声明/表达式仍逐个完整匹配。 */
private val BODY_CONTAINERS: Set<IElementType> = setOf(
    CjNodeTypes.CLASS_BODY, CjNodeTypes.INTERFACE_BODY, CjNodeTypes.ENUM_BODY,
    CjNodeTypes.PROPERTY_BODY, CjNodeTypes.FOREIGN_BODY, CjNodeTypes.BLOCK, CjNodeTypes.CASE_BLOCK,
)
