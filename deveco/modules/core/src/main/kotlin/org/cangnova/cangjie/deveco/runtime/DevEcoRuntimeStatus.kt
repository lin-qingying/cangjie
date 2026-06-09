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
    val name: String,
    val descriptorPath: String,
    val requiredClasses: List<String>,
)

/**
 * 当前插件运行时与鸿蒙项目识别结果。
 */
data class DevEcoRuntimeSnapshot(
    val capabilities: List<DevEcoRuntimeCapabilityStatus>,
    val resources: List<DevEcoRuntimeResourceStatus>,
    val projectMarkers: List<DevEcoProjectMarkerStatus>,
    val officialPluginId: String,
    val officialPluginPath: String?,
    val officialPluginDescriptorAvailable: Boolean,
    val indexing: Boolean,
) {
    val allCapabilitiesAvailable: Boolean
        get() = capabilities.all(DevEcoRuntimeCapabilityStatus::available)

    val allResourcesAvailable: Boolean
        get() = officialPluginDescriptorAvailable && resources.all(DevEcoRuntimeResourceStatus::present)

    val runtimeReady: Boolean
        get() = allCapabilitiesAvailable && allResourcesAvailable

    val harmonyProjectDetected: Boolean
        get() = projectMarkers.any(DevEcoProjectMarkerStatus::present)
}

data class DevEcoRuntimeCapabilityStatus(
    val capability: DevEcoRuntimeCapability,
    val descriptorAvailable: Boolean,
    val missingClasses: List<String>,
) {
    val available: Boolean
        get() = descriptorAvailable && missingClasses.isEmpty()
}

data class DevEcoProjectMarkerStatus(
    val fileName: String,
    val present: Boolean,
)

data class DevEcoRuntimeResource(
    val name: String,
    val relativePath: String,
)

data class DevEcoRuntimeResourceStatus(
    val resource: DevEcoRuntimeResource,
    val present: Boolean,
    val absolutePath: String?,
)

/**
 * 官方 DevEco 仓颉插件标识。
 *
 * 官方 project-mgmt jar 内部会通过该 id 取得插件路径，再读取 lib/templates 等资源。
 */
object DevEcoOfficialCangjiePlugin {
    const val ID: String = "com.huawei.cangjie-support-plugin"
}

/**
 * DevEco Cangjie 运行时能力表。
 *
 * 这里映射的是本插件从官方 jar 注册的框架级入口，不做单点兜底。
 */
object DevEcoRuntimeCapabilities {
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
    private val dumbService = DumbService.getInstance(project)
    private val projectScope = GlobalSearchScope.projectScope(project)
    private val classLoader = javaClass.classLoader

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

    private fun capabilityStatus(capability: DevEcoRuntimeCapability): DevEcoRuntimeCapabilityStatus {
        val descriptorAvailable = classLoader.getResource(capability.descriptorPath) != null
        val missingClasses = capability.requiredClasses.filterNot(::isClassAvailable)
        return DevEcoRuntimeCapabilityStatus(
            capability = capability,
            descriptorAvailable = descriptorAvailable,
            missingClasses = missingClasses,
        )
    }

    private fun isClassAvailable(className: String): Boolean =
        runCatching { Class.forName(className, false, classLoader) }.isSuccess

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

    private fun Path.resolveRuntimePath(relativePath: String): Path =
        relativePath.split('/').fold(this) { path, segment -> path.resolve(segment) }
}
