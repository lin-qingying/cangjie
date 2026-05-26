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
    override val backingPsi: CjExtend?,
    override val analysisSession: CaCfirSession,
    override val lazyCfirSymbol: Lazy<CfirExtendSymbol>,
    private val explicitIdentity: CaCfirExtendSymbolIdentity?,
    private val explicitExtendId: String?,
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

    override val cfirSymbol: CfirExtendSymbol
        get() = super<CaCfirCjBasedSymbol>.cfirSymbol

    private val resolvedIdentity: CaCfirResolvedExtendIdentity
        get() = analysisSession.resolveExtendIdentity(cfirSymbol)

    internal val stableIdentity: CaCfirExtendSymbolIdentity
        get() = explicitIdentity ?: resolvedIdentity.stableIdentity

    private val stableExtendId: String
        get() = explicitExtendId ?: resolvedIdentity.extendId

    internal val extendPackageFqName: FqName
        get() = explicitPackageFqName ?: resolvedIdentity.packageFqName

    private val extendDeclaration: CfirExtend
        get() = cfirSymbol.cfir

    private val status
        get() = (extendDeclaration as? CfirMemberDeclaration)?.status

    override val containingModule
        get() = analysisSession.useSiteModule

    override val annotations: CaAnnotationList
        get() = withValidityAssertion {
            psiOrSymbolAnnotationList()
        }

    override val psi
        get() = withValidityAssertion { backingPsiOrFindCurrentPsi { findPsi() } }

    override val origin: CaSymbolOrigin
        get() = withValidityAssertion { psiOrSymbolOrigin() }

    override val extendId: String
        get() = stableExtendId

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

    override val extendedType: CaType
        get() = extendDeclaration.extendedTypeRef.coneTypeOrNull?.let(builder.typeBuilder::buildType)
            ?: error("Cannot build extended type for extend `${extendId}`")

    override val superTypes: List<CaType>
        get() = extendDeclaration.superTypeRefs.mapNotNull { superTypeRef -> superTypeRef.coneTypeOrNull?.let(builder.typeBuilder::buildType) }

    override val typeParameters: List<CaTypeParameterSymbol>
        get() = extendDeclaration.typeParameters.map { typeParameter -> builder.classifierBuilder.buildTypeParameterSymbol(typeParameter.symbol) }

    override val visibility: CaSymbolVisibility
        get() = status?.visibility?.asPublicVisibility() ?: CaSymbolVisibility.PUBLIC

    override val isVisibilityExplicit: Boolean
        get() = status?.isVisibilityExplicit == true

    override val modality: CaSymbolModality?
        get() = status?.modality?.asPublicModality()

    override val isModalityExplicit: Boolean
        get() = status?.isModalityExplicit == true

    override val location: CaSymbolLocation
        get() = CaSymbolLocation.TOP_LEVEL

    override fun createPointer(): CaSymbolPointer<CaAnnotatedSymbol> = withValidityAssertion {
        CaCfirExtendSymbolPointer(CaCfirExtendSymbolCacheKey(stableIdentity))
    }

    override fun equals(other: Any?): Boolean = psiOrSymbolEquals(other)
    override fun hashCode(): Int = psiOrSymbolHashCode()
}
