@file:OptIn(ExperimentalUnsignedTypes::class)

package org.cangnova.cangjie.cfir.serialization.cjo

import PackageFormat.DeclKind
import PackageFormat.Package
import PackageFormat.TypeKind
import org.cangnova.cangjie.metadata.model.Attribute
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** 用声明图的语义关系验证源码引用身份，不依赖键的具体字符串编码。 */
class CjoSourceDeclarationKeysTest {
    @Test
    fun `reordering declaration and type tables preserves published keys`() {
        val graph = genericOwnersGraph()
        val expected = graph.publish()

        assertEquals(expected, graph.publish(reverseDeclarations = true))
        assertEquals(expected, graph.publish(reverseTypes = true))
        assertEquals(expected, graph.publish(reverseDeclarations = true, reverseTypes = true))
    }

    @Test
    fun `generic alpha renaming preserves callable binder and value parameter keys`() {
        val original = genericFunctionGraph(parameterName = "T").publish()
        val renamed = genericFunctionGraph(parameterName = "Renamed").publish(reverseDeclarations = true, reverseTypes = true)

        assertEquals(original, renamed)
        assertNotEquals(original.getValue("function"), original.getValue("type-parameter"))
        assertNotEquals(original.getValue("type-parameter"), original.getValue("value-parameter"))
    }

    @Test
    fun `same named generic parameters belong to distinct nominal owners`() {
        val keys = genericOwnersGraph().publish()

        assertNotEquals(keys.getValue("holder-parameter"), keys.getValue("other-parameter"))
        assertNotEquals(keys.getValue("holder-parameter"), keys.getValue("class-T"))
        assertNotEquals(keys.getValue("other-parameter"), keys.getValue("class-T"))
        assertEquals(keys, genericOwnersGraph(parameterName = "U").publish())
    }

    @Test
    fun `generic extension binders retain distinct lexical owners and do not occupy a class key`() {
        val graph = genericExtensionsGraph(parameterName = "T")
        val keys = graph.publish()

        assertNotEquals(keys.getValue("first-extend-parameter"), keys.getValue("second-extend-parameter"))
        assertNotEquals(keys.getValue("first-extend-parameter"), keys.getValue("class-T"))
        assertNotEquals(keys.getValue("second-extend-parameter"), keys.getValue("class-T"))
        assertEquals(keys, graph.publish(reverseDeclarations = true, reverseTypes = true))
        assertEquals(keys, genericExtensionsGraph(parameterName = "Element").publish())
    }

    @Test
    fun `overloaded default parameter helpers retain their function and parameter ownership`() {
        val graph = defaultHelpersGraph()
        val keys = graph.publish()

        assertNotEquals(keys.getValue("int-function"), keys.getValue("bool-function"))
        assertNotEquals(keys.getValue("int-helper"), keys.getValue("bool-helper"))
        assertNotEquals(keys.getValue("int-parameter"), keys.getValue("bool-parameter"))
        assertEquals(keys, graph.publish(reverseDeclarations = true, reverseTypes = true))
    }

    @Test
    fun `private members in repeated extensions of the same target have distinct stable keys`() {
        val graph = privateExtensionsGraph()
        val keys = graph.publish()

        assertNotEquals(keys.getValue("first-private"), keys.getValue("second-private"))
        assertEquals(keys, graph.publish(reverseDeclarations = true, reverseTypes = true))
        assertEquals(keys, privateExtensionsGraph(sourceOffsetShift = 100).publish())

        val withUnrelatedExtension = privateExtensionsGraph(includeUnrelatedTarget = true).publish()
        assertEquals(keys.getValue("first-private"), withUnrelatedExtension.getValue("first-private"))
        assertEquals(keys.getValue("second-private"), withUnrelatedExtension.getValue("second-private"))
    }

