package org.cangnova.cangjie.cfir.serialization.cjd

import PackageFormat.*
import PackageFormat.Package
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext
import org.cangnova.cangjie.cfir.serialization.deserialize.ResolvedFullId
import org.cangnova.cangjie.metadata.model.Attribute

/** 只读取原始 CJO 类型表；每个包上下文拥有独立实例，绝不调用 deserializeDecl。 */
interface CjdBinaryTypeAdapter {
    /** 字段使用 1-based 编码；0 与现有反序列化器一致表示 Unit。 */
    fun typeFromField(field: UInt): CjdResolvedType

    companion object {
        fun create(context: CfirDeserializationContext): CjdBinaryTypeAdapter = create(context.pkg) { id ->
            val resolved = context.fullIdResolver.resolve(id) as? ResolvedFullId.Declaration
            resolved?.let {
                CjdBinaryTypeTarget(it.declaration.name.asString(), it.packageIndex.packageFqName.asString(),
                    it.declaration.decl.cjdBaseExported())
            }
        }

        /** 原始包入口便于二进制单测；跨包名必须由 FullId 解析器提供，禁止猜测 exportId 文本。 */
        fun create(
            pkg: Package,
            resolveTarget: (FullId) -> CjdBinaryTypeTarget? = { id ->
                if (id.pkgId == -2) {
                    cjdDeclIndex(pkg, id.index)?.let(pkg::allDecls)?.let { decl ->
                        decl.identifier?.let { CjdBinaryTypeTarget(it, decl.fullPkgName ?: pkg.fullPkgName.orEmpty(), decl.cjdBaseExported()) }
                    }
                } else null
            },
        ): CjdBinaryTypeAdapter = CjdBinaryTypeAdapterImpl(pkg, resolveTarget)
    }
}

/** [CjdBinaryTypeAdapter] 的默认实现；带缓存与递归检测的 SemaTy → 结构类型转换器。 */
private class CjdBinaryTypeAdapterImpl(
    private val pkg: Package,
    private val resolveTarget: (FullId) -> CjdBinaryTypeTarget?,
) : CjdBinaryTypeAdapter {
    private val cache = mutableMapOf<UInt, CjdResolvedType>()
    private val visiting = mutableSetOf<UInt>()

    @Synchronized
    override fun typeFromField(field: UInt): CjdResolvedType {
        if (field == 0u) return CjdResolvedType.Primitive("Unit")
        cache[field]?.let { return it }
        if (field == UInt.MAX_VALUE || field > pkg.allTypesLength.toUInt()) return CjdResolvedType.Unknown("Invalid type index $field")
        if (!visiting.add(field)) return CjdResolvedType.Unknown("Recursive type index $field")
        return try {
            val ty = pkg.allTypes(field.toInt() - 1)
            val result = ty?.let(::convert) ?: CjdResolvedType.Unknown("Missing type $field")
            cache[field] = result
            result
        } finally {
            visiting.remove(field)
        }
    }

    private fun convert(ty: SemaTy): CjdResolvedType {
        val primitive = when (ty.kind) {
            TypeKind.Unit -> "Unit"
            TypeKind.Int8 -> "Int8"
            TypeKind.Int16 -> "Int16"
            TypeKind.Int32 -> "Int32"
            TypeKind.Int64 -> "Int64"
            TypeKind.IntNative -> "IntNative"
            TypeKind.UInt8 -> "UInt8"
            TypeKind.UInt16 -> "UInt16"
            TypeKind.UInt32 -> "UInt32"
            TypeKind.UInt64 -> "UInt64"
            TypeKind.UIntNative -> "UIntNative"
            TypeKind.Float16 -> "Float16"
            TypeKind.Float32 -> "Float32"
            TypeKind.Float64 -> "Float64"
            TypeKind.Rune -> "Rune"
            TypeKind.Nothing -> "Nothing"
            TypeKind.Bool -> "Bool"
            else -> null
        }
        if (primitive != null) return CjdResolvedType.Primitive(primitive)
        val args = List(ty.typeArgsLength) { typeFromField(ty.typeArgs(it)) }
        return when (ty.kind) {
            TypeKind.Class, TypeKind.Struct, TypeKind.Enum, TypeKind.Interface -> {
                val info = if (ty.infoType == SemaTyInfo.CompositeTyInfo) ty.info(CompositeTyInfo()) as? CompositeTyInfo else null
                val target = info?.declPtr?.let(resolveTarget)
                    ?: return CjdResolvedType.Unknown("Unresolved composite FullId")
                CjdResolvedType.Named(target.name, args, target,
                    ty.kind == TypeKind.Enum && target.packageName == "std.core" && target.name == "Option")
            }
            TypeKind.Generic -> {
                val info = if (ty.infoType == SemaTyInfo.GenericTyInfo) ty.info(GenericTyInfo()) as? GenericTyInfo else null
                val target = info?.declPtr?.let(resolveTarget)
                    ?: return CjdResolvedType.Unknown("Unresolved generic FullId")
                CjdResolvedType.Named(target.name, args, target)
            }
            TypeKind.Func -> {
                val info = if (ty.infoType == SemaTyInfo.FuncTyInfo) ty.info(FuncTyInfo()) as? FuncTyInfo else null
                info?.let { CjdResolvedType.Function(args, typeFromField(it.retType)) }
                    ?: CjdResolvedType.Unknown("Missing FuncTyInfo")
            }
            TypeKind.Tuple -> CjdResolvedType.Tuple(args)
            TypeKind.VArray -> {
                val info = if (ty.infoType == SemaTyInfo.ArrayTyInfo) ty.info(ArrayTyInfo()) as? ArrayTyInfo else null
                if (info != null && args.size == 1) CjdResolvedType.VArray(args.single(), info.dimsOrSize)
                else CjdResolvedType.Unknown("Missing VArray element or size")
            }
            TypeKind.Array -> CjdResolvedType.Named("Array", args)
            TypeKind.CPointer -> CjdResolvedType.Named("CPointer", args)
            TypeKind.CString -> CjdResolvedType.Named("CString", args)
            else -> CjdResolvedType.Unknown("Unsupported type kind ${ty.kind}; aliases are not expanded")
        }
    }
}

/** 把 1-based 声明引用解码为 allDecls 下标；0、越界与哨兵值均视为无效返回 `null`。 */
internal fun cjdDeclIndex(pkg: Package, field: UInt): Int? =
    if (field == 0u || field == UInt.MAX_VALUE || field > pkg.allDeclsLength.toUInt()) null else field.toInt() - 1

/** 判断声明属性位图中是否包含给定 [attribute]；位图按 64 位字组织。 */
internal fun Decl.cjdHasAttribute(attribute: Attribute): Boolean {
    val bit = attribute.ordinal
    return bit / 64 < attributesLength && (attributes(bit / 64).toLong() ushr (bit % 64)) and 1L != 0L
}

/** CJO 不保存 noSubPkg；遵循官方 imported decl 无 curFile 时 internal 可导出的分支。 */
internal fun Decl.cjdBaseExported(): Boolean =
    cjdHasAttribute(Attribute.PUBLIC) || cjdHasAttribute(Attribute.PROTECTED) || cjdHasAttribute(Attribute.INTERNAL) ||
        !cjdHasAttribute(Attribute.PRIVATE)
