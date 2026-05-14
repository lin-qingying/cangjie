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
    /**
     * 把 callable symbol 转为未替换的签名,作为后续替换/分析的载体。
     */
    fun <S : CaCallableSymbol> S.asSignature(): CaCallableSignature<S>

    /**
     * 把 function symbol 转为未替换的签名,保留函数级别的精确返回类型。
     */
    fun <S : CaFunctionSymbol> S.asSignature(): CaFunctionSignature<S>

    /**
     * 把 variable symbol 转为未替换的签名,保留变量级别的精确类型。
     */
    fun <S : CaVariableSymbol> S.asSignature(): CaVariableSignature<S>

    /**
     * 用给定 [substitutor] 替换 callable 签名中的类型变量,得到 use-site 视图下的签名。
     */
    fun <S : CaCallableSymbol> S.substitute(substitutor: CaSubstitutor): CaCallableSignature<S>

    /**
     * 用给定 [substitutor] 替换函数签名中的类型变量。
     */
    fun <S : CaFunctionSymbol> S.substitute(substitutor: CaSubstitutor): CaFunctionSignature<S>

    /**
     * 用给定 [substitutor] 替换变量签名中的类型变量。
     */
    fun <S : CaVariableSymbol> S.substitute(substitutor: CaSubstitutor): CaVariableSignature<S>
}

/**
 * 顶层桥接:在当前 [CaSession] 上下文中把 callable symbol 转为未替换签名。
 */
context(session: CaSession)
fun <S : CaCallableSymbol> S.asSignature(): CaCallableSignature<S> {
    return with(session) {
        asSignature()
    }
}

/**
 * 顶层桥接:在当前 [CaSession] 上下文中把 function symbol 转为未替换签名。
 */
context(session: CaSession)
fun <S : CaFunctionSymbol> S.asSignature(): CaFunctionSignature<S> {
    return with(session) {
        asSignature()
    }
}

/**
 * 顶层桥接:在当前 [CaSession] 上下文中把 variable symbol 转为未替换签名。
 */
context(session: CaSession)
fun <S : CaVariableSymbol> S.asSignature(): CaVariableSignature<S> {
    return with(session) {
        asSignature()
    }
}

/**
 * 顶层桥接:在当前 [CaSession] 上下文中,使用 [substitutor] 替换 callable 签名的类型变量。
 */
context(session: CaSession)
fun <S : CaCallableSymbol> S.substitute(substitutor: CaSubstitutor): CaCallableSignature<S> {
    return with(session) {
        substitute(substitutor)
    }
}

/**
 * 顶层桥接:在当前 [CaSession] 上下文中,使用 [substitutor] 替换函数签名的类型变量。
 */
context(session: CaSession)
fun <S : CaFunctionSymbol> S.substitute(substitutor: CaSubstitutor): CaFunctionSignature<S> {
    return with(session) {
        substitute(substitutor)
    }
}

/**
 * 顶层桥接:在当前 [CaSession] 上下文中,使用 [substitutor] 替换变量签名的类型变量。
 */
context(session: CaSession)
fun <S : CaVariableSymbol> S.substitute(substitutor: CaSubstitutor): CaVariableSignature<S> {
    return with(session) {
        substitute(substitutor)
    }
}
