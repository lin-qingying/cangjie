package org.cangnova.cangjie.frontend.pipeline

import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path

/** SDK 选择的来源；显式配置无效时不得改选另一个版本。 */
enum class MacroSdkOrigin {
    COMPILER_CONFIGURATION,
    CANGJIE_HOME,
    CJC_HOME_PROPERTY,
    PATH,
    USER_HOME,
}

/** 一次编译调用的不可变 SDK 选择结果；缺失与明确的无效配置是不同事实。 */
sealed interface MacroSdkSelection {
    data class Selected(val home: Path, val origin: MacroSdkOrigin) : MacroSdkSelection {
        /** 各组件只从同一个已选择的根目录取路径，不再次读取环境或搜索其他安装。 */
        val compilerExecutable: Path get() = home.resolve("bin").resolve(macroToolName("cjc"))
        val executorExecutable: Path get() = home.resolve("tools/bin").resolve(macroToolName("LSPMacroServer"))
    }

    data object Absent : MacroSdkSelection

    data class Invalid(val requestedHome: String, val origin: MacroSdkOrigin, val reason: String) : MacroSdkSelection
}

/** 显式环境输入便于在配置边界确定选择，也使选择规则可独立验证而不修改进程环境。 */
data class MacroSdkSelectionInputs(
    val explicitHome: String?,
    val cangjieHome: String?,
    val cjcHome: String?,
    val path: String?,
    val userHome: Path? = null,
)

/** SDK 只在调用边界选择；locator、compiler 与 executor 消费同一份结果。 */
object MacroSdkSelector {
    fun select(explicitHome: String?): MacroSdkSelection = select(
        MacroSdkSelectionInputs(
            explicitHome,
            System.getenv("CANGJIE_HOME"),
            System.getProperty("cjc.home"),
            System.getenv("PATH"),
            System.getProperty("user.home")?.let(Path::of),
        ),
    )

    fun select(inputs: MacroSdkSelectionInputs): MacroSdkSelection {
        inputs.explicitHome?.let { return fromHome(it, MacroSdkOrigin.COMPILER_CONFIGURATION) }
        inputs.cangjieHome?.takeIf(String::isNotBlank)?.let { return fromHome(it, MacroSdkOrigin.CANGJIE_HOME) }
        inputs.cjcHome?.takeIf(String::isNotBlank)?.let { return fromHome(it, MacroSdkOrigin.CJC_HOME_PROPERTY) }

        // 只有没有任何显式 home 时才采用 PATH。任意目录中的包装命令不证明完整 SDK 布局。
        for (entry in inputs.path.orEmpty().split(File.pathSeparatorChar).filter(String::isNotBlank)) {
            val executable = try {
                Path.of(entry).toAbsolutePath().normalize().resolve(macroToolName("cjc"))
            } catch (_: InvalidPathException) {
                continue // PATH 中无效的路径项不能指向可执行文件。
            }
            if (!Files.isRegularFile(executable)) continue
            val resolvedExecutable = try {
                executable.toRealPath()
            } catch (error: IOException) {
                return MacroSdkSelection.Invalid(executable.toString(), MacroSdkOrigin.PATH, error.message.orEmpty())
            }
            val bin = resolvedExecutable.parent
            if (bin.fileName?.toString()?.equals("bin", ignoreCase = isWindows()) != true || bin.parent == null) {
                return MacroSdkSelection.Invalid(
                    executable.toString(), MacroSdkOrigin.PATH,
                    "The cjc found on PATH is not in an SDK bin directory.",
                )
            }
            return fromHome(bin.parent.toString(), MacroSdkOrigin.PATH)
        }
        val userHome = inputs.userHome ?: return MacroSdkSelection.Absent
        val installedRoots = listOf(userHome.resolve(".cangjie/sdks"), userHome.resolve("sdk"))
        for (root in installedRoots) {
            val installs = root.toFile().listFiles().orEmpty().asSequence()
                .filter(File::isDirectory)
                .sortedByDescending(File::getName)
            for (install in installs) {
                val installRoot = install.toPath()
                val sdk = sequenceOf(installRoot.resolve("cangjie"), installRoot).firstOrNull {
                    Files.isRegularFile(it.resolve("bin").resolve(macroToolName("cjc")))
                }
                if (sdk != null) return fromHome(sdk.toString(), MacroSdkOrigin.USER_HOME)
            }
        }
        return MacroSdkSelection.Absent
    }

    private fun fromHome(value: String, origin: MacroSdkOrigin): MacroSdkSelection {
        if (value.isBlank()) return MacroSdkSelection.Invalid(value, origin, "The SDK root is empty.")
        val home = try {
            Path.of(value).toAbsolutePath().normalize()
        } catch (error: InvalidPathException) {
            return MacroSdkSelection.Invalid(value, origin, error.reason)
        }
        if (!Files.isDirectory(home)) {
            return MacroSdkSelection.Invalid(value, origin, "The SDK root directory does not exist: $home")
        }
        // 不在选择阶段要求所有组件存在：纯 artifact 消费不需要 cjc 或 LSPMacroServer。
        val realHome = try {
            home.toRealPath()
        } catch (error: IOException) {
            return MacroSdkSelection.Invalid(value, origin, error.message.orEmpty())
        }
        return MacroSdkSelection.Selected(realHome, origin)
    }
}

/** 进入宏缓存键的选择身份；切换 SDK 不能沿用另一安装的结果。 */
internal val MacroSdkSelection.identity: String
    get() = when (this) {
        is MacroSdkSelection.Selected -> "${origin.name}:$home"
        is MacroSdkSelection.Invalid -> "${origin.name}:invalid:$requestedHome:$reason"
        MacroSdkSelection.Absent -> "absent"
    }

internal fun MacroSdkSelection.requireSelected(): MacroSdkSelection.Selected = when (this) {
    is MacroSdkSelection.Selected -> this
    is MacroSdkSelection.Invalid -> error("Invalid macro SDK from ${origin.name}: $reason")
    MacroSdkSelection.Absent -> error("Macro SDK is not configured. Set macroSdkHome, CANGJIE_HOME or cjc.home, or put SDK bin on PATH.")
}

/** 原生组件使用同一 SDK 的工具与运行库；旧父进程 CANGJIE_HOME 不能覆盖明确选择。 */
internal fun MacroSdkSelection.Selected.processEnvironment(
    parentEnvironment: Map<String, String> = System.getenv(),
): Map<String, String> {
    val pathKey = parentEnvironment.keys.firstOrNull { it.equals("PATH", ignoreCase = isWindows()) } ?: "PATH"
    val path = listOf(
        home.resolve("bin").toString(),
        home.resolve("tools/bin").toString(),
        home.resolve("runtime/lib/${defaultCangjieLibHost()}").toString(),
        home.resolve("third_party/llvm/bin").toString(),
        parentEnvironment[pathKey].orEmpty(),
    ).filter(String::isNotBlank).joinToString(File.pathSeparator)
    return mapOf("CANGJIE_HOME" to home.toString(), pathKey to path)
}

private fun macroToolName(name: String): String = if (isWindows()) "$name.exe" else name
private fun isWindows(): Boolean = System.getProperty("os.name").lowercase().contains("win")

/** SDK 库布局的共享宿主名称，artifact 查找与原生进程环境必须使用同一规则。 */
internal fun defaultCangjieLibHost(): String = when {
    System.getProperty("os.name").contains("Windows", ignoreCase = true) -> "windows_x86_64_cjnative"
    System.getProperty("os.name").contains("Mac", ignoreCase = true) -> "darwin_x86_64_cjnative"
    else -> "linux_x86_64_cjnative"
}
