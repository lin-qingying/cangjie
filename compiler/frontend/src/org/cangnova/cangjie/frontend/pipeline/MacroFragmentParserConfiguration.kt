package org.cangnova.cangjie.frontend.pipeline

import com.intellij.openapi.project.Project
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.builder.toParsingContext
import org.cangnova.cangjie.cfir.builder.macro.MacroPayloadTokenizer
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.lightTree.LightTreeRawCfirDeclarationBuilder
import org.cangnova.cangjie.cfir.lightTree.LightTree2Cfir
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentInput
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentParser
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentReparseResult
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroParameterSignature
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceParam
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceToken
import org.cangnova.cangjie.cfir.resolve.providers.macro.TokenBackedMacroFragmentParser
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.languageVersionSettings
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.useLightTree
import org.cangnova.cangjie.config.parserSettings
import org.cangnova.cangjie.parsing.CjParserSettings
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CjMacroDeclarationPrefix
import org.cangnova.cangjie.parsing.CjMacroFragmentContext
import org.cangnova.cangjie.parsing.CangJieLightMacroParseResult
import org.cangnova.cangjie.parsing.collectParsingDiagnosticPublication
import org.cangnova.cangjie.parsing.ParsingFileFacts
import org.cangnova.cangjie.parsing.createMacroFragmentParserSeed
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.CjAnnotation
import org.cangnova.cangjie.psi.CjMacroCodeFragment
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.psi
import org.cangnova.cangjie.source.isInSameSourceTree
import org.cangnova.cangjie.source.toCjPsiSourceElement
import org.cangnova.cangjie.source.toCjLightSourceElement
import org.cangnova.cangjie.cfir.builder.CfirRawSourceElementFactory
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentSourceMap

/**
 * 安装生产级 token-backed macro fragment parser。
 *
 * CLI、测试 facade、IDE host 都应通过这个入口接入同一条 re-tokenize + raw-builder
 * fragment reparse 链路。该函数只装配 parser，不创建 executor，不注入宏定义。
 */
fun CompilerConfiguration.installDefaultMacroFragmentParserFactory(project: Project? = null) {
    if (macroFragmentParserFactory != null) return

    val useLightTreeParser = useLightTree
    require(useLightTreeParser || project != null) {
        "PSI macro fragment parser requires an IntelliJ Project."
    }

    macroFragmentParserFactory = MacroFragmentParserFactory { session ->
        val invocationSettings = parserSettings
        TokenBackedMacroFragmentParser(
            reparse = { text, input ->
                if (useLightTreeParser) {
                    reparseLightTreeMacroFragment(session, text, input, invocationSettings)
                } else {
                    reparsePsiMacroFragment(project!!, session, text, input, invocationSettings)
                }
            },
            reTokenize = { tokens -> tokens.reTokenizeMacroSurfaceTokens() },
        )
    }
}

/**
 * 使用 PSI parser 重新解析宏返回的源码片段。
 */
