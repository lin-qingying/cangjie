package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.DeclKind
import org.cangnova.cangjie.metadata.model.Attribute

/**
 * 为源码 CJO 的整个声明图发布对称的跨包引用身份。
 *
 * 该协议不是 native ABI mangling。键由目标声明持有，包含逻辑 owner 和规范化签名，
 * 不依赖声明表顺序、类型参数名称或源码偏移；同目标重复 extend 的 private 成员
 * 按官方规则保留独立的文件/词法作用域次序。读侧只读取已经发布的 exportId。
 * 已加载 CJO 的键保持原样，不能在重导出时重新归属或重算。
 */
internal class CjoSourceDeclarationKeys(
    private val packageName: String,
    private val declarations: List<CjoPackageDeclaration>,
    private val types: List<CjoTypeMetadata>,
    private val imports: List<String>,
    private val sourceFiles: Map<Int, String>,
    private val sourceOffsets: Map<Int, Int>,
    private val sourceDeclarations: Set<Int>,
    private val owners: List<Int?>,
    /** SOURCE正文局部定义只按本包d索引引用，不发布跨包export key。 */
    private val localDeclarations: Set<Int> = emptySet(),
) {
    private data class HelperOwner(val function: Int, val parameterList: Int, val parameter: Int)

    private val keys = mutableMapOf<Int, String>()
    private val activeDeclarations = mutableSetOf<Int>()
    private val helperOwners = mutableMapOf<Int, HelperOwner>()
    private val typeKeys = mutableMapOf<Pair<UInt, Int>, String>()
    private val activeTypes = mutableSetOf<Pair<UInt, Int>>()
    private val privateExtendOccurrences = mutableMapOf<Int, Int>()

    init {
        require(owners.size == declarations.size)
        declarations.forEachIndexed { functionIndex, declaration ->
            val body = (declaration.info as? CjoFunctionInfo)?.body ?: return@forEachIndexed
            body.desugaredParameterLists.forEachIndexed { listIndex, parameters ->
                parameters.forEachIndexed helperLoop@{ parameterIndex, helper ->
                    if (helper == 0u) return@helperLoop
                    val index = declarationIndex(helper)
                    val owner = HelperOwner(functionIndex, listIndex, parameterIndex)
                    val previous = helperOwners.putIfAbsent(index, owner)
                    check(previous == null || previous == owner) {
                        "A CJO default-parameter helper must have one logical function/parameter owner"
                    }
                }
            }
        }
        sourceDeclarations.filter { declarations[it].kind == DeclKind.ExtendDecl }
            .groupBy { index ->
                Triple(sourceFile(index), typeKey(declarations[index].type, index), genericConstraints(index))
            }
            .values.forEach { extensions ->
                extensions.sortedBy { checkNotNull(sourceOffsets[it]) { "A source extend must retain its lexical position" } }
                    .forEachIndexed { occurrence, index -> privateExtendOccurrences[index] = occurrence }
            }
    }

    /** 一次发布全部源码声明键，并验证它们没有与其他输出声明占用相同身份。 */
    fun publish(): List<CjoPackageDeclaration> {
        val published = sourceDeclarations.filterTo(mutableSetOf(), ::needsExportKey)
        val result = declarations.mapIndexed { index, declaration ->
            if (index !in published) declaration
            else declaration.copy(exportId = declarationKey(index))
        }
        val occupied = mutableMapOf<String, Int>()
        result.forEachIndexed { index, declaration ->
            if (index in localDeclarations) return@forEachIndexed
            val key = CjoFullIdReferenceKey.fromDeclarationMetadata(declaration) ?: return@forEachIndexed
            val previous = occupied.putIfAbsent(key, index)
            check(previous == null || (index !in published && previous !in published)) {
                "CJO declaration reference key collision: ${result[previous!!].identifier} and ${declaration.identifier}"
            }
        }
        return result
    }

    // 官方 ExtendDecl 自身没有跨包导出身份；其成员使用逻辑扩展目标而不是声明表位置。
    private fun needsExportKey(index: Int): Boolean =
        declarations[index].kind != DeclKind.ExtendDecl && index !in localDeclarations

    private fun declarationKey(index: Int): String {
        keys[index]?.let { return it }
        val declaration = declarations[index]
        if (index !in sourceDeclarations) {
            return checkNotNull(CjoFullIdReferenceKey.fromDeclarationMetadata(declaration)) {
                "An imported CJO declaration must retain its original reference key"
            }
        }
        check(activeDeclarations.add(index)) { "Recursive CJO declaration export identity: ${declaration.identifier}" }
        try {
            val helper = helperOwners[index]
            val key = when {
                helper != null -> node(
                    "default-helper", declarationKey(helper.function),
                    helper.parameterList.toString(), helper.parameter.toString(),
                )
                declaration.kind == DeclKind.GenericParamDecl -> {
                    val owner = checkNotNull(owners[index]) { "A CJO generic parameter must have a lexical owner" }
                    val ordinal = declarations[owner].generic?.typeParameters?.indexOf(index.toUInt() + 1u) ?: -1
                    check(ordinal >= 0) { "A CJO generic parameter must occur in its owner's generic table" }
                    val lexicalOwner = if (declarations[owner].kind == DeclKind.ExtendDecl) {
                        extendScope(owner, privateMember = true)
                    } else declarationKey(owner)
                    node("generic-parameter", lexicalOwner, ordinal.toString())
                }
                declaration.kind == DeclKind.FuncParam -> {
                    val owner = checkNotNull(owners[index]) { "A CJO value parameter must have a function owner" }
                    val body = (declarations[owner].info as? CjoFunctionInfo)?.body
                        ?: error("A CJO value parameter owner must be a function")
                    val coordinates = body.parameterLists.flatMapIndexed { listIndex, parameters ->
                        parameters.mapIndexedNotNull { parameterIndex, reference ->
                            if (reference == index.toUInt() + 1u) listIndex to parameterIndex else null
                        }
                    }.single()
                    node("value-parameter", declarationKey(owner), coordinates.first.toString(), coordinates.second.toString())
                }
                declaration.kind == DeclKind.ExtendDecl -> extendScope(index, privateMember = false)
                declaration.kind in nominalKinds -> nominalKey(index)
                else -> node(
                    "CJO1", ownerKey(index), declaration.kind.toString(), declaration.identifier,
                    privateFileScope(index), signatureFlags(declaration), typeKey(declaration.type, index),
                    genericConstraints(index), parameterLabels(declaration),
                )
            }
            keys[index] = key
            return key
        } finally {
            activeDeclarations.remove(index)
        }
    }

    private fun nominalKey(index: Int): String {
        val declaration = declarations[index]
        return node(
            "CJO1", ownerKey(index), declaration.kind.toString(), declaration.identifier,
            privateFileScope(index), (declaration.generic?.typeParameters?.size ?: 0).toString(),
        )
    }

    private fun ownerKey(index: Int): String {
        val owner = owners[index] ?: return node("package", declarations[index].fullPackageName ?: packageName)
        return if (declarations[owner].kind == DeclKind.ExtendDecl) {
            extendScope(owner, hasAttribute(declarations[index], Attribute.PRIVATE))
        } else declarationKey(owner)
    }

    private fun extendScope(index: Int, privateMember: Boolean): String = node(
        "extend", node("package", declarations[index].fullPackageName ?: packageName),
        typeKey(declarations[index].type, index), genericConstraints(index),
        if (privateMember) sourceFile(index) else "",
        if (privateMember) checkNotNull(privateExtendOccurrences[index]).toString() else "",
    )

    private fun privateFileScope(index: Int): String {
        if (!hasAttribute(declarations[index], Attribute.PRIVATE)) return ""
        val owner = owners[index]
        return if (owner == null || declarations[owner].kind == DeclKind.ExtendDecl) sourceFile(index) else ""
    }

    private fun sourceFile(index: Int): String {
        return checkNotNull(sourceFiles[index]) { "A source declaration must retain its physical file identity" }
    }

    private fun genericConstraints(index: Int): String {
        val generic = declarations[index].generic ?: return node("generic", "0")
        val constraints = generic.constraints.map { constraint ->
            node("constraint", typeKey(constraint.type, index), *constraint.upperBounds.map { typeKey(it, index) }.sorted().toTypedArray())
        }.sorted()
        return node("generic", generic.typeParameters.size.toString(), *constraints.toTypedArray())
    }

    private fun parameterLabels(declaration: CjoPackageDeclaration): String {
        val body = (declaration.info as? CjoFunctionInfo)?.body ?: return ""
        return node("parameter-lists", *body.parameterLists.map { parameters ->
            node("parameters", *parameters.map { reference ->
                val parameter = declarations[declarationIndex(reference)]
                val info = parameter.info as? CjoParameterInfo ?: error("A CJO function parameter requires ParamInfo")
                node("parameter", info.isNamed.toString(), info.isMember.toString(), if (info.isNamed) parameter.identifier else "")
            }.toTypedArray())
        }.toTypedArray())
    }

    private fun typeKey(reference: UInt, context: Int): String {
        if (reference == 0u) return node("unit")
        val cacheKey = reference to context
        typeKeys[cacheKey]?.let { return it }
        check(activeTypes.add(cacheKey)) { "Recursive CJO signature type" }
        try {
            val type = types[declarationIndex(reference)]
            val infoKey = when (val info = type.semanticInfo) {
                null -> ""
                is CjoGenericTypeInfoMetadata -> {
                    check(info.packageId == PackageIndex.CURRENT.value) { "A source signature generic binder must belong to the output package" }
                    genericParameterCoordinate(info.declarationIndex, context)
                }
                is CjoCompositeTypeInfoMetadata -> {
                    if (info.packageId == PackageIndex.CURRENT.value) {
                        val targetIndex = declarationIndex(info.declarationIndex)
                        val target = declarations[targetIndex]
                        check(target.kind in nominalKinds) { "A CJO nominal type must refer to a nominal declaration" }
                        node("nominal", target.fullPackageName ?: packageName, declarationKey(targetIndex))
                    } else {
                        check(PackageIndex.isImportedPackage(info.packageId)) { "A CJO nominal signature requires a declaration package" }
                        node("nominal", imports[info.packageId], checkNotNull(info.declarationKey))
                    }
                }
                is CjoArrayTypeInfoMetadata -> node("array", info.dimsOrSize.toString())
                is CjoFunctionTypeInfoMetadata -> node(
                    "function", info.isC.toString(), info.hasVariableLenArg.toString(), typeKey(info.returnType, context),
                )
            }
            return node("type", type.kind.toString(), infoKey, *type.typeArguments.map { typeKey(it, context) }.toTypedArray())
                .also { typeKeys[cacheKey] = it }
        } finally {
            activeTypes.remove(cacheKey)
        }
    }

    private fun genericParameterCoordinate(reference: UInt, context: Int): String {
        var scope: Int? = context
        var depth = 0
        while (scope != null) {
            val ordinal = declarations[scope].generic?.typeParameters?.indexOf(reference) ?: -1
            if (ordinal >= 0) return node("binder", depth.toString(), ordinal.toString())
            scope = owners[scope]
            depth++
        }
        error("A CJO signature type parameter must be declared in its lexical owner chain")
    }

    private fun signatureFlags(declaration: CjoPackageDeclaration): String = node(
        "flags", *signatureAttributes.filter { hasAttribute(declaration, it) }.map { it.ordinal.toString() }.toTypedArray(),
    )

    private fun hasAttribute(declaration: CjoPackageDeclaration, attribute: Attribute): Boolean =
        (declaration.attributes.getOrElse(attribute.ordinal / ULong.SIZE_BITS) { 0uL } and
            (1uL shl (attribute.ordinal % ULong.SIZE_BITS))) != 0uL

    private fun declarationIndex(reference: UInt): Int {
        require(reference > 0u) { "A CJO declaration/type/file reference must be one-based" }
        return (reference - 1u).toInt()
    }

    private fun node(tag: String, vararg fields: String): String = buildString {
        append(tag).append('[')
        fields.forEach { append(it.length).append(':').append(it) }
        append(']')
    }

    private companion object {
        val nominalKinds = setOf(
            DeclKind.ClassDecl, DeclKind.InterfaceDecl, DeclKind.StructDecl,
            DeclKind.EnumDecl, DeclKind.TypeAliasDecl, DeclKind.BuiltInDecl,
        )
        val signatureAttributes = listOf(
            Attribute.STATIC, Attribute.CONSTRUCTOR, Attribute.PRIMARY_CONSTRUCTOR,
            Attribute.ENUM_CONSTRUCTOR, Attribute.C, Attribute.MUT, Attribute.OPERATOR,
        )
    }
}
