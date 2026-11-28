package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.findPsi
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirValueParameterSymbolPointer
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
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
import org.cangnova.cangjie.psi.CjParameter

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
    ) : this(
        backingPsi = symbol.backingPsiIfApplicable as? org.cangnova.cangjie.psi.CjParameter,
        analysisSession = session,
        lazyCfirSymbol = lazyOf(symbol),
    )

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
     * 与 Kotlin 值参数入口一致：源码使用正式 PSI pointer，二进制从真实 owner 计算参数下标。
     */
    override fun createPointer(): CaSymbolPointer<CaCallableSymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource<CaValueParameterSymbol> { source ->
            (source as? CjParameter)?.symbol
        }?.let { return@withValidityAssertion it }
        val owner = checkNotNull(builder.buildSymbol(cfirSymbol.containingDeclarationSymbol)) {
            "A binary value parameter must have a pointer-restorable owner."
        }
        CaCfirValueParameterSymbolPointer(owner.createPointer(), name, index)
    }

    /** 指针与仓颉稳定缓存键共享实际 owner 下标；与 Kotlin index 同源，不缓存外部传入值。 */
    internal val index: Int
        get() {
            val function = checkNotNull(cfirSymbol.containingDeclarationSymbol.cfir as? CfirFunction) {
                "A value parameter must belong to an actual function."
            }
            return function.valueParameters.indexOfFirst { it.symbol === cfirSymbol }.also {
                check(it >= 0) { "The function must retain its actual value parameter." }
            }
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
        get() = psi as? org.cangnova.cangjie.psi.CjParameter

    /** 公开包装共享源对象身份；参数与字段仍由实际符号类型区分。 */
    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)

    override fun hashCode(): Int = psiOrSymbolHashCode()

    private companion object {
        val ARRAY_CLASS_ID = ClassId(StandardNames.FqNames.core, StandardNames.ARRAY)
    }
}
