package org.cangnova.cangjie.analysis.api.components

import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.signatures.CaCallableSignature
import org.cangnova.cangjie.analysis.api.signatures.CaFunctionSignature
import org.cangnova.cangjie.analysis.api.signatures.CaVariableSignature
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol
import org.cangnova.cangjie.analysis.api.types.CaSubstitutor

/**
 * 公开 callable symbol 的 use-site 签名构造与替换入口。
 *
 * 这里直接对齐 Kotlin `KaSignatureSubstitutor` 的主路径：
 * 1. `asSignature()` 从公开 symbol 构造未替换签名；
 * 2. `substitute()` 在 symbol 维度统一套用公开 substitutor；
 * 3. function / variable 族保留精确返回类型。
 */
interface CaSignatureSubstitutor : CaLifetimeOwner {
    fun <S : CaCallableSymbol> S.asSignature(): CaCallableSignature<S>

    fun <S : CaFunctionSymbol> S.asSignature(): CaFunctionSignature<S>

    fun <S : CaVariableSymbol> S.asSignature(): CaVariableSignature<S>

    fun <S : CaCallableSymbol> S.substitute(substitutor: CaSubstitutor): CaCallableSignature<S>

    fun <S : CaFunctionSymbol> S.substitute(substitutor: CaSubstitutor): CaFunctionSignature<S>

    fun <S : CaVariableSymbol> S.substitute(substitutor: CaSubstitutor): CaVariableSignature<S>
}

context(session: CaSession)
fun <S : CaCallableSymbol> S.asSignature(): CaCallableSignature<S> {
    return with(session) {
        asSignature()
    }
}

context(session: CaSession)
fun <S : CaFunctionSymbol> S.asSignature(): CaFunctionSignature<S> {
    return with(session) {
        asSignature()
    }
}

context(session: CaSession)
fun <S : CaVariableSymbol> S.asSignature(): CaVariableSignature<S> {
    return with(session) {
        asSignature()
    }
}

context(session: CaSession)
fun <S : CaCallableSymbol> S.substitute(substitutor: CaSubstitutor): CaCallableSignature<S> {
    return with(session) {
        substitute(substitutor)
    }
}

context(session: CaSession)
fun <S : CaFunctionSymbol> S.substitute(substitutor: CaSubstitutor): CaFunctionSignature<S> {
    return with(session) {
        substitute(substitutor)
    }
}

context(session: CaSession)
fun <S : CaVariableSymbol> S.substitute(substitutor: CaSubstitutor): CaVariableSignature<S> {
    return with(session) {
        substitute(substitutor)
    }
}
