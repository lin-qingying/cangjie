package org.cangnova.cangjie.lsp.state

import java.io.File
import java.nio.file.Files
import kotlin.random.Random
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.WorkspaceFolder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * 工程配置解析的性质与边界测试。
 *
 * `initializationOptions` 来自任意客户端，形态不可控：这里用固定种子随机出嵌套
 * map/list/数字/字符串，要求解析**不抛错、结果确定、去重**；再单独固定
 * URI（含空格与非 ASCII）与系统属性这两条会影响后续语义的路径。
 */
class LspProjectConfigurationPropertyTest {
    /**
     * 任意形态的初始化选项都不得让解析抛错，且同一输入必须得到同一结果。
     */
    @Test
    fun `random initialization options parse deterministically`() {
        val random = Random(SEED)
        repeat(CASES) { case ->
            val options = randomOptions(random)
            val params = InitializeParams().apply {
                rootUri = "file:///workspace"
                initializationOptions = options
            }

            val first = LspProjectConfiguration.fromInitializeParams(params)
            val second = LspProjectConfiguration.fromInitializeParams(params)

            assertEquals(first, second, "case=$case 同一输入必须得到同一配置")
            assertEquals(
                first.stdlibSearchPaths,
                first.stdlibSearchPaths.distinct(),
                "case=$case 标准库路径必须去重",
            )
            assertEquals(
                first.librarySearchPaths,
                first.librarySearchPaths.distinct(),
                "case=$case 库搜索路径必须去重",
            )
            assertTrue(
                first.workspaceModules.all { it.sourceRootUris.isNotEmpty() },
                "case=$case 模块必须至少有一个源码根",
            )
            assertTrue(
                first.workspaceModules.all { it.name.isNotBlank() },
                "case=$case 模块名不得为空",
            )
        }
    }

    /**
     * multiModuleOption 的完整形态必须被如实翻译成模块定义（含 requires 与 package 路径）。
     */
    @Test
    fun `multi module option is translated into module definitions`() {
        val params = InitializeParams().apply {
            rootUri = "file:///workspace"
            initializationOptions = mapOf(
                "targetLib" to "file:///cache",
                "stdLibPathOption" to "/sdk/stdlib",
                "showDiagnosticFactoryNames" to true,
                "multiModuleOption" to mapOf(
                    "file:///workspace/app" to mapOf(
                        "name" to "app",
                        "requires" to mapOf("lib" to mapOf("path" to "file:///workspace/lib")),
                        "package_requires" to mapOf(
                            "path_option" to listOf("/packages/a", "/packages/b", "/packages/a"),
                        ),
                        "source_sets" to mapOf(
                            "main" to mapOf(
                                "source_roots" to listOf("file:///workspace/app/src"),
                            ),
                        ),
                    ),
                    "file:///workspace/lib" to mapOf(
                        "name" to "lib",
                        "source_sets" to mapOf(
                            "main" to mapOf("source_roots" to listOf("file:///workspace/lib/src")),
                        ),
                    ),
                ),
            )
        }

        val configuration = LspProjectConfiguration.fromInitializeParams(params)

        assertEquals(listOf("/sdk/stdlib"), configuration.stdlibSearchPaths)
        assertEquals(
            listOf("file:///cache", "/packages/a", "/packages/b"),
            configuration.librarySearchPaths,
            "targetLib 与模块 package 路径必须合并去重且保持首次出现顺序",
        )
        assertTrue(configuration.showDiagnosticFactoryNames)

        val app = configuration.workspaceModules.single { it.name == "app" }
        assertEquals(listOf("file:///workspace/app/src"), app.sourceRootUris)
        assertEquals(listOf("lib"), app.requiredModuleNames)
        assertEquals(listOf("/packages/a", "/packages/b"), app.packageSearchPaths)
        assertEquals("lib", configuration.workspaceModules.single { it.name == "lib" }.name)
    }

    /**
     * 未声明 multiModuleOption 时，每个 workspace folder 退化为一个模块。
     */
    @Test
    fun `workspace folders degrade to modules when multi module option is absent`() {
        val params = InitializeParams().apply {
            workspaceFolders = listOf(
                WorkspaceFolder("file:///workspace/one", "one"),
                WorkspaceFolder("file:///workspace/two", ""),
            )
        }

        val configuration = LspProjectConfiguration.fromInitializeParams(params)

        assertEquals(listOf("one", "two"), configuration.workspaceModules.map { it.name })
        assertEquals(
            listOf(listOf("file:///workspace/one"), listOf("file:///workspace/two")),
            configuration.workspaceModules.map { it.sourceRootUris },
        )
    }

    /**
     * 目录覆盖参数必须优先于 initialize 参数里的目录列表。
     */
    @Test
    fun `workspace folder override takes precedence`() {
        val params = InitializeParams().apply {
            workspaceFolders = listOf(WorkspaceFolder("file:///workspace/original", "original"))
        }

        val configuration = LspProjectConfiguration.fromInitializeParams(
            params = params,
            workspaceFoldersOverride = listOf(WorkspaceFolder("file:///workspace/override", "override")),
        )

        assertEquals(listOf("override"), configuration.workspaceModules.map { it.name })
    }

