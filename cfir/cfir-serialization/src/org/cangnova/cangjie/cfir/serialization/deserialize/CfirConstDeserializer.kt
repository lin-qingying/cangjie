package org.cangnova.cangjie.cfir.serialization.deserialize

import PackageFormat.ConstValue
import PackageFormat.VarInfo
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.expressions.CfirConstantValue
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagWithFixedSymbol
import org.cangnova.cangjie.cfir.serialization.cjo.*
import org.cangnova.cangjie.cfir.resolve.substitution.substitutorByMap
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.cangnova.cangjie.cfir.types.*
import org.cangnova.cangjie.name.Name
import java.math.BigInteger
import java.util.Collections

/**
 * 同一 loaded package 的 typed 常量材料化 owner。
 *
 * VarInfo、Pattern 与 allValues 共用 context 的值身份和完成事务；原始 union reader 只读保存槽。
 * Kotlin FirConstDeserializer 的 container/helper 职责在此本地化，CJO 的 v 池、binary16 与对象
 * 回边是仓颉格式所需的扩展，不冒称固定 native writer 已经生产非空 VarInfo.value/allValues。
 */
internal class CfirConstDeserializer(private val context: CfirDeserializationContext) {
    private val types = context.createTypeDeserializer()

    fun read(info: VarInfo, typeReference: UInt, hostPosition: Int): CfirConstantValue? = context.materialize {
        if (info.valueType == ConstValue.NONE) return@materialize null
        val type = requiredType(typeReference)
        root(CjoConstantRootSlot.Variable(hostPosition), checkNotNull(CjoConstValueReader.readRawRoot(info)), typeReference, type)
    }

    fun read(pattern: PackageFormat.Pattern, hostPosition: Int): List<CfirConstantValue> = context.materialize {
        check(pattern.valuesTypeLength == pattern.valuesLength)
        if (pattern.valuesLength == 0) return@materialize emptyList()
        check(pattern.typesLength != 0)
        val type = requiredType(pattern.types(0))
        Collections.unmodifiableList(CjoConstValueReader.readRawRoots(pattern).map { root(CjoConstantRootSlot.Pattern(hostPosition, it.ordinal), it, pattern.types(0), type) })
    }

    private fun root(slot: CjoConstantRootSlot, wire: CjoConstValueReader.Root, typeReference: UInt, type: ConeCangJieType): CfirConstantValue {
        context.constantRoots[slot]?.let { saved ->
            check(saved.typeReference == typeReference && saved.type === type) { "One constant host has conflicting storage type uses" }
            checkCompatible(saved.value.type, type)
            return saved.value
        }
        return value(wire.value, type).also { context.constantRoots[slot] = CjoSavedConstantRoot(typeReference, type, it) }
    }

    private fun requiredType(reference: UInt): ConeCangJieType {
        require(reference != 0u && reference <= context.pkg.allTypesLength.toUInt()) {
            "A saved constant requires its actual CJO type slot: $reference"
        }
        return types.deserializeTypeFromField(reference).also {
            check(!it.containsErrorType()) { "A saved constant has an invalid semantic type" }
            context.completeTypeHeaders()
        }
    }

