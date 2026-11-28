package org.cangnova.cangjie.codegen.types

import org.cangnova.cangjie.chir.core.identity.ChirSemanticId
import org.cangnova.cangjie.chir.core.type.ChirBoxType
import org.cangnova.cangjie.chir.core.type.ChirCPointerType
import org.cangnova.cangjie.chir.core.type.ChirClassType
import org.cangnova.cangjie.chir.core.type.ChirCStringType
import org.cangnova.cangjie.chir.core.type.ChirEnumType
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirGenericType
import org.cangnova.cangjie.chir.core.type.ChirNamedType
import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirRawArrayType
import org.cangnova.cangjie.chir.core.type.ChirRefType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirStructType
import org.cangnova.cangjie.chir.core.type.ChirThisType
import org.cangnova.cangjie.chir.core.type.ChirTupleType
import org.cangnova.cangjie.chir.core.type.ChirType
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.chir.core.type.ChirVArrayType
import org.cangnova.cangjie.codegen.diagnostics.CodegenLoweringException
import org.cangnova.cangjie.codegen.ir.LlvmFunctionAbi
import org.cangnova.cangjie.codegen.ir.llvmIdentifier

/**
 * 当前 LLVM module 中 RawArray 分配所需的官方运行时元数据与 ABI 声明。
 */
internal data class LlvmRawArrayRuntimePlan(
    val typeDeclarations: List<String>,
    val globalDeclarations: List<String>,
    val runtimeDeclarations: List<String>,
    val runtimeFunctionAbis: List<LlvmFunctionAbi>,
)

/** RawArray 元素复制需要的布局与 GC 语义。 */
internal enum class LlvmRawArrayCopyStrategy {
    /** 元素大小在编译期确定且布局不含托管引用。 */
    MEMMOVE,

    /** 元素本身是托管引用。 */
    REFERENCE,

    /** 元素是含托管引用的聚合布局。 */
    STRUCT,

    /** 元素尺寸或引用布局取决于运行时泛型 TypeInfo。 */
    GENERIC,
}

/** 仓颉运行时 RawArray ABI 的类型身份和符号规则。 */
internal object LlvmRawArrayRuntime {
    const val TYPE_INFO_TYPE_NAME = "%TypeInfo"
    const val TYPE_INFO_TYPE_DECLARATION =
        "%TypeInfo = type { ptr, i8, i8, i16, i32, ptr, i32, i8, i8, i16, ptr, ptr, ptr, ptr, ptr, ptr, ptr, ptr }"
    const val TYPE_INFO_TYPE_ARGUMENTS_FIELD_INDEX = 12
    const val NEGATIVE_ARRAY_SIZE_TYPE_INFO = "std.core:NegativeArraySizeException.ti"
    const val NEGATIVE_ARRAY_SIZE_CONSTRUCTOR = "_CNat26NegativeArraySizeException6<init>Hv"
    const val GET_OR_CREATE_TYPE_INFO = "CJ_MCC_GetOrCreateTypeInfo"

    fun arrayType(typeRef: ChirTypeRef?, sourceId: ChirSemanticId): ChirRawArrayType {
        val resolved = (typeRef as? ChirResolvedTypeRef)?.type
        val arrayType = when (resolved) {
            is ChirRawArrayType -> resolved
            is ChirRefType -> (resolved.referencedType as? ChirResolvedTypeRef)?.type as? ChirRawArrayType
            else -> null
        }
        return arrayType ?: throw CodegenLoweringException(
            "RawArray operation requires a resolved RawArray result or destination type",
            sourceId,
        )
    }

    fun copyStrategy(elementType: ChirTypeRef): LlvmRawArrayCopyStrategy {
        if (containsGeneric(elementType)) return LlvmRawArrayCopyStrategy.GENERIC
        val type = (elementType as? ChirResolvedTypeRef)?.type
        return when {
            type is ChirRefType || type is ChirRawArrayType -> LlvmRawArrayCopyStrategy.REFERENCE
            containsManagedReferences(elementType) -> LlvmRawArrayCopyStrategy.STRUCT
            else -> LlvmRawArrayCopyStrategy.MEMMOVE
        }
    }

