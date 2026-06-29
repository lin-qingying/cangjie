

package org.cangnova.cangjie.generators.tree

import kotlin.reflect.KMutableProperty1

/**
 * 树模型根对象，持有所有元素并提供后处理步骤。
 */
data class Model<Element : AbstractElement<Element, *, *>>(
    /**
     * 当前树模型中除根元素之外的所有可生成元素。
     *
     * 列表顺序由具体树构建器决定，后续继承、visitor 和 implementation 配置会遍历该集合。
     */
    val elements: List<Element>,
    /**
     * 当前树模型的根元素。
     *
     * 根元素代表整棵树的公共父类型，子元素遍历、visitor 生成和 accept/transform 冒泡计算都以它为入口。
     */
    val rootElement: Element,
) {
    /**
     * 计算并写入每个元素的完整字段列表。
     *
     * 该步骤会先递归处理父元素，保证子元素合并字段时父级 [AbstractElement.allFields] 已经稳定。
     */
    fun inheritFields() {
        val processed = mutableSetOf<Element>()
        fun recurse(element: Element) {
            if (!processed.add(element)) return
            for (parent in element.elementParents) {
                recurse(parent.element)
            }
            element.inheritFields()
        }

        for (element in elements + rootElement) {
            recurse(element)
        }
    }

    /**
     * 为需要纯抽象公共父类的元素补充额外父类型。
     *
     * @param pureAbstractElement 由具体树生成器提供的纯抽象元素类型引用。
     */
    fun addPureAbstractElement(pureAbstractElement: ClassRef<*>) {
        for (el in elements) {
            if (el.needPureAbstractElement) {
                el.otherParents.add(pureAbstractElement)
            }
        }
    }

    /**
     * 推导每个元素是否应声明 `acceptChildren` 与 `transformChildren`。
     *
     * 先在叶子层根据可遍历/可转换字段标记方法需求，再把字段形状一致的子类方法向父元素冒泡，
     * 避免在每个具体子类中生成重复方法体。
     */
    fun specifyHasAcceptAndTransformChildrenMethods() {
        for (el in elements) {
            el.hasAcceptChildrenMethod = el.isRootElement || (el.subElements.isEmpty() && el.walkableChildren.isNotEmpty())
            el.hasTransformChildrenMethod = el.isRootElement || (el.subElements.isEmpty() && el.transformableChildren.isNotEmpty())
        }

        rootElement.elementDescendantsAndSelfDepthFirst().toList().reversed().forEach { element ->
            @Suppress("UNCHECKED_CAST")
            bubbleUpAcceptOrTransformChildrenMethod(
                element,
                AbstractElement<*, *, *>::hasAcceptChildrenMethod as KMutableProperty1<Element, Boolean>,
                AbstractElement<*, *, *>::walkableChildren
            )
            @Suppress("UNCHECKED_CAST")
            bubbleUpAcceptOrTransformChildrenMethod(
                element,
                AbstractElement<*, *, *>::hasTransformChildrenMethod as KMutableProperty1<Element, Boolean>,
                AbstractElement<*, *, *>::transformableChildren
            )
        }
    }

    /**
     * 将子元素中形状完全一致的 children 方法需求提升到父元素。
     *
     * 当父元素的所有直接子元素都需要同一种 children 方法，且它们的字段集合大小和顺序覆盖与父元素兼容时，
     * 方法可只在父元素生成，子元素标志被清空以减少重复代码。
     */
    private fun bubbleUpAcceptOrTransformChildrenMethod(
        parent: Element,
        hasAcceptOrTransformChildrenMethod: KMutableProperty1<Element, Boolean>,
        walkableOrTransformableChildren: Element.() -> List<AbstractField<*>>,
    ) {
        if (parent.subElements.isNotEmpty() &&
            parent.subElements.all {
                hasAcceptOrTransformChildrenMethod.get(it) &&
                        it.walkableOrTransformableChildren().size == parent.walkableOrTransformableChildren().size
                        && it.childrenOrderOverride == null
            }
        ) {
            for (child in parent.subElements) {
                hasAcceptOrTransformChildrenMethod.set(child, false)
            }

            hasAcceptOrTransformChildrenMethod.set(parent, true)
        }
    }
}
