package org.cangnova.cangjie.analysis.api.cfir.types

import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.types.CaClassTypeQualifier
import org.cangnova.cangjie.analysis.api.types.CaTypeProjection
import org.cangnova.cangjie.cfir.diagnostic.ConeUnmatchedTypeArgumentsError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedNameError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedReferenceError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedSymbolError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedTypeQualifierError
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.types.ConeErrorType

/**
 * 对齐 Kotlin `ErrorClassTypeQualifierBuilder`：
 * 将 class-like error diagnostic 还原成 public qualifier 列表。
 */
internal object ErrorClassTypeQualifierBuilder {
    fun buildQualifiers(
        coneType: ConeErrorType,
        diagnostic: org.cangnova.cangjie.cfir.types.ConeDiagnostic,
        builder: CaSymbolByCfirBuilder,
    ): List<CaClassTypeQualifier> {
        return when (diagnostic) {
            is ConeUnresolvedTypeQualifierError -> diagnostic.qualifiers.map { qualifier ->
                CaCfirUnresolvedClassTypeQualifierImpl(
                    name = qualifier.name,
                    typeArguments = qualifier.typeArguments.map { typeRef ->
                        CaTypeProjection(
                            type = builder.typeBuilder.buildType(typeRef),
                            token = builder.token,
                        )
                    },
                    token = builder.token,
                )
            }

            is ConeUnresolvedSymbolError -> diagnostic.classId.asSingleFqName().pathSegments().map { segment ->
                CaCfirUnresolvedClassTypeQualifierImpl(
                    name = segment,
                    typeArguments = emptyList(),
                    token = builder.token,
                )
            }

            is ConeUnresolvedNameError -> listOf(
                CaCfirUnresolvedClassTypeQualifierImpl(
                    name = diagnostic.name,
                    typeArguments = emptyList(),
                    token = builder.token,
                )
            )

            is ConeUnresolvedReferenceError -> listOf(
                CaCfirUnresolvedClassTypeQualifierImpl(
                    name = diagnostic.name,
                    typeArguments = emptyList(),
                    token = builder.token,
                )
            )

            is ConeUnmatchedTypeArgumentsError -> createResolvedQualifiers(coneType, diagnostic.symbol, builder)
            else -> emptyList()
        }
    }

    private fun createResolvedQualifiers(
        coneType: ConeErrorType,
        symbol: CfirClassLikeSymbol<*>,
        builder: CaSymbolByCfirBuilder,
    ): List<CaClassTypeQualifier> {
        val publicSymbol = builder.classifierBuilder.buildClassLikeSymbol(symbol)
        return listOf(
            CaCfirResolvedClassTypeQualifierImpl(
                name = publicSymbol.name,
                typeArguments = builder.typeBuilder.buildTypeProjections(coneType),
                symbol = publicSymbol,
                token = builder.token,
            )
        )
    }
}
