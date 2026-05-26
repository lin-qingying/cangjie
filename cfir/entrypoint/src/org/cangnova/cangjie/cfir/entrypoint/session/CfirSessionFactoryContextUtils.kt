package org.cangnova.cangjie.cfir.entrypoint.session

import org.cangnova.cangjie.cfir.entrypoint.configuration.apiLevel
import org.cangnova.cangjie.cfir.entrypoint.configuration.apiLevelSyscapConfigPath
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.cfir.session.CfirApiLevelProvider
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.classpathRoots
import java.io.File
import java.util.regex.Pattern

/**
 * 统一构造 CFIR session factory context。
 *
 * 这个 owner 必须同时服务真实 frontend pipeline 与测试 facade：
 * - classpath/CJO 搜索路径必须一致，否则 phased frontend 与普通 diagnostics
 *   会落到两套不同的标准库/依赖解析环境；
 * - API level / syscap 也必须从同一份 [CompilerConfiguration] 解读，
 *   避免测试环境和生产环境出现隐藏分叉。
 */
fun createDefaultCfirSessionFactoryContext(
    configuration: CompilerConfiguration,
): CfirDefaultSessionFactory.Context {
    val classpath = configuration.classpathRoots.map { it.path }.filter { it.isNotBlank() }
    val cjoManager = CjoManager(
        CjoSearchPath { key ->
            when (key) {
                "CANGJIE_LIBRARY" -> classpath.takeIf { it.isNotEmpty() }?.joinToString(File.pathSeparator)
                else -> System.getenv(key)
            }
        }
    )
    val apiLevelProvider = createCfirApiLevelProvider(configuration)
    return CfirDefaultSessionFactory.Context(
        cjoManager = cjoManager,
        registerSourceSessionComponents = {
            if (apiLevelProvider != null) {
                register(CfirApiLevelProvider::class, apiLevelProvider)
            }
        },
    )
}

/**
 * 从 frontend 配置恢复 API level/syscap 语义。
 *
 * 这里不依赖具体 facade/pipeline 的运行方式，保证所有入口都按同一规则
 * 把测试指令/CLI 配置转换成 session 组件。
 */
fun createCfirApiLevelProvider(configuration: CompilerConfiguration): CfirApiLevelProvider? {
    val projectApiLevel = configuration.apiLevel
    val syscapConfigPath = configuration.apiLevelSyscapConfigPath

    if (projectApiLevel == null && syscapConfigPath.isNullOrBlank()) {
        return null
    }

    val syscapInfo = syscapConfigPath
        ?.takeIf { it.isNotBlank() }
        ?.let(::parseSyscapConfiguration)
        ?: ParsedSyscapConfiguration.EMPTY

    return object : CfirApiLevelProvider {
        override val projectApiLevel: Int =
            projectApiLevel ?: syscapInfo.apiLevel ?: CfirApiLevelProvider.DISABLED
        override val syscapEnabled: Boolean =
            syscapInfo.union.isNotEmpty() || syscapInfo.intersection.isNotEmpty()
        override val syscapUnion: Set<String> = syscapInfo.union
        override val syscapIntersection: Set<String> = syscapInfo.intersection
    }
}

private fun parseSyscapConfiguration(rawPath: String): ParsedSyscapConfiguration {
    val configFile = File(rawPath)
    if (!configFile.exists() || !configFile.isFile) {
        return ParsedSyscapConfiguration.EMPTY
    }

    val content = runCatching { configFile.readText(Charsets.UTF_8) }.getOrDefault("")
    if (content.isBlank()) return ParsedSyscapConfiguration.EMPTY

    val apiLevel = API_LEVEL_REGEX.find(content)?.groupValues?.getOrNull(1)?.toIntOrNull()
    val referencedFiles = SYS_CAP_FILE_REGEX.findAll(content)
        .mapNotNull { it.groupValues.getOrNull(1) }
        .map { relativePath -> configFile.parentFile.resolve(relativePath).normalize() }
        .filter { it.exists() && it.isFile }
        .toList()

    if (referencedFiles.isEmpty()) {
        return ParsedSyscapConfiguration(apiLevel = apiLevel)
    }

    val syscapSets = referencedFiles.mapNotNull(::parseSyscapLeafFile)
    if (syscapSets.isEmpty()) {
        return ParsedSyscapConfiguration(apiLevel = apiLevel)
    }

    val union = linkedSetOf<String>()
    syscapSets.forEach { union += it }

    val intersection = syscapSets
        .drop(1)
        .fold(syscapSets.first().toSet()) { acc, next -> acc intersect next }

    return ParsedSyscapConfiguration(
        apiLevel = apiLevel,
        union = union,
        intersection = intersection,
    )
}

private fun parseSyscapLeafFile(file: File): Set<String>? {
    val content = runCatching { file.readText(Charsets.UTF_8) }.getOrDefault("")
    if (content.isBlank()) return null
    val values = SYS_CAP_VALUE_REGEX.findAll(content)
        .mapNotNull { it.groupValues.getOrNull(1) }
        .map(String::trim)
        .filter(String::isNotEmpty)
        .toCollection(linkedSetOf())
    return values.takeIf { it.isNotEmpty() }
}

private data class ParsedSyscapConfiguration(
    val apiLevel: Int? = null,
    val union: Set<String> = emptySet(),
    val intersection: Set<String> = emptySet(),
) {
    companion object {
        val EMPTY = ParsedSyscapConfiguration()
    }
}

private val API_LEVEL_REGEX = Pattern.compile("\"apiLevel\"\\s*:\\s*(\\d+)").toRegex()
private val SYS_CAP_FILE_REGEX = Pattern.compile("\"(\\./[^\"]+\\.json)\"").toRegex()
private val SYS_CAP_VALUE_REGEX = Pattern.compile("\"([^\"]+)\"").toRegex()
