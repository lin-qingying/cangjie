package org.cangnova.cangjie.chir.core.serializer

import com.google.flatbuffers.reflection.BaseType
import com.google.flatbuffers.reflection.Enum as FlatBufferEnum
import com.google.flatbuffers.reflection.Field as FlatBufferField
import com.google.flatbuffers.reflection.Object as FlatBufferObject
import com.google.flatbuffers.reflection.Schema
import com.google.flatbuffers.reflection.Type as FlatBufferType
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 依据随 v1.1.3 schema 生成的 BFBS 描述验证完整 FlatBuffers 图，避免 object API 在校验前分配嵌套向量。
 * 验证范围与官方 FlatBuffers Verifier 一致：表、字符串、向量、结构体和 union 的边界及结构关系。
 */
internal object OfficialChirV113StructureVerifier {
    private const val MAX_DEPTH = 64
    private const val MAX_TABLES = 1_000_000
    private const val MAX_VECTOR_ELEMENTS = 4_000_000
    private const val REFLECTION_SCHEMA_RESOURCE = "/PackageFormatV113.bfbs"

    private val schema: Schema by lazy {
        val bytes = checkNotNull(Schema::class.java.getResourceAsStream(REFLECTION_SCHEMA_RESOURCE)) {
            "missing generated official CHIR v1.1.3 reflection schema"
        }.use { it.readBytes() }
        Schema.getRootAsSchema(ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN))
    }

    internal fun tableFieldNames(schemaType: String): List<String> {
        val descriptor = schema.objectsByKey("PackageFormatV113.$schemaType")
            ?: invalid("official CHIR v1.1.3 schema has no table $schemaType")
        val fields = (0 until descriptor.fieldsLength()).map { descriptor.fields(it) }
        val unionDiscriminants = fields.asSequence()
            .filter { field ->
                val type = field.type()
                type.baseType() == BaseType.Union ||
                    (type.baseType() == BaseType.Vector && type.element() == BaseType.Union)
            }
            .map { "${it.name()}_type" }
            .toSet()
        return fields.asSequence()
            .map { it.name() }
            .filterNot(unionDiscriminants::contains)
            .toList()
    }

    fun verify(bytes: ByteArray) {
        if (bytes.size < Int.SIZE_BYTES) {
            throw OfficialChirV113FormatException("Official CHIR v1.1.3 payload has no root offset")
        }
        try {
            Verifier(bytes).verify()
        } catch (exception: OfficialChirV113FormatException) {
            throw exception
        } catch (exception: RuntimeException) {
            throw OfficialChirV113FormatException(
                "Official CHIR v1.1.3 payload has an invalid FlatBuffers graph",
                exception,
            )
        }
    }

    private class Verifier(bytes: ByteArray) {
        private val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        private val objects = List(schema.objectsLength()) { schema.objects(it) }
        private val enums = List(schema.enumsLength()) { schema.enums(it) }
        private val activeTables = HashSet<TableKey>()
        private var tableCount = 0
        private var vectorElementCount = 0L

        fun verify() {
            val rootObject = schema.rootTable()
                ?: invalid("reflection schema has no root table")
            val rootPosition = readOffset(0, "root table")
            verifyTable(rootPosition, rootObject, 0, rootObject.name())
        }

        private fun verifyTable(position: Int, descriptor: FlatBufferObject, depth: Int, path: String) {
            if (descriptor.isStruct) {
                verifyStruct(position, descriptor, depth, path)
                return
            }
            if (depth > MAX_DEPTH) invalid("nested table depth exceeds $MAX_DEPTH at $path")
            val key = TableKey(position, descriptor.name())
            if (key in activeTables) invalid("cyclic nested table pointer at $path")
            tableCount++
            if (tableCount > MAX_TABLES) invalid("table count exceeds $MAX_TABLES")
            activeTables += key
            try {
                requireRange(position.toLong(), Int.SIZE_BYTES.toLong(), "$path table header")
                val vtablePosition = position.toLong() - buffer.getInt(position).toLong()
                requireRange(vtablePosition, 2L * Short.SIZE_BYTES, "$path vtable header")
                val vtableLength = readU16(vtablePosition.toInt(), "$path vtable length")
                val objectLength = readU16(vtablePosition.toInt() + Short.SIZE_BYTES, "$path table length")
                if (vtableLength < 2 * Short.SIZE_BYTES || vtableLength % Short.SIZE_BYTES != 0) {
                    invalid("invalid vtable length at $path")
                }
                if (objectLength < Int.SIZE_BYTES) invalid("invalid table length at $path")
                requireRange(vtablePosition, vtableLength.toLong(), "$path vtable")
                requireRange(position.toLong(), objectLength.toLong(), "$path table")

                val fields = (0 until descriptor.fieldsLength())
                    .map { descriptor.fields(it) }
                    .associateBy { it.name() }
                val fieldOffsets = HashMap<String, Int>()
                fields.values.forEach { field ->
                    val slot = 2L * Short.SIZE_BYTES + field.id().toLong() * Short.SIZE_BYTES
                    val offset = if (slot + Short.SIZE_BYTES > vtableLength) {
                        0
                    } else {
                        readU16(vtablePosition.toInt() + slot.toInt(), "$path.${field.name()} field offset")
                    }
                    if (offset == 0) {
                        if (field.required()) invalid("required field is absent at $path.${field.name()}")
                    } else {
                        val width = inlineWidth(field.type(), "$path.${field.name()}")
                        if (offset < Int.SIZE_BYTES || offset.toLong() + width > objectLength) {
                            invalid("field exceeds table bounds at $path.${field.name()}")
                        }
                        fieldOffsets[field.name()] = offset
                    }
                }

                fields.values.forEach { field ->
                    val type = field.type()
                    if (type.baseType() == BaseType.Union ||
                        (type.baseType() == BaseType.Vector && type.element() == BaseType.Union)
                    ) {
                        return@forEach
                    }
                    val fieldOffset = fieldOffsets[field.name()] ?: return@forEach
                    verifyValue(position + fieldOffset, type, depth + 1, "$path.${field.name()}")
                }
                fields.values.forEach { field ->
                    val type = field.type()
                    when {
                        type.baseType() == BaseType.Union -> verifyUnionField(
                            payloadField = field,
                            payloadPosition = fieldOffsets[field.name()]?.let { position + it },
                            tagField = fields["${field.name()}_type"],
                            tagPosition = fieldOffsets["${field.name()}_type"]?.let { position + it },
                            depth = depth,
                            path = path,
                        )

                        type.baseType() == BaseType.Vector && type.element() == BaseType.Union ->
                            verifyUnionVectorField(
                                field = field,
                                payloadPointerPosition = fieldOffsets[field.name()]?.let { position + it },
                                tagField = fields["${field.name()}_type"],
                                tagPointerPosition = fieldOffsets["${field.name()}_type"]?.let { position + it },
                                depth = depth,
                                path = path,
                            )
                    }
                }
            } finally {
                activeTables -= key
            }
        }

        private fun verifyStruct(position: Int, descriptor: FlatBufferObject, depth: Int, path: String) {
            if (depth > MAX_DEPTH) invalid("nested struct depth exceeds $MAX_DEPTH at $path")
            val size = descriptor.bytesize()
            if (size <= 0) invalid("invalid struct size at $path")
            requireRange(position.toLong(), size.toLong(), "$path struct")
            (0 until descriptor.fieldsLength()).map { descriptor.fields(it) }.forEach { field ->
                val offset = field.offset()
                val width = inlineWidth(field.type(), "$path.${field.name()}")
                if (offset < 0 || offset.toLong() + width > size) {
                    invalid("struct field exceeds bounds at $path.${field.name()}")
                }
                verifyInlineValue(position + offset, field.type(), depth + 1, "$path.${field.name()}")
            }
        }

        private fun verifyValue(position: Int, type: FlatBufferType, depth: Int, path: String) {
            when (type.baseType()) {
                BaseType.String -> verifyStringAt(position, path)
                BaseType.Vector -> verifyVectorAt(position, type, depth, path)
                BaseType.Obj -> {
                    val target = objectDescriptor(type.index(), path)
                    if (target.isStruct) {
                        verifyStruct(position, target, depth, path)
                    } else {
                        verifyTable(readOffset(position, path), target, depth, path)
                    }
                }

                BaseType.Union -> invalid("union field was not paired with its discriminant at $path")
                BaseType.Array -> verifyArray(position, type, depth, path)
                BaseType.Vector64 -> verifyVector64(position, type, depth, path)
                else -> verifyScalar(position, type.baseType(), type.index(), path)
            }
        }

        private fun verifyInlineValue(position: Int, type: FlatBufferType, depth: Int, path: String) {
            when (type.baseType()) {
                BaseType.Obj -> {
                    val descriptor = objectDescriptor(type.index(), path)
                    if (!descriptor.isStruct) invalid("table field inside struct at $path")
                    verifyStruct(position, descriptor, depth, path)
                }

                BaseType.Array -> verifyArray(position, type, depth, path)
                BaseType.String,
                BaseType.Vector,
                BaseType.Union,
                BaseType.Vector64,
                -> invalid("non-inline field in struct at $path")

                else -> verifyScalar(position, type.baseType(), type.index(), path)
            }
        }

        private fun verifyVectorAt(position: Int, vectorType: FlatBufferType, depth: Int, path: String) {
            val vectorPosition = readOffset(position, path)
            val length = readInt(vectorPosition, "$path vector length")
            if (length < 0) invalid("negative vector length at $path")
            countElements(length, path)
            val elementType = vectorType.element()
            val width = vectorElementWidth(elementType, vectorType.index(), path)
            val dataPosition = vectorPosition.toLong() + Int.SIZE_BYTES
            requireRange(dataPosition, length.toLong() * width, "$path vector")
            when (elementType) {
                BaseType.Obj -> {
                    val descriptor = objectDescriptor(vectorType.index(), path)
                    if (descriptor.isStruct) {
                        repeat(length) { index ->
                            verifyStruct((dataPosition + index.toLong() * width).toInt(), descriptor, depth, "$path[$index]")
                        }
                    } else {
                        repeat(length) { index ->
                            val itemPosition = (dataPosition + index.toLong() * width).toInt()
                            val relativeOffset = readInt(itemPosition, "$path[$index] offset")
                            if (relativeOffset != 0) {
                                verifyTable(targetPosition(itemPosition, relativeOffset, "$path[$index]"), descriptor, depth, "$path[$index]")
                            }
                        }
                    }
                }

                BaseType.String -> repeat(length) { index ->
                    val itemPosition = (dataPosition + index.toLong() * width).toInt()
                    val relativeOffset = readInt(itemPosition, "$path[$index] offset")
                    if (relativeOffset == 0) invalid("null string vector item at $path[$index]")
                    verifyString(targetPosition(itemPosition, relativeOffset, "$path[$index]"), "$path[$index]")
                }

                BaseType.Union -> invalid("union vector was not paired with its discriminant at $path")
                BaseType.Vector,
                BaseType.Vector64,
                -> invalid("nested vector element is not supported by the v1.1.3 CHIR schema at $path")

                else -> repeat(length) { index ->
                    verifyScalar(
                        (dataPosition + index.toLong() * width).toInt(),
                        elementType,
                        vectorType.index(),
                        "$path[$index]",
                    )
                }
            }
        }

        private fun verifyUnionField(
            payloadField: FlatBufferField,
            payloadPosition: Int?,
            tagField: FlatBufferField?,
            tagPosition: Int?,
            depth: Int,
            path: String,
        ) {
            val union = enumDescriptor(payloadField.type().index(), "$path.${payloadField.name()}")
            if (!union.isUnion) invalid("union field refers to a non-union enum at $path.${payloadField.name()}")
            val tag = tagPosition?.let { readScalar(it, tagField!!.type().baseType(), "$path.${payloadField.name()}_type") }
                ?: 0L
            val variant = unionVariant(union, tag, "$path.${payloadField.name()}")
            val payloadOffset = payloadPosition?.let { readInt(it, "$path.${payloadField.name()} offset") } ?: 0
            if (variant == null) {
                if (payloadOffset != 0) invalid("NONE union has a payload at $path.${payloadField.name()}")
                return
            }
            if (tagPosition == null || tagField == null) invalid("union discriminant is absent at $path.${payloadField.name()}")
            if (payloadOffset == 0) invalid("union payload is absent at $path.${payloadField.name()}")
            val target = objectDescriptor(variant, "$path.${payloadField.name()}")
            verifyTable(
                targetPosition(checkNotNull(payloadPosition), payloadOffset, "$path.${payloadField.name()}"),
                target,
                depth + 1,
                "$path.${payloadField.name()}",
            )
        }

        private fun verifyUnionVectorField(
            field: FlatBufferField,
            payloadPointerPosition: Int?,
            tagField: FlatBufferField?,
            tagPointerPosition: Int?,
            depth: Int,
            path: String,
        ) {
            val itemPath = "$path.${field.name()}"
            val union = enumDescriptor(field.type().index(), itemPath)
            if (!union.isUnion) invalid("union vector refers to a non-union enum at $itemPath")
            val payloadVector = payloadPointerPosition?.let { readOffsetOrNull(it, itemPath) }
            val tagVector = tagPointerPosition?.let { readOffsetOrNull(it, "$itemPath type vector") }
            if (payloadVector == null && tagVector == null) return
            val payloadLength = payloadVector?.let { readInt(it, "$itemPath vector length") } ?: 0
            val tagLength = tagVector?.let { readInt(it, "$itemPath type vector length") } ?: 0
            if (payloadLength < 0 || tagLength != payloadLength) {
                invalid("union vector and discriminant lengths differ at $itemPath")
            }
            if (payloadLength == 0) return
            if (payloadVector == null || tagVector == null || tagField == null) {
                invalid("union vector and discriminant vector are not paired at $itemPath")
            }
            countElements(payloadLength, itemPath)
            val payloadData = payloadVector.toLong() + Int.SIZE_BYTES
            val tagData = tagVector.toLong() + Int.SIZE_BYTES
            requireRange(payloadData, payloadLength.toLong() * Int.SIZE_BYTES, "$itemPath vector")
            val tagWidth = scalarWidth(tagField.type().element())
            requireRange(tagData, payloadLength.toLong() * tagWidth, "$itemPath type vector")
            repeat(payloadLength) { index ->
                val tagPosition = (tagData + index.toLong() * tagWidth).toInt()
                val tag = readScalar(tagPosition, tagField.type().element(), "$itemPath[$index] type")
                val variant = unionVariant(union, tag, "$itemPath[$index]")
                val payloadPosition = (payloadData + index.toLong() * Int.SIZE_BYTES).toInt()
                val payloadOffset = readInt(payloadPosition, "$itemPath[$index] offset")
                if (variant == null) {
                    if (payloadOffset != 0) invalid("NONE union has a payload at $itemPath[$index]")
                } else {
                    if (payloadOffset == 0) invalid("union payload is absent at $itemPath[$index]")
                    val target = objectDescriptor(variant, "$itemPath[$index]")
                    verifyTable(
                        targetPosition(payloadPosition, payloadOffset, "$itemPath[$index]"),
                        target,
                        depth + 1,
                        "$itemPath[$index]",
                    )
                }
            }
        }

        private fun verifyStringAt(pointerPosition: Int, path: String) {
            verifyString(readOffset(pointerPosition, path), path)
        }

        private fun verifyString(position: Int, path: String) {
            val length = readInt(position, "$path string length")
            if (length < 0) invalid("negative string length at $path")
            requireRange(position.toLong() + Int.SIZE_BYTES, length.toLong() + 1L, "$path string")
            val terminatorPosition = position + Int.SIZE_BYTES + length
            if (buffer.get(terminatorPosition) != 0.toByte()) invalid("string has no null terminator at $path")
        }

        private fun verifyArray(position: Int, type: FlatBufferType, depth: Int, path: String) {
            val elementType = type.element()
            val width = vectorElementWidth(elementType, type.index(), path)
            val count = type.fixedLength()
            if (count < 0) invalid("negative array length at $path")
            countElements(count, path)
            requireRange(position.toLong(), count.toLong() * width, "$path array")
            repeat(count) { index ->
                val elementPosition = (position.toLong() + index.toLong() * width).toInt()
                when (elementType) {
                    BaseType.Obj -> verifyStruct(elementPosition, objectDescriptor(type.index(), path), depth + 1, "$path[$index]")
                    BaseType.Array -> invalid("nested array is not supported at $path[$index]")
                    else -> verifyScalar(elementPosition, elementType, type.index(), "$path[$index]")
                }
            }
        }

        private fun verifyVector64(position: Int, type: FlatBufferType, depth: Int, path: String) {
            requireRange(position.toLong(), Long.SIZE_BYTES.toLong(), "$path vector64 offset")
            val vectorPosition = readLongOffset(position, path)
            val length = readInt(vectorPosition, "$path vector64 length")
            if (length < 0) invalid("negative vector64 length at $path")
            countElements(length, path)
            val width = vectorElementWidth(type.element(), type.index(), path)
            val dataPosition = vectorPosition.toLong() + Int.SIZE_BYTES
            requireRange(dataPosition, length.toLong() * width, "$path vector64")
            repeat(length) { index ->
                verifyScalar((dataPosition + index.toLong() * width).toInt(), type.element(), type.index(), "$path[$index]")
            }
        }

        private fun verifyScalar(position: Int, baseType: Byte, enumIndex: Int, path: String) {
            val width = scalarWidth(baseType)
            requireRange(position.toLong(), width.toLong(), path)
            if (enumIndex >= 0) {
                val descriptor = enumDescriptor(enumIndex, path)
                val value = readScalar(position, baseType, path)
                if (descriptor.valuesByKey(value) == null) invalid("unsupported ${descriptor.name()} value $value at $path")
            }
        }

        private fun readScalar(position: Int, baseType: Byte, path: String): Long {
            requireRange(position.toLong(), scalarWidth(baseType).toLong(), path)
            return when (baseType) {
                BaseType.Bool,
                BaseType.Byte,
                -> buffer.get(position).toLong()

                BaseType.UByte,
                BaseType.UType,
                -> buffer.get(position).toUByte().toLong()

                BaseType.Short -> buffer.getShort(position).toLong()
                BaseType.UShort -> buffer.getShort(position).toUShort().toLong()
                BaseType.Int -> buffer.getInt(position).toLong()
                BaseType.UInt -> buffer.getInt(position).toUInt().toLong()
                BaseType.Long -> buffer.getLong(position)
                BaseType.ULong -> buffer.getLong(position).toULong().toLong()
                else -> invalid("non-scalar field type ${BaseType.name(baseType.toInt())} at $path")
            }
        }

        private fun unionVariant(descriptor: FlatBufferEnum, value: Long, path: String): Int? {
            val item = descriptor.valuesByKey(value)
                ?: invalid("unsupported ${descriptor.name()} union discriminant $value at $path")
            if (value == 0L) return null
            val type = item.unionType()
                ?: invalid("${descriptor.name()} union variant ${item.name()} has no table type at $path")
            if (type.baseType() != BaseType.Obj) invalid("${descriptor.name()} union variant ${item.name()} is not a table at $path")
            return type.index()
        }

        private fun vectorElementWidth(baseType: Byte, index: Int, path: String): Int = when (baseType) {
            BaseType.String,
            BaseType.Vector,
            BaseType.Union,
            -> Int.SIZE_BYTES

            BaseType.Obj -> {
                val descriptor = objectDescriptor(index, path)
                if (descriptor.isStruct) descriptor.bytesize() else Int.SIZE_BYTES
            }

            BaseType.Array -> invalid("array vector elements are not supported by the v1.1.3 CHIR schema at $path")
            BaseType.Vector64 -> Long.SIZE_BYTES
            else -> scalarWidth(baseType)
        }

        private fun inlineWidth(type: FlatBufferType, path: String): Int = when (type.baseType()) {
            BaseType.String,
            BaseType.Vector,
            BaseType.Union,
            -> Int.SIZE_BYTES

            BaseType.Vector64 -> Long.SIZE_BYTES
            BaseType.Obj -> {
                val descriptor = objectDescriptor(type.index(), path)
                if (descriptor.isStruct) descriptor.bytesize() else Int.SIZE_BYTES
            }

            BaseType.Array -> type.fixedLength() * vectorElementWidth(type.element(), type.index(), path)
            else -> scalarWidth(type.baseType())
        }

        private fun scalarWidth(baseType: Byte): Int = when (baseType) {
            BaseType.Bool,
            BaseType.Byte,
            BaseType.UByte,
            BaseType.UType,
            -> Byte.SIZE_BYTES

            BaseType.Short,
            BaseType.UShort,
            -> Short.SIZE_BYTES

            BaseType.Int,
            BaseType.UInt,
            BaseType.Float,
            -> Int.SIZE_BYTES

            BaseType.Long,
            BaseType.ULong,
            BaseType.Double,
            -> Long.SIZE_BYTES

            else -> invalid("unsupported inline field type ${BaseType.name(baseType.toInt())}")
        }

        private fun objectDescriptor(index: Int, path: String): FlatBufferObject =
            objects.getOrNull(index) ?: invalid("invalid table type index $index at $path")

        private fun enumDescriptor(index: Int, path: String): FlatBufferEnum =
            enums.getOrNull(index) ?: invalid("invalid enum type index $index at $path")

        private fun readOffset(position: Int, path: String): Int {
            val relative = readInt(position, "$path offset")
            return targetPosition(position, relative, path)
        }

        private fun readOffsetOrNull(position: Int, path: String): Int? {
            val relative = readInt(position, "$path offset")
            return if (relative == 0) null else targetPosition(position, relative, path)
        }

        private fun readLongOffset(position: Int, path: String): Int {
            val relative = buffer.getLong(position)
            val target = position.toLong() + relative
            requireRange(target, Int.SIZE_BYTES.toLong(), "$path vector64")
            return target.toInt()
        }

        private fun targetPosition(position: Int, relative: Int, path: String): Int {
            if (relative <= 0) invalid("invalid relative offset at $path")
            val target = position.toLong() + relative.toLong()
            requireRange(target, Int.SIZE_BYTES.toLong(), "$path target")
            return target.toInt()
        }

        private fun readInt(position: Int, path: String): Int {
            requireRange(position.toLong(), Int.SIZE_BYTES.toLong(), path)
            return buffer.getInt(position)
        }

        private fun readU16(position: Int, path: String): Int {
            requireRange(position.toLong(), Short.SIZE_BYTES.toLong(), path)
            return buffer.getShort(position).toUShort().toInt()
        }

        private fun countElements(count: Int, path: String) {
            vectorElementCount += count.toLong()
            if (vectorElementCount > MAX_VECTOR_ELEMENTS) {
                invalid("total vector element count exceeds $MAX_VECTOR_ELEMENTS at $path")
            }
        }

        private fun requireRange(position: Long, size: Long, path: String) {
            if (position < 0 || size < 0 || position + size > buffer.limit().toLong()) {
                invalid("out-of-range FlatBuffers data at $path")
            }
        }
    }

    private data class TableKey(val position: Int, val typeName: String)

    private fun invalid(message: String): Nothing = throw OfficialChirV113FormatException(message)
}
