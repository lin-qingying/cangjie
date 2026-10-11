package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.annotations.BuiltInAnnotationRegistry
import org.cangnova.cangjie.name.FqName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * 「偏移 → 展开面」只读定位回归（变更 `macro-expansion-source-mapping` 的 2.2 / 2.3）。
 *
 * 固定四类情形与一条取舍规则：
 * - **嵌套**宏取最内层；
 * - **跨行**调用按偏移定位（不按行）；
 * - **无展开**位置与「落在展开面内」可区分；
 * - **不可靠**展开由父边通道表达（[MacroPayloadChannel.UNRESOLVED]），不是靠启发式猜；
 * - 同一偏移命中多个互不嵌套的展开面时报**歧义**，不按表面顺序任取一个。
 */
class MacroSurfaceLookupTest {
    /** 嵌套宏：同一偏移同时落在外层与内层区间内时取最内层。 */
    @Test
    fun `nested surfaces resolve to the innermost`() {
        val outer = surface(id = 1, name = "Outer", start = 0, end = 100)
        val inner = surface(id = 2, name = "Inner", start = 20, end = 30)
        val forest = MacroCallForestBuilder.build(listOf(outer, inner))

        assertEquals(
            "Inner",
            forest.innermostNameAt(25),
            "20..30 同时落在外层与内层区间内，必须取最内层",
        )
        assertEquals("Outer", forest.innermostNameAt(50), "内层区间之外仍是外层调用点")
    }

    /** 区间端点属于该调用点：补全常发生在首字符之前、尾字符之后。 */
    @Test
    fun `boundary offsets belong to the call`() {
        val outer = surface(id = 1, name = "Outer", start = 0, end = 100)
        val inner = surface(id = 2, name = "Inner", start = 20, end = 30)
        val forest = MacroCallForestBuilder.build(listOf(outer, inner))

        assertEquals("Inner", forest.innermostNameAt(20))
        assertEquals("Inner", forest.innermostNameAt(30))
        assertEquals("Outer", forest.innermostNameAt(0))
        assertEquals("Outer", forest.innermostNameAt(100))
    }

    /** 跨行调用按偏移定位：区间覆盖多行时，行中间与行尾都必须落在同一展开面内。 */
    @Test
    fun `multi line call is located by offset`() {
        val multiLine = surface(id = 1, name = "MultiLine", start = 10, end = 200)
        val forest = MacroCallForestBuilder.build(listOf(multiLine))

        assertEquals("MultiLine", forest.innermostNameAt(10))
        assertEquals("MultiLine", forest.innermostNameAt(120))
        assertEquals("MultiLine", forest.innermostNameAt(200))
    }

    /** 不在任何调用区间内的偏移返回「无展开」，与「落在不可靠展开内」是两回事。 */
    @Test
    fun `offset outside every call reports no surface`() {
        val forest = MacroCallForestBuilder.build(listOf(surface(id = 1, name = "Only", start = 10, end = 20)))

        assertEquals(
            MacroSurfaceLookupResult.NoSurface,
            forest.lookupSurfaceAt(9),
            "区间之前不属于该调用点",
        )
        assertEquals(
            MacroSurfaceLookupResult.NoSurface,
            forest.lookupSurfaceAt(21),
            "区间之后不属于该调用点",
        )
    }

    /**
     * 互不嵌套的重叠区间报歧义。
     *
     * 两个 surface 都不是对方的祖先时无法判定最内层；按表面顺序任取一个会把
     * 另一个展开面的区间当成「当前位置的展开」。
     */
    @Test
    fun `overlapping but incomparable ranges are ambiguous`() {
        val left = surface(id = 1, name = "Left", start = 0, end = 50)
        val right = surface(id = 2, name = "Right", start = 40, end = 60)
        val forest = MacroCallForestBuilder.build(listOf(left, right))

        val result = forest.lookupSurfaceAt(45)
        val ambiguous = result as? MacroSurfaceLookupResult.Ambiguous
            ?: error("重叠但不嵌套时必须报歧义，实际是 $result")
        assertEquals(
            setOf("Left", "Right"),
            ambiguous.nodes.map { it.surface.qualifiedName!!.shortName().asString() }.toSet(),
            "歧义结果必须如实给出全部候选，供调用方降级为「不可靠」",
        )
    }