    @Test
    fun `private declarations in different directories retain distinct source file identities`() {
        val graph = DeclarationGraph().apply {
            type("unit") { CjoTypeMetadata(kind = TypeKind.Unit) }
            type("function-type") { functionType(type("unit")) }
            for (directory in listOf("first", "second")) {
                declaration(directory, sourceFile = "D:/fixtures/$directory/keys.cj") {
                    CjoPackageDeclaration(
                        identifier = "hidden",
                        type = type("function-type"),
                        attributes = attributeWords(Attribute.PRIVATE),
                        info = CjoFunctionInfo(body = functionBody(type("unit"))),
                    )
                }
            }
        }
        val keys = graph.publish()

        assertNotEquals(keys.getValue("first"), keys.getValue("second"))
        assertEquals(keys, graph.publish(reverseDeclarations = true))
    }

    @Test
    fun `getter and setter identities include their property owner`() {
        val graph = accessorsGraph()
        val keys = graph.publish()
        val accessorKeys = listOf("first-getter", "first-setter", "second-getter", "second-setter").map(keys::getValue)

        assertEquals(4, accessorKeys.toSet().size)
        assertEquals(keys, graph.publish(reverseDeclarations = true, reverseTypes = true))

        val renamedProperty = accessorsGraph(firstPropertyName = "renamed").publish()
        assertNotEquals(keys.getValue("first-getter"), renamedProperty.getValue("first-getter"))
        assertNotEquals(keys.getValue("first-setter"), renamedProperty.getValue("first-setter"))
        assertEquals(keys.getValue("second-getter"), renamedProperty.getValue("second-getter"))
        assertEquals(keys.getValue("second-setter"), renamedProperty.getValue("second-setter"))
    }

    @Test
    fun `F bounded generic signatures terminate and remain alpha invariant`() {
        val graph = genericFunctionGraph(parameterName = "T", fBounded = true)
        val keys = graph.publish()

        assertTrue(keys.getValue("function").isNotBlank())
        assertTrue(keys.getValue("type-parameter").isNotBlank())
        assertEquals(keys, graph.publish(reverseDeclarations = true, reverseTypes = true))
        assertEquals(keys, genericFunctionGraph(parameterName = "Element", fBounded = true).publish())
        assertNotEquals(
            genericFunctionGraph(parameterName = "T").publish().getValue("function"),
            keys.getValue("function"),
            "the recursive upper bound is part of the callable signature",
        )
    }

    private fun genericOwnersGraph(parameterName: String = "T"): DeclarationGraph = DeclarationGraph().apply {
        for (owner in listOf("holder", "other")) {
            type("$owner-type") {
                CjoTypeMetadata(
                    kind = TypeKind.Class,
                    typeArguments = listOf(type("$owner-parameter-type")),
                    semanticInfo = CjoCompositeTypeInfoMetadata(declarationIndex = declaration(owner)),
                )
            }
            type("$owner-parameter-type") {
                CjoTypeMetadata(
                    kind = TypeKind.Generic,
                    semanticInfo = CjoGenericTypeInfoMetadata(declarationIndex = declaration("$owner-parameter")),
                )
            }
            declaration("$owner-parameter", owner = owner) {
                CjoPackageDeclaration(
                    identifier = parameterName,
                    kind = DeclKind.GenericParamDecl,
                    isTopLevel = false,
                    type = type("$owner-parameter-type"),
                )
            }
            declaration(owner) {
                CjoPackageDeclaration(
                    identifier = if (owner == "holder") "Holder" else "Other",
                    kind = DeclKind.ClassDecl,
                    type = type("$owner-type"),
                    generic = CjoGenericMetadata(typeParameters = listOf(declaration("$owner-parameter"))),
                    info = CjoClassInfo(),
                )
            }
        }
        type("class-T-type") {
            CjoTypeMetadata(
                kind = TypeKind.Class,
                semanticInfo = CjoCompositeTypeInfoMetadata(declarationIndex = declaration("class-T")),
            )
        }
        declaration("class-T") {
            CjoPackageDeclaration(identifier = "T", kind = DeclKind.ClassDecl, type = type("class-T-type"), info = CjoClassInfo())
        }
    }

