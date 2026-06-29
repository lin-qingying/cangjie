package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.findPsi
import org.cangnova.cangjie.analysis.api.cfir.getAllowedPsi
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirPatternBindingSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirPatternVariableSymbolPointer
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPatternBindingSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPatternVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternVariableSymbol
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjBindingPattern
import org.cangnova.cangjie.psi.CjPatternVariable
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.cfir.getExplicitCallableReceiverType

/**
 * 局部变量族叶子实现。
 *
 * 把普通局部变量、模式变量、模式绑定变量的本地可见性语义集中在同一簇，
 * 避免与属性或值参数混在一起。
 */
internal open class CaCfirLocalVariableSymbol(
    /**
     * 底层 CFIR callable 符号。
     */
    final override val cfirSymbol: CfirCallableSymbol<*>,
    /**
     * 当前符号绑定的 CFIR Analysis session。
     */
    final override val analysisSession: CaCfirSession,
    /**
     * 局部变量所在模块。
     */
    final override val containingModule: CaModule,
    /**
     * 局部变量符号生命周期 token。
     */
    final override val token: CaLifetimeToken,
) : org.cangnova.cangjie.analysis.api.symbols.CaLocalVariableSymbol(),
    CaCfirSymbol<CfirCallableSymbol<*>> {
    /**
     * 局部变量对应的 PSI。
     */
    override val psi: PsiElement?
        get() = withValidityAssertion { cfirSymbol.cfir.getAllowedPsi(analysisSession.project) ?: findPsi() }

    /**
     * 局部变量公开注解列表。
     */
    override val annotations: CaAnnotationList
        get() = withValidityAssertion { CaCfirAnnotationListForDeclaration.create(cfirSymbol, builder) }

    /**
     * 局部变量没有稳定 callableId。
     */
    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = null

    /**
     * 局部变量显式 receiver 类型。
     */
    override val receiverType: CaType?
        get() = analysisSession.getExplicitCallableReceiverType(backingPsi = null, builder) { cfirSymbol }

    /**
     * 局部变量类型。
     */
    override val returnType: CaType
        get() = cfirSymbol.returnType(builder)

    /**
     * 局部变量可见性固定为 local。
     */
    override val visibility: CaSymbolVisibility
        get() = CaSymbolVisibility.LOCAL

    /**
     * 局部变量不显式声明可见性。
     */
    override val isVisibilityExplicit: Boolean
        get() = false

    /**
     * 局部变量 modality 固定为 final。
     */
    override val modality: CaSymbolModality?
        get() = CaSymbolModality.FINAL

    /**
     * 局部变量不显式声明 modality。
     */
    override val isModalityExplicit: Boolean
        get() = false

    /**
     * 局部变量公开符号位置固定为 local。
     */
    override val location: CaSymbolLocation
        get() = CaSymbolLocation.LOCAL

    /**
     * 无源码 PSI 的局部变量不能创建稳定 pointer。
     */
    override fun createPointer(): CaSymbolPointer<CaCallableSymbol> = withValidityAssertion {
        error("Local variable symbol cannot create a stable pointer without source PSI")
    }

    /**
     * 局部变量是否不可变。
     */
    override val isLet: Boolean
        get() = when (val currentDeclaration = cfirSymbol.cfir) {
            is CfirPatternVariable -> !currentDeclaration.isVar
            is CfirPatternBindingVariable -> !currentDeclaration.isVar
            else -> true
        }

    /**
     * 局部变量名称。
     */
    override val name: Name
        get() = cfirSymbol.name
}

/**
 * CFIR pattern variable 符号实现。
 */
