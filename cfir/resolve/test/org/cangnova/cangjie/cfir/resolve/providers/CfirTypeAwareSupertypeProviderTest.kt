@file:OptIn(
    org.cangnova.cangjie.cfir.CfirImplementationDetail::class,
    org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals::class,
)

package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.toCfirResolvedTypeRef
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirTypeAlias
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.builder.buildTypeAlias
import org.cangnova.cangjie.cfir.declarations.builder.buildInterface
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.symbols.CfirTypeAliasSymbol
import org.cangnova.cangjie.cfir.types.ConeTypeAliasType
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.cfir.types.builder.buildErrorTypeRef
import org.cangnova.cangjie.cfir.types.CfirErrorTypeRef
import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.session.directSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.session.superTypeGraphStoreOrNull
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.scopes.impl.CfirClassMemberScopeKind
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.cfir.declarations.EmptyDeprecationsProvider
import org.cangnova.cangjie.cfir.declarations.impl.CfirClassImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirInterfaceImpl
import org.cangnova.cangjie.cfir.declarations.initDefaultResolveState
import org.cangnova.cangjie.cfir.declarations.isInReferenceCycle
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.cfir.resolve.services.CfirTypeAwareSupertypeProviderImpl
import org.cangnova.cangjie.cfir.resolve.services.CfirSuperTypeGraphStore
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.cfir.session.typeAwareSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.ConeClassLikeLookupTagWithFixedSymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.symbols.CfirDummyCompilerLazyDeclarationResolver
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.CfirLazyDeclarationResolver
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.CfirTypeRefOrigin
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeInferenceContext
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.TypeComponents
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Test

/**
 * [CfirTypeAwareSupertypeProvider] 与类型检查器集成的超类型查询测试。
 */
