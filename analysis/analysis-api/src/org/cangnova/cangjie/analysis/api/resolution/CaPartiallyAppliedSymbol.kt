package org.cangnova.cangjie.analysis.api.resolution

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.signatures.CaCallableSignature
import org.cangnova.cangjie.analysis.api.signatures.CaFunctionSignature
import org.cangnova.cangjie.analysis.api.signatures.CaVariableSignature
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol


/**
 * A callable symbol partially applied with receivers and type arguments. Essentially, this is a call that misses some information. For
 * properties, the missing information is the type of access (read, write, or compound access) to this property. For functions, the missing
 * information is the value arguments for the call.
 */
@SubclassOptInRequired(CaImplementationDetail::class)
public interface CaPartiallyAppliedSymbol<out S : CaCallableSymbol, out C : CaCallableSignature<S>> : CaLifetimeOwner {
    /**
     * The function or variable declaration.
     */
    public val signature: C

    /**
     * The [dispatch receiver](https://kotlin.github.io/analysis-api/receivers.html#types-of-receivers) for this symbol access. A dispatch
     * receiver is available if the callable is declared inside a class or object.
     */
    public val dispatchReceiver: CaReceiverValue?




}


public typealias CaPartiallyAppliedFunctionSymbol<S> = CaPartiallyAppliedSymbol<S, CaFunctionSignature<S>>

public typealias CaPartiallyAppliedVariableSymbol<S> = CaPartiallyAppliedSymbol<S, CaVariableSignature<S>>
