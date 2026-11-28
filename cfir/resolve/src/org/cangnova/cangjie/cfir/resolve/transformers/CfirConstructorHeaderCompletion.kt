package org.cangnova.cangjie.cfir.resolve.transformers

import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.cjoDeclarationOrigin
import org.cangnova.cangjie.cfir.resolvedTypeFromPrototype
import org.cangnova.cangjie.cfir.types.CfirTypeRefOrigin
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.declarationSelfType

/** 复用实际 owner symbol 自有的类型身份；Source/CJO 不另造第二种 constructor tag。 */
internal fun CfirClassLikeDeclaration.constructorOwnerType(): ConeCangJieType =
    checkNotNull(declarationSelfType(symbol))

/** 正常 TYPES 与后置生成构造器共用 owner 返回引用生产；完成类型后才可发布阶段。 */
internal fun CfirConstructor.completeConstructorReturnType(ownerType: ConeCangJieType) {
    replaceReturnTypeRef(returnTypeRef.resolvedTypeFromPrototype(ownerType, returnTypeRef.source).also { ref ->
        if (cjoDeclarationOrigin == null && (origin.fromSource || origin == CfirDeclarationOrigin.ImplicitDefault ||
                origin == CfirDeclarationOrigin.Synthetic.ImplicitConstructor))
            ref.replaceTypeUseOrigin(CfirTypeRefOrigin.COMPILER_ADDED)
    })
}
