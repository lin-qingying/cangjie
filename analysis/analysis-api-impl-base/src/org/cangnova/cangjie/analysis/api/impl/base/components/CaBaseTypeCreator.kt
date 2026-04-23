package org.cangnova.cangjie.analysis.api.impl.base.components

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.components.CaClassTypeBuilder
import org.cangnova.cangjie.analysis.api.components.CaTypeCreator
import org.cangnova.cangjie.analysis.api.components.CaTypeParameterTypeBuilder
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaTypeProjection
import org.cangnova.cangjie.name.ClassId

@CaImplementationDetail
abstract class CaBaseTypeCreator<T : CaSession> : CaBaseSessionComponent<T>(), CaTypeCreator {



}

@CaImplementationDetail
sealed class CaBaseClassTypeBuilder : CaClassTypeBuilder {
    private val backingArguments = mutableListOf<CaTypeProjection>()



    override val arguments: List<CaTypeProjection> get() = withValidityAssertion { backingArguments }

    override fun argument(argument: CaTypeProjection): Unit = withValidityAssertion {
        backingArguments += argument
    }

    override fun argument(type: CaType ): Unit = withValidityAssertion {
        backingArguments += CaTypeProjection(type, type.token)
    }

    class ByClassId(classId: ClassId, override val token: CaLifetimeToken) : CaBaseClassTypeBuilder() {
        private val backingClassId: ClassId = classId

        val classId: ClassId get() = withValidityAssertion { backingClassId }
    }

    class BySymbol(symbol: CaClassLikeSymbol, override val token: CaLifetimeToken) : CaBaseClassTypeBuilder() {
        private val backingSymbol: CaClassLikeSymbol = symbol

        val symbol: CaClassLikeSymbol get() = withValidityAssertion { backingSymbol }
    }
}


@CaImplementationDetail
sealed class CaBaseTypeParameterTypeBuilder : CaTypeParameterTypeBuilder {

    class BySymbol(symbol: CaTypeParameterSymbol, override val token: CaLifetimeToken) : CaBaseTypeParameterTypeBuilder() {
        private val backingSymbol: CaTypeParameterSymbol = symbol

        val symbol: CaTypeParameterSymbol get() = withValidityAssertion { backingSymbol }
    }
}
