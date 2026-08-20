@file:OptIn(org.cangnova.cangjie.cfir.CfirImplementationDetail::class)

package org.cangnova.cangjie.cfir.resolve.services

import org.cangnova.cangjie.cfir.ScopeSession
import org.cangnova.cangjie.cfir.common.CfirModuleData
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationAttributes
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase
import org.cangnova.cangjie.cfir.declarations.builder.buildImport
import org.cangnova.cangjie.cfir.declarations.builder.buildNamedFunction
import org.cangnova.cangjie.cfir.declarations.builder.buildProperty
import org.cangnova.cangjie.cfir.declarations.impl.CfirClassImpl
import org.cangnova.cangjie.cfir.declarations.impl.CfirDeclarationStatusImpl
import org.cangnova.cangjie.cfir.resolve.CfirImportBindingResolver
import org.cangnova.cangjie.cfir.resolve.CfirTypeCandidateCollector
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolutionResult
import org.cangnova.cangjie.cfir.resolve.CfirTypeResolver
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures.TestSession
import org.cangnova.cangjie.cfir.resolve.SupertypeSupplier
import org.cangnova.cangjie.cfir.resolve.TypeResolutionConfiguration
import org.cangnova.cangjie.cfir.resolve.createFileLookupScopes
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessContext
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessKind
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityChecker
import org.cangnova.cangjie.cfir.resolve.providers.CfirAccessibilityResult
import org.cangnova.cangjie.cfir.resolve.providers.CfirCompositeExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendExportSurfaceService
import org.cangnova.cangjie.cfir.resolve.providers.CfirExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirInstantiatedSupertypeDescriptor
import org.cangnova.cangjie.cfir.resolve.providers.CfirInstantiatedSupertypeOrigin
import org.cangnova.cangjie.cfir.resolve.providers.CfirLookupOrigin
import org.cangnova.cangjie.cfir.resolve.providers.CfirLookupDisposition
import org.cangnova.cangjie.cfir.resolve.providers.CfirProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolNamesProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSessionExtendProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirTypeAwareSupertypeProvider
import org.cangnova.cangjie.cfir.scopes.CfirCallableLookupProvenance
import org.cangnova.cangjie.cfir.scopes.CfirDefaultImportsProviderHolder
import org.cangnova.cangjie.cfir.scopes.defaultImportsProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.accessibilityChecker
import org.cangnova.cangjie.cfir.session.services.CfirExtendTargetKey
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirClassLikeSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.types.CfirTypeRef
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.cfir.types.PrimitiveTypeKind
import org.cangnova.cangjie.cfir.types.builder.buildImplicitTypeRef
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.descriptors.Visibility
import org.cangnova.cangjie.descriptors.Visibilities
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.DefaultImportsProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 统一 [CfirAccessibilityChecker] 的 deserialized（库）extend 路径测试。
 *
 * provider 只提供 extend 的结构索引；consumer session 的 checker 从声明和显式
 * use-site context 派生导出面，source 与 library 路径必须共享同一判断。
 *
 * 组合链注册（own + 匿名库 stub）精确模拟生产环境：`CfirSessionExtendProvider`
 * 对库声明返回 `null` 包名后，组合 provider 继续查库 stub。
 */
class CfirAccessibilityCheckerExtendTest {

    /**
     * private 顶层声明只在声明文件中可发现；同包的另一个文件不能复用该访问结论。
     */
    @Test
    fun `private top level declarations are visible only from their declaration file`() {
        val context = newContext(extendPackage = FqName("visibility.private"))
        val packageName = FqName("visibility.private")
        val hiddenType = newClass(
            context = context,
            classId = ClassId(packageName, Name.identifier("HiddenType")),
            visibility = Visibilities.Private,
        )
        val hiddenFunction = newFunction(
            context = context,
            callableId = CallableId(packageName, Name.identifier("hiddenFunction")),
            visibility = Visibilities.Private,
        )
        val declarationFile = ExtendTestFixtures.newFile(
            context.moduleData,
            packageName,
            listOf(hiddenType, hiddenFunction),
            fileName = "declaration.cj",
        )
        val otherFile = ExtendTestFixtures.newFile(
            context.moduleData,
            packageName,
            emptyList(),
            fileName = "use.cj",
        )
        context.recordFiles(declarationFile, otherFile)

        assertAccessible(context.session.accessibilityChecker.checkClassLike(
            hiddenType.symbol,
            accessContext(declarationFile, CfirAccessKind.TYPE),
        ))
        assertInaccessible(
            context.session.accessibilityChecker.checkClassLike(
                hiddenType.symbol,
                accessContext(otherFile, CfirAccessKind.TYPE),
            ),
            owner = hiddenType.symbol,
            disposition = CfirLookupDisposition.NOT_DISCOVERABLE,
        )
        assertAccessible(context.session.accessibilityChecker.checkCallable(
            hiddenFunction.symbol,
            accessContext(declarationFile, CfirAccessKind.CALLABLE),
        ))
        assertInaccessible(
            context.session.accessibilityChecker.checkCallable(
                hiddenFunction.symbol,
                accessContext(otherFile, CfirAccessKind.CALLABLE),
            ),
            owner = hiddenFunction.symbol,
            disposition = CfirLookupDisposition.NOT_DISCOVERABLE,
        )
    }