class CfirTypeAwareSupertypeProviderTest {
    /** 同一TP的重复出现使用类型语义等值；不同物理RawArray表示不能改变extend适用性。 */
    @Test
    fun `repeated extend parameter accepts all equal raw array representations`() {
        val (session, module) = ExtendTestFixtures.newSessionAndModule()
        val core = CoreModel(module)
        val pairId = ClassId(FqName("sample.rawrepeat"), Name.identifier("Pair"))
        val viewId = ClassId(FqName("sample.rawrepeat"), Name.identifier("View"))
        val pair = newClass(
            module, pairId,
            listOf(ExtendTestFixtures.newTypeParameter(module, "Left"), ExtendTestFixtures.newTypeParameter(module, "Right")),
            superTypeRefs = listOf(core.objectRef()),
        )
        val view = newInterface(module, viewId, listOf(ExtendTestFixtures.newTypeParameter(module, "Element")))
        val parameter = ExtendTestFixtures.newTypeParameter(module, "T")
        val extension = ExtendTestFixtures.newExtend(moduleData = module, typeParameters = listOf(parameter),
            extendedTypeRef = ExtendTestFixtures.classTypeRef(pairId, listOf(typeParameterType(parameter), typeParameterType(parameter))),
            superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(viewId, listOf(typeParameterType(parameter)), isInterface = true)))
        val context = session.registerTestTypeContext(core.declarations + listOf(pair, view), listOf(extension))
        fun raw(element: ConeCangJieType, dimensions: UInt = 1u): ConeClassLikeType =
            ConeClassLikeType(StdlibClassIds.RawArray.toLookupTag(), listOf(element), rawArrayDimensions = dimensions)
        val nested = raw(raw(ConePrimitiveType.INT64))
        val flat = raw(ConePrimitiveType.INT64, 2u)
        val provider = requireNotNull(session.typeAwareSupertypeProviderOrNull)
        for ((first, second) in listOf(nested to nested, flat to flat, nested to flat, flat to nested)) {
            val receiver = classType(pairId, first, second)
            val descriptor = provider.getDirectSupertypeDescriptors(receiver).single { it.origin is CfirInstantiatedSupertypeOrigin.Extend }
            assertTrue(AbstractTypeChecker.equalTypes(context, interfaceType(viewId, first), descriptor.type))
            assertSame(extension, (descriptor.origin as CfirInstantiatedSupertypeOrigin.Extend).sourceExtend)
        }
        for (other in listOf(raw(ConePrimitiveType.INT64), raw(ConePrimitiveType.BOOLEAN, 2u))) {
            assertTrue(provider.getDirectSupertypeDescriptors(classType(pairId, flat, other))
                .none { it.origin is CfirInstantiatedSupertypeOrigin.Extend })
        }
    }

    /** 正常 provider 的同布局两种 receiver 必须实例化出相同的降维接口实参。 */
    @Test
    fun `raw array extend uses declaration arguments for nested and loaded receivers`() {
        val (session, module) = ExtendTestFixtures.newSessionAndModule()
        val core = CoreModel(module)
        session.register(CfirCangJieScopeProvider::class, CfirCangJieScopeProvider())
        val arrayDeclaration = requireNotNull(CfirBuiltinSymbolProvider(session, module)
            .getClassLikeSymbolByClassId(StdlibClassIds.RawArray)).cfir
        val interfaceId = ClassId(FqName("sample.rawlayout"), Name.identifier("ElementView"))
        val interfaceParameter = ExtendTestFixtures.newTypeParameter(module, "Element")
        val requirement = newInterface(module, interfaceId, listOf(interfaceParameter))
        val parameter = ExtendTestFixtures.newTypeParameter(module, "T")
        val extension = ExtendTestFixtures.newExtend(moduleData = module, typeParameters = listOf(parameter),
            extendedTypeRef = ExtendTestFixtures.classTypeRef(StdlibClassIds.RawArray, listOf(typeParameterType(parameter))),
            superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(interfaceId, listOf(typeParameterType(parameter)), isInterface = true)))
        session.registerTestTypeContext(core.declarations + listOf(arrayDeclaration, requirement), listOf(extension))
        fun raw(element: ConeCangJieType, dimensions: UInt = 1u): ConeClassLikeType =
            ConeClassLikeType(StdlibClassIds.RawArray.toLookupTag(), listOf(element), rawArrayDimensions = dimensions)
        val leaf = ConePrimitiveType.INT64
        val element = raw(leaf)
        val expected = interfaceType(interfaceId, element)
        for (receiver in listOf(raw(element), raw(leaf, 2u))) {
            val descriptors = requireNotNull(session.typeAwareSupertypeProviderOrNull).getDirectSupertypeDescriptors(receiver)
            val projected = descriptors.single { it.origin is CfirInstantiatedSupertypeOrigin.Extend }
            assertEquals(expected, projected.type)
            assertSame(extension, (projected.origin as CfirInstantiatedSupertypeOrigin.Extend).sourceExtend)
        }
    }

    /**
     * 验证泛型 extend 会按接收者具体类型实参实例化并参与子类型遍历。
     */
    @Test
    fun `generic extend is instantiated for concrete type and subtype traversal`() {
        val packageFqName = FqName("sample.generic")
        val boxClassId = ClassId(packageFqName, Name.identifier("Box"))
        val iterableClassId = ClassId(packageFqName, Name.identifier("Iterable"))

        val (session, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val core = CoreModel(moduleData)
        val typeParameter = ExtendTestFixtures.newTypeParameter(moduleData, "T")
        val boxClass = newClass(
            moduleData = moduleData,
            classId = boxClassId,
            typeParameters = listOf(typeParameter),
            superTypeRefs = listOf(core.objectRef()),
        )
        val iterableInterface = newInterface(
            moduleData = moduleData,
            classId = iterableClassId,
            typeParameters = listOf(typeParameter),
        )
        val boxExtend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            typeParameters = listOf(typeParameter),
            extendedTypeRef = ExtendTestFixtures.classTypeRef(
                classId = boxClassId,
                typeArguments = listOf(typeParameterType(typeParameter)),
            ),
            superTypeRefs = listOf(
                ExtendTestFixtures.classTypeRef(
                    classId = iterableClassId,
                    typeArguments = listOf(typeParameterType(typeParameter)),
                    isInterface = true,
                )
            ),
        )

        val typeContext = session.registerTestTypeContext(
            declarations = core.declarations + listOf(boxClass, iterableInterface),
            extends = listOf(boxExtend),
        )

        val boxInt = classType(boxClassId, ConePrimitiveType.INT32)
        val iterableInt = interfaceType(iterableClassId, ConePrimitiveType.INT32)
        val iterableExtendDescriptor = session.typeAwareSupertypeProviderOrNull
            ?.getDirectSupertypeDescriptors(boxInt)
            ?.single { it.origin is CfirInstantiatedSupertypeOrigin.Extend }

        assertIterableEquals(
            listOf(objectType(), iterableInt),
            session.typeAwareSupertypeProviderOrNull?.getDirectSupertypes(boxInt),
        )
        assertEquals(iterableInt, iterableExtendDescriptor?.type)
        assertEquals(
            interfaceType(iterableClassId, typeParameterType(typeParameter)),
            iterableExtendDescriptor?.provenanceType,
        )
        assertTrue(AbstractTypeChecker.isSubtypeOf(typeContext, boxInt, iterableInt))
        assertTrue(
            with(typeContext) {
                boxInt.anySuperTypeConstructor { supertype ->
                    val constructor = supertype.typeConstructor()
                    constructor is org.cangnova.cangjie.cfir.types.ConeClassLikeLookupTag &&
                            constructor.classId == iterableClassId
                }
            }
        )
    }

    /** 泛型父边同时保留实例化类型与声明中的原始类型身份。 */
    @Test
    fun `declared generic supertype keeps its pre-instantiation provenance`() {
        val packageFqName = FqName("sample.provenance")
        val childClassId = ClassId(packageFqName, Name.identifier("Child"))
        val parentInterfaceId = ClassId(packageFqName, Name.identifier("Parent"))

        val (session, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val core = CoreModel(moduleData)
        val childTypeParameter = ExtendTestFixtures.newTypeParameter(moduleData, "T")
        val parentTypeParameter = ExtendTestFixtures.newTypeParameter(moduleData, "T")
        val parentInterface = newInterface(
            moduleData = moduleData,
            classId = parentInterfaceId,
            typeParameters = listOf(parentTypeParameter),
        )
        val childClass = newClass(
            moduleData = moduleData,
            classId = childClassId,
            typeParameters = listOf(childTypeParameter),
            superTypeRefs = listOf(
                core.objectRef(),
                ExtendTestFixtures.classTypeRef(
                    classId = parentInterfaceId,
                    typeArguments = listOf(typeParameterType(childTypeParameter)),
                    isInterface = true,
                ),
            ),
        )
        session.registerTestTypeContext(
            declarations = core.declarations + listOf(childClass, parentInterface),
            extends = emptyList(),
        )

        val childInt = classType(childClassId, ConePrimitiveType.INT32)
        val parentInt = interfaceType(parentInterfaceId, ConePrimitiveType.INT32)
        val descriptor = session.typeAwareSupertypeProviderOrNull
            ?.getDirectSupertypeDescriptors(childInt)
            ?.single { it.type.classId == parentInterfaceId }

        assertEquals(parentInt, descriptor?.type)
        assertSame(childClass.superTypeRefs[1], (descriptor?.origin as CfirInstantiatedSupertypeOrigin.Declared).sourceTypeRef)
        assertEquals(
            interfaceType(parentInterfaceId, typeParameterType(childTypeParameter)),
            descriptor?.provenanceType,
        )
    }

    /**
     * 验证父类上的 extend 会被具体子类继承。
     */
    @Test
    fun `extends on superclass are inherited by concrete child type`() {
        val packageFqName = FqName("sample.inherited")
        val baseClassId = ClassId(packageFqName, Name.identifier("Base"))
        val childClassId = ClassId(packageFqName, Name.identifier("Child"))
        val markerClassId = ClassId(packageFqName, Name.identifier("Marker"))

        val (session, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val core = CoreModel(moduleData)
        val typeParameter = ExtendTestFixtures.newTypeParameter(moduleData, "T")
        val baseClass = newClass(
            moduleData = moduleData,
            classId = baseClassId,
            typeParameters = listOf(typeParameter),
            superTypeRefs = listOf(core.objectRef()),
        )
        val childClass = newClass(
            moduleData = moduleData,
            classId = childClassId,
            superTypeRefs = listOf(
                ExtendTestFixtures.classTypeRef(
                    classId = baseClassId,
                    typeArguments = listOf(ConePrimitiveType.INT32),
                )
            ),
        )
        val markerInterface = newInterface(
            moduleData = moduleData,
            classId = markerClassId,
            typeParameters = listOf(typeParameter),
        )
        val baseExtend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            typeParameters = listOf(typeParameter),
            extendedTypeRef = ExtendTestFixtures.classTypeRef(
                classId = baseClassId,
                typeArguments = listOf(typeParameterType(typeParameter)),
            ),
            superTypeRefs = listOf(
                ExtendTestFixtures.classTypeRef(
                    classId = markerClassId,
                    typeArguments = listOf(typeParameterType(typeParameter)),
                    isInterface = true,
                )
            ),
        )

        val typeContext = session.registerTestTypeContext(
            declarations = core.declarations + listOf(baseClass, childClass, markerInterface),
            extends = listOf(baseExtend),
        )

        val childType = classType(childClassId)
        val baseInt = classType(baseClassId, ConePrimitiveType.INT32)
        val markerInt = interfaceType(markerClassId, ConePrimitiveType.INT32)

        assertEquals(
            listOf(baseInt, markerInt),
            session.typeAwareSupertypeProviderOrNull?.getDirectSupertypes(childType),
        )
        assertTrue(AbstractTypeChecker.isSubtypeOf(typeContext, childType, markerInt))
    }

    /**
     * 验证类型比较通过 extend provider 而非声明图获得跨文件扩展父类型。
     */
    @Test
    fun `type comparison uses extend provider instead of declaration graph`() {
        val packageFqName = FqName("sample.crossfile")
        val targetClassId = ClassId(packageFqName, Name.identifier("Target"))
        val parentInterfaceId = ClassId(packageFqName, Name.identifier("Parent"))

        val (session, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val core = CoreModel(moduleData)
        val targetClass = newClass(moduleData, targetClassId, superTypeRefs = listOf(core.objectRef()))
        val parentInterface = newInterface(moduleData, parentInterfaceId)
        val extend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(parentInterfaceId, isInterface = true)),
        )

        val typeContext = session.registerTestTypeContext(
            declarations = core.declarations + listOf(targetClass, parentInterface),
            extends = listOf(extend),
        )

        assertTrue(
            AbstractTypeChecker.isSubtypeOf(
                typeContext,
                classType(targetClassId),
                interfaceType(parentInterfaceId),
            )
        )
    }

    /**
     * 验证受上界约束的泛型 extend 使用接收者具体类型实参做子类型遍历。
     */
    @Test
    fun `constrained generic extend uses concrete receiver arguments for subtype traversal`() {
        val packageFqName = FqName("sample.constrained")
        val eqClassId = ClassId(packageFqName, Name.identifier("EQ"))
        val aClassId = ClassId(packageFqName, Name.identifier("A"))
        val bClassId = ClassId(packageFqName, Name.identifier("B"))

        val (session, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val core = CoreModel(moduleData)
        val eqInterface = newInterface(moduleData, eqClassId)
        val typeParameter = ExtendTestFixtures.newTypeParameter(
            moduleData = moduleData,
            name = "T",
            bounds = listOf(ExtendTestFixtures.classTypeRef(eqClassId, isInterface = true)),
        )
        val aClass = newClass(
            moduleData = moduleData,
            classId = aClassId,
            typeParameters = listOf(typeParameter),
            superTypeRefs = listOf(core.objectRef()),
        )
        val bInterface = newInterface(
            moduleData = moduleData,
            classId = bClassId,
            typeParameters = listOf(typeParameter),
        )
        val int32EqExtend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = primitiveTypeRef(ConePrimitiveType.INT32),
            superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(eqClassId, isInterface = true)),
        )
        val constrainedAExtend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            typeParameters = listOf(typeParameter),
            extendedTypeRef = ExtendTestFixtures.classTypeRef(
                classId = aClassId,
                typeArguments = listOf(typeParameterType(typeParameter)),
            ),
            superTypeRefs = listOf(
                ExtendTestFixtures.classTypeRef(
                    classId = bClassId,
                    typeArguments = listOf(typeParameterType(typeParameter)),
                    isInterface = true,
                )
            ),
        )

        val typeContext = session.registerTestTypeContext(
            declarations = core.declarations + listOf(eqInterface, aClass, bInterface),
            extends = listOf(int32EqExtend, constrainedAExtend),
        )

        val aInt32 = classType(aClassId, ConePrimitiveType.INT32)
        val aInt64 = classType(aClassId, ConePrimitiveType.INT64)
        val bInt32 = interfaceType(bClassId, ConePrimitiveType.INT32)
        val bInt64 = interfaceType(bClassId, ConePrimitiveType.INT64)

        assertIterableEquals(
            listOf(objectType(), bInt32),
            session.typeAwareSupertypeProviderOrNull?.getDirectSupertypes(aInt32),
        )
        assertTrue(AbstractTypeChecker.isSubtypeOf(typeContext, aInt32, bInt32))
        // BFS 通过构造器级 supertypes 遍历声明型父类型，use-site 约束过滤发生在
        // provider 的 getDirectSupertypes 入口：A<Int64> 不满足 `T <: EQ`，因此不贡献 B<Int64>。
        assertIterableEquals(
            listOf(objectType()),
            session.typeAwareSupertypeProviderOrNull?.getDirectSupertypes(aInt64),
        )
    }

    /**
     * 官方 `HasExtendInterfaceTyHelper` 的 direct-TyVar-only 映射规则：
     * 嵌套形状 extend（`extend<X> Box<List<X>> <: Iterable<X>`）在结构化成员视图中
     * 贡献 `Iterable<Int32>`，但在子类型谓词视图中因接口实参残留 extend 类型形参
     * 而不成立（cjc 对应 assign_012 的 mismatched types）。
     */
    @Test
    fun `nested shape extend is invisible to subtype predicate but visible to member view`() {
        val packageFqName = FqName("sample.nested")
        val boxClassId = ClassId(packageFqName, Name.identifier("Box"))
        val listClassId = ClassId(packageFqName, Name.identifier("List"))
        val iterableClassId = ClassId(packageFqName, Name.identifier("Iterable"))

        val (session, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val core = CoreModel(moduleData)
        val boxTypeParameter = ExtendTestFixtures.newTypeParameter(moduleData, "T")
        val listTypeParameter = ExtendTestFixtures.newTypeParameter(moduleData, "E")
        val extendTypeParameter = ExtendTestFixtures.newTypeParameter(moduleData, "X")
        val boxClass = newClass(moduleData, boxClassId, typeParameters = listOf(boxTypeParameter), superTypeRefs = listOf(core.objectRef()))
        val listClass = newClass(moduleData, listClassId, typeParameters = listOf(listTypeParameter), superTypeRefs = listOf(core.objectRef()))
        val iterableInterface = newInterface(
            moduleData = moduleData,
            classId = iterableClassId,
            typeParameters = listOf(extendTypeParameter),
        )
        val nestedExtend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            typeParameters = listOf(extendTypeParameter),
            extendedTypeRef = ExtendTestFixtures.classTypeRef(
                classId = boxClassId,
                typeArguments = listOf(classType(listClassId, typeParameterType(extendTypeParameter))),
            ),
            superTypeRefs = listOf(
                ExtendTestFixtures.classTypeRef(
                    classId = iterableClassId,
                    typeArguments = listOf(typeParameterType(extendTypeParameter)),
                    isInterface = true,
                )
            ),
        )

        val typeContext = session.registerTestTypeContext(
            declarations = core.declarations + listOf(boxClass, listClass, iterableInterface),
            extends = listOf(nestedExtend),
        )

        val boxListOfInt = classType(boxClassId, classType(listClassId, ConePrimitiveType.INT32))
        val iterableInt = interfaceType(iterableClassId, ConePrimitiveType.INT32)
        val supertypeProvider = session.typeAwareSupertypeProviderOrNull

        // 成员图保持官方 GetAllExtendInterfaceTy 的结构化语义。
        assertIterableEquals(
            listOf(objectType(), iterableInt),
            supertypeProvider?.getDirectSupertypes(boxListOfInt),
        )
        // 子类型谓词保持官方 HasExtendInterfaceTyHelper 的 direct-TyVar-only 语义。
        assertIterableEquals(
            listOf(objectType()),
            supertypeProvider?.getPredicateSupertypes(boxListOfInt),
        )
        assertTrue(!AbstractTypeChecker.isSubtypeOf(typeContext, boxListOfInt, iterableInt))
    }

    /**
     * 嵌套形状 extend 的接口实参为常量时无需嵌套映射，谓词视图保持该边
     * （官方探针 `extend<X> A<B<X>> <: I<Int64>` 赋值通过）。
     */
    @Test
    fun `nested shape extend with constant interface argument keeps predicate edge`() {
        val packageFqName = FqName("sample.constant")
        val boxClassId = ClassId(packageFqName, Name.identifier("Box"))
        val listClassId = ClassId(packageFqName, Name.identifier("List"))
        val counterClassId = ClassId(packageFqName, Name.identifier("Counter"))

        val (session, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val core = CoreModel(moduleData)
        val boxTypeParameter = ExtendTestFixtures.newTypeParameter(moduleData, "T")
        val extendTypeParameter = ExtendTestFixtures.newTypeParameter(moduleData, "X")
        val boxClass = newClass(moduleData, boxClassId, typeParameters = listOf(boxTypeParameter), superTypeRefs = listOf(core.objectRef()))
        val listClass = newClass(moduleData, listClassId, typeParameters = listOf(boxTypeParameter), superTypeRefs = listOf(core.objectRef()))
        val counterInterface = newInterface(moduleData, counterClassId)
        val constantExtend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            typeParameters = listOf(extendTypeParameter),
            extendedTypeRef = ExtendTestFixtures.classTypeRef(
                classId = boxClassId,
                typeArguments = listOf(classType(listClassId, typeParameterType(extendTypeParameter))),
            ),
            superTypeRefs = listOf(
                ExtendTestFixtures.classTypeRef(counterClassId, isInterface = true)
            ),
        )

        val typeContext = session.registerTestTypeContext(
            declarations = core.declarations + listOf(boxClass, listClass, counterInterface),
            extends = listOf(constantExtend),
        )

        val boxListOfInt = classType(boxClassId, classType(listClassId, ConePrimitiveType.INT32))
        assertTrue(
            AbstractTypeChecker.isSubtypeOf(typeContext, boxListOfInt, interfaceType(counterClassId))
        )
    }

    /**
     * 混合形状：只要接口实参不依赖"仅嵌套出现"的 extend 类型形参，谓词视图就保留该边
     * （官方探针 `extend<X, Y> A<X, B<Y>> <: I<X>` 与 `extend<X> A<X> <: I<B<X>>` 均赋值通过）。
     */
    @Test
    fun `predicate edge survives when interface arguments avoid nested-only parameters`() {
        val packageFqName = FqName("sample.mixed")
        val pairClassId = ClassId(packageFqName, Name.identifier("Pair2"))
        val wrapClassId = ClassId(packageFqName, Name.identifier("Wrap"))
        val iterableClassId = ClassId(packageFqName, Name.identifier("Iterable"))

        val (session, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val core = CoreModel(moduleData)
        val firstParameter = ExtendTestFixtures.newTypeParameter(moduleData, "F")
        val secondParameter = ExtendTestFixtures.newTypeParameter(moduleData, "S")
        val extendFirst = ExtendTestFixtures.newTypeParameter(moduleData, "X")
        val extendSecond = ExtendTestFixtures.newTypeParameter(moduleData, "Y")
        val pairClass = newClass(
            moduleData = moduleData,
            classId = pairClassId,
            typeParameters = listOf(firstParameter, secondParameter),
            superTypeRefs = listOf(core.objectRef()),
        )
        val wrapClass = newClass(moduleData, wrapClassId, typeParameters = listOf(firstParameter), superTypeRefs = listOf(core.objectRef()))
        val iterableInterface = newInterface(
            moduleData = moduleData,
            classId = iterableClassId,
            typeParameters = listOf(extendFirst),
        )
        val mixedShapeExtend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            typeParameters = listOf(extendFirst, extendSecond),
            extendedTypeRef = ExtendTestFixtures.classTypeRef(
                classId = pairClassId,
                typeArguments = listOf(
                    typeParameterType(extendFirst),
                    classType(wrapClassId, typeParameterType(extendSecond)),
                ),
            ),
            superTypeRefs = listOf(
                ExtendTestFixtures.classTypeRef(
                    classId = iterableClassId,
                    typeArguments = listOf(typeParameterType(extendFirst)),
                    isInterface = true,
                )
            ),
        )

        val typeContext = session.registerTestTypeContext(
            declarations = core.declarations + listOf(pairClass, wrapClass, iterableInterface),
            extends = listOf(mixedShapeExtend),
        )

        val pairIntWrapInt = classType(
            pairClassId,
            ConePrimitiveType.INT32,
            classType(wrapClassId, ConePrimitiveType.INT32),
        )
        val iterableInt = interfaceType(iterableClassId, ConePrimitiveType.INT32)
        val supertypeProvider = session.typeAwareSupertypeProviderOrNull

        assertIterableEquals(
            listOf(objectType(), iterableInt),
            supertypeProvider?.getPredicateSupertypes(pairIntWrapInt),
        )
        assertTrue(AbstractTypeChecker.isSubtypeOf(typeContext, pairIntWrapInt, iterableInt))
    }

    /** extend 索引不变时，普通图完成状态仍必须使已缓存的继承成员父边失效。 */
    @Test
    fun `published reference cycle state invalidates cached inherited target`() {
        val (session, module) = ExtendTestFixtures.newSessionAndModule()
        val core = CoreModel(module)
        val ownerId = ClassId(FqName("sample.cycle"), Name.identifier("Owner"))
        val targetId = ClassId(FqName("sample.cycle"), Name.identifier("Target"))
        val target = newInterface(module, targetId)
        val targetRef = ExtendTestFixtures.classTypeRef(targetId, isInterface = true)
        val owner = newClass(module, ownerId, superTypeRefs = listOf(core.objectRef(), targetRef))
        session.registerTestTypeContext(core.declarations + listOf(owner, target), emptyList())
        val graph = CfirSuperTypeGraphStore()
        session.register(CfirDirectSupertypeProvider::class, graph)
        val objectRef = owner.superTypeRefs.first() as CfirResolvedTypeRef
        graph.recordDeclared(owner, owner.superTypeRefs, listOf(
            CfirSuperTypeGraphEdge(objectRef, "Object", core.objectDeclaration.symbol),
            CfirSuperTypeGraphEdge(targetRef, "Target", target.symbol),
        ))
        graph.recordDeclared(core.objectDeclaration, listOf(core.anyRef), listOf(CfirSuperTypeGraphEdge(core.anyRef, "Any", core.any.symbol)))
        graph.recordDeclared(core.any, emptyList(), emptyList())
        graph.recordDeclared(target, emptyList(), emptyList())
        graph.publishCompletedProducts()
        val provider = checkNotNull(session.typeAwareSupertypeProviderOrNull)
        val ownerType = classType(ownerId)

        assertTrue(provider.getDirectSupertypeDescriptors(ownerType).any { it.type.classId == targetId })
        target.isInReferenceCycle = true
        graph.recordDeclared(target, emptyList(), emptyList())
        graph.publishCompletedProducts()
        assertTrue(provider.getDirectSupertypeDescriptors(ownerType).none { it.type.classId == targetId })
        assertSame(objectRef, owner.superTypeRefs.first())
        assertSame(targetRef, owner.superTypeRefs[1])
    }

    /** Source target/alias 的循环状态来自其所属发布图，owner 图版本不变也必须退役父输入。 */
    @Test
    fun `source target and alias cycle publication independently invalidate owner graph parents`() {
        for (cycleOwnerIsAlias in listOf(false, true)) {
            val (session, module) = ExtendTestFixtures.newSessionAndModule("source-cycle-dependency")
            val (declarationSession, declarationModule) = ExtendTestFixtures.newSessionAndModule("source-cycle-declarations")
            val core = CoreModel(module)
            val packageName = FqName("sample.sourcecycles")
            val targetId = ClassId(packageName, Name.identifier("Target"))
            val aliasId = ClassId(packageName, Name.identifier("TargetAlias"))
            val hostId = ClassId(packageName, Name.identifier("Host"))
            // 生命周期模型沿用仓库 binarySource 约定，携带来源而不生产任何 body 产品。
            val target = buildInterface {
                source = object : CjBinarySourceElement("Target", null, "cycle:Target") {}
                moduleData = declarationModule
                resolvePhase = CfirResolvePhase.SUPER_TYPES
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes()
                status = CfirDeclarationStatusImpl()
                scopeProvider = CfirCangJieScopeProvider()
                symbol = CfirInterfaceSymbol(targetId)
                name = targetId.shortClassName
            }
            val rhs = ExtendTestFixtures.classTypeRef(targetId, isInterface = true)
            val alias = buildTypeAlias {
                source = object : CjBinarySourceElement("TargetAlias", null, "cycle:TargetAlias") {}
                moduleData = declarationModule
                resolvePhase = CfirResolvePhase.SUPER_TYPES
                origin = CfirDeclarationOrigin.Source
                attributes = CfirDeclarationAttributes()
                status = CfirDeclarationStatusImpl()
                scopeProvider = CfirCangJieScopeProvider()
                symbol = CfirTypeAliasSymbol(aliasId)
                name = aliasId.shortClassName
                expandedTypeRef = rhs
            }
            val objectRef = core.objectRef()
            val aliasRef = ConeTypeAliasType(aliasId, expandedType = rhs.coneType).toCfirResolvedTypeRef(null)
            val host = newClass(module, hostId, superTypeRefs = listOf(objectRef, aliasRef))
            val declarations = core.declarations + listOf(host, target, alias)
            declarationSession.registerTestTypeContext(listOf(target, alias), emptyList())
            session.registerTestTypeContext(declarations, emptyList())
            val sourceGraph = checkNotNull(declarationSession.superTypeGraphStoreOrNull)
            val ownerGraph = checkNotNull(session.superTypeGraphStoreOrNull)
            assertNotSame(sourceGraph, ownerGraph)
            assertSame(declarationSession, target.moduleData.session)
            assertSame(declarationSession, alias.moduleData.session)
            val ownerRevision = ownerGraph.modificationCount
            val provider = checkNotNull(session.typeAwareSupertypeProviderOrNull)
            val hostType = classType(hostId)
            val cycleOwner = if (cycleOwnerIsAlias) alias else target
            fun targetDescriptor(): CfirInstantiatedSupertypeDescriptor? =
                provider.getDirectSupertypeDescriptors(hostType).singleOrNull { it.type.classId == targetId }

            val before = checkNotNull(targetDescriptor())
            assertSame(aliasRef, (before.origin as CfirInstantiatedSupertypeOrigin.Declared).sourceTypeRef)
            assertEquals(false, sourceGraph.getPublishedReferenceCycleState(cycleOwner.symbol))
            assertSame(rhs, checkNotNull(sourceGraph.getCompletedSupertypeProduct(alias.symbol)).superTypeRefs.single())
            assertTrue(provider.inputStamp.dependencies.any { it.sources.directSupertypes === sourceGraph })
            val sourceRevision = sourceGraph.modificationCount
            // alias 子例模拟真实自环断开产品；单侧 true 配合合法 Target RHS 不是合法输入。
            val nextRhs = if (cycleOwnerIsAlias) {
                val recursiveRhs = ConeTypeAliasType(aliasId).toCfirResolvedTypeRef(alias.source)
                buildErrorTypeRef {
                    source = recursiveRhs.source
                    diagnostic = ConeSimpleDiagnostic("TargetAlias->TargetAlias", DiagnosticKind.RecursiveTypealiasExpansion)
                    delegatedTypeRef = recursiveRhs
                }
            } else rhs
            cycleOwner.isInReferenceCycle = true
            if (cycleOwnerIsAlias) alias.replaceExpandedTypeRef(nextRhs)
            assertEquals(false, sourceGraph.getPublishedReferenceCycleState(cycleOwner.symbol))
            val unpublished = checkNotNull(targetDescriptor())
            assertEquals(before.type, unpublished.type)
            assertSame(aliasRef, (unpublished.origin as CfirInstantiatedSupertypeOrigin.Declared).sourceTypeRef)
            assertSame(rhs, checkNotNull(sourceGraph.getCompletedSupertypeProduct(alias.symbol)).superTypeRefs.single())
            assertEquals(sourceRevision, sourceGraph.modificationCount)

            publishTestProducts(sourceGraph, declarationSession.symbolProvider, listOf(cycleOwner))
            sourceGraph.publishCompletedProducts()
            assertEquals(true, sourceGraph.getPublishedReferenceCycleState(cycleOwner.symbol))
            if (cycleOwnerIsAlias) {
                val publishedError = assertInstanceOf(CfirErrorTypeRef::class.java,
                    checkNotNull(sourceGraph.getCompletedSupertypeProduct(alias.symbol)).superTypeRefs.single())
                assertSame(nextRhs, publishedError)
                assertEquals(DiagnosticKind.RecursiveTypealiasExpansion,
                    assertInstanceOf(ConeSimpleDiagnostic::class.java, publishedError.diagnostic).kind)
                assertEquals(aliasId, assertInstanceOf(ConeTypeAliasType::class.java,
                    assertInstanceOf(CfirResolvedTypeRef::class.java, publishedError.delegatedTypeRef).coneType).classId)
                assertEquals(false, sourceGraph.getPublishedReferenceCycleState(target.symbol))
            }
            assertEquals(false, ownerGraph.getPublishedReferenceCycleState(cycleOwner.symbol))
            assertEquals(ownerRevision, ownerGraph.modificationCount)
            assertNull(targetDescriptor())
            cycleOwner.isInReferenceCycle = false
            if (cycleOwnerIsAlias) alias.replaceExpandedTypeRef(rhs)
            assertEquals(true, sourceGraph.getPublishedReferenceCycleState(cycleOwner.symbol))
            assertNull(targetDescriptor())
            publishTestProducts(sourceGraph, declarationSession.symbolProvider, listOf(cycleOwner))
            sourceGraph.publishCompletedProducts()
            val restored = checkNotNull(targetDescriptor())
            assertSame(rhs, checkNotNull(sourceGraph.getCompletedSupertypeProduct(alias.symbol)).superTypeRefs.single())
            assertSame(aliasRef, (restored.origin as CfirInstantiatedSupertypeOrigin.Declared).sourceTypeRef)
            assertSame(objectRef, host.superTypeRefs.first())
            assertSame(aliasRef, host.superTypeRefs[1])
            assertSame(rhs, alias.expandedTypeRef)
            assertEquals(ownerRevision, ownerGraph.modificationCount)
        }
    }

    /** R4：外部 owner 的 graph identity 是依赖，不能仅比较完成版本数。 */
    @Test
    fun `source owner graph replacement at equal revision invalidates typed parents`() {
        val (session, module) = ExtendTestFixtures.newSessionAndModule("owner-graph-reader")
        val (ownerSession, ownerModule) = ExtendTestFixtures.newSessionAndModule("owner-graph-writer")
        val packageName = FqName("sample.ownergraphidentity")
        val a = newInterface(module, ClassId(packageName, Name.identifier("A")))
        val b = newInterface(module, ClassId(packageName, Name.identifier("B")))
        val aRef = a.symbol.constructType().toCfirResolvedTypeRef(null)
        val bRef = b.symbol.constructType().toCfirResolvedTypeRef(null)
        val owner = newSourceInterface(ownerModule, ClassId(packageName, Name.identifier("Owner")), listOf(aRef))
        val declarations = listOf<CfirClassLikeDeclaration>(a, b, owner)
        ownerSession.registerTestTypeContext(declarations, emptyList())
        session.registerTestTypeContext(declarations, emptyList())
        val oldGraph = checkNotNull(ownerSession.superTypeGraphStoreOrNull)
        val readerGraph = checkNotNull(session.superTypeGraphStoreOrNull)
        val readerRevision = readerGraph.modificationCount
        val provider = checkNotNull(session.typeAwareSupertypeProviderOrNull)
        val ownerType = owner.symbol.constructType()
        val before = provider.getDirectSupertypeDescriptors(ownerType).single()
        assertSame(aRef, (before.origin as CfirInstantiatedSupertypeOrigin.Declared).sourceTypeRef)
        assertTrue(provider.inputStamp.dependencies.any { it.sources.directSupertypes === oldGraph })

        val replacement = CfirSuperTypeGraphStore()
        publishTestProducts(replacement, ownerSession.symbolProvider, listOf(a, b))
        replacement.recordDeclared(owner, listOf(bRef), listOf(CfirSuperTypeGraphEdge(bRef, "B", b.symbol)))
        replacement.publishCompletedProducts()
        assertEquals(oldGraph.modificationCount, replacement.modificationCount)
        ownerSession.register(CfirDirectSupertypeProvider::class, replacement)
        ownerSession.register(CfirSuperTypeGraphStore::class, replacement)
        val after = provider.getDirectSupertypeDescriptors(ownerType).single()
        assertEquals(bRef.coneType, after.type)
        assertSame(bRef, (after.origin as CfirInstantiatedSupertypeOrigin.Declared).sourceTypeRef)
        assertNotSame(before, after)
        assertSame(aRef, owner.superTypeRefs.single())
        assertEquals(readerRevision, readerGraph.modificationCount)
        assertTrue(provider.inputStamp.dependencies.any { it.sources.directSupertypes === replacement })
        assertFalse(provider.inputStamp.dependencies.any { it.sources.directSupertypes === oldGraph })
    }

    /** R5：同名 fixed 目标的反向循环状态必须按实际 symbol 过滤。 */
    @Test
    fun `selected symbols preserve same id fixed targets with opposite published cycle states`() {
        val (session, module) = ExtendTestFixtures.newSessionAndModule("fixed-cycle-reader")
        val core = CoreModel(module)
        val packageName = FqName("sample.fixedcycles")
        val targetId = ClassId(packageName, Name.identifier("Target"))
        val firstSymbol = CfirInterfaceSymbol(targetId, useFixedSymbolLookupTag = true)
        val secondSymbol = CfirInterfaceSymbol(targetId, useFixedSymbolLookupTag = true)
        val first = newSourceInterface(module, targetId, emptyList(), firstSymbol)
        val second = newSourceInterface(module, targetId, emptyList(), secondSymbol)
        val firstRef = firstSymbol.constructType().toCfirResolvedTypeRef(null)
        val secondRef = secondSymbol.constructType().toCfirResolvedTypeRef(null)
        val firstObjectRef = core.objectRef()
        val secondObjectRef = core.objectRef()
        val firstHost = newClass(module, ClassId(packageName, Name.identifier("FirstHost")), superTypeRefs = listOf(firstObjectRef, firstRef))
        val secondHost = newClass(module, ClassId(packageName, Name.identifier("SecondHost")), superTypeRefs = listOf(secondObjectRef, secondRef))
        first.isInReferenceCycle = true
        session.registerTestTypeContext(core.declarations + listOf(first, second, firstHost, secondHost), emptyList())
        val graph = checkNotNull(session.superTypeGraphStoreOrNull)
        val selectedSymbols = TestSymbolProvider(session, core.declarations + listOf(firstHost, secondHost, second))
        val selectedGraph = CfirSuperTypeGraphStore()
        publishTestProducts(selectedGraph, selectedSymbols, core.declarations + listOf(firstHost, secondHost))
        selectedGraph.recordDeclared(first, emptyList(), emptyList())
        selectedGraph.recordDeclared(second, emptyList(), emptyList())
        selectedGraph.publishCompletedProducts()
        val provider = checkNotNull(session.typeAwareSupertypeProviderOrNull).forSources(
            CfirTypeAwareSupertypeSources(session, selectedSymbols, session.extendProvider, selectedGraph),
        )
        assertSame(secondSymbol, selectedSymbols.getClassLikeSymbolByClassId(targetId))
        val firstTag = assertInstanceOf(ConeClassLikeLookupTagWithFixedSymbol::class.java,
            assertInstanceOf(ConeClassLikeType::class.java, firstRef.coneType).lookupTag)
        assertSame(firstSymbol, firstTag.symbol)
        assertEquals(listOf(objectType()), provider.getDirectSupertypes(firstHost.symbol.constructType()))
        val before = provider.getDirectSupertypeDescriptors(secondHost.symbol.constructType())
        assertEquals(listOf(objectType(), secondRef.coneType), before.map { it.type })
        assertSame(secondRef, (before[1].origin as CfirInstantiatedSupertypeOrigin.Declared).sourceTypeRef)
        assertEquals(true, selectedGraph.getPublishedReferenceCycleState(firstSymbol))
        assertEquals(false, selectedGraph.getPublishedReferenceCycleState(secondSymbol))
        first.isInReferenceCycle = false
        second.isInReferenceCycle = true
        assertSame(secondRef, (provider.getDirectSupertypeDescriptors(secondHost.symbol.constructType())[1].origin as CfirInstantiatedSupertypeOrigin.Declared).sourceTypeRef)
        assertEquals(listOf(objectType()), provider.getDirectSupertypes(firstHost.symbol.constructType()))
        selectedGraph.recordDeclared(first, emptyList(), emptyList())
        selectedGraph.recordDeclared(second, emptyList(), emptyList())
        selectedGraph.publishCompletedProducts()
        val after = provider.getDirectSupertypeDescriptors(firstHost.symbol.constructType())
        assertEquals(listOf(objectType(), firstRef.coneType), after.map { it.type })
        assertSame(firstRef, (after[1].origin as CfirInstantiatedSupertypeOrigin.Declared).sourceTypeRef)
        assertEquals(listOf(objectType()), provider.getDirectSupertypes(secondHost.symbol.constructType()))
        assertSame(firstRef, firstHost.superTypeRefs[1])
        assertSame(secondRef, secondHost.superTypeRefs[1])
        assertEquals(false, selectedGraph.getPublishedReferenceCycleState(firstSymbol))
        assertEquals(true, selectedGraph.getPublishedReferenceCycleState(secondSymbol))
        assertEquals(true, graph.getPublishedReferenceCycleState(firstSymbol))
        assertEquals(false, graph.getPublishedReferenceCycleState(secondSymbol))
    }

    /** R6：extend 接口目标图独立换代；live cycle 不是已发布语义输入。 */
    @Test
    fun `extend target graph publication invalidates typed parents without owner graph change`() {
        val (session, module) = ExtendTestFixtures.newSessionAndModule("extend-target-reader")
        val (targetSession, targetModule) = ExtendTestFixtures.newSessionAndModule("extend-target-owner")
        val core = CoreModel(module)
        val packageName = FqName("sample.extendtargetcycles")
        val target = newSourceInterface(targetModule, ClassId(packageName, Name.identifier("Target")), emptyList())
        val objectRef = core.objectRef()
        val host = newClass(module, ClassId(packageName, Name.identifier("Host")), superTypeRefs = listOf(objectRef))
        val targetRef = target.symbol.constructType().toCfirResolvedTypeRef(null)
        val extension = ExtendTestFixtures.newExtend(module, host.symbol.constructType().toCfirResolvedTypeRef(null), listOf(targetRef))
        targetSession.registerTestTypeContext(listOf(target), emptyList())
        session.registerTestTypeContext(core.declarations + listOf(host, target), listOf(extension))
        val ownerGraph = checkNotNull(session.superTypeGraphStoreOrNull)
        val targetGraph = checkNotNull(targetSession.superTypeGraphStoreOrNull)
        val ownerRevision = ownerGraph.modificationCount
        val extendRevision = session.extendProvider.modificationCount
        val provider = checkNotNull(session.typeAwareSupertypeProviderOrNull)
        fun extendParents() = provider.getDirectSupertypeDescriptors(host.symbol.constructType())
            .filter { it.origin is CfirInstantiatedSupertypeOrigin.Extend }
        val before = extendParents().single()
        assertSame(targetRef, (before.origin as CfirInstantiatedSupertypeOrigin.Extend).sourceTypeRef)
        assertSame(extension, (before.origin as CfirInstantiatedSupertypeOrigin.Extend).sourceExtend)
        target.isInReferenceCycle = true
        assertEquals(false, targetGraph.getPublishedReferenceCycleState(target.symbol))
        assertSame(targetRef, (extendParents().single().origin as CfirInstantiatedSupertypeOrigin.Extend).sourceTypeRef)
        publishTestProducts(targetGraph, targetSession.symbolProvider, listOf(target))
        targetGraph.publishCompletedProducts()
        assertTrue(extendParents().isEmpty())
        assertEquals(ownerRevision, ownerGraph.modificationCount)
        assertEquals(extendRevision, session.extendProvider.modificationCount)
        assertTrue(provider.inputStamp.dependencies.any { it.sources.directSupertypes === targetGraph })
        target.isInReferenceCycle = false
        assertTrue(extendParents().isEmpty())
        publishTestProducts(targetGraph, targetSession.symbolProvider, listOf(target))
        targetGraph.publishCompletedProducts()
        assertSame(targetRef, (extendParents().single().origin as CfirInstantiatedSupertypeOrigin.Extend).sourceTypeRef)
        assertSame(objectRef, host.superTypeRefs.single())
        assertEquals(ownerRevision, ownerGraph.modificationCount)
        assertEquals(extendRevision, session.extendProvider.modificationCount)
    }

    /** R7：选定 Source alias 的真实 RHS 和外部 owner identity，均不得回读 canonical alias。 */
    @Test
    fun `selected alias RHS overrides canonical abbreviation and observes external owner replacement`() {
        val (session, module) = ExtendTestFixtures.newSessionAndModule("selected-alias-reader")
        val (aliasSession, aliasModule) = ExtendTestFixtures.newSessionAndModule("selected-alias-owner")
        val core = CoreModel(module)
        val packageName = FqName("sample.aliasowneridentity")
        val aliasId = ClassId(packageName, Name.identifier("Alias"))
        val iA = newInterface(module, ClassId(packageName, Name.identifier("IA")))
        val iB = newInterface(module, ClassId(packageName, Name.identifier("IB")))
        val iC = newInterface(module, ClassId(packageName, Name.identifier("IC")))
        val aRef = iA.symbol.constructType().toCfirResolvedTypeRef(null)
        val bRef = iB.symbol.constructType().toCfirResolvedTypeRef(null)
        val cRef = iC.symbol.constructType().toCfirResolvedTypeRef(null)
        val a = newClass(module, ClassId(packageName, Name.identifier("A")), superTypeRefs = listOf(core.objectRef(), aRef))
        val b = newClass(module, ClassId(packageName, Name.identifier("B")), superTypeRefs = listOf(core.objectRef(), bRef))
        val c = newClass(module, ClassId(packageName, Name.identifier("C")), superTypeRefs = listOf(core.objectRef(), cRef))
        val canonicalRhs = b.symbol.constructType().toCfirResolvedTypeRef(null)
        val selectedRhs = a.symbol.constructType().toCfirResolvedTypeRef(null)
        val replacementRhs = c.symbol.constructType().toCfirResolvedTypeRef(null)
        val canonicalAlias = newSourceAlias(module, aliasId, canonicalRhs)
        val selectedAlias = newSourceAlias(aliasModule, aliasId, selectedRhs)
        val common = core.declarations + listOf(a, b, c, iA, iB, iC)
        session.registerTestTypeContext(common + canonicalAlias, emptyList())
        aliasSession.registerTestTypeContext(common + selectedAlias, emptyList())
        val readerGraph = checkNotNull(session.superTypeGraphStoreOrNull)
        val readerRevision = readerGraph.modificationCount
        val oldAliasGraph = checkNotNull(aliasSession.superTypeGraphStoreOrNull)
        val selectedSymbols = TestSymbolProvider(session, common + selectedAlias)
        val canonical = checkNotNull(session.typeAwareSupertypeProviderOrNull)
        val provider = canonical.forSources(CfirTypeAwareSupertypeSources(session, selectedSymbols, session.extendProvider, readerGraph))
        val aliasType = ConeTypeAliasType(aliasId, expandedType = canonicalRhs.coneType)
        assertEquals(listOf(objectType(), bRef.coneType), canonical.getDirectSupertypes(aliasType))
        val before = provider.getDirectSupertypeDescriptors(aliasType)
        assertEquals(listOf(objectType(), aRef.coneType), before.map { it.type })
        assertSame(aRef, (before[1].origin as CfirInstantiatedSupertypeOrigin.Declared).sourceTypeRef)
        assertTrue(provider.inputStamp.dependencies.any { it.sources.directSupertypes === oldAliasGraph })
        val replacement = CfirSuperTypeGraphStore()
        publishTestProducts(replacement, aliasSession.symbolProvider, common)
        replacement.recordDeclared(selectedAlias, listOf(replacementRhs), emptyList())
        replacement.publishCompletedProducts()
        assertEquals(oldAliasGraph.modificationCount, replacement.modificationCount)
        aliasSession.register(CfirDirectSupertypeProvider::class, replacement)
        aliasSession.register(CfirSuperTypeGraphStore::class, replacement)
        val after = provider.getDirectSupertypeDescriptors(aliasType)
        assertEquals(listOf(objectType(), cRef.coneType), after.map { it.type })
        assertSame(cRef, (after[1].origin as CfirInstantiatedSupertypeOrigin.Declared).sourceTypeRef)
        assertSame(selectedRhs, selectedAlias.expandedTypeRef)
        assertSame(canonicalRhs, canonicalAlias.expandedTypeRef)
        assertEquals(listOf(objectType(), bRef.coneType), canonical.getDirectSupertypes(aliasType))
        assertEquals(readerRevision, readerGraph.modificationCount)
        assertTrue(provider.inputStamp.dependencies.any { it.sources.directSupertypes === replacement })
        assertFalse(provider.inputStamp.dependencies.any { it.sources.directSupertypes === oldAliasGraph })
    }

    /** 所选 alias 的 RHS 同时决定结构接口边和直接 extend 成员，默认同名 alias 不得介入。 */
    @Test
    fun `selected alias source drives both typed parents and own extend members`() {
        val (session, module) = ExtendTestFixtures.newSessionAndModule("selected-extend-source")
        val core = CoreModel(module)
        val aId = ClassId(FqName("sample.sources"), Name.identifier("A"))
        val bId = ClassId(FqName("sample.sources"), Name.identifier("B"))
        val iId = ClassId(FqName("sample.sources"), Name.identifier("I"))
        val aliasId = ClassId(FqName("sample.sources"), Name.identifier("Alias"))
        val a = newClass(module, aId, superTypeRefs = listOf(core.objectRef()))
        val b = newClass(module, bId, superTypeRefs = listOf(core.objectRef()))
        val i = newInterface(module, iId)
        val provider = CfirCangJieScopeProvider()
        fun alias(target: ClassId) = buildTypeAlias {
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Library
            attributes = CfirDeclarationAttributes()
            status = CfirDeclarationStatusImpl().resolved(Visibilities.Public, Modality.FINAL)
            scopeProvider = provider
            symbol = CfirTypeAliasSymbol(aliasId)
            name = aliasId.shortClassName
            expandedTypeRef = ExtendTestFixtures.classTypeRef(target)
        }
        val canonicalAlias = alias(bId)
        val selectedAlias = alias(aId)
        session.registerTestTypeContext(core.declarations + listOf(a, b, i, canonicalAlias), emptyList())
        session.register(CfirCangJieScopeProvider::class, provider)
        session.register(CfirProvider::class, CfirLibrarySessionProvider(session.symbolProvider))
        val selectedSymbols = TestSymbolProvider(session, core.declarations + listOf(a, b, i, selectedAlias))
        val function = buildNamedFunction {
            moduleData = module
            resolvePhase = CfirResolvePhase.BODY_RESOLVE
            origin = CfirDeclarationOrigin.Library
            attributes = CfirDeclarationAttributes()
            status = CfirDeclarationStatusImpl().resolved(Visibilities.Public, Modality.FINAL)
            isLocal = false
            isMut = false
            name = Name.identifier("selectedMember")
            symbol = CfirNamedFunctionSymbol(CallableId(FqName("sample.sources"), name))
            returnTypeRef = buildResolvedTypeRef { coneType = ConePrimitiveType.INT64 }
        }
        val extend = ExtendTestFixtures.newExtend(
            module, buildResolvedTypeRef { coneType = ConeTypeAliasType(aliasId, expandedType = classType(bId)) },
            listOf(ExtendTestFixtures.classTypeRef(iId, isInterface = true)), declarations = listOf(function),
        )
        val selectedExtends = TestExtendProvider(listOf(extend), mapOf(extend to aId))
        val graph = checkNotNull(session.directSupertypeProviderOrNull)
        val sources = CfirTypeAwareSupertypeSources(session, selectedSymbols, selectedExtends, graph)
        val typed = checkNotNull(session.typeAwareSupertypeProviderOrNull).forSources(sources)
        assertTrue(typed.getDirectSupertypeDescriptors(classType(aId)).any { it.type.classId == iId })
        val storage = ScopeSession()
        fun scope() = provider.getUseSiteMemberScope(
            a.symbol, session, storage, symbolProvider = selectedSymbols, extendProvider = selectedExtends,
            directSupertypeProvider = graph, scopeKind = CfirClassMemberScopeKind.USE_SITE,
            memberRequiredPhase = null,
        )
        val actual = scope()
        val functions = mutableListOf<CfirNamedFunctionSymbol>()
        actual.processFunctionsByName(function.name, functions::add)
        assertEquals(listOf(function.symbol), functions)
        assertTrue(actual.getCallableNames().contains(function.name))
        assertSame(actual, scope())
        assertTrue(provider.getUseSiteMemberScope(
            a.symbol, session, storage, scopeKind = CfirClassMemberScopeKind.USE_SITE,
            memberRequiredPhase = null,
        ).getCallableNames().none { it == function.name })
        val missingAlias = ConeTypeAliasType(
            ClassId(FqName("sample.sources"), Name.identifier("MissingAlias")), expandedType = classType(aId),
        )
        assertNull(org.cangnova.cangjie.cfir.scopes.impl.CfirClassUseSiteMemberScope.createForUseSiteType(
            session, missingAlias, storage, memberRequiredPhase = null,
        ))
    }
    /** compiler-added ref 和显式 stdlib ref 都必须保留声明来源与引用身份。 */
    @Test
    fun `completed Object and Any refs retain declared provenance without duplicates`() {
        val (session, module) = ExtendTestFixtures.newSessionAndModule()
        val core = CoreModel(module)
        val objectRef = core.objectRef()
        val hostId = ClassId(FqName("sample.rootrefs"), Name.identifier("Host"))
        val host = newClass(module, hostId, superTypeRefs = listOf(objectRef))
        val explicitHostId = ClassId(FqName("sample.rootrefs"), Name.identifier("ExplicitHost"))
        val explicitObjectRef = ExtendTestFixtures.classTypeRef(StdlibClassIds.Object).also {
            it.replaceTypeUseOrigin(CfirTypeRefOrigin.SOURCE)
        }
        val explicitHost = newClass(module, explicitHostId, superTypeRefs = listOf(explicitObjectRef))
        session.registerTestTypeContext(core.declarations + listOf(host, explicitHost), emptyList())
        val provider = checkNotNull(session.typeAwareSupertypeProviderOrNull)
        val hostDescriptors = provider.getDirectSupertypeDescriptors(classType(hostId))
        assertEquals(listOf(objectType()), hostDescriptors.map { it.type })
        val origin = assertInstanceOf(CfirInstantiatedSupertypeOrigin.Declared::class.java, hostDescriptors.single().origin)
        assertSame(objectRef, origin.sourceTypeRef)
        assertEquals(CfirTypeRefOrigin.COMPILER_ADDED, origin.sourceTypeRef.typeUseOrigin)
        val explicitDescriptor = provider.getDirectSupertypeDescriptors(classType(explicitHostId)).single()
        assertEquals(objectType(), explicitDescriptor.type)
        assertSame(explicitObjectRef, (explicitDescriptor.origin as CfirInstantiatedSupertypeOrigin.Declared).sourceTypeRef)
        assertEquals(CfirTypeRefOrigin.SOURCE, explicitObjectRef.typeUseOrigin)
        val anyDescriptors = provider.getDirectSupertypeDescriptors(objectType())
        assertEquals(listOf(interfaceType(StdlibClassIds.Any)), anyDescriptors.map { it.type })
        assertSame(core.anyRef, (anyDescriptors.single().origin as CfirInstantiatedSupertypeOrigin.Declared).sourceTypeRef)
        assertEquals(CfirTypeRefOrigin.SOURCE, core.anyRef.typeUseOrigin)
        assertTrue(provider.getDirectSupertypeDescriptors(interfaceType(StdlibClassIds.Any)).isEmpty())
    }

    /** Object 上的 extend 经真实 compiler-added 父 ref 传播，不能丢掉路径。 */
    @Test
    fun `Object extend propagation retains compiler added superclass reference`() {
        val (session, module) = ExtendTestFixtures.newSessionAndModule()
        val core = CoreModel(module)
        val hostId = ClassId(FqName("sample.rootextend"), Name.identifier("Host"))
        val markerId = ClassId(FqName("sample.rootextend"), Name.identifier("Marker"))
        val objectRef = core.objectRef()
        val host = newClass(module, hostId, superTypeRefs = listOf(objectRef))
        val marker = newInterface(module, markerId)
        val objectExtend = ExtendTestFixtures.newExtend(
            module, ExtendTestFixtures.classTypeRef(StdlibClassIds.Object),
            listOf(ExtendTestFixtures.classTypeRef(markerId, isInterface = true)),
        )
        session.registerTestTypeContext(core.declarations + listOf(host, marker), listOf(objectExtend))
        val provider = checkNotNull(session.typeAwareSupertypeProviderOrNull)
        val descriptor = provider.getDirectSupertypeDescriptors(classType(hostId))
            .single { it.origin is CfirInstantiatedSupertypeOrigin.Extend }
        val origin = descriptor.origin as CfirInstantiatedSupertypeOrigin.Extend
        assertEquals(interfaceType(markerId), descriptor.type)
        assertSame(objectExtend, origin.sourceExtend)
        assertSame(objectExtend.superTypeRefs.single(), origin.sourceTypeRef)
        assertEquals(1, origin.propagationPath.size)
        assertSame(objectRef, origin.propagationPath.single())
    }

    /** 显式绑定图是唯一父输入源；缺产品不得改读 canonical 声明或补根。 */
    @Test
    fun `selected graph reads only its completed product and rejects unpublished owner`() {
        val (session, module) = ExtendTestFixtures.newSessionAndModule()
        val core = CoreModel(module)
        val packageName = FqName("sample.selectedgraph")
        val canonicalId = ClassId(packageName, Name.identifier("Canonical"))
        val selectedId = ClassId(packageName, Name.identifier("Selected"))
        val hostId = ClassId(packageName, Name.identifier("Host"))
        val canonical = newInterface(module, canonicalId)
        val selected = newInterface(module, selectedId)
        val objectRef = core.objectRef()
        val canonicalRef = ExtendTestFixtures.classTypeRef(canonicalId, isInterface = true)
        val selectedRef = ExtendTestFixtures.classTypeRef(selectedId, isInterface = true)
        val host = newClass(module, hostId, superTypeRefs = listOf(objectRef, canonicalRef))
        val declarations = core.declarations + listOf(host, canonical, selected)
        session.registerTestTypeContext(declarations, emptyList())
        val canonicalProvider = checkNotNull(session.typeAwareSupertypeProviderOrNull)
        val selectedGraph = CfirSuperTypeGraphStore()
        publishTestProducts(selectedGraph, session.symbolProvider, core.declarations + listOf(canonical, selected))
        selectedGraph.publishCompletedProducts()
        val provider = canonicalProvider.forSources(
            CfirTypeAwareSupertypeSources(session, session.symbolProvider, session.extendProvider, selectedGraph),
        )
        val before = selectedGraph.modificationCount
        val stamp = provider.inputStamp
        assertEquals(before, stamp.revision.graphRevision)
        assertEquals(before, selectedGraph.modificationCount)
        assertNull(selectedGraph.getCompletedSupertypeProduct(host.symbol))
        assertThrows(IllegalStateException::class.java) {
            provider.getDirectSupertypeDescriptors(classType(hostId))
        }
        selectedGraph.recordDeclared(host, listOf(objectRef, selectedRef), listOf(
            CfirSuperTypeGraphEdge(objectRef, "Object", core.objectDeclaration.symbol),
            CfirSuperTypeGraphEdge(selectedRef, "Selected", selected.symbol),
        ))
        selectedGraph.publishCompletedProducts()
        val descriptors = provider.getDirectSupertypeDescriptors(classType(hostId))
        assertEquals(listOf(objectType(), interfaceType(selectedId)), descriptors.map { it.type })
        assertFalse(descriptors.any { it.type.classId == canonicalId })
        assertSame(selectedRef, (descriptors[1].origin as CfirInstantiatedSupertypeOrigin.Declared).sourceTypeRef)
        assertEquals(listOf(objectType(), interfaceType(canonicalId)), canonicalProvider.getDirectSupertypes(classType(hostId)))
        assertSame(canonicalRef, host.superTypeRefs[1])
        assertTrue(provider.getDirectSupertypeDescriptors(interfaceType(selectedId)).isEmpty())
    }

    /** Library 的正式父引用属于声明自身；源码图缺节点不能让消费者制造 stdlib 父边。 */
    @Test
    fun `library superclass chain does not require nodes in source graph`() {
        val (session, module) = ExtendTestFixtures.newSessionAndModule()
        val core = CoreModel(module)
        val hostId = ClassId(FqName("sample.libraryroots"), Name.identifier("Host"))
        val objectRef = core.objectRef()
        val host = newClass(module, hostId, superTypeRefs = listOf(objectRef))
        session.registerTestTypeContext(core.declarations + host, emptyList())
        val sourceGraph = CfirSuperTypeGraphStore()
        session.register(CfirDirectSupertypeProvider::class, sourceGraph)
        session.register(CfirSuperTypeGraphStore::class, sourceGraph)
        assertNull(sourceGraph.getCompletedSupertypeProduct(core.objectDeclaration.symbol))
        val provider = checkNotNull(session.typeAwareSupertypeProviderOrNull)
        assertEquals(listOf(objectType()), provider.getDirectSupertypes(classType(hostId)))
        val descriptors = provider.getDirectSupertypeDescriptors(objectType())
        assertEquals(listOf(interfaceType(StdlibClassIds.Any)), descriptors.map { it.type })
        assertSame(core.anyRef, (descriptors.single().origin as CfirInstantiatedSupertypeOrigin.Declared).sourceTypeRef)
        assertEquals(0L, sourceGraph.modificationCount)
    }

    /** 错误引用保留在完整产品中；空成员投影不得被当作缺省父类请求。 */
    @Test
    fun `completed error superclass does not produce phantom Object`() {
        val (session, module) = ExtendTestFixtures.newSessionAndModule()
        val core = CoreModel(module)
        val hostId = ClassId(FqName("sample.errorroot"), Name.identifier("Host"))
        val error = ConeErrorType(org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic("invalid superclass"))
            .toCfirResolvedTypeRef(null)
        val host = newClass(module, hostId, superTypeRefs = listOf(error))
        session.registerTestTypeContext(core.declarations + host, emptyList())
        val graph = checkNotNull(session.directSupertypeProviderOrNull)
        val product = checkNotNull(graph.getCompletedSupertypeProduct(host.symbol))
        assertSame(error, product.superTypeRefs.single())
        assertTrue(product.declaredSuperTypes.isEmpty())
        assertTrue(checkNotNull(session.typeAwareSupertypeProviderOrNull).getDirectSupertypeDescriptors(classType(hostId)).isEmpty())
    }

}

