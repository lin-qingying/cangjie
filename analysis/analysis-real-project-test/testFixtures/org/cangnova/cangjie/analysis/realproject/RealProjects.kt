package org.cangnova.cangjie.analysis.realproject

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * 解析真实项目列表。
 *
 * 优先级：
 * 1. `-PrealProjectRepo/-PrealProjectRef`：单项目模式；
 * 2. `-PrealProjectConfig=<path>`：指定 properties 文件；
 * 3. 默认读取 `analysis/analysis-real-project-test/projects.properties`；
 * 4. 都没有时返回空列表，测试跳过。
 *
 * properties 键：`<id>.repo`、`<id>.ref`、`<id>.sdk`（可选，缺省用 `-PcangjieHome`）、
 * `<id>.refresh`（可选，缺省 true）。
 */
object RealProjects {

    private const val DEFAULT_CONFIG_PATH = "analysis/analysis-real-project-test/projects.properties"

    /** 读取全部项目配置；`realProject.filter` 可按 id 过滤。 */
    fun resolve(): List<RealProjectConfig> {
        val workspaceRoot = Paths.get(
            System.getProperty("realProject.root").orEmpty().ifBlank { "build/real-projects" },
        )
        val defaultSdk = System.getProperty("cangjie.home").orEmpty()
        val filters = System.getProperty("realProject.filter").orEmpty()
            .split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toSet()

        val singleRepo = System.getProperty("realProject.repo").orEmpty()
        val configs = if (singleRepo.isNotBlank()) {
            listOf(
                RealProjectConfig(
                    id = repoName(singleRepo),
                    repo = singleRepo,
                    ref = System.getProperty("realProject.ref").orEmpty(),
                    cangjieHome = Paths.get(defaultSdk),
                    workspaceRoot = workspaceRoot,
                    refresh = System.getProperty("realProject.refresh").orEmpty().ifBlank { "true" }.toBoolean(),
                ),
            )
        } else {
            readConfigFile(workspaceRoot, defaultSdk)
        }

        return configs
            .filter { filters.isEmpty() || it.id in filters }
            .onEach { config ->
                require(config.ref.isNotBlank()) { "项目 `${config.id}` 缺少 ref" }
                require(config.cangjieHome.toString().isNotBlank()) {
                    "项目 `${config.id}` 缺少 sdk，且未提供 -PcangjieHome"
                }
            }
    }

    private fun readConfigFile(workspaceRoot: Path, defaultSdk: String): List<RealProjectConfig> {
        val configuredPath = System.getProperty("realProject.config").orEmpty()
        val configFile = if (configuredPath.isNotBlank()) Paths.get(configuredPath) else Paths.get(DEFAULT_CONFIG_PATH)
        if (!Files.isRegularFile(configFile)) return emptyList()

        // 不用 java.util.Properties：它把反斜杠当转义，Windows 路径会被破坏。
        val entries = Files.readAllLines(configFile)
            .map { line -> line.substringBefore('#').trim() }
            .filter { line -> line.isNotEmpty() && '=' in line }
            .associate { line ->
                line.substringBefore('=').trim() to line.substringAfter('=').trim()
            }
        val ids = entries.keys
            .map { key -> key.substringBefore('.') }
            .filter { key -> key.isNotBlank() }
            .distinct()
            .sorted()

        return ids.map { id ->
            val sdk = entries["$id.sdk"].orEmpty().ifBlank { defaultSdk }
            RealProjectConfig(
                id = id,
                repo = entries["$id.repo"].orEmpty(),
                ref = entries["$id.ref"].orEmpty(),
                cangjieHome = Paths.get(sdk),
                workspaceRoot = workspaceRoot,
                refresh = entries["$id.refresh"].orEmpty().ifBlank { "true" }.toBoolean(),
            )
        }.onEach { config ->
            require(config.repo.isNotBlank()) { "配置项 `${config.id}` 缺少 repo: $configFile" }
        }
    }

    private fun repoName(repo: String): String =
        repo.replace('\\', '/').substringAfterLast('/').removeSuffix(".git").ifBlank { "real-project" }
}
