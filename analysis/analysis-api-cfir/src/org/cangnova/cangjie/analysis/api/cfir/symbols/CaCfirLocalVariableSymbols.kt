package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.findPsi
import org.cangnova.cangjie.analysis.api.cfir.getAllowedPsi
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirPatternBindingSymbolPointer
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPatternBindingSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.declarations.CfirPatternBindingVariable
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPatternBindingSymbol
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjNamedPattern
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.llCfirModuleData
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.cfir.getExplicitCallableReceiverType

/**
 * 普通局部变量与 catch、command、资源宿主绑定的符号实现。
 *
 * 模式声明容器由 CaCfirPatternVariableSymbol 独立表示；模式叶子由
 * CaCfirPatternBindingSymbol 读取自己的声明状态和全局或局部位置。
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
     * 局部变量符号生命周期 token。
     */
    final override val token: CaLifetimeToken,
) : org.cangnova.cangjie.analysis.api.symbols.CaLocalVariableSymbol(),
    CaCfirPsiSymbol<CjElement, CfirCallableSymbol<*>> {
    /** catch、command 与资源保留各自规范 PSI，公开符号比较不依赖包装对象的创建次数。 */
    override val backingPsi: CjElement? = cfirSymbol.cfir.getAllowedPsi(analysisSession.project) as? CjElement

    override val lazyCfirSymbol: Lazy<CfirCallableSymbol<*>> = lazyOf(cfirSymbol)

    override val origin
        get() = withValidityAssertion { psiOrSymbolOrigin() }

    /**
     * 局部变量对应的 PSI。
     */
    override val psi: PsiElement?
        get() = withValidityAssertion { backingPsiOrFindCurrentPsi { findPsi() } }

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
        get() = analysisSession.getExplicitCallableReceiverType(backingPsi = null, builder) { cfirSymbolOrNull as? CfirCallableSymbol<*> }

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
        org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirLocalVariableSymbolPointer(checkNotNull(psi))
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

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    override fun hashCode(): Int = psiOrSymbolHashCode()
}


/**
 * CFIR pattern binding 符号实现。
 */
internal class CaCfirPatternBindingSymbol private constructor(
    /**
     * pattern binding 对应的源码 PSI。
     */
    override val backingPsi: CjNamedPattern?,
    /**
     * 当前符号绑定的 CFIR Analysis session。
     */
    override val analysisSession: CaCfirSession,
    /**
     * 延迟取得的底层 CFIR pattern binding 符号。
     */
    override val lazyCfirSymbol: Lazy<CfirPatternBindingSymbol>,
) : CaPatternBindingSymbol(),
    CaCfirCjBasedSymbol<CjNamedPattern, CfirPatternBindingSymbol> {
    constructor(declaration: CjNamedPattern, session: CaCfirSession) : this(
        backingPsi = declaration,
        analysisSession = session,
        lazyCfirSymbol = lazyCfirSymbol<CfirPatternBindingVariable, CfirPatternBindingSymbol>(
            declaration,
            session,
        ) { variable -> variable.symbol },
    )

    constructor(symbol: CfirPatternBindingSymbol, session: CaCfirSession) : this(
        backingPsi = symbol.cfir.getAllowedPsi(session.project) as? CjNamedPattern,
        analysisSession = session,
        lazyCfirSymbol = lazyOf(symbol),
    )

    /**
     * pattern binding 底层 CFIR 符号。
     */
    override val cfirSymbol: CfirPatternBindingSymbol
        get() = super<CaCfirCjBasedSymbol>.cfirSymbol

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
     * 只有全局 pattern binding 具有稳定 callableId。
     */
    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = withValidityAssertion { if (cfirSymbol.cfir.isLocal) null else cfirSymbol.callableId }

    /**
     * pattern binding 显式 receiver 类型。
     */
    override val receiverType: CaType?
        get() = analysisSession.getExplicitCallableReceiverType(backingPsi = null, builder) { cfirSymbolOrNull as? CfirCallableSymbol<*> }

    /**
     * pattern binding 类型。
     */
    override val returnType: CaType
        get() = cfirSymbol.returnType(builder)

    /**
     * pattern binding 的可见性由真实声明状态决定。
     */
    override val visibility: CaSymbolVisibility
        get() = withValidityAssertion {
            if (cfirSymbol.cfir.isLocal) CaSymbolVisibility.LOCAL
            else cfirSymbol.symbolDeclarationStatus.visibility.asPublicVisibility()
        }

    /**
     * pattern binding 不显式声明可见性。
     */
    override val isVisibilityExplicit: Boolean
        get() = withValidityAssertion { cfirSymbol.symbolDeclarationStatus.isVisibilityExplicit }

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
     * 模式叶子保持声明宿主的全局或局部位置。
     */
    override val location: CaSymbolLocation
        get() = withValidityAssertion { if (cfirSymbol.cfir.isLocal) CaSymbolLocation.LOCAL else CaSymbolLocation.TOP_LEVEL }

    /**
     * 创建 pattern binding 符号 pointer。
     */
    override fun createPointer(): CaSymbolPointer<CaCallableSymbol> = withValidityAssertion {
        val sourcePsi = psi ?: error("Pattern binding symbol is missing PSI")
        val sourceFile = sourcePsi.containingFile as? org.cangnova.cangjie.psi.CjFile
        val owningSession = cfirSymbol.cfir.moduleData.session as? org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirResolvableModuleSession
        val generatedGlobal = location == CaSymbolLocation.TOP_LEVEL && cfirSymbol.cfir.source?.let { source ->
            owningSession?.macroSemanticSnapshot?.isGeneratedSource(source)
        } == true
        if (sourceFile?.isCompiled == true || generatedGlobal) {
            val key = checkNotNull(cfirSymbol.publicSymbolCacheKeyOrNull(analysisSession) as? CaCfirCallableSymbolCacheKey) {
                "Compiled/generated global pattern binding must have a stable callable identity"
            }
            return@withValidityAssertion org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirCallableSymbolPointer(
                key, CaPatternBindingSymbol::class.java,
            )
        }
        @Suppress("UNCHECKED_CAST")
        CaCfirPatternBindingSymbolPointer(sourcePsi) as CaSymbolPointer<CaCallableSymbol>
    }

    /**
     * pattern binding 是否不可变。
     */
    override val isLet: Boolean
        get() = withValidityAssertion { !cfirSymbol.cfir.isVar }

    override val isConst: Boolean
        get() = withValidityAssertion { cfirSymbol.symbolDeclarationStatus.isConst }

    /**
     * pattern binding 名称。
     */
    override val name: Name
        get() = withValidityAssertion { cfirSymbol.name }

    /**
     * 按 PSI 或 CFIR 符号身份比较 pattern binding。
     */
    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    /**
     * 按 PSI 或 CFIR 符号身份计算 pattern binding hash。
     */
    override fun hashCode(): Int = psiOrSymbolHashCode()
}