/**
 * 为测试 session 注册 symbol、extend 和 type-aware supertype provider。
 */
private fun CfirSession.registerTestTypeContext(
    declarations: List<CfirClassLikeDeclaration>,
    extends: List<CfirExtend>,
): ConeInferenceContext {
    val symbols = TestSymbolProvider(this, declarations)
    register(CfirSymbolProvider::class, symbols)
    register(CfirExtendProvider::class, TestExtendProvider(extends))
    val graph = CfirSuperTypeGraphStore()
    register(CfirSuperTypeGraphStore::class, graph)
    register(CfirDirectSupertypeProvider::class, graph)
    publishTestProducts(graph, symbols, declarations)
    graph.publishCompletedProducts()
    register(CfirTypeAwareSupertypeProvider::class, CfirTypeAwareSupertypeProviderImpl(this))
    register(CfirLazyDeclarationResolver::class, CfirDummyCompilerLazyDeclarationResolver)
    register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
    register(TypeComponents::class, TypeComponents(this))
    return object : ConeInferenceContext {
        override val session: CfirSession
            get() = this@registerTestTypeContext
    }
}

/** 完整声明输入和普通成员投影分别记录；未解析或缺 target 是 fixture 装配错误。 */
private fun publishTestProducts(
    graph: CfirSuperTypeGraphStore,
    symbols: CfirSymbolProvider,
    declarations: List<CfirClassLikeDeclaration>,
) {
    for (declaration in declarations) {
        val refs = if (declaration is CfirTypeAlias) listOf(declaration.expandedTypeRef) else declaration.superTypeRefs
        val edges = if (declaration is CfirTypeAlias) emptyList() else refs.mapNotNull { ref ->
            check(ref is CfirResolvedTypeRef) { "Test products require completed type references" }
            if (ref.coneType is ConeErrorType) return@mapNotNull null
            val type = ref.coneType
            val target = if (type is ConeClassLikeType) {
                when (val tag = type.lookupTag) {
                    is ConeClassLikeLookupTagWithFixedSymbol -> tag.symbol
                    else -> checkNotNull(symbols.getClassLikeSymbolByClassId(tag.classId)) { "Missing fixture declaration: ${tag.classId}" }
                }
            } else {
                val targetId = checkNotNull(type.classIdOrPrimitiveClassId)
                checkNotNull(symbols.getClassLikeSymbolByClassId(targetId)) { "Missing fixture declaration: $targetId" }
            }
            CfirSuperTypeGraphEdge(ref, ref.toString(), target)
        }
        graph.recordDeclared(declaration, refs, edges)
    }
}

