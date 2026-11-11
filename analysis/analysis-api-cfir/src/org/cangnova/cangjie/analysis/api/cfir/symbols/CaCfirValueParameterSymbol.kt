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
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.resolve.calls.cangjieVariadicParameterOrNull
import org.cangnova.cangjie.cfir.symbols.CfirValueParameterSymbol
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.cfir.types.isArray
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.SpecialNames
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
     * 值参数底层 CFIR 声明；符号不可恢复时为 `null`。
     *
     * 所有从 CFIR 派生的属性都必须经这里取值并给出安全默认值：
     * 缺符号是环境状态，不是编程错误，属性层不允许把它变成异常。
     */
    private val parameterDeclarationOrNull: CfirValueParameter?
        get() = cfirSymbolOrNull?.cfir

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
     *
     * 符号不可恢复时返回错误类型而不是抛异常：调用方（补全、提示、检查）据此跳过该候选，
     * 而不是让一次库符号缺失把整段分析带崩。
     */
    override val returnType: CaType
        get() = withValidityAssertion {
            val symbol = cfirSymbolOrNull
            if (symbol == null) {
                builder.typeBuilder.buildType(ConeErrorType(ConeSimpleDiagnostic("value parameter type is unavailable")))
            } else {
                symbol.returnType(builder)
            }
        }

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
     *
     * 优先取 PSI 名；符号不可恢复时退化为无名占位，而不是抛异常——
     * 缺符号是环境状态，展示层只需知道「这个名字拿不到」。
     */
    override val name: Name
        get() = withValidityAssertion {
            backingPsi?.nameAsSafeName ?: cfirSymbolOrNull?.name ?: SpecialNames.NO_NAME_PROVIDED
        }

    /**
     * 值参数是否不可变。
     *
     * 符号不可恢复时按「不可变」这一默认形态给出，而不是抛异常。
     */
    override val isLet: Boolean
        get() = parameterDeclarationOrNull?.let { !it.isVar } ?: true

    /**
     * 值参数是否为命名参数。
     *
     * 符号不可恢复时返回 `false`（不可判定的调用形态不应被猜成命名实参）。
     */
    override val isNamed: Boolean
        get() = parameterDeclarationOrNull?.isNamed ?: false

    /**
     * 值参数是否为变长（variadic）参数。
     *
     * 判定**不在本层**：仓颉的变长参数不是语法记号（那是 Kotlin 的 `vararg`），而是
     * 「最后一个**非命名**形参的类型是 `Array<T>`」这条调用语义规则，外加 enum 构造器与
     * 除 `()`/`[]` 外的运算符重载两个例外。唯一实现在
     * [org.cangnova.cangjie.cfir.resolve.calls.cangjieVariadicParameterOrNull]，
     * 调用解析与分析层共用，避免两边各写一份后漂移。
     *
     * 该规则读取的是形参**已定型**的类型（官方 `IsFuncDeclPossibleVariadic` 以
     * `Ty::IsTyCorrect(param->GetTy())` 为前提，
     * 见 `external/cangjie_compiler/src/Sema/TypeCheckCall.cpp:385-402`），而分析层拿到的
     * CFIR 符号默认只解析到 [org.cangnova.cangjie.cfir.declarations.CfirResolvePhase.RAW_CFIR]：
     * 不先推进阶段时形参类型引用尚未定型，
     * 规则会把真实变参静默判成非变参（因此本属性此前只有被 `returnType` 抢先推进后才正确）。
     */
    override val isVararg: Boolean
        get() = withValidityAssertion {
            val symbol = cfirSymbolOrNull ?: return@withValidityAssertion false
            symbol.ensureReturnTypeIsResolvable()
            val owner = symbol.containingDeclarationSymbol.cfir as? CfirDeclaration ?: return@withValidityAssertion false
            val declaration = parameterDeclarationOrNull ?: return@withValidityAssertion false
            owner.cangjieVariadicParameterOrNull() === declaration
        }

    /**
     * 值参数是否声明默认值。
     */
    override val hasDefaultValue: Boolean
        get() = parameterDeclarationOrNull?.defaultValue != null || resolvedParameterPsi?.defaultValue != null

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
            ?: parameterDeclarationOrNull?.source?.psi as? org.cangnova.cangjie.psi.CjParameter
            ?: psi as? org.cangnova.cangjie.psi.CjParameter

    private companion object {
        val ARRAY_CLASS_ID = ClassId(StandardNames.FqNames.core, StandardNames.ARRAY)
    }
}
