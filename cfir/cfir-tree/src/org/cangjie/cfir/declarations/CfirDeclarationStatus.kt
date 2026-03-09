package org.cangjie.cfir.declarations

import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.descriptors.Visibility

/**
 * 声明状态，包含修饰符信息。
 *
 * 对应仓颉编译器中 Decl 节点的各种属性标记。
 */
data class CfirDeclarationStatus(
    val visibility: Visibility = Visibilities.Public,
    val isAbstract: Boolean = false,
    val isOpen: Boolean = false,
    val isSealed: Boolean = false,
    val isStatic: Boolean = false,
    /** 仓颉 mut 函数修饰符 */
    val isMut: Boolean = false,
    val isOverride: Boolean = false,
    val isOperator: Boolean = false,
    val isInline: Boolean = false,
    val isUnsafe: Boolean = false,
    val isForeign: Boolean = false,
) {
    companion object {
        val DEFAULT = CfirDeclarationStatus()
    }
}
