package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixture
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.eclipse.lsp4j.DefinitionParams
import org.eclipse.lsp4j.DocumentSymbol
import org.eclipse.lsp4j.DocumentSymbolParams
import org.eclipse.lsp4j.HoverParams
import org.eclipse.lsp4j.Position
import org.eclipse.lsp4j.SymbolKind
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.eclipse.lsp4j.jsonrpc.messages.Either
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 真实语义下的**层级结构与负向结果**。
 *
 * 正向用例只能证明「有结果」，这里补两类客户端真正依赖的行为：
 * 1. `documentSymbol` 的父子层级（大纲视图按它缩进）；
 * 2. 空白位置的 hover / definition 必须为空，不能返回上一次的缓存或兜底内容。
 */
class CangjieRealSemanticsStructureTest : AbstractLspIntegrationTest() {
    /**
     * 禁用默认会话：需要带源码的临时工作区。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 成员必须作为类型的子符号返回，而不是全部平铺在顶层。
     *
     * 大纲视图、面包屑、结构化搜索都按 `children` 组织；平铺会让成员看起来像顶层声明。
     */
    @Test
    fun `document symbols nest members under their type`() {
        structureWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri("src/structure.cj")
                session.openDocument(uri, fixture.text("src/structure.cj"))

                val symbols = session.documentSymbol(DocumentSymbolParams(TextDocumentIdentifier(uri)))
                val square = findSymbol(symbols, "Square")
                assertNotNull(square, "必须包含 Square 顶层符号，实际 ${flattenNames(symbols)}")
                assertTrue(
                    square!!.children.orEmpty().any { it.name == "area" },
                    "成员 area 必须是 Square 的子符号；实际顶层=${flattenNames(symbols)}，" +
                        "Square.children=${square.children.orEmpty().map { it.name }}",
                )
                assertTrue(
                    square.children.orEmpty().any { it.name == "side" },
                    "字段 side 必须是 Square 的子符号；实际 Square.children=" +
                        "${square.children.orEmpty().map { it.name }}",
                )

                val interfaceShape = findSymbol(symbols, "Shape")
                assertNotNull(interfaceShape, "必须包含 Shape 顶层符号")
                assertTrue(
                    interfaceShape!!.children.orEmpty().any { it.name == "area" },
                    "接口方法 area 必须是 Shape 的子符号；实际 Shape.children=" +
                        "${interfaceShape.children.orEmpty().map { it.name }}",
                )
                assertTrue(
                    square.children.orEmpty().none { it.kind == SymbolKind.File },
                    "子符号不应把文件节点混进成员列表",
                )
            }
        }
    }

    /**
     * 空白位置上的 hover 必须为空，不能返回任何兜底内容。
     */
    @Test
    fun `hover on a blank position returns nothing`() {
        structureWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri("src/structure.cj")
                val text = fixture.text("src/structure.cj")
                session.openDocument(uri, text)

                val blankLine = blankLineIndex(text)
                val hover = session.hover(HoverParams(TextDocumentIdentifier(uri), Position(blankLine, 0)))

                assertTrue(
                    isEmptyHover(hover),
                    "空白行的 hover 必须为空，实际：$hover",
                )
            }
        }
    }

    /**
     * 空白位置上的 definition 必须为空列表。
     */
    @Test
    fun `definition on a blank position returns no locations`() {
        structureWorkspace().use { fixture ->
            createSession(fixture.initializeParams()).use { session ->
                val uri = fixture.uri("src/structure.cj")
                val text = fixture.text("src/structure.cj")
                session.openDocument(uri, text)

                val blankLine = blankLineIndex(text)
                val locations = session.definition(
                    DefinitionParams(TextDocumentIdentifier(uri), Position(blankLine, 0)),
                )

                assertTrue(
                    locations.isLeft && locations.left.orEmpty().isEmpty(),
                    "空白行的 definition 必须为空，实际：$locations",
                )
            }
        }
    }

    // ---------------------------------------------------------------- 夹具

    /**
     * 带接口、实现类、字段与方法的单文件工作区（源码见 `testData/realSemantics/structure.cj`）。
     */
    private fun structureWorkspace(): LspWorkspaceFixture = LspWorkspaceFixtureBuilder()
        .sourceFromTestData("structure.cj", "realSemantics/structure.cj")
        .build()

    // ---------------------------------------------------------- 断言辅助

    /**
     * 递归查找同名符号。
     */
    private fun findSymbol(
        symbols: List<Either<org.eclipse.lsp4j.SymbolInformation, DocumentSymbol>>,
        name: String,
    ): DocumentSymbol? {
        symbols.forEach { symbol ->
            if (symbol.isRight) {
                findSymbol(symbol.right, name)?.let { return it }
            }
        }
        return null
    }

    /**
     * 在符号树里递归查找。
     */
    private fun findSymbol(symbol: DocumentSymbol?, name: String): DocumentSymbol? {
        if (symbol == null) return null
        if (symbol.name == name) return symbol
        symbol.children.orEmpty().forEach { child -> findSymbol(child, name)?.let { return it } }
        return null
    }

    /**
     * 取符号树的全部名字（含子符号），用于失败诊断。
     */
    private fun flattenNames(
        symbols: List<Either<org.eclipse.lsp4j.SymbolInformation, DocumentSymbol>>,
    ): List<String> {
        val names = mutableListOf<String>()
        fun collect(symbol: DocumentSymbol?) {
            if (symbol == null) return
            names += symbol.name
            symbol.children.orEmpty().forEach(::collect)
        }
        symbols.forEach { if (it.isRight) collect(it.right) else names += it.left?.name.orEmpty() }
        return names
    }

    /**
     * hover 是否为空（无内容）。
     */
    private fun isEmptyHover(hover: org.eclipse.lsp4j.Hover): Boolean = when {
        hover.contents == null -> true
        hover.contents.isLeft -> hover.contents.left.orEmpty().isEmpty()
        else -> hover.contents.right?.value.isNullOrBlank()
    }

    /**
     * 找到文本中第一处空白行（内容为空白字符）的行号。
     */
    private fun blankLineIndex(text: String): Int {
        val index = text.lineSequence().indexOfFirst { it.isBlank() }
        check(index >= 0) { "夹具必须包含空行" }
        return index
    }
}
