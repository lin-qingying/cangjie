package org.cangnova.cangjie.chir.core.declaration

import org.cangnova.cangjie.chir.core.attribute.ChirAttribute
import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirNode
import org.cangnova.cangjie.chir.core.type.ChirTypeRef

/**
 * CHIR 类型定义声明的分类。
 */
enum class ChirTypeDefinitionKind {
    CLASS,
    STRUCT,
    ENUM,
    EXTEND,
    GENERIC,
}

/**
 * CHIR 声明节点公共接口。
 */
sealed interface ChirDeclaration : ChirNode {
    /**
     * 声明名称。
     */
    val name: String

    /**
     * 声明携带的属性集合。
     */
    val attributes: Set<ChirAttribute>
}

/**
 * CHIR 类型声明公共接口。
 */
interface ChirTypeDeclaration : ChirDeclaration {
    /**
     * 类型声明的泛型参数名称列表。
     */
    val typeParameters: List<String>
}

/**
 * 用户可定义成员的自定义类型声明接口。
 */
interface ChirCustomTypeDeclaration : ChirTypeDeclaration {
    /**
     * 自定义类型定义分类。
     */
    val definitionKind: ChirTypeDefinitionKind

    /**
     * 自定义类型包含的成员声明列表。
     */
    val memberDeclarations: List<ChirDeclaration>
}

/**
 * CHIR 变量声明接口。
 */
interface ChirVariableDeclaration : ChirDeclaration {
    /**
     * 变量声明类型。
     */
    val type: ChirTypeRef

    /**
     * 变量是否可变。
     */
    val mutable: Boolean
}

/**
 * CHIR 属性声明接口。
 */
interface ChirPropertyDeclaration : ChirVariableDeclaration

/**
 * CHIR 函数声明接口。
 */
interface ChirFunctionDeclaration : ChirDeclaration {
    /**
     * 函数返回类型。
     */
    val returnType: ChirTypeRef

    /**
     * 函数参数声明列表。
     */
    val parameters: List<ChirVariableDeclaration>

    /**
     * 函数控制流基本块列表。
     */
    val blocks: List<ChirBlock>

    /**
     * 函数入口基本块语义标识。
     */
    val entryBlockId: ChirSemanticId
}

/**
 * CHIR 类声明接口。
 */
interface ChirClassDeclaration : ChirCustomTypeDeclaration {
    /**
     * 类直接父类型列表。
     */
    val superTypes: List<ChirTypeRef>

    /**
     * 类实现的接口或扩展类型列表。
     */
    val implementedTypes: List<ChirTypeRef>
}

/**
 * CHIR 结构体声明接口。
 */
interface ChirStructDeclaration : ChirCustomTypeDeclaration {
    /**
     * 结构体字段声明列表。
     */
    val fieldDeclarations: List<ChirVariableDeclaration>
}

/**
 * CHIR 枚举声明接口。
 */
interface ChirEnumDeclaration : ChirCustomTypeDeclaration {
    /**
     * 枚举 case 名称列表。
     */
    val cases: List<String>
}

/**
 * CHIR extend 声明接口。
 */
interface ChirExtendDeclaration : ChirCustomTypeDeclaration {
    /**
     * 被扩展的目标类型。
     */
    val targetType: ChirTypeRef

    /**
     * 扩展声明引入的扩展类型列表。
     */
    val extendedTypes: List<ChirTypeRef>
}

/**
 * 默认类型声明实现。
 */
data class DefaultChirTypeDeclaration(
    /**
     * 声明语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 类型声明名称。
     */
    override val name: String,

    /**
     * 类型参数名称列表。
     */
    override val typeParameters: List<String> = emptyList(),

    /**
     * 声明属性集合。
     */
    override val attributes: Set<ChirAttribute> = emptySet(),
) : ChirTypeDeclaration

/**
 * 默认类声明实现。
 */
data class DefaultChirClassDeclaration(
    /**
     * 声明语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 类名称。
     */
    override val name: String,

    /**
     * 类类型参数名称列表。
     */
    override val typeParameters: List<String> = emptyList(),

    /**
     * 类直接父类型列表。
     */
    override val superTypes: List<ChirTypeRef> = emptyList(),

    /**
     * 类实现类型列表。
     */
    override val implementedTypes: List<ChirTypeRef> = emptyList(),

    /**
     * 类成员声明列表。
     */
    override val memberDeclarations: List<ChirDeclaration> = emptyList(),

    /**
     * 类声明属性集合。
     */
    override val attributes: Set<ChirAttribute> = emptySet(),
) : ChirClassDeclaration {
    /**
     * 类声明的固定定义分类。
     */
    override val definitionKind: ChirTypeDefinitionKind = ChirTypeDefinitionKind.CLASS
}