    /**
     * private 类函数在同包结构成员集中参与 lookup，跨包则不进入导出成员面；函数值访问
     * 与普通调用使用同一个可访问性判断，但消费不同 disposition。private 属性始终保留
     * 真实目标，由访问控制诊断负责报告。
     */
    @Test
    fun `private member functions and properties preserve call and value dispositions`() {
        val context = newContext(extendPackage = FqName("visibility.members"))
        val packageName = FqName("visibility.members")
        val ownerId = ClassId(packageName, Name.identifier("Owner"))
        val hiddenFunction = newFunction(
            context,
            CallableId(ownerId, Name.identifier("hiddenFunction")),
            Visibilities.Private,
        )
        val hiddenProperty = newProperty(
            context,
            CallableId(ownerId, Name.identifier("hiddenProperty")),
            Visibilities.Private,
        )
        val owner = newClass(
            context = context,
            classId = ownerId,
            declarations = listOf(hiddenFunction, hiddenProperty),
        )
        val declarationFile = ExtendTestFixtures.newFile(
            context.moduleData,
            packageName,
            listOf(owner),
            fileName = "owner.cj",
        )
        val samePackageFile = ExtendTestFixtures.newFile(
            context.moduleData,
            packageName,
            emptyList(),
            fileName = "same_package.cj",
        )
        val otherPackageFile = ExtendTestFixtures.newFile(
            context.moduleData,
            FqName("consumer"),
            emptyList(),
            fileName = "other_package.cj",
        )
        context.recordFiles(declarationFile, samePackageFile, otherPackageFile)

        assertAccessible(context.session.accessibilityChecker.checkCallable(
            hiddenFunction.symbol,
            accessContext(declarationFile, CfirAccessKind.CALLABLE, listOf(owner)),
        ))
        assertInaccessible(
            context.session.accessibilityChecker.checkCallable(
                hiddenFunction.symbol,
                accessContext(samePackageFile, CfirAccessKind.CALLABLE),
            ),
            owner = hiddenFunction.symbol,
            disposition = CfirLookupDisposition.EXCLUDE_CALLABLE,
        )
        assertInaccessible(
            context.session.accessibilityChecker.checkCallable(
                hiddenFunction.symbol,
                accessContext(otherPackageFile, CfirAccessKind.CALLABLE),
            ),
            owner = hiddenFunction.symbol,
            disposition = CfirLookupDisposition.NOT_DISCOVERABLE,
        )
        assertInaccessible(
            context.session.accessibilityChecker.checkCallable(
                hiddenFunction.symbol,
                accessContext(samePackageFile, CfirAccessKind.NAMED_VALUE),
            ),
            owner = hiddenFunction.symbol,
            disposition = CfirLookupDisposition.REPORT_ACCESS_ERROR,
        )
        assertInaccessible(
            context.session.accessibilityChecker.checkCallable(
                hiddenFunction.symbol,
                accessContext(otherPackageFile, CfirAccessKind.NAMED_VALUE),
            ),
            owner = hiddenFunction.symbol,
            disposition = CfirLookupDisposition.NOT_DISCOVERABLE,
        )
        assertInaccessible(
            context.session.accessibilityChecker.checkCallable(
                hiddenProperty.symbol,
                accessContext(otherPackageFile, CfirAccessKind.NAMED_VALUE),
            ),
            owner = hiddenProperty.symbol,
            disposition = CfirLookupDisposition.REPORT_ACCESS_ERROR,
        )
    }