    /**
     * 含空格与非 ASCII 的目录 URI 必须原样保留（百分号编码不做二次加工）。
     */
    @Test
    fun `uri with spaces and non ascii characters is preserved verbatim`() {
        val directory = Files.createTempDirectory("仓颉 lsp 工作区-")
        try {
            val uri = directory.toUri().toString()
            assertTrue(uri.contains("%20"), "测试前提：toUri 会把空格编码成 %20")
            assertTrue(uri.none { it == ' ' }, "测试前提：URI 里不应残留原始空格")

            val configuration = LspProjectConfiguration.fromInitializeParams(
                InitializeParams().apply {
                    workspaceFolders = listOf(WorkspaceFolder(uri, "工作区"))
                },
            )

            assertEquals(listOf(uri), configuration.workspaceModules.single().sourceRootUris)
            assertEquals("工作区", configuration.workspaceModules.single().name)
        } finally {
            directory.toFile().deleteRecursively()
        }
    }

    /**
     * 库搜索路径通过系统属性下发；空列表必须清除属性，避免污染后续会话。
     */
    @Test
    fun `library search properties are joined and cleared`() {
        val originalStdlib = System.getProperty(LspProjectConfiguration.STDLIB_PROPERTY)
        val originalLibrary = System.getProperty(LspProjectConfiguration.LIBRARY_PROPERTY)
        try {
            LspProjectConfiguration.fromInitializeParams(
                InitializeParams().apply {
                    initializationOptions = mapOf(
                        "stdLibPathOption" to "/a",
                        "targetLib" to "/b",
                        "multiModuleOption" to mapOf(
                            "file:///workspace/m" to mapOf(
                                "name" to "m",
                                "package_requires" to mapOf("path_option" to listOf("/c")),
                                "source_sets" to mapOf(
                                    "main" to mapOf("source_roots" to listOf("file:///workspace/m/src")),
                                ),
                            ),
                        ),
                    )
                },
            ).applyLibrarySearchProperties()

            assertEquals("/a", System.getProperty(LspProjectConfiguration.STDLIB_PROPERTY))
            assertEquals(
                listOf("/b", "/c").joinToString(File.pathSeparator),
                System.getProperty(LspProjectConfiguration.LIBRARY_PROPERTY),
            )

            LspProjectConfiguration(
                workspaceModules = emptyList(),
                stdlibSearchPaths = emptyList(),
                librarySearchPaths = emptyList(),
                showDiagnosticFactoryNames = false,
            ).applyLibrarySearchProperties()

            assertNull(System.getProperty(LspProjectConfiguration.STDLIB_PROPERTY))
            assertNull(System.getProperty(LspProjectConfiguration.LIBRARY_PROPERTY))
        } finally {
            restoreProperty(LspProjectConfiguration.STDLIB_PROPERTY, originalStdlib)
            restoreProperty(LspProjectConfiguration.LIBRARY_PROPERTY, originalLibrary)
        }
    }

    /**
     * 随机初始化选项：覆盖 null、字符串 JSON、Gson 元素、嵌套 map/list 与混合类型。
     */
    private fun randomOptions(random: Random): Map<String, Any?> = buildMap {
        if (random.nextBoolean()) {
            put("targetLib", randomScalar(random))
        }
        if (random.nextBoolean()) {
            put("stdLibPathOption", randomScalar(random))
        }
        if (random.nextBoolean()) {
            put("showDiagnosticFactoryNames", random.nextBoolean())
        }
        when (random.nextInt(3)) {
            0 -> put(
                "multiModuleOption",
                mapOf(
                    "file:///workspace/m" to mapOf(
                        "name" to randomScalar(random),
                        "package_requires" to mapOf("path_option" to randomList(random)),
                        "source_sets" to mapOf(
                            "main" to mapOf("source_roots" to randomList(random)),
                        ),
                        "requires" to random.nextBoolean().let { hasRequires ->
                            if (hasRequires) mapOf("lib" to randomScalar(random)) else null
                        },
                    ),
                ),
            )

            1 -> put("multiModuleOption", randomList(random))
            else -> Unit
        }
    }

    /**
     * 随机标量：字符串、数字、null 与布尔。
     */
    private fun randomScalar(random: Random): Any? = when (random.nextInt(5)) {
        0 -> null
        1 -> random.nextInt()
        2 -> random.nextDouble()
        3 -> listOf("a", "", "b")[random.nextInt(3)]
        else -> random.nextBoolean()
    }

    /**
     * 随机列表：元素可能是标量或嵌套 map。
     */
    private fun randomList(random: Random): List<Any?> = List(random.nextInt(0, 4)) {
        when (random.nextInt(3)) {
            0 -> mapOf("k" to randomScalar(random))
            else -> randomScalar(random)
        }
    }

    /**
     * 恢复系统属性。
     */
    private fun restoreProperty(key: String, value: String?) {
        if (value == null) System.clearProperty(key) else System.setProperty(key, value)
    }

    companion object {
        /** 固定种子。 */
        private const val SEED: Int = 20261111

        /** 随机用例数。 */
        private const val CASES: Int = 40
    }
}
