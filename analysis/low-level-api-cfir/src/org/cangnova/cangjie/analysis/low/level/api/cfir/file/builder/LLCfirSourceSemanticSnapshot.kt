package org.cangnova.cangjie.analysis.low.level.api.cfir.file.builder

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import org.cangnova.cangjie.CjmpFileRoleError
import org.cangnova.cangjie.CjmpFileRoleMode
import org.cangnova.cangjie.CjmpRoleEventKind
import org.cangnova.cangjie.CjmpRoleEventOutcome
import org.cangnova.cangjie.CjmpRoleInputCompleteness
import org.cangnova.cangjie.CjmpRoleSyntaxIdentity
import org.cangnova.cangjie.LanguageVersionSettings
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.parsing.*
import org.cangnova.cangjie.psi.*
import java.util.IdentityHashMap

/**
 * 完整目标 parser 产品的保留判据。Kotlin 的原树不换代；仓颉的 body 角色事件能改变后继声明，
 * 因此 LL 在换代前同时比较真实文法、header owner、返回属性和按 parser 调用顺序执行的角色结果。
 * 此快照在产品有效时冻结，修改后不从旧 correspondence 再认领 owner。
 */
internal class LLCfirSourceSemanticSnapshot(
    product: ParsedCangJiePsiFile,
    origins: Map<PsiElement, PsiElement>,
    languageVersionSettings: LanguageVersionSettings,
) {
    private data class Modifiers(
        val attributes: Set<CjModifierKind>,
        val hasConst: Boolean,
        val kind: CjmpModifierDeclarationKind,
        val scope: CjScopeKind,
    )

    private data class Grammar(
        val kind: IElementType,
        val psiClass: Class<out PsiElement>,
        val leaf: String?,
        val error: String?,
        val owner: PsiElement?,
        val modifiers: Modifiers?,
        val roleCheck: CjmpDeclarationRoleCheck?,
        val nominal: CjmpNominalSyntaxFacts?,
        val bodyOwner: CjElement?,
        val children: List<Grammar>,
    )

    private data class RoleCarrier(val origin: PsiElement?, val bodyOwner: CjElement?)
    private data class RoleResult(
        val carrier: RoleCarrier,
        val kind: CjmpRoleEventKind,
        val before: CjmpFileRole?,
        val after: CjmpFileRole?,
        val outcome: CjmpRoleEventOutcome,
        val error: CjmpFileRoleError?,
    )

    private val input = product.actualInputSeed.let {
        listOf(it.sourceKind, it.languageModuleName, it.cjmpContext, it.fileRole, it.quotedSyntaxDepth, it.context, it.macroFragment)
    }
    private val bodies = IdentityHashMap<CjElement, Grammar>()
    private val bodyOrigins = IdentityHashMap<CjElement, PsiElement>()
    private val header: Grammar
    private val roles: List<RoleResult>
    private val complete: Boolean
    private val fileRole: CjmpFileRole?

    init {
        val file = product.file
        val snapshot = checkNotNull(file.collectParsedRoleSnapshot()) { "A target PSI file requires its actual role snapshot." }
        val carriers = mutableMapOf<CjmpParsedRoleEventId, RoleCarrier>()
        val boundaries = IdentityHashMap<ASTNode, CjElement>()

        fun registerBodies(node: ASTNode) {
            val psi = node.psi
            val body = when (psi) {
                is CjNamedFunction -> psi.bodyExpression
                is CjPropertyAccessor -> psi.bodyExpression
                is CjProperty -> psi.initializer
                else -> null
            }
            if (body != null) {
                val originalOwner = origins[psi] as? CjElement
                if (originalOwner != null) {
                    boundaries[body.node] = originalOwner
                    origins[body]?.let { bodyOrigins[originalOwner] = it }
                }
            }
            var child = node.firstChildNode
            while (child != null) {
                registerBodies(child)
                child = child.treeNext
            }
        }
        registerBodies(file.node)

        fun capture(node: ASTNode, bodyOwner: CjElement?, useBoundaries: Boolean): Grammar {
            val boundary = if (useBoundaries) boundaries[node] else null
            if (boundary != null) {
                bodies[boundary] = capture(node, boundary, useBoundaries = false)
                return Grammar(node.elementType, node.psi.javaClass, null, null, null, null, null, null, boundary, emptyList())
            }
            node.cjmpDeclarationRoleEventsIn(snapshot)?.forEach { event ->
                check(carriers.put(event.id, RoleCarrier(origins[node.psi], bodyOwner)) == null) {
                    "One parser role event must retain one actual syntax carrier."
                }
            }
            val children = buildList {
                var child = node.firstChildNode
                while (child != null) {
                    if (child.elementType !in CjTokens.ALL_WHITESPACE_TOKENS && child.elementType !in CjTokens.COMMENTS) {
                        add(capture(child, bodyOwner, useBoundaries))
                    }
                    child = child.treeNext
                }
            }
            val psi = node.psi
            val modifiers = node.parsedDeclarationModifiersOrNull()?.let {
                Modifiers(it.attributes, it.hasConstModifier, it.declarationKind, it.scope)
            }
            return Grammar(
                if (psi is PsiErrorElement) TokenType.ERROR_ELEMENT else node.elementType,
                psi.javaClass,
                if (node.firstChildNode == null && psi !is PsiErrorElement) node.chars.toString() else null,
                (psi as? PsiErrorElement)?.errorDescription,
                if (bodyOwner == null && psi is CjDeclaration) origins[psi] else null,
                modifiers, node.cjmpDeclarationRoleCheckOrNull(), node.cjmpNominalSyntaxFactsOrNull(), null, children,
            )
        }
        header = capture(file.node, null, useBoundaries = true)
        val context = product.actualInputSeed.cjmpContext as CjmpParsingContext.Compilation
        val replay = CjmpRoleSnapshotReplay.replay(
            snapshot,
            CjmpFileRoleMode.Compilation(context.enabled, context.compileCommon, context.compileSpecific, context.mixedCommonSpecificDeclarations),
            languageVersionSettings,
            CjmpRoleSyntaxIdentity.VERIFIED_EQUIVALENT,
        ).result
        val checkedEvents = snapshot.events.filter { it.kind is CjmpRoleEventKind.Check || it.kind is CjmpRoleEventKind.Extend }
        complete = snapshot.completeness == CjmpRoleInputCompleteness.COMPLETE && checkedEvents.all { it.id in carriers }
        roles = replay.events.mapNotNull { result ->
            val carrier = carriers[result.event.id.value] ?: return@mapNotNull null
            RoleResult(carrier, result.event.kind, result.before, result.after, result.outcome, result.error)
        }
        fileRole = file.parsedFileFacts?.cjmpRole
    }

    /** 未改正文也必须保持完整文法；允许修改的正文仍须独立证明文件和后继角色结果不变。 */
    fun canRetain(next: LLCfirSourceSemanticSnapshot, modifiedBodies: Set<CjElement>): Boolean {
        if (!complete || !next.complete || input != next.input || header != next.header || fileRole != next.fileRole) return false
        if (bodies.keys != next.bodies.keys) return false
        if (bodies.any { (owner, grammar) -> owner !in modifiedBodies && grammar != next.bodies[owner] }) return false
        return roles.filter { it.carrier.bodyOwner !in modifiedBodies } ==
            next.roles.filter { it.carrier.bodyOwner !in modifiedBodies }
    }

    /** 即使仅改 trivia，实际正文根被平台替换后也必须重建正文对象和其映射。 */
    fun replacedBodyOwners(next: LLCfirSourceSemanticSnapshot): Set<CjElement> =
        bodies.keys.filterTo(linkedSetOf()) { bodyOrigins[it] !== next.bodyOrigins[it] }
}
