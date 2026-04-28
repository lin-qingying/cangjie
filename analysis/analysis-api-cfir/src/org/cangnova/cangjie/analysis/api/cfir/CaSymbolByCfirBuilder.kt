package org.cangnova.cangjie.analysis.api.cfir

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirAnonymousFunctionSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirPropertyAccessorKind
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirClassSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirConstructorSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirFieldSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirFinalizerSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirMacroSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirMainFunctionSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirPatternBindingSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirPatternVariableSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirPropertyGetterSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirPropertySetterSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirExtendSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirFileSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirPackageSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirPropertySymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirTypeAliasSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirValueParameterSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.resolveExtendIdentity
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirClassErrorType
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirErrorType
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirFunctionType
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirGenericSubstitutor
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirIntersectionType
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirNonClassErrorType
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirPrimitiveType
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirTupleType
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirTypeParameterType
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirUnionType
import org.cangnova.cangjie.analysis.api.cfir.types.CaCfirUsualClassType
import org.cangnova.cangjie.analysis.api.cfir.utils.asPublicTypeProjection
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.platform.packages.CangJiePackageProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertyAccessorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeAliasSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaValueParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.types.CaSubstitutor
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaTypeProjection
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.errorWithCfirSpecificEntries
import org.cangnova.cangjie.cfir.CfirElement
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.diagnostic.ConeUnmatchedTypeArgumentsError
import org.cangnova.cangjie.cfir.diagnostic.ConeUnresolvedError
import org.cangnova.cangjie.cfir.expressions.withCfirSymbolEntry
import org.cangnova.cangjie.cfir.originalForSubstitutionOverride
import org.cangnova.cangjie.cfir.originalIfFakeOverride
import org.cangnova.cangjie.cfir.scopes.impl.typeAliasConstructorInfo
import org.cangnova.cangjie.cfir.symbols.CfirAnonymousFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirExtendSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFinalizerSymbol
import org.cangnova.cangjie.cfir.symbols.CfirMacroDeclarationSymbol
import org.cangnova.cangjie.cfir.symbols.CfirMainFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertyAccessorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassifierSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.symbols.CfirVariableSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterLookupTag
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeEnumType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeFunctionType
import org.cangnova.cangjie.cfir.types.ConeIntersectionType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeQuestType
import org.cangnova.cangjie.cfir.types.ConeStructType
import org.cangnova.cangjie.cfir.types.CfirTypeSubstitutorByMap
import org.cangnova.cangjie.cfir.types.ConeStubType
import org.cangnova.cangjie.cfir.resolve.substitution.ConeSubstitutor
import org.cangnova.cangjie.cfir.resolve.toSymbol
import org.cangnova.cangjie.cfir.types.ConeTupleType
import org.cangnova.cangjie.cfir.types.ConeClassLikeLookupTag
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.ConeTypeProjection
import org.cangnova.cangjie.cfir.types.ConeUnionType
import org.cangnova.cangjie.cfir.types.coneType
import org.cangnova.cangjie.cfir.types.contains
import org.cangnova.cangjie.cfir.types.forEachType
import org.cangnova.cangjie.cfir.types.renderForDebugging
import org.cangnova.cangjie.cfir.visitors.CfirVisitorVoid
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment

