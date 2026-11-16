package org.cangnova.cangjie.lsp.analysis

import com.google.gson.JsonObject

/**
 * 通用 resolve 路径使用的最小 URI 定位数据。
 *
 * ModCommand quick fix 已在 command 中携带结果，不需要 resolve data。
 */
internal data class LspCodeActionResolveData(val documentUri: String) {
    companion object {
        fun parse(value: Any?): LspCodeActionResolveData? = when (value) {
            is JsonObject -> value.get("documentUri")
                ?.takeIf { it.isJsonPrimitive && it.asJsonPrimitive.isString }
                ?.asString
                ?.let(::LspCodeActionResolveData)
            is Map<*, *> -> (value["documentUri"] as? String)?.let(::LspCodeActionResolveData)
            else -> null
        }
    }
}
