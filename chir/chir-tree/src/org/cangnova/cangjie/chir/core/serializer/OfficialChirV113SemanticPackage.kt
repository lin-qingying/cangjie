package org.cangnova.cangjie.chir.core.serializer

import org.cangnova.cangjie.chir.official.v113.PackageFormatV113.CHIRPackage
import org.cangnova.cangjie.chir.official.v113.PackageFormatV113.CHIRPackageT
import java.lang.reflect.Array as ReflectArray
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Collections
import java.util.IdentityHashMap

/**
 * 官方 v1.1.3 表在仓内语义图中的无损表示。
 *
 * 顶层表保留官方独立的索引域和 1-based ID；嵌套表、union、字段值与向量都从对应版本 schema
 * 解包，不把 FlatBuffers 运行时表对象暴露给 CHIR 链接器或后端。
 */
class OfficialChirV113SemanticPackage internal constructor(
    val metadata: OfficialChirV113PackageMetadata,
    val types: List<OfficialChirV113IndexedTable>,
    val values: List<OfficialChirV113IndexedTable>,
    val expressions: List<OfficialChirV113IndexedTable>,
    val definitions: List<OfficialChirV113IndexedTable>,
) {
    val tableCount: Int
        get() = types.size + values.size + expressions.size + definitions.size
}

/** 一个官方 union 向量中的完整类型化表及其域内 ID。 */
data class OfficialChirV113IndexedTable(
    val domain: OfficialChirV113TableDomain,
    val id: UInt,
    val unionTag: UByte,
    val table: OfficialChirV113SemanticTable,
)

/** 官方 schema 表的不可变字段视图。 */
data class OfficialChirV113SemanticTable(
    val schemaType: String,
    val fields: Map<String, OfficialChirV113SemanticValue>,
)

/** 官方 CHIR 表字段值；表与 union 在图中保留为不同节点种类。 */
sealed interface OfficialChirV113SemanticValue {
    data object NullValue : OfficialChirV113SemanticValue

    data class Scalar(val value: Any) : OfficialChirV113SemanticValue

    data class Bytes(private val value: ByteArray) : OfficialChirV113SemanticValue {
        fun toByteArray(): ByteArray = value.copyOf()
    }

    data class Reference(
        val domain: OfficialChirV113TableDomain,
        val id: Long,
    ) : OfficialChirV113SemanticValue

    data class Sequence(val values: List<OfficialChirV113SemanticValue>) : OfficialChirV113SemanticValue

    data class Table(val value: OfficialChirV113SemanticTable) : OfficialChirV113SemanticValue

    data class Union(
        val discriminant: Int,
        val variant: String,
        val value: OfficialChirV113SemanticTable,
    ) : OfficialChirV113SemanticValue
}

/**
 * 将官方 v1.1.3 FlatBuffers 图导入仓内的不可变语义图。
 *
 * 根表和顶层向量边界先由 wire decoder 检查；之后使用同一版本 schema 生成的 object API 解包全部
 * 嵌套表，再复制到不依赖生成类的语义模型。任何解包或复制失败都会在返回包之前拒绝输入。
 */
object OfficialChirV113SemanticPackageDecoder {
    fun decode(bytes: ByteArray): OfficialChirV113SemanticPackage {
        val stableBytes = bytes.copyOf()
        OfficialChirV113StructureVerifier.verify(stableBytes)
        val wirePackage = OfficialChirV113WirePackageDecoder.decode(stableBytes)
        return try {
            val buffer = ByteBuffer.wrap(stableBytes).order(ByteOrder.LITTLE_ENDIAN)
            val decoded = CHIRPackage.getRootAsCHIRPackage(buffer).unpack()
            import(decoded, wirePackage)
        } catch (exception: OfficialChirV113FormatException) {
            throw exception
        } catch (exception: Exception) {
            throw OfficialChirV113FormatException(
                "Official CHIR v1.1.3 package contains an invalid nested table graph: " +
                    (exception.message ?: exception.javaClass.simpleName),
                exception,
            )
        }
    }