    private fun genericFunctionGraph(parameterName: String, fBounded: Boolean = false): DeclarationGraph =
        DeclarationGraph().apply {
            type("parameter-type") {
                CjoTypeMetadata(
                    kind = TypeKind.Generic,
                    semanticInfo = CjoGenericTypeInfoMetadata(
                        declarationIndex = declaration("type-parameter"),
                        upperBounds = if (fBounded) listOf(type("recursive-bound")) else emptyList(),
                    ),
                )
            }
            type("function-type") { functionType(type("parameter-type"), type("parameter-type")) }
            declaration("type-parameter", owner = "function") {
                CjoPackageDeclaration(
                    identifier = parameterName,
                    kind = DeclKind.GenericParamDecl,
                    isTopLevel = false,
                    type = type("parameter-type"),
                )
            }
            declaration("function") {
                CjoPackageDeclaration(
                    identifier = "identity",
                    type = type("function-type"),
                    generic = CjoGenericMetadata(
                        typeParameters = listOf(declaration("type-parameter")),
                        constraints = if (fBounded) listOf(
                            CjoConstraintMetadata(type = type("parameter-type"), upperBounds = listOf(type("recursive-bound"))),
                        ) else emptyList(),
                    ),
                    info = CjoFunctionInfo(body = functionBody(type("parameter-type"), declaration("value-parameter"))),
                )
            }
            declaration("value-parameter", owner = "function") {
                CjoPackageDeclaration(
                    identifier = "value",
                    kind = DeclKind.FuncParam,
                    isTopLevel = false,
                    type = type("parameter-type"),
                    info = CjoParameterInfo(isNamed = false, isMember = false),
                )
            }
            if (fBounded) {
                type("comparable-parameter-type") {
                    CjoTypeMetadata(
                        kind = TypeKind.Generic,
                        semanticInfo = CjoGenericTypeInfoMetadata(declarationIndex = declaration("comparable-parameter")),
                    )
                }
                type("comparable-type") {
                    CjoTypeMetadata(
                        kind = TypeKind.Interface,
                        typeArguments = listOf(type("comparable-parameter-type")),
                        semanticInfo = CjoCompositeTypeInfoMetadata(declarationIndex = declaration("comparable")),
                    )
                }
                // T -> Comparable<T> -> T 是真实 F-bound 图，不能通过删掉 upperBounds 消除环。
                type("recursive-bound") {
                    CjoTypeMetadata(
                        kind = TypeKind.Interface,
                        typeArguments = listOf(type("parameter-type")),
                        semanticInfo = CjoCompositeTypeInfoMetadata(declarationIndex = declaration("comparable")),
                    )
                }
                declaration("comparable-parameter", owner = "comparable") {
                    CjoPackageDeclaration(
                        identifier = "U",
                        kind = DeclKind.GenericParamDecl,
                        isTopLevel = false,
                        type = type("comparable-parameter-type"),
                    )
                }
                declaration("comparable") {
                    CjoPackageDeclaration(
                        identifier = "Comparable",
                        kind = DeclKind.InterfaceDecl,
                        type = type("comparable-type"),
                        generic = CjoGenericMetadata(typeParameters = listOf(declaration("comparable-parameter"))),
                        info = CjoInterfaceInfo(),
                    )
                }
            }
        }

