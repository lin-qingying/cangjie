package org.cangnova.cangjie.cfir.patterns

import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement
import java.util.Collections
import java.util.IdentityHashMap

/**
 * 模式绑定出现位置。
 *
 * 统一把“名字 + 源位置 + 绑定变量实体”收敛成一个稳定视图，
 * 供 resolve、诊断、导航和索引层共享，避免每一层再次手写递归。
 *
 * @property name 绑定名。
 * @property source 绑定名或绑定节点的源码位置。
 * @property variable 绑定变量声明实体；无法形成变量实体时为 `null`。
 */
data class CfirPatternBindingOccurrence(
    /**
     * 模式绑定引入的名称。
     */
    val name: Name,
    /**
     * 绑定名称或绑定节点对应的源码位置。
     */
    val source: CjSourceElement?,
    /**
     * 绑定名称对应的 CFIR 变量声明；无法形成变量声明时为 null。
     */
    val variable: CfirPatternBindingVariable?,
)

/**
 * 收集模式树中所有具名绑定。
 *
 * 注意：
 * 1. 外层 `CfirPatternVariable` 容器不是绑定本身，不会出现在结果里。
 * 2. `or-pattern` 仍然按各自分支分别保留，调用方可以自行决定是否只看首分支。
 */
fun CfirPattern.bindingOccurrences(): List<CfirPatternBindingOccurrence> = when (this) {
    is CfirBindingPattern -> buildList {
        add(CfirPatternBindingOccurrence(name, source, bindingVariable))
        nestedPattern?.let { addAll(it.bindingOccurrences()) }
    }

    is CfirVarOrEnumPattern -> bindingVariable?.let { variable ->
        listOf(CfirPatternBindingOccurrence(name, source, variable))
    }.orEmpty()
    is CfirTuplePattern -> elements.flatMap(CfirPattern::bindingOccurrences)
    is CfirEnumPattern -> arguments.flatMap(CfirPattern::bindingOccurrences)
    is CfirTypePattern -> bindingName?.let {
        listOf(CfirPatternBindingOccurrence(it, bindingVariable?.source ?: source, bindingVariable))
    }.orEmpty()

    is CfirOrPattern -> alternatives.flatMap(CfirPattern::bindingOccurrences)
    is CfirWildcardPattern, is CfirConstPattern, is CfirExpressionPattern -> emptyList()
}

/**
 * 收集模式树中所有绑定变量实体。
 */
fun CfirPattern.bindingVariables(): List<CfirPatternBindingVariable> =
    bindingOccurrences().mapNotNull(CfirPatternBindingOccurrence::variable)

/** 供生成源码调用的模式绑定集合入口。 */
internal fun patternBindingVariables(pattern: CfirPattern): List<CfirPatternBindingVariable> =
    pattern.bindingVariables()

/**
 * 返回可进入局部作用域/局部重定义检查的模式绑定。
 *
 * 官方语义中，`|` 连接的模式禁止引入变量。普通 enum/var 绑定在这种非法
 * or-pattern 中不会成为分支体可解析的局部变量；但 `x: T` type-pattern
 * 仍会形成可见绑定，并继续参与同一 case 内的重定义诊断。
 */
fun CfirPattern.visibleBindingVariables(): List<CfirPatternBindingVariable> = when (this) {
    is CfirOrPattern -> alternatives.flatMap(CfirPattern::typePatternBindingVariables)
    else -> bindingVariables()
}

/**
 * 返回 binding 唯一 owner 的完整 initializer。拆分、嵌套模式和脱离 owner 的 binding 均无此值。
 */
fun wholeValueInitializer(
    owner: CfirPatternTreeOwner?,
    binding: CfirPatternBindingVariable,
) = (owner as? CfirPatternVariable)
    ?.takeIf { it.pattern.wholeValueBindingOrNull() === binding }
    ?.initializer

/** 一棵模式子树在 owner 变更前的节点身份快照。 */
internal data class CfirPatternTreeSnapshot(
    val nodes: List<CfirPatternTreeNode>,
)

/** 捕获任何 CFIR 模式宿主的整棵模式树，包括 catch/handle 专用模式节点。 */
internal fun capturePatternTree(root: CfirElement): CfirPatternTreeSnapshot {
    val nodes = mutableListOf<CfirPatternTreeNode>()
    val visited = Collections.newSetFromMap(IdentityHashMap<CfirPatternTreeNode, Boolean>())

    fun visit(element: CfirElement) {
        val node = element as? CfirPatternTreeNode
            ?: error("Pattern tree contains a node without CfirPatternTreeNode ownership support: ${element::class.qualifiedName}")
        require(visited.add(node)) {
            "A CFIR pattern tree cannot contain a cycle or share the same tree node more than once."
        }
        nodes += node
        for (binding in immediatePatternBindings(element)) {
            require(visited.add(binding)) {
                "A CFIR pattern tree cannot contain a cycle or share the same tree node more than once."
            }
            nodes += binding
        }
        when (element) {
            is CfirCatchPattern -> Unit
            is CfirPattern -> element.childPatterns().forEach(::visit)
            is CfirCommandTypePattern -> Unit
            else -> error("Unsupported CFIR pattern tree root: ${element::class.qualifiedName}")
        }
    }

    visit(root)
    return CfirPatternTreeSnapshot(nodes)
}

