package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolver
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeClassLookupTagImpl
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.cfir.types.impl.CfirResolvedTypeRefImpl
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [CfirExtendIndexStore] 索引构建与查询服务测试。
 */
class CfirExtendIndexStoreTest {
    /**
     * 验证 rebuild 会合并多个文件中的 extend。
     */
    @Test
    fun `rebuild indexes extends from multiple files`() {
        val (_, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val packageFqName = FqName("sample.pkg")
        val targetClassId = ClassId(packageFqName, Name.identifier("Target"))
        val interfaceA = ClassId(packageFqName, Name.identifier("IA"))
        val interfaceB = ClassId(packageFqName, Name.identifier("IB"))

        val extend1 = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(interfaceA, isInterface = true)),
        )
        val extend2 = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(interfaceB, isInterface = true)),
        )
        val file1 = ExtendTestFixtures.newFile(moduleData, packageFqName, listOf(extend1))
        val file2 = ExtendTestFixtures.newFile(moduleData, packageFqName, listOf(extend2))

        val store = CfirExtendIndexStore()
        store.rebuild(listOf(file1, file2), NoopTypeResolver)

        assertEquals(2, store.modelsForClass(targetClassId).size)
        assertEquals(listOf(interfaceA), store.modelForDeclaration(extend1)?.inheritedInterfaceClassIds)
        assertEquals(listOf(interfaceB), store.modelForDeclaration(extend2)?.inheritedInterfaceClassIds)
    }

    /**
     * 验证查询重复接口时可以排除当前 extend 声明。
     */
    @Test
    fun `query service excludes current declaration when collecting duplicates`() {
        val (_, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val packageFqName = FqName("sample.pkg")
        val targetClassId = ClassId(packageFqName, Name.identifier("Target"))
        val interfaceA = ClassId(packageFqName, Name.identifier("IA"))
        val interfaceB = ClassId(packageFqName, Name.identifier("IB"))

        val extend1 = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(interfaceA, isInterface = true)),
        )
        val extend2 = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(interfaceB, isInterface = true)),
        )
        val file1 = ExtendTestFixtures.newFile(moduleData, packageFqName, listOf(extend1))
        val file2 = ExtendTestFixtures.newFile(moduleData, packageFqName, listOf(extend2))

        val store = CfirExtendIndexStore()
        store.rebuild(listOf(file1, file2), NoopTypeResolver)

        val query = CfirExtendRuleQueryServiceImpl(store)
        assertEquals(targetClassId, query.targetClassIdOf(extend1))
        assertNull(query.targetClassIdOf(Any()))
        assertEquals(
            listOf(interfaceB),
            query.inheritedInterfaceClassIdsForTarget(targetClassId, excludingDeclaration = extend1),
        )
        assertEquals(
            listOf(interfaceA, interfaceB),
            query.inheritedInterfaceClassIdsForTarget(targetClassId),
        )
        assertEquals(1, query.inheritedInterfacesOf(extend1).size)
        assertEquals(interfaceA, query.inheritedInterfacesOf(extend1).single().classId)
    }

    /**
     * 验证 typealias 目标与真实目标共享同一索引和接口语义等价类，同时保留 alias 声明元数据。
     */
    @Test
    fun `type alias targets are indexed by expanded semantic identity`() {
        val (_, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val packageFqName = FqName("sample.pkg")
        val targetClassId = ClassId(packageFqName, Name.identifier("Target"))
        val targetAliasId = ClassId(packageFqName, Name.identifier("TargetAlias"))
        val interfaceClassId = ClassId(packageFqName, Name.identifier("I"))
        val interfaceAliasId = ClassId(packageFqName, Name.identifier("IAlias"))

        fun aliasTypeRef(aliasId: ClassId, expandedClassId: ClassId, isInterface: Boolean = false) =
            CfirResolvedTypeRefImpl(
                source = null,
                annotations = emptyList(),
                coneType = ConeTypeAliasType(
                    classId = aliasId,
                    expandedType = ConeClassLikeType(
                        lookupTag = ConeClassLookupTagImpl(expandedClassId),
                        isInterface = isInterface,
                    ),
                ),
                delegatedTypeRef = null,
            )

        val aliasExtend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = aliasTypeRef(targetAliasId, targetClassId),
            superTypeRefs = listOf(aliasTypeRef(interfaceAliasId, interfaceClassId, isInterface = true)),
        )
        val directExtend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(interfaceClassId, isInterface = true)),
        )
        val file = ExtendTestFixtures.newFile(moduleData, packageFqName, listOf(aliasExtend, directExtend))
        val resolver = MapBackedTypeResolver(
            mapOf(
                targetClassId to ExtendTestFixtures.newClass(moduleData, "Target"),
                interfaceClassId to ExtendTestFixtures.newInterface(moduleData, "I"),
            ),
        )

        val store = CfirExtendIndexStore()
        store.rebuild(listOf(file), resolver)

        val aliasModel = store.modelForDeclaration(aliasExtend)
        assertEquals(targetClassId, aliasModel?.targetClassId)
        assertEquals(targetAliasId, aliasModel?.declaredTargetClassId)
        assertEquals(2, store.modelsForClass(targetClassId).size)
        assertEquals(0, store.modelsForClass(targetAliasId).size)
        assertEquals(
            store.modelForDeclaration(directExtend)?.inheritedInterfaceSemanticKeys,
            aliasModel?.inheritedInterfaceSemanticKeys,
        )
    }

    /**
     * 验证语义 key 会规范化 extend 类型参数名称。
     */
    @Test
    fun `semantic keys normalize extend type parameter names`() {
        val (_, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val packageFqName = FqName("sample.pkg")
        val targetClassId = ClassId(packageFqName, Name.identifier("Target"))
        val genericInterfaceId = ClassId(packageFqName, Name.identifier("IGeneric"))

        val extendT = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            typeParameters = listOf(ExtendTestFixtures.newTypeParameter(moduleData, "T")),
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(
                ExtendTestFixtures.classTypeRef(
                    classId = genericInterfaceId,
                    typeArguments = listOf(ExtendTestFixtures.typeParameterType("T")),
                    isInterface = true,
                ),
            ),
        )
        val extendU = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            typeParameters = listOf(ExtendTestFixtures.newTypeParameter(moduleData, "U")),
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(
                ExtendTestFixtures.classTypeRef(
                    classId = genericInterfaceId,
                    typeArguments = listOf(ExtendTestFixtures.typeParameterType("U")),
                    isInterface = true,
                ),
            ),
        )
        val file1 = ExtendTestFixtures.newFile(moduleData, packageFqName, listOf(extendT))
        val file2 = ExtendTestFixtures.newFile(moduleData, packageFqName, listOf(extendU))

        val store = CfirExtendIndexStore()
        store.rebuild(listOf(file1, file2), NoopTypeResolver)

        val query = CfirExtendRuleQueryServiceImpl(store)
        assertEquals(
            query.inheritedInterfaceSemanticKeysOf(extendT),
            query.inheritedInterfaceSemanticKeysOf(extendU),
        )
        assertEquals(
            query.inheritedInterfacesOf(extendT).single().semanticKey,
            query.inheritedInterfacesOf(extendU).single().semanticKey,
        )
    }

    /**
     * 验证同一 target 的模型返回顺序不受输入文件顺序影响。
     */
    @Test
    fun `models for same target are returned in stable order regardless of input file order`() {
        val (_, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val packageFqName = FqName("sample.pkg")
        val targetClassId = ClassId(packageFqName, Name.identifier("Target"))
        val interfaceA = ClassId(packageFqName, Name.identifier("IA"))
        val interfaceB = ClassId(packageFqName, Name.identifier("IB"))

        val extendB = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(interfaceB, isInterface = true)),
        )
        val extendA = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(interfaceA, isInterface = true)),
        )
        val fileB = ExtendTestFixtures.newFile(
            moduleData = moduleData,
            packageFqName = packageFqName,
            declarations = listOf(extendB),
            fileName = "z_extend_b.cj",
        )
        val fileA = ExtendTestFixtures.newFile(
            moduleData = moduleData,
            packageFqName = packageFqName,
            declarations = listOf(extendA),
            fileName = "a_extend_a.cj",
        )

        val store = CfirExtendIndexStore()
        store.rebuild(listOf(fileB, fileA), NoopTypeResolver)

        val query = CfirExtendRuleQueryServiceImpl(store)
        val ordered = query.inheritedInterfaceClassIdsForTarget(targetClassId)
        assertEquals(listOf(interfaceA, interfaceB), ordered)
    }

    /**
     * 验证语义 key 规范化时包含类型参数上界。
     */
    @Test
    fun `semantic keys include type parameter bounds when normalizing extend interfaces`() {
        val (_, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val packageFqName = FqName("sample.pkg")
        val targetClassId = ClassId(packageFqName, Name.identifier("Target"))
        val genericInterfaceId = ClassId(packageFqName, Name.identifier("IGeneric"))
        val boundA = ClassId(packageFqName, Name.identifier("BoundA"))
        val boundB = ClassId(packageFqName, Name.identifier("BoundB"))

        val extendBoundA = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            typeParameters = listOf(
                ExtendTestFixtures.newTypeParameter(
                    moduleData = moduleData,
                    name = "T",
                    bounds = listOf(ExtendTestFixtures.classTypeRef(boundA)),
                ),
            ),
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(
                ExtendTestFixtures.classTypeRef(
                    classId = genericInterfaceId,
                    typeArguments = listOf(ExtendTestFixtures.typeParameterType("T")),
                    isInterface = true,
                ),
            ),
        )
        val extendBoundB = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            typeParameters = listOf(
                ExtendTestFixtures.newTypeParameter(
                    moduleData = moduleData,
                    name = "T",
                    bounds = listOf(ExtendTestFixtures.classTypeRef(boundB)),
                ),
            ),
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(
                ExtendTestFixtures.classTypeRef(
                    classId = genericInterfaceId,
                    typeArguments = listOf(ExtendTestFixtures.typeParameterType("T")),
                    isInterface = true,
                ),
            ),
        )
        val file1 = ExtendTestFixtures.newFile(moduleData, packageFqName, listOf(extendBoundA))
        val file2 = ExtendTestFixtures.newFile(moduleData, packageFqName, listOf(extendBoundB))

        val store = CfirExtendIndexStore()
        store.rebuild(listOf(file1, file2), NoopTypeResolver)

        val query = CfirExtendRuleQueryServiceImpl(store)
        assertNotEquals(
            query.inheritedInterfacesOf(extendBoundA).single().semanticKey,
            query.inheritedInterfacesOf(extendBoundB).single().semanticKey,
        )
    }

    /**
     * 验证目标类自身接口集合包含父类链继承的接口。
     */
    @Test
    fun `target own interface ids include interfaces inherited through superclass chain`() {
        val (_, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val packageFqName = FqName("sample.pkg")
        val rootInterfaceId = ClassId(packageFqName, Name.identifier("IRoot"))
        val leafInterfaceId = ClassId(packageFqName, Name.identifier("ILeaf"))
        val baseClassId = ClassId(packageFqName, Name.identifier("Base"))
        val targetClassId = ClassId(packageFqName, Name.identifier("Target"))
        val extraInterfaceId = ClassId(packageFqName, Name.identifier("IExtra"))

        val declarations = linkedMapOf(
            rootInterfaceId to ExtendTestFixtures.newInterface(moduleData, "IRoot"),
            leafInterfaceId to ExtendTestFixtures.newInterface(
                moduleData = moduleData,
                name = "ILeaf",
                superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(rootInterfaceId, isInterface = true)),
            ),
            baseClassId to ExtendTestFixtures.newClass(
                moduleData = moduleData,
                name = "Base",
                superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(leafInterfaceId, isInterface = true)),
            ),
            targetClassId to ExtendTestFixtures.newClass(
                moduleData = moduleData,
                name = "Target",
                superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(baseClassId)),
            ),
            extraInterfaceId to ExtendTestFixtures.newInterface(moduleData, "IExtra"),
        )

        val extend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(extraInterfaceId, isInterface = true)),
        )
        val file = ExtendTestFixtures.newFile(moduleData, packageFqName, listOf(extend))

        val store = CfirExtendIndexStore()
        store.rebuild(listOf(file), MapBackedTypeResolver(declarations))

        assertEquals(
            linkedSetOf(rootInterfaceId, leafInterfaceId),
            store.targetClassOwnInterfaceClassIds(targetClassId),
        )
    }

    /**
     * 验证其他包扩展接口集合包含传递父接口。
     */
    @Test
    fun `other package extended interface ids include transitive parent interfaces`() {
        val (_, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val targetPackage = FqName("sample.target")
        val remotePackage = FqName("remote.pkg")
        val localPackage = FqName("local.pkg")
        val targetClassId = ClassId(targetPackage, Name.identifier("Target"))
        val rootInterfaceId = ClassId(remotePackage, Name.identifier("IRoot"))
        val leafInterfaceId = ClassId(remotePackage, Name.identifier("ILeaf"))
        val localInterfaceId = ClassId(localPackage, Name.identifier("ILocal"))

        val declarations = linkedMapOf(
            targetClassId to ExtendTestFixtures.newClass(moduleData, "Target"),
            rootInterfaceId to ExtendTestFixtures.newInterface(moduleData, "IRoot"),
            leafInterfaceId to ExtendTestFixtures.newInterface(
                moduleData = moduleData,
                name = "ILeaf",
                superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(rootInterfaceId, isInterface = true)),
            ),
            localInterfaceId to ExtendTestFixtures.newInterface(moduleData, "ILocal"),
        )

        val remoteExtend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(leafInterfaceId, isInterface = true)),
        )
        val localExtend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(localInterfaceId, isInterface = true)),
        )
        val remoteFile = ExtendTestFixtures.newFile(moduleData, remotePackage, listOf(remoteExtend))
        val localFile = ExtendTestFixtures.newFile(moduleData, localPackage, listOf(localExtend))

        val store = CfirExtendIndexStore()
        store.rebuild(listOf(remoteFile, localFile), MapBackedTypeResolver(declarations))

        assertEquals(
            linkedSetOf(rootInterfaceId, leafInterfaceId),
            store.otherPackageExtendedInterfaceClassIds(targetClassId, localPackage),
        )
    }

    /**
     * 验证单个 extend 声明的接口闭包包含传递父接口。
     */
    @Test
    fun `declaration interface closure includes transitive parent interfaces`() {
        val (_, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val packageFqName = FqName("sample.pkg")
        val targetClassId = ClassId(packageFqName, Name.identifier("Target"))
        val rootInterfaceId = ClassId(packageFqName, Name.identifier("IRoot"))
        val leafInterfaceId = ClassId(packageFqName, Name.identifier("ILeaf"))

        val declarations = linkedMapOf(
            targetClassId to ExtendTestFixtures.newClass(moduleData, "Target"),
            rootInterfaceId to ExtendTestFixtures.newInterface(moduleData, "IRoot"),
            leafInterfaceId to ExtendTestFixtures.newInterface(
                moduleData = moduleData,
                name = "ILeaf",
                superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(rootInterfaceId, isInterface = true)),
            ),
        )

        val extend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(leafInterfaceId, isInterface = true)),
        )
        val file = ExtendTestFixtures.newFile(moduleData, packageFqName, listOf(extend))

        val store = CfirExtendIndexStore()
        store.rebuild(listOf(file), MapBackedTypeResolver(declarations))

        val query = CfirExtendRuleQueryServiceImpl(store)
        assertEquals(
            linkedSetOf(leafInterfaceId, rootInterfaceId),
            query.inheritedInterfaceClosureClassIdsOf(extend),
        )
    }

    /**
     * 验证非法接口父类型不会污染 extend 规则闭包。
     */
    @Test
    fun `invalid interface supertypes are excluded from declaration closure`() {
        val (_, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val localPackage = FqName("local.pkg")
        val remotePackage = FqName("remote.pkg")
        val targetClassId = ClassId(remotePackage, Name.identifier("Target"))
        val remoteInterfaceId = ClassId(remotePackage, Name.identifier("IRemote"))
        val invalidInterfaceId = ClassId(localPackage, Name.identifier("InvalidInterface"))
        val concreteClassId = ClassId(remotePackage, Name.identifier("Concrete"))

        val declarations = linkedMapOf(
            targetClassId to ExtendTestFixtures.newClass(moduleData, "Target"),
            remoteInterfaceId to ExtendTestFixtures.newInterface(moduleData, "IRemote"),
            concreteClassId to ExtendTestFixtures.newClass(moduleData, "Concrete"),
            invalidInterfaceId to ExtendTestFixtures.newInterface(
                moduleData = moduleData,
                name = "InvalidInterface",
                superTypeRefs = listOf(
                    ExtendTestFixtures.classTypeRef(remoteInterfaceId, isInterface = true),
                    ExtendTestFixtures.classTypeRef(concreteClassId),
                ),
            ),
        )

        val extend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(invalidInterfaceId, isInterface = true)),
        )
        val file = ExtendTestFixtures.newFile(moduleData, localPackage, listOf(extend))

        val store = CfirExtendIndexStore()
        store.rebuild(listOf(file), MapBackedTypeResolver(declarations))

        val query = CfirExtendRuleQueryServiceImpl(store)
        assertEquals(listOf(invalidInterfaceId), query.inheritedInterfaceClassIdsOf(extend))
        assertEquals(emptySet<ClassId>(), query.inheritedInterfaceClosureClassIdsOf(extend))
    }

    /**
     * 验证重复接口不会改变声明接口闭包语义。
     */
    @Test
    fun `duplicate interfaces do not change declaration closure semantics`() {
        val (_, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val packageFqName = FqName("sample.pkg")
        val targetClassId = ClassId(packageFqName, Name.identifier("Target"))
        val rootInterfaceId = ClassId(packageFqName, Name.identifier("IRoot"))
        val leafInterfaceId = ClassId(packageFqName, Name.identifier("ILeaf"))

        val declarations = linkedMapOf(
            targetClassId to ExtendTestFixtures.newClass(moduleData, "Target"),
            rootInterfaceId to ExtendTestFixtures.newInterface(moduleData, "IRoot"),
            leafInterfaceId to ExtendTestFixtures.newInterface(
                moduleData = moduleData,
                name = "ILeaf",
                superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(rootInterfaceId, isInterface = true)),
            ),
        )

        val duplicatedExtend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(
                ExtendTestFixtures.classTypeRef(leafInterfaceId, isInterface = true),
                ExtendTestFixtures.classTypeRef(leafInterfaceId, isInterface = true),
            ),
        )
        val file = ExtendTestFixtures.newFile(moduleData, packageFqName, listOf(duplicatedExtend))

        val store = CfirExtendIndexStore()
        store.rebuild(listOf(file), MapBackedTypeResolver(declarations))

        val query = CfirExtendRuleQueryServiceImpl(store)
        assertEquals(
            linkedSetOf(leafInterfaceId, rootInterfaceId),
            query.inheritedInterfaceClosureClassIdsOf(duplicatedExtend),
        )
    }

    /**
     * 验证 extend 接口继承查询保留 child 到 parent 的方向。
     */
    @Test
    fun `extend interface inheritance relation is directional`() {
        val (_, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val packageFqName = FqName("sample.pkg")
        val targetClassId = ClassId(packageFqName, Name.identifier("Target"))
        val parentInterfaceId = ClassId(packageFqName, Name.identifier("IParent"))
        val childInterfaceId = ClassId(packageFqName, Name.identifier("IChild"))

        val declarations = linkedMapOf(
            targetClassId to ExtendTestFixtures.newClass(moduleData, "Target"),
            parentInterfaceId to ExtendTestFixtures.newInterface(moduleData, "IParent"),
            childInterfaceId to ExtendTestFixtures.newInterface(
                moduleData = moduleData,
                name = "IChild",
                superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(parentInterfaceId, isInterface = true)),
            ),
        )
        val parentExtend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(parentInterfaceId, isInterface = true)),
        )
        val childExtend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(childInterfaceId, isInterface = true)),
        )
        val file = ExtendTestFixtures.newFile(moduleData, packageFqName, listOf(parentExtend, childExtend))
        val store = CfirExtendIndexStore()
        store.rebuild(listOf(file), MapBackedTypeResolver(declarations))
        val query = CfirExtendRuleQueryServiceImpl(store)

        assertTrue(query.doesExtendInheritFrom(childExtend, parentExtend))
        assertFalse(query.doesExtendInheritFrom(parentExtend, childExtend))
    }
}

