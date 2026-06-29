package org.cangnova.cangjie.chir.core.type

/**
 * CHIR 类型节点的稳定分类。
 *
 * 该枚举用于序列化、校验、打印和后端 lowering 阶段快速区分类型形态。
 */
enum class ChirTypeKind {
    INVALID,
    INT8,
    INT16,
    INT32,
    INT64,
    INT_NATIVE,
    UINT8,
    UINT16,
    UINT32,
    UINT64,
    UINT_NATIVE,
    FLOAT16,
    FLOAT32,
    FLOAT64,
    RUNE,
    BOOLEAN,
    UNIT,
    NOTHING,
    VOID,
    TUPLE,
    STRUCT,
    ENUM,
    FUNCTION,
    CLASS,
    RAW_ARRAY,
    VARRAY,
    C_POINTER,
    C_STRING,
    GENERIC,
    REF,
    BOX,
    THIS,
    NAMED,
}

/**
 * CHIR 类型模型的公共接口。
 */
sealed interface ChirType {
    /**
     * 类型的稳定分类。
     */
    val kind: ChirTypeKind

    /**
     * 面向调试、打印和序列化的可读类型名称。
     */
    val renderName: String
}

/**
 * CHIR 内建原始类型。
 */
enum class ChirPrimitiveType(
    /**
     * 原始类型对应的类型分类。
     */
    override val kind: ChirTypeKind,

    /**
     * 原始类型的稳定渲染名称。
     */
    override val renderName: String,
) : ChirType {
    UNIT(ChirTypeKind.UNIT, "unit"),
    BOOL(ChirTypeKind.BOOLEAN, "bool"),
    INT8(ChirTypeKind.INT8, "int8"),
    INT16(ChirTypeKind.INT16, "int16"),
    INT32(ChirTypeKind.INT32, "int32"),
    INT64(ChirTypeKind.INT64, "int64"),
    INT_NATIVE(ChirTypeKind.INT_NATIVE, "int_native"),
    UINT8(ChirTypeKind.UINT8, "uint8"),
    UINT16(ChirTypeKind.UINT16, "uint16"),
    UINT32(ChirTypeKind.UINT32, "uint32"),
    UINT64(ChirTypeKind.UINT64, "uint64"),
    UINT_NATIVE(ChirTypeKind.UINT_NATIVE, "uint_native"),
    FLOAT16(ChirTypeKind.FLOAT16, "float16"),
    FLOAT32(ChirTypeKind.FLOAT32, "float32"),
    FLOAT64(ChirTypeKind.FLOAT64, "float64"),
    RUNE(ChirTypeKind.RUNE, "rune"),
    NOTHING(ChirTypeKind.NOTHING, "nothing"),
    VOID(ChirTypeKind.VOID, "void"),
}

/**
 * 通过名称引用的 CHIR 类型。
 */
data class ChirNamedType(
    /**
     * 类型的名称或已解析显示名。
     */
    override val renderName: String,

    /**
     * 名称类型携带的类型实参。
     */
    val typeArguments: List<ChirTypeRef> = emptyList(),
) : ChirType {
    /**
     * 名称类型的固定分类。
     */
    override val kind: ChirTypeKind = ChirTypeKind.NAMED
}

/**
 * 元组类型。
 */
data class ChirTupleType(
    /**
     * 元组元素类型，顺序即元组布局顺序。
     */
    val elementTypes: List<ChirTypeRef>,
) : ChirType {
    /**
     * 元组类型的固定分类。
     */
    override val kind: ChirTypeKind = ChirTypeKind.TUPLE

    /**
     * 由元素类型拼接出的元组渲染名。
     */
    override val renderName: String = elementTypes.joinToString(prefix = "(", postfix = ")") { it.renderName }
}

/**
 * 函数类型。
 */