/** Source 声明测试只构造已完成头与正式来源，不制造 body 产品。 */
private fun newSourceInterface(
    moduleData: CfirModuleData,
    classId: ClassId,
    superTypeRefs: List<CfirTypeRef>,
    symbol: CfirInterfaceSymbol = CfirInterfaceSymbol(classId),
): CfirInterfaceImpl = CfirInterfaceImpl(
    source = object : CjBinarySourceElement(classId.shortClassName.asString(), null, "source:$classId") {},
    moduleData = moduleData,
    resolvePhase = CfirResolvePhase.SUPER_TYPES,
    annotations = MutableOrEmptyList.empty(),
    origin = CfirDeclarationOrigin.Source,
    attributes = CfirDeclarationAttributes(),
    deprecationsProvider = EmptyDeprecationsProvider,
    scopeProvider = CfirCangJieScopeProvider(),
    declarations = mutableListOf(),
    status = CfirDeclarationStatusImpl(),
    typeParameters = mutableListOf(),
    symbol = symbol,
    superTypeRefs = superTypeRefs.toMutableList(),
    name = classId.shortClassName,
)

private fun newSourceAlias(moduleData: CfirModuleData, classId: ClassId, rhs: CfirResolvedTypeRef): CfirTypeAlias = buildTypeAlias {
    source = object : CjBinarySourceElement(classId.shortClassName.asString(), null, "source:$classId") {}
    this.moduleData = moduleData
    resolvePhase = CfirResolvePhase.SUPER_TYPES
    origin = CfirDeclarationOrigin.Source
    attributes = CfirDeclarationAttributes()
    status = CfirDeclarationStatusImpl()
    scopeProvider = CfirCangJieScopeProvider()
    symbol = CfirTypeAliasSymbol(classId)
    name = classId.shortClassName
    expandedTypeRef = rhs
}