    fun containsManagedReferences(typeRef: ChirTypeRef): Boolean {
        val type = (typeRef as? ChirResolvedTypeRef)?.type ?: return false
        return when (type) {
            is ChirRefType, is ChirRawArrayType -> true
            is ChirStructType -> type.fieldTypes.any(::containsManagedReferences)
            is ChirTupleType -> type.elementTypes.any(::containsManagedReferences)
            is ChirEnumType -> type.cases.any { case -> case.payloadTypes.any(::containsManagedReferences) }
            is ChirBoxType -> containsManagedReferences(type.boxedType)
            is ChirVArrayType -> containsManagedReferences(type.elementType)
            else -> false
        }
    }

    fun typeInfoName(type: ChirTypeRef, sourceId: ChirSemanticId): String {
        val resolved = (type as? ChirResolvedTypeRef)?.type ?: throw CodegenLoweringException(
            "RawArray component type '${type.renderName}' is unresolved",
            sourceId,
        )
        return typeInfoName(resolved, sourceId)
    }

    fun rawArrayTypeInfoName(
        type: ChirRawArrayType,
        sourceId: ChirSemanticId,
        forNameFieldOfTypeInfo: Boolean = false,
    ): String {
        if (type.dimensions != 1u) {
            throw CodegenLoweringException(
                "RawArray TypeInfo emission does not support dimension count ${type.dimensions}",
                sourceId,
            )
        }
        return "RawArray<${typeInfoDisplayName(type.elementType, sourceId, forNameFieldOfTypeInfo)}>"
    }

    fun allocationFunction(type: ChirRawArrayType, sourceId: ChirSemanticId): String {
        val element = (type.elementType as? ChirResolvedTypeRef)?.type ?: throw CodegenLoweringException(
            "RawArray allocation requires a resolved component type",
            sourceId,
        )
        return when (element) {
            is ChirGenericType -> "MCC_NewArrayGeneric"
            is ChirRefType -> "MCC_NewObjArray"
            is ChirClassType, is ChirRawArrayType, is ChirBoxType -> "MCC_NewObjArray"
            is ChirPrimitiveType -> when (element) {
                ChirPrimitiveType.BOOL,
                ChirPrimitiveType.INT8,
                ChirPrimitiveType.UINT8,
                -> "MCC_NewArray8"
                ChirPrimitiveType.INT16,
                ChirPrimitiveType.UINT16,
                ChirPrimitiveType.FLOAT16,
                -> "MCC_NewArray16"
                ChirPrimitiveType.INT32,
                ChirPrimitiveType.UINT32,
                ChirPrimitiveType.FLOAT32,
                ChirPrimitiveType.RUNE,
                -> "MCC_NewArray32"
                ChirPrimitiveType.INT64,
                ChirPrimitiveType.UINT64,
                ChirPrimitiveType.INT_NATIVE,
                ChirPrimitiveType.UINT_NATIVE,
                ChirPrimitiveType.FLOAT64,
                -> "MCC_NewArray64"
                else -> "MCC_NewArray"
            }
            else -> "MCC_NewArray"
        }
    }

    fun global(name: String): String = "@\"${escapeLlvmIdentifier(name)}\""

