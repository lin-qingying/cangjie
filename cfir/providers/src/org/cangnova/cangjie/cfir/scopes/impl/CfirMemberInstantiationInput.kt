package org.cangnova.cangjie.cfir.scopes.impl

import org.cangnova.cangjie.cfir.scopes.CfirInheritanceMemberHeader
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.type.model.TypeConstructorMarker
import java.util.Collections

/** 结构 scope 不承诺实例化 header；完成输入由 provider 在正常签名阶段发布。 */
internal sealed interface CfirMemberInstantiationInput {
    data object NotRequested : CfirMemberInstantiationInput

    class Complete(
        headers: Map<CfirCallableSymbol<*>, CfirInheritanceMemberHeader>,
        substitutions: Map<CfirCallableSymbol<*>, CfirPreparedMemberSubstitution>,
    ) : CfirMemberInstantiationInput {
        val headers = Collections.unmodifiableMap(LinkedHashMap(headers))
        val substitutions = Collections.unmodifiableMap(LinkedHashMap(substitutions))
        init { require(headers.keys == substitutions.keys) }
        override fun equals(other: Any?): Boolean = other is Complete && headers == other.headers && substitutions == other.substitutions
        override fun hashCode(): Int = 31 * headers.hashCode() + substitutions.hashCode()
    }
}

/** physical→logical→签名 owner 的完成映射；不保存 resolver 或 substitutor 回调。 */
internal class CfirPreparedMemberSubstitution(
    val structType: ConeCangJieType,
    replacements: Map<TypeConstructorMarker, ConeCangJieType>,
) {
    val replacements = Collections.unmodifiableMap(LinkedHashMap(replacements))
    override fun equals(other: Any?): Boolean = other is CfirPreparedMemberSubstitution && structType == other.structType && replacements == other.replacements
    override fun hashCode(): Int = 31 * structType.hashCode() + replacements.hashCode()
}