    private fun import(
        decoded: CHIRPackageT,
        wirePackage: OfficialChirV113WirePackage,
    ): OfficialChirV113SemanticPackage {
        val types = importUnionVector(
            decoded = decoded.types,
            references = wirePackage.typeReferences,
            domain = OfficialChirV113TableDomain.TYPE,
        )
        val values = importUnionVector(
            decoded = decoded.values,
            references = wirePackage.valueReferences,
            domain = OfficialChirV113TableDomain.VALUE,
        )
        val expressions = importUnionVector(
            decoded = decoded.exprs,
            references = wirePackage.expressionReferences,
            domain = OfficialChirV113TableDomain.EXPRESSION,
        )
        val definitions = importUnionVector(
            decoded = decoded.defs,
            references = wirePackage.definitionReferences,
            domain = OfficialChirV113TableDomain.DEFINITION,
        )

        val semanticPackage = OfficialChirV113SemanticPackage(
            metadata = wirePackage.metadata,
            types = types,
            values = values,
            expressions = expressions,
            definitions = definitions,
        )
        validateReferences(semanticPackage)
        return semanticPackage
    }

    private fun importUnionVector(
        decoded: Array<*>?,
        references: List<OfficialChirV113TableReference>,
        domain: OfficialChirV113TableDomain,
    ): List<OfficialChirV113IndexedTable> {
        if (decoded == null && references.isNotEmpty()) {
            throw OfficialChirV113FormatException("Official CHIR v1.1.3 $domain vector was lost during import")
        }
        if ((decoded?.size ?: 0) != references.size) {
            throw OfficialChirV113FormatException("Official CHIR v1.1.3 $domain vector changed size during import")
        }

        return Collections.unmodifiableList(
            references.mapIndexed { index, reference ->
                val union = checkNotNull(decoded?.get(index)) {
                    "Official CHIR v1.1.3 $domain entry ${index + 1} has no union payload"
                }
                val discriminant = invokeGetter(union, "getType") as? Number
                    ?: throw OfficialChirV113FormatException(
                        "Official CHIR v1.1.3 $domain entry ${index + 1} has no numeric union discriminant",
                    )
                if (discriminant.toInt().toUByte() != reference.unionTag) {
                    throw OfficialChirV113FormatException(
                        "Official CHIR v1.1.3 $domain entry ${index + 1} changed union tag during import",
                    )
                }
                val payload = invokeGetter(union, "getValue")
                    ?: throw OfficialChirV113FormatException(
                        "Official CHIR v1.1.3 $domain entry ${index + 1} has no table payload",
                    )
                val table = importTable(payload, IdentityHashMap())
                OfficialChirV113IndexedTable(
                    domain = domain,
                    id = reference.id,
                    unionTag = reference.unionTag,
                    table = table,
                )
            },
        )
    }

    private fun importValue(
        value: Any?,
        activeTables: IdentityHashMap<Any, Unit>,
        referenceDomain: OfficialChirV113TableDomain? = null,
    ): OfficialChirV113SemanticValue = when {
        value == null -> OfficialChirV113SemanticValue.NullValue
        value is ByteArray -> {
            if (referenceDomain != null) {
                throw OfficialChirV113FormatException("Official CHIR v1.1.3 reference field has a byte vector")
            }
            OfficialChirV113SemanticValue.Bytes(value.copyOf())
        }
        value.javaClass.isArray -> {
            val values = List(ReflectArray.getLength(value)) { index ->
                importValue(ReflectArray.get(value, index), activeTables, referenceDomain)
            }
            OfficialChirV113SemanticValue.Sequence(Collections.unmodifiableList(values))
        }
        referenceDomain != null && value is Number -> OfficialChirV113SemanticValue.Reference(
            domain = referenceDomain,
            id = value.toLong(),
        )
        referenceDomain != null -> throw OfficialChirV113FormatException(
            "Official CHIR v1.1.3 reference field has a non-numeric value",
        )
        value.javaClass.simpleName.endsWith("Union") -> {
            val discriminant = (invokeGetter(value, "getType") as? Number)?.toInt()
                ?: throw OfficialChirV113FormatException("Official CHIR v1.1.3 nested union has no discriminant")
            val payload = invokeGetter(value, "getValue")
                ?: return OfficialChirV113SemanticValue.Union(discriminant, "NONE", emptyTable("NONE"))
            OfficialChirV113SemanticValue.Union(
                discriminant = discriminant,
                variant = schemaType(payload),
                value = importTable(payload, activeTables),
            )
        }
        value.javaClass.simpleName.endsWith("T") -> {
            OfficialChirV113SemanticValue.Table(importTable(value, activeTables))
        }
        else -> OfficialChirV113SemanticValue.Scalar(value)
    }

