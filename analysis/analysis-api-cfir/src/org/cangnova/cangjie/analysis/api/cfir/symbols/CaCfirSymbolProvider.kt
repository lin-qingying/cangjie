package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.components.CaCfirSessionComponent
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSymbolProvider
import org.cangnova.cangjie.analysis.api.impl.base.components.withPsiValidityAssertion
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.platform.index.CaSymbolIndexEntry
import org.cangnova.cangjie.analysis.api.platform.index.CaSymbolIndexEntryKind
import org.cangnova.cangjie.analysis.api.platform.index.CaSymbolIndexNameFilter
import org.cangnova.cangjie.analysis.api.platform.index.CaSymbolIndexQueryService
import org.cangnova.cangjie.analysis.api.symbols.CaAnonymousFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
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
import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
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
    /**
     * 延迟取得当前 CFIR Analysis session。
     */
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSymbolProvider<CaCfirSession>(), CaCfirSessionComponent {
    /**
     * 将参数 PSI 转换为公开变量符号。
     */
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

    /**
     * 将文件 PSI 转换为公开文件符号。
     */
    override val CjFile.symbol: CaFileSymbol
        get() = withPsiValidityAssertion {
            CaCfirFileSymbol(this@symbol, analysisSession)
        }

    /**
     * 将类型声明 PSI 转换为公开类符号。
     */
    override val CjTypeStatement.classSymbol: CaClassSymbol
        get() = withPsiValidityAssertion {
            CaCfirClassSymbol(this@classSymbol, analysisSession)
        }

    /**
     * 将 typealias PSI 转换为公开 typealias 符号。
     */
    override val CjTypeAlias.symbol: CaTypeAliasSymbol
        get() = withPsiValidityAssertion {
            CaCfirTypeAliasSymbol(this@symbol, analysisSession)
        }

    /**
     * 将命名函数 PSI 转换为公开命名函数符号。
     */
    override val CjNamedFunction.symbol: CaNamedFunctionSymbol
        get() = withPsiValidityAssertion {
            CaCfirNamedFunctionSymbol(this@symbol, analysisSession)
        }

    /**
     * 将函数 literal PSI 转换为公开匿名函数符号。
     */
    override val CjFunctionLiteral.symbol: CaAnonymousFunctionSymbol
        get() = withPsiValidityAssertion {
            CaCfirAnonymousFunctionSymbol(this@symbol, analysisSession)
        }

    /**
     * 将构造器 PSI 转换为公开构造器符号。
     */
    override val CjConstructor<*>.symbol: CaConstructorSymbol
        get() = withPsiValidityAssertion {
            CaCfirConstructorSymbol(this@symbol, analysisSession)
        }

    /**
     * 将 macro 声明 PSI 转换为公开 macro 符号。
     */
    override val CjMacroDeclaration.symbol: CaMacroSymbol
        get() = withPsiValidityAssertion {
            CaCfirMacroSymbol(this@symbol, analysisSession)
        }

    /**
     * 将 finalizer PSI 转换为公开 finalizer 符号。
     */
    override val CjFinalizer.symbol: CaFinalizerSymbol
        get() = withPsiValidityAssertion {
            CaCfirFinalizerSymbol(this@symbol, analysisSession)
        }

    /**
     * 将属性 PSI 转换为公开属性符号。
     */
    override val CjProperty.symbol: CaPropertySymbol
        get() = withPsiValidityAssertion {
            CaCfirPropertySymbol(this@symbol, analysisSession)
        }

    /**
     * 将属性访问器 PSI 转换为公开 getter 或 setter 符号。
     */
    override val CjPropertyAccessor.symbol: CaPropertyAccessorSymbol
        get() = withPsiValidityAssertion {
            val propertySymbol = property.symbol
            if (isGetter) {
                propertySymbol.getter ?: error("Cannot build getter symbol for ${this@symbol::class}")
            } else {
                propertySymbol.setter ?: error("Cannot build setter symbol for ${this@symbol::class}")
            }
        }

    /**
     * 将字段变量 PSI 转换为公开字段符号。
     */
    override val CjFieldVariable.symbol: CaFieldSymbol
        get() = withPsiValidityAssertion {
            CaCfirFieldSymbol(this@symbol, analysisSession)
        }

    /**
     * 将 enum constructor PSI 转换为公开 enum constructor 符号。
     */
    override val CjEnumConstructor.symbol: CaEnumConstructorSymbol
        get() = withPsiValidityAssertion {
            CaCfirEnumConstructorSymbol(this@symbol, analysisSession)
        }

    /**
     * 将 pattern variable PSI 转换为公开 pattern variable 符号。
     */
    override val CjPatternVariable.symbol: CaPatternVariableSymbol
        get() = withPsiValidityAssertion {
            CaCfirPatternVariableSymbol(this@symbol, analysisSession)
        }

    /**
     * 将 binding pattern PSI 转换为公开 pattern binding 符号。
     */
    override val CjBindingPattern.symbol: CaPatternBindingSymbol
        get() = withPsiValidityAssertion {
            CaCfirPatternBindingSymbol(this@symbol, analysisSession)
        }

    /**
     * 将 extend PSI 转换为公开 extend 符号。
     */
    override val CjExtend.symbol: CaExtendSymbol
        get() = withPsiValidityAssertion {
            CaCfirExtendSymbol(this@symbol, analysisSession)
        }

    /**
     * 将类型参数 PSI 转换为公开类型参数符号。
     */
    override val CjTypeParameter.symbol: CaTypeParameterSymbol
        get() = withPsiValidityAssertion {
            CaCfirTypeParameterSymbol(this@symbol, analysisSession)
        }

    /**
     * 按包名查询公开包符号。
     */
    override fun getPackageSymbol(fqName: FqName, organizationName: Name?): CaPackageSymbol? = withValidityAssertion {
        analysisSession.cfirSymbolBuilder.createPackageSymbolIfOneExists(fqName, organizationName)
    }

    /**
     * 按 classId 查询公开 class-like 符号。
     */
    override fun getClassLikeSymbol(classId: ClassId): CaClassLikeSymbol? = withValidityAssertion {
        analysisSession.cfirSymbolBuilder.classifierBuilder.buildClassLikeSymbolByClassId(classId)
    }

    /**
     * 按 classId 查询公开类符号。
     */
    override fun getClassSymbol(classId: ClassId): CaClassSymbol? = withValidityAssertion {
        val symbol = analysisSession.cfirSession.symbolProvider.getClassLikeSymbolByClassId(classId) as? CfirClassSymbol
            ?: return@withValidityAssertion null
        analysisSession.cfirSymbolBuilder.classifierBuilder.buildClassSymbol(symbol)
    }

    /**
     * 按 classId 查询公开 typealias 符号。
     */
    override fun getTypeAliasSymbol(classId: ClassId): CaTypeAliasSymbol? = withValidityAssertion {
        val symbol = analysisSession.cfirSession.symbolProvider.getClassLikeSymbolByClassId(classId) as? CfirTypeAliasSymbol
            ?: return@withValidityAssertion null
        analysisSession.cfirSymbolBuilder.classifierBuilder.buildTypeAliasSymbol(symbol)
    }

    /**
     * 查询指定包和短名下的顶层 class-like 符号。
     */
    override fun getTopLevelClassLikeSymbols(packageFqName: FqName, name: Name): List<CaClassLikeSymbol> = withValidityAssertion {
        val classId = ClassId(packageFqName, name)
        analysisSession.cfirSymbolBuilder.classifierBuilder.buildClassLikeSymbolByClassId(classId)
            ?.let(::listOf)
            .orEmpty()
    }

    /**
     * 查询指定包和短名下的顶层 callable 符号。
     */
    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name): List<CaCallableSymbol> = withValidityAssertion {
        analysisSession.cfirSession.symbolProvider.getTopLevelCallableSymbols(packageFqName, name)
            .map { symbol -> analysisSession.cfirSymbolBuilder.buildSymbol(symbol) as CaCallableSymbol }
    }

    /**
     * 查询指定包下的顶层 extend 符号。
     */
    override fun getTopLevelExtendSymbols(packageFqName: FqName): List<CaExtendSymbol> = withValidityAssertion {
        analysisSession.cfirSession.extendProviderOrNull
            ?.getExtendsInPackage(packageFqName)
            ?.map { extend -> analysisSession.cfirSymbolBuilder.buildExtendSymbol(extend.symbol) }
            .orEmpty()
    }

    /**
     * 查询指定目标 classId 关联的 extend 符号。
     */
    override fun getExtendSymbols(targetClassId: ClassId): List<CaExtendSymbol> = withValidityAssertion {
        analysisSession.cfirSession.extendProviderOrNull
            ?.getExtendsForClass(targetClassId)
            ?.map { extend -> analysisSession.cfirSymbolBuilder.buildExtendSymbol(extend.symbol) }
            .orEmpty()
    }

    /**
     * 按名称前缀发现声明：名称来自宿主索引，符号由本提供器按包作用域恢复。
     *
     * 索引只给名称，恢复走与 [getTopLevelClassLikeSymbols] / [getTopLevelCallableSymbols]
     * 同一条查询路径，因此索引候选与作用域候选的来源、可见性语义完全一致。
     * 索引不可用时返回空集合，不降级为全工作区声明遍历。
     */
    override fun getDeclarationsByNamePrefix(
        namePrefix: String,
        isRepeatedInvocation: Boolean,
    ): List<CaDeclarationSymbol> = withValidityAssertion {
        val indexQuery = analysisSession.project
            .getService(CaSymbolIndexQueryService::class.java)
            ?.createSymbolIndexQuery(analysisSession.analysisScope)
            ?: return@withValidityAssertion emptyList()
        indexQuery.findDeclarations(CaSymbolIndexNameFilter(namePrefix, isRepeatedInvocation))
            .flatMap(::resolveIndexEntry)
            .distinct()
    }

    /**
     * 把名称条目恢复成公开符号。
     *
     * 条目里没有符号，只有身份：这一步是**唯一**把名称变成符号的地方，
     * 三个索引实现共用它，因此换索引不会改变候选符号集合。
     */
    private fun resolveIndexEntry(entry: CaSymbolIndexEntry): List<CaDeclarationSymbol> = when (entry.kind) {
        CaSymbolIndexEntryKind.CLASS_LIKE,
        CaSymbolIndexEntryKind.TYPE_ALIAS,
        -> getTopLevelClassLikeSymbols(entry.packageFqName, entry.name)

        CaSymbolIndexEntryKind.CALLABLE -> getTopLevelCallableSymbols(entry.packageFqName, entry.name)

        // 枚举构造器是枚举的成员：索引只记名字，这里按 ClassId 定位所属枚举再取其成员，
        // 因此同名不同 payload 个数的构造器会各自保留。
        CaSymbolIndexEntryKind.ENUM_CONSTRUCTOR -> {
            val owner = entry.ownerClassId?.let(::getClassLikeSymbol) ?: return emptyList()
            with(analysisSession) {
                owner.declaredMemberScope.callables(listOf(entry.name))
                    .filterIsInstance<CaEnumConstructorSymbol>()
                    .toList()
            }
        }
    }
}
