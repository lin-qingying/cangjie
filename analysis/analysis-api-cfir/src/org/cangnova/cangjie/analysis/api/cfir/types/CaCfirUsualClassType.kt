package org.cangnova.cangjie.analysis.api.cfir.types

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.CaSymbolByCfirBuilder
import org.cangnova.cangjie.analysis.api.cfir.utils.asPublicTypeProjections
import org.cangnova.cangjie.analysis.api.cfir.utils.buildAbbreviatedType
import org.cangnova.cangjie.analysis.api.cfir.utils.createTypePointer
import org.cangnova.cangjie.analysis.api.cfir.utils.restoreUsualClassType
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.types.CaResolvedClassTypeQualifier
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaTypePointer
import org.cangnova.cangjie.analysis.api.types.CaUsualClassType
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.errorWithCfirSpecificEntries
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassifierType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.renderForDebugging
import org.cangnova.cangjie.name.ClassId

/**
 * 仓颉常规 class-like public type。
 *
 * 对类型别名场景，这里保留别名本身作为 public type；
 * `fullyExpandedType` 仍由 type component 侧负责展开。
 */
internal class CaCfirUsualClassType(
    /**
     * 底层 CFIR class-like 类型。
     */
    override val coneType: ConeCangJieType,
    /**
     * 用于解析 class-like 符号和构造公开类型实参的 CFIR builder。
     */
    private val builder: CaSymbolByCfirBuilder,
) : CaUsualClassType(), CaCfirType {
    /**
     * 当前公开类型的生命周期令牌。
     */
    override val token
        get() = builder.token

    /**
     * 面向调试和展示的 class-like 类型文本。
     */
    override val presentation: String
        get() = withValidityAssertion { coneType.renderForDebugging() }

    /**
     * usual class type 当前不携带类型注解。
     */
    override val annotations: CaAnnotationList
        get() = withValidityAssertion { emptyTypeAnnotations(token) }

    /**
     * 当前类型对应的缩写类型。
     */
    override val abbreviation: CaUsualClassType?
        get() = withValidityAssertion { builder.buildAbbreviatedType(coneType) }

    /**
     * 当前 class-like 类型的 ClassId。
     */
    override val classId: ClassId
        get() = withValidityAssertion {
            coneType.classIdOrPrimitiveClassId
                ?: error("Only class-like CFIR types can expose ClassId: ${coneType::class.simpleName}")
        }

    /**
     * 当前 class-like 类型对应的公开符号。
     */
    override val symbol: CaClassLikeSymbol
        get() = withValidityAssertion {
            resolveClassLikeSymbol()?.let(builder.classifierBuilder::buildClassLikeSymbol)
                ?: errorWithCfirSpecificEntries("Class was not found", coneType = coneType)
        }

    /**
     * 当前类型的限定名片段列表。
     */
    override val qualifiers: List<CaResolvedClassTypeQualifier>
        get() = withValidityAssertion {
            when (coneType) {
                is ConeClassifierType -> UsualClassTypeQualifierBuilder.buildQualifiers(coneType, builder)
                is ConeTypeAliasType -> listOf(
                    CaCfirResolvedClassTypeQualifierImpl(
                        name = symbol.name,
                        typeArguments = coneType.asPublicTypeProjections(builder.analysisSession),
                        symbol = symbol,
                        token = symbol.token,
                    )
                )

                else -> error("Unsupported usual class type qualifier source: ${coneType::class.simpleName}")
            }
        }

    /**
     * 最内层 qualifier 暴露的类型实参。
     */
    override val typeArguments: List<CaType>
        get() = withValidityAssertion {
            qualifiers.last().typeArguments.mapNotNull { projection -> projection.type }
        }

    /**
     * 创建可跨会话恢复该 usual class type 的指针。
     */
    override fun createPointer(): CaTypePointer<CaUsualClassType> = withValidityAssertion {
        createTypePointer(coneType, ::restoreUsualClassType)
    }

    /**
     * 按底层 Cone 类型判断公开类型相等性。
     */
    override fun equals(other: Any?) = typeEquals(other)

    /**
     * 返回底层 Cone 类型的哈希码。
     */
    override fun hashCode() = typeHashcode()

    /**
     * 返回底层 Cone 类型调试文本。
     */
    override fun toString(): String = coneType.renderForDebugging()

    /**
     * 从底层 Cone 类型恢复对应的 CFIR class-like 符号。
     */
    private fun resolveClassLikeSymbol(): CfirClassLikeSymbol<*>? = when (coneType) {
        is ConeClassifierType -> coneType.lookupTag.toSymbol(builder.analysisSession.cfirSession)
        is ConeTypeAliasType -> coneType.classId.toSymbol(builder.analysisSession.cfirSession)
        else -> null
    }
}