/**
 * 对齐 Kotlin `CaSymbolByCfirBuilder` 的 CFIR public symbol builder。
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
companion object{
    private fun throwUnexpectedElementError(element: CfirBasedSymbol<*>): Nothing {
        errorWithAttachment("Unexpected ${element::class.simpleName}") {
            withCfirSymbolEntry("cfirSymbol", element)
        }
    }
}
    private val useSiteModule: CaModule
        get() = analysisSession.useSiteModule
    @OptIn(CaPlatformInterface::class)
    private val packageProvider: CangJiePackageProvider
        get() = analysisSession.useSitePackageProvider

    val classifierBuilder = ClassifierSymbolBuilder()
    val functionBuilder = FunctionSymbolBuilder()
    val variableBuilder = VariableSymbolBuilder()
    val callableBuilder = CallableSymbolBuilder()
    val typeBuilder = TypeBuilder()
    fun buildSymbol(cfir: CfirDeclaration): CaSymbol = buildSymbol(cfir.symbol)

    fun buildSymbol(symbol: CfirBasedSymbol<*>): CaSymbol = when (symbol) {
        is CfirClassLikeSymbol<*> -> classifierBuilder.buildClassLikeSymbol(symbol)
        is CfirCallableSymbol<*> -> callableBuilder.buildCallableSymbol(symbol)
        is CfirTypeParameterSymbol -> classifierBuilder.buildTypeParameterSymbol(symbol)
        is CfirFileSymbol -> buildFileSymbol(symbol)
        is CfirExtendSymbol -> buildExtendSymbol(symbol)
        else -> error("Unsupported public symbol mapping for `${symbol::class.simpleName}`")
    }

    fun buildFileSymbol(symbol: CfirFileSymbol): CaFileSymbol = CaCfirFileSymbol(symbol, analysisSession)

    @OptIn(CaPlatformInterface::class)
    fun createPackageSymbolIfOneExists(packageFqName: FqName): CaPackageSymbol? {
        if (!packageProvider.doesPackageExist(packageFqName)) {
            return null
        }

        return createPackageSymbol(packageFqName)
    }

    fun createPackageSymbol(packageFqName: FqName): CaPackageSymbol =
        CaCfirPackageSymbol(packageFqName, analysisSession.useSiteModule, token)

    fun buildExtendSymbol(symbol: CfirExtendSymbol): CaExtendSymbol {
        val identity = analysisSession.resolveExtendIdentity(symbol)
            return CaCfirExtendSymbol(
                backingSymbol = symbol,
                extendPsi = identity.extendPsi,
                stableIdentity = identity.stableIdentity,
                stableExtendId = identity.extendId,
            extendPackageFqName = identity.packageFqName,
            analysisSession = analysisSession,
        )
    }

    inner class ClassifierSymbolBuilder {
        fun buildClassLikeSymbol(symbol: CfirClassLikeSymbol<*>): CaClassLikeSymbol = when (symbol) {
            is CfirTypeAliasSymbol -> CaCfirTypeAliasSymbol(symbol, analysisSession)
            is CfirClassSymbol -> CaCfirClassSymbol(symbol, analysisSession)
            else -> CaCfirClassSymbol(symbol, analysisSession)
        }

        fun buildClassSymbol(symbol: CfirClassSymbol): CaClassSymbol =
            CaCfirClassSymbol(symbol, analysisSession)

        fun buildTypeAliasSymbol(symbol: CfirTypeAliasSymbol): CaTypeAliasSymbol =
            CaCfirTypeAliasSymbol(symbol, analysisSession)

        fun buildClassifierSymbol(firSymbol: CfirClassifierSymbol<*>): CaClassifierSymbol = when (firSymbol) {
            is CfirClassLikeSymbol<*> -> classifierBuilder.buildClassLikeSymbol(firSymbol)
            is CfirTypeParameterSymbol -> buildTypeParameterSymbol(firSymbol)
        }
        fun buildTypeParameterSymbol(symbol: CfirTypeParameterSymbol): CaTypeParameterSymbol =
            CaCfirTypeParameterSymbol(symbol, analysisSession)

        fun buildClassLikeSymbolByClassId(classId: ClassId): CaClassLikeSymbol? {
            val symbol = analysisSession.cfirSession.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
            return buildClassLikeSymbol(symbol)
        }

        fun buildClassLikeSymbolByLookupTag(lookupTag: ConeClassLikeLookupTag): CaClassLikeSymbol? {
            val symbol = lookupTag.toSymbol(analysisSession.cfirSession) ?: return null
            return buildClassLikeSymbol(symbol)
        }
    }

    inner class FunctionSymbolBuilder {
        fun buildConstructorSymbol(cfirSymbol: CfirConstructorSymbol): CaConstructorSymbol {
            val unwrapped = cfirSymbol.cfir.unwrapSubstitutionOverrideIfNeeded()?.symbol ?: cfirSymbol
            return CaCfirConstructorSymbol(unwrapped, analysisSession)
        }
        fun buildNamedFunctionSymbol(cfirSymbol: CfirNamedFunctionSymbol): CaNamedFunctionSymbol {
            cfirSymbol.cfir.unwrapSubstitutionOverrideIfNeeded()?.let {
                return buildNamedFunctionSymbol(it.symbol)
            }

            if (cfirSymbol.dispatchReceiverType?.contains { it is ConeStubType } == true) {
                return buildNamedFunctionSymbol(
                    cfirSymbol.originalIfFakeOverride()
                        ?: errorWithCfirSpecificEntries("Stub type in real declaration", cfir = cfirSymbol.cfir)
                )
            }


            check(cfirSymbol.origin != CfirDeclarationOrigin.SamConstructor)
            return CaCfirNamedFunctionSymbol(cfirSymbol, analysisSession)
        }

        fun buildPropertyAccessorSymbol(cfirSymbol: CfirPropertyAccessorSymbol): CaPropertyAccessorSymbol {
            val propertySymbol = variableBuilder.buildVariableSymbol(cfirSymbol.propertySymbol)
            requireWithAttachment(
                propertySymbol is CaPropertySymbol,
                { "Unexpected property symbol type: ${propertySymbol::class.simpleName}" },
            ) {
                withCfirSymbolEntry("propertySymbol", cfirSymbol.propertySymbol)
            }

            val accessorSymbol = if (cfirSymbol.isGetter) {
                propertySymbol.getter
            } else {
                propertySymbol.setter
            }
            requireWithAttachment(
                accessorSymbol != null,
                { "Inconsistent state: property accessor is null while property symbol is not null" },
            ) {
                withCfirSymbolEntry("propertySymbol", cfirSymbol.propertySymbol)
            }

            return accessorSymbol
        }

        fun buildPropertyAccessorSymbol(
            backingSymbol: CfirCallableSymbol<*>,
            ownerSymbol: CaPropertySymbol,
            kind: CaCfirPropertyAccessorKind,
        ): CaSymbol = when (kind) {
            CaCfirPropertyAccessorKind.GETTER ->
                CaCfirPropertyGetterSymbol(backingSymbol, ownerSymbol, analysisSession)

            CaCfirPropertyAccessorKind.SETTER ->
                CaCfirPropertySetterSymbol(backingSymbol, ownerSymbol, analysisSession)
        }

        fun buildFunctionSymbol(symbol: CfirCallableSymbol<*>): CaFunctionSymbol = when (symbol) {
            is CfirAnonymousFunctionSymbol -> CaCfirAnonymousFunctionSymbol(symbol, analysisSession)
            is CfirMainFunctionSymbol -> CaCfirMainFunctionSymbol(symbol, analysisSession, useSiteModule, analysisSession.token)
            is CfirMacroDeclarationSymbol -> CaCfirMacroSymbol(symbol, analysisSession)
            is CfirFinalizerSymbol -> CaCfirFinalizerSymbol(symbol, analysisSession)
            is CfirPropertyAccessorSymbol -> buildPropertyAccessorSymbol(symbol)
            is CfirConstructorSymbol -> CaCfirConstructorSymbol(symbol, analysisSession)
            is CfirNamedFunctionSymbol -> CaCfirNamedFunctionSymbol(symbol, analysisSession)
            else -> error("Unsupported function public symbol mapping for `${symbol::class.simpleName}`")
        }

        fun buildFunctionSignature(symbol: CfirFunctionSymbol<*>): org.cangnova.cangjie.analysis.api.signatures.CaFunctionSignature<org.cangnova.cangjie.analysis.api.symbols.CaFunctionSymbol> {
            return with(analysisSession) {
                functionBuilder.buildFunctionSymbol(symbol).asSignature()
            }
        }
    }
    /**
     * N.B. This functions lifts only a single layer of SUBSTITUTION_OVERRIDE at a time.
     */
    private inline fun <reified T : CfirCallableDeclaration> T.unwrapSubstitutionOverrideIfNeeded(): T? {
        unwrapUseSiteSubstitutionOverride()?.let { return it }

        unwrapInheritanceSubstitutionOverrideIfNeeded()?.let { return it }

        return null
    }
    /**
     * We want to unwrap a SUBSTITUTION_OVERRIDE wrapper if it doesn't affect the declaration's signature in any way. If the signature
     * is somehow changed, then we want to keep the wrapper.
     *
     * Such substitute overrides happen because of inheritance.
     *
     * If the declaration references only its own type parameters, or parameters from the outer declarations, then
     * we consider that it's signature will not be changed by the SUBSTITUTION_OVERRIDE, so the wrapper can be unwrapped.
     *
     * This have a few caveats when it comes to the inner classes. TODO Provide a reference to some more in-detail description of that.
     *
     * @receiver A declaration that needs to be unwrapped.
     * @return An unsubstituted declaration ([originalForSubstitutionOverride]]) if it exists and if it does not have any change
     * in signature; `null` otherwise.
     */
    private inline fun <reified T : CfirCallableDeclaration> T.unwrapInheritanceSubstitutionOverrideIfNeeded(): T? {
        if (this is CfirConstructor && typeAliasConstructorInfo?.typeAliasSymbol != null) {
            return null
        }

        val originalDeclaration = originalForSubstitutionOverride ?: return null

        // 不需要 containingClass，只考虑声明自身的类型参数
        val allowedTypeParameters = originalDeclaration.typeParameters
            .map { it.symbol.toLookupTag() }
            .toSet()

        val usedTypeParameters = collectReferencedTypeParameters(originalDeclaration)

        return if (allowedTypeParameters.containsAll(usedTypeParameters)) {
            originalDeclaration
        } else {
            null
        }
    }

    /**
     * Use-site substitution override happens in situations like this:
     *
     * ```
     * interface List<A> { fun get(i: Int): A }
     *
     * fun take(list: List<String>) {
     *   list.get(10) // this call
     * }
     * ```
     *
     * In FIR, `List::get` symbol in the example will be a substitution override with a `String` instead of `A`.
     * We want to lift such substitution overrides.
     *
     * @receiver A declaration that needs to be unwrapped.
     * @return An unsubstituted declaration ([originalForSubstitutionOverride]]) if [this] is a use-site substitution override.
     */
    private inline fun <reified T : CfirCallableDeclaration> T.unwrapUseSiteSubstitutionOverride(): T? {
        val originalDeclaration = originalForSubstitutionOverride ?: return null
        return originalDeclaration.takeIf { this.origin is CfirDeclarationOrigin.SubstitutionOverride.CallSite }
    }
    inner class VariableSymbolBuilder {
        fun buildVariableSymbol(symbol: CfirCallableSymbol<*>): CaVariableSymbol = when (symbol) {
            is CfirPropertySymbol -> CaCfirPropertySymbol(symbol, analysisSession)
            is CfirFieldVariableSymbol -> CaCfirFieldSymbol(symbol, analysisSession)
            is CfirPatternVariableSymbol -> CaCfirPatternVariableSymbol(symbol, analysisSession)
            is CfirPatternBindingSymbol -> CaCfirPatternBindingSymbol(symbol, analysisSession)
            is CfirValueParameterSymbol -> buildValueParameterSymbol(symbol)
            else -> error("Unsupported variable public symbol mapping for `${symbol::class.simpleName}`")
        }

        fun buildValueParameterSymbol(symbol: CfirValueParameterSymbol): CaValueParameterSymbol {

            val functionSymbol = symbol.containingDeclarationSymbol

            (functionSymbol as? CfirFunctionSymbol)?.cfir?.unwrapSubstitutionOverrideIfNeeded()?.let { unwrappedFunction ->
                val originalIndex = functionSymbol.valueParameterSymbols.indexOf(symbol)
                if (originalIndex == -1) {
                    errorWithAttachment("Containing function doesn't have the corresponding parameter") {
                        withCfirSymbolEntry("valueParameter", symbol)
                        withCfirSymbolEntry("function", functionSymbol)
                    }
                }

                val unwrappedParameter = unwrappedFunction.symbol.valueParameterSymbols[originalIndex]
                return buildValueParameterSymbol(unwrappedParameter)
            }


            return when (functionSymbol) {

                else -> CaCfirValueParameterSymbol(symbol, analysisSession)
            }
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

        fun buildVariableLikeSignature(symbol: CfirVariableSymbol<*>): org.cangnova.cangjie.analysis.api.signatures.CaVariableSignature<org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol> {
            return with(analysisSession) {
                variableBuilder.buildVariableSymbol(symbol).asSignature()
            }
        }

        fun buildVariableLikeSignature(symbol: CfirPropertySymbol): org.cangnova.cangjie.analysis.api.signatures.CaVariableSignature<org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol> {
            return with(analysisSession) {
                variableBuilder.buildVariableSymbol(symbol).asSignature()
            }
        }
    }

    inner class CallableSymbolBuilder {
        fun buildCallableSymbol(cfirSymbol: CfirCallableSymbol<*>): CaCallableSymbol = when (cfirSymbol) {
            is CfirFunctionSymbol<*> -> functionBuilder.buildFunctionSymbol(cfirSymbol)
            is CfirVariableSymbol<*> -> variableBuilder.buildVariableSymbol(cfirSymbol)
            else -> throwUnexpectedElementError(cfirSymbol)
        }
    }

    /**
     * 对齐 Kotlin `CaSymbolByCfirBuilder.TypeBuilder`：
     * 类型公共叶子的构造统一收敛到 builder，而不是分散在各个 helper 顶层函数里。
     */
    inner class TypeBuilder {
        fun buildType(coneType: CfirTypeRef): CaType = buildType(coneType.coneType)

        fun buildType(coneType: ConeCangJieType): CaType = when (coneType) {
            is ConeClassLikeType,
            is ConeStructType,
            is ConeEnumType,
            is ConeTypeAliasType,
            -> CaCfirUsualClassType(coneType, this@CaSymbolByCfirBuilder)

            is ConePrimitiveType -> CaCfirPrimitiveType(coneType, analysisSession)
            is ConeFunctionType -> CaCfirFunctionType(coneType, analysisSession)
            is ConeTupleType -> CaCfirTupleType(coneType, analysisSession)
            is ConeIntersectionType -> CaCfirIntersectionType(coneType, analysisSession)
            is ConeUnionType -> CaCfirUnionType(coneType, analysisSession)
            is ConeTypeParameterType -> CaCfirTypeParameterType(coneType, this@CaSymbolByCfirBuilder)
            is ConeErrorType ->
                when (val diagnostic = coneType.diagnostic) {
                    is ConeUnresolvedError, is ConeUnmatchedTypeArgumentsError -> {
                        CaCfirClassErrorType(coneType, diagnostic, this@CaSymbolByCfirBuilder)
                    }

                    else -> CaCfirErrorType(coneType, this@CaSymbolByCfirBuilder)
                }


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


        fun buildSubstitutor(substitutor: ConeSubstitutor): CaSubstitutor = when (substitutor) {
            ConeSubstitutor.Empty -> CaSubstitutor.Empty(analysisSession.token)
            is CfirTypeSubstitutorByMap -> CaCfirGenericSubstitutor(substitutor, this@CaSymbolByCfirBuilder)
            else -> CaCfirGenericSubstitutor(substitutor, this@CaSymbolByCfirBuilder)
        }
    }
}




internal fun CfirElement.buildSymbol(builder: CaSymbolByCfirBuilder): CaSymbol? = (this as? CfirDeclaration)?.symbol?.let(builder::buildSymbol)
internal fun CfirDeclaration.buildSymbol(builder: CaSymbolByCfirBuilder): CaSymbol = builder.buildSymbol(symbol)
internal fun CfirBasedSymbol<*>.buildSymbol(builder: CaSymbolByCfirBuilder): CaSymbol = builder.buildSymbol(this)

private fun collectReferencedTypeParameters(declaration: CfirCallableDeclaration): Set<ConeTypeParameterLookupTag> {
    val allUsedTypeParameters = mutableSetOf<ConeTypeParameterLookupTag>()
    declaration.accept(object : CfirVisitorVoid(){
        override fun visitElement(element: CfirElement) {
            element.acceptChildren(this)
        }


        override fun visitNamedFunction(namedFunction: CfirNamedFunction) {
            namedFunction.typeParameters.forEach { it.accept(this) }

            namedFunction.valueParameters.forEach { it.returnTypeRef.accept(this) }
            namedFunction.returnTypeRef.accept(this)
        }




        override fun visitResolvedTypeRef(resolvedTypeRef: CfirResolvedTypeRef) {
            super.visitResolvedTypeRef(resolvedTypeRef)

            handleTypeRef(resolvedTypeRef)
        }

        private fun handleTypeRef(resolvedTypeRef: CfirResolvedTypeRef) {
            val resolvedType = resolvedTypeRef.coneType

            resolvedType.forEachType {
                if (it is ConeTypeParameterType) {
                    allUsedTypeParameters.add(it.lookupTag)
                }
            }
        }
    })
    return allUsedTypeParameters

}
