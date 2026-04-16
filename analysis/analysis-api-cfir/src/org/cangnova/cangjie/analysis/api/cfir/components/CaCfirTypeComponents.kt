package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirClassLikeSymbolBase
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirType
import org.cangnova.cangjie.analysis.api.cfir.types.asCaType
import org.cangnova.cangjie.analysis.api.components.CaExpressionTypeProvider
import org.cangnova.cangjie.analysis.api.components.CaTypeInformationProvider
import org.cangnova.cangjie.analysis.api.components.CaTypeProvider
import org.cangnova.cangjie.analysis.api.components.CaTypeRelationChecker
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.types.CaClassLikeType
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaTypePointer
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjExpression

internal class CaCfirExpressionTypeProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaExpressionTypeProvider, CaCfirSessionComponent {
    override val CjExpression.expressionType: CaType?
        get() = withValidityAssertion {
            analysisSession.queryExpressionType(this@expressionType)?.asPublicType()
        }

    override val CjCallableDeclaration.returnType: CaType?
        get() = withValidityAssertion {
            analysisSession.queryDeclarationReturnType(this@returnType)?.asPublicType()
        }
}

internal class CaCfirTypeProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaTypeProvider, CaCfirSessionComponent {
    override val CaClassLikeSymbol.defaultType: CaType
        get() = withValidityAssertion {
            when (this@defaultType) {
                is CaCfirClassLikeSymbolBase<*> -> analysisSession.queryClassLikeDefaultType(backingSymbol)?.asPublicType()
                    ?: error("Cannot build default type for `${classId?.asString() ?: "<anonymous>"}`")
                else -> error("Only CFIR class-like symbols can expose defaultType: ${this@defaultType::class.simpleName}")
            }
        }
}

internal class CaCfirTypeInformationProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaTypeInformationProvider, CaCfirSessionComponent {
    override fun CaType.createPointer(): CaTypePointer<CaType> = withValidityAssertion {
        when (this@createPointer) {
            is CaCfirType -> createPointer() as CaTypePointer<CaType>
            else -> error("Only CFIR public types can create type pointers: ${this@createPointer::class.simpleName}")
        }
    }

    override val CaType.isErrorType: Boolean
        get() = withValidityAssertion {
            when (this@isErrorType) {
                is CaCfirType -> coneType.isError
                else -> error("Only CFIR public types can expose error flag: ${this@isErrorType::class.simpleName}")
            }
        }

    override val CaType.fullyExpandedType: CaType
        get() = withValidityAssertion {
            when (this@fullyExpandedType) {
                is CaCfirType -> coneType.fullyExpandedType(analysisSession.cfirSession).asCaType(analysisSession)
                else -> error("Only CFIR public types can expose fullyExpandedType: ${this@fullyExpandedType::class.simpleName}")
            }
        }

    override val CaType.classLikeSymbol: CaClassLikeSymbol?
        get() = withValidityAssertion {
            when (this@classLikeSymbol) {
                is CaClassLikeType -> symbol
                is CaCfirType -> analysisSession.queryTypeClassLikeSymbol(coneType)?.let(analysisSession::createClassLikeSymbol)
                else -> error("Only CFIR public types can resolve class-like symbols: ${this@classLikeSymbol::class.simpleName}")
            }
        }
}

internal class CaCfirTypeRelationChecker(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaTypeRelationChecker, CaCfirSessionComponent {
    override fun CaType.isSubTypeOf(superType: CaType): Boolean = withValidityAssertion {
        val subConeType = this@isSubTypeOf.requireCfirConeType("subtype check")
        val superConeType = superType.requireCfirConeType("subtype check")
        analysisSession.isSubTypeOf(subConeType, superConeType)
    }

    override fun CaType.semanticallyEquals(other: CaType): Boolean = withValidityAssertion {
        val leftConeType = this@semanticallyEquals.requireCfirConeType("type equality check")
        val rightConeType = other.requireCfirConeType("type equality check")
        analysisSession.areTypesEqual(leftConeType, rightConeType)
    }
}
