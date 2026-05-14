package org.cangnova.cangjie.analysis.api.signatures

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol
import org.cangnova.cangjie.analysis.api.types.CaSubstitutor
import org.cangnova.cangjie.name.Name


/**
 * 变量符号的 use-site 签名视图,同时实现 [CaValueParameterSignature]。
 *
 * 对齐 Kotlin Analysis API 的 `KaVariableSignature`。
 *
 * @param S 实际变量符号类型(协变)。
 */
@SubclassOptInRequired(CaImplementationDetail::class)
interface CaVariableSignature<out S : CaVariableSymbol> : CaCallableSignature<S>, CaValueParameterSignature {
    /**
     * 考虑了 `@ParameterName` 注解的参数名,可能与 [CaVariableSymbol.name] 不同。
     *
     * 某些变量名会被 `@ParameterName(name = "newName")` 等特殊注解改写,
     * 用以保留 lambda 参数名等信息。例如:
     *
     * ```
     * // 已编译库
     * fun foo(): (bar: String) -> Unit { ... }
     *
     * // 源码
     * fun test() {
     *   val action = foo()
     *   action("") // 此处调用
     * }
     * ```
     *
     * `action("")` 的符号会指向 `Function1<P1, R>.invoke(p1: P1): R`,
     * 因为 use-site 替换被刻意 unwrap,这样 `symbol.name` 会得到 `"p1"` 而非 `"bar"`。
     * 通过 [name] 即可拿到结合 `@ParameterName` 之后的预期参数名 `"bar"`。
     */
    override val name: Name

    /**
     * 变量签名特化的替换入口,返回 [CaVariableSignature] 以保留精确类型。
     */
    @CaExperimentalApi
    abstract override fun substitute(substitutor: CaSubstitutor): CaVariableSignature<S>
}
