package org.cangnova.cangjie.lsp.server

import org.cangnova.cangjie.lsp.framework.AbstractLspIntegrationTest
import org.eclipse.lsp4j.Diagnostic
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.WorkspaceFolder
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.invariantSeparatorsPathString
import kotlin.io.path.isDirectory
import kotlin.io.path.writeText

class CangjieWorkspaceOverlayIntegrationTest : AbstractLspIntegrationTest() {
    override val autoCreateDefaultSession: Boolean = false

    @Test
    fun `workspace source module resolves disk dependency without splitting opened file into dangling module`() {
        withWorkspaceAndStdlib(
            mainText = """
                package untitled89

                import untitled89.a.*

                func b(): Int64 {
                    return a()
                }
            """.trimIndent(),
            dependencyText = """
                package untitled89.a

                public func a(): Int64 {
                    return 1
                }
            """.trimIndent(),
        ) { workspaceRoot, stdlibRoot ->
            createSession(buildInitializeParams(workspaceRoot, stdlibRoot)).use { testSession ->
                val mainUri = (workspaceRoot / "src" / "main.cj").toUri().toString()
                testSession.clearPublishedDiagnostics()
                testSession.openDocument(mainUri, Files.readString(workspaceRoot / "src" / "main.cj"))
                testSession.awaitDiagnosticsCount(1)

                val diagnostics = diagnosticsFor(testSession, mainUri)
                assertTrue(
                    diagnostics.isEmpty(),
                    "工作区源码依赖应当通过同一 source module 解析。actual=${diagnostics.map(::renderDiagnostic)}",
                )
            }
        }
    }

    @Test
    fun `overlay dependency updates and close fallback refresh diagnostics for other opened workspace files`() {
        withWorkspaceAndStdlib(
            mainText = """
                package untitled89

                import untitled89.a.*

                func b(): Int64 {
                    return a()
                }
            """.trimIndent(),
            dependencyText = """
                package untitled89.a

                public func a(): Int64 {
                    return 1
                }
            """.trimIndent(),
        ) { workspaceRoot, stdlibRoot ->
            createSession(buildInitializeParams(workspaceRoot, stdlibRoot)).use { testSession ->
                val mainUri = (workspaceRoot / "src" / "main.cj").toUri().toString()
                val dependencyUri = (workspaceRoot / "src" / "a" / "a.cj").toUri().toString()

                testSession.openDocument(mainUri, Files.readString(workspaceRoot / "src" / "main.cj"))
                testSession.awaitDiagnosticsCount(1)

                testSession.clearPublishedDiagnostics()
                testSession.openDocument(dependencyUri, Files.readString(workspaceRoot / "src" / "a" / "a.cj"))
                testSession.awaitDiagnosticsCount(2)
                assertTrue(
                    diagnosticsFor(testSession, mainUri).isEmpty(),
                    "打开依赖文件后主文件不应退化为 unresolved。actual=${diagnosticsFor(testSession, mainUri).map(::renderDiagnostic)}",
                )
                assertTrue(
                    diagnosticsFor(testSession, dependencyUri).isEmpty(),
                    "overlay 依赖文件本身不应产生无关诊断。actual=${diagnosticsFor(testSession, dependencyUri).map(::renderDiagnostic)}",
                )

                testSession.clearPublishedDiagnostics()
                testSession.changeDocument(
                    uri = dependencyUri,
                    version = 2,
                    newText = """
                        package untitled89.a

                        public func renamed(): Int64 {
                            return 1
                        }
                    """.trimIndent(),
                )
                testSession.awaitDiagnosticsCount(2)
                assertTrue(
                    diagnosticsFor(testSession, mainUri).any { diagnostic ->
                        diagnostic.message.left?.contains("Unresolved reference") == true
                    },
                    "依赖 overlay 变更后，主文件诊断必须随同刷新。actual=${diagnosticsFor(testSession, mainUri).map(::renderDiagnostic)}",
                )

                testSession.clearPublishedDiagnostics()
                testSession.closeDocument(dependencyUri)
                testSession.awaitDiagnosticsCount(2)
                assertTrue(
                    diagnosticsFor(testSession, dependencyUri).isEmpty(),
                    "关闭 overlay 文件后，客户端必须先收到该文件的清空诊断。actual=${diagnosticsFor(testSession, dependencyUri).map(::renderDiagnostic)}",
                )
                assertTrue(
                    diagnosticsFor(testSession, mainUri).isEmpty(),
                    "关闭 overlay 后应回退到磁盘 PSI，主文件诊断必须恢复为空。actual=${diagnosticsFor(testSession, mainUri).map(::renderDiagnostic)}",
                )
            }
        }
    }

