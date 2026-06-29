package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.cfir.*
import org.cangnova.cangjie.analysis.api.cfir.scopes.CaCfirDelegatingNamesAwareScope
import org.cangnova.cangjie.analysis.api.cfir.scopes.CaCfirDeclaredMemberScope
import org.cangnova.cangjie.analysis.api.cfir.scopes.CaCfirFileScope
import org.cangnova.cangjie.analysis.api.cfir.scopes.CaCfirPackageScope
import org.cangnova.cangjie.analysis.api.cfir.scopes.CfirSingleExtendDeclaredMemberScope
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirClassSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirExtendSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirFileSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirPackageSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirSymbol
import org.cangnova.cangjie.analysis.api.components.CaScopeProvider
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaDeclarationContainerSymbol
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.resolve.toClassLikeSymbol
import org.cangnova.cangjie.cfir.scopes.unsubstitutedScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassDeclaredMemberScope
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile

/**
 * 作用域组件。
 *
 * 所有 low-level scope 查询都统一通过 session 协议映射为公开 `CaScope`，
 * 不再保留额外的 snapshot 包装协议层。
 */
@OptIn(CaPlatformInterface::class, CaExperimentalApi::class)
internal class CaCfirScopeProvider(
    /**
     * 延迟取得当前 CFIR Analysis session，作用域查询需要复用其中的 scope session 与符号构建器。
     */
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaScopeProvider, CaCfirSessionComponent {
    /**
     * 取得当前 CFIR session 对应的作用域缓存会话。
     */
    private fun getScopeSession(): ScopeSession {
        return analysisSession.getScopeSessionFor(analysisSession.cfirSession)
    }

    /**
     * 将公开声明容器符号还原为可用于底层作用域查询的 CFIR class-like 声明。
     */
    private fun CaDeclarationContainerSymbol.getCfirForScope(): CfirClassLikeDeclaration = when (this) {
        is CaCfirClassSymbol -> cfirSymbol.cfir
        else -> error(
            "`${this::class.qualifiedName}` needs to be specially handled by the scope provider or is an unknown" +
                    " ${CaDeclarationContainerSymbol::class.simpleName} implementation."
        )
    }

    /**
     * 返回声明容器的可见成员作用域。
     */
    override val CaDeclarationContainerSymbol.memberScope: CaScope
        get() = withValidityAssertion {
            val cfirScope = getCfirForScope().unsubstitutedScope(
                analysisSession.cfirSession,
                getScopeSession(),
                withForcedTypeCalculator = false,
                memberRequiredPhase = CfirResolvePhase.STATUS,
            )
            CaCfirDelegatingNamesAwareScope(cfirScope, analysisSession.cfirSymbolBuilder)
        }

    /**
     * 返回文件级作用域，包含文件导入和包内短名解析视图。
     */
    override fun CjFile.getFileScope(): CaScope = withValidityAssertion {
        CaCfirFileScope(CaCfirFileSymbol(this@getFileScope, analysisSession), analysisSession.cfirSymbolBuilder)
    }

    /**
     * 按包名返回包级作用域，包不存在时返回 null。
     */
    override fun getPackageScope(packageFqName: FqName): CaScope? = withValidityAssertion {
        if (!analysisSession.useSitePackageProvider.doesPackageExist(packageFqName)) return@withValidityAssertion null
        CaCfirPackageScope(packageFqName, analysisSession)
    }

    /**
     * 返回包符号自身对应的包级作用域。
     */
    override val CaPackageSymbol.packageScope: CaScope
        get() = withValidityAssertion {
            val packageSymbol = this@packageScope as? CaCfirPackageSymbol
                ?: error("仅 CFIR 包符号支持包级作用域查询：${this@packageScope::class.simpleName}")
            CaCfirPackageScope(packageSymbol.fqName, analysisSession)
        }

    /**
     * 返回声明容器显式声明成员的组合视图。
     */
    override val CaDeclarationContainerSymbol.combinedDeclaredMemberScope: CaScope
        get() = withValidityAssertion {
            when (this@combinedDeclaredMemberScope) {
                is CaClassLikeSymbol -> {
                    val classSymbol = requireClassLikeSymbol(this@combinedDeclaredMemberScope)
                    CaCfirDeclaredMemberScope(
                        CfirClassDeclaredMemberScope(classSymbol.cfirSymbol),
                        analysisSession.cfirSymbolBuilder,
                    )
                }
                else -> error("当前仅 class-like 声明容器支持 combinedDeclaredMemberScope：${this@combinedDeclaredMemberScope::class.simpleName}")
            }
        }

    /**
     * 返回 class-like 符号直接声明的成员作用域。
     */
    override val CaClassLikeSymbol.declaredMemberScope: CaScope
        get() = withValidityAssertion {
            val classSymbol = requireClassLikeSymbol(this@declaredMemberScope)
            CaCfirDeclaredMemberScope(
                CfirClassDeclaredMemberScope(classSymbol.cfirSymbol),
                analysisSession.cfirSymbolBuilder,
            )
        }

    /**
     * 返回 extend 声明直接贡献的成员作用域。
     */
    override val org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol.declaredMemberScope: CaScope
        get() = withValidityAssertion {
            val extendSymbol = this@declaredMemberScope as? CaCfirExtendSymbol
                ?: error("Only CFIR extend symbols can expose declared-member scope: ${this@declaredMemberScope::class.simpleName}")
            CaCfirDeclaredMemberScope(
                CfirSingleExtendDeclaredMemberScope(extendSymbol.cfirSymbol.cfir),
                analysisSession.cfirSymbolBuilder,
            )
        }

    /**
     * 返回 class-like 符号包含继承成员在内的成员作用域。
     */
    override val CaClassLikeSymbol.memberScope: CaScope
        get() = withValidityAssertion {
            val classSymbol = requireClassLikeSymbol(this@memberScope)
            val cfirScope = classSymbol.cfirSymbol.cfir.unsubstitutedScope(
                analysisSession.cfirSession,
                getScopeSession(),
                withForcedTypeCalculator = false,
                memberRequiredPhase = CfirResolvePhase.STATUS,
            )
            CaCfirDelegatingNamesAwareScope(cfirScope, analysisSession.cfirSymbolBuilder)
        }

    /**
     * 返回类型对应 class-like 声明的成员作用域。
     */
    override val org.cangnova.cangjie.analysis.api.types.CaType.scope: CaScope?
        get() = withValidityAssertion {
            val classLikeSymbol = this@scope.requireCfirConeType("成员作用域查询")
                .toClassLikeSymbol(analysisSession.cfirSession)
                ?: return@withValidityAssertion null
            val cfirScope = classLikeSymbol.cfir.unsubstitutedScope(
                analysisSession.cfirSession,
                getScopeSession(),
                withForcedTypeCalculator = false,
                memberRequiredPhase = CfirResolvePhase.STATUS,
            )
            CaCfirDelegatingNamesAwareScope(cfirScope, analysisSession.cfirSymbolBuilder)
        }

    /**
     * 校验公开 class-like 符号确实由 CFIR 符号承载。
     */
    private fun requireClassLikeSymbol(symbol: CaClassLikeSymbol): CaCfirSymbol<CfirClassLikeSymbol<*>> {
        return symbol as? CaCfirSymbol<CfirClassLikeSymbol<*>>
            ?: error("仅 CFIR class-like 符号支持成员作用域查询：${symbol::class.simpleName}")
    }

}
