package org.cangnova.cangjie.lsp.process

import org.cangnova.cangjie.CangJieCoreEnvironmentMode
import org.cangnova.cangjie.lsp.CangjieLspServerLauncher
import org.cangnova.cangjie.lsp.CangjieLspServerOptions
import org.cangnova.cangjie.lsp.LspIoManager
import org.cangnova.cangjie.lsp.testkit.ProtocolContractAnalysisFacadeFactory
import org.cangnova.cangjie.lsp.testkit.ServiceLoadedLspTestEnvironment

/**
 * 进程级测试用的服务端入口。
 *
 * 复用生产的 [LspIoManager] 与 [CangjieLspServerLauncher]（即真实传输、真实 framing、
 * 真实生命周期），只把分析环境与 facade 换成可控实现：
 *
 * - 分析环境用 UnitTest 模式 + 插件 XML 服务注册，避免拉起完整 IDE 容器；
 * - facade 用可记录、可注入失败的协议契约实现，因此跨进程用例只验证传输与协议，
 *   不依赖仓颉语义。
 *
 * 参数：`--transport stdio|socket`，socket 模式另需 `--port <n>`。
 */
fun main(args: Array<String>) {
    val transport = args.optionValue("--transport") ?: "stdio"
    val port = args.optionValue("--port")?.toIntOrNull() ?: 0

    // 与生产入口保持一致：先把 println 之类的业务输出挪到日志流，协议流只留 JSON-RPC 帧。
    LspIoManager.setupStandardIo()

    val facadeFactory = ProtocolContractAnalysisFacadeFactory()
    val options = CangjieLspServerOptions(
        environmentMode = CangJieCoreEnvironmentMode.UnitTest,
        environmentFactory = {
            ServiceLoadedLspTestEnvironment.create(CangJieCoreEnvironmentMode.UnitTest)
        },
        analysisFacadeFactory = facadeFactory::create,
        exitHandler = { code -> System.exit(code) },
    )

    when (transport) {
        "socket" -> CangjieLspServerLauncher.launchSocket(port = port, options = options)
        else -> CangjieLspServerLauncher.launch(
            input = LspIoManager.inputStream,
            output = LspIoManager.outputStream,
            options = options,
        )
    }
}

/** 读取 `--name value` 或 `--name=value` 形式的参数值。 */
private fun Array<String>.optionValue(name: String): String? {
    val prefix = "$name="
    for (index in indices) {
        val argument = this[index]
        if (argument == name) return getOrNull(index + 1)?.takeIf(String::isNotBlank)
        if (argument.startsWith(prefix)) return argument.substring(prefix.length).takeIf(String::isNotBlank)
    }
    return null
}
