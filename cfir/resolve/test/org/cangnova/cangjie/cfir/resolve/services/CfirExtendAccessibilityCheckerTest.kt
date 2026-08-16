@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.builder.buildImport
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionResult
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolver
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures.TestSession
import org.cangnova.cangjie.cfir.resolve.SupertypeSupplier
import org.cangnova.cangjie.cfir.resolve.TypeResolutionConfiguration
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityFileScope
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompositeExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSessionExtendProvider
import org.cangnova.cangjie.cfir.scopes.CfirDefaultImportsProviderHolder
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.extendProvider
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.DefaultImportsProvider
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * [CfirExtendAccessibilityChecker] 的 deserialized（库）extend 回退分支测试。
 *
 * 库 extend 没有语义模型（`CfirExtendIndexStore` 只索引 source 文件），可见性判定
 * 必须经 [CfirExtendAccessibilityChecker.ExtendAccessView] 的声明派生路径；source
 * extend 仍走语义模型路径（见回归对照用例）。
 *
 * 组合链注册（own + 匿名库 stub）精确模拟生产环境：`CfirSessionExtendProvider`
 * 对库声明返回 `null` 包名后，组合 provider 继续查库 stub。
 */
class CfirExtendAccessibilityCheckerTest {

    /**
     * 反例：库 extend 的目标与接口在异包，use-site 未导入时不可见。
     *
     * 对齐官方 `IsExtendAccessible`：目标与 extend 异包时要求至少一个接口已导入，
     * 且目标类型可访问；两者都不可达时拒绝。
     */
    @Test
    fun `library extend with cross package target is not accessible without imports`() {
        val context = newContext(extendPackage = FqName("lib"))
        val extend = newLibraryExtend(
            context,
            targetClassId = ClassId(FqName("base"), Name.identifier("Target")),
            interfaceClassId = ClassId(FqName("base"), Name.identifier("I")),
        )
        val useSite = ExtendTestFixtures.newFile(context.moduleData, FqName("a"), emptyList())

        assertFalse(isExtendAccessible(context, useSite, extend))
    }

    /**
     * 正例：use-site 文件与 extend 同包时直接放行，不依赖导入。
     */
    @Test
    fun `extend is accessible from file in same package`() {
        val context = newContext(extendPackage = FqName("lib"))
        val extend = newLibraryExtend(
            context,
            targetClassId = ClassId(FqName("base"), Name.identifier("Target")),
            interfaceClassId = ClassId(FqName("base"), Name.identifier("I")),
        )
        val useSite = ExtendTestFixtures.newFile(context.moduleData, FqName("lib"), emptyList())

        assertTrue(isExtendAccessible(context, useSite, extend))
    }

    /**
     * 正例：目标与 extend 同包时只看目标可见性（接口导入不作要求），
     * 对齐官方同包 direct extend 分支。
     */
    @Test
    fun `extend with target in same package as declaration is accessible`() {
        val context = newContext(extendPackage = FqName("lib"))
        val extend = newLibraryExtend(
            context,
            targetClassId = ClassId(FqName("lib"), Name.identifier("Target")),
            interfaceClassId = ClassId(FqName("lib"), Name.identifier("I")),
        )
        val useSite = ExtendTestFixtures.newFile(context.moduleData, FqName("a"), emptyList())

        assertTrue(isExtendAccessible(context, useSite, extend))
    }

    /**
     * 正例：目标/接口在 std.core 时经语言默认导入可达（`std.core.*`）。
     */
    @Test
    fun `extend with std core target is accessible through default imports`() {
        val context = newContext(extendPackage = FqName("lib"))
        val extend = newLibraryExtend(
            context,
            targetClassId = ClassId(FqName("std.core"), Name.identifier("Target")),
            interfaceClassId = ClassId(FqName("std.core"), Name.identifier("I")),
        )
        val useSite = ExtendTestFixtures.newFile(context.moduleData, FqName("a"), emptyList())

        assertTrue(isExtendAccessible(context, useSite, extend))
    }

    /**
     * 正例：显式导入目标包后，异包目标的库 extend 可见。
     */
    @Test
    fun `library extend is accessible after explicit package import`() {
        val context = newContext(extendPackage = FqName("lib"))
        val base = FqName("base")
        val extend = newLibraryExtend(
            context,
            targetClassId = ClassId(base, Name.identifier("Target")),
            interfaceClassId = ClassId(base, Name.identifier("I")),
        )
        val useSite = ExtendTestFixtures.newFile(context.moduleData, FqName("a"), emptyList())
        context.bindingStore.record(
            useSite,
            listOf(
                org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportBinding(
                    importDirective = buildImport {
                        importedFqName = base
                        isAllUnder = true
                    },
                    effectiveName = Name.identifier("base"),
                    targets = listOf(
                        org.cangnova.cangjie.cfir.resolve.services.CfirResolvedImportTarget.Package(base),
                    ),
                ),
            ),
        )

        assertTrue(isExtendAccessible(context, useSite, extend))
    }

