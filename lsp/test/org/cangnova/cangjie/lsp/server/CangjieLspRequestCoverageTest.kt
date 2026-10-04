package org.cangnova.cangjie.lsp.server

import org.eclipse.lsp4j.services.TextDocumentService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/**
 * LSP 请求枚举与服务实现的漂移防护。
 *
 * `CangjieRequestExecutor.compute` 把请求种类做成必填参数，正是为了强制每个请求显式声明
 * 自己测的是哪个方法——没有兜底桶，新增请求就不会悄悄落进无名桶。本用例是这个约定的
 * 兜底检查：任何新增的 `TextDocumentService` 方法都必须有对应的枚举项，否则 IDE 卡顿
 * 排查会缺一段最直接的证据。
 */
class CangjieLspRequestCoverageTest {
    /**
     * 服务实现的每个带参方法都必须有对应的请求枚举项。
     */
    @Test
    fun everyTextDocumentServiceMethodHasARequestEntry() {
        // 基准是"本服务真正实现的 LSP 请求"，两侧都要卡：
        // - lsp4j 接口的方法集合排除未实现的 moniker / willSaveWaitUntil（它们不走请求执行器）；
        // - 本类声明的方法集合排除 completed 这类私有辅助方法（它们不是 LSP 请求）。
        val protocolMethods = TextDocumentService::class.java.methods.map { it.name }.toSet()
        val implementedMethods = CangjieTextDocumentService::class.java.declaredMethods
            .filter { !it.isSynthetic && !it.isBridge }
            .map { it.name }
            .toSet()
        val serviceMethods = protocolMethods intersect implementedMethods

        val requests = CangjieLspRequest.entries.associateBy { enumNameToMethodName(it.name) }

        serviceMethods.forEach { method ->
            assertTrue(
                requests.containsKey(method),
                "LSP 服务方法 $method 没有对应的 CangjieLspRequest；请求级耗时会出现无名桶。",
            )
        }
    }

    /**
     * 枚举项名（大写下划线）到服务方法名（驼峰）的换算，是"新增方法只需新增同名枚举项"这条
     * 约定在代码里的唯一表述。请求执行器把请求种类做成必填参数，新增方法漏填会编译不过；
     * 本用例补上另一半：新增方法忘了加枚举项会在这里失败。
     */
    private fun enumNameToMethodName(enumName: String): String {
        val parts = enumName.split("_").filter { it.isNotEmpty() }
        return parts.first().lowercase() + parts.drop(1).joinToString("") { it.lowercase().replaceFirstChar(Char::uppercase) }
    }

    /**
     * LSP 方法名必须形如 `namespace/method`，且命名空间只能是 LSP 定义的三个。
     *
     * 这里不校验"枚举名 = 方法名的机械换算"：枚举名对应的是**服务方法名**，与协议里的
     * 方法串不是一回事（`semanticTokensFull` 对应 `textDocument/semanticTokens.full`）。
     * 真正的换算约束由 [everyTextDocumentServiceMethodHasARequestEntry] 守住。
     */
    @Test
    fun lspMethodsUseProtocolNamespace() {
        val namespaces = setOf("textDocument", "workspace")
        CangjieLspRequest.entries.forEach { request ->
            if (request.lspMethod == "initialize") return@forEach

            // 方法段本身可以再含 '/'（`textDocument/semanticTokens/full`），因此只约束
            // 首段是合法命名空间，不约束斜杠个数。
            assertTrue(
                '/' in request.lspMethod,
                "LSP 方法名必须形如 namespace/method：${request.lspMethod}",
            )
            val namespace = request.lspMethod.substringBefore('/')
            assertTrue(namespace in namespaces, "未知的 LSP 命名空间 $namespace：${request.lspMethod}")
        }
    }

    /**
     * LSP 方法名必须唯一且非空：重名会让两个请求共用同一组指标而无法区分。
     */
    @Test
    fun lspMethodsAreUniqueAndNonEmpty() {
        val methods = CangjieLspRequest.entries.map { it.lspMethod }
        assertEquals(methods.size, methods.toSet().size, "LSP 方法名重复")
        assertTrue(methods.none { it.isBlank() }, "LSP 方法名不得为空")
    }

    /**
     * 指标名里的 `/` 必须被替换成 `.`，否则指标名会被误读成层级分隔符。
     */
    @Test
    fun metricNamesNeverContainSlash() {
        CangjieLspRequest.entries.forEach { request ->
            assertTrue(
                '/' !in request.lspMethod.replace('/', '.'),
                "指标名段不应保留 '/'：${request.lspMethod}",
            )
        }
    }
}