/** 显式装配已完成的 Library 声明模型；查询器不负责补齐缺省根。 */
private class CoreModel(module: CfirModuleData) {
    val any = newInterface(module, StdlibClassIds.Any)
    val anyRef = buildResolvedTypeRef {
        coneType = interfaceType(StdlibClassIds.Any)
        typeUseOrigin = CfirTypeRefOrigin.SOURCE
    }
    val objectDeclaration = newClass(module, StdlibClassIds.Object, superTypeRefs = listOf(anyRef))
    val declarations: List<CfirClassLikeDeclaration> = listOf(any, objectDeclaration)
    fun objectRef(): CfirResolvedTypeRef = buildResolvedTypeRef {
        coneType = objectDeclaration.symbol.constructType()
        typeUseOrigin = CfirTypeRefOrigin.COMPILER_ADDED
    }
}

/**
 * 基于内存声明表的测试 symbol provider。
 */
private class TestSymbolProvider(
    session: CfirSession,
    declarations: List<CfirClassLikeDeclaration>,
) : CfirSymbolProvider(session) {
    /**
     * 按 ClassId 索引的测试声明表。
     */
    private val declarationsByClassId = declarations.associateBy { it.symbol.classId }

    /**
     * 名称索引查询一律保守返回可能命中，让真实 symbol lookup 决定结果。
     */
    override val symbolNamesProvider: CfirSymbolNamesProvider = CfirNullSymbolNamesProvider

    /**
     * 按 ClassId 返回测试 class-like symbol。
     */
    override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? =
        declarationsByClassId[classId]?.symbol

    /**
     * 测试 provider 不暴露 callable symbol。
     */
    override fun getTopLevelCallableSymbolsTo(
        destination: MutableList<CfirCallableSymbol<*>>,
        packageFqName: FqName,
        name: Name,
    ) {
    }

    /**
     * 测试 provider 不暴露函数 symbol。
     */
    override fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<CfirNamedFunctionSymbol>,
        packageFqName: FqName,
        name: Name,
    ) {
    }

    /**
     * 测试 provider 不暴露属性 symbol。
     */
    override fun getTopLevelPropertySymbolsTo(
        destination: MutableList<CfirPropertySymbol>,
        packageFqName: FqName,
        name: Name,
    ) {
    }

    /**
     * 当内存声明表中存在对应包声明时认为包存在。
     */
    override fun hasPackage(fqName: FqName): Boolean =
        declarationsByClassId.keys.any { it.packageFqName == fqName }
}

