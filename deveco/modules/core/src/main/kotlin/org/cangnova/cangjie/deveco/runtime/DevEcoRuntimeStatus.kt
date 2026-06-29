package org.cangnova.cangjie.deveco.runtime

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import java.nio.file.Files
import java.nio.file.Path

/**
 * DevEco Cangjie 运行时能力项。
 *
 * 每一项同时声明必须存在的描述文件和核心类，工具窗口据此确认插件包是否真正携带了对应功能。
 */
data class DevEcoRuntimeCapability(
    /** 面向用户展示的能力名称。 */
    val name: String,
    /** 注册该能力所需的插件描述文件路径。 */
    val descriptorPath: String,
    /** 证明能力可用所需加载成功的核心类名。 */
    val requiredClasses: List<String>,
)

/**
 * 当前插件运行时与鸿蒙项目识别结果。
 */
data class DevEcoRuntimeSnapshot(
    /** 官方仓颉运行时能力检查结果。 */
    val capabilities: List<DevEcoRuntimeCapabilityStatus>,
    /** 官方插件资源目录检查结果。 */
    val resources: List<DevEcoRuntimeResourceStatus>,
    /** 当前项目中的鸿蒙/仓颉项目标记检查结果。 */
    val projectMarkers: List<DevEcoProjectMarkerStatus>,
    /** 官方 DevEco 仓颉插件 id。 */
    val officialPluginId: String,
    /** 官方插件安装路径，未安装时为 null。 */
    val officialPluginPath: String?,
    /** 官方插件描述是否可从宿主插件系统读取。 */
    val officialPluginDescriptorAvailable: Boolean,
    /** 当前项目是否处于 IntelliJ dumb/indexing 阶段。 */
    val indexing: Boolean,
) {
    /** 所有运行时能力是否均可用。 */
    val allCapabilitiesAvailable: Boolean
        get() = capabilities.all(DevEcoRuntimeCapabilityStatus::available)

    /** 官方插件描述和所需资源是否均可用。 */
    val allResourcesAvailable: Boolean
        get() = officialPluginDescriptorAvailable && resources.all(DevEcoRuntimeResourceStatus::present)

    /** DevEco 仓颉运行时是否已满足基础使用条件。 */
    val runtimeReady: Boolean
        get() = allCapabilitiesAvailable && allResourcesAvailable

    /** 当前项目是否检测到鸿蒙/仓颉项目标记。 */
    val harmonyProjectDetected: Boolean
        get() = projectMarkers.any(DevEcoProjectMarkerStatus::present)
}

/**
 * 单项运行时能力的检查结果。
 */
data class DevEcoRuntimeCapabilityStatus(
    /** 被检查的运行时能力定义。 */
    val capability: DevEcoRuntimeCapability,
    /** 能力描述文件是否可从插件 classpath 读取。 */
    val descriptorAvailable: Boolean,
    /** 未能加载的核心类名列表。 */
    val missingClasses: List<String>,
) {
    /** 描述文件存在且没有缺失类时能力可用。 */
    val available: Boolean
        get() = descriptorAvailable && missingClasses.isEmpty()
}

/**
 * 单个项目标记文件的检测结果。
 */
data class DevEcoProjectMarkerStatus(
    /** 项目标记文件名。 */
    val fileName: String,
    /** 当前项目中是否存在该标记文件。 */
    val present: Boolean,
)

/**
 * 官方插件运行时资源目录定义。
 */
data class DevEcoRuntimeResource(
    /** 面向用户展示的资源名称。 */
    val name: String,
    /** 相对官方插件安装目录的资源路径。 */
    val relativePath: String,
)

/**
 * 官方插件运行时资源目录检查结果。
 */
data class DevEcoRuntimeResourceStatus(
    /** 被检查的资源定义。 */
    val resource: DevEcoRuntimeResource,
    /** 资源目录是否存在。 */
    val present: Boolean,
    /** 解析后的绝对路径，官方插件不存在时为 null。 */
    val absolutePath: String?,
)

