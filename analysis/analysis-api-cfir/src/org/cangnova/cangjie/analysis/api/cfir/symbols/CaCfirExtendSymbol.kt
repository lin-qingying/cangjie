package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.findPsi
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirExtendSymbolPointer
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.symbols.CaAnnotatedSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolOrigin
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.symbols.CfirExtendSymbol
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjExtend

/**
 * extend 叶子实现。
 *
 * 这是仓颉特有公开语义，保留该差异，但组织方式改为 Kotlin 风格的单叶子单文件。
 */
internal class CaCfirExtendSymbol private constructor(
    /**
     * extend 声明对应的源码 PSI。
     */
    override val backingPsi: CjExtend?,
    /**
     * 当前符号绑定的 CFIR Analysis session。
     */
    override val analysisSession: CaCfirSession,
    /**
     * 延迟取得的底层 CFIR extend 符号。
     */
    override val lazyCfirSymbol: Lazy<CfirExtendSymbol>,
    /**
     * 预解析并注入的稳定 extend 身份。
     */
    private val explicitIdentity: CaCfirExtendSymbolIdentity?,
    /**
     * 预解析并注入的公开 extendId。
     */
    private val explicitExtendId: String?,
    /**
     * 预解析并注入的 extend 所在包名。
     */
    private val explicitPackageFqName: FqName?,
) : CaCfirCjBasedSymbol<CjExtend, CfirExtendSymbol>,
    CaExtendSymbol,
    CaTypeParameterOwnerSymbol {
    constructor(declaration: CjExtend, session: CaCfirSession) : this(
        backingPsi = declaration,
        analysisSession = session,
        lazyCfirSymbol = lazyCfirSymbol(declaration, session),
        explicitIdentity = null,
        explicitExtendId = null,
        explicitPackageFqName = null,
    )

    constructor(
        backingSymbol: CfirExtendSymbol,
        extendPsi: CjExtend?,
        stableIdentity: CaCfirExtendSymbolIdentity,
        stableExtendId: String,
        extendPackageFqName: FqName,
        analysisSession: CaCfirSession,
    ) : this(
        backingPsi = extendPsi ?: backingSymbol.backingPsiIfApplicable as? CjExtend,
        analysisSession = analysisSession,
        lazyCfirSymbol = lazyOf(backingSymbol),
        explicitIdentity = stableIdentity,
        explicitExtendId = stableExtendId,
        explicitPackageFqName = extendPackageFqName,
    )

    /**
     * extend 底层 CFIR 符号。
     */
    override val cfirSymbol: CfirExtendSymbol
        get() = super<CaCfirCjBasedSymbol>.cfirSymbol

    /**
     * 当前 session 下按语义模型解析出的 extend 身份。
     */
    private val resolvedIdentity: CaCfirResolvedExtendIdentity
        get() = analysisSession.resolveExtendIdentity(cfirSymbol)

    /**
     * 用于缓存和 pointer 恢复的稳定 extend 身份。
     */
    internal val stableIdentity: CaCfirExtendSymbolIdentity
        get() = explicitIdentity ?: resolvedIdentity.stableIdentity

    /**
     * 公开 API 暴露的稳定 extendId 文本。
     */
    private val stableExtendId: String
        get() = explicitExtendId ?: resolvedIdentity.extendId

    /**
     * extend 所在包名。
     */
    internal val extendPackageFqName: FqName
        get() = explicitPackageFqName ?: resolvedIdentity.packageFqName

    /**
     * extend 底层 CFIR 声明。
     */
    private val extendDeclaration: CfirExtend
        get() = cfirSymbol.cfir

    /**
     * extend CFIR member 状态。
     */
    private val status
        get() = (extendDeclaration as? CfirMemberDeclaration)?.status

    /**
     * extend 所在的 use-site 模块。
     */
    override val containingModule
        get() = analysisSession.useSiteModule

    /**
     * extend 公开注解列表。
     */
    override val annotations: CaAnnotationList
        get() = withValidityAssertion {
            psiOrSymbolAnnotationList()
        }

    /**
     * extend 对应的 PSI。
     */
    override val psi
        get() = withValidityAssertion { backingPsiOrFindCurrentPsi { findPsi() } }

    /**
     * extend 公开来源。
     */
    override val origin: CaSymbolOrigin
        get() = withValidityAssertion { psiOrSymbolOrigin() }

    /**
     * 公开 API 暴露的 extendId。
     */
    override val extendId: String
        get() = stableExtendId

    /**
     * 被扩展目标类型的 classId。
     */
    override val targetClassId: ClassId?
        get() = withValidityAssertion {
            /**
             * `extend` 的可寻址目标类身份已经在统一的 stable identity 中固化。
             *
             * 公开 symbol 也必须复用这一路径，避免 source PSI 入口与
             * symbol-provider / pointer-restore 入口对同一 extend 给出不同 targetClassId。
             */
            stableIdentity.targetClassId
        }

    /**
     * extend 的被扩展目标类型。
     */
    override val extendedType: CaType
        get() = extendDeclaration.extendedTypeRef.coneTypeOrNull?.let(builder.typeBuilder::buildType)
            ?: error("Cannot build extended type for extend `${extendId}`")

    /**
     * extend 显式声明的父接口或父类型列表。
     */
    override val superTypes: List<CaType>
        get() = extendDeclaration.superTypeRefs.mapNotNull { superTypeRef -> superTypeRef.coneTypeOrNull?.let(builder.typeBuilder::buildType) }

    /**
     * extend 类型参数列表。
     */
    override val typeParameters: List<CaTypeParameterSymbol>
        get() = extendDeclaration.typeParameters.map { typeParameter -> builder.classifierBuilder.buildTypeParameterSymbol(typeParameter.symbol) }

    /**
     * extend 可见性。
     */
    override val visibility: CaSymbolVisibility
        get() = status?.visibility?.asPublicVisibility() ?: CaSymbolVisibility.PUBLIC

    /**
     * extend 可见性是否显式声明。
     */
    override val isVisibilityExplicit: Boolean
        get() = status?.isVisibilityExplicit == true

    /**
     * extend modality。
     */
    override val modality: CaSymbolModality?
        get() = status?.modality?.asPublicModality()

    /**
     * extend modality 是否显式声明。
     */
    override val isModalityExplicit: Boolean
        get() = status?.isModalityExplicit == true

    /**
     * extend 在公开 API 中位于顶层。
     */
    override val location: CaSymbolLocation
        get() = CaSymbolLocation.TOP_LEVEL

    /**
     * 创建可按稳定 extend identity 恢复的 pointer。
     */
    override fun createPointer(): CaSymbolPointer<CaAnnotatedSymbol> = withValidityAssertion {
        CaCfirExtendSymbolPointer(CaCfirExtendSymbolCacheKey(stableIdentity))
    }

    /**
     * 按 PSI 或 CFIR 符号身份比较 extend。
     */
    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    /**
     * 按 PSI 或 CFIR 符号身份计算 extend hash。
     */
    override fun hashCode(): Int = psiOrSymbolHashCode()
}
