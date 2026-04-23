/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.api.signatures

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.types.CaSubstitutor
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.CallableId


/**
 * A use-site signature for a [callable symbol][CaCallableSymbol]. Compared to the symbol, the signature carries additional use-site type
 * information.
 *
 * The equality of [CaCallableSignature] is derived from its content.
 *
 * #### Example
 *
 * ```kotlin
 * fun test(l: List<String>) {
 *   l.get(1)
 * }
 * ```
 *
 * The [callable symbol][CaCallableSymbol] for `get` has the type `(Int) -> T` where `T` is the type parameter declared in `List`. On the
 * other hand, a [CaCallableSignature] for `l.get` carries the instantiated type information `(Int) -> String`.
 */
@OptIn(CaImplementationDetail::class)
public sealed interface CaCallableSignature<out S : CaCallableSymbol> : CaLifetimeOwner {
    /**
     * The underlying symbol which the signature carries use-site information about.
     */
    public val symbol: S

    /**
     * The use-site-substituted [return type][CaCallableSymbol.returnType].
     */
    public val returnType: CaType

    /**
     * The use-site-substituted [extension receiver type][CaCallableSymbol.receiverParameter].
     */
    public val receiverType: CaType?

    /**
     * The [CallableId] of the signature, corresponding to the symbol's callable ID.
     */
    public val callableId: CallableId? get() = withValidityAssertion { symbol.callableId }


    /**
     * Applies the given [substitutor] to the signature, returning a new signature with substituted types.
     *
     * @see CaSubstitutor.substitute
     */
    @CaExperimentalApi
    public fun substitute(substitutor: CaSubstitutor): CaCallableSignature<S>

    abstract override fun equals(other: Any?): Boolean
    abstract override fun hashCode(): Int
}
