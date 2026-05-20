package org.cangnova.cangjie.analysis.api.impl.base.components

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.components.CaTypeProvider
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.types.CaClassLikeType
import org.cangnova.cangjie.analysis.api.types.CaType

/**
 * 对齐 Kotlin `KaBaseTypeProvider` 的共享实现落位。
 */
@CaImplementationDetail
abstract class CaBaseTypeProvider<T : CaSession> : CaBaseSessionComponent<T>(), CaTypeProvider {
    override val CaValueParameterSymbol.varargArrayType: CaType?
        get() = withValidityAssertion {
            if (!isVararg) {
                return null
            }

            val arrayType = returnType as? CaClassLikeType ?: return null
            val elementType = arrayType.typeArguments.singleOrNull() ?: return null
            return analysisSession.buildVarargArrayType(elementType)
        }
}
