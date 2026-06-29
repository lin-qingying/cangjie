package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.symbols.CaPropertyGetterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySetterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.session.extendIndexStore
import org.cangnova.cangjie.cfir.symbols.CfirAnonymousFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirExtendSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFinalizerSymbol
import org.cangnova.cangjie.cfir.symbols.CfirMacroDeclarationSymbol
import org.cangnova.cangjie.cfir.symbols.CfirMainFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol

/**
 * public symbol 稳定 key 推导协议。
 *
 * factory 只负责入口与构造委托；稳定 key 的推导集中放在这里，
 * 避免再次回到“构造 + 缓存 + 恢复协议”混在同一个文件里的状态。
 */
internal fun CfirBasedSymbol<*>.publicTypeParameterOwnerKey(session: CaCfirSession): CaCfirPublicSymbolCacheKey? =
    when (this) {
        is CfirClassLikeSymbol<*> -> CaCfirClassLikeSymbolCacheKey(classId)
        is CfirExtendSymbol -> CaCfirExtendSymbolCacheKey(session.resolveExtendIdentity(this).stableIdentity)
        is CfirCallableSymbol<*> -> publicSymbolCacheKeyOrNull(session)
        else -> null
    }

/**
 * 类型参数稳定身份遵循 Kotlin FIR 的 owner + index + name 模式。
 *
 * 名字只是额外一致性检查，真正的恢复身份由 owner 中的稳定序号承担。
 */
internal fun CfirTypeParameterSymbol.stableTypeParameterIndex(): Int? {
    val ownerDeclaration = containingDeclarationSymbol.cfir
    val parameterIndex = when (ownerDeclaration) {
        is CfirClassLikeDeclaration -> ownerDeclaration.typeParameters.indexOfFirst { parameter ->
            parameter.symbol == this
        }

        is CfirExtend -> ownerDeclaration.typeParameters.indexOfFirst { parameter ->
            parameter.symbol == this
        }

        is CfirCallableDeclaration -> ownerDeclaration.typeParameters.indexOfFirst { parameter ->
            parameter is CfirTypeParameter && parameter.symbol == this
        }

        else -> -1
    }
    return parameterIndex.takeIf { it >= 0 }
}

/**
 * 从 CFIR callable 推导 stable public key。
 *
 * `extend` 成员不再依赖 PSI 父链，而是通过 `extendIndexStore` 的 owner 语义索引恢复。
 */
internal fun CfirCallableSymbol<*>.publicSymbolCacheKeyOrNull(session: CaCfirSession): CaCfirPublicSymbolCacheKey? {
    val psi = backingPsiIfApplicable
    if ((cfir as? CfirCallableDeclaration)?.isLocal == true) {
        return psi?.let { localPsi ->
            when (this) {
                is CfirAnonymousFunctionSymbol -> CaCfirPsiSymbolCacheKey(
                    localPsi,
                    CaCfirPsiSymbolKind.ANONYMOUS_FUNCTION,
                )

                is CfirPatternVariableSymbol -> CaCfirPsiSymbolCacheKey(localPsi, CaCfirPsiSymbolKind.PATTERN_VARIABLE)
                is CfirPatternBindingSymbol -> CaCfirPsiSymbolCacheKey(localPsi, CaCfirPsiSymbolKind.PATTERN_BINDING)
                else -> CaCfirPsiSymbolCacheKey(localPsi, CaCfirPsiSymbolKind.LOCAL_VARIABLE)
            }
        }
    }

    val extendDeclaration = session.cfirSession.extendIndexStore.containingExtendOf(this)
    if (extendDeclaration != null) {
        return CaCfirExtendMemberCallableSymbolCacheKey(
            extendIdentity = session.resolveExtendIdentity(extendDeclaration.symbol).stableIdentity,
            callableName = callableId.callableName,
            kind = when (this) {
                is CfirNamedFunctionSymbol -> CaCfirCallableSymbolKind.NAMED_FUNCTION
                is CfirPropertySymbol -> CaCfirCallableSymbolKind.PROPERTY
                is CfirFieldVariableSymbol -> CaCfirCallableSymbolKind.FIELD
                else -> return null
            },
        )
    }

    return when (this) {
        is CfirAnonymousFunctionSymbol -> psi?.let {
            CaCfirPsiSymbolCacheKey(
                it,
                CaCfirPsiSymbolKind.ANONYMOUS_FUNCTION,
            )
        }

        is CfirNamedFunctionSymbol -> CaCfirCallableSymbolCacheKey(callableId, CaCfirCallableSymbolKind.NAMED_FUNCTION)
        is CfirMainFunctionSymbol -> CaCfirCallableSymbolCacheKey(callableId, CaCfirCallableSymbolKind.MAIN_FUNCTION)
        is CfirMacroDeclarationSymbol -> CaCfirCallableSymbolCacheKey(callableId, CaCfirCallableSymbolKind.MACRO)
        is CfirFinalizerSymbol -> CaCfirCallableSymbolCacheKey(callableId, CaCfirCallableSymbolKind.FINALIZER)
        is CfirConstructorSymbol -> CaCfirCallableSymbolCacheKey(callableId, CaCfirCallableSymbolKind.CONSTRUCTOR)
        is CfirPropertySymbol -> CaCfirCallableSymbolCacheKey(callableId, CaCfirCallableSymbolKind.PROPERTY)
        is CfirFieldVariableSymbol -> CaCfirCallableSymbolCacheKey(callableId, CaCfirCallableSymbolKind.FIELD)
        is CfirPatternVariableSymbol -> CaCfirCallableSymbolCacheKey(callableId, CaCfirCallableSymbolKind.PATTERN_VARIABLE)
        is CfirPatternBindingSymbol -> CaCfirCallableSymbolCacheKey(callableId, CaCfirCallableSymbolKind.PATTERN_BINDING)
        is CfirEnumConstructorSymbol -> CaCfirCallableSymbolCacheKey(callableId, CaCfirCallableSymbolKind.ENUM_CONSTRUCTOR)
        is CfirValueParameterSymbol -> null
        else -> null
    }
}

