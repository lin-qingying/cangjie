package org.cangnova.cangjie.analysis.light.declarations

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.annotations.CaAnnotationList
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaTypePointer
import org.cangnova.cangjie.analysis.api.types.CaUsualClassType
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 锁定声明视图基座模块的纯模型行为。
 *
 * `analysis:symbol-light-declarations` 负责接入真实语义，
 * 当前测试聚焦于 `analysis:light-declarations` 自己维护的两块稳定契约：
 * 1. 文本渲染树不会丢失结构信息；
 * 2. 缓存键会复用同一声明实例。
 */
class CaLightDeclarationRendererTest {
    /**
     * 验证 class-like 与 callable 的树形渲染保留声明层级。
     */
    @Test
    fun renderTreeKeepsDeclarationShape() {
        val token = TestLifetimeToken()
        val packageFqName = FqName("sample.peripheral")
        val ownerClassId = ClassId(packageFqName, Name.identifier("Greeter"))

        val member = CaLightCallableDeclarationImpl(
            name = "member",
            module = null,
            annotations = emptyList(),
            origin = sourceOrigin("Greeter.member", containingFile = null, sourceElement = null),
            token = token,
            callableId = CallableId(ownerClassId, Name.identifier("member")),
            signature = null,
        )
        val topLevel = CaLightCallableDeclarationImpl(
            name = "topLevel",
            module = null,
            annotations = emptyList(),
            origin = sourceOrigin("topLevel", containingFile = null, sourceElement = null),
            token = token,
            callableId = CallableId(packageFqName, Name.identifier("topLevel")),
            signature = null,
        )
        val classLike = CaLightClassLikeDeclarationImpl(
            name = "Greeter",
            module = null,
            annotations = emptyList(),
            origin = sourceOrigin("Greeter", containingFile = null, sourceElement = null),
            token = token,
            classId = ownerClassId,
            typeParameters = listOf(Name.identifier("T")),
            superTypes = emptyList(),
            members = listOf(member),
        )

        val renderedTree = CaLightDeclarationRenderer.renderTree(listOf(classLike, topLevel))

        assertTrue(renderedTree.contains("class sample/peripheral/Greeter<T>"))
        assertTrue(renderedTree.contains("  callable sample/peripheral/Greeter.member"))
        assertTrue(renderedTree.contains("callable sample/peripheral/topLevel"))
    }

    /**
     * 验证 extend 声明及其成员的树形渲染格式。
     */
    @Test
    fun renderTreeKeepsExtendShape() {
        val token = TestLifetimeToken()
        val packageFqName = FqName("sample.peripheral")
        val targetClassId = ClassId(packageFqName, Name.identifier("Display"))
        val member = CaLightCallableDeclarationImpl(
            name = "render",
            module = null,
            annotations = emptyList(),
            origin = sourceOrigin("extend:Display.render", containingFile = null, sourceElement = null),
            token = token,
            callableId = CallableId(targetClassId, Name.identifier("render")),
            signature = null,
        )
        val extendDeclaration = CaLightExtendDeclarationImpl(
            name = "Display",
            module = null,
            annotations = emptyList(),
            origin = sourceOrigin("extend:Display", containingFile = null, sourceElement = null),
            token = token,
            extendId = "sample.peripheral:Display<:",
            targetClassId = targetClassId,
            extendedType = TestType("sample.peripheral.Display", token),
            typeParameters = listOf(Name.identifier("T")),
            superTypes = emptyList(),
            members = listOf(member),
        )

        val renderedTree = CaLightDeclarationRenderer.renderTree(listOf(extendDeclaration))

        assertTrue(renderedTree.contains("extend sample/peripheral/Display<T>"))
        assertTrue(renderedTree.contains("  callable sample/peripheral/Display.render"))
    }

    /**
     * 验证相同缓存键会返回同一个 light declaration 实例。
     */
    @Test
    fun cacheReturnsStableDeclarationInstance() {
        val cache = CaLightDeclarationCache()
        val token = TestLifetimeToken()
        val cacheKey = CaLightDeclarationCacheKey("callable:sample/peripheral/topLevel")

        val first = cache.getOrPut(cacheKey) {
            CaLightCallableDeclarationImpl(
                name = "topLevel",
                module = null,
                annotations = emptyList(),
                origin = sourceOrigin("topLevel", containingFile = null, sourceElement = null),
                token = token,
                callableId = CallableId(FqName("sample.peripheral"), Name.identifier("topLevel")),
                signature = null,
            )
        }
        val second = cache.getOrPut(cacheKey) { first }

        assertSame(first, second)
    }

    /**
     * 测试用 lifetime token，始终处于有效且可访问状态。
     */
    private class TestLifetimeToken : CaLifetimeToken() {
        /**
         * 测试 token 始终有效。
         */
        override fun isValid(): Boolean = true

        /**
         * 返回测试 token 的稳定有效性说明。
         */
        override fun getInvalidationReason(): String = "valid in tests"

        /**
         * 测试 token 始终可访问。
         */
        override fun isAccessible(): Boolean = true

        /**
         * 返回测试 token 的稳定可访问性说明。
         */
        override fun getInaccessibilityReason(): String = "accessible in tests"
    }

    /**
     * 测试用类型实现，只提供 renderer 需要的展示文本和注解列表。
     */
    private class TestType(
        /**
         * 类型展示文本。
         */
        override val presentation: String,
        /**
         * 类型绑定的测试 token。
         */
        override val token: CaLifetimeToken,
    ) : CaType {
        /**
         * 测试类型没有缩写类型。
         */
        override val abbreviation: CaUsualClassType? = null
        /**
         * 测试类型没有注解。
         */
        override val annotations: CaAnnotationList = EmptyAnnotationList(token)

        /**
         * 创建可恢复为当前测试类型实例的类型指针。
         */
        override fun createPointer(): CaTypePointer<CaType> = object : CaTypePointer<CaType> {
            @OptIn(CaImplementationDetail::class)
            override fun restore(session: CaSession): CaType = this@TestType
        }
    }

    /**
     * 空注解列表测试实现。
     */
    private class EmptyAnnotationList(
        /**
         * 注解列表绑定的测试 token。
         */
        override val token: CaLifetimeToken,
    ) : AbstractList<CaAnnotation>(), CaAnnotationList {
        /**
         * 空注解列表大小固定为 0。
         */
        override val size: Int = 0
        /**
         * 空列表读取任意下标都会失败。
         */
        override fun get(index: Int): CaAnnotation = throw IndexOutOfBoundsException("Index $index out of bounds")
        /**
         * 空列表不包含任何注解 classId。
         */
        override fun contains(classId: ClassId): Boolean = false
        /**
         * 空列表按 classId 查询始终返回空集合。
         */
        override fun get(classId: ClassId): List<CaAnnotation> = emptyList()
        /**
         * 空列表没有注解 classId 集合。
         */
        override val classIds: Collection<ClassId> = emptyList()
    }
}
