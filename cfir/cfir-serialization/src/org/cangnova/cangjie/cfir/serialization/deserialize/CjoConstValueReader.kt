package org.cangnova.cangjie.cfir.serialization.deserialize

import PackageFormat.ArrayValue
import PackageFormat.CompositeValue
import PackageFormat.CompositeValueIndex
import PackageFormat.ConstValue
import PackageFormat.MemberValue
import PackageFormat.Package
import PackageFormat.VarInfo
import com.google.flatbuffers.Table
import com.google.flatbuffers.Utf8
import java.nio.ByteBuffer

/**
 * ModuleFormat `ConstValue` 的类型化 reader。
 *
 * 当前 flatc Kotlin 输出只为包含 table 的 union 生成 `value(Table)`；官方
 * `ConstValue` 同时包含 struct 和 string，因此这里提供唯一的序列化层适配器。
 * 它通过 FlatBuffers 生成 accessor 已经解析出的 union 目标读取值，不根据源码
 * 文本、声明名称或未验证的 raw CFIR 猜测常量类型。
 */
internal object CjoConstValueReader {
    internal sealed interface Value {
        data class Scalar(val tag: UByte, val value: Any) : Value
        data class StringValue(val value: String) : Value
        data class ArrayValue(val elements: List<Value>) : Value
        /** Raw union reference; it is not a materialized semantic value. */
        data class CompositeReference(val index: UInt) : Value

        /** Fully materialized official Package.allValues entry. */
        data class CompositeValue(
            val index: UInt,
            val type: UInt,
            val fields: List<Field>,
        ) : Value {
            data class Field(
                val name: String,
                val type: UInt,
                val value: Value,
            )
        }
    }

    /**
     * FlatBuffers union 的统一目标对象。
     *
     * 对 struct/string union 成员，flatc 仍把 union 目标传给 `Table`; 该类型
     * 只暴露已生成 runtime 的 buffer/position，并集中承载 struct/string 读取。
     */
    private class UnionValueTable : Table() {
        /** Struct union members are not tables; prevent Table.__reset from reading a vtable. */
        override fun __reset(_i: Int, _bb: ByteBuffer) {
            bb_pos = _i
            bb = _bb
        }

        fun position(): Int = bb_pos

        fun buffer() = getByteBuffer()

        /** The union accessor points directly at a FlatBuffers string object. */
        fun stringValue(): String {
            val length = buffer().getInt(position())
            return Utf8.getDefault().decodeUtf8(buffer(), position() + 4, length)
        }
    }

    /** Read only the union payload; composite values remain explicit raw references. */
    internal fun readRaw(info: VarInfo): Value? {
        if (info.valueType == ConstValue.NONE) return null
        val target = UnionValueTable()
        checkNotNull(info.value(target)) {
            "VarInfo declares ConstValue tag ${info.valueType}, but its value union is absent"
        }
        return decode(info.valueType, target, null)
    }

    /** Read and fully materialize a union against its owning official Package. */
    internal fun read(info: VarInfo, packageData: Package): Value? {
        if (info.valueType == ConstValue.NONE) return null
        val target = UnionValueTable()
        checkNotNull(info.value(target)) {
            "VarInfo declares ConstValue tag ${info.valueType}, but its value union is absent"
        }
        return decode(info.valueType, target, packageData)
    }

