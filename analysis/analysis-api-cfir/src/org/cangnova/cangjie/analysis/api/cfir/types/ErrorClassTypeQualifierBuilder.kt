package org.cangnova.cangjie.analysis.api.cfir.types

import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.types.CaResolvedClassTypeQualifier
import org.cangnova.cangjie.analysis.api.types.CaClassTypeQualifier
import org.cangnova.cangjie.analysis.api.types.CaTypeProjection
import org.cangnova.cangjie.cfir.diagnostic.ConeUnmatchedTypeArgumentsError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedNameError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedReferenceError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedSymbolError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedTypeQualifierError
import org.cangnova.cangjie.cfir.resolve.getContainingClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol

/**
 * 对齐 Kotlin `ErrorClassTypeQualifierBuilder`：
 * 将 class-like error diagnostic 还原成 public qualifier 列表。
 */
internal object ErrorClassTypeQualifierBuilder {
    /**
     * 对齐 Kotlin `createQualifiersForUnresolvedType`：
     * unresolved 分支直接按 diagnostic 中保留的语法限定符恢复 public qualifier。
     */
    fun createQualifiersForUnresolvedType(
        diagnostic: ConeUnresolvedError,
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

        }
    }

    /**
     * 对齐 Kotlin `createQualifiersForUnmatchedTypeArgumentsType`：
     * 类型实参数量不匹配时，qualifier 只能从已解析的 class-like symbol 链恢复，
     * 不能再从 `ConeErrorType` 上抽 type arguments，否则会把非 class-like error type
     * 误当成普通 class-like type。
     */
    fun createQualifiersForUnmatchedTypeArgumentsType(
        diagnostic: ConeUnmatchedTypeArgumentsError,
        builder: CaSymbolByCfirBuilder,
    ): List<CaClassTypeQualifier> {
        return createQualifiersByClassSymbol(diagnostic.symbol, builder)
    }

    private fun createQualifiersByClassSymbol(
        symbol: CfirClassLikeSymbol<*>,
        builder: CaSymbolByCfirBuilder,
    ): List<CaResolvedClassTypeQualifier> {
        return generateSequence(symbol) { currentSymbol ->
            currentSymbol.getContainingClassSymbol()
        }.mapTo(mutableListOf()) { classSymbol ->
            val publicSymbol = builder.classifierBuilder.buildClassLikeSymbol(classSymbol)
            CaCfirResolvedClassTypeQualifierImpl(
                name = publicSymbol.name,
                typeArguments = emptyList(),
                symbol = publicSymbol,
                token = builder.token,
            )
        }
    }
}
