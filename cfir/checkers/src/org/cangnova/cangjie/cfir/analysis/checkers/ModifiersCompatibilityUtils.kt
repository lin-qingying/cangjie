package org.cangnova.cangjie.cfir.analysis.checkers

import com.intellij.lang.ASTNode
import com.intellij.lang.LighterASTNode
import com.intellij.openapi.util.Ref
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.lexer.CjKeywordToken
import org.cangnova.cangjie.psi.CjModifierListOwner
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.CjTypeProjection
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.source.CjRealSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.toCjLightSourceElement
import org.cangnova.cangjie.source.toCjPsiSourceElement

/** 从真实源码中提取出的单个修饰符 token 及其 source。 */
internal data class SourceModifier(
    /** 修饰符对应的关键字 token。 */
    val token: CjKeywordToken,
    /** 修饰符在源码中的精确 source。 */
    val source: CjSourceElement,
)

/** 在修饰符列表中按 token 查找第一个修饰符。 */
internal fun List<SourceModifier>.modifierByToken(token: CjKeywordToken): SourceModifier? {
    return firstOrNull { it.token == token }
}

/** 检查两个修饰符之间的兼容性，并按兼容性分类上报重复、冗余、弃用或冲突诊断。 */
context(reporter: DiagnosticReporter, context: CheckerContext)
internal fun checkCompatibilityType(
    firstModifier: SourceModifier,
    secondModifier: SourceModifier,
    reportedNodes: MutableSet<SourceModifier>,
    owner: Any?,
) {
    val firstModifierToken = firstModifier.token
    val secondModifierToken = secondModifier.token
    when (val compatibilityType = compatibility(firstModifierToken, secondModifierToken)) {
        Compatibility.COMPATIBLE -> Unit
        Compatibility.REPEATED -> {
            if (reportedNodes.add(secondModifier)) {
                reporter.reportOn(secondModifier.source, CfirErrors.REPEATED_MODIFIER, secondModifierToken)
            }
        }
        Compatibility.REDUNDANT -> {
            reporter.reportOn(
                secondModifier.source,
                CfirErrors.REDUNDANT_MODIFIER,
                secondModifierToken,
                firstModifierToken,
            )
        }
        Compatibility.REVERSE_REDUNDANT -> {
            reporter.reportOn(
                firstModifier.source,
                CfirErrors.REDUNDANT_MODIFIER,
                firstModifierToken,
                secondModifierToken,
            )
        }
        Compatibility.DEPRECATED -> {
            reporter.reportOn(
                firstModifier.source,
                CfirErrors.DEPRECATED_MODIFIER_PAIR,
                firstModifierToken,
                secondModifierToken,
            )
            reporter.reportOn(
                secondModifier.source,
                CfirErrors.DEPRECATED_MODIFIER_PAIR,
                secondModifierToken,
                firstModifierToken,
            )
        }
        Compatibility.INCOMPATIBLE, Compatibility.COMPATIBLE_FOR_CLASSES_ONLY -> {
            if (compatibilityType == Compatibility.COMPATIBLE_FOR_CLASSES_ONLY &&
                (owner is CfirClassLikeDeclaration || owner is CfirExtend)
            ) {
                return
            }
            if (reportedNodes.add(firstModifier)) {
                reporter.reportOn(
                    firstModifier.source,
                    CfirErrors.INCOMPATIBLE_MODIFIERS,
                    firstModifierToken,
                    secondModifierToken,
                )
            }
            if (reportedNodes.add(secondModifier)) {
                reporter.reportOn(
                    secondModifier.source,
                    CfirErrors.INCOMPATIBLE_MODIFIERS,
                    secondModifierToken,
                    firstModifierToken,
                )
            }
        }
    }
}

/** 对一个声明或 source owner 的修饰符列表执行两两兼容性检查。 */
context(reporter: DiagnosticReporter, context: CheckerContext)
internal fun checkModifiersCompatibility(
    owner: Any?,
    modifiers: List<SourceModifier>,
    reportedNodes: MutableSet<SourceModifier>,
) {
    for ((secondIndex, secondModifier) in modifiers.withIndex()) {
        for (firstIndex in 0 until secondIndex) {
            checkCompatibilityType(modifiers[firstIndex], secondModifier, reportedNodes, owner)
        }
    }
}