    /**
     * internal 以声明包及其子包为边界；不可访问函数调用从 overload 集排除，函数值访问
     * 保留目标并报告访问错误。
     */
    @Test
    fun `internal declarations follow package and subpackage visibility`() {
        val context = newContext(extendPackage = FqName("library.api"))
        val declarationPackage = FqName("library.api")
        val ownerId = ClassId(declarationPackage, Name.identifier("InternalOwner"))
        val function = newFunction(
            context,
            CallableId(ownerId, Name.identifier("internalFunction")),
            Visibilities.Internal,
        )
        val owner = newClass(context, ownerId, declarations = listOf(function))
        val declarationFile = ExtendTestFixtures.newFile(context.moduleData, declarationPackage, listOf(owner))
        val subpackageFile = ExtendTestFixtures.newFile(context.moduleData, FqName("library.api.child"), emptyList())
        val unrelatedFile = ExtendTestFixtures.newFile(context.moduleData, FqName("consumer"), emptyList())
        context.recordFiles(declarationFile, subpackageFile, unrelatedFile)

        assertAccessible(context.session.accessibilityChecker.checkCallable(
            function.symbol,
            accessContext(declarationFile, CfirAccessKind.CALLABLE),
        ))
        assertAccessible(context.session.accessibilityChecker.checkCallable(
            function.symbol,
            accessContext(subpackageFile, CfirAccessKind.CALLABLE),
        ))
        assertInaccessible(
            context.session.accessibilityChecker.checkCallable(
                function.symbol,
                accessContext(unrelatedFile, CfirAccessKind.CALLABLE),
            ),
            owner = function.symbol,
            disposition = CfirLookupDisposition.EXCLUDE_CALLABLE,
        )
        assertInaccessible(
            context.session.accessibilityChecker.checkCallable(
                function.symbol,
                accessContext(unrelatedFile, CfirAccessKind.NAMED_VALUE),
            ),
            owner = function.symbol,
            disposition = CfirLookupDisposition.REPORT_ACCESS_ERROR,
        )
    }

    /**
     * protected 同时消费包关系与真实继承关系；两条路径都由 session checker 统一计算。
     */
    @Test
    fun `protected declarations are accessible from related packages and subclasses`() {
        val context = newContext(extendPackage = FqName("library.api"))
        val declarationPackage = FqName("library.api")
        val baseId = ClassId(declarationPackage, Name.identifier("ProtectedBase"))
        val function = newFunction(
            context,
            CallableId(baseId, Name.identifier("protectedFunction")),
            Visibilities.Protected,
        )
        val base = newClass(context, baseId, declarations = listOf(function))
        val childId = ClassId(FqName("consumer"), Name.identifier("ProtectedChild"))
        val child = newClass(context, childId)
        context.supertypeProvider.recordSupertype(childId, base.symbol.constructType())
        val declarationFile = ExtendTestFixtures.newFile(context.moduleData, declarationPackage, listOf(base))
        val relatedPackageFile = ExtendTestFixtures.newFile(context.moduleData, FqName("library.feature"), emptyList())
        val childFile = ExtendTestFixtures.newFile(context.moduleData, FqName("consumer"), listOf(child))
        val unrelatedFile = ExtendTestFixtures.newFile(context.moduleData, FqName("outsider"), emptyList())
        context.recordFiles(declarationFile, relatedPackageFile, childFile, unrelatedFile)

        assertAccessible(context.session.accessibilityChecker.checkCallable(
            function.symbol,
            accessContext(relatedPackageFile, CfirAccessKind.CALLABLE),
        ))
        assertAccessible(context.session.accessibilityChecker.checkCallable(
            function.symbol,
            accessContext(childFile, CfirAccessKind.CALLABLE, listOf(child)),
        ))
        assertInaccessible(
            context.session.accessibilityChecker.checkCallable(
                function.symbol,
                accessContext(unrelatedFile, CfirAccessKind.CALLABLE),
            ),
            owner = function.symbol,
            disposition = CfirLookupDisposition.EXCLUDE_CALLABLE,
        )
        assertInaccessible(
            context.session.accessibilityChecker.checkCallable(
                function.symbol,
                accessContext(unrelatedFile, CfirAccessKind.NAMED_VALUE),
            ),
            owner = function.symbol,
            disposition = CfirLookupDisposition.REPORT_ACCESS_ERROR,
        )
    }

