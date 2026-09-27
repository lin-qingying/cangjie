package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirRawMacroSurfaceUsageStore
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceExpr

/** 当前 session 的 Analysis API raw expression macro surface 使用事实。 */
val CfirSession.rawMacroSurfaceUsageStoreOrNull: CfirRawMacroSurfaceUsageStore? by
    CfirSession.nullableSessionComponentAccessor()

/**
 * 确保 session 有 Raw macro surface 使用事实存储。
 *
 * 文件可并行构建，组件必须在 session 锁下只创建一次。
 */
fun CfirSession.ensureRawMacroSurfaceUsageStore(): CfirRawMacroSurfaceUsageStore = synchronized(this) {
    rawMacroSurfaceUsageStoreOrNull ?: CfirRawMacroSurfaceUsageStore().also {
        register(CfirRawMacroSurfaceUsageStore::class, it)
    }
}

/** 将 PSI raw builder 捕获的 expression macro surfaces 写入本 session 的 Analysis API usage store。 */
fun CfirSession.recordRawExpressionMacroSurfaces(
    file: CfirFile,
    sourceFileIdentity: Any,
    surfaces: List<MacroSurface>,
) {
    val expressionSurfaces = surfaces.filterIsInstance<MacroSurfaceExpr>()
    if (expressionSurfaces.isEmpty()) return
    ensureRawMacroSurfaceUsageStore().recordExpressionSurfaces(file, sourceFileIdentity, expressionSurfaces)
}