    /**
     * 正例：std.core 包内 primitive 目标的库 extend（如 `extend Int64 <: Hashable`）。
     *
     * 目标 classId 与声明同包（std.core），走同包 direct 分支，任意 use-site 可见。
     */
    @Test
    fun `std core primitive extend is accessible from any package`() {
        val context = newContext(extendPackage = FqName("std.core"))
        val extend = newLibraryExtend(
            context,
            targetClassId = ClassId(FqName("std.core"), Name.identifier("Int64")),
            interfaceClassId = ClassId(FqName("std.core"), Name.identifier("Hashable")),
        )
        val useSite = ExtendTestFixtures.newFile(context.moduleData, FqName("a"), emptyList())

        assertTrue(isExtendAccessible(context, useSite, extend))
    }

    /**
     * 回归对照：source extend（语义模型存在）走 model 路径，行为与 fallback 一致。
     */
    @Test
    fun `source extend keeps model based accessibility`() {
        val context = newContext(extendPackage = FqName("lib"))
        val extend = newLibraryExtend(
            context,
            targetClassId = ClassId(FqName("lib"), Name.identifier("Target")),
            interfaceClassId = ClassId(FqName("lib"), Name.identifier("I")),
        )
        val declarationFile = ExtendTestFixtures.newFile(context.moduleData, FqName("lib"), listOf(extend))
        context.store.rebuild(listOf(declarationFile), NoopTypeResolver)
        val useSite = ExtendTestFixtures.newFile(context.moduleData, FqName("a"), emptyList())

        assertTrue(isExtendAccessible(context, useSite, extend))
    }

    /**
     * 反例：泛型上界在异包且未导入时，即使接口/目标可达也不可见。
     *
     * 对齐官方 `IsExtendAllUpperBoundsImported`：上界全部导入是跨包可见性的前置条件。
     */
    @Test
    fun `library extend with inaccessible upper bound is not accessible`() {
        val context = newContext(extendPackage = FqName("lib"))
        val moduleData = context.moduleData
        val extend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(ClassId(FqName("lib"), Name.identifier("Box"))),
            superTypeRefs = listOf(
                ExtendTestFixtures.classTypeRef(ClassId(FqName("lib"), Name.identifier("I")), isInterface = true),
            ),
            typeParameters = listOf(
                ExtendTestFixtures.newTypeParameter(
                    moduleData = moduleData,
                    name = "T",
                    bounds = listOf(ExtendTestFixtures.classTypeRef(ClassId(FqName("base"), Name.identifier("Bound")))),
                ),
            ),
        )
        context.session.register(CfirExtendProvider::class, context.compositeWithLibraryExtend(extend, FqName("lib")))
        val useSite = ExtendTestFixtures.newFile(moduleData, FqName("a"), emptyList())

