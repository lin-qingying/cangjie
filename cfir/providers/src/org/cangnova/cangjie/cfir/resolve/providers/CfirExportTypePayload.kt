package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.utils.explicitUpperBounds
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterType
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.ClassId
import java.util.Collections

/** 类型角色的完成输入与依赖闭包分开；有序子槽不能从去重children重建。 */
sealed interface CfirExportTypePayload {
    /** 实际 InvalidTy 状态无错误诊断；不能用于替代其它不可表示类型。 */
    data object Invalid : CfirExportTypePayload
    data class Primitive(val kind: PrimitiveTypeKind) : CfirExportTypePayload
    data class Nominal(
        val kind: CfirExportNominalTypeKind,
        val classId: ClassId,
        val declaration: CfirDeclaration?,
        val arguments: List<ConeCangJieType>,
        val isThisType: Boolean,
        val isRefEnum: Boolean,
    ) : CfirExportTypePayload
    data class Alias(
        val declaration: CfirDeclaration?,
        val arguments: List<ConeCangJieType>,
        val expandedType: ConeCangJieType?,
    ) : CfirExportTypePayload
    /** 语义bounds和排除隐式默认上界后的输入分别冻结；它们不代表原始where constraint节点。 */
    data class Generic(
        val parameter: CfirTypeParameter,
        val bounds: List<ConeCangJieType>,
        val explicitBounds: List<ConeCangJieType>,
    ) : CfirExportTypePayload
    data class Function(
        val parameters: List<ConeCangJieType>,
        val returnType: ConeCangJieType,
        val isC: Boolean,
        val isClosure: Boolean,
        val hasVariableLenArg: Boolean,
    ) : CfirExportTypePayload
    data class Tuple(val elements: List<ConeCangJieType>) : CfirExportTypePayload
    /** normal 实参与类型构造发布的原生布局分开冻结；捕获器不重新计算叶子或维度。 */
    data class RawArray(
        val declaration: CfirDeclaration?,
        val arguments: List<ConeCangJieType>,
        val layout: ConeRawArrayLayout,
    ) : CfirExportTypePayload
    data class VArray(val element: ConeCangJieType, val size: Long) : CfirExportTypePayload
    data class Pointer(val pointee: ConeCangJieType) : CfirExportTypePayload
    data object CString : CfirExportTypePayload
    /** 非完成/无SOURCE格式投影的实际类型仍保留身份；编码器不得将它们转为Native Invalid。 */
    data class Unrepresentable(val identity: ConeCangJieType) : CfirExportTypePayload
}

enum class CfirExportNominalTypeKind { CLASS, INTERFACE, STRUCT, ENUM }

/** 本函数只冻结已解析类型字段，不展开别名、不执行推断或为缺失绑定制造声明。 */
internal fun ConeCangJieType.captureExportTypePayload(declaration: CfirDeclaration?): CfirExportTypePayload = when (this) {
    ConeInvalidType -> CfirExportTypePayload.Invalid
    is ConePrimitiveType -> CfirExportTypePayload.Primitive(kind)
    is ConeClassLikeType -> if (classId == StdlibClassIds.RawArray) {
        CfirExportTypePayload.RawArray(declaration, freeze(typeArguments.map { it.type }),
            checkNotNull(rawArrayLayout) { "RawArray export requires its completed native layout" })
    } else CfirExportTypePayload.Nominal(
        if (isInterface) CfirExportNominalTypeKind.INTERFACE else CfirExportNominalTypeKind.CLASS,
        classId, declaration, freeze(typeArguments.map { it.type }), isThisType, false,
    )
    is ConeStructType -> CfirExportTypePayload.Nominal(
        CfirExportNominalTypeKind.STRUCT, classId, declaration, freeze(typeArguments.map { it.type }), false, false,
    )
    is ConeEnumType -> CfirExportTypePayload.Nominal(
        CfirExportNominalTypeKind.ENUM, classId, declaration, freeze(typeArguments.map { it.type }), false, isRefEnum,
    )
    is ConeTypeAliasType -> CfirExportTypePayload.Alias(declaration, freeze(typeArguments.map { it.type }), expandedType)
    is ConeTypeParameterType -> {
        val parameter = lookupTag.typeParameterSymbol.cfir
        CfirExportTypePayload.Generic(parameter,
            freeze(parameter.bounds.mapNotNull { it.coneTypeOrNull }),
            freeze(parameter.explicitUpperBounds.mapNotNull { it.coneTypeOrNull }))
    }
    is ConeFunctionType -> CfirExportTypePayload.Function(
        freeze(parameterTypes), returnType, isCFunc, isClosureType, hasVariableLenArg,
    )
    is ConeTupleType -> CfirExportTypePayload.Tuple(freeze(elementTypes))
    is ConeVArrayType -> CfirExportTypePayload.VArray(elementType, size)
    is ConePointerType -> CfirExportTypePayload.Pointer(pointeeType)
    is ConeCStringType -> CfirExportTypePayload.CString
    else -> CfirExportTypePayload.Unrepresentable(this)
}

private fun <T> freeze(values: Collection<T>): List<T> = Collections.unmodifiableList(values.toList())

/** 不具有格式槽角色的依赖闭包仍按身份去重；发布验证另查payload的完整有序列表。 */
internal fun ConeCangJieType.captureExportTypeDependencies(): List<ConeCangJieType> {
    val values = buildList {
        addAll(typeArguments.map { it.type })
        when (this@captureExportTypeDependencies) {
            is ConeFunctionType -> { addAll(parameterTypes); add(returnType) }
            is ConeTupleType -> addAll(elementTypes)
            is ConeVArrayType -> add(elementType)
            is ConePointerType -> add(pointeeType)
            is ConeIntersectionType -> { addAll(intersectedTypes); upperBoundForApproximation?.let(::add) }
            is ConeUnionType -> addAll(unionTypes)
            is ConeTypeAliasType -> expandedType?.let(::add)
            is ConeErrorType -> delegatedType?.let(::add)
            is ConeTypeParameterType -> lookupTag.typeParameterSymbol.cfir.bounds.mapNotNullTo(this) { it.coneTypeOrNull }
            else -> Unit
        }
    }
    val seen = Collections.newSetFromMap(java.util.IdentityHashMap<ConeCangJieType, Boolean>())
    return freeze(values.filter(seen::add))
}