/**
 * 基于内存 extend 列表的测试 extend provider。
 */
private class TestExtendProvider(
    private val extends: List<CfirExtend>,
    private val normalizedTargets: Map<CfirExtend, ClassId> = emptyMap(),
) : CfirExtendProvider {
    override val modificationCount: Long = 0L

    override fun getAllExtends(): List<CfirExtend> = extends

    /**
     * 按扩展目标 ClassId 分组的 extend 列表。
     */
    private val extendsByClassId: Map<ClassId, List<CfirExtend>> = extends.groupBy { extend ->
        val extendedType = extend.extendedTypeRef as? org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
            ?: error("extend target must already be resolved in test fixtures")
        normalizedTargets[extend] ?: extendedType.coneType.classIdOrPrimitiveClassId
            ?: error("extend target must be classifier type in test fixtures")
    }

    /**
     * 返回指定 class 的 extend 列表。
     */
    override fun getExtendsForClass(classId: ClassId): List<CfirExtend> =
        extendsByClassId[classId].orEmpty()

    /**
     * 按规范化目标 key 查询 extend：测试 fixture 只支持 ClassLike 目标。
     */
    override fun getExtendsForTarget(targetKey: CfirExtendTargetKey): List<CfirExtend> =
        (targetKey as? CfirExtendTargetKey.ClassLike)?.let { getExtendsForClass(it.classId) } ?: emptyList()

    /**
     * 返回指定包内所有 extend。
     */
    override fun getExtendsInPackage(packageFqName: FqName): List<CfirExtend> =
        extendsByClassId
            .filterKeys { it.packageFqName == packageFqName }
            .values
            .flatten()

    /**
     * 返回指定 primitive builtin 类型的 extend。
     */
    override fun getExtendsForBuiltinType(kind: org.cangnova.cangjie.cfir.types.PrimitiveTypeKind): List<CfirExtend> =
        extendsByClassId[kind.classId].orEmpty()

    /** 测试声明统一属于稳定的虚拟包，确保父边 provenance 完整。 */
    override fun getPackageFqName(extend: CfirExtend): FqName = FqName("test.extend")
}

