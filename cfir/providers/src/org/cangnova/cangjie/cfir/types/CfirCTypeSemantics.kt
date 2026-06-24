package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.text

/**
 * 仓颉 C 互操作 `CType` 语义。
 *
 * 官方前端在 `Ty::IsMetCType` / CHIR `Type::SatisfyCType` 中把
 * primitive C 类型、`CPointer`、`CString`、`CFunc`、C struct 与递归满足
 * 条件的 `VArray` 视为满足 `CType`。这里放在 providers/type-system 层，
 * 让解析、推断、诊断共享同一套 subtype 事实。
 */
object CfirCTypeSemantics {
    /**
     * 官方视为满足 `CType` 的 primitive 类型集合。
     */
    private val primitiveCTypes: Set<PrimitiveTypeKind> = setOf(
        PrimitiveTypeKind.UNIT,
        PrimitiveTypeKind.BOOLEAN,
        PrimitiveTypeKind.INT8,
        PrimitiveTypeKind.UINT8,
        PrimitiveTypeKind.INT16,
        PrimitiveTypeKind.UINT16,
        PrimitiveTypeKind.INT32,
        PrimitiveTypeKind.UINT32,
        PrimitiveTypeKind.INT64,
        PrimitiveTypeKind.UINT64,
        PrimitiveTypeKind.INT_NATIVE,
        PrimitiveTypeKind.UINT_NATIVE,
        PrimitiveTypeKind.FLOAT32,
        PrimitiveTypeKind.FLOAT64,
    )

    /**
     * 标记外部互操作边界的注解短名集合。
     */
    private val ffiBoundaryAnnotationNames: Set<Name> = setOf(Name.identifier("C"))

    /**
     * 判断 [classId] 是否为标准库 `CType`。
     */
    fun isCTypeClassId(classId: ClassId?): Boolean =
        classId == StdlibClassIds.CType

    /**
     * 判断 [type] 是否满足官方 C 互操作 `CType` 约束。
     */
    fun isMetCType(session: CfirSession, type: ConeCangJieType): Boolean {
        return when (val expandedType = type.fullyExpandedType(session)) {
            is ConeVArrayType -> isMetCType(session, expandedType.elementType)
            is ConePrimitiveType -> expandedType.kind in primitiveCTypes
            is ConePointerType,
            is ConeCStringType,
            is ConeQuestType,
                -> true
            is ConeFunctionType -> expandedType.isCFunc &&
                    expandedType.parameterTypes.all { isMetCType(session, it) } &&
                    isMetCType(session, expandedType.returnType)
            is ConeStructType -> isCStructType(session, expandedType)
            else -> isCTypeClassId(expandedType.classIdOrPrimitiveClassId)
        }
    }

    /**
     * 判断 struct 类型是否带有 C 互操作边界注解。
     */
    private fun isCStructType(session: CfirSession, type: ConeStructType): Boolean {
        val symbol = session.symbolProvider.getClassLikeSymbolByClassId(type.classId) ?: return false
        val declaration = symbol.cfir as? CfirStruct ?: return false
        return declaration.hasForeignInteropBoundaryAnnotation()
    }

    /**
     * 判断 struct 声明是否显式标记为 foreign interop 边界。
     */
    private fun CfirStruct.hasForeignInteropBoundaryAnnotation(): Boolean {
        return annotations.any { annotation ->
            val annotationClassId = annotation.typeRef.coneTypeOrNull?.classIdOrPrimitiveClassId
            annotationClassId?.shortClassName in ffiBoundaryAnnotationNames ||
                    annotation.source.annotationShortNameOrNull() in ffiBoundaryAnnotationNames
        } || source.annotationTextContainsAny(ffiBoundaryAnnotationNames)
    }

    /**
     * 从注解 source 文本中提取短名。
     */
    private fun org.cangnova.cangjie.source.CjSourceElement?.annotationShortNameOrNull(): Name? {
        val rawText = this?.text?.toString()?.trim().orEmpty()
        if (!rawText.startsWith("@")) return null

        val shortName = rawText
            .removePrefix("@")
            .substringBefore('(')
            .substringAfterLast('.')
            .trim()
        return Name.identifierIfValid(shortName)
    }

    /**
     * 通过 source 文本检查是否包含目标注解。
     *
     * 该路径用于 annotation type 尚未完全解析时保留官方 `@C` 边界语义。
     */
    private fun org.cangnova.cangjie.source.CjSourceElement?.annotationTextContainsAny(names: Set<Name>): Boolean {
        val rawText = this?.text?.toString().orEmpty()
        return names.any { name -> rawText.contains("@${name.asString()}") }
    }
}
