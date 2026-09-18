package org.cangnova.cangjie.cfir.serialization.cjd

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjSourceElement

/** sidecar source 的稳定来源信息；不会持有 PSI 或二进制声明。 */
interface CjdAnnotationSourceProvenance {
    val sourceId: String
    val rawText: String
    val range: CjdSourceRange
}

/**
 * 读取 source 元素上挂载的 sidecar 来源信息。
 *
 * 仅当底层 treeStructure 实现了 [CjdAnnotationSourceProvenance] 时返回；
 * 普通 PSI / light-tree source 不属于 sidecar 体系，返回 `null`。
 */
val CjSourceElement.cjdAnnotationProvenance: CjdAnnotationSourceProvenance?
    get() = treeStructure as? CjdAnnotationSourceProvenance

/** 从不可变快照重建轻量 source 锚点；不保留 parser builder、PSI 或整份文件。 */
internal fun cjdAnnotationSource(rawText: String, range: CjdSourceRange, sourceId: String): CjSourceElement {
    val node = object : LighterASTNode {
        override fun getTokenType(): IElementType = CjNodeTypes.ANNOTATION
        override fun getStartOffset(): Int = range.startOffset
        override fun getEndOffset(): Int = range.endOffset
    }
    val tree = object : FlyweightCapableTreeStructure<LighterASTNode>, CjdAnnotationSourceProvenance {
        override val sourceId: String = sourceId
        override val rawText: String = rawText
        override val range: CjdSourceRange = range
        override fun getRoot(): LighterASTNode = node
        override fun getParent(node: LighterASTNode): LighterASTNode? = null
        override fun getChildren(node: LighterASTNode, into: Ref<Array<LighterASTNode>>): Int {
            into.set(LighterASTNode.EMPTY_ARRAY)
            return 0
        }
        override fun disposeChildren(children: Array<out LighterASTNode>?, count: Int) {}
        override fun toString(node: LighterASTNode): CharSequence = rawText
        override fun getStartOffset(node: LighterASTNode): Int = node.startOffset
        override fun getEndOffset(node: LighterASTNode): Int = node.endOffset
    }
    return CjLightSourceElement(node, range.startOffset, range.endOffset, tree)
}