/**
 * 构造测试 class 声明。
 */
private fun newClass(
    moduleData: CfirModuleData,
    classId: ClassId,
    typeParameters: List<CfirTypeParameter> = emptyList(),
    superTypeRefs: List<CfirTypeRef>,
): CfirClassImpl {
    val symbol = CfirClassSymbol(classId)
    return CfirClassImpl(
        source = null,
        moduleData = moduleData,
        resolvePhase = CfirResolvePhase.BODY_RESOLVE,
        annotations = MutableOrEmptyList.empty(),
        origin = CfirDeclarationOrigin.Library,
        attributes = CfirDeclarationAttributes.EMPTY,
        deprecationsProvider = EmptyDeprecationsProvider,
        scopeProvider = CfirCangJieScopeProvider(),
        status = CfirDeclarationStatusImpl(),
        typeParameters = typeParameters.toMutableList(),
        symbol = symbol,
        superTypeRefs = superTypeRefs.toMutableList(),
        declarations = mutableListOf(),
        name = classId.shortClassName,
    ).also { it.initDefaultResolveState() }
}

/**
 * 构造测试 interface 声明。
 */
private fun newInterface(
    moduleData: CfirModuleData,
    classId: ClassId,
    typeParameters: List<CfirTypeParameter> = emptyList(),
    superTypeRefs: List<CfirTypeRef> = emptyList(),
): CfirInterfaceImpl {
    val symbol = CfirInterfaceSymbol(classId)
    return CfirInterfaceImpl(
        source = null,
        moduleData = moduleData,
        resolvePhase = CfirResolvePhase.BODY_RESOLVE,
        annotations = MutableOrEmptyList.empty(),
        origin = CfirDeclarationOrigin.Library,
        attributes = CfirDeclarationAttributes.EMPTY,
        deprecationsProvider = EmptyDeprecationsProvider,
        scopeProvider = CfirCangJieScopeProvider(),
        declarations = mutableListOf<CfirDeclaration>(),
        status = CfirDeclarationStatusImpl(),
        typeParameters = typeParameters.toMutableList(),
        symbol = symbol,
        superTypeRefs = superTypeRefs.toMutableList(),
        name = classId.shortClassName,
    ).also { it.initDefaultResolveState() }
}

