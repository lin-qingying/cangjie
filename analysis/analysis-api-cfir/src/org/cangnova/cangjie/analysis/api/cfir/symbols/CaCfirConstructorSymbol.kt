package org.cangnova.cangjie.analysis.api.cfir.symbols

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.findPsi
import org.cangnova.cangjie.analysis.api.cfir.getExplicitCallableReceiverType
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaModifierExplicitness
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolUnavailableException
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolUnavailabilityReason
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjConstructor
import org.cangnova.cangjie.psi.CjPrimaryConstructor
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType

/**
 * 构造器叶子实现。
 *
 * 对齐 Kotlin `KaFirConstructorSymbol` 的独立叶子落位：
 * 构造器不复用普通命名函数叶子，而是直接在类内表达自身的参数、返回类型、
 * 所属类与 pointer 语义。
 */
internal class CaCfirConstructorSymbol private constructor(
    /**
     * 构造器对应的源码 PSI。
     */
    override val backingPsi: CjConstructor<*>?,
    /**
     * 当前符号绑定的 CFIR Analysis session。
     */
    override val analysisSession: CaCfirSession,
    /**
     * 延迟取得的底层 CFIR 构造器符号。
     */
    override val lazyCfirSymbol: Lazy<CfirConstructorSymbol>,
) : CaConstructorSymbol(),
    CaCfirCjBasedSymbol<CjConstructor<*>, CfirConstructorSymbol> {
    /**
     * 构造器底层 CFIR 符号。
     */
    override val cfirSymbol: CfirConstructorSymbol
        get() = super<CaCfirCjBasedSymbol>.cfirSymbol

    constructor(declaration: CjConstructor<*>, session: CaCfirSession) : this(
        backingPsi = declaration,
        analysisSession = session,
        lazyCfirSymbol = lazyCfirSymbol(declaration, session),
    )

    constructor(symbol: CfirConstructorSymbol, session: CaCfirSession) : this(
        backingPsi = symbol.backingPsiIfApplicable as? CjConstructor<*>,
        analysisSession = session,
        lazyCfirSymbol = lazyOf(symbol),
    )

    /**
     * 构造器所在的 use-site 模块。
     */
    override val containingModule: CaModule
        get() = analysisSession.useSiteModule

    /**
     * 构造器 CFIR member 状态。
     */
    private val status
        get() = (cfirSymbolOrNull?.cfir as? CfirMemberDeclaration)?.status

    /**
     * 构造器公开注解列表。
     */
    override val annotations: CaAnnotationList
        get() = withValidityAssertion { psiOrSymbolAnnotationList() }

    /**
     * 构造器对应的 PSI。
     */
    override val psi: PsiElement?
        get() = withValidityAssertion { backingPsiOrFindCurrentPsi { findPsi() } }

    /**
     * 构造器公开来源。
     */
    override val origin
        get() = withValidityAssertion { psiOrSymbolOrigin() }

    /**
     * 构造器 callableId。
     */
    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = withValidityAssertion {
            val symbol = cfirSymbolOrNull ?: return@withValidityAssertion null
            val callableDeclaration = symbol.cfir as? CfirCallableDeclaration
            symbol.callableId.takeUnless { callableDeclaration?.isLocal == true }
        }

    /**
     * 构造器可见性。
     */
    override val visibility: CaSymbolVisibility
        get() = withValidityAssertion { status?.visibility?.asPublicVisibility() ?: CaSymbolVisibility.PUBLIC }

    /**
     * 构造器可见性是否显式声明。
     */
    override val isVisibilityExplicit: Boolean
        get() = visibilityExplicitness == CaModifierExplicitness.EXPLICIT

    override val visibilityExplicitness: CaModifierExplicitness
        get() = withValidityAssertion { status.asPublicVisibilityExplicitness() }

    /**
     * 构造器不显式暴露 modality。
     */
    override val isModalityExplicit: Boolean
        get() = withValidityAssertion { false }

    override val modalityExplicitness: CaModifierExplicitness
        get() = withValidityAssertion { CaModifierExplicitness.UNKNOWN }

    /**
     * 构造器显式 receiver 类型。
     */
    override val receiverType: CaType?
        get() = withValidityAssertion { analysisSession.getExplicitCallableReceiverType(backingPsi, builder) { cfirSymbolOrNull } }

    /**
     * 构造器返回类型。
     */
    override val returnType: CaType
        get() = withValidityAssertion {
            // 缺失时返回错误类型而不是抛异常：调用方（补全尾文本、签名渲染、类型检查）
            // 据 isErrorType 判定「这个构造器的返回类型拿不到」，不必靠捕获异常探测。
            cfirSymbolOrNull?.returnType(builder)
                ?: builder.typeBuilder.buildType(ConeErrorType(ConeSimpleDiagnostic("constructor return type is unavailable")))
        }

    /**
     * 构造器公开符号位置固定为 class。
     */
    override val location: CaSymbolLocation
        get() = withValidityAssertion { CaSymbolLocation.CLASS }

    /**
     * 创建构造器符号 pointer。
     */
    override fun createPointer(): CaSymbolPointer<CaConstructorSymbol> = withValidityAssertion {
        psiBasedSymbolPointerOfTypeIfSource<CaConstructorSymbol> { psi ->
            (psi as? CjConstructor<*>)?.symbol
        } ?: error("Constructor symbol cannot create a stable pointer")
    }

    /**
     * 构造器是否为 static。
     */
    override val isStatic: Boolean
        get() = withValidityAssertion { status?.isStatic == true }

    /**
     * 构造器是否为 const。
     */
    override val isConst: Boolean
        get() = withValidityAssertion { status?.isConst == true }

    /**
     * 构造器是否为 mutating。
     */
    override val isMutating: Boolean
        get() = withValidityAssertion { status?.isMut == true }

    /**
     * 构造器是否为 override。
     */
    override val isOverride: Boolean
        get() = withValidityAssertion { status?.isOverride == true }

    /**
     * 构造器是否为 operator。
     */
    override val isOperator: Boolean
        get() = withValidityAssertion { status?.isOperator == true }

    /**
     * 构造器是否为 unsafe。
     */
    override val isUnsafe: Boolean
        get() = withValidityAssertion { status?.isUnsafe == true }

    /**
     * 构造器是否为 foreign。
     */
    override val isForeign: Boolean
        get() = withValidityAssertion { status?.isForeign == true }

    /**
     * 构造器所属类型的类型参数列表。
     *
     * 与 [valueParameters] 同一策略：**未知必须与「空的」区分**。返回空列表是「确定没有类型参数」
     * 的正向断言，而下层 `CfirCallResolver` 会拿它做「类型实参个数 = 类型参数个数」的前置判定，
     * 冒出一个假的空列表会让显式类型实参映射被静默丢弃。
     *
     * 语义口径：官方构造器本身不能声明类型参数
     * （`external/cangjie_compiler/src/Sema/TypeChecker.cpp:1330-1341` 的
     * `sema_forbid_generic_constructor`），调用点的类型实参来自外层类
     * （`TypeCheckUtil.cpp:868-876` 的 `GetCurrentGeneric`），因此公开 API 这里暴露的是
     * 「所属类型的类型参数」；取不到时抛类型化异常，由补全按候选粒度丢弃。
     */
    override val typeParameters: List<CaTypeParameterSymbol>
        get() = withValidityAssertion {
            with(analysisSession) {
                backingPsi?.getContainingTypeStatement()?.classSymbol?.typeParameters
                    ?: ownerClassTypeParameters()
            }
        }

    /**
     * CFIR 分支：返回**外层类型**的类型参数。
     *
     * 两条口径必须统一到「外层类」这一侧：`cfirSymbol.typeParameters` 是构造器**自身**声明的
     * 类型参数，而官方构造器根本不能声明类型参数
     * （`external/cangjie_compiler/src/Sema/TypeChecker.cpp:1330-1341` 的
     * `sema_forbid_generic_constructor`），psi2cfir 也只转换构造器自身列表
     * （`PsiRawCfirBuilder.kt` 的 `typeParametersForConstructor`），因此那个列表恒为空——
     * 用它会让没有 PSI 的库构造器在泛型类上少报类型参数，与源码路径给出不同答案。
     *
     * 外层类符号构建不出来时抛可分类异常（与 [valueParameters] 同策略），而不是退回空列表。
     */
    private fun ownerClassTypeParameters(): List<CaTypeParameterSymbol> {
        val classId = cfirSymbol.callableId.classId
            ?: throw CaSymbolUnavailableException(
                reason = CaSymbolUnavailabilityReason.MISSING_DECLARATION,
                message = "Constructor owner class is unavailable",
            )
        val ownerCfirSymbol = analysisSession.cfirSession.symbolProvider.getClassLikeSymbolByClassId(classId)
            ?: throw CaSymbolUnavailableException(
                reason = CaSymbolUnavailabilityReason.MISSING_DECLARATION,
                message = "Constructor owner class `$classId` is not in the symbol provider",
            )
        val ownerSymbol = builder.buildSymbol(ownerCfirSymbol)
        return (ownerSymbol as? CaTypeParameterOwnerSymbol)?.typeParameters
            ?: throw CaSymbolUnavailableException(
                reason = CaSymbolUnavailabilityReason.MISSING_DECLARATION,
                message = "Type parameters of the constructor owner `$classId` are unavailable",
            )
    }

    /**
     * 构造器值参数列表。
     *
     * 这里**刻意保留**「不可用即抛类型化异常」的形态，而不是降级成空列表：
     * 空列表与「真的没有形参」不可区分，补全会把它渲染成 `Foo()`——一个看起来正常
     * 但形态错误的调用模板。抛出的
     * [org.cangnova.cangjie.analysis.api.symbols.CaSymbolUnavailableException] 可被补全层
     * 按候选粒度捕获并丢弃，其余候选不受影响（HANDOVER T5 / 设计决策 7）。
     */
    override val valueParameters: List<CaValueParameterSymbol>
        get() = withValidityAssertion {
            createCaValueParameters() ?: cfirSymbol.createCjValueParameters(builder)
        }

    /**
     * 构造器是否为主构造器。
     */
    override val isPrimary: Boolean
        get() = withValidityAssertion { backingPsi is CjPrimaryConstructor || cfirSymbolOrNull?.cfir?.isPrimary == true }

    /**
     * 构造器所属类的 classId。
     */
    override val containingClassId: ClassId?
        get() = withValidityAssertion {
            backingPsi?.getContainingTypeStatement()?.getClassId()
                ?: cfirSymbolOrNull?.callableId?.classId
        }
}
