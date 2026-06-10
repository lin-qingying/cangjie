package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.renderForDebugging

/**
 * 成员 override 签名的 providers 层表示。
 *
 * Kotlin FIR 通过 `FirOverrideChecker` 在 use-site scope 中判断声明与父成员的覆盖关系；
 * 当前 CFIR 尚未抽出完整 override checker，因此 providers / checkers 共用这个签名入口，
 * 避免不同阶段各自用不同规则判断“同一成员签名”。
 */
fun CfirCallableSymbol<*>.overrideSignatureKey(): String {
    if (!isBound) return callableIdAsString()

    return when (val declaration = cfir) {
        is CfirFunction -> {
            val typeParameterPart = "#tp${declaration.typeParameters.size}"
            val parameterPart = declaration.valueParameters.joinToString(
                prefix = "(",
                postfix = ")",
                separator = ",",
            ) { parameter ->
                parameter.returnTypeRef.toOverrideSignatureComponent()
            }
            "fun:${name.asString()}$typeParameterPart$parameterPart"
        }

        is CfirProperty -> "prop:${name.asString()}"
        else -> callableIdAsString()
    }
}

fun CfirCallableSymbol<*>.isStaticMemberForOverride(): Boolean =
    isBound && cfir.status.isStatic

private fun CfirTypeRef.toOverrideSignatureComponent(): String = when (this) {
    is CfirResolvedTypeRef -> coneType.renderForDebugging()
    else -> toString()
}