    fun declarations(
        allocationExpressions: Collection<Pair<ChirRawArrayType, ChirSemanticId>>,
        referencedArrayTypes: Collection<Pair<ChirRawArrayType, ChirSemanticId>> = emptyList(),
        referencedTypeInfos: Collection<Pair<ChirTypeRef, ChirSemanticId>> = emptyList(),
    ): LlvmRawArrayRuntimePlan {
        val externalTypeInfoNames = linkedSetOf<String>()
        val dynamicTemplateReferences = linkedMapOf<String, LlvmTypeInfoTemplateReference>()
        fun recordTypeInfoReference(type: ChirTypeRef, sourceId: ChirSemanticId) {
            val template = LlvmRuntimeTypeInfo.dynamicTemplateReference(type)
            if (template != null) {
                dynamicTemplateReferences.putIfAbsent(template.symbolName, template)
                template.forEachDistinctTypeArgument { argument -> recordTypeInfoReference(argument, sourceId) }
            } else if (requiresExternalTypeInfo(type)) {
                externalTypeInfoNames += typeInfoSymbol(type, sourceId)
            }
        }
        referencedTypeInfos.forEach { (type, sourceId) -> recordTypeInfoReference(type, sourceId) }
        val hasRawArrayMetadata = allocationExpressions.isNotEmpty() || referencedArrayTypes.isNotEmpty()
        if (!hasRawArrayMetadata && externalTypeInfoNames.isEmpty() && dynamicTemplateReferences.isEmpty()) {
            return LlvmRawArrayRuntimePlan(emptyList(), emptyList(), emptyList(), emptyList())
        }

        val sourceIdByTypeInfo = linkedMapOf<String, ChirSemanticId>()
        val allTypesByTypeInfo = linkedMapOf<String, ChirRawArrayType>()
        fun registerArrayType(type: ChirRawArrayType, sourceId: ChirSemanticId) {
            val key = typeInfoNameKey(type)
            if (allTypesByTypeInfo.putIfAbsent(key, type) != null) return
            sourceIdByTypeInfo[key] = sourceId
            val component = (type.elementType as? ChirResolvedTypeRef)?.type as? ChirRawArrayType
            if (component != null) registerArrayType(component, sourceId)
        }
        (allocationExpressions + referencedArrayTypes).forEach { (type, sourceId) -> registerArrayType(type, sourceId) }
        val allocatedTypes = allocationExpressions.distinctBy { (type, _) -> typeInfoNameKey(type) }
        val allTypes = allTypesByTypeInfo.values.toList()
        allTypes.forEach { arrayType ->
            recordTypeInfoReference(arrayType.elementType, sourceIdByTypeInfo.getValue(typeInfoNameKey(arrayType)))
        }
        val dynamicTypes = allTypes.filter { containsGeneric(it.elementType) }
        val hasDynamicTemplateInstantiation = dynamicTemplateReferences.isNotEmpty() || dynamicTypes.isNotEmpty()
        if (hasRawArrayMetadata) {
            dynamicTemplateReferences.putIfAbsent(
                "RawArray.tt",
                LlvmTypeInfoTemplateReference("RawArray.tt", emptyList()),
            )
        }

        val externalTypeInfos = linkedSetOf<String>()
        allTypes.forEach { arrayType ->
            val sourceId = sourceIdByTypeInfo.getValue(typeInfoNameKey(arrayType))
            val componentType = arrayType.elementType
            if (!containsGeneric(componentType) && (componentType as? ChirResolvedTypeRef)?.type !is ChirRawArrayType) {
                externalTypeInfos += typeInfoSymbol(componentType, sourceId)
            }
        }
        externalTypeInfos += externalTypeInfoNames
        if (hasRawArrayMetadata) externalTypeInfos += NEGATIVE_ARRAY_SIZE_TYPE_INFO

        val typeDeclarations = buildList {
            if (externalTypeInfos.isNotEmpty() || hasRawArrayMetadata || dynamicTemplateReferences.isNotEmpty()) {
                add(TYPE_INFO_TYPE_DECLARATION)
            }
            if (hasRawArrayMetadata || dynamicTemplateReferences.isNotEmpty()) {
                add("%TypeTemplate = type { ptr, i8, i8, i16, i16, ptr, ptr, ptr, ptr, ptr, i16 }")
                if (dynamicTypes.isNotEmpty()) add("%GenericCustomTypeInfo = type { ptr, i8, i8, ptr, ptr }")
            }
        }
        val globalDeclarations = buildList {
            dynamicTemplateReferences.keys.sorted().forEach { symbol ->
                add("${global(symbol)} = external global %TypeTemplate")
            }
            externalTypeInfos.sorted().forEach { name ->
                add("${global(name)} = external global %TypeInfo")
            }
            allTypes.sortedBy(::typeInfoNameKey).forEach { arrayType ->
                val sourceId = sourceIdByTypeInfo.getValue(typeInfoNameKey(arrayType))
                val typeInfoName = rawArrayTypeInfoName(arrayType, sourceId)
                val nameGlobal = "$typeInfoName.name"
                val reflectedTypeInfoName = rawArrayTypeInfoName(arrayType, sourceId, forNameFieldOfTypeInfo = true)
                val bytes = (reflectedTypeInfoName.toByteArray(Charsets.UTF_8) + byteArrayOf(0))
                val initializer = bytes.joinToString(separator = "") { byte ->
                    val unsigned = byte.toInt() and 0xff
                    if (unsigned in 0x20..0x7e && unsigned != '"'.code && unsigned != '\\'.code) {
                        unsigned.toChar().toString()
                    } else {
                        "\\${unsigned.toString(16).uppercase().padStart(2, '0')}"
                    }
                }
                add("${global(nameGlobal)} = private global [${bytes.size} x i8] c\"$initializer\", align 1")
                if (containsGeneric(arrayType.elementType)) return@forEach
                add(
                    "${global("$typeInfoName.ti")} = internal global %TypeInfo { " +
                        "ptr getelementptr inbounds ([${bytes.size} x i8], ptr ${global(nameGlobal)}, i64 0, i64 0), " +
                        "i8 -126, i8 0, i16 0, i32 0, ptr null, i32 0, i8 0, i8 0, i16 -32768, " +
                        "ptr null, ptr @RawArray.tt, ptr null, ptr null, ptr ${global(typeInfoName(arrayType.elementType, sourceId) + ".ti")}, " +
                        "ptr null, ptr null, ptr null }",
                )
            }
        }
        val needsTypeInfoInstantiation = hasDynamicTemplateInstantiation
        val runtimeFunctionAbis = buildList {
            if (hasRawArrayMetadata) {
                add(LlvmFunctionAbi("MCC_NewObject", "ptr", listOf("ptr", "i32")))
                add(LlvmFunctionAbi("MCC_ThrowException", "void", listOf("ptr")))
                add(LlvmFunctionAbi(NEGATIVE_ARRAY_SIZE_CONSTRUCTOR, "void", listOf("ptr", "ptr")))
            }
            if (needsTypeInfoInstantiation) {
                add(LlvmFunctionAbi(GET_OR_CREATE_TYPE_INFO, "ptr", listOf("ptr", "i32", "ptr")))
            }
            allocatedTypes.map { (type, sourceId) -> allocationFunction(type, sourceId) }
                .distinct()
                .sorted()
                .forEach { function -> add(LlvmFunctionAbi(function, "ptr", listOf("ptr", "i64"))) }
        }
        val runtimeDeclarations = runtimeFunctionAbis.map { function ->
            "declare ${function.returnType} @${llvmIdentifier(function.symbol)}(${function.argumentTypes.joinToString(", ")})"
        }
        return LlvmRawArrayRuntimePlan(typeDeclarations, globalDeclarations, runtimeDeclarations, runtimeFunctionAbis)
    }

