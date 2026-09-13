package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.cfir.declarations.CfirStruct
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.annotations.CangjieAnnotationKind
import org.cangnova.cangjie.name.ClassId

/**
 * 仓颉 C 互操作 `CType` 语义。
 *
 * 官方前端在 `Ty::IsMetCType` / CHIR `Type::SatisfyCType` 中把
 * primitive C 类型、`CPointer`、`CString`、C function type、C struct 与递归满足
 * 条件的 `VArray` 视为满足 `CType`。`QuestTy` 是官方 AST 前端在部分未决
 * 类型场景中保留的 CType 原子值；CHIR lowering 必须在它仍存在时拒绝，不能
 * 在这里偷偷把它转换成其他类型。
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
            // Official Ty::IsMetCType treats a C function type as an atomic
            // CType. Its parameter/return signature is checked separately.
            is ConeFunctionType -> expandedType.isCFunc
            is ConeStructType -> isCStructType(session, expandedType)
            else -> false
        }
    }

    /**
     * 判断 struct 类型是否带有 C 互操作边界注解。
     */
    private fun isCStructType(session: CfirSession, type: ConeStructType): Boolean {
        val symbol = session.symbolProvider.getClassLikeSymbolByClassId(type.classId) ?: return false
        val declaration = symbol.cfir as? CfirStruct ?: return false
        return declaration.hasCAnnotation()
    }

    /**
     * 判断 struct 声明是否显式标记为 C 互操作边界。
     *
     * `annotationKind` 是 annotation resolve 的唯一 builtin 事实源。
     * raw callee 的短名只属于展示/诊断数据，不能在 CType owner 中再次参与
     * 语义判断；未完成 annotation resolve 的声明必须等待其 owner 发布结果。
     */
    private fun CfirStruct.hasCAnnotation(): Boolean =
        annotations.asSequence()
            .filterIsInstance<CfirAnnotationCall>()
            .any { annotation -> annotation.annotationKind == CangjieAnnotationKind.C }
}