/**
 * 不执行真实类型解析的测试 resolver。
 */
private object NoopTypeResolver : CfirTypeResolver() {
    /**
     * 返回固定错误类型，当前测试只依赖已解析 type ref。
     */
    override fun resolveType(
        typeRef: org.cangnova.cangjie.cfir.types.CfirTypeRef,
        configuration: org.cangnova.cangjie.cfir.resolve.TypeResolutionConfiguration,
        areBareTypesAllowed: Boolean,
        isOperandOfIsOperator: Boolean,
        resolveDeprecations: Boolean,
        supertypeSupplier: org.cangnova.cangjie.cfir.resolve.SupertypeSupplier,
        expandTypeAliases: Boolean,
    ): org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionResult {
        return org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionResult(
            type = org.cangnova.cangjie.cfir.types.ConeErrorType(
                org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic("NoopTypeResolver"),
            ),
            diagnostic = null,
        )
    }

    /**
     * 不从 type ref 解析 class。
     */
    override fun resolveClass(typeRef: org.cangnova.cangjie.cfir.types.CfirTypeRef): CfirClassLikeDeclaration? = null

    /**
     * 不从 ClassId 解析 class。
     */
    override fun resolveClass(classId: ClassId): CfirClassLikeDeclaration? = null
}

