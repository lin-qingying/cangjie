package org.cangnova.cangjie.cfir.resolve.cjmp

import org.cangnova.cangjie.cfir.hasImplicitOrInferredReturnType
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.session.CfirCjmpMappingStorage
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cjmpHasCommonDefault
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.containsErrorType
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMatchingContext
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMatchingValueParameter
import org.cangnova.cangjie.resolve.calls.mpp.CjmpMismatchKind
import org.cangnova.cangjie.resolve.calls.mpp.CjmpParameterMismatch
import org.cangnova.cangjie.resolve.calls.mpp.CjmpTypeCompatibility
import org.cangnova.cangjie.resolve.calls.mpp.CjmpTypeParameterSymbolMarker
import org.cangnova.cangjie.type.AbstractTypeChecker

internal data class CfirCjmpTypeParameterMarker(val symbol: CfirTypeParameterSymbol) : CjmpTypeParameterSymbolMarker

/** CFIR 适配层：把平台中立 matcher 的符号和类型操作映射到当前 session 与单侧存储。 */
internal class CfirCjmpMatchingContext(
    private val session: CfirSession,
    private val storage: CfirCjmpMappingStorage,
) : CjmpMatchingContext<CfirBasedSymbol<*>, CfirCjmpTypeParameterMarker, ConeCangJieType> {
    private fun CfirBasedSymbol<*>.declaration(): CfirDeclaration = cfir

    fun parentTypeParameterMapping(
        mapping: Map<CfirTypeParameter, CfirTypeParameter>,
    ): Map<CfirCjmpTypeParameterMarker, CfirCjmpTypeParameterMarker> =
        mapping.mapKeys { CfirCjmpTypeParameterMarker(it.key.symbol) }
            .mapValues { CfirCjmpTypeParameterMarker(it.value.symbol) }

    private fun Map<CfirCjmpTypeParameterMarker, CfirCjmpTypeParameterMarker>.toCfirTypeParameters():
        Map<CfirTypeParameter, CfirTypeParameter> =
        entries.associate { (common, specific) -> common.symbol.cfir to specific.symbol.cfir }

    override fun classLikeKind(declaration: CfirBasedSymbol<*>): String? =
        (declaration.declaration() as? CfirClassLikeDeclaration)?.let(CfirCjmpMatcher::nominalKind)

    override fun isExtend(declaration: CfirBasedSymbol<*>): Boolean = declaration.declaration() is CfirExtend

    override fun haveSameExtendKey(
        first: CfirBasedSymbol<*>,
        second: CfirBasedSymbol<*>,
    ): CjmpTypeCompatibility =
        if (first.declaration() is CfirExtend && second.declaration() is CfirExtend) {
            CfirCjmpMatcher.haveSameExtendKey(first.declaration() as CfirExtend, second.declaration() as CfirExtend)
        } else {
            CjmpTypeCompatibility.INCOMPATIBLE
        }

    override fun callableKind(declaration: CfirBasedSymbol<*>): String =
        CfirCjmpMatcher.callableKind(declaration.declaration())

    override fun isFunction(declaration: CfirBasedSymbol<*>): Boolean = declaration.declaration() is CfirFunction

    override fun isConstructor(declaration: CfirBasedSymbol<*>): Boolean =
        declaration.declaration() is org.cangnova.cangjie.cfir.declarations.CfirConstructor

    override fun typeParameters(declaration: CfirBasedSymbol<*>): List<CfirCjmpTypeParameterMarker> =
        CfirCjmpMatcher.typeParameters(declaration.declaration()).map { CfirCjmpTypeParameterMarker(it.symbol) }

    override fun valueParameters(
        declaration: CfirBasedSymbol<*>,
    ): List<CjmpMatchingValueParameter<ConeCangJieType>> =
        (declaration.declaration() as? CfirFunction)?.valueParameters.orEmpty().map { parameter ->
            CjmpMatchingValueParameter(
                type = CfirCjmpMatcher.typeOf(parameter.returnTypeRef),
                isNamed = parameter.isNamed,
                name = parameter.name.asString(),
                hasDefaultValue = parameter.defaultValue != null,
            )
        }

    override fun returnType(declaration: CfirBasedSymbol<*>): ConeCangJieType? =
        (declaration.declaration() as? CfirFunction)?.let { CfirCjmpMatcher.typeOf(it.returnTypeRef) }

    override fun canDeferReturnTypeCompatibility(
        specific: CfirBasedSymbol<*>,
        common: CfirBasedSymbol<*>,
    ): Boolean {
        val specificFunction = specific.declaration() as? CfirFunction ?: return false
        if (!specificFunction.hasImplicitOrInferredReturnType()) return false

        val commonFunction = common.declaration() as? CfirFunction ?: return false
        val commonReturnType = CfirCjmpMatcher.typeOf(commonFunction.returnTypeRef) ?: return false
        if (commonReturnType.containsErrorType()) return false

        // 官方 PreTypeCheck 先建立 CJMP 配对，PostTypeCheck 再由
        // CheckMatchedFunctionReturnTypes 检查函数体推断的返回类型。specific 的隐式返回类型
        // 此时可能尚未推断，或因函数体错误含 Error；这两种情形都保留首个候选供后置检查处理。
        return true
    }

    override fun areTypesEquivalent(
        specific: ConeCangJieType?,
        common: ConeCangJieType?,
        typeParameterMapping: Map<CfirCjmpTypeParameterMarker, CfirCjmpTypeParameterMarker>,
    ): CjmpTypeCompatibility =
        CfirCjmpMatcher.typesEquivalent(specific, common, typeParameterMapping.toCfirTypeParameters())

    override fun isSubtypeOf(
        specific: ConeCangJieType?,
        common: ConeCangJieType?,
        typeParameterMapping: Map<CfirCjmpTypeParameterMarker, CfirCjmpTypeParameterMarker>,
    ): CjmpTypeCompatibility {
        if (specific == null || common == null) return CjmpTypeCompatibility.UNRESOLVED
        if (specific.containsErrorType() || common.containsErrorType()) return CjmpTypeCompatibility.UNRESOLVED

        val cfirTypeParameterMapping = typeParameterMapping.toCfirTypeParameters()
        val substitutedCommon = CfirCjmpMatcher.substituteCommonType(common, cfirTypeParameterMapping)
        when (CfirCjmpMatcher.typesEquivalent(specific, substitutedCommon, emptyMap())) {
            CjmpTypeCompatibility.COMPATIBLE -> return CjmpTypeCompatibility.COMPATIBLE
            CjmpTypeCompatibility.UNRESOLVED -> return CjmpTypeCompatibility.UNRESOLVED
            CjmpTypeCompatibility.INCOMPATIBLE -> Unit
        }
        return if (AbstractTypeChecker.isSubtypeOf(session.typeContext, specific, substitutedCommon)) {
            CjmpTypeCompatibility.COMPATIBLE
        } else {
            CjmpTypeCompatibility.INCOMPATIBLE
        }
    }

    override fun needToReportMissingBody(common: CfirBasedSymbol<*>, specific: CfirBasedSymbol<*>): Boolean {
        val commonDeclaration = common.declaration() as? CfirMemberDeclaration ?: return false
        val specificDeclaration = specific.declaration() as? CfirMemberDeclaration ?: return false
        if (commonDeclaration !is CfirCallableDeclaration) return false
        return commonDeclaration.cjmpHasCommonDefault() &&
                !commonDeclaration.status.isAbstract &&
                specificDeclaration.status.isAbstract
    }

    override fun trySetSpecificImplementation(
        specific: CfirBasedSymbol<*>,
        common: CfirBasedSymbol<*>,
        typeParameterMapping: Map<CfirCjmpTypeParameterMarker, CfirCjmpTypeParameterMarker>,
    ): Boolean = storage.bind(
        specific.declaration(),
        common.declaration(),
        typeParameterMapping.toCfirTypeParameters(),
    )

    override fun recordMismatch(
        specific: CfirBasedSymbol<*>,
        kind: CjmpMismatchKind,
        common: CfirBasedSymbol<*>,
    ) {
        storage.recordMismatch(specific.declaration(), kind, common.declaration())
    }

    override fun recordParameterMismatch(
        specific: CfirBasedSymbol<*>,
        mismatch: CjmpParameterMismatch,
        common: CfirBasedSymbol<*>,
    ) {
        storage.recordParameterMismatch(specific.declaration(), mismatch, common.declaration())
    }
}
