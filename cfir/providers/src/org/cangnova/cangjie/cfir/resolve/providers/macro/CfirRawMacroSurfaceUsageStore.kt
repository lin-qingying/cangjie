package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.name.FqName
import java.util.IdentityHashMap

/**
 * Analysis API raw-build 路径保留 expression macro 的结构化语法事实。
 *
 * 该路径只构建 Raw CFIR，不运行 compiler macro construction；因此 expression macro 会被替换为
 * construction-only error carrier。此 store 按 [CfirFile] 与调用方提供的精确 source identity 对象保存 raw builder 已收集的 surface，
 * 支持 lazy body 重建产生不同 CFIR 文件对象时继续读取同一 PSI snapshot 的语法事实；不把它伪装成已展开的 construction registry 状态。
 */
class CfirRawMacroSurfaceUsageStore : CfirSessionComponent {
    /** 按 CFIR/PSI 文件对象身份索引 expression macro surface；session 生命周期限定查询范围。 */
    private val expressionSurfacesByIdentity: MutableMap<Any, List<MacroSurfaceExpr>> = IdentityHashMap()

    /**
     * 记录 Raw builder 为 [file] 收集的 expression macro surface。
     *
     * 文件缓存负责单次构建；同一 CFIR 文件对象若再次提交，则要求 surface 身份顺序保持一致。
     */
    @Synchronized
    fun recordExpressionSurfaces(file: CfirFile, surfaces: List<MacroSurfaceExpr>) {
        record(file, surfaces)
    }

    /** 同时按 CFIR 文件和精确 source identity 索引，以覆盖 lazy body raw rebuild 产生的新 CFIR 文件对象。 */
    @Synchronized
    fun recordExpressionSurfaces(file: CfirFile, sourceFileIdentity: Any, surfaces: List<MacroSurfaceExpr>) {
        record(file, surfaces)
        record(sourceFileIdentity, surfaces)
    }

    /** 返回 [file] 对应的 CFIR 文件对象记录。 */
    @Synchronized
    fun expressionSurfaces(file: CfirFile): List<MacroSurfaceExpr> = expressionSurfacesByIdentity[file].orEmpty()

    /** 返回同一 source identity 对象的 surface，支持 lazy body 重建生成的等价 CFIR 文件。 */
    @Synchronized
    fun expressionSurfacesForSourceFile(sourceFileIdentity: Any): List<MacroSurfaceExpr> =
        expressionSurfacesByIdentity[sourceFileIdentity].orEmpty()

    /** 按 raw source range 与语法身份合并重复 raw rebuild 的 surface 列表。 */
    private fun record(identity: Any, surfaces: List<MacroSurfaceExpr>) {
        if (surfaces.isEmpty()) return
        expressionSurfacesByIdentity[identity] =
            (expressionSurfacesByIdentity[identity].orEmpty() + surfaces).distinctBy { it.stableIdentity() }
    }

    private fun MacroSurfaceExpr.stableIdentity(): SurfaceIdentity = SurfaceIdentity(
        qualifiedName = qualifiedName,
        isQualifiedName = isQualifiedName,
        startOffset = sourceRange?.startOffset,
        endOffset = sourceRange?.endOffset,
        rawSyntax = capturedRawSyntax,
    )

    private data class SurfaceIdentity(
        val qualifiedName: FqName?,
        val isQualifiedName: Boolean,
        val startOffset: Int?,
        val endOffset: Int?,
        val rawSyntax: String?,
    )
}