data class ChirFunctionType(
    /**
     * 函数参数类型列表。
     */
    val parameterTypes: List<ChirTypeRef>,

    /**
     * 函数返回类型。
     */
    val returnType: ChirTypeRef,

    /**
     * 扩展或成员函数的接收者类型；普通函数为 `null`。
     */
    val receiverType: ChirTypeRef? = null,

    /**
     * 函数类型是否声明抛出语义。
     */
    val throws: Boolean = false,
) : ChirType {
    /**
     * 函数类型的固定分类。
     */
    override val kind: ChirTypeKind = ChirTypeKind.FUNCTION

    /**
     * 由接收者、参数、返回类型和 throws 标记组合出的函数类型渲染名。
     */
    override val renderName: String = buildString {
        if (receiverType != null) {
            append(receiverType.renderName)
            append('.')
        }
        append(parameterTypes.joinToString(prefix = "(", postfix = ")") { it.renderName })
        append(" -> ")
        append(returnType.renderName)
        if (throws) append(" throws")
    }
}

/**
 * 结构体类型。
 */
data class ChirStructType(
    /**
     * 结构体名称。
     */
    val name: String,

    /**
     * 结构体字段类型列表。
     */
    val fieldTypes: List<ChirTypeRef> = emptyList(),

    /**
     * 结构体类型实参。
     */
    val typeArguments: List<ChirTypeRef> = emptyList(),
) : ChirType {
    /**
     * 结构体类型的固定分类。
     */
    override val kind: ChirTypeKind = ChirTypeKind.STRUCT

    /**
     * 结构体类型渲染名。
     */
    override val renderName: String = name
}

/**
 * 类类型。
 */
data class ChirClassType(
    /**
     * 类名称。
     */
    val name: String,

    /**
     * 类字段类型列表。
     */
    val fieldTypes: List<ChirTypeRef> = emptyList(),

    /**
     * 类直接父类型列表。
     */
    val superTypes: List<ChirTypeRef> = emptyList(),

    /**
     * 类类型实参。
     */
    val typeArguments: List<ChirTypeRef> = emptyList(),
) : ChirType {
    /**
     * 类类型的固定分类。
     */
    override val kind: ChirTypeKind = ChirTypeKind.CLASS

    /**
     * 类类型渲染名。
     */
    override val renderName: String = name
}

/**
 * 枚举类型中的单个 case 类型信息。
 */
data class ChirEnumCaseType(
    /**
     * 枚举 case 名称。
     */
    val name: String,

    /**
     * 枚举 case 携带的 payload 类型列表。
     */
    val payloadTypes: List<ChirTypeRef> = emptyList(),
)

/**
 * 枚举类型。
 */
data class ChirEnumType(
    /**
     * 枚举名称。
     */
    val name: String,

    /**
     * 枚举 case 列表。
     */
    val cases: List<ChirEnumCaseType> = emptyList(),

    /**
     * 枚举类型实参。
     */
    val typeArguments: List<ChirTypeRef> = emptyList(),
) : ChirType {
    /**
     * 枚举类型的固定分类。
     */
    override val kind: ChirTypeKind = ChirTypeKind.ENUM

    /**
     * 枚举类型渲染名。
     */
    override val renderName: String = name
}

/**
 * 原始数组类型。
 */
data class ChirRawArrayType(
    /**
     * 数组元素类型。
     */
    val elementType: ChirTypeRef,

    /**
     * 固定数组大小；未知或动态大小时为 `null`。
     */
    val size: Int? = null,
) : ChirType {
    /**
     * 原始数组类型的固定分类。
     */
    override val kind: ChirTypeKind = ChirTypeKind.RAW_ARRAY

    /**
     * 原始数组类型渲染名。
     */
    override val renderName: String = "rawarray<${elementType.renderName}>"
}

/**
 * VArray 类型。
 */
