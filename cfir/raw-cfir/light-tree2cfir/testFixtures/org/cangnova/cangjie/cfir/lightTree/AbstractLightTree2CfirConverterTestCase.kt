package org.cangnova.cangjie.cfir.lightTree

import com.intellij.lang.PsiBuilderFactory
import com.intellij.lang.LighterASTNode
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.cfir.builder.AbstractRawCfirBuilderTestCase
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.renderer.CfirRenderer
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.source.toSourceLinesMapping
import java.io.File

/**
 * LightTree → CFIR 转换测试基类（对齐 K2 的 AbstractLightTree2FirConverterTestCase）。
 *
 * 使用 [CangJieLightParser] 解析源码得到 LightTree，
 * 再通过 [LightTree2Cfir] 构建 [CfirFile]，
 * 最终与 Golden 文件对比。
 */
abstract class AbstractLightTree2CfirConverterTestCase : AbstractRawCfirBuilderTestCase() {

    /**
     * 将源码文本解析为 LightTree。
     */
    protected fun parseLightTree(text: String): FlyweightCapableTreeStructure<LighterASTNode> {
        val parserDefinition = CangJieParserDefinition()
        val builder = PsiBuilderFactory.getInstance().createBuilder(
            parserDefinition,
            CangJieLexer(),
            text,
        )
        return CangJieLightParser.parse(builder)
    }

    /**
     * 通过 LightTree 路径构建 CfirFile。
     */
    protected fun buildCfirFileFromLightTree(
        text: String,
        session: CfirSession = createTestSession(),
        fileName: String = "test.cj",
        bodyBuildingMode: BodyBuildingMode = BodyBuildingMode.NORMAL,
    ): CfirFile {
        val lightTree = parseLightTree(text)
        val sourceFile = CjInMemoryTextSourceFile(fileName, null, text)
        return LightTree2Cfir(
            session = session,
            scopeProvider = session.cangjieScopeProvider,
            bodyBuildingMode = bodyBuildingMode,
        ).buildCfirFile(lightTree, sourceFile, text.toSourceLinesMapping())
    }

    /**
     * 核心测试方法：加载 .cj 文件，通过 LightTree 路径构建 CFIR，与 .txt Golden 文件对比。
     */
    open fun doLightTree2CfirTest(filePath: String) {
        val file = resolveTestDataPath(filePath)
        val sourceText = loadFile(file.path).trim()
        val cfirFile = buildCfirFileFromLightTree(sourceText, fileName = file.name)
        val actual = dumpCfirFile(cfirFile)
        val expectedPath = file.path.replace(".cj", ".txt")
        assertEqualsToFile(File(expectedPath), actual)
    }

    /**
     * 对比测试：同时通过 PSI 和 LightTree 两条路径构建 CFIR，
     * 验证渲染结果一致。
     */
    protected fun doCompareTest(sourceText: String, fileName: String = "test") {
        val session = createTestSession()

        // PSI 路径
        val cjFile = createCjFile(fileName, sourceText)
        val cfirFromPsi = cjFile.toCfirFile(session)
        val psiDump = dumpCfirFile(cfirFromPsi)

        // LightTree 路径
        val cfirFromLightTree = buildCfirFileFromLightTree(
            sourceText, session, fileName = "$fileName.cj",
        )
        val lightTreeDump = dumpCfirFile(cfirFromLightTree)

        // 对比（忽略文件名差异）
        val normalizedPsi = normalizeDump(psiDump)
        val normalizedLightTree = normalizeDump(lightTreeDump)

        if (normalizedPsi != normalizedLightTree) {
            throw AssertionError(
                "PSI and LightTree CFIR outputs differ for source:\n$sourceText\n" +
                        "=== PSI ===\n$psiDump\n=== LightTree ===\n$lightTreeDump"
            )
        }
    }

    /**
     * 对比测试：从测试数据文件加载。
     */
    protected fun doCompareTestFromFile(filePath: String) {
        val file = resolveTestDataPath(filePath)
        val sourceText = loadFile(file.path).trim()
        doCompareTest(sourceText, file.nameWithoutExtension)
    }

    /**
     * 规范化 CFIR dump 输出以进行对比。
     * 移除可能因路径不同而差异的部分（如文件名行）。
     */
    private fun normalizeDump(dump: String): String {
        return dump.lines()
            .map { line ->
                // 规范化 FILE: 行（PSI 路径有 .cj 后缀，LightTree 没有文件名）
                if (line.trimStart().startsWith("FILE:")) {
                    "FILE: <normalized>"
                } else {
                    line
                }
            }
            .joinToString("\n")
            .trim()
    }
}