/**
 * 默认结构体声明实现。
 */
data class DefaultChirStructDeclaration(
    /**
     * 声明语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 结构体名称。
     */
    override val name: String,

    /**
     * 结构体类型参数名称列表。
     */
    override val typeParameters: List<String> = emptyList(),

    /**
     * 结构体字段声明列表。
     */
    override val fieldDeclarations: List<ChirVariableDeclaration> = emptyList(),

    /**
     * 结构体成员声明列表，默认与字段声明一致。
     */
    override val memberDeclarations: List<ChirDeclaration> = fieldDeclarations,

    /**
     * 结构体声明属性集合。
     */
    override val attributes: Set<ChirAttribute> = emptySet(),
) : ChirStructDeclaration {
    /**
     * 结构体声明的固定定义分类。
     */
    override val definitionKind: ChirTypeDefinitionKind = ChirTypeDefinitionKind.STRUCT
}

/**
 * 默认枚举声明实现。
 */
data class DefaultChirEnumDeclaration(
    /**
     * 声明语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 枚举名称。
     */
    override val name: String,

    /**
     * 枚举类型参数名称列表。
     */
    override val typeParameters: List<String> = emptyList(),

    /**
     * 枚举 case 名称列表。
     */
    override val cases: List<String> = emptyList(),

    /**
     * 枚举成员声明列表。
     */
    override val memberDeclarations: List<ChirDeclaration> = emptyList(),

    /**
     * 枚举声明属性集合。
     */
    override val attributes: Set<ChirAttribute> = emptySet(),
) : ChirEnumDeclaration {
    /**
     * 枚举声明的固定定义分类。
     */
    override val definitionKind: ChirTypeDefinitionKind = ChirTypeDefinitionKind.ENUM
}

/**
 * 默认 extend 声明实现。
 */
data class DefaultChirExtendDeclaration(
    /**
     * 声明语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * extend 声明名称。
     */
    override val name: String,

    /**
     * 被扩展的目标类型。
     */
    override val targetType: ChirTypeRef,

    /**
     * 扩展引入的类型列表。
     */
    override val extendedTypes: List<ChirTypeRef> = emptyList(),

    /**
     * extend 类型参数名称列表。
     */
    override val typeParameters: List<String> = emptyList(),

    /**
     * extend 成员声明列表。
     */
    override val memberDeclarations: List<ChirDeclaration> = emptyList(),

    /**
     * extend 声明属性集合。
     */
    override val attributes: Set<ChirAttribute> = emptySet(),
) : ChirExtendDeclaration {
    /**
     * extend 声明的固定定义分类。
     */
    override val definitionKind: ChirTypeDefinitionKind = ChirTypeDefinitionKind.EXTEND
}

/**
 * 默认变量声明实现。
 */
data class DefaultChirVariableDeclaration(
    /**
     * 声明语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 变量名称。
     */
    override val name: String,

    /**
     * 变量类型。
     */
    override val type: ChirTypeRef,

    /**
     * 变量是否可变。
     */
    override val mutable: Boolean,

    /**
     * 变量声明属性集合。
     */
    override val attributes: Set<ChirAttribute> = emptySet(),
) : ChirVariableDeclaration

/**
 * 默认属性声明实现。
 */
data class DefaultChirPropertyDeclaration(
    /**
     * 声明语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 属性名称。
     */
    override val name: String,

    /**
     * 属性类型。
     */
    override val type: ChirTypeRef,

    /**
     * 属性是否可变。
     */
    override val mutable: Boolean,

    /**
     * 属性声明属性集合。
     */
    override val attributes: Set<ChirAttribute> = emptySet(),
) : ChirPropertyDeclaration

/**
 * 默认函数声明实现。
 */
data class DefaultChirFunctionDeclaration(
    /**
     * 声明语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 函数名称。
     */
    override val name: String,

    /**
     * 函数返回类型。
     */
    override val returnType: ChirTypeRef,

    /**
     * 函数参数声明列表。
     */
    override val parameters: List<ChirVariableDeclaration>,

    /**
     * 函数基本块列表。
     */
    override val blocks: List<ChirBlock>,

    /**
     * 函数入口基本块标识。
     */
    override val entryBlockId: ChirSemanticId,

    /**
     * 函数声明属性集合。
     */
    override val attributes: Set<ChirAttribute> = emptySet(),
) : ChirFunctionDeclaration
