package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.containingClassForStaticMemberAttr
import org.cangnova.cangjie.cfir.declarations.*
import org.cangnova.cangjie.cfir.builder.buildImplicitDefaultConstructor
import org.cangnova.cangjie.cfir.declarations.utils.constructedClassTypeParameterRefs
import org.cangnova.cangjie.cfir.symbols.CfirConstructorSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagWithFixedSymbol
import org.cangnova.cangjie.cfir.types.ConeSimpleCangJieType
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.SpecialNames
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.fakeElement

/** 原生 AddDefaultCtor 的声明资格；COMMON 与 mirror 不生成新的实例默认构造器。 */
internal fun CfirClassLikeDeclaration.canGenerateDefaultInstanceConstructor(): Boolean =
    (this is CfirClass || this is CfirStruct) && origin.fromSource && !status.isCommon &&
        interopInfo?.java?.isMirror != true && interopInfo?.objc?.isMirror != true &&
        interopInfo?.objc?.isSyntheticWrapper != true

/**
 * normal/post 共用真实的 generated init skeleton。独立 Public/FINAL 状态不继承 class 修饰符；
 * 空 Block 是生成阶段的真实实现体，父调用与 CFG 由正常 BODY_RESOLVE 继续完成。
 */
internal fun CfirClassLikeDeclaration.createDefaultInstanceConstructor(): CfirConstructor {
    require(canGenerateDefaultInstanceConstructor())
    val owner = this
    val generatedSource = source?.fakeElement(CjFakeSourceElementKind.ImplicitConstructor)
    val ownerTag = ConeClassLikeLookupTagWithFixedSymbol(symbol.classId, symbol)
    return buildImplicitDefaultConstructor(
        CfirConstructorSymbol(CallableId(owner.symbol.classId, SpecialNames.INIT)), generatedSource, owner.moduleData,
        false, owner.constructorOwnerType() as ConeSimpleCangJieType,
        constructedClassTypeParameterRefs(owner.typeParameters.map { it.symbol }),
    ).also { it.containingClassForStaticMemberAttr = ownerTag }
}
