/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cfir.serialization.deserialize

import PackageFormat.*
import org.cangnova.cangjie.cfir.CfirImplementationDetail
import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.impl.CfirTypeParameterImpl
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.impl.CfirResolvedTypeRefImpl
import org.cangnova.cangjie.name.Name

/** 构造最小 Cone 诊断对象，用于反序列化无法恢复类型时保留错误原因。 */
private fun simpleDiagnostic(reason: String): ConeDiagnostic = object : ConeDiagnostic {
    override val reason: String = reason
}

/** 构造错误类型，并可选择保留被代理的原始类型。 */
private fun errorType(reason: String, delegatedType: ConeCangJieType? = null): ConeErrorType =
    ConeErrorType(simpleDiagnostic(reason), delegatedType = delegatedType)

/**
 * 将 FlatBuffers 中的 [SemaTy] 反序列化为 CFIR Cone 类型。
 *
 * 类型反序列化必须复用当前包中已经 materialized 的类型参数 symbol，
 * 否则 callable 签名中的泛型参数会和声明列表中的类型参数失去身份一致性。
 */
@OptIn(CfirImplementationDetail::class)
class CfirTypeDeserializer(
    /** 当前 `.cjo` 包的反序列化上下文与共享缓存。 */
    private val context: CfirDeserializationContext,
) {
    /** 当前调用栈正在反序列化的 `allTypes` 索引集合，用于检测递归类型引用。 */
    private val typesUnderDeserialization = HashSet<Int>()

    /**
     * 从 `allTypes` 中反序列化单个类型索引。
     *
     * 该入口负责缓存复用、并发锁定与递归引用降级；真正的类型 kind 分派在 [convertSemaTy] 中完成。
     */
    fun deserializeType(typeIndex: Int): ConeCangJieType {
        context.typeCache[typeIndex]?.let { return it }
        val lock = context.typeMaterializationLock(typeIndex)
        synchronized(lock) {
            context.typeCache[typeIndex]?.let { return it }
            if (!typesUnderDeserialization.add(typeIndex)) {
                return createRecursiveTypeFallback(typeIndex)
            }

            val semaTy = context.pkg.allTypes(typeIndex)
                ?: return errorType("Cannot read type index $typeIndex")

            return try {
                val result = convertSemaTy(semaTy)
                context.typeCache.putIfAbsent(typeIndex, result)
                context.typeCache[typeIndex] ?: result
            } finally {
                typesUnderDeserialization.remove(typeIndex)
                context.releaseTypeMaterializationLock(typeIndex, lock)
            }
        }
    }

    /**
     * 从 `.cjo` 中 1-based 类型字段反序列化类型。
     *
     * 官方二进制格式用 `0` 表示 Unit/缺省类型，非零值需要减一后进入 `allTypes`。
     */
    fun deserializeTypeFromField(typeFieldValue: UInt): ConeCangJieType {
        if (typeFieldValue == 0u) return ConePrimitiveType.UNIT
        return deserializeType((typeFieldValue - 1u).toInt())
    }

    /** 按 [SemaTy.kind] 分派具体类型反序列化逻辑。 */
    private fun convertSemaTy(semaTy: SemaTy): ConeCangJieType {
        return when (semaTy.kind) {
            TypeKind.Unit -> ConePrimitiveType.UNIT
            TypeKind.Int8 -> ConePrimitiveType.INT8
            TypeKind.Int16 -> ConePrimitiveType.INT16
            TypeKind.Int32 -> ConePrimitiveType.INT32
            TypeKind.Int64 -> ConePrimitiveType.INT64
            TypeKind.IntNative -> ConePrimitiveType.INT_NATIVE
            TypeKind.UInt8 -> ConePrimitiveType.UINT8
            TypeKind.UInt16 -> ConePrimitiveType.UINT16
            TypeKind.UInt32 -> ConePrimitiveType.UINT32
            TypeKind.UInt64 -> ConePrimitiveType.UINT64
            TypeKind.UIntNative -> ConePrimitiveType.UINT_NATIVE
            TypeKind.Float16 -> ConePrimitiveType.FLOAT16
            TypeKind.Float32 -> ConePrimitiveType.FLOAT32
            TypeKind.Float64 -> ConePrimitiveType.FLOAT64
            TypeKind.Rune -> ConePrimitiveType.RUNE
            TypeKind.Nothing -> ConePrimitiveType.NOTHING
            TypeKind.Bool -> ConePrimitiveType.BOOLEAN

            TypeKind.Class -> convertClassType(semaTy, isInterface = false)
            TypeKind.Interface -> convertClassType(semaTy, isInterface = true)
            TypeKind.Struct -> convertStructType(semaTy)
            TypeKind.Enum -> convertEnumType(semaTy)

            TypeKind.Func -> convertFuncType(semaTy)

            TypeKind.Tuple -> convertTupleType(semaTy)
            TypeKind.Array -> convertArrayType(semaTy)
            TypeKind.VArray -> convertVArrayType(semaTy)

            TypeKind.CPointer -> convertPointerType(semaTy)
            TypeKind.CString -> ConeCStringType()

            TypeKind.Generic -> convertGenericType(semaTy)

            TypeKind.Type -> errorType("Unsupported type kind: Type")
            else -> errorType("Unknown TypeKind: ${semaTy.kind}")
        }
    }

    /** 反序列化 [SemaTy] 携带的类型实参数组。 */
    private fun deserializeTypeArgs(semaTy: SemaTy): List<ConeTypeProjection> {
        val len = semaTy.typeArgsLength
        if (len == 0) return emptyList()
        return (0 until len).map { index ->
            deserializeTypeFromField(semaTy.typeArgs(index))
        }
    }

    /** 反序列化 class/interface 类型，并通过 [FullId] 解析其 [org.cangnova.cangjie.name.ClassId]。 */
    private fun convertClassType(semaTy: SemaTy, isInterface: Boolean): ConeCangJieType {
        val info = semaTy.info(CompositeTyInfo()) as? CompositeTyInfo
            ?: return errorType("Class/Interface missing CompositeTyInfo")
        val fullId = info.declPtr
            ?: return errorType("CompositeTyInfo missing declPtr")
        val classId = context.fullIdResolver.resolveClassId(fullId)
            ?: return errorType("Cannot resolve class FullId: ${context.fullIdResolver.describe(fullId)}")
        return ConeClassLikeType(
            lookupTag = ConeClassLikeLookupTagImpl(classId),
            typeArguments = deserializeTypeArgs(semaTy),
            isInterface = isInterface,
            isThisType = info.isThisTy,
        )
    }

    /** 反序列化 struct 类型。 */
    private fun convertStructType(semaTy: SemaTy): ConeCangJieType {
        val info = semaTy.info(CompositeTyInfo()) as? CompositeTyInfo
            ?: return errorType("Struct missing CompositeTyInfo")
        val fullId = info.declPtr
            ?: return errorType("CompositeTyInfo missing declPtr")
        val classId = context.fullIdResolver.resolveClassId(fullId)
            ?: return errorType("Cannot resolve struct FullId: ${context.fullIdResolver.describe(fullId)}")
        return ConeStructType(
            lookupTag = ConeClassLikeLookupTagImpl(classId),
            typeArguments = deserializeTypeArgs(semaTy),
        )
    }

    /** 反序列化 enum 类型。 */
    private fun convertEnumType(semaTy: SemaTy): ConeCangJieType {
        val info = semaTy.info(CompositeTyInfo()) as? CompositeTyInfo
            ?: return errorType("Enum missing CompositeTyInfo")
        val fullId = info.declPtr
            ?: return errorType("CompositeTyInfo missing declPtr")
        val classId = context.fullIdResolver.resolveClassId(fullId)
            ?: return errorType("Cannot resolve enum FullId: ${context.fullIdResolver.describe(fullId)}")
        return ConeEnumType(
            lookupTag = ConeClassLikeLookupTagImpl(classId),
            typeArguments = deserializeTypeArgs(semaTy),
        )
    }

    /** 反序列化函数类型，保留 C 函数与变长参数标志。 */
    private fun convertFuncType(semaTy: SemaTy): ConeCangJieType {
        val info = semaTy.info(FuncTyInfo()) as? FuncTyInfo
            ?: return errorType("Func missing FuncTyInfo")
        val paramTypes = deserializeTypeArgs(semaTy).map { it.type }
        val returnType = deserializeTypeFromField(info.retType)
        return ConeFunctionType(
            parameterTypes = paramTypes,
            returnType = returnType,
            isCFunc = info.isC,
            hasVariableLenArg = info.hasVariableLenArg,
        )
    }

    /** 反序列化 tuple 类型。 */
    private fun convertTupleType(semaTy: SemaTy): ConeCangJieType {
        return ConeTupleType(elementTypes = deserializeTypeArgs(semaTy).map { it.type })
    }

    /** 反序列化 Array 类型，并映射到标准库 `Array` class-like 类型。 */
    private fun convertArrayType(semaTy: SemaTy): ConeCangJieType {
        val elementProjection = deserializeTypeArgs(semaTy).singleOrNull()
            ?: errorType("Array missing element type")
        return ConeStructType(
            lookupTag = ConeClassLikeLookupTagImpl(StdlibClassIds.Array),
            typeArguments = listOf(elementProjection),
        )
    }

    /** 反序列化定长 VArray 类型，保留元素类型与尺寸。 */
    private fun convertVArrayType(semaTy: SemaTy): ConeCangJieType {
        val elementType = deserializeTypeArgs(semaTy).singleOrNull()?.type
            ?: errorType("VArray missing element type")
        val info = semaTy.info(ArrayTyInfo()) as? ArrayTyInfo
        val size = info?.dimsOrSize ?: 0L
        return ConeVArrayType(elementType = elementType, size = size)
    }

    /** 反序列化 C pointer 类型，缺失 pointee 时使用 Unit 维持错误恢复。 */
    private fun convertPointerType(semaTy: SemaTy): ConeCangJieType {
        val pointeeType = deserializeTypeArgs(semaTy).singleOrNull()?.type ?: ConePrimitiveType.UNIT
        return ConePointerType(pointeeType = pointeeType)
    }

    /**
     * 反序列化泛型类型参数引用。
     *
     * 当前包类型参数必须复用声明缓存中的 symbol；跨包或尚未 materialized 的类型参数才创建 synthetic symbol。
     */
    private fun convertGenericType(semaTy: SemaTy): ConeCangJieType {
        val info = semaTy.info(GenericTyInfo()) as? GenericTyInfo
            ?: return errorType("Generic missing GenericTyInfo")
        val fullId = info.declPtr
            ?: return errorType("GenericTyInfo missing declPtr")
        resolveCurrentPackageTypeParameter(fullId)?.let { symbol ->
            return ConeTypeParameterTypeImpl(symbol.toLookupTag())
        }
        val name = context.fullIdResolver.resolveDeclarationName(fullId)
            ?: return errorType("Cannot resolve generic parameter FullId: ${context.fullIdResolver.describe(fullId)}")
        val upperBounds = (0 until info.upperBoundsLength).map {
            deserializeTypeFromField(info.upperBounds(it))
        }
        return ConeTypeParameterTypeImpl(createSyntheticTypeParameterSymbol(name, upperBounds).toLookupTag())
    }

    /**
     * CJO `GenericTyInfo.declPtr` 指向真实的 `GenericParamDecl`。
     * 当声明反序列化已物化该类型参数时，类型引用必须复用同一个 symbol，
     * 否则函数/enum constructor 签名中的 `T` 无法被调用候选的 fresh substitutor 命中。
     */
    private fun resolveMaterializedTypeParameterSymbol(fullId: FullId): CfirTypeParameterSymbol? {
        val resolved = context.fullIdResolver.resolve(fullId) as? ResolvedFullId.Declaration ?: return null
        if (resolved.source != ResolvedFullId.Declaration.Source.CURRENT_PACKAGE) return null
        val declaration = context.declCache[resolved.declaration.zeroBasedIndex] as? CfirTypeParameter
            ?: return null
        return declaration.symbol
    }

    /**
     * 为递归类型引用创建可继续传播的 fallback 类型。
     *
     * 泛型参数递归优先恢复类型参数 symbol，其他递归形态退化为带原因的错误类型。
     */
    private fun createRecursiveTypeFallback(typeIndex: Int): ConeCangJieType {
        val semaTy = context.pkg.allTypes(typeIndex)
            ?: return errorType("Recursive type reference: $typeIndex")
        if (semaTy.kind == TypeKind.Generic) {
            val info = semaTy.info(GenericTyInfo()) as? GenericTyInfo
            val fullId = info?.declPtr
            val symbol = fullId?.let(::resolveCurrentPackageTypeParameter)
            if (symbol != null) {
                return ConeTypeParameterTypeImpl(symbol.toLookupTag())
            }
            if (fullId != null) {
                val name = context.fullIdResolver.resolveDeclarationName(fullId)
                    ?: return errorType("Cannot resolve generic parameter FullId: ${context.fullIdResolver.describe(fullId)}")
                return ConeTypeParameterTypeImpl(createSyntheticTypeParameterSymbol(name, emptyList()).toLookupTag())
            }
        }
        return errorType("Recursive type reference: $typeIndex")
    }

    /**
     * 对齐 Kotlin FIR 反序列化：GenericTy 必须回指声明列表中的类型参数符号。
     *
     * fresh type variable substitutor 以声明符号为身份；如果这里临时创建同名
     * synthetic symbol，`println<T>(value: T)` 这类库函数的形参类型就无法替换成
     * 调用候选的新鲜类型变量。
     */
    private fun resolveCurrentPackageTypeParameter(fullId: FullId): org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol? {
        val resolved = context.fullIdResolver.resolve(fullId) as? ResolvedFullId.Declaration ?: return null
        if (resolved.source != ResolvedFullId.Declaration.Source.CURRENT_PACKAGE) return null
        val declaration = context.declCache[resolved.declaration.zeroBasedIndex] as? CfirTypeParameter ?: return null
        return declaration.symbol
    }

    /**
     * 为无法直接复用声明缓存的泛型参数创建 synthetic symbol。
     *
     * 该 symbol 会立即绑定一个最小 [CfirTypeParameter] 声明，使 Cone lookup tag 可被后续类型系统使用。
     */
    private fun createSyntheticTypeParameterSymbol(
        name: Name,
        upperBounds: List<ConeCangJieType>,
    ): CfirTypeParameterSymbol {
        val symbol = CfirTypeParameterSymbol()
        val boundRefs = upperBounds.mapTo(mutableListOf<CfirTypeRef>()) { upperBound ->
            CfirResolvedTypeRefImpl(
                source = null,
                annotations = MutableOrEmptyList.empty(),
                customRenderer = false,
                coneType = upperBound,
                delegatedTypeRef = null,
            )
        }
        val declaration = CfirTypeParameterImpl(
            source = null,
            moduleData = context.moduleData,
            resolvePhase = CfirResolvePhase.BODY_RESOLVE,
            annotations = MutableOrEmptyList.empty(),
            origin = CfirDeclarationOrigin.Library,
            attributes = CfirDeclarationAttributes.EMPTY,
            containingDeclarationSymbol = symbol,
            symbol = symbol,
            name = name,
            bounds = boundRefs,
        )
        symbol.bind(declaration)
        return symbol
    }
}
