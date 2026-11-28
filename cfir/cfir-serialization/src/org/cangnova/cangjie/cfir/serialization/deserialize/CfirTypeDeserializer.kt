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
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagWithFixedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.cfir.types.impl.CfirResolvedTypeRefImpl

/** 以共享 Cone 诊断保留无法恢复类型的实际原因。 */
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
    context: CfirDeserializationContext,
) {
    private val context = context.canonical()

    /** 非 Generic 结构类型不允许拥有递归构造边；Generic 的回边由真实 symbol 壳承接。 */
    private val typesUnderDeserialization = HashSet<Int>()

    /**
     * 从 `allTypes` 中反序列化单个类型索引。
     *
     * 该入口负责缓存复用与包事务；合法 Generic 回边先登记身份，结构循环明确拒绝。
     */
    fun deserializeType(typeIndex: Int): ConeCangJieType {
        if (context.currentSourceSelection == null) {
            val selection = context.metadataDemand(CjoSourceContentRoot.TypeMetadata((typeIndex + 1).toUInt()))
            context.typeCache[typeIndex]?.let { value ->
                check((typeIndex + 1).toUInt() in selection.types)
                check(selection.borrowedContexts.declarations.keys.all { it.toInt() - 1 in context.declCache })
                return value
            }
            return context.materialize {
                context.registerSourceSelection(selection)
                context.withSourceSelection(selection) { deserializeType(typeIndex) }
            }
        }
        context.requireSourceTypeIdentity(typeIndex)
        context.typeCache[typeIndex]?.let { return it }
        return context.materialize {
            context.types[typeIndex]?.let { return@materialize it }
            val semaTy = context.pkg.allTypes(typeIndex)
                ?: return@materialize errorType("Cannot read type index $typeIndex")
            if (semaTy.kind == TypeKind.Generic) return@materialize convertGenericType(semaTy, typeIndex)
            check(typesUnderDeserialization.add(typeIndex)) { "CJO structural type cycle at index $typeIndex" }

            try {
                val result = convertSemaTy(semaTy)
                context.types.putIfAbsent(typeIndex, result)
                context.types[typeIndex] ?: result
            } finally {
                typesUnderDeserialization.remove(typeIndex)
            }
        }
    }

    /**
     * 从 `.cjo` 中 1-based 类型字段反序列化类型。
     *
     * 官方二进制格式用 `0` 表示 Invalid，不授予 Unit；非零值减一后进入 `allTypes`。
     */
    fun deserializeTypeFromField(typeFieldValue: UInt): ConeCangJieType {
        if (typeFieldValue == 0u) return errorType("Invalid CJO type reference")
        return deserializeType((typeFieldValue - 1u).toInt())
    }

    /** 按 [SemaTy.kind] 分派具体类型反序列化逻辑。 */
    private fun convertSemaTy(semaTy: SemaTy): ConeCangJieType {
        return when (semaTy.kind) {
            // 值/签名类型中的 Invalid 保留既有错误类型合同；父匹配元信息由 wire kind 独立识别。
            TypeKind.Invalid -> errorType("Explicit Invalid CJO type")
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

            TypeKind.Generic -> error("Generic type must enter the identity reservation path")
            TypeKind.Type -> convertAliasType(semaTy)
            else -> errorType("Unknown TypeKind: ${semaTy.kind}")
        }
    }

    /** TypeAliasTy 保留 Alias classifier 和应用实参，展开 RHS 仍由真实 Alias 声明持有。 */
    private fun convertAliasType(semaTy: SemaTy): ConeCangJieType {
        val info = checkNotNull(semaTy.info(CompositeTyInfo()) as? CompositeTyInfo)
        val fullId = checkNotNull(info.declPtr)
        val resolved = context.fullIdResolver.resolve(fullId) as? ResolvedFullId.Declaration
            ?: error("Alias type has invalid FullId: ${context.fullIdResolver.describe(fullId)}")
        check(resolved.declaration.decl.kind == DeclKind.TypeAliasDecl)
        val symbol = nominalSymbol(fullId, DeclKind.TypeAliasDecl)
        return ConeClassLikeType(ConeClassLikeLookupTagWithFixedSymbol(symbol.classId, symbol), deserializeTypeArgs(semaTy))
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
        val symbol = nominalSymbol(fullId, if (isInterface) DeclKind.InterfaceDecl else DeclKind.ClassDecl)
        return ConeClassLikeType(
            lookupTag = symbol.toLookupTag(),
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
        val symbol = nominalSymbol(fullId, DeclKind.StructDecl)
        return ConeStructType(
            lookupTag = symbol.toLookupTag(),
            typeArguments = deserializeTypeArgs(semaTy),
        )
    }

    /** 反序列化 enum 类型。 */
    private fun convertEnumType(semaTy: SemaTy): ConeCangJieType {
        val info = semaTy.info(CompositeTyInfo()) as? CompositeTyInfo
            ?: return errorType("Enum missing CompositeTyInfo")
        val fullId = info.declPtr
            ?: return errorType("CompositeTyInfo missing declPtr")
        val symbol = nominalSymbol(fullId, DeclKind.EnumDecl)
        return ConeEnumType(
            lookupTag = symbol.toLookupTag(),
            typeArguments = deserializeTypeArgs(semaTy),
        )
    }

    /** 原 wire kind 与 FullId 所选声明 kind 必须一致，不能以另一种 Cone 包装相同名称的 symbol。 */
    private fun nominalSymbol(reference: FullId, expectedKind: UShort): CfirClassLikeSymbol<*> {
        val target = context.fullIdResolver.resolve(reference) as? ResolvedFullId.Declaration
            ?: error("Nominal type has no actual classifier declaration")
        check(target.declaration.decl.kind == expectedKind) { "Nominal type kind disagrees with its actual FullId declaration kind" }
        return context.materializeClassLikeSymbol(reference)
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

    /** 反序列化官方内部 `RawArray<T>` 类型；用户可见的 `Array<T>` 是独立的 struct 声明。 */
    private fun convertArrayType(semaTy: SemaTy): ConeCangJieType {
        val elementProjection = deserializeTypeArgs(semaTy).singleOrNull()
            ?: errorType("Array missing element type")
        val info = checkNotNull(semaTy.info(ArrayTyInfo()) as? ArrayTyInfo) { "Array missing native dimension info" }
        return ConeClassLikeType(
            lookupTag = ConeClassLikeLookupTagImpl(StdlibClassIds.RawArray),
            typeArguments = listOf(elementProjection),
            rawArrayDimensions = info.dimsOrSize.toUInt(),
        )
    }

    /** 反序列化定长 VArray 类型，保留元素类型与尺寸。 */
    private fun convertVArrayType(semaTy: SemaTy): ConeCangJieType {
        val elementType = deserializeTypeArgs(semaTy).singleOrNull()?.type
            ?: errorType("VArray missing element type")
        val info = checkNotNull(semaTy.info(ArrayTyInfo()) as? ArrayTyInfo) { "VArray missing native size info" }
        val size = info.dimsOrSize
        return ConeVArrayType(elementType = elementType, size = size)
    }

    /** C pointer 必须拥有唯一 pointee 槽，不以 Unit 代替损坏输入。 */
    private fun convertPointerType(semaTy: SemaTy): ConeCangJieType {
        val pointeeType = checkNotNull(deserializeTypeArgs(semaTy).singleOrNull()?.type)
        return ConePointerType(pointeeType = pointeeType)
    }

    /**
     * 反序列化泛型类型参数引用。
     *
     * 当前及跨包参数均复用真实 owner 的 symbol；GenericTyInfo 的最终上界独立于原始约束分组。
     */
    private fun convertGenericType(semaTy: SemaTy, typeIndex: Int, completeBounds: Boolean = false): ConeCangJieType {
        val info = checkNotNull(semaTy.info(GenericTyInfo()) as? GenericTyInfo)
        val parameter = context.materializeTypeParameter(checkNotNull(info.declPtr))
        val symbol = parameter.symbol
        val type = context.genericTypes.getOrPut(symbol) { ConeTypeParameterTypeImpl(symbol.toLookupTag()) }
        // 与 native SetGenericTy 同序：先缓存真实参数类型，再读取可回指本类型的上界。
        context.types[typeIndex] = type
        if (!completeBounds) {
            context.enqueueGenericType(typeIndex)
            return type
        }
        if (!context.genericBoundsInProgress.add(symbol)) return type
        try {
            val bounds = (0 until info.upperBoundsLength).map { ordinal ->
                CfirResolvedTypeRefImpl(
                    source = null,
                    annotations = MutableOrEmptyList.empty(),
                    customRenderer = false,
                    typeUseOrigin = CfirTypeRefOrigin.COMPILER_ADDED,
                    coneType = deserializeTypeFromField(info.upperBounds(ordinal)),
                    delegatedTypeRef = null,
                )
            }
            val isReserved = context.reservedTypeParameters.values.any { it === parameter }
            val previous = context.genericBounds[symbol] ?: if (isReserved) null else parameter.bounds
            check(previous == null || previous.map { it.coneTypeOrNull } == bounds.map { it.coneType }) {
                "Conflicting semantic bounds for one CJO generic parameter"
            }
            context.genericBounds[symbol] = java.util.Collections.unmodifiableList(bounds)
            if (isReserved) parameter.replaceBounds(bounds)
            context.finishGenericType(typeIndex)
        } catch (failure: Throwable) {
            context.types.remove(typeIndex, type)
            throw failure
        } finally {
            context.genericBoundsInProgress.remove(symbol)
        }
        return type
    }

    /** Generic 缓存只承载身份；bounds 完成必须显式执行，不能被 staging type cache 命中跳过。 */
    internal fun completeGenericType(typeIndex: Int): ConeCangJieType = context.materialize {
        context.requireSourceTypeIdentity(typeIndex)
        val wire = checkNotNull(context.pkg.allTypes(typeIndex))
        check(wire.kind == TypeKind.Generic)
        convertGenericType(wire, typeIndex, completeBounds = true)
    }

}