    private fun importTable(
        source: Any,
        activeTables: IdentityHashMap<Any, Unit>,
    ): OfficialChirV113SemanticTable {
        if (activeTables.put(source, Unit) != null) {
            throw OfficialChirV113FormatException(
                "Official CHIR v1.1.3 nested table graph contains a cycle at ${schemaType(source)}",
            )
        }
        try {
            val fields = LinkedHashMap<String, OfficialChirV113SemanticValue>()
            propertyGetters(source.javaClass).forEach { (propertyName, getter) ->
                val value = try {
                    getter.invoke(source)
                } catch (exception: ReflectiveOperationException) {
                    throw OfficialChirV113FormatException(
                        "Could not import ${schemaType(source)}.$propertyName",
                        exception,
                    )
                }
                fields[propertyName] = importValue(
                    value,
                    activeTables,
                    OfficialChirV113ReferenceSemantics.referenceDomain(schemaType(source), propertyName),
                )
            }
            return OfficialChirV113SemanticTable(
                schemaType = schemaType(source),
                fields = Collections.unmodifiableMap(fields),
            )
        } finally {
            activeTables.remove(source)
        }
    }

    private fun propertyGetters(type: Class<*>): List<Pair<String, Method>> {
        val schemaType = type.simpleName.removeSuffix("T")
        return OfficialChirV113StructureVerifier.tableFieldNames(schemaType).map { fieldName ->
            val accessorName = "get${fieldName.replaceFirstChar(Char::uppercaseChar)}"
            val getter = type.methods.singleOrNull { method ->
                !Modifier.isStatic(method.modifiers) &&
                    method.parameterCount == 0 &&
                    method.returnType != Void.TYPE &&
                    method.name.equals(accessorName, ignoreCase = true)
            } ?: throw OfficialChirV113FormatException(
                "Generated official CHIR table ${type.name} has no accessor for schema field $fieldName",
            )
            fieldName to getter
        }
    }

    private fun invokeGetter(receiver: Any, name: String): Any? {
        val getter = receiver.javaClass.methods.singleOrNull {
            it.name == name && it.parameterCount == 0 && !Modifier.isStatic(it.modifiers)
        } ?: throw OfficialChirV113FormatException(
            "Generated official CHIR union ${receiver.javaClass.name} has no $name accessor",
        )
        return try {
            getter.invoke(receiver)
        } catch (exception: ReflectiveOperationException) {
            throw OfficialChirV113FormatException("Could not read official CHIR union $name", exception)
        }
    }

    private fun schemaType(value: Any): String = value.javaClass.simpleName.removeSuffix("T")

    private fun emptyTable(type: String) = OfficialChirV113SemanticTable(type, emptyMap())

    private fun validateReferences(semanticPackage: OfficialChirV113SemanticPackage) {
        val tablesByDomain = mapOf(
            OfficialChirV113TableDomain.TYPE to semanticPackage.types,
            OfficialChirV113TableDomain.VALUE to semanticPackage.values,
            OfficialChirV113TableDomain.EXPRESSION to semanticPackage.expressions,
            OfficialChirV113TableDomain.DEFINITION to semanticPackage.definitions,
        )
        val domainSizes = tablesByDomain.mapValues { it.value.size.toLong() }
        tablesByDomain.forEach { (domain, indexedTables) ->
            indexedTables.forEach { indexed ->
                validateTopLevelIdentity(indexed, domain)
                validateTableReferences(indexed.table, domainSizes, "${domain.name}#${indexed.id}")
            }
        }
        semanticPackageMetadataReferences(semanticPackage, domainSizes)
    }

