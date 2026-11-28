package org.cangnova.cangjie.chir.core.declaration

import org.cangnova.cangjie.chir.core.attribute.ChirAttribute
import org.cangnova.cangjie.chir.core.controlflow.ChirBlock
import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.model.ChirNode
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.chir.core.type.ChirTypeDeclarationId
import org.cangnova.cangjie.chir.core.type.ChirEnumCaseType
import org.cangnova.cangjie.chir.core.type.ChirFunctionType

/**
 * CHIR 类型定义声明的分类。
 */
enum class ChirTypeDefinitionKind {
    CLASS,
    INTERFACE,
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

    /** 泛型参数的完整类型身份与约束；名字列表用于源码显示和兼容查询。 */
    val typeParameterTypes: List<ChirTypeRef>
        get() = emptyList()
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

/** 有稳定名义类型身份、可被类型引用索引的 class/struct/enum 定义。 */
interface ChirNominalTypeDeclaration : ChirCustomTypeDeclaration {
    /** 定义与所有类型引用共享的结构化身份。 */
    val typeDeclarationId: ChirTypeDeclarationId
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
     * 函数自身声明的泛型形参，顺序与官方 CHIR 的 genericTypeParams 一致。
     *
     * 该信息用于后端构造函数隐藏 TypeInfo ABI 和校验泛型调用点；不能仅由函数参数类型推断，
     * 因为合法泛型函数可以不在值参数中直接使用某些类型形参。
     */
    val genericTypeParameters: List<ChirTypeRef>
        get() = emptyList()

    /** 函数是否以静态类型成员身份声明。 */
    val isStatic: Boolean
        get() = false

    /**
     * 函数控制流基本块列表。
     */
    val blocks: List<ChirBlock>

    /** 函数是否具有可执行的 CFG；仅有签名的导入声明必须明确标记为 false。 */
    val hasBody: Boolean
        get() = true

    /** 此函数是否以外部导入声明存在，而不是本包定义。 */
    val isImported: Boolean
        get() = false

    /**
     * 函数入口基本块语义标识。
     */
    val entryBlockId: ChirSemanticId?
}

/**
 * CHIR 类声明接口。
 */
interface ChirClassDeclaration : ChirNominalTypeDeclaration {
    /**
     * 类直接父类型列表。
     */
    val superTypes: List<ChirTypeRef>

    /**
     * 类实现的接口或扩展类型列表。
     */
    val implementedTypes: List<ChirTypeRef>

    /** 此 class definition 是否声明接口类型。 */
    val isInterface: Boolean
        get() = false

    /** 官方 CHIR 的按引入类型分组的有序虚方法表。 */
    val virtualMethodTables: List<ChirVirtualMethodTable>
        get() = emptyList()
}

/** 官方 CHIR 中一个引入类型对应的有序虚方法槽位组。 */
data class ChirVirtualMethodTable(
    /** 声明该组虚方法槽位的类或接口类型。 */
    val sourceParentType: ChirTypeRef,

    /** 按官方 CHIR 槽位序号排列的方法；列表索引就是组内分派 offset。 */
    val methods: List<ChirVirtualMethod>,
)

/** 官方虚方法槽位的签名、来源和当前实现事实。 */
data class ChirVirtualMethod(
    /** 仓颉源码方法标识符。 */
    val sourceCodeIdentifier: String,

    /** 虚方法未实例化的原始函数签名。 */
    val signatureType: ChirFunctionType,

    /** 方法声明的泛型参数，顺序与签名一致。 */
    val genericTypeParameters: List<ChirTypeRef>,

    /** 当前类型在此槽位选择的函数；null 表示抽象或尚无实现。 */
    val implementationSemanticId: ChirSemanticId?,

    /** 官方虚方法记录上的声明属性。 */
    val attributes: Set<ChirAttribute>,

    /** 此方法最初声明时的类型。 */
    val originalType: ChirTypeRef,

    /** 当前槽位所属的方法父类型。 */
    val parentType: ChirTypeRef,

    /** 官方记录中的返回类型。 */
    val returnType: ChirTypeRef,
)

/**
 * CHIR 接口声明接口。
 *
 * 接口是可被类实现的无状态抽象契约：成员只允许抽象方法声明，
 * 不携带实例字段状态。这与仓颉语言中 interface 的语义一致。
 */
interface ChirInterfaceDeclaration : ChirCustomTypeDeclaration {
    /**
     * 接口继承的父接口类型列表。
     */
    val superInterfaceTypes: List<ChirTypeRef>
}

/**
 * CHIR 结构体声明接口。
 */
interface ChirStructDeclaration : ChirNominalTypeDeclaration {
    /**
     * 结构体字段声明列表。
     */
    val fieldDeclarations: List<ChirVariableDeclaration>
}

/**
 * CHIR 枚举声明接口。
 */
interface ChirEnumDeclaration : ChirNominalTypeDeclaration {
    /**
     * 枚举 case 名称列表。
     */
    val cases: List<String>

