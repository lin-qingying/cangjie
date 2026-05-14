package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirSessionComponent
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSymbolProvider
import org.cangnova.cangjie.analysis.api.impl.base.components.withPsiValidityAssertion
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaAnonymousFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFieldSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFinalizerSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaMacroSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPatternBindingSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPatternVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertyAccessorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeAliasSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.util.errorWithCfirSpecificEntries
import org.cangnova.cangjie.cfir.session.extendProviderOrNull
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjConstructor
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.CjFieldVariable
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjFinalizer
import org.cangnova.cangjie.psi.CjFunctionLiteral
import org.cangnova.cangjie.psi.CjMacroDeclaration
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjPatternVariable
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjPropertyAccessor
import org.cangnova.cangjie.psi.CjTypeAlias
import org.cangnova.cangjie.psi.CjTypeParameter
import org.cangnova.cangjie.psi.CjTypeStatement

/**
 * CFIR 符号查询入口。
 *
 * 该类按 Kotlin `KaFirSymbolProvider` 的形状组织：
 * PSI 入口直接构造对应 public symbol；只有基于 provider 的语义查询入口
 * 才从 CFIR symbol provider 取符号后交给 public symbol builder。
 */
internal class CaCfirSymbolProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSymbolProvider<CaCfirSession>(), CaCfirSessionComponent {
    override val CjParameter.symbol: CaVariableSymbol
        get() = withPsiValidityAssertion {
            when {
                isFunctionTypeParameter() -> errorWithCfirSpecificEntries(
                    "Creating ${CaVariableSymbol::class.simpleName} for function type parameter is not possible. " +
                        "Please see the KDoc of `symbol`",
                    psi = this,
                )

                else -> CaCfirValueParameterSymbol(this@symbol, analysisSession)
            }
        }

    override val CjFile.symbol: CaFileSymbol
        get() = withPsiValidityAssertion {
            CaCfirFileSymbol(this@symbol, analysisSession)
        }

    override val CjTypeStatement.classSymbol: CaClassSymbol
        get() = withPsiValidityAssertion {
            CaCfirClassSymbol(this@classSymbol, analysisSession)
        }

    override val CjTypeAlias.symbol: CaTypeAliasSymbol
        get() = withPsiValidityAssertion {
            CaCfirTypeAliasSymbol(this@symbol, analysisSession)
        }

    override val CjNamedFunction.symbol: CaNamedFunctionSymbol
        get() = withPsiValidityAssertion {
            CaCfirNamedFunctionSymbol(this@symbol, analysisSession)
        }

    override val CjFunctionLiteral.symbol: CaAnonymousFunctionSymbol
        get() = withPsiValidityAssertion {
            CaCfirAnonymousFunctionSymbol(this@symbol, analysisSession)
        }

    override val CjConstructor<*>.symbol: CaConstructorSymbol
        get() = withPsiValidityAssertion {
            CaCfirConstructorSymbol(this@symbol, analysisSession)
        }

    override val CjMacroDeclaration.symbol: CaMacroSymbol
        get() = withPsiValidityAssertion {
            CaCfirMacroSymbol(this@symbol, analysisSession)
        }

    override val CjFinalizer.symbol: CaFinalizerSymbol
        get() = withPsiValidityAssertion {
            CaCfirFinalizerSymbol(this@symbol, analysisSession)
        }

    override val CjProperty.symbol: CaPropertySymbol
        get() = withPsiValidityAssertion {
            CaCfirPropertySymbol(this@symbol, analysisSession)
        }

    override val CjPropertyAccessor.symbol: CaPropertyAccessorSymbol
        get() = withPsiValidityAssertion {
            val propertySymbol = property.symbol
            if (isGetter) {
                propertySymbol.getter ?: error("Cannot build getter symbol for ${this@symbol::class}")
            } else {
                propertySymbol.setter ?: error("Cannot build setter symbol for ${this@symbol::class}")
            }
        }