    private fun validateTopLevelIdentity(
        indexed: OfficialChirV113IndexedTable,
        domain: OfficialChirV113TableDomain,
    ) {
        val (identityType, identityField) = OfficialChirV113ReferenceSemantics.identityField(domain)
        val matches = buildList {
            fun visit(table: OfficialChirV113SemanticTable) {
                if (table.schemaType == identityType) add(table)
                table.fields.values.forEach { value ->
                    when (value) {
                        is OfficialChirV113SemanticValue.Table -> visit(value.value)
                        is OfficialChirV113SemanticValue.Union -> visit(value.value)
                        is OfficialChirV113SemanticValue.Sequence -> value.values.forEach { nested ->
                            when (nested) {
                                is OfficialChirV113SemanticValue.Table -> visit(nested.value)
                                is OfficialChirV113SemanticValue.Union -> visit(nested.value)
                                else -> Unit
                            }
                        }
                        is OfficialChirV113SemanticValue.Bytes,
                        OfficialChirV113SemanticValue.NullValue,
                        is OfficialChirV113SemanticValue.Reference,
                        is OfficialChirV113SemanticValue.Scalar,
                        -> Unit
                    }
                }
            }
            visit(indexed.table)
        }
        if (matches.size != 1) {
            throw OfficialChirV113FormatException(
                "Official CHIR v1.1.3 $domain entry ${indexed.id} contains ${matches.size} $identityType identity tables",
            )
        }
        val id = matches.single().fields[identityField] as? OfficialChirV113SemanticValue.Reference
            ?: throw OfficialChirV113FormatException(
                "Official CHIR v1.1.3 $domain entry ${indexed.id} has no typed $identityField identity",
            )
        if (id.domain != domain || id.id != indexed.id.toLong()) {
            throw OfficialChirV113FormatException(
                "Official CHIR v1.1.3 $domain entry ${indexed.id} disagrees with its $identityField value ${id.id}",
            )
        }
    }

    private fun validateTableReferences(
        table: OfficialChirV113SemanticTable,
        domainSizes: Map<OfficialChirV113TableDomain, Long>,
        path: String,
    ) {
        table.fields.forEach { (field, value) ->
            validateValueReferences(value, domainSizes, "$path.${table.schemaType}.$field")
        }
    }

    private fun validateValueReferences(
        value: OfficialChirV113SemanticValue,
        domainSizes: Map<OfficialChirV113TableDomain, Long>,
        path: String,
    ) {
        when (value) {
            is OfficialChirV113SemanticValue.Reference -> {
                val size = checkNotNull(domainSizes[value.domain])
                if (value.id < 0L || value.id > 0xFFFF_FFFFL || value.id > size) {
                    throw OfficialChirV113FormatException(
                        "Official CHIR v1.1.3 reference ${value.domain}#${value.id} is out of range at $path",
                    )
                }
            }

            is OfficialChirV113SemanticValue.Table -> validateTableReferences(value.value, domainSizes, path)
            is OfficialChirV113SemanticValue.Union -> validateTableReferences(value.value, domainSizes, path)
            is OfficialChirV113SemanticValue.Sequence -> value.values.forEachIndexed { index, nested ->
                validateValueReferences(nested, domainSizes, "$path[$index]")
            }

            is OfficialChirV113SemanticValue.Bytes,
            OfficialChirV113SemanticValue.NullValue,
            is OfficialChirV113SemanticValue.Scalar,
            -> Unit
        }
    }

    private fun semanticPackageMetadataReferences(
        semanticPackage: OfficialChirV113SemanticPackage,
        domainSizes: Map<OfficialChirV113TableDomain, Long>,
    ) {
        listOf(
            OfficialChirV113TableDomain.VALUE to semanticPackage.metadata.packageInitFunctionId,
            OfficialChirV113TableDomain.VALUE to semanticPackage.metadata.packageLiteralInitFunctionId,
        ).forEach { (domain, id) ->
            if (id.toLong() > checkNotNull(domainSizes[domain])) {
                throw OfficialChirV113FormatException(
                    "Official CHIR v1.1.3 package initialization reference $domain#$id is out of range",
                )
            }
        }
    }
}
