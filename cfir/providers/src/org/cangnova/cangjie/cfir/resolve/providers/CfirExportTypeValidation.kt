package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.types.ConeCangJieType

/** 验证冻结类型的有序槽采用节点身份；结构相等的替换也属于输入换代。 */
internal fun CfirExportTypePayload.hasSameSourceFields(current: CfirExportTypePayload): Boolean = when (this) {
    CfirExportTypePayload.Invalid -> current === CfirExportTypePayload.Invalid
    is CfirExportTypePayload.Primitive -> current is CfirExportTypePayload.Primitive && kind == current.kind
    is CfirExportTypePayload.Nominal -> current is CfirExportTypePayload.Nominal &&
        kind == current.kind && classId == current.classId && declaration === current.declaration && sameTypes(arguments, current.arguments) &&
        isThisType == current.isThisType && isRefEnum == current.isRefEnum
    is CfirExportTypePayload.Alias -> current is CfirExportTypePayload.Alias &&
        declaration === current.declaration && sameTypes(arguments, current.arguments) && expandedType === current.expandedType
    is CfirExportTypePayload.Generic -> current is CfirExportTypePayload.Generic &&
        parameter === current.parameter && sameTypes(bounds, current.bounds) && sameTypes(explicitBounds, current.explicitBounds)
    is CfirExportTypePayload.Function -> current is CfirExportTypePayload.Function &&
        sameTypes(parameters, current.parameters) && returnType === current.returnType &&
        isC == current.isC && isClosure == current.isClosure && hasVariableLenArg == current.hasVariableLenArg
    is CfirExportTypePayload.Tuple -> current is CfirExportTypePayload.Tuple && sameTypes(elements, current.elements)
    is CfirExportTypePayload.RawArray -> current is CfirExportTypePayload.RawArray &&
        declaration === current.declaration && sameTypes(arguments, current.arguments) &&
        layout.element === current.layout.element && layout.dimensions == current.layout.dimensions
    is CfirExportTypePayload.VArray -> current is CfirExportTypePayload.VArray && element === current.element && size == current.size
    is CfirExportTypePayload.Pointer -> current is CfirExportTypePayload.Pointer && pointee === current.pointee
    CfirExportTypePayload.CString -> current === CfirExportTypePayload.CString
    is CfirExportTypePayload.Unrepresentable -> current is CfirExportTypePayload.Unrepresentable && identity === current.identity
}

private fun sameTypes(first: List<ConeCangJieType>, second: List<ConeCangJieType>): Boolean =
    first.size == second.size && first.indices.all { first[it] === second[it] }