    /**
     * public 成员不能穿透 private 外围类型；诊断 owner 必须指向真正阻断访问的外围声明。
     */
    @Test
    fun `inaccessible containing type becomes the reporting owner`() {
        val context = newContext(extendPackage = FqName("visibility.container"))
        val packageName = FqName("visibility.container")
        val ownerId = ClassId(packageName, Name.identifier("PrivateContainer"))
        val publicFunction = newFunction(
            context,
            CallableId(ownerId, Name.identifier("publicFunction")),
            Visibilities.Public,
        )
        val privateOwner = newClass(
            context = context,
            classId = ownerId,
            visibility = Visibilities.Private,
            declarations = listOf(publicFunction),
        )
        val declarationFile = ExtendTestFixtures.newFile(context.moduleData, packageName, listOf(privateOwner))
        val otherFile = ExtendTestFixtures.newFile(context.moduleData, packageName, emptyList())
        context.recordFiles(declarationFile, otherFile)

        assertAccessible(context.session.accessibilityChecker.checkCallable(
            publicFunction.symbol,
            accessContext(declarationFile, CfirAccessKind.CALLABLE),
        ))
        assertInaccessible(
            context.session.accessibilityChecker.checkCallable(
                publicFunction.symbol,
                accessContext(otherFile, CfirAccessKind.CALLABLE),
            ),
            owner = privateOwner.symbol,
            disposition = CfirLookupDisposition.NOT_DISCOVERABLE,
        )
    }

    /**
     * private extend 成员在 extend 本体内可访问，离开 owner 后作为调用候选排除。
     */
    @Test
    fun `private extend member uses the real extend owner`() {
        val packageName = FqName("visibility.extend")
        val context = newContext(extendPackage = packageName)
        val targetId = ClassId(packageName, Name.identifier("Target"))
        context.registerPublicClass(targetId)
        val member = newFunction(
            context,
            CallableId(packageName, Name.identifier("privateExtendFunction")),
            Visibilities.Private,
        )
        val extend = ExtendTestFixtures.newExtend(
            moduleData = context.moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetId),
            superTypeRefs = emptyList(),
            declarations = listOf(member),
        )
        context.session.register(
            CfirExtendProvider::class,
            context.compositeWithLibraryExtend(extend, packageName),
        )
        val useSite = ExtendTestFixtures.newFile(context.moduleData, packageName, emptyList())

