package org.cangnova.cangjie.chir.core.serializer

import PackageFormatV113.CHIRPackage
import PackageFormatV113.CustomTypeDefElem
import PackageFormatV113.ExpressionElem
import PackageFormatV113.PackageAccessLevel
import PackageFormatV113.Phase
import PackageFormatV113.TypeElem
import PackageFormatV113.ValueElem
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** 官方 v1.1.3 CHIR 表索引域；各域分别使用从 1 开始的 ID 空间。 */
enum class OfficialChirV113TableDomain {
    TYPE,
    VALUE,
    EXPRESSION,
    DEFINITION,
}

/** 官方 CHIR 四类 union 向量中的一条无损索引引用。 */
data class OfficialChirV113TableReference(
    val domain: OfficialChirV113TableDomain,
    val id: UInt,
    val unionTag: UByte,
)

enum class OfficialChirV113Phase(val wireValue: UByte) {
    RAW(Phase.RAW),
    OPT(Phase.OPT),
    PLUGIN(Phase.PLUGIN),
    ANALYSIS_FOR_CJLINT(Phase.ANALYSIS_FOR_CJLINT),
}

enum class OfficialChirV113PackageAccessLevel(val wireValue: UByte) {
    INVALID(PackageAccessLevel.INVALID),
    INTERNAL(PackageAccessLevel.INTERNAL),
    PROTECTED(PackageAccessLevel.PROTECTED),
    PUBLIC(PackageAccessLevel.PUBLIC),
}

/** 官方 v1.1.3 包根表字段，与仓内 CHIR codec 的字段模型隔离。 */
data class OfficialChirV113PackageMetadata(
    val name: String?,
    val path: String?,
    val accessLevel: OfficialChirV113PackageAccessLevel,
    val phase: OfficialChirV113Phase,
    val packageInitFunctionId: UInt,
    val packageLiteralInitFunctionId: UInt,
    val maxImportedValueId: UInt,
    val maxImportedStructId: UInt,
    val maxImportedClassId: UInt,
    val maxImportedEnumId: UInt,
    val maxImportedExtendId: UInt,
)

/**
 * 官方 v1.1.3 FlatBuffers 包的版本化无损边界。
 *
 * 原始字节是完整载荷的权威表示：仓内 [ChirPackageCodec] 使用另一套 schema，不能读取此格式。
 * 表引用保留官方各自独立的 ID 域和 union 判别值，不将索引扁平化或重新编号。
 * 此类型只解析根表元数据与 union 标签，不验证嵌套表图，也不验证跨表 ID；生产消费前必须经过
 * v1.1.3 专用的完整结构与语义验证。
 */
class OfficialChirV113WirePackage internal constructor(
    val metadata: OfficialChirV113PackageMetadata,
    val typeReferences: List<OfficialChirV113TableReference>,
    val valueReferences: List<OfficialChirV113TableReference>,
    val expressionReferences: List<OfficialChirV113TableReference>,
    val definitionReferences: List<OfficialChirV113TableReference>,
    private val serializedPackage: ByteArray,
) {
    /** 返回官方 FlatBuffers 载荷的副本。 */
    fun toByteArray(): ByteArray = serializedPackage.copyOf()
}

/**
 * 读取官方 v1.1.3 CHIR wire 格式，不经由不兼容的本地 [ChirPackageCodec]。
 */
object OfficialChirV113WirePackageDecoder {
    private const val MINIMUM_FLATBUFFER_SIZE = 8

    /**
 * 解码包根表并保留各类型表索引及完整原始图。该方法不等价于官方 FlatBuffers Verifier，
 * 不可单独作为生产 `.chir` 输入的完整性验证或语义导入入口。
     * 调用方根据编译器版本协议选择 schema；CHIR 文件标识本身不包含 schema 版本。
     */
    fun decode(bytes: ByteArray): OfficialChirV113WirePackage {
        if (bytes.size < MINIMUM_FLATBUFFER_SIZE) {
            throw OfficialChirV113FormatException(
                "Official CHIR v1.1.3 payload is shorter than a FlatBuffers root header",
            )
        }

        val stableBytes = bytes.copyOf()
        return try {
            val buffer = ByteBuffer.wrap(stableBytes).order(ByteOrder.LITTLE_ENDIAN)
            validateRootTable(buffer)
            val root = CHIRPackage.getRootAsCHIRPackage(buffer)

            val types = readReferences(
                domain = OfficialChirV113TableDomain.TYPE,
                tagCount = root.typesTypeLength,
                entryCount = root.typesLength,
                tagAt = root::typesType,
                validTag = { it > TypeElem.NONE && it <= TypeElem.ThisType },
                payloadSize = stableBytes.size,
            )
            val values = readReferences(
                domain = OfficialChirV113TableDomain.VALUE,
                tagCount = root.valuesTypeLength,
                entryCount = root.valuesLength,
                tagAt = root::valuesType,
                validTag = { it > ValueElem.NONE && it <= ValueElem.BlockGroup },
                payloadSize = stableBytes.size,
            )
            val expressions = readReferences(
                domain = OfficialChirV113TableDomain.EXPRESSION,
                tagCount = root.exprsTypeLength,
                entryCount = root.exprsLength,
                tagAt = root::exprsType,
                validTag = { it > ExpressionElem.NONE && it <= ExpressionElem.GetRTTIStatic },
                payloadSize = stableBytes.size,
            )
            val definitions = readReferences(
                domain = OfficialChirV113TableDomain.DEFINITION,
                tagCount = root.defsTypeLength,
                entryCount = root.defsLength,
                tagAt = root::defsType,
                validTag = { it > CustomTypeDefElem.NONE && it <= CustomTypeDefElem.ExtendDef },
                payloadSize = stableBytes.size,
            )

            val metadata = OfficialChirV113PackageMetadata(
                name = root.name,
                path = root.path,
                accessLevel = decodeAccessLevel(root.pkgAccessLevel),
                phase = decodePhase(root.phase),
                packageInitFunctionId = root.packageInitFunc,
                packageLiteralInitFunctionId = root.packageLiteralInitFunc,
                maxImportedValueId = root.maxImportedValueId,
                maxImportedStructId = root.maxImportedStructId,
                maxImportedClassId = root.maxImportedClassId,
                maxImportedEnumId = root.maxImportedEnumId,
                maxImportedExtendId = root.maxImportedExtendId,
            )
            OfficialChirV113WirePackage(
                metadata = metadata,
                typeReferences = types,
                valueReferences = values,
                expressionReferences = expressions,
                definitionReferences = definitions,
                serializedPackage = stableBytes,
            )
        } catch (exception: OfficialChirV113FormatException) {
            throw exception
        } catch (exception: RuntimeException) {
            throw OfficialChirV113FormatException(
                "Official CHIR v1.1.3 payload has an invalid root envelope",
                exception,
            )
        }
    }