    private fun value(wire: CjoConstValueReader.Value, type: ConeCangJieType): CfirConstantValue {
        val result = when (wire) {
        is CjoConstValueReader.Value.Scalar -> scalar(wire, type)
        is CjoConstValueReader.Value.StringValue -> {
            check((type is ConeClassLikeType || type is ConeStructType) &&
                (type as ConeLookupTagBasedType).classId == StdlibClassIds.String)
            CfirConstantValue.Primitive(type, CfirLiteralKind.STRING, wire.value)
        }
        is CjoConstValueReader.Value.ArrayValue -> when (type) {
            is ConeTupleType -> {
                check(type.elementTypes.size == wire.elements.size)
                CfirConstantValue.Tuple(type, Collections.unmodifiableList(wire.elements.mapIndexed { index, item ->
                    value(item, type.elementTypes[index])
                }))
            }
            is ConeVArrayType -> CfirConstantValue.VArray(type,
                Collections.unmodifiableList(wire.elements.map { value(it, type.elementType) }))
            else -> {
                val elementType = checkNotNull(type.arrayElementType) { "ConstValue.ArrayValue has no actual element type" }
                CfirConstantValue.ArrayValue(type, Collections.unmodifiableList(wire.elements.map { value(it, elementType) }))
            }
        }
        is CjoConstValueReader.Value.CompositeReference -> composite(wire.index).also { checkCompatible(it.type, type) }
        is CjoConstValueReader.Value.CompositeValue -> error("Expanded wire composite reached the typed constant reader")
        }
        val storage = when (wire) {
            is CjoConstValueReader.Value.Scalar -> CjoSavedConstantStorage.Scalar(when (wire.tag) {
                ConstValue.Int8Value -> CjoInt8ConstValue(wire.value as Byte)
                ConstValue.UInt8Value -> CjoUInt8ConstValue(wire.value as UByte)
                ConstValue.Int16Value -> CjoInt16ConstValue(wire.value as Short)
                ConstValue.UInt16Value -> CjoUInt16ConstValue(wire.value as UShort)
                ConstValue.Int32Value -> CjoInt32ConstValue(wire.value as Int)
                ConstValue.UInt32Value -> CjoUInt32ConstValue(wire.value as UInt)
                ConstValue.Int64Value -> CjoInt64ConstValue(wire.value as Long)
                ConstValue.UInt64Value -> CjoUInt64ConstValue(wire.value as ULong)
                ConstValue.Float32Value -> CjoFloat32BitsConstValue(wire.bits.toInt())
                ConstValue.Float64Value -> CjoFloat64BitsConstValue(wire.bits)
                else -> error("A typed scalar has no saved union representation")
            })
            is CjoConstValueReader.Value.StringValue -> CjoSavedConstantStorage.Scalar(CjoStringConstValue(wire.value))
            is CjoConstValueReader.Value.ArrayValue -> CjoSavedConstantStorage.Array
            is CjoConstValueReader.Value.CompositeReference -> return result
            is CjoConstValueReader.Value.CompositeValue -> error("An expanded wire value cannot enter storage capture")
        }
        check(context.constantStorage.put(result, storage) == null)
        return result
    }

    private fun scalar(wire: CjoConstValueReader.Value.Scalar, type: ConeCangJieType): CfirConstantValue.Primitive {
        // 固定 schema 未规定 Bool/Rune 的整数承载 tag；其 SOURCE literal 路径独立保留。
        val expectedTag = when (type) {
            ConePrimitiveType.INT8 -> ConstValue.Int8Value
            ConePrimitiveType.INT16, ConePrimitiveType.FLOAT16 -> ConstValue.Int16Value
            ConePrimitiveType.INT32 -> ConstValue.Int32Value
            ConePrimitiveType.INT64 -> ConstValue.Int64Value
            ConePrimitiveType.UINT8 -> ConstValue.UInt8Value
            ConePrimitiveType.UINT16 -> ConstValue.UInt16Value
            ConePrimitiveType.UINT32 -> ConstValue.UInt32Value
            ConePrimitiveType.UINT64 -> ConstValue.UInt64Value
            ConePrimitiveType.FLOAT32 -> ConstValue.Float32Value
            ConePrimitiveType.FLOAT64 -> ConstValue.Float64Value
            else -> error("ConstValue scalar has no encoding for its actual semantic type")
        }
        check(wire.tag == expectedTag) { "ConstValue scalar tag disagrees with its actual semantic type" }
        return when (type) {
            ConePrimitiveType.FLOAT16 -> CfirConstantValue.Primitive(type, CfirLiteralKind.FLOAT,
                binary16((wire.value as Short).toInt() and 0xffff))
            ConePrimitiveType.FLOAT32, ConePrimitiveType.FLOAT64 ->
                CfirConstantValue.Primitive(type, CfirLiteralKind.FLOAT, (wire.value as Number).toDouble())
            else -> CfirConstantValue.Primitive(type, CfirLiteralKind.INT, integer(wire.value))
        }
    }

