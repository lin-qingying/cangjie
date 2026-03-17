

package org.cangnova.cangjie.generators.tree

import org.cangnova.cangjie.generators.tree.imports.Importable

/**
 * 持有实现类型信息的模型抽象。
 */
interface ImplementationKindOwner : TypeRef, Importable {
    var kind: ImplementationKind?
    val allParents: List<ImplementationKindOwner>

    val needPureAbstractElement: Boolean
        get() = kind?.isInterface != true &&
                allParents.none { it.kind == ImplementationKind.AbstractClass || it.kind == ImplementationKind.SealedClass }
}