    private fun readReferences(
        domain: OfficialChirV113TableDomain,
        tagCount: Int,
        entryCount: Int,
        tagAt: (Int) -> UByte,
        validTag: (UByte) -> Boolean,
        payloadSize: Int,
    ): List<OfficialChirV113TableReference> {
        if (tagCount < 0 || entryCount < 0 || tagCount > payloadSize || entryCount > payloadSize / Int.SIZE_BYTES) {
            throw OfficialChirV113FormatException(
                "Official CHIR v1.1.3 $domain vector lengths exceed the payload bounds",
            )
        }
        if (tagCount != entryCount) {
            throw OfficialChirV113FormatException(
                "Official CHIR v1.1.3 $domain tag count $tagCount does not match entry count $entryCount",
            )
        }

        return List(entryCount) { index ->
            val tag = tagAt(index)
            if (!validTag(tag)) {
                throw OfficialChirV113FormatException(
                    "Official CHIR v1.1.3 $domain entry ${index + 1} has unsupported union tag $tag",
                )
            }
            OfficialChirV113TableReference(domain, index.toUInt() + 1u, tag)
        }
    }

    private fun validateRootTable(buffer: ByteBuffer) {
        if (buffer.limit() < MINIMUM_FLATBUFFER_SIZE) {
            throw OfficialChirV113FormatException(
                "Official CHIR v1.1.3 payload is shorter than a FlatBuffers root header",
            )
        }

        val rootPosition = buffer.getInt(buffer.position()) + buffer.position()
        if (rootPosition < buffer.position() + Int.SIZE_BYTES ||
            rootPosition > buffer.limit() - Int.SIZE_BYTES
        ) {
            throw OfficialChirV113FormatException(
                "Official CHIR v1.1.3 payload has an out-of-range FlatBuffers root offset",
            )
        }

        val vtablePosition = rootPosition.toLong() - buffer.getInt(rootPosition).toLong()
        if (vtablePosition < 0 || vtablePosition > buffer.limit() - 2L * Short.SIZE_BYTES) {
            throw OfficialChirV113FormatException(
                "Official CHIR v1.1.3 payload has an out-of-range root vtable offset",
            )
        }

        val vtableStart = vtablePosition.toInt()
        val vtableLength = buffer.getShort(vtableStart).toUShort().toInt()
        val objectLength = buffer.getShort(vtableStart + Short.SIZE_BYTES).toUShort().toInt()
        if (vtableLength < 2 * Short.SIZE_BYTES || objectLength < Int.SIZE_BYTES ||
            vtablePosition + vtableLength > buffer.limit() ||
            rootPosition.toLong() + objectLength > buffer.limit()
        ) {
            throw OfficialChirV113FormatException(
                "Official CHIR v1.1.3 payload has an invalid root table or vtable length",
            )
        }
    }

    private fun decodePhase(value: UByte): OfficialChirV113Phase =
        OfficialChirV113Phase.entries.singleOrNull { it.wireValue == value }
            ?: throw OfficialChirV113FormatException(
                "Official CHIR v1.1.3 package has unsupported phase $value",
            )

    private fun decodeAccessLevel(value: UByte): OfficialChirV113PackageAccessLevel =
        OfficialChirV113PackageAccessLevel.entries.singleOrNull { it.wireValue == value }
            ?: throw OfficialChirV113FormatException(
                "Official CHIR v1.1.3 package has unsupported access level $value",
            )
}

class OfficialChirV113FormatException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)
