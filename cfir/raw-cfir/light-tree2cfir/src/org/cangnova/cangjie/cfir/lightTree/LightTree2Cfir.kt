package org.cangnova.cangjie.cfir.lightTree

import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilderFactory
import com.intellij.util.diff.FlyweightCapableTreeStructure
import java.io.File
import java.nio.file.Path
import org.cangnova.cangjie.CjIoFileSourceFile
import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.scopes.CfirScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.source.CjSourceFileLinesMapping
import org.cangnova.cangjie.source.readSourceFileWithMapping

/**
 * LightTree 到 raw CFIR 的文件级入口。
 *
 * 该类负责从文件、源码文本或已解析 LightTree 构造 [CfirFile]，并在需要时返回
 * raw 构建期间收集到的 macro surface 列表。
 *
 * @property session 当前 CFIR session。
 * @property scopeProvider class-like 声明使用的 scope provider。
 * @property bodyBuildingMode body 构建策略。
 */
class LightTree2Cfir(
    /** 当前 CFIR session。 */
    val session: CfirSession,
    /** class-like 声明使用的 scope provider。 */
    private val scopeProvider: CfirScopeProvider,

    /** body 构建策略。 */
    private val bodyBuildingMode: BodyBuildingMode = BodyBuildingMode.NORMAL,
) {
    /** 从磁盘路径读取源码并构造 CFIR 文件。 */
    fun buildCfirFile(path: Path): CfirFile {
        return buildCfirFile(path.toFile())
    }

    /** 从磁盘文件读取源码并构造 CFIR 文件。 */
    fun buildCfirFile(file: File): CfirFile {
        val sourceFile = CjIoFileSourceFile(file)
        val (code, linesMapping) = file.inputStream().reader(Charsets.UTF_8).use {
            it.readSourceFileWithMapping()
        }
        return buildCfirFile(code, sourceFile, linesMapping)
    }

    /** 从已解析 LightTree 构造 CFIR 文件。 */
    fun buildCfirFile(
        lightTree: FlyweightCapableTreeStructure<LighterASTNode>,
        sourceFile: CjSourceFile,
        linesMapping: CjSourceFileLinesMapping,
    ): CfirFile = buildCfirFileWithSurfaces(lightTree, sourceFile, linesMapping).first

    /**
     * 与 [buildCfirFile] 同行为，但同时返回构造期间收集的 macro surface 列表
     * （baseline Batch 4b："PSI 与 LightTree builder 都产 surface"）。
     */
    fun buildCfirFileWithSurfaces(
        lightTree: FlyweightCapableTreeStructure<LighterASTNode>,
        sourceFile: CjSourceFile,
        linesMapping: CjSourceFileLinesMapping,
    ): Pair<CfirFile, List<MacroSurface>> {
        @Suppress("UNUSED_VARIABLE")
        val code = sourceFile.getContentsAsStream().reader(Charsets.UTF_8).use { it.readText() }
        val declarationBuilder = LightTreeRawCfirDeclarationBuilder(
            session = session,
            baseScopeProvider = scopeProvider,
            tree = lightTree,
            source = code,
            bodyBuildingMode = bodyBuildingMode,
        )
        val file = declarationBuilder.buildCfirFile(lightTree.root, sourceFile, linesMapping)
        val surfaces = declarationBuilder.consumeCollectedMacroSurfaces()
        return file to surfaces
    }

    /** 从源码文本解析 LightTree 并构造 CFIR 文件。 */
    fun buildCfirFile(
        code: CharSequence,
        sourceFile: CjSourceFile,
        linesMapping: CjSourceFileLinesMapping,
    ): CfirFile = buildCfirFileWithSurfaces(code, sourceFile, linesMapping).first

    /** 从源码文本解析 LightTree，构造 CFIR 文件并返回 macro surface 列表。 */
    fun buildCfirFileWithSurfaces(
        code: CharSequence,
        sourceFile: CjSourceFile,
        linesMapping: CjSourceFileLinesMapping,
    ): Pair<CfirFile, List<MacroSurface>> {
        val parserDefinition = CangJieParserDefinition()
        val builder = PsiBuilderFactory.getInstance().createBuilder(
            parserDefinition,
            CangJieLexer(),
            code,
        )
        val lightTree = CangJieLightParser.parse(builder)
        return buildCfirFileWithSurfaces(lightTree, sourceFile, linesMapping)
    }
}