private fun reparsePsiMacroFragment(
    project: Project,
    session: CfirSession,
    text: String,
    input: MacroFragmentInput,
    settings: CjParserSettings,
): MacroFragmentReparseResult {
    val fileFacts = input.fileFacts
        ?: return MacroFragmentReparseResult.Failure("Macro fragment has no original parsing file facts.")
    val owner = input.node
    val surface = owner.surface
    val ownerSnapshot = surface.replaceHandle.ownerSnapshot
    check(ownerSnapshot.packageFqName == surface.scopeContext.packageFqName) { "Macro surface and raw owner disagree on the host package." }
    val sourcePsi = when (val source = surface.sourceRange?.source) {
        is CjPsiSourceElement -> source.psi
        is CjLightSourceElement -> source.unwrapToCjPsiSourceElement()?.psi
        else -> null
    }
    val entry = macroFragmentEntry(input.mode, surface, ownerSnapshot)
        ?: return MacroFragmentReparseResult.Failure("Macro fragment has no parser facts for its replacement slot.")
    val seed = createMacroFragmentParserSeed(
        fileFacts,
        session.cjmpSettings.toParsingContext(session.languageVersionSettings).copy(initialFileRole = fileFacts.cjmpRole),
        entry,
        settings,
    ).copy(annotationModuleProvenance = input.annotationModuleProvenance(0))
    // 新 token 不继承宿主 builtin-module；seed 持有真实 ParseNodes 文法，context 仅供 PSI 名称解析。
    val fragment = CjMacroCodeFragment(project, "macroFragment.cj", text, sourcePsi, seed)
    val contents = fragment.getContentElement()
    val publication = fragment.collectParsingDiagnosticPublication()
    val parsedFacts = contents.fileFacts
    if (publication.diagnostics.any { it.isError }) return MacroFragmentReparseResult.SyntaxFailure(text, publication, parsedFacts)

    val builder = PsiRawCfirBuilder(
        session,
        annotationMetadata = input.annotationMetadata,
        sourceElementFactory = if (input.mode == MacroFragmentParser.Mode.CUSTOM_ANNOTATION) {
            input.customAnnotationSourceFactory()
        } else {
            input.sourceFactory(contents.toCjPsiSourceElement())
        },
    )
    if (input.mode == MacroFragmentParser.Mode.CUSTOM_ANNOTATION) {
        val original = surface.replaceHandle.annotationCarrier?.owner as? CfirValueParameter
        val containingSymbol = original?.containingDeclarationSymbol ?: surface.replaceHandle.annotationCarrier?.owner?.symbol
            ?: return MacroFragmentReparseResult.Failure("Custom annotation has no declaration owner.", parsedFacts)
        val annotation = contents.roots.singleOrNull() as? CjAnnotation
            ?: return MacroFragmentReparseResult.Failure("Custom annotation reparse did not produce one annotation.", parsedFacts)
        val annotationSourceOverride: CjSourceElement? = input.annotationSnapshot?.let { snapshot ->
            surface.annotationReparseSource(snapshot)
        }
        val payload = builder.buildAnnotationCallFragment(
            annotation = annotation,
            containingSymbol = containingSymbol,
            ownerSnapshot = ownerSnapshot,
            sourceOverride = annotationSourceOverride,
            argumentListSourceOverride = input.annotationSnapshot?.originalAnnotation?.argumentList?.source,
        )
        return MacroFragmentReparseResult.AnnotationSuccess(payload, parsedFacts, publication)
    }
    val payload = builder.buildMacroFragmentPayload(contents, ownerSnapshot)
        ?: return MacroFragmentReparseResult.Failure("A direct macro root has no raw CFIR conversion.", parsedFacts)
    return MacroFragmentReparseResult.Success(payload, parsedFacts, publication)
}

/**
 * 使用 light tree parser 重新解析宏返回的源码片段。
 */
private fun reparseLightTreeMacroFragment(
    session: CfirSession,
    text: String,
    input: MacroFragmentInput,
    settings: CjParserSettings,
): MacroFragmentReparseResult {
    val fileFacts = input.fileFacts
        ?: return MacroFragmentReparseResult.Failure("Macro fragment has no original parsing file facts.")
    val owner = input.node
    val surface = owner.surface
    val ownerSnapshot = surface.replaceHandle.ownerSnapshot
    check(ownerSnapshot.packageFqName == surface.scopeContext.packageFqName) { "Macro surface and raw owner disagree on the host package." }
    val entry = macroFragmentEntry(input.mode, surface, ownerSnapshot)
        ?: return MacroFragmentReparseResult.Failure("Macro fragment has no parser facts for its replacement slot.")
    val transaction = parseLightTreeMacroCodeFragment(session, text, fileFacts, entry, settings, input)
    if (input.mode != MacroFragmentParser.Mode.CUSTOM_ANNOTATION) {
        return LightTree2Cfir(session, session.cangjieScopeProvider, parserSettings = settings).buildMacroFragment(
            transaction, fileFacts.cjmpRole, ownerSnapshot, input.annotationMetadata,
            sourceElementFactory = { parsed -> input.sourceFactory(parsed.tree.root.toCjLightSourceElement(parsed.tree)) },
        )
    }
    return transaction.consume { fragment, diagnostics ->
        if (diagnostics.any { it.isError }) {
            return@consume MacroFragmentReparseResult.SyntaxFailure(fragment.inputText, fragment.diagnosticPublication, fragment.fileFacts)
        }
        val builder = LightTreeRawCfirDeclarationBuilder(
            annotationMetadata = input.annotationMetadata,
            session = session,
            baseScopeProvider = session.cangjieScopeProvider,
            tree = fragment.tree,
            source = fragment.inputText,
            sourceKind = fragment.fileFacts.sourceKind,
            macroFacts = fragment.macroFacts,
            nominalFacts = fragment.cjmpNominalFacts,
            declarationModifiers = fragment.declarationModifiers,
            sourceElementFactory = input.customAnnotationSourceFactory(),
        )
        val ownerDeclaration = surface.replaceHandle.annotationCarrier?.owner
            ?: return@consume MacroFragmentReparseResult.Failure("Custom annotation has no declaration owner.", fragment.fileFacts)
        val containingSymbol = (ownerDeclaration as? CfirValueParameter)?.containingDeclarationSymbol
            ?: ownerDeclaration.symbol
        val sourceOverride: CjSourceElement? = input.annotationSnapshot?.let { snapshot ->
            surface.annotationReparseSource(snapshot)
        }
        val argumentListSourceOverride = input.annotationSnapshot?.originalAnnotation?.argumentList?.source
        val annotation = fragment.roots.singleOrNull()
            ?.takeIf { it.tokenType == CjNodeTypes.ANNOTATION }
            ?: return@consume MacroFragmentReparseResult.Failure("Custom annotation reparse did not produce an annotation.", fragment.fileFacts)
        val payload = builder.buildAnnotationCallFragment(
            annotation = annotation,
            containingSymbol = containingSymbol,
            ownerSnapshot = ownerSnapshot,
            sourceOverride = sourceOverride,
            argumentListSourceOverride = argumentListSourceOverride,
        ) ?: return@consume MacroFragmentReparseResult.Failure("Custom annotation could not be converted to raw CFIR.", fragment.fileFacts)
        MacroFragmentReparseResult.AnnotationSuccess(payload, fragment.fileFacts, fragment.diagnosticPublication)
    }
}

