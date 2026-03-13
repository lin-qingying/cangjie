package org.cangnova.cangjie.chir.core.type

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

sealed interface ChirType {
    val kind: ChirTypeKind
    val renderName: String
}

enum class ChirPrimitiveType(
    override val kind: ChirTypeKind,
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

data class ChirNamedType(
    override val renderName: String,
    val typeArguments: List<ChirTypeRef> = emptyList(),
) : ChirType {
    override val kind: ChirTypeKind = ChirTypeKind.NAMED
}

data class ChirTupleType(
    val elementTypes: List<ChirTypeRef>,
) : ChirType {
    override val kind: ChirTypeKind = ChirTypeKind.TUPLE
    override val renderName: String = elementTypes.joinToString(prefix = "(", postfix = ")") { it.renderName }
}

data class ChirFunctionType(
    val parameterTypes: List<ChirTypeRef>,
    val returnType: ChirTypeRef,
    val receiverType: ChirTypeRef? = null,
    val throws: Boolean = false,
) : ChirType {
    override val kind: ChirTypeKind = ChirTypeKind.FUNCTION
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

data class ChirStructType(
    val name: String,
    val fieldTypes: List<ChirTypeRef> = emptyList(),
    val typeArguments: List<ChirTypeRef> = emptyList(),
) : ChirType {
    override val kind: ChirTypeKind = ChirTypeKind.STRUCT
    override val renderName: String = name
}

data class ChirClassType(
    val name: String,
    val fieldTypes: List<ChirTypeRef> = emptyList(),
    val superTypes: List<ChirTypeRef> = emptyList(),
    val typeArguments: List<ChirTypeRef> = emptyList(),
) : ChirType {
    override val kind: ChirTypeKind = ChirTypeKind.CLASS
    override val renderName: String = name
}

data class ChirEnumCaseType(
    val name: String,
    val payloadTypes: List<ChirTypeRef> = emptyList(),
)

data class ChirEnumType(
    val name: String,
    val cases: List<ChirEnumCaseType> = emptyList(),
    val typeArguments: List<ChirTypeRef> = emptyList(),
) : ChirType {
    override val kind: ChirTypeKind = ChirTypeKind.ENUM
    override val renderName: String = name
}

data class ChirRawArrayType(
    val elementType: ChirTypeRef,
    val size: Int? = null,
) : ChirType {
    override val kind: ChirTypeKind = ChirTypeKind.RAW_ARRAY
    override val renderName: String = "rawarray<${elementType.renderName}>"
}

data class ChirVArrayType(
    val elementType: ChirTypeRef,
    val rank: Int = 1,
) : ChirType {
    override val kind: ChirTypeKind = ChirTypeKind.VARRAY
    override val renderName: String = "varray$rank<${elementType.renderName}>"
}

data class ChirCPointerType(
    val pointeeType: ChirTypeRef,
) : ChirType {
    override val kind: ChirTypeKind = ChirTypeKind.C_POINTER
    override val renderName: String = "cpointer<${pointeeType.renderName}>"
}

data object ChirCStringType : ChirType {
    override val kind: ChirTypeKind = ChirTypeKind.C_STRING
    override val renderName: String = "cstring"
}

data class ChirGenericType(
    val identifier: String,
    val upperBounds: List<ChirTypeRef> = emptyList(),
    val declarationIndex: Int? = null,
) : ChirType {
    override val kind: ChirTypeKind = ChirTypeKind.GENERIC
    override val renderName: String = identifier
}

data class ChirRefType(
    val referencedType: ChirTypeRef,
    val mutable: Boolean = false,
) : ChirType {
    override val kind: ChirTypeKind = ChirTypeKind.REF
    override val renderName: String = if (mutable) "mutref<${referencedType.renderName}>" else "ref<${referencedType.renderName}>"
}

data class ChirBoxType(
    val boxedType: ChirTypeRef,
) : ChirType {
    override val kind: ChirTypeKind = ChirTypeKind.BOX
    override val renderName: String = "box<${boxedType.renderName}>"
}

data class ChirThisType(
    val ownerTypeName: String,
) : ChirType {
    override val kind: ChirTypeKind = ChirTypeKind.THIS
    override val renderName: String = "this<$ownerTypeName>"
}

sealed interface ChirTypeRef {
    val renderName: String
}

data class ChirResolvedTypeRef(
    val type: ChirType,
) : ChirTypeRef {
    override val renderName: String = type.renderName
}

data class ChirUnresolvedTypeRef(
    val symbol: String,
) : ChirTypeRef {
    override val renderName: String = symbol
}