/** 从 PSI 或 LightTree source 中提取真实源码修饰符列表。 */
internal fun CjSourceElement.realSourceModifiers(): List<SourceModifier>? {
    if (kind !is CjRealSourceElementKind) return null
    return when (this) {
        is CjPsiSourceElement -> {
            val modifierList = (psi as? CjModifierListOwner)?.modifierList ?: return null
            modifierList.node
                .modifierChildren()
                .map { modifierNode ->
                    SourceModifier(
                        token = modifierNode.elementType as CjKeywordToken,
                        source = modifierNode.psi.toCjPsiSourceElement(kind),
                    )
                }
                .takeIf { it.isNotEmpty() }
        }
        is CjLightSourceElement -> realSourceModifiersForNode(lighterASTNode)
        else -> null
    }
}

/** 返回当前 source 外层的类型投影 source，用于检查类型投影修饰符。 */
internal fun CjSourceElement.enclosingTypeProjectionSource(): CjSourceElement? {
    return when (this) {
        is CjPsiSourceElement -> {
            val projection = psi.parent as? CjTypeProjection ?: return null
            projection.toCjPsiSourceElement(kind)
        }
        is CjLightSourceElement -> {
            val parent = treeStructure.getParent(lighterASTNode) ?: return null
            if (parent.tokenType != CjNodeTypes.TYPE_PROJECTION) return null
            parent.toCjLightSourceElement(
                treeStructure,
                kind,
                treeStructure.getStartOffset(parent),
                treeStructure.getEndOffset(parent),
            )
        }
        else -> null
    }
}

/** 判断 source 是否对应构造器节点。 */
internal fun CjSourceElement.isConstructorSource(): Boolean {
    return when (this) {
        is CjPsiSourceElement -> psi is org.cangnova.cangjie.psi.CjConstructor<*>
        is CjLightSourceElement -> lighterASTNode.tokenType == CjNodeTypes.PRIMARY_CONSTRUCTOR ||
                lighterASTNode.tokenType == CjNodeTypes.SECONDARY_CONSTRUCTOR
        else -> false
    }
}

/** 提取 PSI modifier list 下的修饰符子节点。 */
private fun ASTNode.modifierChildren(): List<ASTNode> =
    getChildren(null).filter { it.elementType is CjKeywordToken }

/** 从 LightTree 节点提取修饰符列表。 */
private fun CjLightSourceElement.realSourceModifiersForNode(node: LighterASTNode): List<SourceModifier>? {
    val modifierListNode = treeStructure.findChildByType(node, CjNodeTypes.MODIFIER_LIST) ?: return null
    return buildList {
        for (child in treeStructure.childrenOf(modifierListNode)) {
            val token = child.tokenType as? CjKeywordToken ?: continue
            add(
                SourceModifier(
                    token = token,
                    source = child.toCjLightSourceElement(
                        treeStructure,
                        kind,
                        treeStructure.getStartOffset(child),
                        treeStructure.getEndOffset(child),
                    ),
                )
            )
        }
    }.takeIf { it.isNotEmpty() }
}

/** 以 sequence 形式遍历 LightTree 节点的直接子节点。 */
private fun FlyweightCapableTreeStructure<LighterASTNode>.childrenOf(node: LighterASTNode): Sequence<LighterASTNode> {
    return sequence {
        for (child in getChildrenArray(node)) {
            if (child != null) {
                yield(child)
            }
        }
    }
}

/** 读取 LightTree 节点的直接子节点数组。 */
private fun FlyweightCapableTreeStructure<LighterASTNode>.getChildrenArray(node: LighterASTNode): Array<LighterASTNode?> {
    val childrenRef = Ref<Array<LighterASTNode?>>()
    getChildren(node, childrenRef)
    return childrenRef.get() ?: emptyArray()
}

/** 在 LightTree 节点的直接子节点中查找指定 token 类型的节点。 */
private fun FlyweightCapableTreeStructure<LighterASTNode>.findChildByType(
    node: LighterASTNode,
    type: IElementType,
): LighterASTNode? {
    return getChildrenArray(node).firstOrNull { it?.tokenType == type }
}
