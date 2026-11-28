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
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.symbols.CfirTypeParameterSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.types.*

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
    context: CfirDeserializationContext,
) {
    private val context = context.canonical()

    /** 当前调用栈正在反序列化的 `allTypes` 索引集合，用于检测递归类型引用。 */
    private val typesUnderDeserialization = HashSet<Int>()

    /**
     * 从 `allTypes` 中反序列化单个类型索引。
     *
     * 该入口负责缓存复用、并发锁定与递归引用降级；真正的类型 kind 分派在 [convertSemaTy] 中完成。
     */
    fun deserializeType(typeIndex: Int): ConeCangJieType = context.materialization.read {
        // 冷类型与声明共用 metadata 事务；缓存命中也必须等待 owner、约束和跨模块 postCompute 完成。
        context.typeCache[typeIndex]?.let { return@read it }
        if (!typesUnderDeserialization.add(typeIndex)) return@read recursiveTypeReference(typeIndex)
        try {
            // FlatBuffers 缺失向量或越界抛错也属于本次读取，任何失败退出都必须清除递归守卫。
            val semaTy = context.pkg.allTypes(typeIndex)
                ?: return@read errorType("Cannot read type index $typeIndex")
            val result = convertSemaTy(semaTy)
            context.typeCache.putIfAbsent(typeIndex, result)
            context.materialization.onCompletion(
                publish = {},
                fail = { context.typeCache.remove(typeIndex, result) },
            )
            context.typeCache[typeIndex] ?: result
        } finally {
            typesUnderDeserialization.remove(typeIndex)
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

            TypeKind.Type -> convertTypeAliasType(semaTy)
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

    /** GenericTyInfo.declPtr 是真实 GenericParamDecl 身份；普通和 F-bound 重入共用此入口。 */
    private fun convertGenericType(semaTy: SemaTy): ConeCangJieType {
        if (semaTy.infoType != SemaTyInfo.GenericTyInfo) return errorType("Generic has the wrong info union")
        val info = semaTy.info(GenericTyInfo()) as? GenericTyInfo
            ?: return errorType("Generic missing GenericTyInfo")
        val fullId = info.declPtr ?: return errorType("GenericTyInfo missing declPtr")
        val resolved = context.fullIdResolver.resolve(fullId)
        if (resolved !is ResolvedFullId.Declaration || resolved.declaration.decl.kind != DeclKind.GenericParamDecl) {
            return errorType("GenericTyInfo target is not a GenericParamDecl: $resolved")
        }
        val symbol = context.fullIdResolver.resolveSymbol(resolved)
        check(symbol is CfirTypeParameterSymbol && symbol.isBound) { "GenericParamDecl wire record has the wrong symbol family" }
        return ConeTypeParameterTypeImpl(symbol.toLookupTag())
    }

    /**
     * 反序列化 typealias 类型引用（`TypeKind.Type`）。
     *
     * 官方 `.cjo` 把“源码里出现别名”的类型位置记录为 `TypeKind.Type`（schema 注释即 typealias），
     * 携带 `CompositeTyInfo.declPtr` 指向别名声明的 FullId。这里必须恢复成与源码解析一致的
     * [ConeTypeAliasType]：保留别名身份（`classId` + 实参），并尽量附加别名 RHS 的展开类型，
     * 使 `fullyExpandedType` 与 `Array<UInt8>` 等展开类型等价。
     *
     * 反例（修复前的行为）：把该 kind 一律退化为错误类型，会导致库签名里的 `Array<Byte>`
     * 变成 `Array<ERROR TYPE>`，进而在 IDE（STUBS）反编译视图里被渲染成 `Array<Any>`，
     * 并在使用点报出 `RETURN_TYPE_MISMATCH`。
     */
    private fun convertTypeAliasType(semaTy: SemaTy): ConeCangJieType {
        val info = semaTy.info(CompositeTyInfo()) as? CompositeTyInfo
            ?: return errorType("TypeAlias missing CompositeTyInfo")
        val fullId = info.declPtr
            ?: return errorType("TypeAlias CompositeTyInfo missing declPtr")
        val classId = context.fullIdResolver.resolveClassId(fullId)
            ?: return errorType("Cannot resolve typealias FullId: ${context.fullIdResolver.describe(fullId)}")
        val typeArguments = deserializeTypeArgs(semaTy)
        val aliasDeclaration = materializedTypeAliasOrNull(fullId)
        return ConeTypeAliasType(
            classId = classId,
            /*
             * 展开类型只在“同包已物化、且别名不是泛型”时内联：
             * 泛型别名的 RHS 需要按 use-site 实参替换，直接内联会得到未代入的 `Array<T>`，
             * 比留空更糟。留空走 `directExpansionType` 在 use-site 惰性展开（含实参替换）。
             */
            expandedType = aliasDeclaration
                ?.takeIf { alias -> alias.typeParameters.isEmpty() }
                ?.expandedTypeRef
                ?.coneTypeOrNull,
            typeArguments = typeArguments,
        )
    }

    /**
     * 取当前包中**已经物化**的别名声明；跨包或尚未物化时返回 null。
     *
     * 类型反序列化期间不得回调 `symbolProvider`：那会进入声明反序列化，而“正在反序列化”的
     * 重入守卫返回 null，provider 随即把该 classId 记入负缓存，别名将永久不可解析
     * （`AbstractCfirDeserializedSymbolProvider` 的 `missingClasses`）。
     * 跨包别名交给 use-site 的 `directExpansionType` 惰性展开，这也是源码路径的契约。
     */
    private fun materializedTypeAliasOrNull(fullId: FullId): CfirTypeAlias? {
        val resolved = context.fullIdResolver.resolve(fullId) as? ResolvedFullId.Declaration ?: return null
        if (resolved.source != ResolvedFullId.Declaration.Source.CURRENT_PACKAGE) return null
        return context.headerRecordOrNull(resolved.declaration.zeroBasedIndex)?.declarationRoot as? CfirTypeAlias
    }

    /** Generic F-bound 重入返回同一 bound record；其他真正的类型环保留结构化错误。 */
    private fun recursiveTypeReference(typeIndex: Int): ConeCangJieType {
        val semaTy = context.pkg.allTypes(typeIndex) ?: return errorType("Recursive type reference: $typeIndex")
        return if (semaTy.kind == TypeKind.Generic) convertGenericType(semaTy)
        else errorType("Recursive type reference: $typeIndex")
    }
}
