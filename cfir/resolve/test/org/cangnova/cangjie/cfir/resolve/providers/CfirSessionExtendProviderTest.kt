package org.cangnova.cangjie.cfir.resolve.providers

import org.cangnova.cangjie.cfir.resolve.CfirTypeResolver
import org.cangnova.cangjie.cfir.resolve.ExtendTestFixtures
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.resolve.services.CfirExtendIndexStore
import org.cangnova.cangjie.cfir.types.classId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * [CfirSessionExtendProvider] 基于 extend index 查询扩展声明的测试。
 */
class CfirSessionExtendProviderTest {
    /**
     * 验证 provider 不会在 EXTENSIONS 阶段前隐式重建索引。
     */
    @Test
    fun `provider does not rebuild index implicitly before extensions phase`() {
        val (session, _) = ExtendTestFixtures.newSessionAndModule()
        val packageFqName = FqName("sample.pkg")
        val targetClassId = ClassId(packageFqName, Name.identifier("Target"))

        val store = CfirExtendIndexStore()
        val provider = CfirSessionExtendProvider(session, store)

        assertEquals(emptyList<CfirExtend>(), provider.getExtendsForClass(targetClassId))
    }

    /**
     * 验证 provider 从 index store 读取 class 和 package 级 extend。
     */
    @Test
    fun `provider reads extends for class and package from index store`() {
        val (session, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val packageFqName = FqName("sample.pkg")
        val targetClassId = ClassId(packageFqName, Name.identifier("Target"))
        val otherClassId = ClassId(packageFqName, Name.identifier("Other"))

        val targetExtend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(targetClassId),
            superTypeRefs = emptyList(),
        )
        val otherExtend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(otherClassId),
            superTypeRefs = emptyList(),
        )
        val file = ExtendTestFixtures.newFile(moduleData, packageFqName, listOf(targetExtend, otherExtend))

        val store = CfirExtendIndexStore().also { it.rebuild(listOf(file), NoopTypeResolver) }
        val provider = CfirSessionExtendProvider(session, store)

        assertEquals(listOf(targetExtend), provider.getExtendsForClass(targetClassId))
        assertEquals(listOf(targetExtend, otherExtend), provider.getExtendsInPackage(packageFqName))
    }

    /**
     * 验证 provider 可通过内建类型短名查找 primitive extend。
     */
    @Test
    fun `provider resolves builtin extends by builtin short name`() {
        val (session, moduleData) = ExtendTestFixtures.newSessionAndModule()
        val packageFqName = FqName("std.core")
        val int64ClassId = org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.INT64.classId

        val int64Extend = ExtendTestFixtures.newExtend(
            moduleData = moduleData,
            extendedTypeRef = ExtendTestFixtures.classTypeRef(int64ClassId),
            superTypeRefs = emptyList(),
        )
        val file = ExtendTestFixtures.newFile(moduleData, packageFqName, listOf(int64Extend))

        val store = CfirExtendIndexStore().also { it.rebuild(listOf(file), NoopTypeResolver) }
        val provider = CfirSessionExtendProvider(session, store)

        assertEquals(listOf(int64Extend), provider.getExtendsForBuiltinType(org.cangnova.cangjie.cfir.types.PrimitiveTypeKind.INT64))
    }
}

/**
 * extend index 测试使用的空 type resolver。
 */
private object NoopTypeResolver : CfirTypeResolver() {
    /**
     * 返回固定错误类型，测试不依赖真实类型解析。
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
     * 不解析 type ref 到 class。
     */
    override fun resolveClass(typeRef: org.cangnova.cangjie.cfir.types.CfirTypeRef): org.cangnova.cangjie.cfir.declarations.CfirClass? = null

    /**
     * 不解析 ClassId 到 class。
     */
    override fun resolveClass(classId: ClassId): org.cangnova.cangjie.cfir.declarations.CfirClass? = null
}
