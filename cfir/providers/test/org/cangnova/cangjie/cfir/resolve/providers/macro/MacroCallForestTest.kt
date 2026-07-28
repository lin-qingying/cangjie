package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.name.FqName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

/**
 * 验证宏调用森林构建和求值顺序，确保嵌套宏按子节点优先的构造流程执行。
 */
class MacroCallForestTest {
    /**
     * 验证根节点按照源码范围和 surfaceId 形成稳定顺序。
     */
    @Test
    fun `roots are sorted deterministically by source range and surface id`() {
        val later = surface(id = 30, name = "Later", start = 40, end = 50)
        val firstLowerId = surface(id = 10, name = "FirstLowerId", start = 10, end = 15)
        val noRangeLowerId = surface(id = 1, name = "NoRangeLowerId", start = null, end = null)
        val noRangeHigherId = surface(id = 40, name = "NoRangeHigherId", start = null, end = null)

        val forest = MacroCallForestBuilder.build(
            listOf(later, noRangeHigherId, firstLowerId, noRangeLowerId),
        )

        assertEquals(
            listOf("FirstLowerId", "Later", "NoRangeLowerId", "NoRangeHigherId"),
            forest.roots.map { it.surface.qualifiedName!!.shortName().asString() },
        )
    }

    /**
     * 验证求值器先展开嵌套子节点，并把直接子节点结果传递给父节点。
     */
    @Test
    fun `evaluator visits nested forest child first and passes direct child results to parent`() {
        val outer = surface(id = 1, name = "Outer", start = 0, end = 100)
        val middle = surface(id = 2, name = "Middle", start = 10, end = 80)
        val inner = surface(id = 3, name = "Inner", start = 20, end = 30)
        val forest = MacroCallForestBuilder.build(listOf(outer, inner, middle))
        val visited = mutableListOf<String>()
        val childResultSizes = linkedMapOf<String, Int>()

        val results = MacroForestEvaluator().evaluate(
            forest = forest,
            expand = { node, childResults ->
                val name = node.surface.qualifiedName!!.shortName().asString()
                visited += name
                childResultSizes[name] = childResults.size
                listOf(token("${name.lowercase()}Result"))
            },
        )

        assertEquals(listOf("Inner", "Middle", "Outer"), visited)
        assertEquals(mapOf("Inner" to 0, "Middle" to 1, "Outer" to 1), childResultSizes)
        assertEquals(listOf("innerResult", "middleResult", "outerResult"), results.values.flatten().map { it.text })
    }

    /**
     * 验证父节点通过输入 token 与属性 token 覆盖范围记录子节点 payload 通道。
     */
    @Test
    fun `forest records child payload channel from parent token coverage`() {
        val inputChild = surface(id = 2, name = "InputChild", start = 20, end = 30)
        val attrChild = surface(id = 3, name = "AttrChild", start = 50, end = 60)
        val parent = surface(
            id = 1,
            name = "Parent",
            start = 0,
            end = 100,
            inputTokens = listOf(token("@InputChild", 20, 30)),
        ).copy(
            attrTokens = listOf(token("@AttrChild", 50, 60)),
        )

        val forest = MacroCallForestBuilder.build(listOf(parent, attrChild, inputChild))
        val edges = forest.roots.single().childEdges.associate {
            it.child.surface.qualifiedName!!.shortName().asString() to it.channel
        }

        assertEquals(MacroPayloadChannel.INPUT, edges.getValue("InputChild"))
        assertEquals(MacroPayloadChannel.ATTR, edges.getValue("AttrChild"))
    }

    /**
     * 验证直接子节点没有展开结果时父节点不会继续执行。
     */
    @Test
    fun `evaluator skips parent when a direct child has no expansion result`() {
        val outer = surface(id = 1, name = "Outer", start = 0, end = 100)
        val inner = surface(id = 2, name = "Inner", start = 20, end = 30)
        val forest = MacroCallForestBuilder.build(listOf(outer, inner))
        val visited = mutableListOf<String>()

        val results = MacroForestEvaluator().evaluate(
            forest = forest,
            expand = { node, _ ->
                val name = node.surface.qualifiedName!!.shortName().asString()
                visited += name
                if (name == "Inner") null else listOf(token("${name.lowercase()}Result"))
            },
        )

        assertEquals(listOf("Inner"), visited)
        assertEquals(emptyMap<MacroCallNode, List<MacroSurfaceToken>>(), results)
    }