    @Test
    fun `stdlib default imports resolve String through configured cjo search path`() {
        withWorkspaceAndStdlib(
            mainText = """
                package untitled89

                func echo(value: String): String {
                    return value
                }
            """.trimIndent(),
            dependencyText = """
                package untitled89.a

                public func a(): Int64 {
                    return 1
                }
            """.trimIndent(),
        ) { workspaceRoot, stdlibRoot ->
            createSession(buildInitializeParams(workspaceRoot, stdlibRoot)).use { testSession ->
                val mainUri = (workspaceRoot / "src" / "main.cj").toUri().toString()
                testSession.clearPublishedDiagnostics()
                testSession.openDocument(mainUri, Files.readString(workspaceRoot / "src" / "main.cj"))
                testSession.awaitDiagnosticsCount(1)

                val diagnostics = diagnosticsFor(testSession, mainUri)
                assertTrue(
                    diagnostics.isEmpty(),
                    "std.core default import should resolve String. actual=${diagnostics.map(::renderDiagnostic)}, stdlibRoot=$stdlibRoot, property=${System.getProperty("cangjie.stdlib.module")}",
                )
            }
        }
    }

    /**
     * 构造真实工作区与标准库输入，验证 overlay 与 stdlib 注入链路。
     */
    private fun withWorkspaceAndStdlib(
        mainText: String,
        dependencyText: String,
        action: (workspaceRoot: Path, stdlibRoot: Path) -> Unit,
    ) {
        val workspaceRoot = Files.createTempDirectory("cangjie-lsp-workspace-")
        val fixtureStdlibRoot = Files.createTempDirectory("cangjie-lsp-stdlib-")
        try {
            writeWorkspaceFile(workspaceRoot / "src" / "main.cj", mainText)
            writeWorkspaceFile(workspaceRoot / "src" / "a" / "a.cj", dependencyText)
            (workspaceRoot / "target").createDirectories()
            (workspaceRoot / ".cache" / "lsp").createDirectories()
            copyStdlibFixtures(fixtureStdlibRoot)
            val stdlibRoot = resolveStdlibRoot(fixtureStdlibRoot)
            action(workspaceRoot, stdlibRoot)
        } finally {
            workspaceRoot.toFile().deleteRecursively()
            fixtureStdlibRoot.toFile().deleteRecursively()
        }
    }

    private fun buildInitializeParams(
        workspaceRoot: Path,
        stdlibRoot: Path,
    ): InitializeParams {
        val workspaceUri = workspaceRoot.toUri().toString()
        val sourceRootUri = (workspaceRoot / "src").toUri().toString()
        val outputUri = (workspaceRoot / "target").toUri().toString()
        val workspaceName = workspaceRoot.fileName.toString()

        return InitializeParams().apply {
            this.rootUri = workspaceUri
            workspaceFolders = listOf(WorkspaceFolder(workspaceUri, workspaceName))
            initializationOptions = mapOf(
                "stdLibPathOption" to stdlibRoot.toString(),
                "targetLib" to (workspaceRoot / ".cache" / "lsp").toString(),
                "multiModuleOption" to mapOf(
                    workspaceUri to mapOf(
                        "name" to workspaceName,
                        "package_requires" to mapOf(
                            "path_option" to emptyList<String>(),
                            "package_option" to emptyMap<String, String>(),
                        ),
                        "source_sets" to mapOf(
                            "main" to mapOf(
                                "source_roots" to listOf(sourceRootUri),
                                "resource_roots" to emptyList<String>(),
                                "output_directory" to listOf(outputUri),
                                "is_test" to false,
                            ),
                        ),
                    ),
                ),
            )
        }
    }

