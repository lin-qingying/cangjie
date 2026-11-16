package org.cangnova.cangjie.wasm.common

/**
 * Wasm 宿主目标。目标 alias 对外稳定，宿主差异集中在 [profile] 中。
 */
enum class WasmTarget(
    /** 编译器参数与 API 使用的稳定目标名。 */
    val alias: String,
    /** 目标使用的宿主 ABI。 */
    val hostAbi: WasmHostAbi,
) {
    WASI("wasm-wasi", WasmHostAbi.WASI_PREVIEW_1),
    UNKNOWN_UNKNOWN("wasm32-unknown-unknown", WasmHostAbi.CORE_MODULE),
    ;

    /** 返回目标的不可变 Wasm 宿主约定。 */
    val profile: WasmTargetProfile
        get() = when (this) {
            WASI -> WasmTargetProfile(
                target = this,
                runtimeImportModule = "cangjie_runtime",
                wasiImportModule = "wasi_snapshot_preview1",
                entryExportName = "_start",
                memoryExportName = "memory",
                defaultExportPublicFunctions = false,
            )
            UNKNOWN_UNKNOWN -> WasmTargetProfile(
                target = this,
                runtimeImportModule = "cangjie_runtime",
                wasiImportModule = null,
                entryExportName = null,
                memoryExportName = "memory",
                defaultExportPublicFunctions = true,
            )
        }

    companion object {
        /**
         * 按稳定 alias 解析目标。`wasm-js` 不会被隐式映射到其它宿主 ABI。
         */
        @JvmStatic
        fun fromAlias(alias: String): WasmTarget? = entries.firstOrNull { it.alias == alias }

        /** 按 alias 解析目标；未支持的目标（包括 `wasm-js`）以明确异常报告。 */
        @JvmStatic
        fun parse(alias: String): WasmTarget = fromAlias(alias)
            ?: throw UnsupportedWasmTargetException(alias)
    }
}

/** Wasm target alias 尚未被该独立 backend 实现时抛出的解析错误。 */
class UnsupportedWasmTargetException(alias: String) : IllegalArgumentException(
    "unsupported Wasm target '$alias'; supported targets are ${WasmTarget.entries.joinToString { it.alias }}",
)

/** Wasm Core module 所使用的宿主 ABI 类别。 */
enum class WasmHostAbi {
    /** WASI Preview 1 导入命名空间与 `_start` 启动约定。 */
    WASI_PREVIEW_1,

    /** 由嵌入宿主显式提供 imports 并调用 exports 的核心模块 ABI。 */
    CORE_MODULE,
}

/**
 * 一个目标对应的宿主接口契约。
 *
 * 后端按此 profile 生成 imports、exports、linear memory 与启动函数，避免在指令 lowering 中
 * 混入宿主专属分支。
 */
data class WasmTargetProfile(
    /** 此 profile 所属目标。 */
    val target: WasmTarget,
    /** CHIR runtime imports 所在的 module namespace。 */
    val runtimeImportModule: String,
    /** WASI system imports 所在的 namespace；核心模块目标没有该 namespace。 */
    val wasiImportModule: String?,
    /** 由宿主调用的程序入口 export 名称；无自动入口时为 null。 */
    val entryExportName: String?,
    /** 导出 linear memory 的名称；null 表示不导出。 */
    val memoryExportName: String?,
    /** 默认是否将有外部可见性的 CHIR 函数导出给宿主。 */
    val defaultExportPublicFunctions: Boolean,
)
