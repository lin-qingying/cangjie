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
import org.cangnova.cangjie.cfir.diagnostics.CjDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.CjSyntaxErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticContext
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface
import org.cangnova.cangjie.cfir.scopes.CfirScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement
import org.cangnova.cangjie.source.CjSourceFileLinesMapping
import org.cangnova.cangjie.source.readSourceFileWithMapping

class LightTree2Cfir(
    val session: CfirSession,
    private val scopeProvider: CfirScopeProvider,

    private val diagnosticsReporter: DiagnosticReporter? = null,
    private val bodyBuildingMode: BodyBuildingMode = BodyBuildingMode.NORMAL,
) {
    fun buildCfirFile(path: Path): CfirFile {
        return buildCfirFile(path.toFile())
    }

    fun buildCfirFile(file: File): CfirFile {
        val sourceFile = CjIoFileSourceFile(file)
        val (code, linesMapping) = file.inputStream().reader(Charsets.UTF_8).use {
            it.readSourceFileWithMapping()
        }
        return buildCfirFile(code, sourceFile, linesMapping)
    }

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

    fun buildCfirFile(
        code: CharSequence,
        sourceFile: CjSourceFile,
        linesMapping: CjSourceFileLinesMapping,
    ): CfirFile = buildCfirFileWithSurfaces(code, sourceFile, linesMapping).first

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
        val lightTree = CangJieLightParser.parse(builder, makeErrorListener(sourceFile))
        return buildCfirFileWithSurfaces(lightTree, sourceFile, linesMapping)
    }

    private fun makeErrorListener(sourceFile: CjSourceFile): CangJieLightParser.LightTreeParsingErrorListener? {
        val reporter = diagnosticsReporter ?: return null
        val diagnosticContext = object : DiagnosticContext {
            override val languageVersionSettings = session.languageVersionSettings
            override val containingFilePath: String? = sourceFile.path
            override fun isDiagnosticSuppressed(diagnostic: CjDiagnostic): Boolean = false
        }
        return CangJieLightParser.LightTreeParsingErrorListener { startOffset, endOffset, message ->
            val factory = CjSyntaxErrors.factoryForParserMessage(message) ?: return@LightTreeParsingErrorListener
            reporter.reportOn(
                CjOffsetsOnlySourceElement(startOffset, endOffset),
                factory,
                diagnosticContext,
            )
        }
    }
}
