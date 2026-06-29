

package org.cangnova.cangjie.generators.tree

import org.cangnova.cangjie.generators.util.Node
import org.cangnova.cangjie.generators.util.solveGraphForClassVsInterface

/**
 * 决定树中哪些元素应为（抽象）类，哪些应为接口。
 *
 * @property elements 需要推断 [ImplementationKind] 的元素列表。
 */
class InterfaceAndAbstractClassConfigurator(val elements: List<ImplementationKindOwner>) {

    /**
     * 将 [ImplementationKindOwner] 适配为图求解器需要的节点类型。
     */
    private inner class NodeImpl(val element: ImplementationKindOwner) : Node {
        /**
         * 当前节点在实现层级中的父节点。
         */
        override val parents: List<NodeImpl>
            get() = element.allParents.map(::NodeImpl)

        /**
         * 图求解器要求的原始节点引用。
         */
        override val origin: NodeImpl
            get() = this

        /**
         * 节点相等性按底层元素判断。
         */
        override fun equals(other: Any?): Boolean = other is NodeImpl && element == other.element

        /**
         * 节点哈希值按底层元素计算。
         */
        override fun hashCode(): Int = element.hashCode()
    }

    /**
     * 判断实现节点是否应生成为 final class。
     *
     * 只有没有被其他节点作为父节点引用的具体实现，才能成为 final class。
     */
    private fun shouldBeFinalClass(element: ImplementationKindOwner, allParents: Set<ImplementationKindOwner>): Boolean =
        element is AbstractImplementation<*, *, *> && element !in allParents

    /**
     * 根据图求解结果写回每个节点的 [ImplementationKind]。
     */
    private fun updateKinds(nodes: List<NodeImpl>, solution: List<Boolean>) {
        val allParents = nodes.flatMapTo(mutableSetOf()) { element -> element.parents.map { it.origin.element } }

        for (index in solution.indices) {
            val isClass = solution[index]
            val node = nodes[index].origin
            val element = node.element
            val existingKind = element.kind
            if (isClass) {
                require(existingKind != ImplementationKind.Interface) {
                    "$element must NOT be an interface"
                }
                if (existingKind == null) {
                    element.kind = if (shouldBeFinalClass(element, allParents))
                        ImplementationKind.FinalClass
                    else
                        ImplementationKind.AbstractClass
                }
            } else {
                element.kind = ImplementationKind.Interface
            }
        }
    }

    /**
     * 根据元素上的 sealed 标志修正已推导出的 class/interface 种类。
     */
    private fun updateSealedKinds(nodes: Collection<NodeImpl>) {
        for (node in nodes) {
            val element = node.element
            if (element is AbstractElement<*, *, *>) {
                if (element.isSealed) {
                    element.kind = when (element.kind) {
                        ImplementationKind.AbstractClass, ImplementationKind.SealedClass -> ImplementationKind.SealedClass
                        ImplementationKind.Interface, ImplementationKind.SealedInterface -> ImplementationKind.SealedInterface
                        else -> error("element $element with kind ${element.kind} can not be sealed")
                    }
                }
            }
        }
    }

    /**
     * 为全部元素和实现类推导最终的接口/抽象类/具体类形态。
     *
     * 显式配置为接口或类的节点会作为约束传给图求解器，求解结果再写回缺省节点。
     */
    fun configureInterfacesAndAbstractClasses() {
        val nodes = this.elements.map(::NodeImpl)
        val solution = solveGraphForClassVsInterface(
            nodes,
            nodes.filter { it.element.kind?.typeKind == TypeKind.Interface },
            nodes.filter { it.element.kind?.typeKind == TypeKind.Class },
        )
        updateKinds(nodes, solution)
        updateSealedKinds(nodes)
    }
}
