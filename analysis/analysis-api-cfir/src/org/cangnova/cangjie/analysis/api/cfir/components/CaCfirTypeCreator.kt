package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaClassTypeBuilder
import org.cangnova.cangjie.analysis.api.components.CaTypeCreator
import org.cangnova.cangjie.analysis.api.components.CaTypeParameterTypeBuilder
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseTypeCreator
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseClassTypeBuilder
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseTypeParameterTypeBuilder
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirSymbol
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.types.CaClassLikeType
import org.cangnova.cangjie.analysis.api.types.CaFunctionType
import org.cangnova.cangjie.analysis.api.types.CaIntersectionType
import org.cangnova.cangjie.analysis.api.types.CaTupleType
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaTypeParameterType
import org.cangnova.cangjie.analysis.api.types.CaUnionType
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedSymbolError
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumSymbol
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPrimitiveTypeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirStructSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol as CfirClassLikeSymbolBase
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.types.ConeAttributes
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
import org.cangnova.cangjie.cfir.types.ConeUnionType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.name.ClassId

/**
 * 对齐 Kotlin `CaCfirTypeCreator` 的组件落位。
 *
 * 所有公开类型构造都必须直接绑定当前 use-site session 的 CFIR 类型系统，
 * 避免外部自行拼接低层 `Cone*Type` 后再倒灌回 Analysis API。
 */