    /**
     * 验证不同源码调用点即使 fingerprint 相同，也会作为独立 sibling 全部展开。
     */
    @Test
    fun `evaluator treats identical sibling fingerprints as independent call sites`() {
        val first = surface(id = 1, name = "Loop", start = 0, end = 10, inputTokens = listOf(token("same")))
        val second = surface(id = 2, name = "Loop", start = 20, end = 30, inputTokens = listOf(token("same")))
        val cycles = mutableListOf<MacroExpansionCycle>()

        val results = MacroForestEvaluator(maxIterations = 1).evaluate(
            forest = MacroCallForestBuilder.build(listOf(first, second)),
            expand = { node, _ -> listOf(token(node.surface.surfaceId.toString())) },
            onCycle = cycles::add,
        )

        assertEquals(emptyList<MacroExpansionCycle>(), cycles)
        assertEquals(listOf(1L, 2L), results.keys.map { it.surface.surfaceId })
        assertEquals(listOf("1", "2"), results.values.flatten().map { it.text })
    }

    /**
     * 验证同一逻辑 surface 在 re-evaluation 中再次进入同一状态时才会报告循环。
     */
    @Test
    fun `cycle detector reports repeated fingerprint within one logical surface`() {
        val surface = surface(id = 1, name = "Loop", start = 0, end = 10, inputTokens = listOf(token("same")))
        val node = MacroCallForestBuilder.build(listOf(surface)).roots.single()
        val detector = MacroExpansionCycleDetector(maxIterations = 1)

        assertEquals(null, detector.observe(node, emptyMap()))
        val cycle = requireNotNull(detector.observe(node, emptyMap()))

        assertEquals("Loop", cycle.fingerprint.qualifiedName)
        assertEquals(listOf(1L, 1L), cycle.nodes.map { it.surface.surfaceId })
        assertSame(node, cycle.nodes.first())
        assertSame(node, cycle.nodes.last())
    }

    /**
     * 构造带源码范围、输入 token 和标准上下文的宏 surface。
     */
    private fun surface(
        id: Long,
        name: String,
        start: Int?,
        end: Int?,
        inputTokens: List<MacroSurfaceToken> = emptyList(),
    ): MacroSurfaceExpr {
        return MacroSurfaceExpr(
            surfaceId = id,
            qualifiedName = FqName(name),
            kind = MacroSurface.Kind.PLAIN,
            hasParenthesis = true,
            attrTokens = emptyList(),
            inputTokens = inputTokens,
            sourceRange = if (start != null && end != null) MacroSurfaceSourceRange(
                source = null,
                startOffset = start,
                endOffset = end,
            ) else null,
            scopeContext = MacroSurfaceScopeContext(
                packageFqName = FqName("test"),
                enclosingClassFqName = null,
                enclosingFunctionName = null,
            ),
            modifiers = emptyList(),
            carriedAnnotations = emptyList(),
            capturedRawSyntax = null,
            containerContext = MacroSurfaceContainerContext(
                outerDeclarationKind = MacroSurfaceContainerContext.OuterDeclarationKind.FUNCTION_BODY,
                isInsidePrimaryConstructor = false,
                isInsideEnumBody = false,
                isInsideBlock = true,
            ),
            replaceHandle = CfirReplaceHandle(id),
        )
    }

    /**
     * 构造覆盖完整文本范围的测试 token。
     */
    private fun token(text: String): MacroSurfaceToken = token(text, 0, text.length)

    /**
     * 构造指定源码范围的测试 token。
     */
    private fun token(text: String, startOffset: Int, endOffset: Int): MacroSurfaceToken = MacroSurfaceToken(
        text = text,
        startOffset = startOffset,
        endOffset = endOffset,
        kindName = "TEST",
    )
}
