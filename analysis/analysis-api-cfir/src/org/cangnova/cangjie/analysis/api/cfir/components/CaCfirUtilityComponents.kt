package org.cangnova.cangjie.analysis.api.cfir.components

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.CaLibraryModule
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaCInteropComponent
import org.cangnova.cangjie.analysis.api.components.CaDataFlowProvider
import org.cangnova.cangjie.analysis.api.components.CaDocProvider
import org.cangnova.cangjie.analysis.api.components.CaEvaluator
import org.cangnova.cangjie.analysis.api.components.CaExpressionInformationProvider
import org.cangnova.cangjie.analysis.api.components.CaImportOptimizer
import org.cangnova.cangjie.analysis.api.components.CaOriginalPsiProvider
import org.cangnova.cangjie.analysis.api.components.CaReferenceShortener
import org.cangnova.cangjie.analysis.api.components.CaRenderer
import org.cangnova.cangjie.analysis.api.components.CaSourceProvider
import org.cangnova.cangjie.analysis.api.components.CaSymbolInformationProvider
import org.cangnova.cangjie.analysis.api.components.CaVisibilityChecker
import org.cangnova.cangjie.analysis.api.dataFlow.CaDataFlowInfo
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledPsiProvider
import org.cangnova.cangjie.analysis.api.evaluation.CaCompileTimeValue
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSymbolProvider
import org.cangnova.cangjie.analysis.api.imports.CaImportOptimizationPlan
import org.cangnova.cangjie.analysis.api.imports.CaReferenceShorteningPlan
import org.cangnova.cangjie.analysis.api.interop.CaInteropInfo
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaProjectStructureProvider
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeAliasSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.symbols.CaScriptSymbol
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjExpression
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
 * CFIR 符号与工具链组件集合。
 *
 * 这里承载的是 Analysis API 对外暴露的“可消费工具能力”，例如：
 * 符号查询、源码导航、文档、渲染、编译期求值、导入优化和可见性判断。
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

    override val CjTypeStatement.classSymbol: org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
        get() = withValidityAssertion {
            analysisSession.getPublicSymbolByPsi<org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol>(this@classSymbol)
                ?: error("Cannot build class symbol for ${this@classSymbol::class}")
        }

    override val org.cangnova.cangjie.psi.CjExtend.symbol: org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
        get() = withValidityAssertion {
            analysisSession.getPublicSymbolByPsi<org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol>(this@symbol)
                ?: error("Cannot build extend symbol for ${this@symbol::class}")
        }

    override val CjTypeAlias.symbol: CaTypeAliasSymbol
        get() = withValidityAssertion {
            analysisSession.getPublicSymbolByPsi<CaTypeAliasSymbol>(this@symbol)
                ?: error("Cannot build type-alias symbol for ${this@symbol::class}")
        }

    override val CjNamedFunction.symbol: CaNamedFunctionSymbol
        get() = withValidityAssertion {
            analysisSession.getPublicSymbolByPsi<CaNamedFunctionSymbol>(this@symbol)
                ?: error("Cannot build named-function symbol for ${this@symbol::class}")
        }

    override val org.cangnova.cangjie.psi.CjFunctionLiteral.symbol: org.cangnova.cangjie.analysis.api.symbols.CaAnonymousFunctionSymbol
        get() = withValidityAssertion {
            analysisSession.getPublicSymbolByPsi<org.cangnova.cangjie.analysis.api.symbols.CaAnonymousFunctionSymbol>(this@symbol)
                ?: error("Cannot build anonymous-function symbol for ${this@symbol::class}")
        }

    override val org.cangnova.cangjie.psi.CjConstructor<*>.symbol: org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
        get() = withValidityAssertion {
            analysisSession.getPublicSymbolByPsi<org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol>(this@symbol)
                ?: error("Cannot build constructor symbol for ${this@symbol::class}")
        }

    override val org.cangnova.cangjie.psi.CjMacroDeclaration.symbol: org.cangnova.cangjie.analysis.api.symbols.CaMacroSymbol
        get() = withValidityAssertion {
            analysisSession.getPublicSymbolByPsi<org.cangnova.cangjie.analysis.api.symbols.CaMacroSymbol>(this@symbol)
                ?: error("Cannot build macro symbol for ${this@symbol::class}")
        }

    override val org.cangnova.cangjie.psi.CjFinalizer.symbol: org.cangnova.cangjie.analysis.api.symbols.CaFinalizerSymbol
        get() = withValidityAssertion {
            analysisSession.getPublicSymbolByPsi<org.cangnova.cangjie.analysis.api.symbols.CaFinalizerSymbol>(this@symbol)
                ?: error("Cannot build finalizer symbol for ${this@symbol::class}")
        }

    override val CjProperty.symbol: CaPropertySymbol
        get() = withValidityAssertion {
            analysisSession.getPublicSymbolByPsi<CaPropertySymbol>(this@symbol)
                ?: error("Cannot build property symbol for ${this@symbol::class}")
        }

    override val org.cangnova.cangjie.psi.CjPropertyAccessor.symbol: org.cangnova.cangjie.analysis.api.symbols.CaPropertyAccessorSymbol
        get() = withValidityAssertion {
            analysisSession.getPublicSymbolByPsi<org.cangnova.cangjie.analysis.api.symbols.CaPropertyAccessorSymbol>(this@symbol)
                ?: error("Cannot build property-accessor symbol for ${this@symbol::class}")
        }

    override val org.cangnova.cangjie.psi.CjFieldVariable.symbol: org.cangnova.cangjie.analysis.api.symbols.CaFieldSymbol
        get() = withValidityAssertion {
            analysisSession.getPublicSymbolByPsi<org.cangnova.cangjie.analysis.api.symbols.CaFieldSymbol>(this@symbol)
                ?: error("Cannot build field symbol for ${this@symbol::class}")
        }

    override val org.cangnova.cangjie.psi.CjEnumConstructor.symbol: org.cangnova.cangjie.analysis.api.symbols.CaEnumEntrySymbol
        get() = withValidityAssertion {
            analysisSession.getPublicSymbolByPsi<org.cangnova.cangjie.analysis.api.symbols.CaEnumEntrySymbol>(this@symbol)
                ?: error("Cannot build enum-entry symbol for ${this@symbol::class}")
        }

    override val CjPatternVariable.symbol: org.cangnova.cangjie.analysis.api.symbols.CaPatternVariableSymbol
        get() = withValidityAssertion {
            analysisSession.getPublicSymbolByPsi<org.cangnova.cangjie.analysis.api.symbols.CaPatternVariableSymbol>(this@symbol)
                ?: error("Cannot build pattern-variable symbol for ${this@symbol::class}")
        }

    override val org.cangnova.cangjie.psi.CjBindingPattern.symbol: org.cangnova.cangjie.analysis.api.symbols.CaPatternBindingSymbol
        get() = withValidityAssertion {
            analysisSession.getPublicSymbolByPsi<org.cangnova.cangjie.analysis.api.symbols.CaPatternBindingSymbol>(this@symbol)
                ?: error("Cannot build pattern-binding symbol for ${this@symbol::class}")
        }

    override val CjParameter.symbol: CaVariableSymbol
        get() = withValidityAssertion {
            analysisSession.getPublicSymbolByPsi<CaVariableSymbol>(this@symbol)
                ?: error("Cannot build variable symbol for ${this@symbol::class}")
        }

    override val CjTypeParameter.symbol: CaTypeParameterSymbol
        get() = withValidityAssertion {
            analysisSession.getPublicSymbolByPsi<CaTypeParameterSymbol>(this@symbol)
                ?: error("Cannot build type-parameter symbol for ${this@symbol::class}")
        }

    override fun getPackageSymbol(fqName: FqName): CaPackageSymbol? = withValidityAssertion {
        analysisSession.getPackagePublicSymbol(fqName)
    }

    override fun getClassLikeSymbol(classId: ClassId): CaClassLikeSymbol? = withValidityAssertion {
        analysisSession.getClassLikePublicSymbol(classId)
    }

    override fun getClassSymbol(classId: ClassId): org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol? = withValidityAssertion {
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

    override fun getTopLevelExtendSymbols(packageFqName: FqName): List<org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol> = withValidityAssertion {
        analysisSession.getTopLevelExtendPublicSymbols(packageFqName)
    }

    override fun getExtendSymbols(targetClassId: ClassId): List<org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol> = withValidityAssertion {
        analysisSession.getExtendPublicSymbols(targetClassId)
    }
}

