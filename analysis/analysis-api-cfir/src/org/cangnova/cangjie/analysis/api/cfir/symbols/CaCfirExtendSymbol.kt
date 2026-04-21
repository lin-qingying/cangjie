package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.symbols.pointers.CaCfirExtendSymbolPointer
import org.cangnova.cangjie.analysis.api.cfir.utils.asCaType
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaAnnotatedSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaExtendSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaTypeParameterOwnerSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.symbols.CfirExtendSymbol
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.coneTypeOrNull
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName

/**
 * extend 叶子实现。
 *
 * 这是仓颉特有公开语义，保留该差异，但组织方式改为 Kotlin 风格的单叶子单文件。
 */
internal class CaCfirExtendSymbol(
    backingSymbol: CfirExtendSymbol,
    internal val extendPsi: org.cangnova.cangjie.psi.CjExtend?,
    internal val stableIdentity: CaCfirExtendSymbolIdentity,
    private val stableExtendId: String,
    internal val extendPackageFqName: FqName,
    analysisSession: CaCfirSession,
    containingModule: CaModule,
    token: CaLifetimeToken,
) : CaCfirDeclarationBackedSymbol<CfirExtendSymbol>(backingSymbol, analysisSession, containingModule, token),
    CaExtendSymbol,
    CaTypeParameterOwnerSymbol {
    private val extendDeclaration: CfirExtend
        get() = backingSymbol.cfir

    override val extendId: String
        get() = stableExtendId

    override val targetClassId: ClassId?
        get() = extendDeclaration.extendedTypeRef.coneTypeOrNull?.classIdOrPrimitiveClassId

    override val extendedType: CaType
        get() = extendDeclaration.extendedTypeRef.coneTypeOrNull?.asCaType(analysisSession)
            ?: error("Cannot build extended type for extend `${extendId}`")

    override val superTypes: List<CaType>
        get() = extendDeclaration.superTypeRefs.mapNotNull { superTypeRef -> superTypeRef.coneTypeOrNull?.asCaType(analysisSession) }

    override val typeParameters: List<CaTypeParameterSymbol>
        get() = extendDeclaration.typeParameters.map { typeParameter -> analysisSession.createTypeParameterSymbol(typeParameter.symbol) }

    override val location: CaSymbolLocation
        get() = CaSymbolLocation.TOP_LEVEL

    override val containingDeclaration: CaSymbol?
        get() = analysisSession.findContainingDeclarationSymbol(extendPsi ?: psi)

    override fun createPointer(): CaSymbolPointer<CaAnnotatedSymbol> = withValidityAssertion {
        CaCfirExtendSymbolPointer(CaCfirExtendSymbolCacheKey(stableIdentity))
    }
}