/**
 * 将测试类型参数声明转换为 cone 类型参数类型。
 */
private fun typeParameterType(typeParameter: CfirTypeParameter): ConeCangJieType =
    ConeTypeParameterTypeImpl(typeParameter.symbol.toLookupTag())

/**
 * 构造 class-like 类型。
 */
private fun classType(classId: ClassId, vararg typeArguments: ConeCangJieType): ConeClassLikeType =
    ConeClassLikeType(
        lookupTag = classId.toLookupTag(),
        typeArguments = typeArguments.toList(),
    )

/**
 * 构造正式 Object 声明父引用的期望类型。
 */
private fun objectType(): ConeClassLikeType =
    ConeClassLikeType(
        lookupTag = org.cangnova.cangjie.cfir.types.StdlibClassIds.Object.toLookupTag(),
    )

/**
 * 构造 interface class-like 类型。
 */
private fun interfaceType(classId: ClassId, vararg typeArguments: ConeCangJieType): ConeClassLikeType =
    ConeClassLikeType(
        lookupTag = classId.toLookupTag(),
        typeArguments = typeArguments.toList(),
        isInterface = true,
    )

/**
 * 构造 primitive resolved type ref。
 */
private fun primitiveTypeRef(type: ConePrimitiveType): CfirTypeRef =
    org.cangnova.cangjie.cfir.types.impl.CfirResolvedTypeRefImpl(
        source = null,
        annotations = MutableOrEmptyList.empty(),
        customRenderer = false,
        typeUseOrigin = org.cangnova.cangjie.cfir.types.CfirTypeRefOrigin.SEMANTIC,
        coneType = type,
        delegatedTypeRef = null,
    )