        assertFalse(isExtendAccessible(context, useSite, extend))
    }

    /**
     * 以 [CfirAccessibilityFileScope] 文件上下文调用组合 provider 的可见性判定，
     * 覆盖生产消费链（ThreadLocal 取文件 + 组合 provider 分发 + checker）。
     */
    private fun isExtendAccessible(context: TestContext, file: CfirFile, extend: CfirExtend): Boolean =
        CfirAccessibilityFileScope.with(file) {
            context.session.extendProvider.isExtendAccessible(extend)
        }

    /**
     * 构造库（deserialized 风格）extend：声明进组合视图的库 stub 包索引，不进 index store。
     */
    private fun newLibraryExtend(
        context: TestContext,
        targetClassId: ClassId,
        interfaceClassId: ClassId,
    ): CfirExtend {
        val extend = ExtendTestFixtures.newExtend(
            moduleData = context.moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = listOf(ExtendTestFixtures.classTypeRef(interfaceClassId, isInterface = true)),
        )
        context.session.register(
            CfirExtendProvider::class,
            context.compositeWithLibraryExtend(extend, context.extendPackage),
        )
        return extend
    }

    /**
     * 建立测试 session 与 extend provider 组合链。
     */
    private fun newContext(extendPackage: FqName): TestContext {
        val (session, moduleData) = ExtendTestFixtures.newSessionAndModule()
        session.register(
            CfirDefaultImportsProviderHolder::class,
            CfirDefaultImportsProviderHolder.of(
                object : DefaultImportsProvider() {
                    override val platformSpecificDefaultImports: List<org.cangnova.cangjie.ImportPath> = emptyList()
                },
            ),
        )
        val store = CfirExtendIndexStore()
        session.register(CfirExtendIndexStore::class, store)
        val bindingStore = CfirImportBindingStore()
        session.register(CfirImportBindingStore::class, bindingStore)
        session.register(CfirSymbolProvider::class, EmptySymbolProvider(session))
        return TestContext(session, moduleData, store, bindingStore, extendPackage)
    }

    /**
     * 测试共享的 session 组件与 fixture 句柄。
     */
    private class TestContext(
        val session: TestSession,
        val moduleData: CfirModuleData,
        val store: CfirExtendIndexStore,
        val bindingStore: CfirImportBindingStore,
        val extendPackage: FqName,
    ) {
        /**
         * 构造 own + 库 stub 的组合 provider 并注册（own 在前，对齐生产 combine 顺序）。
         */
        fun compositeWithLibraryExtend(libraryExtend: CfirExtend, libraryPackage: FqName): CfirExtendProvider {
            val own = CfirSessionExtendProvider(session, store)
            val stub = object : CfirExtendProvider {
                override fun getExtendsForTarget(targetKey: CfirExtendTargetKey): List<CfirExtend> = emptyList()
                override fun getExtendsForClass(classId: ClassId): List<CfirExtend> = emptyList()
                override fun getExtendsInPackage(packageFqName: FqName): List<CfirExtend> = emptyList()
                override fun getExtendsForBuiltinType(kind: PrimitiveTypeKind): List<CfirExtend> = emptyList()
                override fun getPackageFqName(extend: CfirExtend): FqName? =
                    if (extend === libraryExtend) libraryPackage else null
            }
            return CfirCompositeExtendProvider(listOf(own, stub))
        }
    }

    /**
     * 不执行真实类型解析的测试 resolver（对照 `CfirExtendIndexStoreTest` 的同名实现）。
     */
    private object NoopTypeResolver : CfirTypeResolver() {
        /**
         * 返回固定错误类型；测试只依赖已解析 type ref。
         */
        override fun resolveType(
            typeRef: CfirTypeRef,
            configuration: TypeResolutionConfiguration,
            areBareTypesAllowed: Boolean,
            isOperandOfIsOperator: Boolean,
            resolveDeprecations: Boolean,
            supertypeSupplier: SupertypeSupplier,
            expandTypeAliases: Boolean,
        ): CfirTypeResolutionResult {
            return CfirTypeResolutionResult(
                type = org.cangnova.cangjie.cfir.types.ConeErrorType(
                    org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic("NoopTypeResolver"),
                ),
                diagnostic = null,
            )
        }

        /**
         * 不从 type ref 解析 class。
         */
        override fun resolveClass(typeRef: CfirTypeRef): CfirClassLikeDeclaration? = null

        /**
         * 不从 ClassId 解析 class。
         */
        override fun resolveClass(classId: ClassId): CfirClassLikeDeclaration? = null
    }

    /**
     * 空符号 provider：所有 classId 解析失败，导出/可见性检查走宽松分支，
     * 可见性判定结果纯由导入可达性决定。
     */
    private class EmptySymbolProvider(session: CfirSession) : CfirSymbolProvider(session) {
        override val symbolNamesProvider: CfirSymbolNamesProvider = object : CfirSymbolNamesProvider() {
            override val hasSpecificClassifierPackageNamesComputation: Boolean = false
            override val hasSpecificCallablePackageNamesComputation: Boolean = false
            override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name>? = null
            override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name>? = null
        }

        override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? = null

        @OptIn(CfirSymbolProviderInternals::class)
        override fun getTopLevelCallableSymbolsTo(
            destination: MutableList<CfirCallableSymbol<*>>,
            packageFqName: FqName,
            name: Name,
        ) = Unit

        @OptIn(CfirSymbolProviderInternals::class)
        override fun getTopLevelFunctionSymbolsTo(
            destination: MutableList<CfirNamedFunctionSymbol>,
            packageFqName: FqName,
            name: Name,
        ) = Unit

        @OptIn(CfirSymbolProviderInternals::class)
        override fun getTopLevelPropertySymbolsTo(
            destination: MutableList<CfirPropertySymbol>,
            packageFqName: FqName,
            name: Name,
        ) = Unit

        override fun hasPackage(fqName: FqName): Boolean = false
    }
}