/**
 * 从公开符号对象推导可跨 session 恢复的缓存键。
 */
internal fun CaSymbol.publicSymbolCacheKeyOrNull(): CaCfirPublicSymbolCacheKey? = when (this) {
    is CaCfirFileSymbol -> CaCfirFileSymbolCacheKey(file)
    is CaCfirPackageSymbol -> CaCfirPackageSymbolCacheKey(fqName)
    is CaClassLikeSymbol -> classId?.let(::CaCfirClassLikeSymbolCacheKey)
    is CaCfirExtendSymbol -> CaCfirExtendSymbolCacheKey(stableIdentity)
    is CaPropertyGetterSymbol,
    is CaPropertySetterSymbol,
        -> {
        val ownerKey = owningProperty.publicSymbolCacheKeyOrNull() ?: return null
        CaCfirPropertyAccessorSymbolCacheKey(
            ownerKey = ownerKey,
            kind = if (isGetter) CaCfirPropertyAccessorKind.GETTER else CaCfirPropertyAccessorKind.SETTER,
        )
    }

    is CaCfirValueParameterSymbol -> {
        val ownerKey = builder.buildSymbol(cfirSymbol.containingDeclarationSymbol).publicSymbolCacheKeyOrNull() ?: return null
        val parameterIndex = stableParameterIndex ?: return null
        CaCfirValueParameterSymbolCacheKey(ownerKey, parameterIndex, name)
    }

    is CaCfirTypeParameterSymbol -> {
        val owner = builder.buildSymbol(cfirSymbol.containingDeclarationSymbol)
        val ownerKey = owner.publicSymbolCacheKeyOrNull() ?: return null
        val parameterIndex = stableParameterIndex ?: return null
        CaCfirTypeParameterSymbolCacheKey(ownerKey, name, parameterIndex)
    }

    is CaCfirPatternVariableSymbol -> psi?.let { localPsi ->
        CaCfirPsiSymbolCacheKey(localPsi, CaCfirPsiSymbolKind.PATTERN_VARIABLE)
    }

    is CaCfirPatternBindingSymbol -> psi?.let { localPsi ->
        CaCfirPsiSymbolCacheKey(localPsi, CaCfirPsiSymbolKind.PATTERN_BINDING)
    }

    is CaCfirSymbol<*> -> (cfirSymbol as? CfirCallableSymbol<*>)?.publicSymbolCacheKeyOrNull(analysisSession)
    else -> null
}

/**
 * 为补全候选判定生成稳定或临时的去重 key。
 */
internal fun CaSymbol.completionDecisionKey(): CaCfirCompletionSymbolKey =
    publicSymbolCacheKeyOrNull()?.let(::CaCfirStableCompletionSymbolKey)
        ?: CaCfirEphemeralCompletionSymbolKey(this)