/**
 * 基于内存声明表解析 class 的测试 resolver。
 */
private class MapBackedTypeResolver(
    /**
     * 按 ClassId 索引的测试声明表。
     */
    private val declarationsByClassId: Map<ClassId, CfirClassLikeDeclaration>,
) : CfirTypeResolver() {
    /**
     * 返回固定错误类型；测试仅使用 class 解析能力。
     */
    override fun resolveType(
        typeRef: org.cangnova.cangjie.cfir.types.CfirTypeRef,
        configuration: org.cangnova.cangjie.cfir.resolve.TypeResolutionConfiguration,
        areBareTypesAllowed: Boolean,
        isOperandOfIsOperator: Boolean,
        resolveDeprecations: Boolean,
        supertypeSupplier: org.cangnova.cangjie.cfir.resolve.SupertypeSupplier,
        expandTypeAliases: Boolean,
    ): org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionResult {
        return org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionResult(
            type = org.cangnova.cangjie.cfir.types.ConeErrorType(
                org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic("MapBackedTypeResolver"),
            ),
            diagnostic = null,
        )
    }

    /**
     * 从 resolved type ref 的 ClassId 查找声明。
     */
    override fun resolveClass(typeRef: org.cangnova.cangjie.cfir.types.CfirTypeRef): CfirClassLikeDeclaration? {
        val resolvedTypeRef = typeRef as? CfirResolvedTypeRef ?: return null
        val classId = resolvedTypeRef.coneType.classIdOrPrimitiveClassId ?: return null
        return declarationsByClassId[classId]
    }

    /**
     * 从 ClassId 查找声明。
     */
    override fun resolveClass(classId: ClassId): CfirClassLikeDeclaration? =
        declarationsByClassId[classId]
}
