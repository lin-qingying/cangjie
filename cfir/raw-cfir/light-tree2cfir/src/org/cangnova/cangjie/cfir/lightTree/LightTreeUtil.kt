package org.cangnova.cangjie.cfir.lightTree

import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.util.diff.FlyweightCapableTreeStructure

/**
 * LightTree 遍历工具（对齐 Kotlin K2 的 LightTreeUtil 模式）。
 *
 * 封装 [FlyweightCapableTreeStructure] 的子节点遍历 API，
 * 提供便捷的子节点查找和过滤操作。
 */

/**
 * 获取节点的所有子节点数组。
 *
 * 使用 [Ref] + [FlyweightCapableTreeStructure.getChildren] API，
 * 这是 IntelliJ LightTree 标准的子节点访问方式。
 */
fun FlyweightCapableTreeStructure<LighterASTNode>.getChildrenArray(
    node: LighterASTNode,
): Array<out LighterASTNode?> {
    val ref = Ref<Array<LighterASTNode?>>()
    getChildren(node, ref)
    return ref.get() ?: emptyArray()
}

/**
 * 遍历节点的所有非空子节点。
 */
inline fun FlyweightCapableTreeStructure<LighterASTNode>.forEachChildren(
    node: LighterASTNode,
    action: (LighterASTNode) -> Unit,
) {
    val children = getChildrenArray(node)
    for (child in children) {
        if (child == null) break
        action(child)
    }
}

/**
 * 遍历子节点并收集结果到列表。
 */
inline fun <T> FlyweightCapableTreeStructure<LighterASTNode>.forEachChildrenReturnList(
    node: LighterASTNode,
    collector: (LighterASTNode, MutableList<T>) -> Unit,
): MutableList<T> {
    val result = mutableListOf<T>()
    forEachChildren(node) { child ->
        collector(child, result)
    }
    return result
}

/**
 * 获取第一个匹配指定类型的子节点。
 */
fun FlyweightCapableTreeStructure<LighterASTNode>.findChildByType(
    node: LighterASTNode,
    type: IElementType,
): LighterASTNode? {
    forEachChildren(node) { child ->
        if (child.tokenType == type) return child
    }
    return null
}

/**
 * 获取所有匹配指定类型集合的子节点。
 */
fun FlyweightCapableTreeStructure<LighterASTNode>.getChildrenByType(
    node: LighterASTNode,
    types: TokenSet,
): List<LighterASTNode> {
    return forEachChildrenReturnList(node) { child, list ->
        if (types.contains(child.tokenType)) {
            list.add(child)
        }
    }
}

/**
 * 获取所有匹配指定类型的子节点。
 */
fun FlyweightCapableTreeStructure<LighterASTNode>.getChildrenByType(
    node: LighterASTNode,
    type: IElementType,
): List<LighterASTNode> {
    return forEachChildrenReturnList(node) { child, list ->
        if (child.tokenType == type) {
            list.add(child)
        }
    }
}

/**
 * 获取第一个作为表达式的子节点。
 *
 * 简单判断：非叶子节点即为表达式候选。
 */
fun FlyweightCapableTreeStructure<LighterASTNode>.getFirstChildExpression(
    node: LighterASTNode,
): LighterASTNode? {
    forEachChildren(node) { child ->
        if (child.tokenType !is com.intellij.psi.TokenType && child.endOffset > child.startOffset) {
            return child
        }
    }
    return null
}

/**
 * 获取最后一个作为表达式的子节点。
 */
fun FlyweightCapableTreeStructure<LighterASTNode>.getLastChildExpression(
    node: LighterASTNode,
): LighterASTNode? {
    var result: LighterASTNode? = null
    forEachChildren(node) { child ->
        if (child.tokenType !is com.intellij.psi.TokenType && child.endOffset > child.startOffset) {
            result = child
        }
    }
    return result
}

/**
 * 获取节点在源码中的文本。
 *
 * 通过 [startOffset] 和 [endOffset] 从原始源码 [CharSequence] 中截取。
 */
fun getNodeText(
    node: LighterASTNode,
    tree: FlyweightCapableTreeStructure<LighterASTNode>,
    source: CharSequence,
): String {
    val start = tree.getStartOffset(node).coerceAtLeast(0)
    val end = tree.getEndOffset(node).coerceAtMost(source.length)
    return if (start < end) source.substring(start, end) else ""
}
