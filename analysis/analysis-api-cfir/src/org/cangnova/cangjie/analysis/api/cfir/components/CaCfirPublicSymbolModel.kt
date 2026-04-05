package org.cangnova.cangjie.analysis.api.cfir.components

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaModule
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFileSymbol
import org.cangnova.cangjie.cfir.symbols.CfirSymbol
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile

/**
 * 所有公开 CFIR 符号共享的最小基类。
 */
internal sealed class CaCfirSymbolBase(
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : CaSymbol

/**
 * 公开符号与 low-level CFIR 符号之间的绑定协议。
 */
internal interface CaCfirBackedSymbol<T : CfirSymbol<*>> {
    val backingSymbol: T
}

/**
 * 统一承载 low-level symbol 到源码声明 PSI 的公开声明符号基类。
 */
internal abstract class CaCfirDeclarationBackedSymbol<T : CfirSymbol<*>>(
    final override val backingSymbol: T,
    protected val analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirSymbolBase(containingModule, token), CaDeclarationSymbol, CaCfirBackedSymbol<T> {
    final override val psi: PsiElement?
        get() = analysisSession.lookupSourcePsi(backingSymbol)
}

internal class CaCfirPackageSymbolImpl(
    override val fqName: FqName,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirSymbolBase(containingModule, token), CaPackageSymbol

internal class CaCfirFileSymbolImpl(
    override val backingSymbol: CfirFileSymbol,
    override val file: CjFile,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirSymbolBase(containingModule, token), CaFileSymbol, CaCfirBackedSymbol<CfirFileSymbol> {
    override val packageFqName: FqName
        get() = file.packageFqName
}

internal class CaCfirClassLikeSymbolImpl(
    backingSymbol: CfirClassLikeSymbol<*>,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirDeclarationBackedSymbol<CfirClassLikeSymbol<*>>(
    backingSymbol = backingSymbol,
    analysisSession = analysisSession,
    containingModule = containingModule,
    token = token,
), CaClassLikeSymbol {
    override val classId: ClassId
        get() = backingSymbol.classId
}

internal class CaCfirCallableSymbolImpl(
    backingSymbol: CfirCallableSymbol<*>,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirDeclarationBackedSymbol<CfirCallableSymbol<*>>(
    backingSymbol = backingSymbol,
    analysisSession = analysisSession,
    containingModule = containingModule,
    token = token,
), CaCallableSymbol {
    override val callableId: CallableId?
        get() = backingSymbol.callableId
}
