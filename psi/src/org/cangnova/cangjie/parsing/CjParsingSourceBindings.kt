package org.cangnova.cangjie.parsing

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LazyParseableElement
import org.cangnova.cangjie.psi.CjAnnotation
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementTypes
import java.util.UUID

/**
 * 一次 raw 构造所用的真实 PSI 来源绑定。移动的节点以实际 AST 范围绑定本代全文，
 * 首次捕获的事务和原位置保持不变；未展开的 lazy 子树不被本 helper 预热。
 */
class CjParsingSourceBindingScope(private val file: CjFile) {
    private val bindings by lazy(LazyThreadSafetyMode.NONE) { collectBindings() }

    fun bind(prefix: CjMacroDeclarationPrefix, sourceOwner: PsiElement): CjMacroDeclarationPrefix {
        if (prefix.sourceModifiers.isEmpty() && prefix.annotations.isEmpty()) return prefix
        val data = bindings
        data.view.requireCurrent()
        check(file.modificationStamp == data.modificationStamp) { "The PSI source changed during raw construction." }
        return CjMacroDeclarationPrefix(prefix.sourceModifiers.map { fact ->
            if (fact.origin.rootIdentity !in data.roots) fact
            else fact.copy(sourceRange = bindingInOwner(sourceOwner.node, checkNotNull(data.modifiers[fact.origin]) {
                "A local inherited modifier must bind to its actual source token."
            }))
        }, prefix.annotations.map { fact ->
            if (fact.origin.rootIdentity !in data.roots) fact
            else fact.copy(sourceRange = bindingInOwner(sourceOwner.node, checkNotNull(data.annotations[fact.origin]) {
                "A local inherited annotation must bind to its actual annotation node."
            }))
        })
    }

    /** 首次来源可以对应多个复制实例；最近的真实来源 owner 分支必须唯一包含被传递的对象。 */
    private fun bindingInOwner(sourceOwner: ASTNode, candidates: List<Binding>): CjParsingSourceRange {
        var owner: ASTNode? = sourceOwner
        while (owner != null) {
            val branch = owner
            val matches = candidates.filter { candidate ->
                var node: ASTNode? = candidate.node
                while (node != null && node !== branch) node = node.treeParent
                node === branch
            }
            if (matches.isNotEmpty()) return checkNotNull(matches.singleOrNull()) {
                "A prefix source owner must identify one actual AST instance of its captured origin."
            }.range
            owner = branch.treeParent
        }
        error("The prefix source is outside its declared source owner.")
    }

    private data class Binding(val node: ASTNode, val range: CjParsingSourceRange)

    private data class Bindings(
        val view: CjPsiSourceView,
        val roots: Set<UUID>,
        val modifiers: Map<CjParsedSourceOrigin, List<Binding>>,
        val annotations: Map<CjParsedSourceOrigin, List<Binding>>,
        val modificationStamp: Long,
    )

    private fun collectBindings(): Bindings {
        val view = file.currentPsiSourceView()
        val stamp = file.modificationStamp
        val root = file.node
        val snapshot = view.sourceContext.snapshot
        val text = root.chars.toString()
        val roots = linkedSetOf<UUID>()
        val modifiers = linkedMapOf<CjParsedSourceOrigin, MutableList<Binding>>()
        val annotations = linkedMapOf<CjParsedSourceOrigin, MutableList<Binding>>()

        fun visit(node: ASTNode) {
            node.getCopyableUserData(PARSER_SOURCE_ROOT_KEY)?.let(roots::add)
            node.getCopyableUserData(MODIFIER_SOURCE_FACT_KEY)?.let { fact ->
                check(node.elementType == fact.kind.token() && node.chars.toString() == fact.kind.token().value)
                modifiers.getOrPut(fact.origin) { mutableListOf() }.add(Binding(node,
                    CjParsingSourceRange(snapshot, node.startOffset, node.startOffset + node.textLength)))
            }
            node.getCopyableUserData(ANNOTATION_SOURCE_FACT_KEY)?.let { fact ->
                var owner = node.treeParent
                while (owner != null && owner.elementType != CjStubElementTypes.ANNOTATION) owner = owner.treeParent
                val annotation = checkNotNull(owner) { "A captured annotation must retain its grammar owner." }
                if (fact.kind != null) check((annotation.psi as CjAnnotation).shortName?.asString() == fact.sourceName) {
                    "An annotation name change requires a fresh grammar capture."
                }
                annotations.getOrPut(fact.origin) { mutableListOf() }.add(Binding(annotation,
                    CjParsingSourceRange(snapshot, annotation.startOffset, annotation.startOffset + annotation.textLength)))
            }
            if (node is LazyParseableElement && !node.isParsed) return
            var child = node.firstChildNode
            while (child != null) {
                visit(child)
                child = child.treeNext
            }
        }
        visit(root)
        check(file.modificationStamp == stamp && root.chars.toString() == text) {
            "Source bindings require one immutable PSI read transaction."
        }
        view.requireCurrent()
        return Bindings(view, roots, modifiers, annotations, stamp)
    }
}
