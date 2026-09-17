package org.cangnova.cangjie.cfir.serialization.cjd

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjSourceElement

/** 从不可变快照重建轻量 source 锚点；不保留 parser builder、PSI 或整份文件。 */
internal fun cjdAnnotationSource(rawText: String, range: CjdSourceRange): CjSourceElement {
    val node = object : LighterASTNode {
        override fun getTokenType(): IElementType = CjNodeTypes.ANNOTATION
        override fun getStartOffset(): Int = range.startOffset
        override fun getEndOffset(): Int = range.endOffset
    }
    val tree = object : FlyweightCapableTreeStructure<LighterASTNode> {
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
