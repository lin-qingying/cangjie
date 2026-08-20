@file:OptIn(
    org.cangnova.cangjie.cfir.CfirImplementationDetail::class,
    org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals::class,
)

package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.MutableOrEmptyList
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.CfirTypeParameter
import org.cangnova.cangjie.cfir.declarations.EmptyDeprecationsProvider
import org.cangnova.cangjie.cfir.declarations.impl.CfirClassImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirInterfaceImpl
import org.cangnova.cangjie.cfir.declarations.initDefaultResolveState
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.cfir.resolve.services.CfirTypeAwareSupertypeProviderImpl
import org.cangnova.cangjie.cfir.scopes.CfirCangJieScopeProvider
import org.cangnova.cangjie.LanguageVersionSettingsImpl
import org.cangnova.cangjie.cfir.session.CfirLanguageSettingsComponent
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.cfir.session.typeAwareSupertypeProviderOrNull
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassSymbol
import org.cangnova.cangjie.cfir.symbols.CfirDummyCompilerLazyDeclarationResolver
import org.cangnova.cangjie.cfir.symbols.CfirInterfaceSymbol
import org.cangnova.cangjie.cfir.symbols.CfirLazyDeclarationResolver
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.ConeTypeParameterTypeImpl
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.cfir.types.ConeInferenceContext
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.TypeComponents
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.type.AbstractTypeChecker
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [CfirTypeAwareSupertypeProvider] 与类型检查器集成的超类型查询测试。
 */
class CfirTypeAwareSupertypeProviderTest {
    /**
     * 验证泛型 extend 会按接收者具体类型实参实例化并参与子类型遍历。
     */
    @Test
    fun `generic extend is instantiated for concrete type and subtype traversal`() {
        val packageFqName = FqName("sample.generic")
        val boxClassId = ClassId(packageFqName, Name.identifier("Box"))
        val iterableClassId = ClassId(packageFqName, Name.identifier("Iterable"))

        val (session, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val typeParameter = ExtendTestFixtures.newTypeParameter(moduleData, "T")
        val boxClass = newClass(
            moduleData = moduleData,
            classId = boxClassId,
            typeParameters = listOf(typeParameter),
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
            declarations = listOf(boxClass, iterableInterface),
            extends = listOf(boxExtend),
        )

        val boxInt = classType(boxClassId, ConePrimitiveType.INT32)
        val iterableInt = interfaceType(iterableClassId, ConePrimitiveType.INT32)

        assertIterableEquals(
            listOf(objectType(), iterableInt),
            session.typeAwareSupertypeProviderOrNull?.getDirectSupertypes(boxInt),
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
        val typeParameter = ExtendTestFixtures.newTypeParameter(moduleData, "T")
        val baseClass = newClass(
            moduleData = moduleData,
            classId = baseClassId,
            typeParameters = listOf(typeParameter),
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
            declarations = listOf(baseClass, childClass, markerInterface),
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
        val targetClass = newClass(moduleData, targetClassId)
        val parentInterface = newInterface(moduleData, parentInterfaceId)
        val extend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(parentInterfaceId, isInterface = true)),
        )

        val typeContext = session.registerTestTypeContext(
            declarations = listOf(targetClass, parentInterface),
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
            declarations = listOf(eqInterface, aClass, bInterface),
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
}

/**
 * 为测试 session 注册 symbol、extend 和 type-aware supertype provider。
 */
private fun CfirSession.registerTestTypeContext(
    declarations: List<CfirClassLikeDeclaration>,
    extends: List<CfirExtend>,
): ConeInferenceContext {
    register(CfirSymbolProvider::class, TestSymbolProvider(this, declarations))
    register(CfirExtendProvider::class, TestExtendProvider(extends))
    register(CfirTypeAwareSupertypeProvider::class, CfirTypeAwareSupertypeProviderImpl(this))
    register(CfirLazyDeclarationResolver::class, CfirDummyCompilerLazyDeclarationResolver)
    register(CfirLanguageSettingsComponent::class, CfirLanguageSettingsComponent(LanguageVersionSettingsImpl.DEFAULT))
    register(TypeComponents::class, TypeComponents(this))
    return object : ConeInferenceContext {
        override val session: CfirSession
            get() = this@registerTestTypeContext
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
    extends: List<CfirExtend>,
) : CfirExtendProvider {
    /**
     * 按扩展目标 ClassId 分组的 extend 列表。
     */
    private val extendsByClassId: Map<ClassId, List<CfirExtend>> = extends.groupBy { extend ->
        val extendedType = extend.extendedTypeRef as? org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
            ?: error("extend target must already be resolved in test fixtures")
        extendedType.coneType.classIdOrPrimitiveClassId
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
    superTypeRefs: List<CfirTypeRef> = emptyList(),
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
 * 构造隐式补入的 std.core.Object 类型。
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
        coneType = type,
        delegatedTypeRef = null,
    )
