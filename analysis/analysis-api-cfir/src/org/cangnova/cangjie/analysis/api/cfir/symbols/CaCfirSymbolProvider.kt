package org.cangnova.cangjie.analysis.api.cfir.symbols

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirSessionComponent
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSymbolProvider
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaScriptSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeAliasSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirAnonymousFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirExtendSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFinalizerSymbol
import org.cangnova.cangjie.cfir.symbols.CfirMacroDeclarationSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternVariableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjScript
import org.cangnova.cangjie.psi.CjTypeAlias
import org.cangnova.cangjie.psi.CjTypeParameter
import org.cangnova.cangjie.psi.CjTypeStatement

/**
 * CFIR 符号查询入口。
 *
 * 该组件负责两类稳定入口：
 * 1. `PSI -> public symbol`；
 * 2. 基于稳定语义标识的直接查询。
 *
 * 这里对齐 Kotlin `KaFirSymbolProvider` 的做法：
 * PSI 入口先解析到具体 CFIR 叶子符号，再走对应的 `create*` 公开构造入口，
 * 而不是把 provider 退化成一个泛型 `getPublicSymbolByPsi` 转发层。
 */
internal class CaCfirSymbolProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSymbolProvider<CaCfirSession>(), CaCfirSessionComponent {
    override val CjFile.symbol: CaFileSymbol
        get() = withValidityAssertion {
            analysisSession.createFileSymbol(this@symbol)
        }

    override val CjScript.symbol: CaScriptSymbol
        get() = withValidityAssertion {
            analysisSession.createScriptSymbol(this@symbol)
        }

    override val CjTypeStatement.classSymbol: CaClassSymbol
        get() = withValidityAssertion {
            analysisSession.resolvePsiSymbol<CfirClassSymbol, CaClassSymbol>(this@classSymbol, analysisSession::createClassLikeSymbol)
                ?: error("Cannot build class symbol for ${this@classSymbol::class}")
        }

    override val org.cangnova.cangjie.psi.CjExtend.symbol: CaExtendSymbol
        get() = withValidityAssertion {
            analysisSession.resolvePsiSymbol<CfirExtendSymbol, CaExtendSymbol>(this@symbol, analysisSession::createExtendSymbol)
                ?: error("Cannot build extend symbol for ${this@symbol::class}")
        }

    override val CjTypeAlias.symbol: CaTypeAliasSymbol
        get() = withValidityAssertion {
            analysisSession.resolvePsiSymbol<CfirTypeAliasSymbol, CaTypeAliasSymbol>(this@symbol, analysisSession::createClassLikeSymbol)
                ?: error("Cannot build type-alias symbol for ${this@symbol::class}")
        }

    override val CjNamedFunction.symbol: CaNamedFunctionSymbol
        get() = withValidityAssertion {
            CaCfirNamedFunctionSymbolImpl(this@symbol, analysisSession)
        }

    override val org.cangnova.cangjie.psi.CjFunctionLiteral.symbol: org.cangnova.cangjie.analysis.api.symbols.CaAnonymousFunctionSymbol
        get() = withValidityAssertion {
            analysisSession.resolvePsiSymbol<CfirAnonymousFunctionSymbol, org.cangnova.cangjie.analysis.api.symbols.CaAnonymousFunctionSymbol>(
                this@symbol,
                analysisSession::createCallableSymbol,
            )
                ?: error("Cannot build anonymous-function symbol for ${this@symbol::class}")
        }

    override val org.cangnova.cangjie.psi.CjConstructor<*>.symbol: org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
        get() = withValidityAssertion {
            analysisSession.resolvePsiSymbol<CfirConstructorSymbol, org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol>(
                this@symbol,
                analysisSession::createCallableSymbol,
            )
                ?: error("Cannot build constructor symbol for ${this@symbol::class}")
        }

    override val org.cangnova.cangjie.psi.CjMacroDeclaration.symbol: org.cangnova.cangjie.analysis.api.symbols.CaMacroSymbol
        get() = withValidityAssertion {
            analysisSession.resolvePsiSymbol<CfirMacroDeclarationSymbol, org.cangnova.cangjie.analysis.api.symbols.CaMacroSymbol>(
                this@symbol,
                analysisSession::createCallableSymbol,
            )
                ?: error("Cannot build macro symbol for ${this@symbol::class}")
        }

    override val org.cangnova.cangjie.psi.CjFinalizer.symbol: org.cangnova.cangjie.analysis.api.symbols.CaFinalizerSymbol
        get() = withValidityAssertion {
            analysisSession.resolvePsiSymbol<CfirFinalizerSymbol, org.cangnova.cangjie.analysis.api.symbols.CaFinalizerSymbol>(
                this@symbol,
                analysisSession::createCallableSymbol,
            )
                ?: error("Cannot build finalizer symbol for ${this@symbol::class}")
        }

    override val CjProperty.symbol: CaPropertySymbol
        get() = withValidityAssertion {
            analysisSession.resolvePsiSymbol<CfirPropertySymbol, CaPropertySymbol>(this@symbol, analysisSession::createCallableSymbol)
                ?: error("Cannot build property symbol for ${this@symbol::class}")
        }

    override val org.cangnova.cangjie.psi.CjPropertyAccessor.symbol: org.cangnova.cangjie.analysis.api.symbols.CaPropertyAccessorSymbol
        get() = withValidityAssertion {
            analysisSession.resolvePsiSymbol<CfirBasedSymbol<*>, org.cangnova.cangjie.analysis.api.symbols.CaPropertyAccessorSymbol>(
                this@symbol,
                analysisSession::getPublicSymbol,
            )
                ?: error("Cannot build property-accessor symbol for ${this@symbol::class}")
        }

