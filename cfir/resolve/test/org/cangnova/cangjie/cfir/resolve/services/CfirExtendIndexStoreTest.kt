@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolver
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagImpl
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
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
 * [CfirExtendIndexStore] 绱㈠紩鏋勫缓涓庢煡璇㈡湇鍔℃祴璇曘€? */
class CfirExtendIndexStoreTest {
    /**
     * 楠岃瘉 rebuild 浼氬悎骞跺涓枃浠朵腑鐨?extend銆?     */
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
     * 楠岃瘉鏌ヨ閲嶅鎺ュ彛鏃跺彲浠ユ帓闄ゅ綋鍓?extend 澹版槑銆?     */
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
     * 楠岃瘉 typealias 鐩爣涓庣湡瀹炵洰鏍囧叡浜悓涓€绱㈠紩鍜屾帴鍙ｈ涔夌瓑浠风被锛屽悓鏃朵繚鐣?alias 澹版槑鍏冩暟鎹€?     */
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
                annotations = org.cangnova.cangjie.cfir.MutableOrEmptyList.empty(),
                customRenderer = false,
                coneType = ConeTypeAliasType(
                    classId = aliasId,
                    expandedType = ConeClassLikeType(
                        lookupTag = ConeClassLikeLookupTagImpl(expandedClassId),
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
                targetClassId to ExtendTestFixtures.newClass(moduleData, "Target", classId = targetClassId),
                interfaceClassId to ExtendTestFixtures.newInterface(moduleData, "I", classId = interfaceClassId),
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
     * 楠岃瘉璇箟 key 浼氳鑼冨寲 extend 绫诲瀷鍙傛暟鍚嶇О銆?     */
    @Test
    fun `semantic keys normalize extend type parameter names`() {
        val (_, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val packageFqName = FqName("sample.pkg")
        val targetClassId = ClassId(packageFqName, Name.identifier("Target"))
        val genericInterfaceId = ClassId(packageFqName, Name.identifier("IGeneric"))

        val typeParameterT = ExtendTestFixtures.newTypeParameter(moduleData, "T")
        val extendT = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            typeParameters = listOf(typeParameterT),
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(
                ExtendTestFixtures.classTypeRef(
                    classId = genericInterfaceId,
                    typeArguments = listOf(ExtendTestFixtures.typeParameterType(typeParameterT)),
                    isInterface = true,
                ),
            ),
        )
        val typeParameterU = ExtendTestFixtures.newTypeParameter(moduleData, "U")
        val extendU = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            typeParameters = listOf(typeParameterU),
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(
                ExtendTestFixtures.classTypeRef(
                    classId = genericInterfaceId,
                    typeArguments = listOf(ExtendTestFixtures.typeParameterType(typeParameterU)),
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
     * 楠岃瘉鍚屼竴 target 鐨勬ā鍨嬭繑鍥為『搴忎笉鍙楄緭鍏ユ枃浠堕『搴忓奖鍝嶃€?     */
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
     * 楠岃瘉璇箟 key 瑙勮寖鍖栨椂鍖呭惈绫诲瀷鍙傛暟涓婄晫銆?     */
    @Test
    fun `semantic keys include type parameter bounds when normalizing extend interfaces`() {
        val (_, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val packageFqName = FqName("sample.pkg")
        val targetClassId = ClassId(packageFqName, Name.identifier("Target"))
        val genericInterfaceId = ClassId(packageFqName, Name.identifier("IGeneric"))
        val boundA = ClassId(packageFqName, Name.identifier("BoundA"))
        val boundB = ClassId(packageFqName, Name.identifier("BoundB"))

        val typeParameterBoundA = ExtendTestFixtures.newTypeParameter(
            moduleData = moduleData,
            name = "T",
            bounds = listOf(ExtendTestFixtures.classTypeRef(boundA)),
        )
        val extendBoundA = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            typeParameters = listOf(typeParameterBoundA),
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(
                ExtendTestFixtures.classTypeRef(
                    classId = genericInterfaceId,
                    typeArguments = listOf(ExtendTestFixtures.typeParameterType(typeParameterBoundA)),
                    isInterface = true,
                ),
            ),
        )
        val typeParameterBoundB = ExtendTestFixtures.newTypeParameter(
            moduleData = moduleData,
            name = "T",
            bounds = listOf(ExtendTestFixtures.classTypeRef(boundB)),
        )
        val extendBoundB = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            typeParameters = listOf(typeParameterBoundB),
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(
                ExtendTestFixtures.classTypeRef(
                    classId = genericInterfaceId,
                    typeArguments = listOf(ExtendTestFixtures.typeParameterType(typeParameterBoundB)),
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
     * 楠岃瘉鐩爣绫昏嚜韬帴鍙ｉ泦鍚堝寘鍚埗绫婚摼缁ф壙鐨勬帴鍙ｃ€?     */
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
            rootInterfaceId to ExtendTestFixtures.newInterface(moduleData, "IRoot", classId = rootInterfaceId),
            leafInterfaceId to ExtendTestFixtures.newInterface(
                moduleData = moduleData,
                classId = leafInterfaceId,
                name = "ILeaf",
                superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(rootInterfaceId, isInterface = true)),
            ),
            baseClassId to ExtendTestFixtures.newClass(
                moduleData = moduleData,
                classId = baseClassId,
                name = "Base",
                superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(leafInterfaceId, isInterface = true)),
            ),
            targetClassId to ExtendTestFixtures.newClass(
                moduleData = moduleData,
                classId = targetClassId,
                name = "Target",
                superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(baseClassId)),
            ),
            extraInterfaceId to ExtendTestFixtures.newInterface(moduleData, "IExtra", classId = extraInterfaceId),
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
     * 楠岃瘉鍏朵粬鍖呮墿灞曟帴鍙ｉ泦鍚堝寘鍚紶閫掔埗鎺ュ彛銆?     */
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
            targetClassId to ExtendTestFixtures.newClass(moduleData, "Target", classId = targetClassId),
            rootInterfaceId to ExtendTestFixtures.newInterface(moduleData, "IRoot", classId = rootInterfaceId),
            leafInterfaceId to ExtendTestFixtures.newInterface(
                moduleData = moduleData,
                classId = leafInterfaceId,
                name = "ILeaf",
                superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(rootInterfaceId, isInterface = true)),
            ),
            localInterfaceId to ExtendTestFixtures.newInterface(moduleData, "ILocal", classId = localInterfaceId),
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
     * 楠岃瘉鍗曚釜 extend 澹版槑鐨勬帴鍙ｉ棴鍖呭寘鍚紶閫掔埗鎺ュ彛銆?     */
    @Test
    fun `declaration interface closure includes transitive parent interfaces`() {
        val (_, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val packageFqName = FqName("sample.pkg")
        val targetClassId = ClassId(packageFqName, Name.identifier("Target"))
        val rootInterfaceId = ClassId(packageFqName, Name.identifier("IRoot"))
        val leafInterfaceId = ClassId(packageFqName, Name.identifier("ILeaf"))

        val declarations = linkedMapOf(
            targetClassId to ExtendTestFixtures.newClass(moduleData, "Target", classId = targetClassId),
            rootInterfaceId to ExtendTestFixtures.newInterface(moduleData, "IRoot", classId = rootInterfaceId),
            leafInterfaceId to ExtendTestFixtures.newInterface(
                moduleData = moduleData,
                classId = leafInterfaceId,
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
     * 楠岃瘉闈炴硶鎺ュ彛鐖剁被鍨嬩笉浼氭薄鏌?extend 瑙勫垯闂寘銆?     */
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
            targetClassId to ExtendTestFixtures.newClass(moduleData, "Target", classId = targetClassId),
            remoteInterfaceId to ExtendTestFixtures.newInterface(moduleData, "IRemote", classId = remoteInterfaceId),
            concreteClassId to ExtendTestFixtures.newClass(moduleData, "Concrete", classId = concreteClassId),
            invalidInterfaceId to ExtendTestFixtures.newInterface(
                moduleData = moduleData,
                classId = invalidInterfaceId,
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
     * 楠岃瘉閲嶅鎺ュ彛涓嶄細鏀瑰彉澹版槑鎺ュ彛闂寘璇箟銆?     */
    @Test
    fun `duplicate interfaces do not change declaration closure semantics`() {
        val (_, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val packageFqName = FqName("sample.pkg")
        val targetClassId = ClassId(packageFqName, Name.identifier("Target"))
        val rootInterfaceId = ClassId(packageFqName, Name.identifier("IRoot"))
        val leafInterfaceId = ClassId(packageFqName, Name.identifier("ILeaf"))

        val declarations = linkedMapOf(
            targetClassId to ExtendTestFixtures.newClass(moduleData, "Target", classId = targetClassId),
            rootInterfaceId to ExtendTestFixtures.newInterface(moduleData, "IRoot", classId = rootInterfaceId),
            leafInterfaceId to ExtendTestFixtures.newInterface(
                moduleData = moduleData,
                classId = leafInterfaceId,
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
     * 楠岃瘉 extend 鎺ュ彛缁ф壙鏌ヨ淇濈暀 child 鍒?parent 鐨勬柟鍚戙€?     */
    @Test
    fun `extend interface inheritance relation is directional`() {
        val (_, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val packageFqName = FqName("sample.pkg")
        val targetClassId = ClassId(packageFqName, Name.identifier("Target"))
        val parentInterfaceId = ClassId(packageFqName, Name.identifier("IParent"))
        val childInterfaceId = ClassId(packageFqName, Name.identifier("IChild"))

        val declarations = linkedMapOf(
            targetClassId to ExtendTestFixtures.newClass(moduleData, "Target", classId = targetClassId),
            parentInterfaceId to ExtendTestFixtures.newInterface(moduleData, "IParent", classId = parentInterfaceId),
            childInterfaceId to ExtendTestFixtures.newInterface(
                moduleData = moduleData,
                classId = childInterfaceId,
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
 * 涓嶆墽琛岀湡瀹炵被鍨嬭В鏋愮殑娴嬭瘯 resolver銆? */
private object NoopTypeResolver : CfirTypeResolver() {
    /**
     * 杩斿洖鍥哄畾閿欒绫诲瀷锛屽綋鍓嶆祴璇曞彧渚濊禆宸茶В鏋?type ref銆?     */
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
     * 涓嶄粠 type ref 瑙ｆ瀽 class銆?     */
    override fun resolveClass(typeRef: org.cangnova.cangjie.cfir.types.CfirTypeRef): CfirClassLikeDeclaration? = null

    /**
     * 涓嶄粠 ClassId 瑙ｆ瀽 class銆?     */
    override fun resolveClass(classId: ClassId): CfirClassLikeDeclaration? = null
}

/**
 * 鍩轰簬鍐呭瓨澹版槑琛ㄨВ鏋?class 鐨勬祴璇?resolver銆? */
private class MapBackedTypeResolver(
    /**
     * 鎸?ClassId 绱㈠紩鐨勬祴璇曞０鏄庤〃銆?     */
    private val declarationsByClassId: Map<ClassId, CfirClassLikeDeclaration>,
) : CfirTypeResolver() {
    /**
     * 杩斿洖鍥哄畾閿欒绫诲瀷锛涙祴璇曚粎浣跨敤 class 瑙ｆ瀽鑳藉姏銆?     */
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
     * 浠?resolved type ref 鐨?ClassId 鏌ユ壘澹版槑銆?     */
    override fun resolveClass(typeRef: org.cangnova.cangjie.cfir.types.CfirTypeRef): CfirClassLikeDeclaration? {
        val resolvedTypeRef = typeRef as? CfirResolvedTypeRef ?: return null
        val classId = resolvedTypeRef.coneType.classIdOrPrimitiveClassId ?: return null
        return declarationsByClassId[classId]
    }

    /**
     * 浠?ClassId 鏌ユ壘澹版槑銆?     */
    override fun resolveClass(classId: ClassId): CfirClassLikeDeclaration? =
        declarationsByClassId[classId]
}
