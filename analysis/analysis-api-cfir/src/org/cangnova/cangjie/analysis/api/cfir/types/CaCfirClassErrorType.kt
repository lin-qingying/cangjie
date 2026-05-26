package org.cangnova.cangjie.analysis.api.cfir.types

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.cfir.utils.buildAbbreviatedType
import org.cangnova.cangjie.analysis.api.cfir.utils.createTypePointer
import org.cangnova.cangjie.analysis.api.cfir.utils.restoreClassErrorType
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.types.CaClassErrorType
import org.cangnova.cangjie.analysis.api.types.CaClassTypeQualifier
import org.cangnova.cangjie.analysis.api.types.CaTypePointer
import org.cangnova.cangjie.cfir.diagnostic.ConeDiagnosticWithCandidates
import org.cangnova.cangjie.cfir.diagnostic.ConeUnmatchedTypeArgumentsError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedError
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.types.ConeDiagnostic
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.renderForDebugging

/**
 * 仓颉 class-like error public type 叶子。
 *
 * 对齐 Kotlin `KaFirClassErrorType`：尽量保留 qualifier、候选符号与可展示文本，
 * 不把未解析类型退化成单一字符串错误。
 */
internal class CaCfirClassErrorType(
    override val coneType: ConeErrorType,
    private val coneDiagnostic: ConeDiagnostic,
    private val builder: CaSymbolByCfirBuilder,
) : CaClassErrorType(), CaCfirType {
    override val token: CaLifetimeToken
        get() = builder.token


    override val presentation: String
        get() = withValidityAssertion { coneType.renderForDebugging() }

    override val annotations: CaAnnotationList
        get() = withValidityAssertion { emptyTypeAnnotations(token) }

    override val abbreviation: org.cangnova.cangjie.analysis.api.types.CaUsualClassType?
        get() = withValidityAssertion { builder.buildAbbreviatedType(coneType) }

    override val errorMessage: String
        get() = withValidityAssertion { coneDiagnostic.reason }

    override val presentableText: String?
        get() = withValidityAssertion {
            qualifiers.takeIf { it.isNotEmpty() }?.joinToString(".") { qualifier ->
                buildString {
                    append(qualifier.name.asString())
                    if (qualifier.typeArguments.isNotEmpty()) {
                        append('<')
                        append(
                            qualifier.typeArguments.joinToString(",") { projection ->
                                val type = projection.type
                                when (type) {
                                    null -> "*"
                                    is CaCfirType -> type.coneType.renderForDebugging()
                                    else -> type.toString()
                                }
                            }
                        )
                        append('>')
                    }
                }
            } ?: coneType.delegatedType?.renderForDebugging()
        }

    override val qualifiers: List<CaClassTypeQualifier>
        get() = withValidityAssertion {
            when (coneDiagnostic) {
                is ConeUnresolvedError ->
                    ErrorClassTypeQualifierBuilder.createQualifiersForUnresolvedType(coneDiagnostic, builder)

                is ConeUnmatchedTypeArgumentsError ->
                    ErrorClassTypeQualifierBuilder.createQualifiersForUnmatchedTypeArgumentsType(coneDiagnostic, builder)

                else -> error("Unsupported ${coneDiagnostic::class}")
            }
        }

    override val candidateSymbols: Collection<CaClassLikeSymbol>
        get() = withValidityAssertion {
            when (val diagnostic = coneDiagnostic) {
                is ConeDiagnosticWithCandidates -> diagnostic.candidateSymbols
                    .filterIsInstance<CfirClassLikeSymbol<*>>()
                    .map { symbol -> builder.classifierBuilder.buildClassLikeSymbol(symbol) }

                is ConeUnmatchedTypeArgumentsError ->
                    listOf(builder.classifierBuilder.buildClassLikeSymbol(diagnostic.symbol))

                else -> emptyList()
            }
        }

    override fun createPointer(): CaTypePointer<CaClassErrorType> = withValidityAssertion {
        createTypePointer(coneType, ::restoreClassErrorType)
    }

    override fun equals(other: Any?) = typeEquals(other)

    override fun hashCode() = typeHashcode()

    override fun toString(): String = coneType.renderForDebugging()
}