/** 返回当前模式节点直接声明的 binding；嵌套模式由树遍历分别访问。 */
private fun immediatePatternBindings(element: CfirElement): List<CfirPatternBindingVariable> = when (element) {
    is CfirBindingPattern -> listOfNotNull(element.bindingVariable)
    is CfirVarOrEnumPattern -> listOfNotNull(element.bindingVariable)
    is CfirTypePattern -> listOfNotNull(element.bindingVariable)
    is CfirCatchPattern -> listOfNotNull(element.bindingVariable)
    else -> emptyList()
}

private fun CfirPattern.childPatterns(): List<CfirPattern> = when (this) {
    is CfirBindingPattern -> listOfNotNull(nestedPattern)
    is CfirTuplePattern -> elements
    is CfirEnumPattern -> arguments
    is CfirOrPattern -> alternatives
    is CfirConstPattern,
    is CfirWildcardPattern,
    is CfirVarOrEnumPattern,
    is CfirTypePattern,
    is CfirExpressionPattern,
    -> emptyList()
}

/** 校验任意模式树节点的反向 owner 引用只随真实子树关系建立和解除。 */
internal fun validatePatternTreeNodeOwnerChange(
    node: CfirPatternTreeNode,
    previousOwner: CfirPatternTreeOwner?,
    newOwner: CfirPatternTreeOwner?,
) {
    if (previousOwner === newOwner) return

    require(previousOwner == null || newOwner == null) {
        "A pattern tree node cannot be transferred directly between owners."
    }
    if (previousOwner != null) {
        require(capturePatternTree(previousOwner.patternTreeRoot()).nodes.none { it === node }) {
            "A pattern tree node cannot detach while its owner tree still contains it."
        }
    }
    if (newOwner != null) {
        require(capturePatternTree(newOwner.patternTreeRoot()).nodes.any { it === node }) {
            "A pattern tree owner can only claim a node contained in its tree."
        }
    }
}

/**
 * 在 pattern 写入前验证所有 binding 均可由该 owner 持有。
 *
 * 使用身份集合拒绝同一 binding 在一棵 CFIR pattern 中重复出现，保证 pattern 是树而非共享子图。
 */
private fun validatePatternTreeContents(root: CfirElement, owner: CfirPatternTreeOwner?) {
    val snapshot = capturePatternTree(root)
    val seenNodes = Collections.newSetFromMap(IdentityHashMap<CfirPatternTreeNode, Boolean>())
    for (node in snapshot.nodes) {
        require(seenNodes.add(node)) {
            "A CFIR pattern tree cannot contain the same tree node more than once."
        }
        require(node.patternTreeOwner == null || node.patternTreeOwner === owner) {
            "A CFIR pattern tree cannot be shared by different owners."
        }
    }

}

/** 在任意模式宿主安装新的根节点前验证整棵新树的节点和 binding 身份。 */
internal fun validatePatternTreeReplacement(owner: CfirPatternTreeOwner, pattern: CfirElement) =
    validatePatternTreeContents(pattern, owner)

/** 校验暂时没有模式宿主的独立模式树不引用其他 owner 的节点。 */
internal fun validateDetachedPatternTree(pattern: CfirElement) =
    validatePatternTreeContents(pattern, owner = null)

/**
 * 原子恢复一个解析快照中的全部模式根。
 *
 * 候选解析期间允许旧根先从一个 owner 脱离，再由另一个 owner 接管；逐个恢复会受 owner 遍历顺序影响。
 * 这里先验证整组根和节点，再暂存根字段，最后统一重建每个节点的唯一反向 owner 链接。
 */
