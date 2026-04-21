package org.cangnova.cangjie.analysis.api.cfir

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirAnonymousFunctionSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirPropertyAccessorKind
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirClassSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirConstructorSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirEnumConstructorSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirFieldSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirFinalizerSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirMacroSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirMainFunctionSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirPatternBindingSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirPatternVariableSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirPropertyGetterSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirPropertySetterSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirPropertySymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirTypeAliasSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirValueParameterSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.constructExtendSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.constructFilePublicSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.findContainingDeclarationSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.getPublicSymbolByPsi
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirClassErrorType
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirFunctionType
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirGenericSubstitutor
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirIntersectionType
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirMapBackedSubstitutor
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirNonClassErrorType
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirSubstitutorCacheKey
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirTupleType
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirTypeParameterType
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirUnionType
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirUsualClassType
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirType
import org.cangnova.cangjie.analysis.api.cfir.utils.asPublicTypeProjection
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaValueParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.types.CaSubstitutor
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaTypeProjection
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.symbols.CfirAnonymousFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirExtendSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFinalizerSymbol
import org.cangnova.cangjie.cfir.symbols.CfirMacroDeclarationSymbol
import org.cangnova.cangjie.cfir.symbols.CfirMainFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFuncType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeQuestType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.types.ConeSubstitutor
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
import org.cangnova.cangjie.cfir.types.ConeUnionType
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.types.renderForDebugging
import org.cangnova.cangjie.psi.CjPropertyAccessor

/**
 * 对齐 Kotlin `KaSymbolByFirBuilder` 的 CFIR public symbol builder。
 *
 * 这里专门负责“如何从 CFIR 符号构造 public symbol”，
 * 而缓存键选择、pointer restore、session cache 仍由外围组件承担。
 * 这样可以把构造逻辑稳定收敛到 symbol builder 本身，
 * 而不是继续散落在中心化的 symbol factory / query 入口中。
 */