    override val org.cangnova.cangjie.psi.CjFieldVariable.symbol: org.cangnova.cangjie.analysis.api.symbols.CaFieldSymbol
        get() = withValidityAssertion {
            analysisSession.resolvePsiSymbol<CfirFieldVariableSymbol, org.cangnova.cangjie.analysis.api.symbols.CaFieldSymbol>(
                this@symbol,
                analysisSession::createCallableSymbol,
            )
                ?: error("Cannot build field symbol for ${this@symbol::class}")
        }

    override val org.cangnova.cangjie.psi.CjEnumConstructor.symbol: org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
        get() = withValidityAssertion {
            analysisSession.resolvePsiSymbol<CfirEnumConstructorSymbol, org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol>(
                this@symbol,
                analysisSession::createCallableSymbol,
            )
                ?: error("Cannot build enum-constructor symbol for ${this@symbol::class}")
        }

    override val CjPatternVariable.symbol: org.cangnova.cangjie.analysis.api.symbols.CaPatternVariableSymbol
        get() = withValidityAssertion {
            analysisSession.resolvePsiSymbol<CfirPatternVariableSymbol, org.cangnova.cangjie.analysis.api.symbols.CaPatternVariableSymbol>(
                this@symbol,
                analysisSession::createCallableSymbol,
            )
                ?: error("Cannot build pattern-variable symbol for ${this@symbol::class}")
        }

    override val org.cangnova.cangjie.psi.CjBindingPattern.symbol: org.cangnova.cangjie.analysis.api.symbols.CaPatternBindingSymbol
        get() = withValidityAssertion {
            analysisSession.resolvePsiSymbol<CfirPatternBindingSymbol, org.cangnova.cangjie.analysis.api.symbols.CaPatternBindingSymbol>(
                this@symbol,
                analysisSession::createCallableSymbol,
            )
                ?: error("Cannot build pattern-binding symbol for ${this@symbol::class}")
        }

    override val CjParameter.symbol: CaVariableSymbol
        get() = withValidityAssertion {
            analysisSession.resolvePsiSymbol<CfirValueParameterSymbol, CaVariableSymbol>(this@symbol, analysisSession::createValueParameterSymbol)
                ?: error("Cannot build variable symbol for ${this@symbol::class}")
        }

    override val CjTypeParameter.symbol: CaTypeParameterSymbol
        get() = withValidityAssertion {
            analysisSession.resolvePsiSymbol<CfirTypeParameterSymbol, CaTypeParameterSymbol>(this@symbol, analysisSession::createTypeParameterSymbol)
                ?: error("Cannot build type-parameter symbol for ${this@symbol::class}")
        }

    override fun getPackageSymbol(fqName: FqName): CaPackageSymbol? = withValidityAssertion {
        analysisSession.getPackagePublicSymbol(fqName)
    }

    override fun getClassLikeSymbol(classId: ClassId): CaClassLikeSymbol? = withValidityAssertion {
        analysisSession.getClassLikePublicSymbol(classId)
    }

    override fun getClassSymbol(classId: ClassId): CaClassSymbol? = withValidityAssertion {
        analysisSession.getClassPublicSymbol(classId)
    }

    override fun getTypeAliasSymbol(classId: ClassId): CaTypeAliasSymbol? = withValidityAssertion {
        analysisSession.getTypeAliasPublicSymbol(classId)
    }

    override fun getTopLevelClassLikeSymbols(packageFqName: FqName, name: Name): List<CaClassLikeSymbol> = withValidityAssertion {
        analysisSession.getOrCreateTopLevelPublicSymbols(packageFqName, name).classLikeSymbols
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<CaCallableSymbol> = withValidityAssertion {
        analysisSession.getOrCreateTopLevelPublicSymbols(packageFqName, name).callableSymbols
    }

    override fun getTopLevelExtendSymbols(packageFqName: FqName): List<CaExtendSymbol> = withValidityAssertion {
        analysisSession.getTopLevelExtendPublicSymbols(packageFqName)
    }

    override fun getExtendSymbols(targetClassId: ClassId): List<CaExtendSymbol> = withValidityAssertion {
        analysisSession.getExtendPublicSymbols(targetClassId)
    }
}

/**
 * 对单个 declaration PSI 做“CFIR 叶子符号 -> public symbol”恢复。
 *
 * 这层是 `CaCfirSymbolProvider` 的私有实现细节：
 * - 先通过 low-level 查询拿到与 PSI 精确关联的 CFIR 符号；
 * - 再走对应的 `create*` 入口恢复 public symbol；
 * - 最后要求结果在该 PSI 叶子上唯一，避免宽松兜底。
 */
private inline fun <reified C : CfirBasedSymbol<*>, reified S : org.cangnova.cangjie.analysis.api.symbols.CaSymbol> CaCfirSession.resolvePsiSymbol(
    psi: PsiElement,
    noinline create: (C) -> org.cangnova.cangjie.analysis.api.symbols.CaSymbol,
): S? {
    return symbolQueries.lookupSymbolsByPsi(psi)
        .asSequence()
        .filterIsInstance<C>()
        .map(create)
        .filterIsInstance<S>()
        .singleOrNull()
}