/**
 * 官方 DevEco 仓颉插件标识。
 *
 * 官方 project-mgmt jar 内部会通过该 id 取得插件路径，再读取 lib/templates 等资源。
 */
object DevEcoOfficialCangjiePlugin {
    /** 官方 DevEco 仓颉插件 id。 */
    const val ID: String = "com.huawei.cangjie-support-plugin"
}

/**
 * DevEco Cangjie 运行时能力表。
 *
 * 这里映射的是本插件从官方 jar 注册的框架级入口，不做单点兜底。
 */
object DevEcoRuntimeCapabilities {
    /** 需要在宿主中验证的官方仓颉运行时能力集合。 */
    val all: List<DevEcoRuntimeCapability> = listOf(
        DevEcoRuntimeCapability(
            name = "语言服务与编辑能力",
            descriptorPath = "META-INF/deveco-cangjie-official-runtime.xml",
            requiredClasses = listOf(
                "com.huawei.idea.language.CharParserDefinition",
                "com.huawei.idea.syntaxhighlighter.CangjieSyntaxFactory",
                "com.huawei.capabilities.completion.LSPCompletionContributor",
                "com.huawei.capabilities.lsp.CangjieGotoDeclarationHandler",
            ),
        ),
        DevEcoRuntimeCapability(
            name = "鸿蒙项目同步",
            descriptorPath = "META-INF/deveco-cangjie-official-runtime.xml",
            requiredClasses = listOf(
                "com.huawei.idea.lsp.ohoslauncher.CangjieSyncProject",
                "com.huawei.cangjie.projectmgmt.sync.CangjieProjectSyncImpl",
                "com.huawei.cangjie.projectmgmt.sync.CangjieModuleSyncImpl",
            ),
        ),
        DevEcoRuntimeCapability(
            name = "SDK 管理与 Hvigor 构建",
            descriptorPath = "META-INF/deveco-cangjie-official-runtime.xml",
            requiredClasses = listOf(
                "com.huawei.cangjie.sdkmanager.sync.CangjieSdkSync",
                "com.huawei.cangjie.build.filter.CangjieBuildFilterFactory",
            ),
        ),
        DevEcoRuntimeCapability(
            name = "模板与 ArkTS 互操作",
            descriptorPath = "META-INF/deveco-cangjie-official-runtime.xml",
            requiredClasses = listOf(
                "com.huawei.cangjie.projectmgmt.template.CangjieTemplateProvider",
                "com.huawei.cangjie.projectmgmt.action.GenerateCangjieIdlAction",
                "com.huawei.cangjie.projectmgmt.action.GenerateCangjieBindingsAction",
            ),
        ),
        DevEcoRuntimeCapability(
            name = "格式化与 Lint",
            descriptorPath = "META-INF/deveco-cangjie-official-runtime.xml",
            requiredClasses = listOf(
                "com.huawei.deveco.cjfmt.core.ReformatCodeService",
                "com.huawei.deveco.cjfmt.actions.CjReformatCodeAction",
                "com.huawei.deveco.cjlint.CjlintEngine",
            ),
        ),
        DevEcoRuntimeCapability(
            name = "DAP 与 Harmony 调试",
            descriptorPath = "META-INF/deveco-cangjie-official-runtime.xml",
            requiredClasses = listOf(
                "com.huawei.cangjie.debugger.breakpoint.type.CangjieSourceBreakpointType",
                "com.huawei.cangjie.debugger.ohos.CangjieOpenHarmonyDebugger",
                "com.huawei.cangjie.debugger.ohos.CangjieDualOpenHarmonyDebugger",
            ),
        ),
    )
}

/**
 * 官方运行时需要从插件目录读取的资源目录。
 */