    private fun typeInfoName(
        type: ChirType,
        sourceId: ChirSemanticId,
        forNameFieldOfTypeInfo: Boolean = false,
    ): String = when (type) {
        is ChirPrimitiveType -> primitiveTypeInfoName(type)
        is ChirRefType -> typeInfoDisplayName(type.referencedType, sourceId, forNameFieldOfTypeInfo)
        is ChirRawArrayType -> rawArrayTypeInfoName(type, sourceId, forNameFieldOfTypeInfo)
        is ChirNamedType -> type.name
        is ChirTupleType -> "Tuple<${type.elementTypes.joinToString(",") { typeInfoDisplayName(it, sourceId, forNameFieldOfTypeInfo) }}>"
        is ChirFunctionType -> "(${type.parameterTypes.joinToString(",") { typeInfoDisplayName(it, sourceId, forNameFieldOfTypeInfo) }})->${typeInfoDisplayName(type.returnType, sourceId, forNameFieldOfTypeInfo)}"
        is ChirStructType -> nominalTypeInfoName(type.declarationId.packageName, type.declarationId.declarationName, type.typeArguments, sourceId, forNameFieldOfTypeInfo)
        is ChirClassType -> nominalTypeInfoName(type.declarationId.packageName, type.declarationId.declarationName, type.typeArguments, sourceId, forNameFieldOfTypeInfo)
        is ChirEnumType -> nominalTypeInfoName(type.declarationId.packageName, type.declarationId.declarationName, type.typeArguments, sourceId, forNameFieldOfTypeInfo)
        is ChirVArrayType -> "VArray<${typeInfoDisplayName(type.elementType, sourceId, forNameFieldOfTypeInfo)},${type.size}>"
        is ChirCPointerType -> "CPointer<${typeInfoDisplayName(type.pointeeType, sourceId, forNameFieldOfTypeInfo)}>"
        is ChirCStringType -> "CString"
        is ChirGenericType -> type.identifier
        is ChirBoxType -> {
            val boxedTypeName = typeInfoDisplayName(type.boxedType, sourceId, forNameFieldOfTypeInfo)
            if (forNameFieldOfTypeInfo) boxedTypeName else "Box<$boxedTypeName>"
        }
        is ChirThisType -> type.ownerTypeName
    }