    override val CjFieldVariable.symbol: CaFieldSymbol
        get() = withPsiValidityAssertion {
            CaCfirFieldSymbol(this@symbol, analysisSession)
        }

    override val CjEnumConstructor.symbol: CaEnumConstructorSymbol
        get() = withPsiValidityAssertion {
            CaCfirEnumConstructorSymbol(this@symbol, analysisSession)
        }

    override val CjPatternVariable.symbol: CaPatternVariableSymbol
        get() = withPsiValidityAssertion {
            CaCfirPatternVariableSymbol(this@symbol, analysisSession)
        }

    override val CjBindingPattern.symbol: CaPatternBindingSymbol
        get() = withPsiValidityAssertion {
            CaCfirPatternBindingSymbol(this@symbol, analysisSession)
        }

    override val CjExtend.symbol: CaExtendSymbol
        get() = withPsiValidityAssertion {
            CaCfirExtendSymbol(this@symbol, analysisSession)
        }

    override val CjTypeParameter.symbol: CaTypeParameterSymbol
        get() = withPsiValidityAssertion {
            CaCfirTypeParameterSymbol(this@symbol, analysisSession)
        }

    override fun getPackageSymbol(fqName: FqName): CaPackageSymbol? = withValidityAssertion {
        analysisSession.cfirSymbolBuilder.createPackageSymbolIfOneExists(fqName)
    }

    override fun getClassLikeSymbol(classId: ClassId): CaClassLikeSymbol? = withValidityAssertion {
        analysisSession.cfirSymbolBuilder.classifierBuilder.buildClassLikeSymbolByClassId(classId)
    }

    override fun getClassSymbol(classId: ClassId): CaClassSymbol? = withValidityAssertion {
        val symbol = analysisSession.cfirSession.symbolProvider.getClassLikeSymbolByClassId(classId) as? CfirClassSymbol
            ?: return@withValidityAssertion null
        analysisSession.cfirSymbolBuilder.classifierBuilder.buildClassSymbol(symbol)
    }

    override fun getTypeAliasSymbol(classId: ClassId): CaTypeAliasSymbol? = withValidityAssertion {
        val symbol = analysisSession.cfirSession.symbolProvider.getClassLikeSymbolByClassId(classId) as? CfirTypeAliasSymbol
            ?: return@withValidityAssertion null
        analysisSession.cfirSymbolBuilder.classifierBuilder.buildTypeAliasSymbol(symbol)
    }

    override fun getTopLevelClassLikeSymbols(packageFqName: FqName, name: Name): List<CaClassLikeSymbol> = withValidityAssertion {
        val classId = ClassId(packageFqName, name)
        analysisSession.cfirSymbolBuilder.classifierBuilder.buildClassLikeSymbolByClassId(classId)
            ?.let(::listOf)
            .orEmpty()
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<CaCallableSymbol> = withValidityAssertion {
        analysisSession.cfirSession.symbolProvider.getTopLevelCallableSymbols(packageFqName, name)
            .map { symbol -> analysisSession.cfirSymbolBuilder.buildSymbol(symbol) as CaCallableSymbol }
    }

    override fun getTopLevelExtendSymbols(packageFqName: FqName): List<CaExtendSymbol> = withValidityAssertion {
        analysisSession.cfirSession.extendProviderOrNull
            ?.getExtendsInPackage(packageFqName)
            ?.map { extend -> analysisSession.cfirSymbolBuilder.buildExtendSymbol(extend.symbol) }
            .orEmpty()
    }

    override fun getExtendSymbols(targetClassId: ClassId): List<CaExtendSymbol> = withValidityAssertion {
        analysisSession.cfirSession.extendProviderOrNull
            ?.getExtendsForClass(targetClassId)
            ?.map { extend -> analysisSession.cfirSymbolBuilder.buildExtendSymbol(extend.symbol) }
            .orEmpty()
    }
}