@OptIn(CaImplementationDetail::class)
internal class CaCfirTypeCreator(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseTypeCreator<CaCfirSession>(), CaCfirSessionComponent {
    override fun buildClassType(
        classId: ClassId,
        init: CaClassTypeBuilder.() -> Unit
    ): CaType = withValidityAssertion {
        buildClassType(CaBaseClassTypeBuilder.ByClassId(classId, token).apply(init))
    }

    override fun buildClassType(
        symbol: CaClassLikeSymbol,
        init: CaClassTypeBuilder.() -> Unit
    ): CaType = withValidityAssertion {
        buildClassType(CaBaseClassTypeBuilder.BySymbol(symbol, token).apply(init))
    }

    override fun buildTypeParameterType(
        symbol: CaTypeParameterSymbol,
        init: CaTypeParameterTypeBuilder.() -> Unit
    ): CaTypeParameterType = withValidityAssertion {
        val builder = CaBaseTypeParameterTypeBuilder.BySymbol(symbol, token).apply(init)
        val cfirSymbol = builder.symbol.requireCfirTypeParameterSymbol()
        ConeTypeParameterTypeImpl(cfirSymbol.toLookupTag()).asPublicType() as CaTypeParameterType
    }


    override fun buildFunctionType(
        parameterTypes: List<CaType>,
        returnType: CaType,
        isCFunction: Boolean,
        isClosureType: Boolean,
        hasVariableLengthArgument: Boolean,
    ): CaFunctionType = withValidityAssertion {
        ConeFunctionType(
            parameterTypes = parameterTypes.asConeTypes("函数类型构造"),
            returnType = returnType.requireCfirConeType("函数类型构造"),
            isCFunc = isCFunction,
            isClosureType = isClosureType,
            hasVariableLenArg = hasVariableLengthArgument,
            attributes = ConeAttributes.Empty,
        ).asPublicType() as CaFunctionType
    }

    override fun buildTupleType(elementTypes: List<CaType>): CaTupleType = withValidityAssertion {
        ConeTupleType(
            elementTypes = elementTypes.asConeTypes("元组类型构造"),
            attributes = ConeAttributes.Empty,
        ).asPublicType() as CaTupleType
    }

    override fun buildIntersectionType(conjuncts: List<CaType>): CaIntersectionType = withValidityAssertion {
        require(conjuncts.isNotEmpty()) { "交叉类型至少需要一个组成类型" }
        ConeIntersectionType(
            intersectedTypes = conjuncts.asConeTypes("交叉类型构造"),
            attributes = ConeAttributes.Empty,
        ).asPublicType() as CaIntersectionType
    }

    override fun buildUnionType(alternatives: Collection<CaType>): CaUnionType = withValidityAssertion {
        require(alternatives.isNotEmpty()) { "联合类型至少需要一个组成类型" }
        ConeUnionType(
            unionTypes = alternatives.map { type -> type.requireCfirConeType("联合类型构造") }.toSet(),
            attributes = ConeAttributes.Empty,
        ).asPublicType() as CaUnionType
    }

    /**
     * 把公开 `CaType` 列表统一转换为底层 Cone 类型。
     */
    private fun Iterable<CaType>.asConeTypes(owner: String): List<ConeCangJieType> =
        map { type -> type.requireCfirConeType(owner) }

    /**
     * 当前仓颉 Analysis API 还没有公开 variance DSL，
     * 因此这里严格按“不变投影”构造类型实参。
     */
    private fun Iterable<CaType>.asConeTypeArguments(owner: String): List<ConeTypeProjection> =
        asConeTypes(owner)

    private fun buildClassType(builder: CaBaseClassTypeBuilder): CaType {
        val typeArguments = builder.arguments.map { projection ->
            val type = projection.type
                ?: error("仓颉类型构造不支持空类型实参")
            type.requireCfirConeType("class type construction")
        }

        val symbol = when (builder) {
            is CaBaseClassTypeBuilder.ByClassId ->
                analysisSession.cfirSession.symbolProvider.getClassLikeSymbolByClassId(builder.classId)
                    ?: return ConeErrorType(ConeUnresolvedSymbolError(builder.classId)).asPublicType()

            is CaBaseClassTypeBuilder.BySymbol -> builder.symbol.requireCfirClassLikeSymbol()
        }

        return symbol.createResolvedClassLikeType(typeArguments).asPublicType()
    }

    /**
     * 仓颉 class-like 构造在 Kotlin `createSimpleType(lookupTag, arguments, ...)`
     * 的框架位置上，按仓颉官方类型种类落到不同 Cone 类型。
     */
    private fun CfirClassLikeSymbolBase<*>.createResolvedClassLikeType(
        typeArguments: List<ConeTypeProjection>,
    ): ConeCangJieType = when (this) {
        is CfirPrimitiveTypeSymbol -> ConePrimitiveType(kind)
        is CfirClassSymbol -> ConeClassLikeType(
            lookupTag = toLookupTag(),
            typeArguments = typeArguments,
        )
        is CfirInterfaceSymbol -> ConeClassLikeType(
            lookupTag = toLookupTag(),
            typeArguments = typeArguments,
            isInterface = true,
        )
        is CfirStructSymbol -> ConeStructType(
            lookupTag = toLookupTag(),
            typeArguments = typeArguments,
        )
        is CfirEnumSymbol -> ConeEnumType(
            lookupTag = toLookupTag(),
            typeArguments = typeArguments,
            isRefEnum = isRefEnum,
        )
        is CfirTypeAliasSymbol -> ConeTypeAliasType(
            classId = classId,
            expandedType = (cfir as CfirTypeAlias).expandedTypeRef.coneTypeOrNull,
            typeArguments = typeArguments,
        )
    }

    private fun CaClassLikeSymbol.requireCfirClassLikeSymbol(): CfirClassLikeSymbolBase<*> {
        val cfirSymbol = (this as? CaCfirSymbol<*>)?.cfirSymbol
        return cfirSymbol as? CfirClassLikeSymbolBase<*>
            ?: error("Expected CFIR class-like symbol, but got ${this::class.simpleName}")
    }

    private fun CaTypeParameterSymbol.requireCfirTypeParameterSymbol(): CfirTypeParameterSymbol {
        val cfirSymbol = (this as? CaCfirSymbol<*>)?.cfirSymbol
        return cfirSymbol as? CfirTypeParameterSymbol
            ?: error("Expected CFIR type parameter symbol, but got ${this::class.simpleName}")
    }
}
