package org.cangnova.cangjie.resolve.calls.mpp

import java.util.Collections

/** 初始化依赖拥有独立的收集生命周期；尚未收集不能作为已知无依赖参与初始化排序。 */
sealed interface CjmpInitializationDependencies<out D : CjmpDeclarationSymbolMarker> {
    data object NotCollected : CjmpInitializationDependencies<Nothing>

    /** 收集完成后一次冻结真实声明边，包括有意义的空集合。 */
    class Collected<D : CjmpDeclarationSymbolMarker>(declarations: Collection<D>) : CjmpInitializationDependencies<D> {
        val declarations: List<D> = Collections.unmodifiableList(ArrayList(declarations))

        override fun equals(other: Any?): Boolean = other is Collected<*> && declarations == other.declarations
        override fun hashCode(): Int = declarations.hashCode()
        override fun toString(): String = "Collected($declarations)"
    }
}

/** 合并仅替换已经收集的边；不会启动依赖收集，也不会把未收集状态改为空列表。 */
fun <D : CjmpDeclarationSymbolMarker> CjmpInitializationDependencies<D>.mapDeclarations(
    transform: (D) -> D,
): CjmpInitializationDependencies<D> = when (this) {
    CjmpInitializationDependencies.NotCollected -> this
    is CjmpInitializationDependencies.Collected -> CjmpInitializationDependencies.Collected(declarations.map(transform))
}
