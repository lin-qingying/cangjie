package org.cangnova.cangjie.cfir.lightTree

import com.intellij.lang.LighterASTNode
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.CfirSession

/**
 * LightTree → Raw CFIR 顶层入口（对齐 Kotlin K2 的 LightTree2Fir）。
 *
 * 提供从 [FlyweightCapableTreeStructure] 构建 [CfirFile] 的 API。
 * 与 [PsiRawCfirBuilder.buildCfirFile] 对应，产出结构等价的 CFIR 树。
 *
 * 使用方式:
 * ```kotlin
 * val lightTree = CangJieLightParser.parse(builder)
 * val cfirFile = LightTree2Cfir(session, sourceText).buildCfirFile(lightTree)
 * ```
 */
class LightTree2Cfir(
    private val session: CfirSession,
    private val source: CharSequence,
    private val fileName: String = "",
    private val bodyBuildingMode: BodyBuildingMode = BodyBuildingMode.NORMAL,
) {
    /**
     * 将 LightTree 根节点构建为 [CfirFile]。
     *
     * @param tree LightTree 结构（由 [CangJieLightParser.parse] 返回）
     * @return 未解析的 Raw CFIR 文件节点
     */
    fun buildCfirFile(tree: FlyweightCapableTreeStructure<LighterASTNode>): CfirFile {
        val builder = LightTreeRawCfirDeclarationBuilder(
            session = session,
            tree = tree,
            source = source,
            fileName = fileName,
            bodyBuildingMode = bodyBuildingMode,
        )
        val root = tree.root
        return builder.buildFile(root)
    }
}