/** 构建官方 ParseNodes 的精确入口；参数文法取真实签名，scope 与 parent 取 parser 冻结事实。 */
private fun macroFragmentEntry(
    mode: MacroFragmentParser.Mode,
    surface: org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface,
    ownerSnapshot: org.cangnova.cangjie.cfir.resolve.providers.macro.MacroRawOwnerSnapshot,
): CjMacroFragmentContext? {
    if (mode == MacroFragmentParser.Mode.CUSTOM_ANNOTATION) {
        return CjMacroFragmentContext.Annotation(surface.parsingFacts?.outerDeclIdent)
    }
    if (surface is MacroSurfaceParam) {
        val grammar = (ownerSnapshot.signature as? MacroParameterSignature.Declaration)?.grammar ?: return null
        return CjMacroFragmentContext.ParameterSlice(grammar, surface.parsingFacts?.outerDeclIdent)
    }
    val facts = surface.parsingFacts ?: return null
    return CjMacroFragmentContext.Nodes(
        scope = facts.scope,
        parent = facts.parent,
        outerDeclIdent = facts.outerDeclIdent,
        declarationPrefix = facts.declarationPrefix,
    )
}

/**
 * 为 annotation fragment 创建宿主源码范围。
 *
 * 重解析树的 source 仍必须保持为同一类 `CjSourceElement`；只有 LightTree
 * source 可以安全地复用节点并改写范围，不能把无文件的抽象 offset 对象传入
 * annotation builder，否则会丢失原始树能力。
 */
private fun org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurface.annotationReparseSource(
    snapshot: org.cangnova.cangjie.cfir.resolve.providers.macro.CfirAnnotationSlotSnapshot,
): CjSourceElement? {
    val range = sourceRange ?: return snapshot.originalAnnotation.source
    val source = range.source ?: return snapshot.originalAnnotation.source
    val endOffset = range.startOffset + snapshot.rawSyntax.length
    return when (source) {
        is org.cangnova.cangjie.source.CjLightSourceElement ->
            org.cangnova.cangjie.source.CjLightSourceElement(
                lighterASTNode = source.lighterASTNode,
                startOffset = range.startOffset,
                endOffset = endOffset,
                treeStructure = source.treeStructure,
                kind = source.kind,
            )

        is CjPsiSourceElement -> snapshot.originalAnnotation.source
        else -> snapshot.originalAnnotation.source
    }
}

/** 通过真实作用域、parent 与参数文法直接解析 macro roots。 */
private fun parseLightTreeMacroCodeFragment(
    session: CfirSession,
    text: String,
    fileFacts: ParsingFileFacts,
    entry: CjMacroFragmentContext,
    settings: CjParserSettings,
    input: MacroFragmentInput,
): CangJieLightMacroParseResult {
    val psiBuilder = CangJieLightParser.createBuilder(text)
    val seed = createMacroFragmentParserSeed(
        fileFacts,
        session.cjmpSettings.toParsingContext(session.languageVersionSettings).copy(initialFileRole = fileFacts.cjmpRole),
        entry,
        settings,
    ).copy(annotationModuleProvenance = input.annotationModuleProvenance(0))
    return CangJieLightParser.parseMacroCodeFragmentBuffered(psiBuilder, seed)
}

/** 前置 annotation 的 parser 模块只来自 sourceMap 确证的原 @ token 及原槽位身份。 */
private fun MacroFragmentInput.annotationModuleProvenance(fragmentStartOffset: Int): (Int) -> String? = { offset ->
    val start = offset - fragmentStartOffset
    val original = sourceMap?.takeIf { start >= 0 && start < it.text.length }?.originalRange(start, start + 1)
    val snapshots = if (mode == MacroFragmentParser.Mode.CUSTOM_ANNOTATION) {
        listOfNotNull(annotationSnapshot)
    } else inheritedAnnotationSnapshots
    snapshots.firstOrNull { snapshot ->
        original != null && snapshot.annotationSource.isInSameSourceTree(original) &&
            snapshot.annotationSource.startOffset == original.startOffset
    }?.originalAnnotation?.sourceModuleName
}

