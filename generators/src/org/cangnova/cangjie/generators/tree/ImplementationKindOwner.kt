

package org.cangnova.cangjie.generators.tree

import org.cangnova.cangjie.generators.tree.imports.Importable

/**
 * 持有实现类型信息的模型抽象。
 */
interface ImplementationKindOwner : TypeRef, Importable {
    /**
     * 当前节点应生成的实现种类。
     */
    var kind: ImplementationKind?
    /**
     * 当前节点的所有实现种类父节点。
     */
    val allParents: List<ImplementationKindOwner>

    /**
     * 当前节点是否需要额外生成纯抽象元素承载公共 API。
     */
    val needPureAbstractElement: Boolean
        get() = kind?.isInterface != true &&
                allParents.none { it.kind == ImplementationKind.AbstractClass || it.kind == ImplementationKind.SealedClass }
}