/**
 * C 互操作信息入口。
 */
internal class CaCfirCInteropComponent(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaCInteropComponent {
    override fun CjElement.getInteropInfo(): CaInteropInfo? = withValidityAssertion {
        analysisSession.getInteropInfo(this@getInteropInfo)
    }

    override fun CaSymbol.getInteropInfo(): CaInteropInfo? = withValidityAssertion {
        analysisSession.getInteropInfo(this@getInteropInfo)
    }
}

/**
 * 符号指针与恢复入口。
 */
internal class CaCfirSymbolInformationProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaSymbolInformationProvider {
    override fun CaSymbol.createPointer(): CaSymbolPointer<CaSymbol> = withValidityAssertion {
        @Suppress("UNCHECKED_CAST")
        CaCfirSymbolPointerDelegate<CaSymbol>(createRestoreKey())
    }
}

/**
 * 表达式结构与常量性质入口。
 */
internal class CaCfirExpressionInformationProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaExpressionInformationProvider {
    override val CjExpression.isStatementLike: Boolean
        get() = withValidityAssertion {
            this@isStatementLike.isStatementLikeExpression()
        }

    override val CjExpression.isCompileTimeConstant: Boolean
        get() = withValidityAssertion {
            analysisSession.evaluateCompileTimeValue(this@isCompileTimeConstant) != null
        }
}

/**
 * 编译期求值入口。
 */
internal class CaCfirEvaluator(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaEvaluator {
    override fun CjExpression.evaluate(): CaCompileTimeValue? = withValidityAssertion {
        analysisSession.evaluateCompileTimeValue(this@evaluate)
    }
}

/**
 * 引用缩短规划入口。
 */
internal class CaCfirReferenceShortener(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaReferenceShortener {
    override fun CjFile.collectReferenceShorteningPlan(): CaReferenceShorteningPlan = withValidityAssertion {
        analysisSession.collectReferenceShorteningPlan(this@collectReferenceShorteningPlan)
    }
}

/**
 * 导入优化规划入口。
 */
internal class CaCfirImportOptimizer(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaImportOptimizer {
    override fun CjFile.collectImportOptimizationPlan(): CaImportOptimizationPlan = withValidityAssertion {
        analysisSession.collectImportOptimizationPlan(this@collectImportOptimizationPlan)
    }
}

/**
 * 统一文本渲染入口。
 */
internal class CaCfirRenderer(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaRenderer {
    override fun CaSymbol.render(): String = withValidityAssertion {
        when (this@render) {
            is CaPackageSymbol -> fqName.asString()
            is CaFileSymbol -> "${packageFqName.asString()}/${file.name}"
            is CaDeclarationSymbol -> render(org.cangnova.cangjie.analysis.api.renderer.declarations.impl.CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES)
            else -> name?.asString() ?: this@render::class.simpleName.orEmpty()
        }
    }

    override fun CaDeclarationSymbol.render(
        renderer: org.cangnova.cangjie.analysis.api.renderer.declarations.CaDeclarationRenderer,
    ): String = withValidityAssertion {
        renderer.renderDeclaration(analysisSession, this@render)
    }

    override fun CaType.render(): String = withValidityAssertion {
        render(
            renderer = org.cangnova.cangjie.analysis.api.renderer.types.impl.CaTypeRendererForSource.WITH_QUALIFIED_NAMES,
            position = org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRendererPosition.INVARIANT,
        )
    }

    override fun CaType.render(
        renderer: org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRenderer,
        position: org.cangnova.cangjie.analysis.api.renderer.types.CaTypeRendererPosition,
    ): String = withValidityAssertion {
        renderer.renderType(this@render, position)
    }

    /**
     * 统一渲染公开 callable 符号。
     */

    /**
     * 当前公开 API 尚未暴露完整 class-like kind 模型，这里稳定输出注解前缀加 ClassId。
     */
}

/**
 * 当前 session 视角下的可见性判定。
 */
internal class CaCfirVisibilityChecker(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaVisibilityChecker, CaCfirSessionComponent {
    override fun CaSymbol.isVisible(): Boolean = withValidityAssertion {
        when (this@isVisible) {
            is CaPackageSymbol -> analysisSession.hasVisiblePackage(fqName)
            is CaCfirFileSymbolImpl -> analysisSession.lookupFileSymbol(file) != null
            is org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol -> {
                val restoredSymbol = when (this@isVisible) {
                    is CaCfirExtendSymbolImpl -> analysisSession.restoreExtendPublicSymbol(extendId)
                    else -> null
                }
                restoredSymbol === this@isVisible
            }
            is org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol -> {
                val restoredSymbol = when (this@isVisible) {
                    is CaCfirClassLikeSymbolBase<*> -> classId?.let(analysisSession::getClassLikePublicSymbol)
                    else -> null
                }
                restoredSymbol === this@isVisible
            }

            is CaCallableSymbol -> {
                val restoredSymbol = when (this@isVisible) {
                    is CaCfirCallableSymbolBase<*> -> when (val cacheKey = publicSymbolCacheKeyOrNull()) {
                        is CaCfirCallableSymbolCacheKey -> analysisSession.restoreCallablePublicSymbol(cacheKey.callableId, cacheKey.kind)
                        is CaCfirPsiSymbolCacheKey -> psi?.let { psiElement ->
                            analysisSession.lookupSymbolsByPsi(psiElement)
                                .map(analysisSession::getPublicSymbol)
                                .singleOrNull { candidate -> candidate === this@isVisible }
                        }
                        else -> null
                    }
                    else -> null
                }
                restoredSymbol === this@isVisible
            }

            else -> false
        }
    }
}

/**
 * 原始 PSI 定位入口。
 */
internal class CaCfirOriginalPsiProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaOriginalPsiProvider {
    override fun CaSymbol.getOriginalPsi(): PsiElement? = withValidityAssertion {
        when (this@getOriginalPsi) {
            is CaFileSymbol -> file
            is CaCfirExtendSymbolImpl -> extendPsi ?: psi
            is CaDeclarationSymbol -> psi
            is org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol,
            is CaCallableSymbol,
            is CaPackageSymbol -> resolveContainingFile(this@getOriginalPsi)
            else -> null
        }
    }

    private fun resolveContainingFile(symbol: CaSymbol): CjFile? {
        if (symbol is CaDeclarationSymbol) {
            (symbol.psi?.containingFile as? CjFile)?.let { return it }
        }
        if (symbol is CaCfirBackedSymbol<*>) {
            analysisSession.lookupContainingFile(symbol.backingSymbol)?.let { return it }
        }

        val packageFqName = symbol.decompiledContainingPackageFqName() ?: return null
        return analysisSession.findDecompiledContainingFile(packageFqName)
    }
}

/**
 * 数据流快照入口。
 */
internal class CaCfirDataFlowProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaDataFlowProvider {
    override fun CjExpression.getDataFlowInfo(): CaDataFlowInfo = withValidityAssertion {
        analysisSession.getDataFlowInfo(this@getDataFlowInfo)
    }
}

/**
 * 符号到源码文件的稳定导航入口。
 */
internal class CaCfirSourceProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaSourceProvider {
    override fun CaSymbol.getContainingFile(): CjFile? = withValidityAssertion {
        when (this@getContainingFile) {
            is CaFileSymbol -> file
            is CaCfirExtendSymbolImpl -> (extendPsi?.containingFile as? CjFile) ?: decompiledFallbackFile(this@getContainingFile)
            is CaDeclarationSymbol -> {
                (psi?.containingFile as? CjFile)
                    ?: (this@getContainingFile as? CaCfirBackedSymbol<*>)?.let { symbol ->
                        analysisSession.lookupContainingFile(symbol.backingSymbol)
                    }
                    ?: decompiledFallbackFile(this@getContainingFile)
            }

            is CaCfirBackedSymbol<*> -> analysisSession.lookupContainingFile(backingSymbol)
                ?: decompiledFallbackFile(this@getContainingFile)

            else -> decompiledFallbackFile(this@getContainingFile)
        }
    }

    private fun decompiledFallbackFile(symbol: CaSymbol): CjFile? {
        val packageFqName = symbol.decompiledContainingPackageFqName() ?: return null
        return analysisSession.findDecompiledContainingFile(packageFqName)
    }
}

/**
 * 统一抽取“这个 symbol 的声明应落在哪个 package binary 上”。
 *
 * 这里只暴露能够稳定回推 package 的公共 symbol 形态：
 * - file/class-like/callable/package
 * 其余场景若没有显式 PSI，就保持 `null`，避免引入猜测式回退。
 */
private fun CaSymbol.decompiledContainingPackageFqName(): FqName? = when (this) {
    is CaFileSymbol -> packageFqName
    is CaClassLikeSymbol -> classId?.packageFqName
    is CaExtendSymbol -> {
        val declarationPsi = when (this) {
            is CaCfirExtendSymbolImpl -> extendPsi?.containingFile as? CjFile
            else -> (this as? CaDeclarationSymbol)?.psi?.containingFile as? CjFile
        }
        declarationPsi?.packageFqName
            ?: (this as? CaCfirExtendSymbolImpl)?.extendPackageFqName
            ?: targetClassId?.packageFqName
    }
    is CaCallableSymbol -> callableId?.packageName
    is CaPackageSymbol -> fqName
    else -> null
}

/**
 * 统一的 decompiled fallback 查找入口。
 *
 * 规则是：
 * 1. 先尊重当前 analysis session 的 use-site module；
 * 2. 若 use-site 未命中，再固定按 builtins -> libraries 顺序搜索；
 * 3. 不做模糊匹配，只接受 binary index 能稳定命中的 decompiled file。
 *
 * 这样可以让 `CaSourceProvider` / `CaOriginalPsiProvider` 与 decompiled facade
 * 共用一套查找语义，避免普通库与 builtins 出现同包名时各自返回不同文件。
 */
private fun CaCfirSession.findDecompiledContainingFile(
    packageFqName: FqName,
    preferredModule: CaModule? = useSiteModule,
): CjFile? {
    val psiProvider = project.getService(CaDecompiledPsiProvider::class.java) ?: return null
    val projectStructure = CaProjectStructureProvider.getInstance(project)

    fun findInModule(module: CaModule?): CjFile? = when (module) {
        is CaBuiltinsModule -> psiProvider.findDecompiledFile(module, packageFqName)
        is CaLibraryModule -> psiProvider.findDecompiledFile(module, packageFqName)
        else -> null
    }

    findInModule(preferredModule)?.let { return it }

    projectStructure.allModules.filterIsInstance<CaBuiltinsModule>().forEach { module ->
        if (module === preferredModule) return@forEach
        psiProvider.findDecompiledFile(module, packageFqName)?.let { return it }
    }

    projectStructure.allModules.filterIsInstance<CaLibraryModule>().forEach { module ->
        if (module === preferredModule) return@forEach
        psiProvider.findDecompiledFile(module, packageFqName)?.let { return it }
    }

    return null
}

/**
 * 文档渲染入口。
 */
internal class CaCfirDocProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaDocProvider {
    override fun CaSymbol.documentation(): String? = withValidityAssertion {
        analysisSession.renderDocumentation(this@documentation)
    }
}