/** custom payload 已由原槽位 tokens 验证，模块事实按该槽位的完整 source 身份消费。 */
private fun MacroFragmentInput.customAnnotationSourceFactory(): CfirRawSourceElementFactory {
    val snapshot = checkNotNull(annotationSnapshot)
    return object : CfirRawSourceElementFactory by CfirRawSourceElementFactory.Original() {
        override fun annotationSourceModuleName(source: CjSourceElement?, parserModuleName: String): String {
            val original = snapshot.annotationSource
            return if (source != null && source.isInSameSourceTree(original) &&
                source.startOffset == original.startOffset && source.endOffset == original.endOffset
            ) snapshot.originalAnnotation.sourceModuleName else parserModuleName
        }
    }
}

private fun MacroFragmentInput.sourceFactory(anchor: CjSourceElement): CfirRawSourceElementFactory =
    MacroFragmentSourceElementFactory(
        checkNotNull(sourceMap), anchor, 0, node.surface.surfaceId, sourceMapping, inheritedAnnotationSnapshots,
    )

/**
 * 使用宏 payload tokenizer 对表面 token 文本重新分词。
 */
private fun List<MacroSurfaceToken>.reTokenizeMacroSurfaceTokens(): List<MacroSurfaceToken> {
    data class OriginSpan(
        val fragmentStart: Int,
        val fragmentEnd: Int,
        val sourceRange: org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceSourceRange?,
        val sourceStart: Int,
        val sourceEnd: Int,
        val exact: Boolean,
    )

    val sourceMap = MacroFragmentSourceMap.materialize(this)
    val text = sourceMap.text
    val origins = mutableListOf<OriginSpan>()
    var inputOffset = 0
    for (token in this) {
        val tokenEnd = inputOffset + token.text.length
        val sourceRange = token.sourceRange
        val exact = token.isSourceToken
        origins += OriginSpan(
            fragmentStart = inputOffset,
            fragmentEnd = tokenEnd,
            sourceRange = sourceRange,
            sourceStart = if (exact) token.startOffset else 0,
            sourceEnd = if (exact) token.endOffset else 0,
            exact = exact,
        )
        inputOffset = tokenEnd
    }

    val payloadTokens = MacroPayloadTokenizer.tokenize(text, baseOffset = 0)
    return payloadTokens.map { token ->
        val parts = origins.filter { it.fragmentEnd > token.startOffset && it.fragmentStart < token.endOffset }
        val ranges = parts.mapNotNull { it.sourceRange }
        val sharedSource = ranges.firstOrNull()?.source
        val combinedRange = when {
            ranges.isEmpty() -> null
            ranges.all { it === ranges.first() } -> ranges.first()
            sharedSource != null && ranges.all { it.source === sharedSource } ->
                org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceSourceRange(
                    source = sharedSource,
                    startOffset = ranges.minOf { it.startOffset },
                    endOffset = ranges.maxOf { it.endOffset },
                )
            else -> null
        }
        var exactStart: Int? = null
        var exactEnd: Int? = null
        if (parts.isNotEmpty() && parts.all { it.exact } && parts.all { it.sourceRange?.source === sharedSource }) {
            val firstPart = parts.first()
            val firstOverlap = maxOf(token.startOffset, firstPart.fragmentStart)
            exactStart = firstPart.sourceStart + (firstOverlap - firstPart.fragmentStart)
            var expectedSourceEnd = exactStart
            for (part in parts) {
                val overlapStart = maxOf(token.startOffset, part.fragmentStart)
                val overlapEnd = minOf(token.endOffset, part.fragmentEnd)
                val mappedStart = part.sourceStart + (overlapStart - part.fragmentStart)
                val mappedEnd = part.sourceStart + (overlapEnd - part.fragmentStart)
                if (mappedStart != expectedSourceEnd) {
                    exactStart = null
                    break
                }
                expectedSourceEnd = mappedEnd
            }
            if (exactStart != null) exactEnd = expectedSourceEnd
        }
        MacroSurfaceToken(
            text = token.text,
            startOffset = exactStart ?: token.startOffset,
            endOffset = exactEnd ?: token.endOffset,
            kindName = token.kindName,
            sourceRange = combinedRange,
            isSourceToken = exactStart != null,
            sourceSpans = sourceMap.spansForRange(token.startOffset, token.endOffset),
            isTrivia = token.kindName in CfirRawSourceElementFactory.TRIVIA_KINDS,
        )
    }
}
