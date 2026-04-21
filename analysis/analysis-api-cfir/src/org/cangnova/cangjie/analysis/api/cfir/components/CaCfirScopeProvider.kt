package org.cangnova.cangjie.analysis.api.cfir.components

import org.cangnova.cangjie.analysis.api.cfir.*

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.getPublicSymbol
import org.cangnova.cangjie.analysis.api.cfir.scopes.CaCfirDeclaredMemberScope
import org.cangnova.cangjie.analysis.api.cfir.scopes.CaCfirFileScope
import org.cangnova.cangjie.analysis.api.cfir.scopes.CaCfirMemberScope
import org.cangnova.cangjie.analysis.api.cfir.scopes.CaCfirPackageScope
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirClassLikeSymbolBase
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirExtendSymbol
import org.cangnova.cangjie.analysis.api.cfir.symbols.CaCfirPackageSymbol
import org.cangnova.cangjie.analysis.api.components.CaScopeProvider
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.scopes.CaScope
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassifierSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.name
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile

/**
 * 作用域组件。
 *
 * 所有 low-level scope 查询都统一通过 session 协议映射为公开 `CaScope`，
 * 不再保留额外的 snapshot 包装协议层。
 */
internal class CaCfirScopeProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaScopeProvider, CaCfirSessionComponent {
    override fun CjFile.getFileScope(): CaScope = withValidityAssertion {
        CaCfirFileScope(
            fileDeclaredScope = analysisSession.scopeQueries.queryFileDeclaredScope(this@getFileScope),
            packageScope = analysisSession.scopeQueries.queryPackageScope(this@getFileScope.packageFqName),
            analysisSession = analysisSession,
            fileSymbol = with(analysisSession) { this@getFileScope.symbol },
            token = token,
        )
    }

    override fun getPackageScope(packageFqName: FqName): CaScope? = withValidityAssertion {
        val packageSymbol = with(analysisSession) { getPackageSymbol(packageFqName) } ?: return@withValidityAssertion null
        analysisSession.scopeQueries.queryPackageScope(packageFqName)?.let { packageScope ->
            CaCfirPackageScope(
                packageScope = packageScope,
                analysisSession = analysisSession,
                packageSymbol = packageSymbol,
                token = token,
            )
        } ?: error("无法为包 `${packageFqName.asString()}` 构建 package scope。")
    }

    override val CaPackageSymbol.packageScope: CaScope
        get() = withValidityAssertion {
            val packageSymbol = this@packageScope as? CaCfirPackageSymbol
                ?: error("仅 CFIR 包符号支持包级作用域查询：${this@packageScope::class.simpleName}")
            analysisSession.scopeQueries.queryPackageScope(packageSymbol.fqName)?.let { packageScope ->
                CaCfirPackageScope(
                    packageScope = packageScope,
                    analysisSession = analysisSession,
                    packageSymbol = packageSymbol,
                    token = token,
                )
            } ?: error("无法为包 `${packageSymbol.fqName.asString()}` 构建 package scope。")
        }

    override val CaClassLikeSymbol.declaredMemberScope: CaScope
        get() = withValidityAssertion {
            val classSymbol = requireClassLikeSymbol(this@declaredMemberScope)
            val classId = classSymbol.classId ?: error("Local/anonymous class-like symbols do not expose declared-member scope.")
            analysisSession.scopeQueries.queryDeclaredMemberScope(classId)?.let { declaredMemberScope ->
                CaCfirDeclaredMemberScope(declaredMemberScope, analysisSession, token)
            } ?: error("无法为 `${classId.asString()}` 构建 declared-member scope。")
        }

    override val org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol.declaredMemberScope: CaScope
        get() = withValidityAssertion {
            val extendSymbol = this@declaredMemberScope as? CaCfirExtendSymbol
                ?: error("Only CFIR extend symbols can expose declared-member scope: ${this@declaredMemberScope::class.simpleName}")
            extendSymbol.backingSymbol.cfir.declarations.asDeclarationListScope()
        }

    override val CaClassLikeSymbol.memberScope: CaScope
        get() = withValidityAssertion {
            val classSymbol = requireClassLikeSymbol(this@memberScope)
            val classId = classSymbol.classId ?: error("Local/anonymous class-like symbols do not expose use-site member scope.")
            analysisSession.scopeQueries.queryMemberScope(classId)?.let { memberScope ->
                CaCfirMemberScope(memberScope, analysisSession, token)
            } ?: error("无法为 `${classId.asString()}` 构建 use-site member scope。")
        }

    override val org.cangnova.cangjie.analysis.api.types.CaType.scope: CaScope?
        get() = withValidityAssertion {
            analysisSession.scopeQueries.queryTypeScope(this@scope.requireCfirConeType("成员作用域查询"))?.let { memberScope ->
                CaCfirMemberScope(memberScope, analysisSession, token)
            }
        }

    private fun requireClassLikeSymbol(symbol: CaClassLikeSymbol): CaCfirClassLikeSymbolBase<*> {
        return symbol as? CaCfirClassLikeSymbolBase<*>
            ?: error("仅 CFIR class-like 符号支持成员作用域查询：${symbol::class.simpleName}")
    }

    private fun List<org.cangnova.cangjie.cfir.declarations.CfirDeclaration>.asDeclarationListScope(): CaScope {
        val publicSymbols = mapNotNull { declaration ->
            when (declaration) {
                is org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration ->
                    analysisSession.getPublicSymbol(declaration.symbol) as? CaDeclarationSymbol
                is org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration ->
                    analysisSession.getPublicSymbol(declaration.symbol) as? CaDeclarationSymbol
                is org.cangnova.cangjie.cfir.declarations.CfirTypeParameter ->
                    analysisSession.getPublicSymbol(declaration.symbol) as? CaDeclarationSymbol
                is org.cangnova.cangjie.cfir.declarations.CfirExtend ->
                    analysisSession.getPublicSymbol(declaration.symbol) as? CaDeclarationSymbol
                else -> null
            }
        }
        val symbolsByName = publicSymbols.groupBy { symbol -> symbol.name ?: Name.special("<anonymous>") }
        return object : CaScope {
            override val token: CaLifetimeToken
                get() = this@CaCfirScopeProvider.token

            override val symbols: List<CaSymbol>
                get() = publicSymbols

            override val availableNames: Set<Name>
                get() = symbolsByName.keys

            override fun getSymbols(name: Name): List<CaSymbol> = symbolsByName[name].orEmpty()

            override fun getCallableSymbols(name: Name): List<CaCallableSymbol> =
                symbolsByName[name].orEmpty().filterIsInstance<CaCallableSymbol>()

            override fun getClassifierSymbols(name: Name): List<CaClassifierSymbol> =
                symbolsByName[name].orEmpty().filterIsInstance<CaClassifierSymbol>()
        }
    }
}