internal class CaCfirPatternVariableSymbol private constructor(
    /**
     * pattern variable 对应的源码 PSI。
     */
    override val backingPsi: CjPatternVariable?,
    /**
     * 当前符号绑定的 CFIR Analysis session。
     */
    override val analysisSession: CaCfirSession,
    /**
     * 延迟取得的底层 CFIR pattern variable 符号。
     */
    override val lazyCfirSymbol: Lazy<CfirPatternVariableSymbol>,
) : CaPatternVariableSymbol(),
    CaCfirCjBasedSymbol<CjPatternVariable, CfirPatternVariableSymbol> {
    constructor(declaration: CjPatternVariable, session: CaCfirSession) : this(
        backingPsi = declaration,
        analysisSession = session,
        lazyCfirSymbol = lazyCfirSymbol(declaration, session),
    )

    constructor(symbol: CfirPatternVariableSymbol, session: CaCfirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? CjPatternVariable,
        analysisSession = session,
        lazyCfirSymbol = lazyOf(symbol),
    )

    /**
     * pattern variable 底层 CFIR 符号。
     */
    override val cfirSymbol: CfirPatternVariableSymbol
        get() = super<CaCfirCjBasedSymbol>.cfirSymbol

    /**
     * pattern variable 所在的 use-site 模块。
     */
    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    /**
     * pattern variable 对应的 PSI。
     */
    override val psi
        get() = withValidityAssertion {
            backingPsiOrFindCurrentPsi { cfirSymbol.cfir.getAllowedPsi(analysisSession.project) ?: findPsi() }
        }

    /**
     * pattern variable 公开来源。
     */
    override val origin
        get() = withValidityAssertion { psiOrSymbolOrigin() }

    /**
     * pattern variable 公开注解列表。
     */
    override val annotations: CaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    /**
     * pattern variable 没有稳定 callableId。
     */
    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = null

    /**
     * pattern variable 显式 receiver 类型。
     */
    override val receiverType: CaType?
        get() = analysisSession.getExplicitCallableReceiverType(backingPsi = null, builder) { cfirSymbol }

    /**
     * pattern variable 类型。
     */
    override val returnType: CaType
        get() = cfirSymbol.returnType(builder)

    /**
     * pattern variable 可见性固定为 local。
     */
    override val visibility: CaSymbolVisibility
        get() = CaSymbolVisibility.LOCAL

    /**
     * pattern variable 不显式声明可见性。
     */
    override val isVisibilityExplicit: Boolean
        get() = false

    /**
     * pattern variable modality 固定为 final。
     */
    override val modality: CaSymbolModality?
        get() = CaSymbolModality.FINAL

    /**
     * pattern variable 不显式声明 modality。
     */
    override val isModalityExplicit: Boolean
        get() = false

    /**
     * pattern variable 公开符号位置固定为 local。
     */
    override val location: CaSymbolLocation
        get() = CaSymbolLocation.LOCAL

    /**
     * 创建 pattern variable 符号 pointer。
     */
    override fun createPointer(): CaSymbolPointer<CaCallableSymbol> = withValidityAssertion {
        val sourcePsi = psi ?: error("Pattern variable symbol is missing PSI")
        @Suppress("UNCHECKED_CAST")
        CaCfirPatternVariableSymbolPointer(sourcePsi) as CaSymbolPointer<CaCallableSymbol>
    }

    /**
     * pattern variable 是否不可变。
     */
    override val isLet: Boolean
        get() = withValidityAssertion { backingPsi?.isVar != true }

    /**
     * pattern variable 名称。
     */
    override val name: Name
        get() = withValidityAssertion { backingPsi?.pattern?.let { (it as? CjBindingPattern)?.nameAsSafeName } ?: cfirSymbol.name }

    /**
     * 按 PSI 或 CFIR 符号身份比较 pattern variable。
     */
    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    /**
     * 按 PSI 或 CFIR 符号身份计算 pattern variable hash。
     */
    override fun hashCode(): Int = psiOrSymbolHashCode()
}

/**
 * CFIR pattern binding 符号实现。
 */
