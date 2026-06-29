

package org.cangnova.cangjie.generators.tree

/**
 * 生成实现类的类别。
 */
enum class ImplementationKind(val title: String, val typeKind: TypeKind) {
    Interface("interface", TypeKind.Interface),
    FinalClass("class", TypeKind.Class),
    OpenClass("open class", TypeKind.Class),
    AbstractClass("abstract class", TypeKind.Class),
    SealedClass("sealed class", TypeKind.Class),
    SealedInterface("sealed interface", TypeKind.Interface),
    Object("object", TypeKind.Class);

    /**
     * 当前实现种类是否生成接口形态。
     */
    val isInterface: Boolean
        get() = typeKind == TypeKind.Interface
}