    private fun genericExtensionsGraph(parameterName: String): DeclarationGraph = genericOwnersGraph().apply {
        for ((ordinal, scope) in listOf("first", "second").withIndex()) {
            type("$scope-extend-parameter-type") {
                CjoTypeMetadata(
                    kind = TypeKind.Generic,
                    semanticInfo = CjoGenericTypeInfoMetadata(declarationIndex = declaration("$scope-extend-parameter")),
                )
            }
            type("$scope-extend-target-type") {
                CjoTypeMetadata(
                    kind = TypeKind.Class,
                    typeArguments = listOf(type("$scope-extend-parameter-type")),
                    semanticInfo = CjoCompositeTypeInfoMetadata(declarationIndex = declaration("holder")),
                )
            }
            declaration("$scope-extend-parameter", owner = "$scope-generic-extend") {
                CjoPackageDeclaration(
                    identifier = parameterName,
                    kind = DeclKind.GenericParamDecl,
                    isTopLevel = false,
                    type = type("$scope-extend-parameter-type"),
                )
            }
            declaration("$scope-generic-extend", sourceOffset = (ordinal + 1) * 100) {
                CjoPackageDeclaration(
                    identifier = "",
                    kind = DeclKind.ExtendDecl,
                    type = type("$scope-extend-target-type"),
                    generic = CjoGenericMetadata(typeParameters = listOf(declaration("$scope-extend-parameter"))),
                    info = CjoExtendInfo(),
                )
            }
        }
    }

    private fun defaultHelpersGraph(): DeclarationGraph = DeclarationGraph().apply {
        // 重载只改变后续 selector 的类型；第一个默认参数的 value.0(): Int64 辅助函数完全同签名。
        type("unit") { CjoTypeMetadata(kind = TypeKind.Unit) }
        type("int") { CjoTypeMetadata(kind = TypeKind.Int64) }
        type("bool") { CjoTypeMetadata(kind = TypeKind.Bool) }
        type("helper-type") { functionType(type("int")) }
        for (overload in listOf("int", "bool")) {
            type("$overload-function-type") { functionType(type("unit"), type("int"), type(overload)) }
            declaration("$overload-function") {
                CjoPackageDeclaration(
                    identifier = "choose",
                    type = type("$overload-function-type"),
                    info = CjoFunctionInfo(
                        body = CjoFunctionBodyInfo(
                            parameterLists = listOf(listOf(declaration("$overload-parameter"), declaration("$overload-selector"))),
                            desugaredParameterLists = listOf(listOf(declaration("$overload-helper"), 0u)),
                            returnType = type("unit"),
                        ),
                    ),
                )
            }
            declaration("$overload-selector", owner = "$overload-function") {
                CjoPackageDeclaration(
                    identifier = "selector",
                    kind = DeclKind.FuncParam,
                    isTopLevel = false,
                    type = type(overload),
                    info = CjoParameterInfo(isNamed = true, isMember = false),
                )
            }
            declaration("$overload-parameter", owner = "$overload-function") {
                CjoPackageDeclaration(
                    identifier = "value",
                    kind = DeclKind.FuncParam,
                    isTopLevel = false,
                    type = type("int"),
                    info = CjoParameterInfo(isNamed = true, isMember = false),
                )
            }
            // 两个 helper 同名同签名且同为包级声明，只有 desugars 的逻辑参数归属能够区分。
            declaration("$overload-helper") {
                CjoPackageDeclaration(
                    identifier = "value.0",
                    type = type("helper-type"),
                    info = CjoFunctionInfo(body = functionBody(type("int"))),
                )
            }
        }
    }

    private fun privateExtensionsGraph(
        sourceOffsetShift: Int = 0,
        includeUnrelatedTarget: Boolean = false,
    ): DeclarationGraph = DeclarationGraph().apply {
        type("target-type") {
            CjoTypeMetadata(
                kind = TypeKind.Class,
                semanticInfo = CjoCompositeTypeInfoMetadata(declarationIndex = declaration("target")),
            )
        }
        type("unit") { CjoTypeMetadata(kind = TypeKind.Unit) }
        type("function-type") { functionType(type("unit")) }
        declaration("target") {
            CjoPackageDeclaration(identifier = "Target", kind = DeclKind.ClassDecl, type = type("target-type"), info = CjoClassInfo())
        }
        for ((ordinal, scope) in listOf("first", "second").withIndex()) {
            declaration("$scope-extend", sourceOffset = sourceOffsetShift + (ordinal + 1) * 20) {
                CjoPackageDeclaration(
                    identifier = "",
                    kind = DeclKind.ExtendDecl,
                    type = type("target-type"),
                    info = CjoExtendInfo(body = listOf(declaration("$scope-private"))),
                )
            }
            declaration("$scope-private", owner = "$scope-extend", sourceOffset = sourceOffsetShift + (ordinal + 1) * 20 + 1) {
                CjoPackageDeclaration(
                    identifier = "hidden",
                    isTopLevel = false,
                    type = type("function-type"),
                    attributes = attributeWords(Attribute.PRIVATE),
                    info = CjoFunctionInfo(body = functionBody(type("unit"))),
                )
            }
        }
        if (includeUnrelatedTarget) {
            type("int") { CjoTypeMetadata(kind = TypeKind.Int64) }
            declaration("unrelated-extend", sourceOffset = 1) {
                CjoPackageDeclaration(identifier = "", kind = DeclKind.ExtendDecl, type = type("int"), info = CjoExtendInfo())
            }
        }
    }