    /** 枚举构造器的名字及 payload 类型，保留完整声明期布局。 */
    val caseTypes: List<ChirEnumCaseType>
        get() = cases.map(::ChirEnumCaseType)
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

    /** 声明期泛型参数引用，保留其稳定身份与上界。 */
    override val typeParameterTypes: List<ChirTypeRef> = emptyList(),
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
     * 类定义的稳定类型身份。
     */
    override val typeDeclarationId: ChirTypeDeclarationId,

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

    /** 声明期泛型参数引用，保留其稳定身份与上界。 */
    override val typeParameterTypes: List<ChirTypeRef> = emptyList(),

    /** 声明是接口时为 true；该事实参与 classfile 和动态调用指令生成。 */
    override val isInterface: Boolean = false,

    /** 官方 CHIR 的有序虚方法表，供类 vtable 和接口 MTable lowering 共用。 */
    override val virtualMethodTables: List<ChirVirtualMethodTable> = emptyList(),
) : ChirClassDeclaration {
    /**
     * 类声明的固定定义分类。
     */
    override val definitionKind: ChirTypeDefinitionKind = ChirTypeDefinitionKind.CLASS
}

/**
 * 默认接口声明实现。
 */
data class DefaultChirInterfaceDeclaration(
    /**
     * 声明语义标识。
     */
    override val semanticId: ChirSemanticId,

    /**
     * 接口名称。
     */
    override val name: String,

    /**
     * 接口类型参数名称列表。
     */
    override val typeParameters: List<String> = emptyList(),

    /**
     * 接口继承的父接口类型列表。
     */
    override val superInterfaceTypes: List<ChirTypeRef> = emptyList(),

    /**
     * 接口成员声明列表。
     */
    override val memberDeclarations: List<ChirDeclaration> = emptyList(),

    /**
     * 接口声明属性集合。
     */
    override val attributes: Set<ChirAttribute> = emptySet(),
) : ChirInterfaceDeclaration {
    /**
     * 接口声明的固定定义分类。
     */
    override val definitionKind: ChirTypeDefinitionKind = ChirTypeDefinitionKind.INTERFACE
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
     * 结构体定义的稳定类型身份。
     */
    override val typeDeclarationId: ChirTypeDeclarationId,

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

    /** 声明期泛型参数引用，保留其稳定身份与上界。 */
    override val typeParameterTypes: List<ChirTypeRef> = emptyList(),
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
     * 枚举定义的稳定类型身份。
     */
    override val typeDeclarationId: ChirTypeDeclarationId,

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

    /** 声明期泛型参数引用，保留其稳定身份与上界。 */
    override val typeParameterTypes: List<ChirTypeRef> = emptyList(),

    /** 与 [cases] 对齐的完整构造器签名。 */
    override val caseTypes: List<ChirEnumCaseType> = cases.map(::ChirEnumCaseType),
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

    /** 声明期泛型参数引用，保留其稳定身份与上界。 */
    override val typeParameterTypes: List<ChirTypeRef> = emptyList(),
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
    override val entryBlockId: ChirSemanticId?,

    /**
     * 函数声明属性集合。
     */
    override val attributes: Set<ChirAttribute> = emptySet(),

    /** 此函数是否携带可执行 CFG。 */
    override val hasBody: Boolean = true,

    /** 此函数是否为外部导入声明。 */
    override val isImported: Boolean = false,

    /** 函数声明期泛型形参；保留官方 CHIR 顺序与类型身份。 */
    override val genericTypeParameters: List<ChirTypeRef> = emptyList(),

    /** 函数是否以静态类型成员身份声明。 */
    override val isStatic: Boolean = false,
) : ChirFunctionDeclaration
