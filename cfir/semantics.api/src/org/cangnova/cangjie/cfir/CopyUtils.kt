package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.types.CfirImplicitTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirUserTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.withReplacedSourceAndType
import org.cangnova.cangjie.source.CjSourceElement

/**
 * 基于现有 type ref 原型构造 resolved type ref。
 *
 * 若当前 type ref 已经 resolved，则保留原始 resolved 节点结构并替换 source/type；
 * 否则使用 [fallbackSource] 和可能存在的 user type ref 创建新的 resolved type ref。
 */
fun CfirTypeRef.resolvedTypeFromPrototype(
    type: ConeCangJieType,
    fallbackSource: CjSourceElement?,
): CfirResolvedTypeRef {
    if (this is CfirResolvedTypeRef) {
        return withReplacedSourceAndType(source ?: fallbackSource, type)
    }
    return type.toCfirResolvedTypeRef(source ?: fallbackSource, this)
}

/**
 * 判断函数返回类型是否仍是隐式类型，或已由函数体推断得到。
 *
 * 完成BODY时生成的返回Type节点保留其SEMANTIC原型，原始省略与当前COMPILER_ADD节点
 * 是不同事实。显式SOURCE原型及CJO包装不因生成来源被重新当成省略返回。
 */
fun CfirFunction.hasImplicitOrInferredReturnType(): Boolean =
    returnTypeRef is CfirImplicitTypeRef ||
            this is CfirNamedFunction && (returnTypeRef.typeUseOrigin == org.cangnova.cangjie.cfir.types.CfirTypeRefOrigin.SEMANTIC ||
                origin.fromSource && returnTypeRef.typeUseOrigin == org.cangnova.cangjie.cfir.types.CfirTypeRefOrigin.COMPILER_ADDED &&
                (returnTypeRef as? CfirResolvedTypeRef)?.delegatedTypeRef?.typeUseOrigin == org.cangnova.cangjie.cfir.types.CfirTypeRefOrigin.SEMANTIC)
