package org.cangnova.cangjie.analysis.api.cfir.components

import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirClassLikeSymbolBase
import org.cangnova.cangjie.analysis.api.components.CaAnalysisScopeProvider
import org.cangnova.cangjie.analysis.api.components.CaSignatureSubstitutor
import org.cangnova.cangjie.analysis.api.components.CaSubstitutorProvider
import org.cangnova.cangjie.analysis.api.components.CaTypeCreator
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.signatures.CaSignature
import org.cangnova.cangjie.analysis.api.substitution.CaSubstitutedSignature
import org.cangnova.cangjie.analysis.api.substitution.CaTypeSubstitutor
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.types.CaClassLikeType
import org.cangnova.cangjie.analysis.api.types.CaFunctionType
import org.cangnova.cangjie.analysis.api.types.CaIntersectionType
import org.cangnova.cangjie.analysis.api.types.CaTupleType
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaUnionType
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumSymbol
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPrimitiveTypeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirStructSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.types.ConeAttributes
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeFuncType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
import org.cangnova.cangjie.cfir.types.ConeUnionType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

/**
 * 公开类型构造器。
 *
 * 所有构造入口都直接绑定当前 session 的 CFIR 类型系统，
 * 避免外部自行拼装低层对象。
 */
internal class CaCfirTypeCreator(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaTypeCreator, CaCfirSessionComponent {
    override fun buildClassLikeType(
        classId: ClassId,
        typeArguments: List<CaType>,
    ): CaClassLikeType = withValidityAssertion {
        val classLikeSymbol = analysisSession.getClassLikePublicSymbol(classId)
            ?: error("当前 use-site session 中不可见的 class-like 声明无法构造类型：`${classId.asString()}`")
        buildClassLikeType(classLikeSymbol, typeArguments)
    }

    override fun buildClassLikeType(
        symbol: CaClassLikeSymbol,
        typeArguments: List<CaType>,
    ): CaClassLikeType = withValidityAssertion {
        val cfirClassLikeSymbol = symbol as? CaCfirClassLikeSymbolBase<*>
            ?: error("仅支持通过 CFIR class-like 符号构造类型：${symbol::class.simpleName}")
        val coneArguments = typeArguments.asConeTypeArguments("class-like 类型构造")
        val coneType = when (val backingSymbol = cfirClassLikeSymbol.backingSymbol) {
            is CfirClassSymbol -> ConeClassLikeType(backingSymbol.toLookupTag(), coneArguments, ConeAttributes.Empty)
            is CfirInterfaceSymbol -> ConeClassLikeType(
                lookupTag = backingSymbol.toLookupTag(),
                typeArguments = coneArguments,
                attributes = ConeAttributes.Empty,
                isInterface = true,
            )
            is CfirStructSymbol -> ConeStructType(backingSymbol.toLookupTag(), coneArguments, ConeAttributes.Empty)
            is CfirEnumSymbol -> ConeEnumType(
                lookupTag = backingSymbol.toLookupTag(),
                typeArguments = coneArguments,
                attributes = ConeAttributes.Empty,
                isRefEnum = backingSymbol.isRefEnum,
            )
            is CfirTypeAliasSymbol -> ConeTypeAliasType(
                classId = backingSymbol.classId,
                typeArguments = coneArguments,
                attributes = ConeAttributes.Empty,
            )
            is CfirPrimitiveTypeSymbol -> {
                require(coneArguments.isEmpty()) {
                    "原始类型 `${backingSymbol.kind.typeName}` 不能携带类型实参"
                }
                ConePrimitiveType(backingSymbol.kind, ConeAttributes.Empty)
            }
        }
        coneType.asPublicType() as CaClassLikeType
    }

    override fun buildFunctionType(
        parameterTypes: List<CaType>,
        returnType: CaType,
        isCFunction: Boolean,
        isClosureType: Boolean,
        hasVariableLengthArgument: Boolean,
    ): CaFunctionType = withValidityAssertion {
        ConeFuncType(
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
    private fun Iterable<CaType>.asConeTypes(owner: String) =
        map { type -> type.requireCfirConeType(owner) }

    /**
     * 当前仓库 Analysis API 尚未公开 variance DSL，这里固定采用不变投影。
     */
    private fun Iterable<CaType>.asConeTypeArguments(owner: String): List<ConeTypeProjection> =
        asConeTypes(owner).map(::ConeTypeProjection)
}

/**
 * 将模块内容作用域暴露为 Analysis API 搜索范围。
 */
internal class CaCfirAnalysisScopeProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaAnalysisScopeProvider {
    override fun CaModule.analysisScope(): GlobalSearchScope = withValidityAssertion {
        contentScope
    }
}

/**
 * 基于当前 session 的签名替换入口。
 */
internal class CaCfirSignatureSubstitutor(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaSignatureSubstitutor {
    override fun CaSignature.substitute(substitutor: CaTypeSubstitutor): CaSubstitutedSignature = withValidityAssertion {
        analysisSession.substituteSignature(this@substitute, substitutor)
    }
}

/**
 * 对外公开统一的类型替换器构造协议。
 */
internal class CaCfirSubstitutorProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaSubstitutorProvider {
    override fun createTypeSubstitutor(substitutions: Map<Name, CaType>): CaTypeSubstitutor = withValidityAssertion {
        analysisSession.buildTypeSubstitutor(substitutions)
    }

    override fun CaSignature.createSubstitutor(typeArguments: List<CaType>): CaTypeSubstitutor = withValidityAssertion {
        analysisSession.buildSignatureSubstitutor(this@createSubstitutor, typeArguments)
    }
}
