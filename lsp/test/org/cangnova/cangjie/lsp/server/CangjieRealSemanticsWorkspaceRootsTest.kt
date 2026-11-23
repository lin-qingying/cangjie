package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.cangnova.cangjie.lsp.state.LspTextDocument
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixture
import org.cangnova.cangjie.lsp.testkit.LspWorkspaceFixtureBuilder
import org.eclipse.lsp4j.DefinitionParams
import org.eclipse.lsp4j.DiagnosticSeverity
import org.eclipse.lsp4j.Location
import org.eclipse.lsp4j.TextDocumentIdentifier
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 多根工作区（两个 `workspace folder`，客户端**不**提供 `multiModuleOption`）。
 *
 * 这是 VS Code「多根工作区」的标准形态：客户端只说「这两个目录是我的工作区」，
 * 模块结构由服务端自己推断。与 `CangjieRealSemanticsMultiModuleTest` 的区别是那份由客户端
 * 显式给出模块定义；这里检验的是服务端的降级推断（每个 folder 视作一个模块）。
 */
class CangjieRealSemanticsWorkspaceRootsTest : AbstractLspIntegrationTest() {
    /**
     * 禁用默认会话：需要两个 workspace folder。
     */
    override val autoCreateDefaultSession: Boolean = false

    /**
     * 每个根目录内的代码必须能解析自己的符号（不得因为分成两个模块而互相干扰）。
     */
    @Test
    fun `each root resolves its own declarations`() {
        twoRootWorkspace().use { fixture ->
            createSession(twoRootParams(fixture)).use { session ->
                val appUri = fixture.uri(APP)
                val libUri = fixture.uri(LIB)
                session.openDocument(appUri, fixture.text(APP))
                session.openDocument(libUri, fixture.text(LIB))

                val libDiagnostics = session.documentDiagnostic(session.documentDiagnosticParams(libUri))
                    .relatedFullDocumentDiagnosticReport.items
                    .filter { it.severity == DiagnosticSeverity.Error }
                assertTrue(
                    libDiagnostics.isEmpty(),
                    "lib 根目录内的代码不应有错误诊断，实际 ${libDiagnostics.map { it.message.left }}",
                )
            }
        }
    }

    /**
     * 两个根之间**没有**声明依赖时，跨根 import 必须如实报错，而不是假装能解析。
     *
     * 这条是上一条的对照：多根不等于「所有根共享一个符号空间」。Cangjie 的跨模块引用需要
     * 显式依赖（`requires` / cjpm 依赖），客户端只丢两个目录过来时，服务端没有依据建立边——
     * 此时**报 `unresolved import` 是正确行为**，静默解析才是缺陷（用户会在构建时才炸）。
     *
     * 同时要求：解析不到时 `definition` 返回空列表，而不是跳到某个同名的错误目标。
     */
    @Test
    fun `cross root import without a declared dependency is reported as unresolved`() {
        twoRootWorkspace().use { fixture ->
            createSession(twoRootParams(fixture)).use { session ->
                val appUri = fixture.uri(APP)
                val libUri = fixture.uri(LIB)
                session.openDocument(appUri, fixture.text(APP))
                session.openDocument(libUri, fixture.text(LIB))

                val errors = session.documentDiagnostic(session.documentDiagnosticParams(appUri))
                    .relatedFullDocumentDiagnosticReport.items
                    .filter { it.severity == DiagnosticSeverity.Error }
                assertTrue(
                    errors.isNotEmpty(),
                    "未声明依赖的跨根 import 必须报错，实际无错误诊断——" +
                        "这会让用户在编辑器里看着正常、构建时才失败",
                )

                val call = positionOf(fixture, APP, "helper()")
                val definition = session.definition(DefinitionParams(TextDocumentIdentifier(appUri), call))
                    .let { if (it.isLeft) it.left.orEmpty() else emptyList<Location>() }
                assertTrue(
                    definition.none { it.uri == libUri },
                    "未声明依赖时不得跨根跳到另一模块的声明（会掩盖缺失的依赖配置），实际 $definition",
                )
            }
        }
    }

    // ---------------------------------------------------------------- 夹具

    private companion object {
        const val APP: String = "folderA/src/main.cj"
        const val LIB: String = "folderB/src/helper.cj"
    }

    /**
     * 两个根目录，各有独立源码树；文件内容来自 `testData/multiModule/`。
     */
    private fun twoRootWorkspace(): LspWorkspaceFixture = LspWorkspaceFixtureBuilder()
        .fileFromTestData(LIB, "multiModule/lib/helper.cj")
        .fileFromTestData(APP, "multiModule/app/main.cj")
        .build()

    /**
     * 只给两个 workspace folder，不给 `multiModuleOption`——服务端必须自行把每个根降级成一个模块。
     */
    private fun twoRootParams(fixture: LspWorkspaceFixture) = fixture.initializeParams(
        workspaceFolders = listOf(fixture.workspaceFolder("folderA"), fixture.workspaceFolder("folderB")),
        includeMultiModuleOption = false,
    )

    // ---------------------------------------------------------------- 断言辅助

    /** token 起始位置。 */
    private fun positionOf(fixture: LspWorkspaceFixture, relativePath: String, token: String) =
        run {
            val text = fixture.text(relativePath)
            val offset = text.indexOf(token)
            check(offset >= 0) { "在 $relativePath 中找不到 `$token`" }
            LspTextDocument(fixture.uri(relativePath), "cangjie", 1, text).positionAt(offset)
        }
}