    private fun decode(tag: UByte, target: UnionValueTable, packageData: Package?): Value = when (tag) {
        // Struct union members are read through their generated FlatBuffers
        // accessors. The adapter only exposes the union target's position and
        // buffer because the generated union API represents every member as Table.
        ConstValue.Int8Value -> Value.Scalar(tag, PackageFormat.Int8Value().__assign(target.position(), target.buffer()).val_)
        ConstValue.UInt8Value -> Value.Scalar(tag, PackageFormat.UInt8Value().__assign(target.position(), target.buffer()).val_.toUByte())
        ConstValue.Int16Value -> Value.Scalar(tag, PackageFormat.Int16Value().__assign(target.position(), target.buffer()).val_)
        ConstValue.UInt16Value -> Value.Scalar(tag, PackageFormat.UInt16Value().__assign(target.position(), target.buffer()).val_.toUShort())
        ConstValue.Int32Value -> Value.Scalar(tag, PackageFormat.Int32Value().__assign(target.position(), target.buffer()).val_)
        ConstValue.UInt32Value -> Value.Scalar(tag, PackageFormat.UInt32Value().__assign(target.position(), target.buffer()).val_.toUInt())
        ConstValue.Int64Value -> Value.Scalar(tag, PackageFormat.Int64Value().__assign(target.position(), target.buffer()).val_)
        ConstValue.UInt64Value -> Value.Scalar(tag, PackageFormat.UInt64Value().__assign(target.position(), target.buffer()).val_.toULong())
        ConstValue.Float32Value -> Value.Scalar(tag, PackageFormat.Float32Value().__assign(target.position(), target.buffer()).val_)
        ConstValue.Float64Value -> Value.Scalar(tag, PackageFormat.Float64Value().__assign(target.position(), target.buffer()).val_)
        ConstValue.StringValue -> Value.StringValue(target.stringValue())
        ConstValue.CompositeValue -> {
            val index = CompositeValueIndex().__assign(target.position(), target.buffer()).idx
            if (packageData == null) Value.CompositeReference(index) else decodeComposite(index, packageData)
        }
        ConstValue.ArrayValue -> decodeArray(target, packageData)
        else -> error("Unsupported ModuleFormat ConstValue tag: $tag")
    }

    private fun decodeArray(target: UnionValueTable, packageData: Package?): Value.ArrayValue {
        val array = ArrayValue().__assign(target.position(), target.buffer())
        check(array.valTypeLength == array.val_Length) {
            "ConstValue.ArrayValue has mismatched tag/value vector lengths: " +
                "${array.valTypeLength} != ${array.val_Length}"
        }
        val elements = buildList(array.valTypeLength) {
            for (index in 0 until array.valTypeLength) {
                checkNotNull(array.val_(target, index)) {
                    "ConstValue.ArrayValue element #$index has no union value"
                }
                add(decode(array.valType(index), target, packageData))
            }
        }
        return Value.ArrayValue(elements)
    }

    /** 展开官方 Package.allValues，并保留每个字段的 wire type 和常量值。 */
    private fun decodeComposite(index: UInt, packageData: Package): Value.CompositeValue {
        require(index < packageData.allValuesLength.toUInt()) {
            "ConstValue.CompositeValue points outside Package.allValues: $index"
        }
        val composite = requireNotNull(packageData.allValues(index.toInt())) {
            "Package.allValues[$index] is absent"
        }
        val fields = buildList(composite.fieldsLength) {
            for (fieldIndex in 0 until composite.fieldsLength) {
                val field = requireNotNull(composite.fields(fieldIndex)) {
                    "Package.allValues[$index].fields[$fieldIndex] is absent"
                }
                add(decodeCompositeField(field, packageData, index, fieldIndex))
            }
        }
        return Value.CompositeValue(index, composite.type, fields)
    }

    private fun decodeCompositeField(
        field: MemberValue,
        packageData: Package,
        compositeIndex: UInt,
        fieldIndex: Int,
    ): Value.CompositeValue.Field {
        val name = requireNotNull(field.field) {
            "Package.allValues[$compositeIndex].fields[$fieldIndex] has no field name"
        }
        val target = UnionValueTable()
        checkNotNull(field.value(target)) {
            "Package.allValues[$compositeIndex].fields[$fieldIndex] has no union value"
        }
        return Value.CompositeValue.Field(
            name = name,
            type = field.type,
            value = decode(field.valueType, target, packageData),
        )
    }
}