    private fun composite(index: UInt): CfirConstantValue {
        context.constantValues[index]?.let { return it }
        val wire = CjoConstValueReader.readCompositeRaw(index, context.pkg)
        val type = requiredType(wire.type)
        check(context.constantValuesInProgress.add(index))
        try {
            if (type is ConeTupleType) {
                check(wire.fields.size == type.elementTypes.size)
                val elements = ArrayList<CfirConstantValue>()
                val storage = ArrayList<CjoSavedConstantField>()
                val shell = CfirConstantValue.Tuple(type, Collections.unmodifiableList(elements))
                context.constantValues[index] = shell
                wire.fields.forEachIndexed { ordinal, field ->
                    val fieldType = requiredType(field.type)
                    check(fieldType == type.elementTypes[ordinal])
                    val child = value(field.value, fieldType)
                    elements += child
                    storage += CjoSavedConstantField(field.name, fieldType, child, null)
                }
                context.constantStorage[shell] = CjoSavedConstantStorage.Composite(Collections.unmodifiableList(storage.toList()))
                return shell
            }
            check(type is ConeClassLikeType && !type.isInterface) { "CompositeValue requires a stored tuple or nominal object type" }
            val classifier = classifier(type)
            check(classifier.typeParameters.size == type.typeArguments.size) { "CompositeValue has invalid classifier type argument count" }
            val substitutor = substitutorByMap(classifier.typeParameters.zip(type.typeArguments).associate { (parameter, argument) ->
                parameter.symbol to argument.type
            }, context.moduleData.session)
            val fields = LinkedHashMap<CfirCallableSymbol<*>, CfirConstantValue>()
            val storageTypes = LinkedHashMap<CfirCallableSymbol<*>, ConeCangJieType>()
            val storage = ArrayList<CjoSavedConstantField>()
            val shell = CfirConstantValue.ObjectValue(type, Collections.unmodifiableMap(fields), index.toLong(),
                Collections.unmodifiableMap(storageTypes))
            context.constantValues[index] = shell
            for (field in wire.fields) {
                // 名称只定位已选 classifier 的真实存储字段；不生成 symbol、不重选公开 ClassId。
                val member = checkNotNull(classifier.declarations.filterIsInstance<CfirFieldVariable>()
                    .singleOrNull { it.name == Name.identifier(field.name) }) {
                    "CompositeValue has no unique storage field in its actual classifier"
                }
                check(!fields.containsKey(member.symbol)) { "CompositeValue contains a duplicate field" }
                val fieldType = requiredType(field.type)
                val declaredType = substitutor.substituteOrSelf(member.returnTypeRef.coneType)
                checkCompatible(fieldType, declaredType)
                storageTypes[member.symbol] = fieldType
                val child = value(field.value, fieldType)
                fields[member.symbol] = child
                storage += CjoSavedConstantField(field.name, fieldType, child, member.symbol)
            }
            context.constantStorage[shell] = CjoSavedConstantStorage.Composite(Collections.unmodifiableList(storage.toList()))
            return shell
        } catch (failure: Throwable) {
            context.constantValues.remove(index)
            throw failure
        } finally {
            context.constantValuesInProgress.remove(index)
        }
    }

    private fun classifier(type: ConeClassLikeType): CfirClassLikeDeclaration {
        val tag = checkNotNull(type.lookupTag as? ConeClassLikeLookupTagWithFixedSymbol) { "CompositeValue type has no exact classifier identity" }
        check(tag.symbol.isBound) { "A constant cannot consume an incomplete classifier header" }
        return tag.symbol.cfir
    }

    /** 每个入口及缓存/回边都核对真实赋值方向，不修改已完成类型或 canonical 字段声明。 */
    private fun checkCompatible(actual: ConeCangJieType, expected: ConeCangJieType) {
        check(AbstractTypeChecker.isSubtypeOf(context.moduleData.session.typeContext, actual, expected)) {
            "Saved constant type is incompatible with its actual declaration type"
        }
    }

    /** binary16 逐位扩展到 Double，保存正负零、subnormal、Infinity 和 NaN 的原符号/有效位。 */
    private fun binary16(bits: Int): Double {
        val sign = (bits.toLong() and 0x8000L) shl 48
        val exponent = bits ushr 10 and 0x1f
        val fraction = bits and 0x3ff
        return when (exponent) {
            0 -> if (fraction == 0) Double.fromBits(sign) else {
                val magnitude = Math.scalb(fraction.toDouble(), -24)
                if (sign == 0L) magnitude else -magnitude
            }
            0x1f -> Double.fromBits(sign or (0x7ffL shl 52) or (fraction.toLong() shl 42))
            else -> Double.fromBits(sign or ((exponent + 1008L) shl 52) or (fraction.toLong() shl 42))
        }
    }

    private fun integer(value: Any): BigInteger = when (value) {
        is ULong -> BigInteger(value.toString())
        is UInt -> BigInteger(value.toString())
        is UShort -> BigInteger.valueOf(value.toLong())
        is UByte -> BigInteger.valueOf(value.toLong())
        is Byte -> BigInteger.valueOf(value.toLong())
        is Short -> BigInteger.valueOf(value.toLong())
        is Int -> BigInteger.valueOf(value.toLong())
        is Long -> BigInteger.valueOf(value)
        else -> error("An integer ConstValue has a non-integer payload")
    }
}