    private fun typeInfoDisplayName(
        type: ChirTypeRef,
        sourceId: ChirSemanticId,
        forNameFieldOfTypeInfo: Boolean = false,
    ): String = typeInfoName(
        type = (type as? ChirResolvedTypeRef)?.type ?: throw CodegenLoweringException(
            "RawArray component type '${type.renderName}' is unresolved",
            sourceId,
        ),
        sourceId = sourceId,
        forNameFieldOfTypeInfo = forNameFieldOfTypeInfo,
    )

    private fun nominalTypeInfoName(
        packageName: String,
        declarationName: String,
        typeArguments: List<ChirTypeRef>,
        sourceId: ChirSemanticId,
        forNameFieldOfTypeInfo: Boolean,
    ): String = buildString {
        if (packageName.isNotBlank()) {
            append(packageName)
            append(':')
        }
        append(declarationName)
        if (typeArguments.isNotEmpty()) {
            append(
                typeArguments.joinToString(prefix = "<", postfix = ">") {
                    typeInfoDisplayName(it, sourceId, forNameFieldOfTypeInfo)
                },
            )
        }
    }

    internal fun primitiveTypeInfoName(type: ChirPrimitiveType): String = when (type) {
        ChirPrimitiveType.UNIT -> "Unit"
        ChirPrimitiveType.BOOL -> "Bool"
        ChirPrimitiveType.INT8 -> "Int8"
        ChirPrimitiveType.INT16 -> "Int16"
        ChirPrimitiveType.INT32 -> "Int32"
        ChirPrimitiveType.INT64 -> "Int64"
        ChirPrimitiveType.INT_NATIVE -> "IntNative"
        ChirPrimitiveType.UINT8 -> "UInt8"
        ChirPrimitiveType.UINT16 -> "UInt16"
        ChirPrimitiveType.UINT32 -> "UInt32"
        ChirPrimitiveType.UINT64 -> "UInt64"
        ChirPrimitiveType.UINT_NATIVE -> "UIntNative"
        ChirPrimitiveType.FLOAT16 -> "Float16"
        ChirPrimitiveType.FLOAT32 -> "Float32"
        ChirPrimitiveType.FLOAT64 -> "Float64"
        ChirPrimitiveType.RUNE -> "Rune"
        ChirPrimitiveType.NOTHING -> "Nothing"
        ChirPrimitiveType.VOID -> "Unit"
    }

    private fun typeInfoNameKey(type: ChirRawArrayType): String = type.renderName

    fun typeInfoSymbol(type: ChirTypeRef, sourceId: ChirSemanticId): String = "${typeInfoName(type, sourceId)}.ti"

    fun requiresExternalTypeInfo(typeRef: ChirTypeRef): Boolean {
        val type = (typeRef as? ChirResolvedTypeRef)?.type ?: return false
        if (containsGeneric(typeRef)) return false
        return when (type) {
            is ChirThisType, is ChirGenericType, is ChirRawArrayType -> false
            is ChirRefType -> (type.referencedType as? ChirResolvedTypeRef)?.type !is ChirRawArrayType
            else -> true
        }
    }

    fun containsGeneric(typeRef: ChirTypeRef): Boolean {
        val type = (typeRef as? ChirResolvedTypeRef)?.type ?: return false
        return when (type) {
            is ChirGenericType -> true
            is ChirRawArrayType -> containsGeneric(type.elementType)
            is ChirRefType -> containsGeneric(type.referencedType)
            is ChirTupleType -> type.elementTypes.any(::containsGeneric)
            is ChirFunctionType -> type.parameterTypes.any(::containsGeneric) || containsGeneric(type.returnType)
            is ChirStructType -> type.typeArguments.any(::containsGeneric) || type.fieldTypes.any(::containsGeneric)
            is ChirClassType -> type.typeArguments.any(::containsGeneric) || type.fieldTypes.any(::containsGeneric)
            is ChirEnumType -> type.typeArguments.any(::containsGeneric) || type.cases.any { case -> case.payloadTypes.any(::containsGeneric) }
            is ChirVArrayType -> containsGeneric(type.elementType)
            is ChirCPointerType -> containsGeneric(type.pointeeType)
            is ChirBoxType -> containsGeneric(type.boxedType)
            is ChirNamedType -> type.typeArguments.any(::containsGeneric)
            is ChirCStringType, is ChirPrimitiveType, is ChirThisType -> false
        }
    }

    private fun escapeLlvmIdentifier(name: String): String = name
        .replace("\\", "\\5C")
        .replace("\"", "\\22")
}