object DevEcoRuntimeResources {
    /** 官方插件运行时必须可访问的资源目录集合。 */
    val all: List<DevEcoRuntimeResource> = listOf(
        DevEcoRuntimeResource(
            name = "模板根目录",
            relativePath = "lib/templates",
        ),
        DevEcoRuntimeResource(
            name = "Hvigor 构建模板",
            relativePath = "lib/templates/common/hvigor",
        ),
        DevEcoRuntimeResource(
            name = "Lint 规则与文档",
            relativePath = "lib/cjlint",
        ),
    )
}

/**
 * DevEco / OpenHarmony 项目识别标记。
 */
object DevEcoProjectMarkers {
    /** 用于识别 DevEco/OpenHarmony 项目的标记文件名集合。 */
    val fileNames: List<String> = listOf(
        "build-profile.json5",
        "module.json5",
        "oh-package.json5",
        "hvigorfile.ts",
        "cjpm.toml",
    )
}

/**
 * 汇总当前项目中的 DevEco Cangjie 运行时状态。
 */
class DevEcoRuntimeStatus(project: Project) {
    /** IntelliJ dumb service，用于避免索引期间查询文件索引。 */
    private val dumbService = DumbService.getInstance(project)
    /** 当前项目搜索范围。 */
    private val projectScope = GlobalSearchScope.projectScope(project)
    /** 当前插件 classloader，用于验证官方桥接类是否可加载。 */
    private val classLoader = javaClass.classLoader

    /**
     * 生成当前 DevEco 仓颉运行时快照。
     */
    fun snapshot(): DevEcoRuntimeSnapshot {
        val indexing = dumbService.isDumb
        val officialPluginDescriptor = PluginManagerCore.getPlugin(PluginId.getId(DevEcoOfficialCangjiePlugin.ID))
        val officialPluginPath = officialPluginDescriptor?.pluginPath
        return DevEcoRuntimeSnapshot(
            capabilities = DevEcoRuntimeCapabilities.all.map(::capabilityStatus),
            resources = DevEcoRuntimeResources.all.map { resource ->
                runtimeResourceStatus(resource, officialPluginPath)
            },
            projectMarkers = DevEcoProjectMarkers.fileNames.map { marker ->
                DevEcoProjectMarkerStatus(
                    fileName = marker,
                    present = !indexing && FilenameIndex.getVirtualFilesByName(marker, projectScope).isNotEmpty(),
                )
            },
            officialPluginId = DevEcoOfficialCangjiePlugin.ID,
            officialPluginPath = officialPluginPath?.toString(),
            officialPluginDescriptorAvailable = officialPluginDescriptor != null,
            indexing = indexing,
        )
    }

    /**
     * 检查单个运行时能力的描述文件和类加载状态。
     */
    private fun capabilityStatus(capability: DevEcoRuntimeCapability): DevEcoRuntimeCapabilityStatus {
        val descriptorAvailable = classLoader.getResource(capability.descriptorPath) != null
        val missingClasses = capability.requiredClasses.filterNot(::isClassAvailable)
        return DevEcoRuntimeCapabilityStatus(
            capability = capability,
            descriptorAvailable = descriptorAvailable,
            missingClasses = missingClasses,
        )
    }

    /**
     * 判断指定类是否可由当前插件 classloader 加载。
     */
    private fun isClassAvailable(className: String): Boolean =
        runCatching { Class.forName(className, false, classLoader) }.isSuccess

    /**
     * 检查官方插件安装目录下的资源路径是否存在。
     */
    private fun runtimeResourceStatus(
        resource: DevEcoRuntimeResource,
        officialPluginPath: Path?,
    ): DevEcoRuntimeResourceStatus {
        val absolutePath = officialPluginPath?.resolveRuntimePath(resource.relativePath)
        return DevEcoRuntimeResourceStatus(
            resource = resource,
            present = absolutePath != null && Files.isDirectory(absolutePath),
            absolutePath = absolutePath?.toString(),
        )
    }

    /**
     * 按 `/` 分割相对路径并解析到官方插件安装目录下。
     */
    private fun Path.resolveRuntimePath(relativePath: String): Path =
        relativePath.split('/').fold(this) { path, segment -> path.resolve(segment) }
}
