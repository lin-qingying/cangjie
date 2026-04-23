package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaClassTypeBuilder
import org.cangnova.cangjie.analysis.api.components.CaTypeCreator
import org.cangnova.cangjie.analysis.api.components.CaTypeParameterTypeBuilder
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseTypeCreator
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
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumSymbol
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPrimitiveTypeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirStructSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.types.ConeAttributes
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
import org.cangnova.cangjie.cfir.types.ConeUnionType
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
    ): CaType {
        TODO("Not yet implemented")
    }

    override fun buildClassType(
        symbol: CaClassLikeSymbol,
        init: CaClassTypeBuilder.() -> Unit
    ): CaType {
        TODO("Not yet implemented")
    }

    override fun buildTypeParameterType(
        symbol: CaTypeParameterSymbol,
        init: CaTypeParameterTypeBuilder.() -> Unit
    ): CaTypeParameterType {
        TODO("Not yet implemented")
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
}
