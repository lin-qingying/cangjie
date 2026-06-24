package org.cangnova.cangjie.cfir.scopes

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.ScopeSessionKey
import org.cangnova.cangjie.cfir.declarations.CfirClass
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityFileScope
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.scopes.impl.CfirScopeWithCallableCopyReturnTypeUpdater
import org.cangnova.cangjie.cfir.scopes.CallableCopyTypeCalculator
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassMemberScopeKind
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassSubstitutionScope
import org.cangnova.cangjie.cfir.scopes.impl.CfirPackageMemberScope
import org.cangnova.cangjie.cfir.scopes.impl.TypeAliasConstructorsSubstitutingScope
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.name.FqName

/**
 * 仓颉语言的 scope 提供者，对标 K2 FirKotlinScopeProvider。
 */
open class CfirCangJieScopeProvider : CfirScopeProvider(), CfirSessionComponent {
    /**
     * 返回 class 在 use-site 场景下的成员 scope。
     */
    override fun getUseSiteMemberScope(
        klass: CfirClass,
        useSiteSession: CfirSession,
        scopeSession: ScopeSession,
    ): CfirTypeScope {
        val classSymbol = klass.symbol as? CfirClassSymbol ?: return CfirTypeScope.Empty
        val useSitePackage = CfirAccessibilityFileScope.currentPackageFqName()
        return scopeSession.getOrBuild(CfirUseSiteMemberScopeKey(useSiteSession, classSymbol, useSitePackage), USE_SITE) {
            val rawScope = CfirClassUseSiteMemberScope(
                session = useSiteSession,
                classSymbol = classSymbol,
                symbolProvider = useSiteSession.symbolProvider,
                extendProvider = useSiteSession.extendProvider,
                directSupertypeProvider = useSiteSession.directSupertypeProviderOrNull,
                scopeKind = CfirClassMemberScopeKind.USE_SITE,
                useSitePackage = useSitePackage,
            )
            CfirClassSubstitutionScope(useSiteSession, rawScope, classSymbol.constructType())
        }
    }

    /**
     * 返回 typealias 构造器替换 scope。
     */
    override fun getTypealiasConstructorScope(
        typeAlias: CfirTypeAlias,
        useSiteSession: CfirSession,
        scopeSession: ScopeSession,
    ): CfirScope {
        return scopeSession.getOrBuild(useSiteSession to typeAlias.symbol, TYPEALIAS_CONSTRUCTOR) {
            TypeAliasConstructorsSubstitutingScope.initialize(typeAlias.symbol, useSiteSession, scopeSession)
        }
    }

    /**
     * 返回声明站点成员 scope。
     */
    override fun getDeclarationSiteMemberScope(
        klass: CfirClass,
        useSiteSession: CfirSession,
        scopeSession: ScopeSession,
    ): CfirTypeScope {
        val classSymbol = klass.symbol as? CfirClassSymbol ?: return CfirTypeScope.Empty
        return CfirClassUseSiteMemberScope(
            session = useSiteSession,
            classSymbol = classSymbol,
            symbolProvider = useSiteSession.symbolProvider,
            extendProvider = useSiteSession.extendProvider,
            directSupertypeProvider = useSiteSession.directSupertypeProviderOrNull,
            scopeKind = CfirClassMemberScopeKind.DECLARATION_SITE,
        )
    }

    /**
     * 返回包成员 scope。
     */
    fun getPackageMemberScope(
        packageFqName: FqName,
        symbolProvider: CfirSymbolProvider,
        useSiteSession: CfirSession,
        scopeSession: ScopeSession,
    ): CfirPackageScope {
        val key: ScopeSessionKey<PackageMemberScopeKey, CfirPackageMemberScope> = scopeSessionKey()
        return scopeSession.getOrBuild(PackageMemberScopeKey(packageFqName, useSiteSession), key) {
            CfirPackageMemberScope(packageFqName, useSiteSession)
        }
    }

    /**
     * 包成员 scope 缓存 key。
     *
     * @property packageFqName 被查询的包名。
     * @property useSiteSession 查询发生的 session。
     */
    private data class PackageMemberScopeKey(
        val packageFqName: FqName,
        val useSiteSession: CfirSession,
    )
}

/**
 * typealias constructor scope 的 ScopeSession key。
 */
private val TYPEALIAS_CONSTRUCTOR: ScopeSessionKey<Pair<CfirSession, org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol>, CfirScope> =
    scopeSessionKey()

/**
 * 返回 class-like 声明未经外部类型替换的成员 scope。
 */
fun CfirClassLikeDeclaration.unsubstitutedScope(
    useSiteSession: CfirSession,
    scopeSession: ScopeSession,
    withForcedTypeCalculator: Boolean,
    memberRequiredPhase: CfirResolvePhase?,
): CfirTypeScope {
    val scope = when (this) {
        is CfirClass -> scopeProvider.getUseSiteMemberScope(this, useSiteSession, scopeSession)
        else -> {
            val symbol = symbol as? CfirClassLikeSymbol<*> ?: return CfirTypeScope.Empty
            CfirClassUseSiteMemberScope(
                session = useSiteSession,
                classSymbol = symbol,
                symbolProvider = useSiteSession.symbolProvider,
                extendProvider = useSiteSession.extendProvider,
                directSupertypeProvider = useSiteSession.directSupertypeProviderOrNull,
                scopeKind = CfirClassMemberScopeKind.USE_SITE,
                useSitePackage = CfirAccessibilityFileScope.currentPackageFqName(),
            )
        }
    }
    if (withForcedTypeCalculator) return CfirScopeWithCallableCopyReturnTypeUpdater(scope, CallableCopyTypeCalculator.CalculateDeferredForceLazyResolution)
    return scope
}
