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

    private val ffiBoundaryAnnotationNames: Set<Name> = setOf(Name.identifier("C"))

    fun isCTypeClassId(classId: ClassId?): Boolean =
        classId == StdlibClassIds.CType

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

    private fun isCStructType(session: CfirSession, type: ConeStructType): Boolean {
        val symbol = session.symbolProvider.getClassLikeSymbolByClassId(type.classId) ?: return false
        val declaration = symbol.cfir as? CfirStruct ?: return false
        return declaration.hasForeignInteropBoundaryAnnotation()
    }

    private fun CfirStruct.hasForeignInteropBoundaryAnnotation(): Boolean {
        return annotations.any { annotation ->
            val annotationClassId = annotation.typeRef.coneTypeOrNull?.classIdOrPrimitiveClassId
            annotationClassId?.shortClassName in ffiBoundaryAnnotationNames ||
                    annotation.source.annotationShortNameOrNull() in ffiBoundaryAnnotationNames
        } || source.annotationTextContainsAny(ffiBoundaryAnnotationNames)
    }

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

    private fun org.cangnova.cangjie.source.CjSourceElement?.annotationTextContainsAny(names: Set<Name>): Boolean {
        val rawText = this?.text?.toString().orEmpty()
        return names.any { name -> rawText.contains("@${name.asString()}") }
    }
}
