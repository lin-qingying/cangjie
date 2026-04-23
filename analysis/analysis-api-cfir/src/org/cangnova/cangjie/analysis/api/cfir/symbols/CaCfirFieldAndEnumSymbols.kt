package org.cangnova.cangjie.analysis.api.cfir.symbols

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFieldSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolLocation
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.payloadParameterTypesOrEmpty
import org.cangnova.cangjie.cfir.session.cfirProvider
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirEnumConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.CfirFieldVariableSymbol
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name

/**
 * 字段与枚举构造器叶子实现。
 *
 * 这两类都属于 variable/callable 分支，但公开语义与 property、value parameter 完全不同，
 * 独立落位后更容易维持稳定的 pointer 与宿主恢复规则。
 */
internal class CaCfirFieldSymbol(
    final override val backingSymbol: CfirFieldVariableSymbol,
    final override val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : CaFieldSymbol(), CaCfirVariableSymbolSupport<CfirFieldVariableSymbol> {
    override val annotations: CaAnnotationList
        get() = withValidityAssertion { CaCfirAnnotationListForDeclaration.create(backingSymbol, builder) }

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = callableIdImpl

    override val receiverType: CaType?
        get() = receiverTypeImpl

    override val returnType: CaType
        get() = returnTypeImpl

    override val location: CaSymbolLocation
        get() = locationImpl

    override fun createPointer(): CaSymbolPointer<CaCallableSymbol> = withValidityAssertion {
        createStableCallablePointer(CaCallableSymbol::class.java)
    }

    override val isLet: Boolean
        get() = !(backingSymbol.cfir as CfirFieldVariable).isVar

    override val isStatic: Boolean
        get() = status?.isStatic == true

    override val isConst: Boolean
        get() = status?.isConst == true

    override val name: Name
        get() = nameImpl
}

internal class CaCfirEnumConstructorSymbol(
    final override val backingSymbol: CfirEnumConstructorSymbol,
    final override val analysisSession: CaCfirSession,
    final override val containingModule: CaModule,
    final override val token: CaLifetimeToken,
) : CaEnumConstructorSymbol(),
    CaCfirCallableSymbolSupport<CfirEnumConstructorSymbol>,
    CaNamedSymbol {
    override val annotations: CaAnnotationList
        get() = withValidityAssertion { CaCfirAnnotationListForDeclaration.create(backingSymbol, builder) }

    override val callableId: org.cangnova.cangjie.name.CallableId?
        get() = callableIdImpl

    override val receiverType: CaType?
        get() = receiverTypeImpl

    override val returnType: CaType
        get() = returnTypeImpl

    override val location: CaSymbolLocation
        get() = locationImpl

    override fun createPointer(): CaSymbolPointer<CaCallableSymbol> = withValidityAssertion {
        createStableCallablePointer(CaCallableSymbol::class.java)
    }

    override val name: Name
        get() = backingSymbol.name

    override val containingEnumClassId: ClassId?
        get() = analysisSession.cfirSession.cfirProvider.getContainingClass(backingSymbol)?.classId

    override val payloadTypes: List<CaType>
        get() = backingSymbol.cfir.payloadParameterTypesOrEmpty().map(builder.typeBuilder::buildType)
}