    private fun accessorsGraph(firstPropertyName: String = "value"): DeclarationGraph = DeclarationGraph().apply {
        type("owner-type") {
            CjoTypeMetadata(
                kind = TypeKind.Class,
                semanticInfo = CjoCompositeTypeInfoMetadata(declarationIndex = declaration("owner")),
            )
        }
        type("int") { CjoTypeMetadata(kind = TypeKind.Int64) }
        type("unit") { CjoTypeMetadata(kind = TypeKind.Unit) }
        type("getter-type") { functionType(type("int")) }
        type("setter-type") { functionType(type("unit"), type("int")) }
        declaration("owner") {
            CjoPackageDeclaration(
                identifier = "Box",
                kind = DeclKind.ClassDecl,
                type = type("owner-type"),
                info = CjoClassInfo(body = listOf(declaration("first-property"), declaration("second-property"))),
            )
        }
        for (property in listOf("first", "second")) {
            declaration("$property-property", owner = "owner") {
                CjoPackageDeclaration(
                    identifier = if (property == "first") firstPropertyName else "other",
                    kind = DeclKind.PropDecl,
                    isTopLevel = false,
                    type = type("int"),
                    info = CjoPropertyInfo(
                        isMutable = true,
                        getters = listOf(declaration("$property-getter")),
                        setters = listOf(declaration("$property-setter")),
                    ),
                )
            }
            declaration("$property-getter", owner = "$property-property") {
                CjoPackageDeclaration(
                    identifier = "get",
                    isTopLevel = false,
                    type = type("getter-type"),
                    info = CjoFunctionInfo(body = functionBody(type("int"))),
                )
            }
            declaration("$property-setter", owner = "$property-property") {
                CjoPackageDeclaration(
                    identifier = "set",
                    isTopLevel = false,
                    type = type("setter-type"),
                    info = CjoFunctionInfo(body = functionBody(type("unit"), declaration("$property-value"))),
                )
            }
            declaration("$property-value", owner = "$property-setter") {
                CjoPackageDeclaration(
                    identifier = "value",
                    kind = DeclKind.FuncParam,
                    isTopLevel = false,
                    type = type("int"),
                    info = CjoParameterInfo(isNamed = false, isMember = false),
                )
            }
        }
    }

    /** 声明和类型用测试标签连接，重排时重新分配所有一基引用；标签不进入输出身份。 */
    private class DeclarationGraph {
        private data class DeclarationInput(
            val owner: String?,
            val sourceFile: String,
            val sourceOffset: Int,
            val build: References.() -> CjoPackageDeclaration,
        )

        private val declarations = linkedMapOf<String, DeclarationInput>()
        private val types = linkedMapOf<String, References.() -> CjoTypeMetadata>()

        fun declaration(
            name: String,
            owner: String? = null,
            sourceFile: String = "D:/fixtures/keys.cj",
            sourceOffset: Int = declarations.size * 10,
            build: References.() -> CjoPackageDeclaration,
        ) {
            check(declarations.put(name, DeclarationInput(owner, sourceFile, sourceOffset, build)) == null)
        }