fun restorePatternTreeRoots(roots: List<Pair<CfirPatternTreeOwner, CfirElement>>) {
    val rootsSnapshot = roots.toList()
    if (rootsSnapshot.isEmpty()) return

    val owners = Collections.newSetFromMap(IdentityHashMap<CfirPatternTreeOwner, Boolean>())
    for ((owner, restoredRoot) in rootsSnapshot) {
        require(owners.add(owner)) {
            "A pattern tree snapshot cannot contain the same owner more than once."
        }
        owner.validatePatternTreeRoot(restoredRoot)
    }

    val affectedNodes = Collections.newSetFromMap(IdentityHashMap<CfirPatternTreeNode, Boolean>())
    val restoredOwnerByNode = IdentityHashMap<CfirPatternTreeNode, CfirPatternTreeOwner>()

    for ((owner, restoredRoot) in rootsSnapshot) {
        affectedNodes.addAll(capturePatternTree(owner.patternTreeRoot()).nodes)
        for (node in capturePatternTree(restoredRoot).nodes) {
            require(restoredOwnerByNode.put(node, owner) == null) {
                "A restored CFIR pattern node cannot be shared by different owners."
            }
            affectedNodes += node
        }
    }

    for (node in affectedNodes) {
        val currentOwner = node.patternTreeOwner
        require(currentOwner == null || currentOwner in owners) {
            "A pattern tree snapshot cannot restore a node currently owned outside the captured owner set."
        }
    }

    withPatternTreeRootRestore {
        for ((owner, restoredRoot) in rootsSnapshot) {
            owner.replacePatternTreeRoot(restoredRoot)
        }
        for (node in affectedNodes) {
            node.replacePatternTreeOwner(restoredOwnerByNode[node])
        }
    }
}

/** 将原树身份快照与 owner 当前的根节点协调，先完整验证，再解除旧链接和建立新链接。 */
internal fun reconcilePatternTree(
    owner: CfirPatternTreeOwner,
    previousTree: CfirPatternTreeSnapshot,
    newPattern: CfirElement,
) {
    require(owner.patternTreeRoot() === newPattern) {
        "Pattern tree reconciliation requires the owner's current root node."
    }
    validatePatternTreeReplacement(owner, newPattern)
    val newTree = capturePatternTree(newPattern)

    for (node in previousTree.nodes) {
        require(node.patternTreeOwner == null || node.patternTreeOwner === owner) {
            "A previous pattern tree node is owned by a different owner."
        }
        if (node.patternTreeOwner === owner && newTree.nodes.none { it === node }) {
            node.replacePatternTreeOwner(null)
        }
    }
    for (node in newTree.nodes) {
        if (node.patternTreeOwner == null) node.replacePatternTreeOwner(owner)
    }
}

/** 为新构造的模式宿主建立根节点和全部子节点的双向 owner 关系。 */
internal fun bindPatternTree(owner: CfirPatternTreeOwner, pattern: CfirElement) {
    require(owner.patternTreeRoot() === pattern) {
        "Pattern tree nodes can only be bound after the owner has installed its root."
    }
    reconcilePatternTree(owner, CfirPatternTreeSnapshot(emptyList()), pattern)
}

/**
 * 返回模式树中唯一承载整个模式值的 binding；元组/枚举等拆分绑定返回 `null`。
 *
 * 只有这类 binding 的语义类型与 owner 的完整值类型一致，可以直接投影 owner 的
 * return type；其余 binding 必须由 pattern 形状逐元素投影。
 */
fun CfirPattern.wholeValueBindingOrNull(): CfirPatternBindingVariable? = when (this) {
    is CfirBindingPattern if nestedPattern == null -> bindingVariable
    is CfirVarOrEnumPattern -> bindingVariable
    is CfirTypePattern -> bindingVariable
    else -> null
}

/**
 * 收集 type-pattern 产生的绑定变量。
 *
 * 显式类型模式的名字在语义检查前已经声明；形状检查失败也不能撤销这些名字。
 */
fun CfirPattern.typePatternBindingVariables(): List<CfirPatternBindingVariable> = when (this) {
    is CfirTypePattern -> bindingVariable?.let(::listOf).orEmpty()
    is CfirTuplePattern -> elements.flatMap(CfirPattern::typePatternBindingVariables)
    is CfirEnumPattern -> arguments.flatMap(CfirPattern::typePatternBindingVariables)
    is CfirBindingPattern -> nestedPattern?.typePatternBindingVariables().orEmpty()
    is CfirOrPattern -> alternatives.flatMap(CfirPattern::typePatternBindingVariables)
    is CfirVarOrEnumPattern,
    is CfirWildcardPattern,
    is CfirConstPattern,
    is CfirExpressionPattern,
    -> emptyList()
}

/**
 * 收集模式树中所有绑定名称。
 */
fun CfirPattern.bindingNames(): List<Name> =
    bindingOccurrences().map(CfirPatternBindingOccurrence::name)

/**
 * 返回模式树中的主绑定名。
 *
 * 当前策略取绑定出现顺序中的第一个名称；没有绑定时返回 `null`。
 */
fun CfirPattern.primaryBindingNameOrNull(): Name? =
    bindingOccurrences().firstOrNull()?.name
