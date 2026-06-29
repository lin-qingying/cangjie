package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.findPsi
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirValueParameterSymbolPointer
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaValueParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaClassLikeType
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.isArray
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.source.psi

/**
 * 值参数叶子实现。
 *
 * 值参数的 owner 恢复、稳定索引和默认值语义都与普通局部变量不同，
 * 因此单独落位，避免继续依赖“大而全”的变量族文件。
 */
internal class CaCfirValueParameterSymbol private constructor(
    /**
     * 值参数对应的源码 PSI。
     */
    override val backingPsi: org.cangnova.cangjie.psi.CjParameter?,
    /**
     * 当前符号绑定的 CFIR Analysis session。
     */
    override val analysisSession: CaCfirSession,
    /**
     * 延迟取得的底层 CFIR 值参数符号。
     */
    override val lazyCfirSymbol: Lazy<CfirValueParameterSymbol>,
    /**
     * 参数所属的公开 owner 符号。
     */
    internal val ownerSymbol: CaValueParameterOwnerSymbol? = null,
    /**
     * 参数在 owner 值参数列表中的稳定下标。
     */
    internal val stableParameterIndex: Int? = null,
    /**
     * 外部构建时显式传入的参数 PSI。
     */
    private val explicitParameterPsi: org.cangnova.cangjie.psi.CjParameter? = null,
) : CaValueParameterSymbol(),
    CaCfirCjBasedSymbol<org.cangnova.cangjie.psi.CjParameter, CfirValueParameterSymbol> {
    /**
     * 值参数底层 CFIR 符号。
     */
    override val cfirSymbol: CfirValueParameterSymbol
        get() = super<CaCfirCjBasedSymbol>.cfirSymbol

    constructor(declaration: org.cangnova.cangjie.psi.CjParameter, session: CaCfirSession) : this(
        backingPsi = declaration,
        analysisSession = session,
        lazyCfirSymbol = lazyCfirSymbol(declaration, session),
    )

    constructor(
        symbol: CfirValueParameterSymbol,
        session: CaCfirSession,
        ownerSymbol: CaValueParameterOwnerSymbol? = null,
        stableParameterIndex: Int? = null,
        parameterPsi: org.cangnova.cangjie.psi.CjParameter? = null,
    ) : this(
        backingPsi = symbol.backingPsiIfApplicable as? org.cangnova.cangjie.psi.CjParameter ?: parameterPsi,
        analysisSession = session,
        lazyCfirSymbol = lazyOf(symbol),
        ownerSymbol = ownerSymbol,
        stableParameterIndex = stableParameterIndex,
        explicitParameterPsi = parameterPsi,
    )

    /**
     * 值参数所在的 use-site 模块。
     */
    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    /**
     * 值参数底层 CFIR 声明。
     */
    private val parameterDeclaration: CfirValueParameter
        get() = cfirSymbol.cfir

    /**
     * 值参数公开注解列表。
     */
    override val annotations: CaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    /**
     * 值参数对应的 PSI。
     */
    override val psi
        get() = withValidityAssertion { backingPsiOrFindCurrentPsi { findPsi() } }

    /**
     * 值参数公开来源。
     */
    override val origin
        get() = withValidityAssertion { psiOrSymbolOrigin() }

    /**
     * 值参数没有独立 callableId。
     */
    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = null

    /**
     * 值参数没有 receiver 类型。
     */
    override val receiverType: CaType?
        get() = withValidityAssertion { null }

    /**
     * 值参数类型。
     */
    override val returnType: CaType
        get() = withValidityAssertion { cfirSymbol.returnType(builder) }

    /**
     * 值参数公开符号位置固定为 local。
     */
    override val location: CaSymbolLocation
        get() = CaSymbolLocation.LOCAL

    /**
     * 创建基于 owner pointer、参数名和稳定下标的值参数 pointer。
     */
    override fun createPointer(): CaSymbolPointer<CaCallableSymbol> = withValidityAssertion {
        val owner = ownerSymbol ?: builder.buildSymbol(cfirSymbol.containingDeclarationSymbol)
            ?: error("Value parameter `${name}` is missing pointer-restorable owner")
        val parameterIndex = stableParameterIndex
            ?: error("Value parameter `${name}` is missing stable parameter index")
        CaCfirValueParameterSymbolPointer(owner.createPointer(), name, parameterIndex)
    }

    /**
     * 值参数名称。
     */
    override val name: Name
        get() = withValidityAssertion { backingPsi?.nameAsSafeName ?: cfirSymbol.name }

    /**
     * 值参数是否不可变。
     */
    override val isLet: Boolean
        get() = !parameterDeclaration.isVar

    /**
     * 值参数是否为命名参数。
     */
    override val isNamed: Boolean
        get() = parameterDeclaration.isNamed

    /**
     * 值参数是否为 vararg 参数。
     */
    override val isVararg: Boolean
        get() = withValidityAssertion {
            if (parameterDeclaration.isNamed) {
                return false
            }

            val ownerDeclaration = cfirSymbol.containingDeclarationSymbol.cfir as? CfirFunction ?: return false
            val parameterIndex = ownerDeclaration.valueParameters.indexOfFirst { candidate ->
                candidate.symbol.cfir === parameterDeclaration
            }
            if (parameterIndex < 0 || parameterIndex != ownerDeclaration.valueParameters.lastIndex) {
                return false
            }

            parameterDeclaration.returnTypeRef.coneTypeOrNull?.let { return it.isArray }

            val publicReturnType = returnType as? CaClassLikeType ?: return false
            return publicReturnType.classId == ARRAY_CLASS_ID
        }

    /**
     * 值参数是否声明默认值。
     */
    override val hasDefaultValue: Boolean
        get() = parameterDeclaration.defaultValue != null || resolvedParameterPsi?.defaultValue != null

    /**
     * 值参数可见性固定为 local。
     */
    override val visibility: CaSymbolVisibility
        get() = CaSymbolVisibility.LOCAL

    /**
     * 值参数不显式声明可见性。
     */
    override val isVisibilityExplicit: Boolean
        get() = false

    /**
     * 值参数 modality 固定为 final。
     */
    override val modality: CaSymbolModality?
        get() = CaSymbolModality.FINAL

    /**
     * 值参数不显式声明 modality。
     */
    override val isModalityExplicit: Boolean
        get() = false

    /**
     * 用于默认值和来源判断的最终参数 PSI。
     */
    private val resolvedParameterPsi: org.cangnova.cangjie.psi.CjParameter?
        get() = explicitParameterPsi
            ?: backingPsi
            ?: parameterDeclaration.source?.psi as? org.cangnova.cangjie.psi.CjParameter
            ?: psi as? org.cangnova.cangjie.psi.CjParameter

    private companion object {
        val ARRAY_CLASS_ID = ClassId(StandardNames.FqNames.core, StandardNames.ARRAY)
    }
}
