package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.name.FqName
import java.util.IdentityHashMap

/**
 * Analysis API raw-build 路径保留所有宏 surface 的结构化语法事实。
 *
 * 该路径只构建 Raw CFIR，不运行 compiler macro construction。此 store 按 [CfirFile] 与调用方提供的精确 source identity
 * 对象保存 raw builder 已收集的 surface，支持 lazy body 重建产生不同 CFIR 文件对象时继续读取同一 PSI snapshot 的语法事实；
 * 不把它伪装成已展开的 construction registry 状态。
 */
class CfirRawMacroSurfaceUsageStore : CfirSessionComponent {
    /** 按 CFIR/PSI 文件对象身份索引 raw macro surface；session 生命周期限定查询范围。 */
    private val surfacesByIdentity: MutableMap<Any, List<MacroSurface>> = IdentityHashMap()

    /** 记录 Raw builder 为 [file] 收集的全部 macro surface。 */
    @Synchronized
    fun recordSurfaces(file: CfirFile, surfaces: List<MacroSurface>) {
        record(file, surfaces)
    }

    /** 同时按 CFIR 文件和精确 source identity 索引全部 surface。 */
    @Synchronized
    fun recordSurfaces(file: CfirFile, sourceFileIdentity: Any, surfaces: List<MacroSurface>) {
        record(file, surfaces)
        record(sourceFileIdentity, surfaces)
    }

    /** 返回 [file] 对应的全部 raw macro surface。 */
    @Synchronized
    fun surfaces(file: CfirFile): List<MacroSurface> = surfacesByIdentity[file].orEmpty()

    /** 返回同一 source identity 对象对应的全部 raw macro surface。 */
    @Synchronized
    fun surfacesForSourceFile(sourceFileIdentity: Any): List<MacroSurface> = surfacesByIdentity[sourceFileIdentity].orEmpty()

    /**
     * 记录 expression macro surface，保留既有 Analysis API 使用方的兼容入口。
     */
    @Synchronized
    fun recordExpressionSurfaces(file: CfirFile, surfaces: List<MacroSurfaceExpr>) {
        record(file, surfaces)
    }

    /** 同时按 CFIR 文件和精确 source identity 索引 expression macro surface。 */
    @Synchronized
    fun recordExpressionSurfaces(file: CfirFile, sourceFileIdentity: Any, surfaces: List<MacroSurfaceExpr>) {
        record(file, surfaces)
        record(sourceFileIdentity, surfaces)
    }

    /** 返回 [file] 对应的 expression macro surface。 */
    @Synchronized
    fun expressionSurfaces(file: CfirFile): List<MacroSurfaceExpr> =
        surfacesByIdentity[file].orEmpty().filterIsInstance<MacroSurfaceExpr>()

    /** 返回同一 source identity 对象对应的 expression macro surface。 */
    @Synchronized
    fun expressionSurfacesForSourceFile(sourceFileIdentity: Any): List<MacroSurfaceExpr> =
        surfacesByIdentity[sourceFileIdentity].orEmpty().filterIsInstance<MacroSurfaceExpr>()

    /** 按 raw source range 与语法身份合并重复 raw rebuild 的 surface 列表。 */
    private fun record(identity: Any, surfaces: List<MacroSurface>) {
        if (surfaces.isEmpty()) return
        surfacesByIdentity[identity] =
            (surfacesByIdentity[identity].orEmpty() + surfaces).distinctBy { it.stableIdentity() }
    }

    private fun MacroSurface.stableIdentity(): SurfaceIdentity = SurfaceIdentity(
        surfaceType = this::class.java,
        qualifiedName = qualifiedName,
        isQualifiedName = isQualifiedName,
        startOffset = sourceRange?.startOffset,
        endOffset = sourceRange?.endOffset,
        rawSyntax = capturedRawSyntax,
    )

    private data class SurfaceIdentity(
        val surfaceType: Class<out MacroSurface>,
        val qualifiedName: FqName?,
        val isQualifiedName: Boolean,
        val startOffset: Int?,
        val endOffset: Int?,
        val rawSyntax: String?,
    )
}
