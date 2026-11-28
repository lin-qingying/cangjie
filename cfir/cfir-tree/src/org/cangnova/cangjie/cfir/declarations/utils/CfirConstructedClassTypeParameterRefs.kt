package org.cangnova.cangjie.cfir.declarations.utils

import org.cangnova.cangjie.cfir.declarations.CfirConstructedClassTypeParameterRef
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameterRef
import org.cangnova.cangjie.cfir.declarations.builder.buildConstructedClassTypeParameterRef
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.fakeElement

/**
 * 对位 Kotlin `constructorTypeParametersFromConstructedClass`：实例构造器与 enum 构造项
 * 按 header 顺序引用所属 class/struct/enum 的类型参数 symbol，不创建、不绑定新的参数声明。
 *
 * 仓颉构造器不能声明自有泛型（cjc `sema_forbid_generic_constructor`），static init 不借用实例类型参数。
 * 参数 symbol 必须已经绑定到所属名义类型的 header 声明。
 */
fun constructedClassTypeParameterRefs(
    ownerTypeParameterSymbols: List<CfirTypeParameterSymbol>,
): List<CfirConstructedClassTypeParameterRef> = ownerTypeParameterSymbols.map { parameterSymbol ->
    buildConstructedClassTypeParameterRef {
        source = parameterSymbol.cfir.source?.fakeElement(CjFakeSourceElementKind.ConstructorTypeParameter)
        symbol = parameterSymbol
    }
}

/** 声明自身拥有的类型参数；排除构造器借用的 constructed-class 引用。 */
val List<CfirTypeParameterRef>.ownTypeParameters: List<CfirTypeParameter>
    get() = filterIsInstance<CfirTypeParameter>()