        assertAccessible(context.session.accessibilityChecker.checkCallable(
            member.symbol,
            accessContext(useSite, CfirAccessKind.CALLABLE, listOf(extend)),
            CfirCallableLookupProvenance.directExtendMember(extend),
        ))
        assertInaccessible(
            context.session.accessibilityChecker.checkCallable(
                member.symbol,
                accessContext(useSite, CfirAccessKind.CALLABLE),
                CfirCallableLookupProvenance.directExtendMember(extend),
            ),
            owner = member.symbol,
            disposition = CfirLookupDisposition.EXCLUDE_CALLABLE,
        )
    }

    /**
     * 同一 ScopeSession 只缓存文件结构 scope；private 类型的访问结果必须随完整文件 context
     * 重新计算，不能从声明文件泄漏到同包的另一个文件。
     */
    @Test
    fun `shared scope session does not leak private type accessibility between files`() {
        val context = newContext(extendPackage = FqName("visibility.scope"))
        val packageName = FqName("visibility.scope")
        val hiddenType = newClass(
            context,
            ClassId(packageName, Name.identifier("FilePrivateType")),
            Visibilities.Private,
        )
        val declarationFile = ExtendTestFixtures.newFile(
            context.moduleData,
            packageName,
            listOf(hiddenType),
            fileName = "declaration.cj",
        )
        val otherFile = ExtendTestFixtures.newFile(
            context.moduleData,
            packageName,
            emptyList(),
            fileName = "use.cj",
        )
        context.recordFiles(declarationFile, otherFile)
        context.recordImportBindingsIfNeeded(declarationFile)
        context.recordImportBindingsIfNeeded(otherFile)
        val scopeSession = ScopeSession()
        val declarationScopes = context.session.createFileLookupScopes(declarationFile, scopeSession)
        val repeatedDeclarationScopes = context.session.createFileLookupScopes(declarationFile, scopeSession)
        val otherScopes = context.session.createFileLookupScopes(otherFile, scopeSession)

        assertSame(declarationScopes, repeatedDeclarationScopes)
        val declarationCandidate = CfirTypeCandidateCollector(
            context.session,
            accessContext(declarationFile, CfirAccessKind.TYPE),
        ).firstVisibleScopeCandidate(declarationScopes.typeResolutionScopes, hiddenType.name)
        val otherCandidate = CfirTypeCandidateCollector(
            context.session,
            accessContext(otherFile, CfirAccessKind.TYPE),
        ).firstVisibleScopeCandidate(otherScopes.typeResolutionScopes, hiddenType.name)

        assertSame(hiddenType.symbol, declarationCandidate?.symbol)
        assertEquals(null, otherCandidate)
    }

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

        assertFalse(isExtendVisibleFrom(context, useSite, extend))
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

        assertTrue(isExtendVisibleFrom(context, useSite, extend))
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

        assertTrue(isExtendVisibleFrom(context, useSite, extend))
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

        assertTrue(isExtendVisibleFrom(context, useSite, extend))
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
                    lookupOrigin = CfirLookupOrigin.EXPLICIT_IMPORT,
                ),
            ),
        )

        assertTrue(isExtendVisibleFrom(context, useSite, extend))
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

        assertTrue(isExtendVisibleFrom(context, useSite, extend))
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

        assertTrue(isExtendVisibleFrom(context, useSite, extend))
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
        context.registerPublicClass(ClassId(FqName("lib"), Name.identifier("Box")))
        context.registerPublicInterface(ClassId(FqName("lib"), Name.identifier("I")))
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

        assertFalse(isExtendVisibleFrom(context, useSite, extend))
    }

    /** 构造具有真实 ClassId、声明状态和成员列表的测试 class。 */
    private fun newClass(
        context: TestContext,
        classId: ClassId,
        visibility: Visibility = Visibilities.Public,
        declarations: List<CfirDeclaration> = emptyList(),
        superTypeRefs: List<CfirTypeRef> = emptyList(),
    ): CfirClassImpl = ExtendTestFixtures.newClass(
        moduleData = context.moduleData,
        name = classId.shortClassName.asString(),
        classId = classId,
        superTypeRefs = superTypeRefs,
        declarations = declarations,
    ).apply {
        status = CfirDeclarationStatusImpl(visibility = visibility)
    }

    /** 构造供统一 checker 直接消费的具名函数声明。 */
    private fun newFunction(
        context: TestContext,
        callableId: CallableId,
        visibility: Visibility,
    ): CfirNamedFunction = buildNamedFunction {
        moduleData = context.moduleData
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Library
        attributes = CfirDeclarationAttributes.EMPTY
        isLocal = false
        dispatchReceiverType = null
        status = CfirDeclarationStatusImpl(visibility = visibility)
        returnTypeRef = buildImplicitTypeRef()
        symbol = CfirNamedFunctionSymbol(callableId)
        name = callableId.callableName
        isMut = false
    }

    /** 构造供命名值访问路径消费的属性声明。 */
    private fun newProperty(
        context: TestContext,
        callableId: CallableId,
        visibility: Visibility,
    ): CfirProperty = buildProperty {
        moduleData = context.moduleData
        resolvePhase = CfirResolvePhase.BODY_RESOLVE
        origin = CfirDeclarationOrigin.Library
        attributes = CfirDeclarationAttributes.EMPTY
        isLocal = false
        dispatchReceiverType = null
        symbol = CfirPropertySymbol(callableId)
        status = CfirDeclarationStatusImpl(visibility = visibility)
        returnTypeRef = buildImplicitTypeRef()
        name = callableId.callableName
    }

    /** 创建不依赖线程局部状态的显式使用点。 */
    private fun accessContext(
        file: CfirFile,
        kind: CfirAccessKind,
        containingDeclarations: List<CfirDeclaration> = emptyList(),
    ): CfirAccessContext = CfirAccessContext(
        useSiteFile = file,
        containingDeclarations = containingDeclarations,
        kind = kind,
    )

    /** 断言候选对当前使用点可访问。 */
    private fun assertAccessible(result: CfirAccessibilityResult) {
        assertSame(CfirAccessibilityResult.Accessible, result)
    }

    /** 断言不可访问结果同时保留真实诊断 owner 与统一 lookup disposition。 */
    private fun assertInaccessible(
        result: CfirAccessibilityResult,
        owner: CfirBasedSymbol<*>,
        disposition: CfirLookupDisposition,
    ) {
        val inaccessible = assertInstanceOf(CfirAccessibilityResult.Inaccessible::class.java, result)
        assertSame(owner, inaccessible.reportingOwner)
        assertEquals(disposition, inaccessible.disposition)
    }

    /** 以显式 use-site context 调用会话级可见性服务。 */
    private fun isExtendVisibleFrom(context: TestContext, file: CfirFile, extend: CfirExtend): Boolean {
        context.recordImportBindingsIfNeeded(file)
        return context.session.accessibilityChecker.checkExtend(
            extend,
            CfirAccessContext(useSiteFile = file, kind = CfirAccessKind.EXTEND),
        ) is CfirAccessibilityResult.Accessible
    }

    /**
     * 构造库（deserialized 风格）extend：声明进组合视图的库 stub 包索引，不进 index store。
     */
    private fun newLibraryExtend(
        context: TestContext,
        targetClassId: ClassId,
        interfaceClassId: ClassId,
    ): CfirExtend {
        context.registerPublicClass(targetClassId)
        context.registerPublicInterface(interfaceClassId)
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
        val symbolProvider = MutableSymbolProvider(session)
        session.register(CfirSymbolProvider::class, symbolProvider)
        val declarationProvider = MutableDeclarationProvider(symbolProvider)
        session.register(CfirProvider::class, declarationProvider)
        val supertypeProvider = MutableTypeAwareSupertypeProvider()
        session.register(CfirTypeAwareSupertypeProvider::class, supertypeProvider)
        session.register(CfirExtendExportSurfaceService::class, CfirExtendExportSurfaceService(session))
        session.register(CfirAccessibilityChecker::class, CfirAccessibilityChecker(session))
        return TestContext(
            session,
            moduleData,
            store,
            bindingStore,
            extendPackage,
            symbolProvider,
            declarationProvider,
            supertypeProvider,
        )
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
        private val symbolProvider: MutableSymbolProvider,
        private val declarationProvider: MutableDeclarationProvider,
        val supertypeProvider: MutableTypeAwareSupertypeProvider,
    ) {
        /** 记录源码文件的声明、文件归属和成员 owner 结构事实。 */
        fun recordFiles(vararg files: CfirFile) {
            files.forEach(declarationProvider::recordFile)
        }

        /**
         * 单测直接调用 checker，因此在调用前显式执行生产 IMPORTS 阶段的 binding 建立。
         * 这里复用真实 binding resolver，不允许 checker 因测试 session 缺阶段数据而回退。
         */
        fun recordImportBindingsIfNeeded(file: CfirFile) {
            val resolver = CfirImportBindingResolver(session)
            val defaultImportsProvider = session.defaultImportsProvider
            if (bindingStore.getDefaultImportBindings(CfirDefaultImportPriority.HIGH) == null) {
                bindingStore.recordDefaultImportBindings(
                    CfirDefaultImportPriority.HIGH,
                    resolver.resolveDefaultImportBindings(
                        defaultImportsProvider.getDefaultImports(includeLowPriorityImports = false),
                        defaultImportsProvider.excludedImports,
                    ),
                )
            }
            if (bindingStore.getDefaultImportBindings(CfirDefaultImportPriority.LOW) == null) {
                bindingStore.recordDefaultImportBindings(
                    CfirDefaultImportPriority.LOW,
                    resolver.resolveDefaultImportBindings(
                        defaultImportsProvider.defaultLowPriorityImports,
                        defaultImportsProvider.excludedImports,
                    ),
                )
            }
            if (bindingStore.getBindings(file) == null) {
                bindingStore.record(
                    file,
                    file.imports.map { importDirective ->
                        resolver.resolveImportBinding(importDirective, CfirLookupOrigin.EXPLICIT_IMPORT)
                    },
                )
            }
        }

        fun registerPublicClass(classId: ClassId) {
            symbolProvider.register(ExtendTestFixtures.newClass(
                moduleData = moduleData,
                name = classId.shortClassName.asString(),
                classId = classId,
            ))
        }

        fun registerPublicInterface(classId: ClassId) {
            symbolProvider.register(ExtendTestFixtures.newInterface(
                moduleData = moduleData,
                name = classId.shortClassName.asString(),
                classId = classId,
            ))
        }
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
                override fun getContainingExtend(symbol: CfirCallableSymbol<*>): CfirExtend? =
                    libraryExtend.takeIf { extend -> extend.declarations.any { it.symbol === symbol } }
                override fun getPackageFqName(extend: CfirExtend): FqName? =
                    if (extend === libraryExtend) libraryPackage else null
            }
            return CfirCompositeExtendProvider(listOf(own, stub))
        }
    }

    /**
     * 测试使用的声明归属 provider。
     *
     * 它只记录生产 [CfirProvider] 本来就应提供的结构事实：包内文件、声明文件和成员外层类；
     * 不执行任何可见性判断，避免把待测逻辑复制到 fixture 中。
     */
    private class MutableDeclarationProvider(
        override val symbolProvider: MutableSymbolProvider,
    ) : CfirProvider() {
        private val filesByPackage = linkedMapOf<FqName, MutableList<CfirFile>>()
        private val classifierFiles = linkedMapOf<ClassId, CfirFile>()
        private val callableFiles = linkedMapOf<CfirCallableSymbol<*>, CfirFile>()
        private val callableOwners = linkedMapOf<CfirCallableSymbol<*>, CfirClassLikeSymbol<*>>()

        /** 将一个文件及其声明树写入结构索引。 */
        fun recordFile(file: CfirFile) {
            filesByPackage.getOrPut(file.packageDirective.packageFqName, ::mutableListOf).add(file)
            file.declarations.forEach { declaration ->
                recordDeclaration(
                    declaration = declaration,
                    file = file,
                    containingClass = null,
                    isTopLevel = true,
                )
            }
        }

        /** 递归记录 class-like 成员与 callable owner，不生成访问敏感缓存。 */
        private fun recordDeclaration(
            declaration: CfirDeclaration,
            file: CfirFile,
            containingClass: CfirClassLikeSymbol<*>?,
            isTopLevel: Boolean,
        ) {
            when (declaration) {
                is CfirClassLikeDeclaration -> {
                    if (isTopLevel) {
                        symbolProvider.register(declaration)
                        classifierFiles[declaration.symbol.classId] = file
                    }
                    declaration.declarations.forEach { member ->
                        recordDeclaration(member, file, declaration.symbol, isTopLevel = false)
                    }
                }

                is CfirCallableDeclaration -> {
                    val symbol = declaration.symbol
                    callableFiles[symbol] = file
                    containingClass?.let { callableOwners[symbol] = it }
                    if (isTopLevel) {
                        symbolProvider.registerTopLevel(declaration, file.packageDirective.packageFqName)
                    }
                }

                else -> Unit
            }
        }

        override fun getCfirClassifierByFqName(classId: ClassId): CfirClassLikeDeclaration? =
            symbolProvider.getClassLikeSymbolByClassId(classId)?.cfir

        override fun getCfirClassifierContainerFile(fqName: ClassId): CfirFile =
            checkNotNull(classifierFiles[fqName]) { "No classifier file registered for $fqName" }

        override fun getCfirClassifierContainerFileIfAny(fqName: ClassId): CfirFile? = classifierFiles[fqName]

        override fun getCfirCallableContainerFile(symbol: CfirCallableSymbol<*>): CfirFile? = callableFiles[symbol]

        override fun getCfirFilesByPackage(fqName: FqName): List<CfirFile> = filesByPackage[fqName].orEmpty()

        override fun getClassNamesInPackage(fqName: FqName): Set<Name> =
            symbolProvider.symbolNamesProvider.getTopLevelClassifierNamesInPackage(fqName).orEmpty()

        override fun getContainingClass(symbol: CfirBasedSymbol<*>): CfirClassLikeSymbol<*>? =
            (symbol as? CfirCallableSymbol<*>)?.let(callableOwners::get) ?: super.getContainingClass(symbol)
    }

    /** 可变的直接父类型图，仅向 checker 提供真实的结构父边。 */
    private class MutableTypeAwareSupertypeProvider : CfirTypeAwareSupertypeProvider {
        private val descriptorsByClassId = linkedMapOf<ClassId, MutableList<CfirInstantiatedSupertypeDescriptor>>()

        /** 为一个 class-like 声明登记直接父类型。 */
        fun recordSupertype(classId: ClassId, supertype: ConeCangJieType) {
            val supertypeClassId = checkNotNull(supertype.classIdOrPrimitiveClassId) {
                "Test supertype must have a nominal ClassId: $supertype"
            }
            descriptorsByClassId.getOrPut(classId, ::mutableListOf) += CfirInstantiatedSupertypeDescriptor(
                type = supertype,
                origin = CfirInstantiatedSupertypeOrigin.Declared(
                    ExtendTestFixtures.classTypeRef(supertypeClassId),
                ),
            )
        }

        override fun getDirectSupertypeDescriptors(type: ConeCangJieType): List<CfirInstantiatedSupertypeDescriptor> =
            type.classIdOrPrimitiveClassId?.let(descriptorsByClassId::get).orEmpty()
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

    /** 提供真实 class-like symbol 的可变测试索引，禁止用符号缺失模拟可见性通过。 */
    private class MutableSymbolProvider(session: CfirSession) : CfirSymbolProvider(session) {
        private val declarations = linkedMapOf<ClassId, CfirClassLikeSymbol<*>>()
        private val topLevelFunctions = linkedMapOf<Pair<FqName, Name>, MutableList<CfirNamedFunctionSymbol>>()
        private val topLevelProperties = linkedMapOf<Pair<FqName, Name>, MutableList<CfirPropertySymbol>>()

        fun register(declaration: CfirClassLikeDeclaration) {
            declarations[declaration.symbol.classId] = declaration.symbol
        }

        /** 记录包级 callable 的结构索引。 */
        fun registerTopLevel(declaration: CfirCallableDeclaration, packageFqName: FqName) {
            val key = packageFqName to declaration.symbol.name
            when (val symbol = declaration.symbol) {
                is CfirNamedFunctionSymbol -> topLevelFunctions.getOrPut(key, ::mutableListOf).add(symbol)
                is CfirPropertySymbol -> topLevelProperties.getOrPut(key, ::mutableListOf).add(symbol)
                else -> Unit
            }
        }

        override val symbolNamesProvider: CfirSymbolNamesProvider = object : CfirSymbolNamesProvider() {
            override val hasSpecificClassifierPackageNamesComputation: Boolean = false
            override val hasSpecificCallablePackageNamesComputation: Boolean = false
            override fun getPackageNames(): Set<String> = buildSet {
                declarations.keys.mapTo(this) { it.packageFqName.asString() }
                topLevelFunctions.keys.mapTo(this) { it.first.asString() }
                topLevelProperties.keys.mapTo(this) { it.first.asString() }
            }

            override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name> =
                declarations.keys.asSequence()
                    .filter { it.packageFqName == packageFqName }
                    .mapTo(linkedSetOf()) { it.shortClassName }

            override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> = buildSet {
                topLevelFunctions.keys.asSequence()
                    .filter { it.first == packageFqName }
                    .mapTo(this) { it.second }
                topLevelProperties.keys.asSequence()
                    .filter { it.first == packageFqName }
                    .mapTo(this) { it.second }
            }
        }

        override fun getClassLikeSymbolByClassId(classId: ClassId): CfirClassLikeSymbol<*>? = declarations[classId]

        @OptIn(CfirSymbolProviderInternals::class)
        override fun getTopLevelCallableSymbolsTo(
            destination: MutableList<CfirCallableSymbol<*>>,
            packageFqName: FqName,
            name: Name,
        ) {
            destination += topLevelFunctions[packageFqName to name].orEmpty()
            destination += topLevelProperties[packageFqName to name].orEmpty()
        }

        @OptIn(CfirSymbolProviderInternals::class)
        override fun getTopLevelFunctionSymbolsTo(
            destination: MutableList<CfirNamedFunctionSymbol>,
            packageFqName: FqName,
            name: Name,
        ) {
            destination += topLevelFunctions[packageFqName to name].orEmpty()
        }

        @OptIn(CfirSymbolProviderInternals::class)
        override fun getTopLevelPropertySymbolsTo(
            destination: MutableList<CfirPropertySymbol>,
            packageFqName: FqName,
            name: Name,
        ) {
            destination += topLevelProperties[packageFqName to name].orEmpty()
        }

        override fun hasPackage(fqName: FqName): Boolean =
            declarations.keys.any { it.packageFqName == fqName } ||
                topLevelFunctions.keys.any { it.first == fqName } ||
                topLevelProperties.keys.any { it.first == fqName }
    }
}