    private fun diagnosticsFor(
        session: org.cangnova.cangjie.lsp.framework.LspIntegrationTestSession,
        documentUri: String,
    ): List<Diagnostic> {
        return session.publishedDiagnostics()
            .lastOrNull { published -> published.uri == documentUri }
            ?.diagnostics
            .orEmpty()
    }

    private fun writeWorkspaceFile(path: Path, text: String) {
        path.parent?.createDirectories()
        path.writeText(text)
    }

    private fun renderDiagnostic(diagnostic: Diagnostic): String {
        val message = diagnostic.message.left ?: diagnostic.message.right ?: "<no-message>"
        return "${diagnostic.code}:${message}"
    }

    private fun copyStdlibFixtures(stdlibRoot: Path) {
        val repoRoot = locateRepositoryRoot()
        val stdFixtureRoot = repoRoot /
            "cfir" /
            "cfir-serialization" /
            "testResources" /
            "cjo-sdk" /
            "windows_x86_64_cjnative"

        copyFile(stdFixtureRoot / "std.cjo", stdlibRoot / "std.cjo")
        copyFile(stdFixtureRoot / "std" / "std.core.cjo", stdlibRoot / "std" / "std.core.cjo")
    }

    private fun resolveStdlibRoot(fallbackStdlibRoot: Path): Path {
        val userHome = System.getProperty("user.home")?.let(Path::of) ?: return fallbackStdlibRoot
        val sdkRoot = userHome / ".cangjie" / "sdks"
        if (!sdkRoot.exists() || !sdkRoot.isDirectory()) {
            return fallbackStdlibRoot
        }

        val installedSdk = Files.walk(sdkRoot).use { stream ->
            stream
                .filter { path -> path.fileName?.toString() == "std.core.cjo" }
                .map(Path::normalize)
                .toList()
                .sortedWith(
                    compareByDescending<Path> { path ->
                        path.getParent()?.getParent()?.fileName?.toString() == "windows_x86_64_llvm"
                    }
                        .thenByDescending { path -> parseSdkVersion(path).major }
                        .thenByDescending { path -> parseSdkVersion(path).minor }
                        .thenByDescending { path -> parseSdkVersion(path).patch },
                )
                .firstOrNull()
        } ?: return fallbackStdlibRoot

        return installedSdk.getParent()?.getParent() ?: fallbackStdlibRoot
    }

    private fun parseSdkVersion(stdCorePath: Path): SdkVersion {
        val sdkDirName = stdCorePath
            .getParent()
            ?.getParent()
            ?.getParent()
            ?.getParent()
            ?.fileName
            ?.toString()
            .orEmpty()
        val rawVersion = sdkDirName.removePrefix("cangjie-")
        val parts = rawVersion.split('.')
        return SdkVersion(
            major = parts.getOrNull(0)?.toIntOrNull() ?: 0,
            minor = parts.getOrNull(1)?.toIntOrNull() ?: 0,
            patch = parts.getOrNull(2)?.toIntOrNull() ?: 0,
        )
    }

    private fun copyFile(source: Path, target: Path) {
        target.parent?.createDirectories()
        Files.copy(source, target)
    }

    private fun locateRepositoryRoot(): Path {
        var current = Path.of("").toAbsolutePath()
        while (current != current.root) {
            if ((current / "settings.gradle.kts").exists()) {
                return current
            }
            current = current.parent
        }
        error("Cannot locate repository root from ${Path.of("").toAbsolutePath().invariantSeparatorsPathString}")
    }

    private data class SdkVersion(
        val major: Int,
        val minor: Int,
        val patch: Int,
    )
}
