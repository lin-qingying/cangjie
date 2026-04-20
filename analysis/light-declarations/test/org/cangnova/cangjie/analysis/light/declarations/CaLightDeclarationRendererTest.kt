package org.cangnova.cangjie.analysis.light.declarations

import org.cangnova.cangjie.analysis.api.annotations.CaAnnotation
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeToken
import org.cangnova.cangjie.analysis.api.signatures.CaSignature
import org.cangnova.cangjie.analysis.api.signatures.CaValueParameterSignature
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.types.CaSubstitutor
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
            signature = TestSignature("member(value: Int64): Int64", token),
        )
        val topLevel = CaLightCallableDeclarationImpl(
            name = "topLevel",
            module = null,
            annotations = emptyList(),
            origin = sourceOrigin("topLevel", containingFile = null, sourceElement = null),
            token = token,
            callableId = CallableId(packageFqName, Name.identifier("topLevel")),
            signature = TestSignature("topLevel(): Int64", token),
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
        assertTrue(renderedTree.contains("  callable sample/peripheral/Greeter.member member(value: Int64): Int64"))
        assertTrue(renderedTree.contains("callable sample/peripheral/topLevel topLevel(): Int64"))
    }

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
            signature = TestSignature("render(): String", token),
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
        assertTrue(renderedTree.contains("  callable sample/peripheral/Display.render render(): String"))
    }

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
                signature = TestSignature("topLevel(): Int64", token),
            )
        }
        val second = cache.getOrPut(cacheKey) { first }

        assertSame(first, second)
    }

    private class TestLifetimeToken : CaLifetimeToken() {
        override fun isValid(): Boolean = true

        override fun getInvalidationReason(): String = "valid in tests"

        override fun isAccessible(): Boolean = true

        override fun getInaccessibilityReason(): String = "accessible in tests"
    }

    private class TestType(
        override val presentation: String,
        override val token: CaLifetimeToken,
    ) : CaType

    private class TestSignature(
        private val renderedText: String,
        override val token: CaLifetimeToken,
    ) : CaSignature<Nothing> {
        override val symbol: Nothing
            get() = error("渲染测试不依赖底层 symbol")
        override val typeParameters: List<Name> = emptyList()
        override val valueParameters: List<CaValueParameterSignature> = emptyList()
        override val returnType: CaType? = null
        override val receiverType: CaType? = null
        override val annotations: List<CaAnnotation> = emptyList()

        override fun substitute(substitutor: CaSubstitutor): CaSignature<Nothing> = this

        override fun toString(): String = renderedText
    }
}