    /** 没有 sourceRange 的 surface 不参与定位：没有可比较区间，参与只会制造假歧义。 */
    @Test
    fun `surfaces without a source range do not participate`() {
        val ranged = surface(id = 1, name = "Ranged", start = 10, end = 20)
        val noRange = surface(id = 2, name = "NoRange", start = null, end = null)
        val forest = MacroCallForestBuilder.build(listOf(ranged, noRange))

        assertEquals("Ranged", forest.innermostNameAt(15))
        assertEquals(MacroSurfaceLookupResult.NoSurface, forest.lookupSurfaceAt(15 + 10))
    }

    /** 子节点落在父节点的哪条 payload 通道由父边给出；通道缺失按「不可靠」处理。 */
    @Test
    fun `child payload channel comes from the parent edge`() {
        val inputChild = surface(id = 2, name = "InputChild", start = 20, end = 30)
        val parent = surface(
            id = 1,
            name = "Parent",
            start = 0,
            end = 100,
            inputTokens = listOf(token("@InputChild", 20, 30)),
        )
        val forest = MacroCallForestBuilder.build(listOf(parent, inputChild))

        val child = (forest.lookupSurfaceAt(25) as MacroSurfaceLookupResult.Innermost).node
        assertEquals(MacroPayloadChannel.INPUT, child.incomingChannel(), "落在 input token 覆盖内的子节点通道是 INPUT")
        assertNull(forest.roots.single().incomingChannel(), "root 没有父边，也就没有通道问题")
    }

    /**
     * 子节点不在任何 payload token 覆盖内时按 `UNRESOLVED` 如实上报。
     *
     * 消费方据此把该展开面判为「不可靠」，不得按启发式当作可靠。
     */
    @Test
    fun `uncovered child reports unresolved channel`() {
        // 父节点没有任何 token 覆盖：子节点区间的通道无法从覆盖关系判定。
        val child = surface(id = 2, name = "Child", start = 20, end = 30)
        val parent = surface(id = 1, name = "ParentWithoutTokens", start = 0, end = 100)
        val forest = MacroCallForestBuilder.build(listOf(parent, child))

        assertEquals(
            MacroPayloadChannel.UNRESOLVED,
            (forest.lookupSurfaceAt(25) as MacroSurfaceLookupResult.Innermost).node.incomingChannel(),
            "token 覆盖无法判定通道时必须如实报 UNRESOLVED",
        )
    }

    /** 定位到的最内层展开面名；不是唯一最内层时让断言直接失败。 */
    private fun MacroCallForest.innermostNameAt(offset: Int): String =
        when (val result = lookupSurfaceAt(offset)) {
            is MacroSurfaceLookupResult.Innermost -> result.node.surface.qualifiedName!!.shortName().asString()
            else -> error("偏移 $offset 未定位到唯一最内层展开面：$result")
        }

    /** 构造一个只带源码范围的测试 surface。 */
    private fun surface(
        id: Long,
        name: String,
        start: Int?,
        end: Int?,
        inputTokens: List<MacroSurfaceToken> = emptyList(),
        attrTokens: List<MacroSurfaceToken> = emptyList(),
    ): MacroSurface = MacroSurfaceNode(
        surfaceId = id,
        qualifiedName = FqName(name),
        isQualifiedName = false,
        kind = MacroSurface.Kind.PLAIN,
        hasParenthesis = false,
        attrTokens = attrTokens,
        inputTokens = inputTokens,
        sourceRange = if (start == null || end == null) {
            null
        } else {
            MacroSurfaceSourceRange(source = null, startOffset = start, endOffset = end)
        },
        scopeContext = MacroSurfaceScopeContext(
            packageFqName = FqName("test"),
            sourceModuleName = BuiltInAnnotationRegistry.sourceModuleName(FqName("test")),
            enclosingClassFqName = null,
            enclosingFunctionName = null,
        ),
        modifiers = emptyList(),
        carriedAnnotations = emptyList(),
        capturedRawSyntax = null,
        containerContext = MacroSurfaceContainerContext(
            outerDeclarationKind = MacroSurfaceContainerContext.OuterDeclarationKind.TOP_LEVEL,
            isInsidePrimaryConstructor = false,
            isInsideEnumBody = false,
            isInsideBlock = false,
        ),
        replaceHandle = CfirReplaceHandle(id, null),
    )

    /** 构造指定源码范围的测试 token。 */
    private fun token(text: String, startOffset: Int, endOffset: Int): MacroSurfaceToken = MacroSurfaceToken(
        text = text,
        startOffset = startOffset,
        endOffset = endOffset,
        kindName = "TEST",
    )
}
