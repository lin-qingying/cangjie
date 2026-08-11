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
    return type.toCfirResolvedTypeRef(source ?: fallbackSource, this as? CfirUserTypeRef)
}

/**
 * 判断函数返回类型是否仍是隐式类型，或已由函数体推断得到。
 *
 * 无显式返回类型的具名函数会在第一次 body resolve 后把 implicit type ref 替换成
 * source 为 null 的 resolved type ref。该类型是推断结果而不是新的返回类型标注，
 * 后续 body resolve 和 return 检查不能再把它当作目标类型施加约束。
 */
fun CfirFunction.hasImplicitOrInferredReturnType(): Boolean =
    returnTypeRef is CfirImplicitTypeRef ||
            this is CfirNamedFunction && returnTypeRef.source == null