        fun type(name: String, build: References.() -> CjoTypeMetadata) {
            check(types.put(name, build) == null)
        }

        fun publish(reverseDeclarations: Boolean = false, reverseTypes: Boolean = false): Map<String, String> {
            val declarationNames = declarations.keys.toList().let { if (reverseDeclarations) it.reversed() else it }
            val typeNames = types.keys.toList().let { if (reverseTypes) it.reversed() else it }
            val references = References(
                declarationNames.withIndex().associate { (index, name) -> name to (index.toUInt() + 1u) },
                typeNames.withIndex().associate { (index, name) -> name to (index.toUInt() + 1u) },
            )
            val inputDeclarations = declarationNames.map { name ->
                declarations.getValue(name).build(references).copy(begin = CjoPositionMetadata(file = 1u, line = 1, column = 1))
            }
            val inputTypes = typeNames.map { types.getValue(it)(references) }
            val published = CjoSourceDeclarationKeys(
                packageName = "keys.pkg",
                declarations = inputDeclarations,
                types = inputTypes,
                imports = emptyList(),
                sourceDeclarations = inputDeclarations.indices.toSet(),
                owners = declarationNames.map { declarations.getValue(it).owner?.let { owner -> references.declaration(owner).toInt() - 1 } },
                sourceFiles = declarationNames.withIndex().associate { (index, name) -> index to declarations.getValue(name).sourceFile },
                sourceOffsets = declarationNames.withIndex().associate { (index, name) -> index to declarations.getValue(name).sourceOffset },
            ).publish()
            val packageTable = Package.getRootAsPackage(
                ByteBuffer.wrap(
                    CjoPackageWriter.toByteArray(
                        CjoPackageMetadata(
                            fullPackageName = "keys.pkg",
                            moduleName = "keys",
                            allFiles = listOf("keys.cj"),
                            declarations = published,
                            types = inputTypes,
                        ),
                    ),
                ),
            )
            val header = CjoPackageHeader.fromPackage(packageTable)
            val keys = linkedMapOf<String, String>()
            for ((index, declaration) in published.withIndex()) {
                if (declaration.kind == DeclKind.ExtendDecl) continue
                val key = assertNotNull(declaration.exportId, "${declarationNames[index]} publishes a reference identity")
                assertTrue(key.isNotBlank())
                assertEquals(index, header.fullIdReferenceKeyToIndex[key], "the published key resolves to its exact declaration")
                assertEquals(declaration.kind, packageTable.allDecls(header.fullIdReferenceKeyToIndex.getValue(key))!!.kind)
                keys[declarationNames[index]] = key
            }
            assertEquals(keys.size, keys.values.toSet().size, "every published declaration has its own reference key")
            return keys
        }
    }

    private class References(private val declarations: Map<String, UInt>, private val types: Map<String, UInt>) {
        fun declaration(name: String): UInt = declarations.getValue(name)
        fun type(name: String): UInt = types.getValue(name)
    }

    private companion object {
        fun functionType(returnType: UInt, vararg parameters: UInt): CjoTypeMetadata = CjoTypeMetadata(
            kind = TypeKind.Func,
            typeArguments = parameters.toList(),
            semanticInfo = CjoFunctionTypeInfoMetadata(returnType = returnType, isC = false, hasVariableLenArg = false),
        )

        fun functionBody(returnType: UInt, vararg parameters: UInt): CjoFunctionBodyInfo = CjoFunctionBodyInfo(
            parameterLists = listOf(parameters.toList()),
            desugaredParameterLists = listOf(emptyList()),
            returnType = returnType,
        )

        fun attributeWords(vararg attributes: Attribute): List<ULong> =
            List(Attribute.entries.size / ULong.SIZE_BITS + 1) { word ->
                attributes.filter { it.ordinal / ULong.SIZE_BITS == word }
                    .fold(0uL) { bits, attribute -> bits or (1uL shl (attribute.ordinal % ULong.SIZE_BITS)) }
            }
    }
}
