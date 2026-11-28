package org.cangnova.cangjie.cfir.lightTree

import java.io.File
import java.nio.file.Path
import org.cangnova.cangjie.CjIoFileSourceFile
import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.cfir.builder.BodyBuildingMode
import org.cangnova.cangjie.cfir.builder.toParsingContext
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.publishParsingFileFacts
import org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationMetadataBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.PreMacroCfirFile
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentReparseResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroRawOwnerSnapshot
import org.cangnova.cangjie.cfir.scopes.CfirScopeProvider
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.ParsedCangJieLightFile
import org.cangnova.cangjie.parsing.CangJieLightParseResult
import org.cangnova.cangjie.parsing.CangJieLightMacroParseResult
import org.cangnova.cangjie.parsing.CjmpFileRole
import org.cangnova.cangjie.parsing.CangJieLightDiagnosticOwnership
import org.cangnova.cangjie.parsing.MacroParsingFacts
import org.cangnova.cangjie.parsing.CjParserSettings
import org.cangnova.cangjie.source.CjSourceFileLinesMapping
import org.cangnova.cangjie.source.readSourceFileWithMapping
import org.cangnova.cangjie.source.toSourceLinesMapping

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
    /** Raw 只保留最终 parser 产品，编译单元的发布 owner 在收齐文件后决定 suppression/LEX 门。 */
    private val parserSettings: CjParserSettings = CjParserSettings(session.languageVersionSettings.languageVersion),
) {
    /** 在目标 session 中消费完整宏片段事务；入口角色必须由宿主显式提供。 */
    fun buildMacroFragment(
        transaction: CangJieLightMacroParseResult,
        entryFileRole: CjmpFileRole,
        ownerSnapshot: MacroRawOwnerSnapshot,
        annotationMetadata: CfirAnnotationMetadataBuilder,
    ): MacroFragmentReparseResult {
        val target = session.cjmpSettings.toParsingContext(session.languageVersionSettings).copy(initialFileRole = entryFileRole)
        return transaction.forContext(target, entryFileRole).consume { fragment, diagnostics ->
            if (diagnostics.any { it.isError }) {
                return@consume MacroFragmentReparseResult.SyntaxFailure(fragment.inputText, fragment.diagnosticPublication, fragment.fileFacts)
            }
            val builder = LightTreeRawCfirDeclarationBuilder(
                session = session,
                baseScopeProvider = scopeProvider,
                tree = fragment.tree,
                source = fragment.inputText,
                sourceKind = fragment.fileFacts.sourceKind,
                bodyBuildingMode = bodyBuildingMode,
                macroFacts = fragment.macroFacts,
                nominalFacts = fragment.cjmpNominalFacts,
                declarationModifiers = fragment.declarationModifiers,
                annotationMetadata = annotationMetadata,
            )
            val payload = builder.buildMacroFragmentPayload(fragment.roots, fragment.parsingInfo, ownerSnapshot)
                ?: return@consume MacroFragmentReparseResult.Failure("A direct macro root has no raw CFIR conversion.", fragment.fileFacts)
            MacroFragmentReparseResult.Success(payload, fragment.fileFacts, fragment.diagnosticPublication)
        }
    }

    /** 从磁盘路径读取源码并构造 CFIR 文件。 */
    fun buildPreMacroCfirFile(path: Path): PreMacroCfirFile {
        return buildPreMacroCfirFile(path.toFile())
    }

    /** 从磁盘文件读取源码并构造 CFIR 文件。 */
    fun buildPreMacroCfirFile(file: File): PreMacroCfirFile {
        val sourceFile = CjIoFileSourceFile(file)
        val (code, linesMapping) = file.inputStream().reader(Charsets.UTF_8).use {
            it.readSourceFileWithMapping()
        }
        return buildPreMacroCfirFile(code, sourceFile, linesMapping)
    }

    /**
     * 返回完整 raw 产品；macro surfaces 和同次 metadata 事务不能在文件 API 中丢弃。
     */
    fun buildPreMacroCfirFile(
        parsedFile: ParsedCangJieLightFile,
        sourceFile: CjSourceFile,
        linesMapping: CjSourceFileLinesMapping,
    ): PreMacroCfirFile {
        parsedFile.requireConsistentInput()
        require(parsedFile.isCompleteFile) { "A file construction requires a complete file parse." }
        require(parsedFile.diagnosticOwnership != CangJieLightDiagnosticOwnership.BUFFERED) {
            "A buffered parse must be consumed together with its diagnostics."
        }
        val targetContext = session.cjmpSettings.toParsingContext(session.languageVersionSettings)
        if (parsedFile.cjmpContext != targetContext || parsedFile.settings != parserSettings) {
            require(parsedFile.diagnosticOwnership == CangJieLightDiagnosticOwnership.UNOBSERVED) {
                "A parse with externally delivered diagnostics cannot be reused in another compilation context."
            }
            val code = parsedFile.tree.toString(parsedFile.tree.root)
            val builder = CangJieLightParser.createBuilder(code)
            val reparsed = CangJieLightParser.parseBuffered(
                builder, parsedFile.inputLanguageModuleName, parsedFile.fileFacts.sourceKind, targetContext, sourceFile,
                lexicalInput = parsedFile.lexicalInput,
                settings = parserSettings,
            )
            return buildPreMacroCfirFile(reparsed, sourceFile, linesMapping)
        }
        val code = parsedFile.tree.toString(parsedFile.tree.root).toString()
        requireMatchingLinesMapping(code, linesMapping)
        return buildAuthoritativeFile(parsedFile, sourceFile, linesMapping, code)
    }

    private fun buildAuthoritativeFile(
        parsedFile: ParsedCangJieLightFile,
        sourceFile: CjSourceFile,
        linesMapping: CjSourceFileLinesMapping,
        code: String,
    ): PreMacroCfirFile {
        val lightTree = parsedFile.tree
        val declarationBuilder = LightTreeRawCfirDeclarationBuilder(
            annotationMetadata = CfirAnnotationMetadataBuilder(),
            session = session,
            baseScopeProvider = scopeProvider,
            tree = lightTree,
            source = code,
            bodyBuildingMode = bodyBuildingMode,
            // `.cj.d` 无体规则与 parser 共用实际来源，不从物理路径重新推导。
            sourceKind = parsedFile.fileFacts.sourceKind,
            macroFacts = parsedFile.macroFacts,
            nominalFacts = parsedFile.cjmpNominalFacts,
            declarationModifiers = parsedFile.declarationModifiers,
        )
        val file = declarationBuilder.buildCfirFile(lightTree.root, sourceFile, linesMapping)
        parsedFile.requireConsistentInput()
        file.publishParsingFileFacts(parsedFile.fileFacts, parsedFile.sourceContext)
        val surfaces = declarationBuilder.consumeCollectedMacroSurfaces()
        return PreMacroCfirFile(file, surfaces, declarationBuilder.annotationMetadata, parsedFile.diagnosticPublication, parsedFile.settings)
    }

    /** 未发布诊断的解析事务可在 session 变化时整体失效，并只提交最终树的诊断。 */
    fun buildPreMacroCfirFile(
        parsed: CangJieLightParseResult,
        sourceFile: CjSourceFile,
        linesMapping: CjSourceFileLinesMapping,
    ): PreMacroCfirFile {
        val targetContext = session.cjmpSettings.toParsingContext(session.languageVersionSettings)
        val original = parsed.file
        original.requireConsistentInput()
        require(original.isCompleteFile) { "A file construction transaction requires a complete file parse." }
        // 节点偏移只属于实际解析文本，不能重新读取带 CRLF 的磁盘原文。
        val code = original.tree.toString(original.tree.root).toString()
        requireMatchingLinesMapping(code, linesMapping)
        val finalParse = if (original.cjmpContext == targetContext && original.settings == parserSettings) parsed else {
            val builder = CangJieLightParser.createBuilder(code)
            CangJieLightParser.parseBuffered(
                builder, original.inputLanguageModuleName, original.fileFacts.sourceKind, targetContext, sourceFile,
                lexicalInput = original.lexicalInput,
                settings = parserSettings,
            )
        }
        if (finalParse !== parsed) {
            check(finalParse.file.tree.toString(finalParse.file.tree.root).toString() == code) {
                "A reparse must retain the same source text and coordinate system."
            }
        }
        parsed.claimForConstruction()
        finalParse.file.requireConsistentInput()
        return buildAuthoritativeFile(finalParse.file, sourceFile, linesMapping, code)
    }

    /** 从源码文本解析 LightTree，构造 CFIR 文件并返回 macro surface 列表。 */
    fun buildPreMacroCfirFile(
        code: CharSequence,
        sourceFile: CjSourceFile,
        linesMapping: CjSourceFileLinesMapping,
    ): PreMacroCfirFile {
        val builder = CangJieLightParser.createBuilder(code)
        // 解析模式随文件种类：`.cj.d` 走声明模式，体可以缺失。
        // 这里必须显式传入——LightTree 入口不会自己看文件名。
        val parsed = CangJieLightParser.parseBuffered(
            builder,
            sourceKind = sourceFile.sourceKind,
            cjmpContext = session.cjmpSettings.toParsingContext(session.languageVersionSettings),
            sourceFile = sourceFile,
            settings = parserSettings,
        )
        return buildPreMacroCfirFile(parsed, sourceFile, linesMapping)
    }

    /** source 坐标必须与同一棵解析树的文本一致，不能混用磁盘原文或其他版本的映射。 */
    private fun requireMatchingLinesMapping(code: CharSequence, mapping: CjSourceFileLinesMapping) {
        val expected = code.toSourceLinesMapping()
        require(mapping.lastOffset == expected.lastOffset && mapping.linesCount == expected.linesCount &&
            (0 until expected.linesCount).all { mapping.getLineStartOffset(it) == expected.getLineStartOffset(it) }) {
            "Source line mapping must describe the parsed text."
        }
    }
}
