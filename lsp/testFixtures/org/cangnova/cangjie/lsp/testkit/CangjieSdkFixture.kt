package org.cangnova.cangjie.lsp.testkit

import java.nio.file.Files
import java.nio.file.Path
import org.junit.jupiter.api.Assertions.fail

/**
 * 真实仓颉 SDK 的测试入口。
 *
 * 宏展开用例必须跑在固定 SDK 上才有意义：缺 SDK 时**不能静默跳过**，否则
 * 「宏引擎端到端」这类覆盖会在 CI 上悄悄变成 0 个用例，而报告仍然全绿。
 * 这里因此提供 `require*` 系列：解析不到直接让作业失败，并在消息里写清怎么提供 SDK。
 */
object CangjieSdkFixture {
    /**
     * 测试用于覆盖 SDK 位置的环境变量名（优先级最高）。
     *
     * 与产品侧 `CANGJIE_HOME` 分开是为了允许覆盖作业指向固定版本的 SDK，
     * 而本机开发环境可以继续用自己安装的那一份。
     */
    const val SDK_HOME_ENV: String = "LSP_TEST_CANGJIE_HOME"

    /**
     * 产品侧约定的 SDK 根目录环境变量名。
     */
    const val PRODUCT_SDK_HOME_ENV: String = "CANGJIE_HOME"

    /**
     * 返回固定 SDK 根目录；缺失时让当前作业失败。
     */
    fun requireSdkHome(purpose: String): Path {
        return resolveSdkHome() ?: fail<Path>(
            buildString {
                append("缺少可用的仓颉 SDK，无法执行「$purpose」。")
                append("请设置环境变量 $SDK_HOME_ENV 或 $PRODUCT_SDK_HOME_ENV 指向 SDK 根目录")
                append("（目录内需包含 bin/cjc 与 tools/bin/LSPMacroServer）。")
            },
        )
    }

    /**
     * 解析 SDK 根目录；两个环境变量都没有指向有效目录时返回 null。
     */
    fun resolveSdkHome(): Path? {
        return sequenceOf(System.getenv(SDK_HOME_ENV), System.getenv(PRODUCT_SDK_HOME_ENV))
            .filterNotNull()
            .mapNotNull { raw -> raw.takeIf { it.isNotBlank() }?.let(Path::of) }
            .firstOrNull { Files.isDirectory(it) }
    }

    /**
     * 返回宏服务端可执行文件；缺失时让当前作业失败。
     */
    fun requireMacroServer(sdkHome: Path): Path {
        val macroServer = sdkHome.resolve("tools").resolve("bin").resolve("LSPMacroServer")
        if (!Files.isRegularFile(macroServer)) {
            fail<Path>("SDK $sdkHome 缺少宏服务端 tools/bin/LSPMacroServer，无法执行宏展开用例。")
        }
        return macroServer
    }

    /**
     * 返回仓颉编译器可执行文件；缺失时让当前作业失败。
     */
    fun requireCompiler(sdkHome: Path): Path {
        val compiler = sdkHome.resolve("bin").resolve("cjc")
        if (!Files.isRegularFile(compiler)) {
            fail<Path>("SDK $sdkHome 缺少编译器 bin/cjc，无法编译宏包。")
        }
        return compiler
    }

    /**
     * 以 SDK 为根构造子进程环境，保证编译器/宏服务端都看到同一个 CANGJIE_HOME。
     */
    fun processEnvironment(sdkHome: Path): Map<String, String> =
        System.getenv() + mapOf(PRODUCT_SDK_HOME_ENV to sdkHome.toAbsolutePath().toString())
}