internal class CaCfirPatternBindingSymbol private constructor(
    /**
     * pattern binding 对应的源码 PSI。
     */
    override val backingPsi: CjBindingPattern?,
    /**
     * 当前符号绑定的 CFIR Analysis session。
     */
    override val analysisSession: CaCfirSession,
    /**
     * 延迟取得的底层 CFIR pattern binding 符号。
     */
    override val lazyCfirSymbol: Lazy<CfirPatternBindingSymbol>,
) : CaPatternBindingSymbol(),
    CaCfirCjBasedSymbol<CjBindingPattern, CfirPatternBindingSymbol> {
    constructor(declaration: CjBindingPattern, session: CaCfirSession) : this(
        backingPsi = declaration,
        analysisSession = session,
        lazyCfirSymbol = lazyCfirSymbol<CfirPatternBindingVariable, CfirPatternBindingSymbol>(
            declaration,
            session,
        ) { variable -> variable.symbol },
    )

    constructor(symbol: CfirPatternBindingSymbol, session: CaCfirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? CjBindingPattern,
        analysisSession = session,
        lazyCfirSymbol = lazyOf(symbol),
    )

    /**
     * pattern binding 底层 CFIR 符号。
     */
    override val cfirSymbol: CfirPatternBindingSymbol
        get() = super<CaCfirCjBasedSymbol>.cfirSymbol

    /**
     * pattern binding 所在的 use-site 模块。
     */
    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    /**
     * pattern binding 对应的 PSI。
     */
    override val psi
        get() = withValidityAssertion {
            backingPsiOrFindCurrentPsi { cfirSymbol.cfir.getAllowedPsi(analysisSession.project) ?: findPsi() }
        }

    /**
     * pattern binding 公开来源。
     */
    override val origin
        get() = withValidityAssertion { psiOrSymbolOrigin() }

    /**
     * pattern binding 公开注解列表。
     */
    override val annotations: CaAnnotationList
        get() = withValidityAssertion { CaCfirAnnotationListForDeclaration.create(cfirSymbol, builder) }

    /**
     * pattern binding 没有稳定 callableId。
     */
    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = null

    /**
     * pattern binding 显式 receiver 类型。
     */
    override val receiverType: CaType?
        get() = analysisSession.getExplicitCallableReceiverType(backingPsi = null, builder) { cfirSymbol }

    /**
     * pattern binding 类型。
     */
    override val returnType: CaType
        get() = cfirSymbol.returnType(builder)

    /**
     * pattern binding 可见性固定为 local。
     */
    override val visibility: CaSymbolVisibility
        get() = CaSymbolVisibility.LOCAL

    /**
     * pattern binding 不显式声明可见性。
     */
    override val isVisibilityExplicit: Boolean
        get() = false

    /**
     * pattern binding modality 固定为 final。
     */
    override val modality: CaSymbolModality?
        get() = CaSymbolModality.FINAL

    /**
     * pattern binding 不显式声明 modality。
     */
    override val isModalityExplicit: Boolean
        get() = false

    /**
     * pattern binding 公开符号位置固定为 local。
     */
    override val location: CaSymbolLocation
        get() = CaSymbolLocation.LOCAL

    /**
     * 创建 pattern binding 符号 pointer。
     */
    override fun createPointer(): CaSymbolPointer<CaCallableSymbol> = withValidityAssertion {
        val sourcePsi = psi ?: error("Pattern binding symbol is missing PSI")
        @Suppress("UNCHECKED_CAST")
        CaCfirPatternBindingSymbolPointer(sourcePsi) as CaSymbolPointer<CaCallableSymbol>
    }

    /**
     * pattern binding 是否不可变。
     */
    override val isLet: Boolean
        get() = withValidityAssertion { backingPsi?.variable?.isVar != true }

    /**
     * pattern binding 名称。
     */
    override val name: Name
        get() = withValidityAssertion { backingPsi?.nameAsSafeName ?: cfirSymbol.name }

    /**
     * 按 PSI 或 CFIR 符号身份比较 pattern binding。
     */
    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    /**
     * 按 PSI 或 CFIR 符号身份计算 pattern binding hash。
     */
    override fun hashCode(): Int = psiOrSymbolHashCode()
}