internal class CaSymbolByCfirBuilder(
    private val project: Project,
    val analysisSession: CaCfirSession,
    val token: CaLifetimeToken,
) {
    private val useSiteModule: CaModule
        get() = analysisSession.useSiteModule

    val classifierBuilder = ClassifierSymbolBuilder()
    val functionBuilder = FunctionSymbolBuilder()
    val variableBuilder = VariableSymbolBuilder()
    val callableBuilder = CallableSymbolBuilder()
    val typeBuilder = TypeBuilder()

    fun buildSymbol(symbol: CfirBasedSymbol<*>): CaSymbol = when (symbol) {
        is CfirClassLikeSymbol<*> -> classifierBuilder.buildClassLikeSymbol(symbol)
        is CfirCallableSymbol<*> -> callableBuilder.buildCallableSymbol(symbol)
        is CfirTypeParameterSymbol -> classifierBuilder.buildTypeParameterSymbol(symbol)
        is CfirFileSymbol -> analysisSession.constructFilePublicSymbol(symbol)
        is CfirExtendSymbol -> analysisSession.constructExtendSymbol(symbol)
        else -> error("Unsupported public symbol mapping for `${symbol::class.simpleName}`")
    }

    inner class ClassifierSymbolBuilder {
        fun buildClassLikeSymbol(symbol: CfirClassLikeSymbol<*>): CaClassLikeSymbol = when (symbol) {
            is CfirTypeAliasSymbol -> CaCfirTypeAliasSymbol(symbol, analysisSession)
            is CfirClassSymbol -> CaCfirClassSymbol(symbol, analysisSession)
            else -> CaCfirClassSymbol(symbol, analysisSession)
        }

        fun buildTypeParameterSymbol(symbol: CfirTypeParameterSymbol): CaTypeParameterSymbol =
            CaCfirTypeParameterSymbol(symbol, analysisSession)
    }

    inner class FunctionSymbolBuilder {
        fun buildPropertyAccessorSymbol(
            backingSymbol: CfirCallableSymbol<*>,
            ownerSymbol: CaPropertySymbol,
            kind: CaCfirPropertyAccessorKind,
        ): CaSymbol = when (kind) {
            CaCfirPropertyAccessorKind.GETTER ->
                CaCfirPropertyGetterSymbol(backingSymbol, analysisSession, useSiteModule, analysisSession.token)

            CaCfirPropertyAccessorKind.SETTER ->
                CaCfirPropertySetterSymbol(backingSymbol, analysisSession, useSiteModule, analysisSession.token)
        }

        fun buildFunctionSymbol(symbol: CfirCallableSymbol<*>): CaCallableSymbol = when (symbol) {
            is CfirAnonymousFunctionSymbol -> CaCfirAnonymousFunctionSymbol(symbol, analysisSession, useSiteModule, analysisSession.token)
            is CfirMainFunctionSymbol -> CaCfirMainFunctionSymbol(symbol, analysisSession, useSiteModule, analysisSession.token)
            is CfirMacroDeclarationSymbol -> CaCfirMacroSymbol(symbol, analysisSession, useSiteModule, analysisSession.token)
            is CfirFinalizerSymbol -> CaCfirFinalizerSymbol(symbol, analysisSession, useSiteModule, analysisSession.token)
            is CfirConstructorSymbol -> CaCfirConstructorSymbol(symbol, analysisSession)
            is CfirNamedFunctionSymbol -> CaCfirNamedFunctionSymbol(symbol, analysisSession)
            else -> error("Unsupported function public symbol mapping for `${symbol::class.simpleName}`")
        }
    }

    inner class VariableSymbolBuilder {
        fun buildVariableSymbol(symbol: CfirCallableSymbol<*>): CaCallableSymbol = when (symbol) {
            is CfirPropertySymbol -> CaCfirPropertySymbol(symbol, analysisSession)
            is CfirFieldVariableSymbol -> CaCfirFieldSymbol(symbol, analysisSession, useSiteModule, analysisSession.token)
            is CfirPatternVariableSymbol -> CaCfirPatternVariableSymbol(symbol, analysisSession, useSiteModule, analysisSession.token)
            is CfirPatternBindingSymbol -> CaCfirPatternBindingSymbol(symbol, analysisSession, useSiteModule, analysisSession.token)
            is CfirValueParameterSymbol -> buildValueParameterSymbol(symbol)
            is CfirEnumConstructorSymbol -> CaCfirEnumConstructorSymbol(symbol, analysisSession, useSiteModule, analysisSession.token)
            else -> error("Unsupported variable public symbol mapping for `${symbol::class.simpleName}`")
        }

        fun buildValueParameterSymbol(symbol: CfirValueParameterSymbol): CaValueParameterSymbol {
            val psi = analysisSession.symbolQueries.lookupSourcePsi(symbol)
            val parameterPsi = psi as? org.cangnova.cangjie.psi.CjParameter
            val ownerSymbol = parameterPsi?.let { parameter ->
                analysisSession.findContainingDeclarationSymbol(parameter)
            } as? CaValueParameterOwnerSymbol
            val parameterIndex = parameterPsi?.let { currentParameter ->
                (currentParameter.parent as? org.cangnova.cangjie.psi.CjParameterList)?.parameters?.indexOf(currentParameter)
            }
            return CaCfirValueParameterSymbol(
                symbol = symbol,
                session = analysisSession,
                ownerSymbol = ownerSymbol,
                stableParameterIndex = parameterIndex,
                parameterPsi = parameterPsi,
            )
        }

        fun buildOwnedValueParameterSymbol(
            ownerSymbol: CaValueParameterOwnerSymbol,
            parameter: CfirValueParameter,
            parameterIndex: Int,
        ): CaValueParameterSymbol =
            CaCfirValueParameterSymbol(
                symbol = parameter.symbol,
                session = analysisSession,
                ownerSymbol = ownerSymbol,
                stableParameterIndex = parameterIndex,
                parameterPsi = (ownerSymbol as? CaDeclarationSymbol)
                    ?.psi
                    ?.let { ownerPsi ->
                        (ownerPsi as? org.cangnova.cangjie.psi.CjCallableDeclaration)?.valueParameters?.getOrNull(parameterIndex)
                    },
            )
    }

    inner class CallableSymbolBuilder {
        fun buildCallableSymbol(symbol: CfirCallableSymbol<*>): CaCallableSymbol {
            val psi = analysisSession.symbolQueries.lookupSourcePsi(symbol)
            return when {
                psi is CjPropertyAccessor -> {
                    val owner = psi.property.let { propertyPsi ->
                        analysisSession.getPublicSymbolByPsi<CaPropertySymbol>(propertyPsi)
                    } ?: error("Property accessor `${psi.text}` is missing owning property symbol")
                    val kind = if (psi.isGetter) CaCfirPropertyAccessorKind.GETTER else CaCfirPropertyAccessorKind.SETTER
                    functionBuilder.buildPropertyAccessorSymbol(symbol, owner, kind) as CaCallableSymbol
                }

                symbol is CfirAnonymousFunctionSymbol ||
                    symbol is CfirMainFunctionSymbol ||
                    symbol is CfirMacroDeclarationSymbol ||
                    symbol is CfirFinalizerSymbol ||
                    symbol is CfirConstructorSymbol ||
                    symbol is CfirNamedFunctionSymbol
                    -> functionBuilder.buildFunctionSymbol(symbol)

                else -> variableBuilder.buildVariableSymbol(symbol)
            }
        }
    }

    /**
     * 对齐 Kotlin `KaSymbolByFirBuilder.TypeBuilder`：
     * 类型公共叶子的构造统一收敛到 builder，而不是分散在各个 helper 顶层函数里。
     */
    inner class TypeBuilder {
        fun buildType(coneType: CfirTypeRef): CaType = buildType(coneType.coneType)

        fun buildType(coneType: ConeCangJieType): CaType = when (coneType) {
            is ConeClassLikeType,
            is ConeStructType,
            is ConeEnumType,
            is ConeTypeAliasType,
            is ConePrimitiveType,
            -> CaCfirUsualClassType(coneType, analysisSession)

            is ConeFuncType -> CaCfirFunctionType(coneType, analysisSession)
            is ConeTupleType -> CaCfirTupleType(coneType, analysisSession)
            is ConeIntersectionType -> CaCfirIntersectionType(coneType, analysisSession)
            is ConeUnionType -> CaCfirUnionType(coneType, analysisSession)
            is org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType -> CaCfirTypeParameterType(coneType, analysisSession)
            is ConeErrorType -> CaCfirClassErrorType(coneType, analysisSession)
            is ConeQuestType -> CaCfirNonClassErrorType(
                coneType = coneType,
                analysisSession = analysisSession,
                errorMessageImpl = "Quest type cannot be exposed as a stable public type",
                presentableTextImpl = coneType.renderForDebugging(),
            )

            else -> error("Unsupported CFIR public type projection: ${coneType::class.qualifiedName}")
        }

        fun buildTypeProjections(coneType: ConeCangJieType): List<CaTypeProjection> {
            val coneArguments: List<ConeTypeProjection> = when (coneType) {
                is ConeClassLikeType -> coneType.typeArguments
                is ConeStructType -> coneType.typeArguments
                is ConeEnumType -> coneType.typeArguments
                is ConeTypeAliasType -> coneType.typeArguments
                is ConePrimitiveType -> emptyList()
                else -> error("Only class-like CFIR types can expose type arguments: ${coneType::class.simpleName}")
            }
            return coneArguments.map { projection -> projection.asPublicTypeProjection(analysisSession) }
        }

        /**
         * 对齐 Kotlin `TypeBuilder.buildSubstitutor(...)`：
         * 公开 substitutor 的构造统一走 builder，而不是 session 扩展。
         */
        fun buildSubstitutor(mappings: Map<CaTypeParameterSymbol, CaType>): CaSubstitutor {
            if (mappings.isEmpty()) return CaSubstitutor.Empty(analysisSession.token)

            val normalizedMappings = mappings.entries
                .sortedWith(compareBy({ (typeParameter, _) -> typeParameter.name.asString() }, { (typeParameter, _) -> typeParameter.createPointer().hashCode() }))
                .map { (typeParameter, type) -> typeParameter to type }

            val coneMappings = buildList {
                normalizedMappings.forEach { (typeParameter, type) ->
                    val cfirTypeParameter = typeParameter as? CaCfirTypeParameterSymbol
                        ?: error("仅支持使用 CFIR 类型参数符号构建替换器：${typeParameter::class.simpleName}")
                    val cfirType = type as? CaCfirType
                        ?: error("仅支持使用 CFIR Analysis API 类型构建替换器：${type::class.simpleName}")
                    add(cfirTypeParameter.backingSymbol to cfirType.coneType)
                }
            }

            val cacheKey = CaCfirSubstitutorCacheKey(coneMappings)
            return analysisSession.getOrCreateSubstitutor(cacheKey) {
                CaCfirMapBackedSubstitutor(
                    mappings = normalizedMappings,
                    substitutor = CfirTypeSubstitutorByMap(
                        coneMappings.associate { (typeParameter, coneType) -> typeParameter.name.asString() to coneType },
                    ),
                    builder = this@CaSymbolByCfirBuilder,
                )
            }
        }

        fun buildSubstitutor(substitutor: ConeSubstitutor): CaSubstitutor = when (substitutor) {
            ConeSubstitutor.Empty -> CaSubstitutor.Empty(analysisSession.token)
            is CfirTypeSubstitutorByMap -> CaCfirGenericSubstitutor(substitutor, this@CaSymbolByCfirBuilder)
            else -> CaCfirGenericSubstitutor(substitutor, this@CaSymbolByCfirBuilder)
        }
    }
}
