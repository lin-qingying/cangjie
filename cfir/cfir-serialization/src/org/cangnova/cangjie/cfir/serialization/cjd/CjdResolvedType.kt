package org.cangnova.cangjie.cfir.serialization.cjd

/**
 * CJO 的已解析 Ty 视图，刻意不复用源码 [TypeKey]：二进制不能恢复 QualifiedType 的 base。
 * Unknown 不是通配符；不展开 alias，也不创建 CFIR 或查询符号 provider。
 */
sealed interface CjdResolvedType {
    data class Primitive(val name: String) : CjdResolvedType
    data class Named(
        val name: String,
        val arguments: List<CjdResolvedType> = emptyList(),
        val target: CjdBinaryTypeTarget? = null,
        val isCoreOption: Boolean = false,
    ) : CjdResolvedType
    data class Function(val parameters: List<CjdResolvedType>, val result: CjdResolvedType) : CjdResolvedType
    data class Tuple(val elements: List<CjdResolvedType>) : CjdResolvedType
    data class VArray(val component: CjdResolvedType, val size: Long) : CjdResolvedType
    data class Unknown(val reason: String) : CjdResolvedType
}

/** 原始 FullId 的声明事实，仅用于名称比较和 extend 导出判定，不持有声明实例。 */
data class CjdBinaryTypeTarget(val name: String, val packageName: String, val exported: Boolean)

/**
 * Type/Ty 有向比较。Qualified 只比较末段名（官方 MergeAnnoFromCjd.cpp），不臆造源码限定路径。
 * Rune 按现有 TypeKey 策略统一为 UInt8；目标 Unknown 始终拒绝，包括源 Opaque。
 */
fun TypeKey.matchesResolved(target: CjdResolvedType): Boolean {
    if (target is CjdResolvedType.Unknown) return false
    return when (this) {
        is TypeKey.Opaque -> true
        is TypeKey.Primitive -> target is CjdResolvedType.Primitive &&
            TypeKey.primitive(name) == TypeKey.primitive(target.name)
        is TypeKey.Ref -> when (target) {
            is CjdResolvedType.Named -> name == target.name && arguments.matchResolvedTypes(target.arguments)
            is CjdResolvedType.Primitive -> name == target.name && arguments.isEmpty()
            else -> false
        }
        is TypeKey.Qualified -> target is CjdResolvedType.Named && name == target.name
        is TypeKey.Function -> target is CjdResolvedType.Function &&
            parameters.matchResolvedTypes(target.parameters) && result.matchesResolved(target.result)
        is TypeKey.Tuple -> target is CjdResolvedType.Tuple && elements.matchResolvedTypes(target.elements)
        is TypeKey.Option -> target is CjdResolvedType.Named && target.isCoreOption &&
            target.arguments.size == 1 && component.matchesResolved(target.arguments.single())
        is TypeKey.VArray -> target is CjdResolvedType.VArray && component.matchesResolved(target.component) &&
            size.literalKind == "INTEGER" && size.value == target.size.toString()
        // CJO 没有独立 Constant Ty。VArray 的 size 在上一个分支比较，不把未知目标当常量。
        is TypeKey.Constant -> false
    }
}

internal fun List<TypeKey>.matchResolvedTypes(target: List<CjdResolvedType>): Boolean =
    size == target.size && indices.all { this[it].matchesResolved(target[it]) }
