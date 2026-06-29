package org.cangnova.cangjie.chir.core.value

import org.cangnova.cangjie.chir.core.attribute.ChirAttribute
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirNode
import org.cangnova.cangjie.chir.core.type.ChirTypeRef

/**
 * CHIR 值节点的稳定分类。
 */
enum class ChirValueKind {
    LITERAL,
    GLOBAL,
    PARAMETER,
    IMPORTED_FUNCTION,
    IMPORTED_VARIABLE,
    LOCAL,
    FUNCTION,
    BLOCK,
    BLOCK_GROUP,
}

/**
 * CHIR 值节点公共接口。
 *
 * 值节点描述表达式、声明或控制流中可被引用的数据实体，并通过 [semanticId] 与使用点关联。
 */
sealed interface ChirValue : ChirNode {
    /**
     * 值节点分类。
     */
    val kind: ChirValueKind

    /**
     * 值节点的类型引用。
     */
    val type: ChirTypeRef

    /**
     * 面向调试和打印的显示名称。
     */
    val displayName: String?

    /**
     * 绑定在值节点上的 CHIR 属性集合。
     */
    val attributes: Set<ChirAttribute>

    /**
     * 值节点携带的注解文本。
     */
    val annotations: List<String>

    /**
     * 引用该值节点的用户节点标识集合。
     */
    val userIds: Set<ChirSemanticId>
}

/**
 * 局部值。
 */
data class ChirLocalValue(
    /**
     * 局部值的语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 局部值类型。
     */
    override val type: ChirTypeRef,

    /**
     * 局部值名称。
     */
    val name: String,

    /**
     * 局部值属性集合。
     */
    override val attributes: Set<ChirAttribute> = emptySet(),

    /**
     * 局部值注解列表。
     */
    override val annotations: List<String> = emptyList(),

    /**
     * 使用该局部值的节点标识集合。
     */
    override val userIds: Set<ChirSemanticId> = emptySet(),
) : ChirValue {
    /**
     * 局部值的固定分类。
     */
    override val kind: ChirValueKind = ChirValueKind.LOCAL

    /**
     * 局部值显示名。
     */
    override val displayName: String = name
}

/**
 * 常量字面量值。
 */
data class ChirConstantValue(
    /**
     * 常量值的语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 常量值类型。
     */
    override val type: ChirTypeRef,

    /**
     * 常量字面量文本。
     */
    val literal: String,

    /**
     * 常量值属性集合。
     */
    override val attributes: Set<ChirAttribute> = emptySet(),

    /**
     * 常量值注解列表。
     */
    override val annotations: List<String> = emptyList(),

    /**
     * 使用该常量值的节点标识集合。
     */
    override val userIds: Set<ChirSemanticId> = emptySet(),
) : ChirValue {
    /**
     * 常量值的固定分类。
     */
    override val kind: ChirValueKind = ChirValueKind.LITERAL

    /**
     * 常量值显示名。
     */
    override val displayName: String = literal
}

/**
 * 函数参数值。
 */
data class ChirParameterValue(
    /**
     * 参数值的语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 参数值类型。
     */
    override val type: ChirTypeRef,

    /**
     * 参数名称。
     */
    val name: String,

    /**
     * 拥有该参数的函数标识。
     */
    val ownerFunctionId: ChirSemanticId,

    /**
     * 参数值属性集合。
     */
    override val attributes: Set<ChirAttribute> = emptySet(),

    /**
     * 参数值注解列表。
     */
    override val annotations: List<String> = emptyList(),

    /**
     * 使用该参数值的节点标识集合。
     */
    override val userIds: Set<ChirSemanticId> = emptySet(),
) : ChirValue {
    /**
     * 参数值的固定分类。
     */
    override val kind: ChirValueKind = ChirValueKind.PARAMETER

    /**
     * 参数值显示名。
     */
    override val displayName: String = name
}

/**
 * 全局变量值。
 */
data class ChirGlobalValue(
    /**
     * 全局值的语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 全局值类型。
     */
    override val type: ChirTypeRef,

    /**
     * 全局值名称。
     */
    val name: String,

    /**
     * 全局值属性集合。
     */
    override val attributes: Set<ChirAttribute> = emptySet(),

    /**
     * 全局值注解列表。
     */
    override val annotations: List<String> = emptyList(),

    /**
     * 使用该全局值的节点标识集合。
     */
    override val userIds: Set<ChirSemanticId> = emptySet(),
) : ChirValue {
    /**
     * 全局值的固定分类。
     */
    override val kind: ChirValueKind = ChirValueKind.GLOBAL

    /**
     * 全局值显示名。
     */
    override val displayName: String = name
}

