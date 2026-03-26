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
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.scopes.CfirScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.source.CjSourceFileLinesMapping
import org.cangnova.cangjie.source.readSourceFileWithMapping

class LightTree2Cfir(
    val session: CfirSession,
    private val scopeProvider: CfirScopeProvider,

    @Suppress("UNUSED_PARAMETER")
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
    ): CfirFile {
        val code = sourceFile.getContentsAsStream().reader(Charsets.UTF_8).use { it.readText() }
        return LightTreeRawCfirDeclarationBuilder(
            session = session,
            baseScopeProvider = scopeProvider,
            tree = lightTree,
            source = code,
            bodyBuildingMode = bodyBuildingMode,
        ).buildCfirFile(lightTree.root, sourceFile, linesMapping)
    }

    fun buildCfirFile(
        code: CharSequence,
        sourceFile: CjSourceFile,
        linesMapping: CjSourceFileLinesMapping,
    ): CfirFile {
        val parserDefinition = CangJieParserDefinition()
        val builder = PsiBuilderFactory.getInstance().createBuilder(
            parserDefinition,
            CangJieLexer(),
            code,
        )
        val lightTree = CangJieLightParser.parse(builder)
        return buildCfirFile(lightTree, sourceFile, linesMapping)
    }
}
