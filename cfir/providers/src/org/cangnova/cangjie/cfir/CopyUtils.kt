package org.cangnova.cangjie.cfir

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
