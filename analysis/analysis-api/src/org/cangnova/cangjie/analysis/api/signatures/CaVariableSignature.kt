package org.cangnova.cangjie.analysis.api.signatures

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol
import org.cangnova.cangjie.analysis.api.types.CaSubstitutor
import org.cangnova.cangjie.name.Name


/**
 * A [callable signature][CaCallableSignature] of a [variable symbol][CaVariableSymbol].
 */
@SubclassOptInRequired(CaImplementationDetail::class)
public interface CaVariableSignature<out S : CaVariableSymbol> : CaCallableSignature<S>, CaValueParameterSignature {
    /**
     * The name of the variable with respect to a [ParameterName] annotation. It can be different from [CaVariableSymbol.name].
     *
     * Some variables can have their names changed by special annotations like `@ParameterName(name = "newName")`. This is used to preserve
     * the names of the lambda parameters in situations like this:
     *
     * ```
     * // compiled library
     * fun foo(): (bar: String) -> Unit { ... }
     *
     * // source code
     * fun test() {
     *   val action = foo()
     *   action("") // this call
     * }
     * ```
     *
     * Unfortunately, the [symbol] for the `action("")` call will be pointing to `Function1<P1, R>.invoke(p1: P1): R`, because we
     * intentionally unwrap use-site substitution overrides. Because of this, `symbol.name` will yield `"p1"`, and not `"bar"`.
     *
     * To overcome this problem, [name] allows to get the intended name of the parameter, with respect to the `@ParameterName` annotation.
     */
    public override val name: Name

    @CaExperimentalApi
    abstract override fun substitute(substitutor: CaSubstitutor): CaVariableSignature<S>
}