data class ChirVArrayType(
    /**
     * VArray 元素类型。
     */
    val elementType: ChirTypeRef,

    /**
     * VArray 维度或 rank。
     */
    val rank: Int = 1,
) : ChirType {
    /**
     * VArray 类型的固定分类。
     */
    override val kind: ChirTypeKind = ChirTypeKind.VARRAY

    /**
     * VArray 类型渲染名。
     */
    override val renderName: String = "varray$rank<${elementType.renderName}>"
}

/**
 * C 指针类型。
 */
data class ChirCPointerType(
    /**
     * 指针指向的元素类型。
     */
    val pointeeType: ChirTypeRef,
) : ChirType {
    /**
     * C 指针类型的固定分类。
     */
    override val kind: ChirTypeKind = ChirTypeKind.C_POINTER

    /**
     * C 指针类型渲染名。
     */
    override val renderName: String = "cpointer<${pointeeType.renderName}>"
}

/**
 * C 字符串类型。
 */
data object ChirCStringType : ChirType {
    /**
     * C 字符串类型的固定分类。
     */
    override val kind: ChirTypeKind = ChirTypeKind.C_STRING

    /**
     * C 字符串类型渲染名。
     */
    override val renderName: String = "cstring"
}

/**
 * 泛型类型参数引用。
 */
data class ChirGenericType(
    /**
     * 泛型参数标识符。
     */
    val identifier: String,

    /**
     * 泛型参数上界列表。
     */
    val upperBounds: List<ChirTypeRef> = emptyList(),

    /**
     * 泛型参数在声明中的索引；未知时为 `null`。
     */
    val declarationIndex: Int? = null,
) : ChirType {
    /**
     * 泛型类型的固定分类。
     */
    override val kind: ChirTypeKind = ChirTypeKind.GENERIC

    /**
     * 泛型类型渲染名。
     */
    override val renderName: String = identifier
}

/**
 * 引用类型。
 */
data class ChirRefType(
    /**
     * 被引用的类型。
     */
    val referencedType: ChirTypeRef,

    /**
     * 引用是否可变。
     */
    val mutable: Boolean = false,
) : ChirType {
    /**
     * 引用类型的固定分类。
     */
    override val kind: ChirTypeKind = ChirTypeKind.REF

    /**
     * 引用类型渲染名。
     */
    override val renderName: String = if (mutable) "mutref<${referencedType.renderName}>" else "ref<${referencedType.renderName}>"
}

/**
 * 装箱类型。
 */
data class ChirBoxType(
    /**
     * 被装箱的类型。
     */
    val boxedType: ChirTypeRef,
) : ChirType {
    /**
     * 装箱类型的固定分类。
     */
    override val kind: ChirTypeKind = ChirTypeKind.BOX

    /**
     * 装箱类型渲染名。
     */
    override val renderName: String = "box<${boxedType.renderName}>"
}

/**
 * this 类型。
 */
data class ChirThisType(
    /**
     * this 类型所属类型名称。
     */
    val ownerTypeName: String,
) : ChirType {
    /**
     * this 类型的固定分类。
     */
    override val kind: ChirTypeKind = ChirTypeKind.THIS

    /**
     * this 类型渲染名。
     */
    override val renderName: String = "this<$ownerTypeName>"
}

/**
 * CHIR 类型引用。
 *
 * 类型引用可以是已解析的完整类型，也可以是仍待符号绑定的未解析名称。
 */
sealed interface ChirTypeRef {
    /**
     * 类型引用的可读渲染名称。
     */
    val renderName: String
}

/**
 * 已解析的类型引用。
 */
data class ChirResolvedTypeRef(
    /**
     * 已解析出的具体类型节点。
     */
    val type: ChirType,
) : ChirTypeRef {
    /**
     * 透传具体类型的渲染名称。
     */
    override val renderName: String = type.renderName
}

/**
 * 尚未解析的类型引用。
 */
data class ChirUnresolvedTypeRef(
    /**
     * 未解析的类型符号文本。
     */
    val symbol: String,
) : ChirTypeRef {
    /**
     * 未解析引用以符号文本作为渲染名称。
     */
    override val renderName: String = symbol
}
