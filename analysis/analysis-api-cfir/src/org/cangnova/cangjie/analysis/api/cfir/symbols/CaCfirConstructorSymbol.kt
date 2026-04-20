package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.cfir.*

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.cfir.components.asCaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.components.renderAnnotations
import org.cangnova.cangjie.analysis.api.cfir.utils.asCaType
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolOrigin
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.analysis.api.symbols.CaTypeParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaValueParameterSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirMemberDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjConstructor
import org.cangnova.cangjie.psi.CjPrimaryConstructor
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType

/**
 * 构造器叶子实现。
 *
 * 构造器的 `modality` 与普通函数不同，在公开 Analysis API 中是固定语义，
 * 因此单独落位，避免错误复用通用函数 support。
 */
internal class CaCfirConstructorSymbolImpl(
    final override val backingSymbol: CfirConstructorSymbol,
    val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : CaConstructorSymbol(),
    CaCfirBackedSymbol<CfirConstructorSymbol>,
    CaCfirSymbolMixin {
    private val constructorStatus: CfirDeclarationStatus?
        get() = (backingSymbol.cfir as? CfirMemberDeclaration)?.status

    override val annotations: CaAnnotationList
        get() = withValidityAssertion {
            analysisSession.renderAnnotations(this).asCaAnnotationList(token)
        }

    override val psi: PsiElement?
        get() = analysisSession.symbolQueries.lookupSourcePsi(backingSymbol)

    override val origin: CaSymbolOrigin
        get() = backingSymbol.origin.asPublicOrigin()

    override val containingDeclaration: CaSymbol?
        get() = analysisSession.findContainingDeclarationSymbol(psi)

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() {
            val callableDeclaration = backingSymbol.cfir as? CfirCallableDeclaration
            return backingSymbol.callableId.takeUnless { callableDeclaration?.isLocal == true }
        }

    override val receiverType: CaType?
        get() {
            val callablePsi = psi as? CjCallableDeclaration ?: return null
            if (callablePsi.getStrictParentOfType<org.cangnova.cangjie.psi.CjExtend>() == null) return null
            return (backingSymbol.cfir as? CfirCallableDeclaration)?.dispatchReceiverType?.asCaType(analysisSession)
        }

    override val returnType: CaType
        get() = analysisSession.typeQueries.queryCallableReturnType(backingSymbol)?.asCaType(analysisSession)
            ?: error("Cannot build return type for `${backingSymbol::class.simpleName}`")

    override val visibility: CaSymbolVisibility
        get() = constructorStatus?.visibility?.asPublicVisibility() ?: CaSymbolVisibility.PUBLIC

    override val isVisibilityExplicit: Boolean
        get() = constructorStatus?.isVisibilityExplicit == true

    override val isModalityExplicit: Boolean
        get() = false

    override val location: CaSymbolLocation
        get() = analysisSession.locationForDeclaration(this)

    override fun createPointer(): CaSymbolPointer<CaConstructorSymbol> = withValidityAssertion {
        createStableCallablePointer(CaConstructorSymbol::class.java)
    }

    override val isStatic: Boolean
        get() = constructorStatus?.isStatic == true

    override val isConst: Boolean
        get() = constructorStatus?.isConst == true

    override val isMutating: Boolean
        get() = constructorStatus?.isMut == true

    override val isOverride: Boolean
        get() = constructorStatus?.isOverride == true

    override val isOperator: Boolean
        get() = constructorStatus?.isOperator == true

    override val isUnsafe: Boolean
        get() = constructorStatus?.isUnsafe == true

    override val isForeign: Boolean
        get() = constructorStatus?.isForeign == true

    override val typeParameters: List<CaTypeParameterSymbol>
        get() = (backingSymbol.cfir as? CfirCallableDeclaration)
            ?.typeParameters
            ?.filterIsInstance<CfirTypeParameter>()
            ?.map { typeParameter -> analysisSession.createTypeParameterSymbol(typeParameter.symbol) }
            .orEmpty()

    override val valueParameters: List<CaValueParameterSymbol>
        get() = (backingSymbol.cfir as? CfirFunction)
            ?.valueParameters
            ?.mapIndexed { index, parameter ->
                analysisSession.createValueParameterSymbol(
                    ownerSymbol = this,
                    parameter = parameter,
                    parameterIndex = index,
                )
            }
            .orEmpty()

    override val isPrimary: Boolean
        get() = psi is CjPrimaryConstructor

    override val containingClassId: ClassId?
        get() = (psi as? CjConstructor<*>)?.getContainingTypeStatement()?.getClassId()
}
