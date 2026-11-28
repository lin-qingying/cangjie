package org.cangnova.cangjie.codegen.types

import org.cangnova.cangjie.chir.core.type.ChirBoxType
import org.cangnova.cangjie.chir.core.type.ChirCPointerType
import org.cangnova.cangjie.chir.core.type.ChirClassType
import org.cangnova.cangjie.chir.core.type.ChirEnumType
import org.cangnova.cangjie.chir.core.type.ChirFunctionType
import org.cangnova.cangjie.chir.core.type.ChirRawArrayType
import org.cangnova.cangjie.chir.core.type.ChirRefType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.chir.core.type.ChirStructType
import org.cangnova.cangjie.chir.core.type.ChirTupleType
import org.cangnova.cangjie.chir.core.type.ChirTypeRef
import org.cangnova.cangjie.chir.core.type.ChirVArrayType

/**
 * 动态泛型类型对应的官方运行时模板及其有序实参。
 */
internal data class LlvmTypeInfoTemplateReference(
    /** 官方 TypeTemplate 全局符号名。 */
    val symbolName: String,
    /** 显式类型实参，顺序遵循官方 CHIR ABI。 */
    val typeArguments: List<ChirTypeRef>,
    /** 官方 ABI 按固定长度重复传入的类型实参，例如 VArray 的元素类型。 */
    val repeatedTypeArgument: ChirTypeRef? = null,
    /** 重复类型实参的次数。 */
    val repeatedTypeArgumentCount: Long = 0,
) {
    init {
        require(repeatedTypeArgumentCount >= 0) { "repeated type argument count must not be negative" }
        require(repeatedTypeArgument != null || repeatedTypeArgumentCount == 0L) {
            "a repeated type argument is required when its count is non-zero"
        }
    }

    /** 按运行时 ABI 顺序遍历完整实参序列。 */
    fun forEachTypeArgument(action: (ChirTypeRef) -> Unit) {
        typeArguments.forEach(action)
        val repeatedTypeArgument = repeatedTypeArgument ?: return
        var index = 0L
        while (index < repeatedTypeArgumentCount) {
            action(repeatedTypeArgument)
            index++
        }
    }

    /** 遍历构造模板声明所需的实参类型依赖；重复类型只需登记一次。 */
    fun forEachDistinctTypeArgument(action: (ChirTypeRef) -> Unit) {
        typeArguments.forEach(action)
        repeatedTypeArgument?.let(action)
    }

    val typeArgumentCount: Long
        get() = typeArguments.size.toLong() + repeatedTypeArgumentCount
}

/**
 * 仓颉运行时 TypeInfo 模板身份与泛型实参顺序。
 *
 * 该映射集中表达官方 `CGType::GetNameOfTypeTemplateGV` 与各类型 `GenTypeArgsOfTypeInfo` 的规则；
 * 函数与 RawArray lowering、模块级外部符号计划共用同一份定义。
 */
internal object LlvmRuntimeTypeInfo {
    /**
     * 返回复合类型对应的 TypeTemplate 及按官方 ABI 排列的类型实参。
     *
     * 非模板类型返回 `null`；引用类型先解引用，因为仓颉运行时 TypeInfo 描述其 referent。
     */
    fun templateReference(typeRef: ChirTypeRef): LlvmTypeInfoTemplateReference? {
        val type = (typeRef as? ChirResolvedTypeRef)?.type ?: return null
        if (type is ChirRefType) return templateReference(type.referencedType)
        if (type is ChirVArrayType) {
            return LlvmTypeInfoTemplateReference(
                symbolName = "VArray.tt",
                typeArguments = emptyList(),
                repeatedTypeArgument = type.elementType,
                repeatedTypeArgumentCount = type.size,
            )
        }

        val (symbolName, typeArguments) = when (type) {
            is ChirRawArrayType -> "RawArray.tt" to listOf(type.elementType)
            is ChirTupleType -> "Tuple.tt" to type.elementTypes
            is ChirFunctionType -> "CFunc.tt" to (listOf(type.returnType) + type.parameterTypes)
            is ChirStructType -> typeTemplateSymbol(type.declarationId.packageName, type.declarationId.declarationName) to type.typeArguments
            is ChirClassType -> typeTemplateSymbol(type.declarationId.packageName, type.declarationId.declarationName) to type.typeArguments
            is ChirEnumType -> typeTemplateSymbol(type.declarationId.packageName, type.declarationId.declarationName) to type.typeArguments
            is ChirCPointerType -> "CPointer.tt" to listOf(type.pointeeType)
            is ChirBoxType -> "Box.tt" to listOf(type.boxedType)
            else -> return null
        }
        return LlvmTypeInfoTemplateReference(symbolName, typeArguments)
    }

    /**
     * 返回需要运行时实例化的泛型复合类型模板。
     */
    fun dynamicTemplateReference(typeRef: ChirTypeRef): LlvmTypeInfoTemplateReference? {
        val template = templateReference(typeRef) ?: return null
        return template.takeIf { reference ->
            reference.typeArguments.any(LlvmRawArrayRuntime::containsGeneric) ||
                reference.repeatedTypeArgument?.let(LlvmRawArrayRuntime::containsGeneric) == true
        }
    }

    private fun typeTemplateSymbol(packageName: String, declarationName: String): String =
        "$packageName:$declarationName.tt"
}