/**
 * 导入函数值。
 */
data class ChirImportedFunctionValue(
    /**
     * 导入函数值的语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 导入函数值类型。
     */
    override val type: ChirTypeRef,

    /**
     * 导入函数名称。
     */
    val name: String,

    /**
     * 导入函数属性集合。
     */
    override val attributes: Set<ChirAttribute> = emptySet(),

    /**
     * 导入函数注解列表。
     */
    override val annotations: List<String> = emptyList(),

    /**
     * 使用该导入函数值的节点标识集合。
     */
    override val userIds: Set<ChirSemanticId> = emptySet(),
) : ChirValue {
    /**
     * 导入函数值的固定分类。
     */
    override val kind: ChirValueKind = ChirValueKind.IMPORTED_FUNCTION

    /**
     * 导入函数值显示名。
     */
    override val displayName: String = name
}

/**
 * 导入变量值。
 */
data class ChirImportedVariableValue(
    /**
     * 导入变量值的语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 导入变量值类型。
     */
    override val type: ChirTypeRef,

    /**
     * 导入变量名称。
     */
    val name: String,

    /**
     * 导入变量属性集合。
     */
    override val attributes: Set<ChirAttribute> = emptySet(),

    /**
     * 导入变量注解列表。
     */
    override val annotations: List<String> = emptyList(),

    /**
     * 使用该导入变量值的节点标识集合。
     */
    override val userIds: Set<ChirSemanticId> = emptySet(),
) : ChirValue {
    /**
     * 导入变量值的固定分类。
     */
    override val kind: ChirValueKind = ChirValueKind.IMPORTED_VARIABLE

    /**
     * 导入变量值显示名。
     */
    override val displayName: String = name
}

/**
 * 函数值。
 */
data class ChirFunctionValue(
    /**
     * 函数值的语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 函数值类型。
     */
    override val type: ChirTypeRef,

    /**
     * 函数名称。
     */
    val name: String,

    /**
     * 函数值属性集合。
     */
    override val attributes: Set<ChirAttribute> = emptySet(),

    /**
     * 函数值注解列表。
     */
    override val annotations: List<String> = emptyList(),

    /**
     * 使用该函数值的节点标识集合。
     */
    override val userIds: Set<ChirSemanticId> = emptySet(),
) : ChirValue {
    /**
     * 函数值的固定分类。
     */
    override val kind: ChirValueKind = ChirValueKind.FUNCTION

    /**
     * 函数值显示名。
     */
    override val displayName: String = name
}

/**
 * 基本块值。
 */
data class ChirBlockValue(
    /**
     * 基本块值的语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 基本块值类型。
     */
    override val type: ChirTypeRef,

    /**
     * 基本块名称。
     */
    val name: String,

    /**
     * 拥有该基本块的函数标识。
     */
    val ownerFunctionId: ChirSemanticId,

    /**
     * 基本块属性集合。
     */
    override val attributes: Set<ChirAttribute> = emptySet(),

    /**
     * 基本块注解列表。
     */
    override val annotations: List<String> = emptyList(),

    /**
     * 使用该基本块值的节点标识集合。
     */
    override val userIds: Set<ChirSemanticId> = emptySet(),
) : ChirValue {
    /**
     * 基本块值的固定分类。
     */
    override val kind: ChirValueKind = ChirValueKind.BLOCK

    /**
     * 基本块值显示名。
     */
    override val displayName: String = name
}

/**
 * 基本块组值。
 */
data class ChirBlockGroupValue(
    /**
     * 基本块组值的语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 基本块组值类型。
     */
    override val type: ChirTypeRef,

    /**
     * 基本块组名称。
     */
    val name: String,

    /**
     * 基本块组属性集合。
     */
    override val attributes: Set<ChirAttribute> = emptySet(),

    /**
     * 基本块组注解列表。
     */
    override val annotations: List<String> = emptyList(),

    /**
     * 使用该基本块组值的节点标识集合。
     */
    override val userIds: Set<ChirSemanticId> = emptySet(),
) : ChirValue {
    /**
     * 基本块组值的固定分类。
     */
    override val kind: ChirValueKind = ChirValueKind.BLOCK_GROUP

    /**
     * 基本块组值显示名。
     */
    override val displayName: String = name
}
