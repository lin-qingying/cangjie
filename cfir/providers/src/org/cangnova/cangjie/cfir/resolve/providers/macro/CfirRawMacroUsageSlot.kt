package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirCodeFragment
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable

/** 实际采用槽位的稳定身份；owner是现有CFIR对象，不使用名称、源码范围或复制根身份。 */
class CfirRawMacroUsageSlot(val owner: CfirElement, val kind: Kind) {
    enum class Kind { BODY, PARAMETER_DEFAULT, INITIALIZER, CODE_FRAGMENT, HEADER }

    fun currentRootOrNull(): CfirElement? = when (kind) {
        Kind.BODY -> (owner as CfirFunction).body
        Kind.PARAMETER_DEFAULT -> (owner as CfirValueParameter).defaultValue
        Kind.INITIALIZER -> (owner as CfirVariable).initializer
        Kind.CODE_FRAGMENT -> (owner as CfirCodeFragment).block
        Kind.HEADER -> owner
    }

    override fun equals(other: Any?): Boolean =
        other is CfirRawMacroUsageSlot && owner === other.owner && kind == other.kind

    override fun hashCode(): Int = 31 * System.identityHashCode(owner) + kind.hashCode()
}
