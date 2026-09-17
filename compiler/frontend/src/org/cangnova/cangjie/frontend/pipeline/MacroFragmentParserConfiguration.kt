package org.cangnova.cangjie.frontend.pipeline

import com.intellij.lang.LighterASTNode
import com.intellij.lang.PsiBuilder
import com.intellij.lang.PsiBuilderFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.tree.IElementType
import com.intellij.util.diff.FlyweightCapableTreeStructure
import org.cangnova.cangjie.cfir.builder.PsiRawCfirBuilder
import org.cangnova.cangjie.cfir.builder.macro.MacroPayloadTokenizer
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.lightTree.LightTreeRawCfirExpressionBuilder
import org.cangnova.cangjie.cfir.lightTree.LightTreeRawCfirDeclarationBuilder
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroCallNode
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentInput
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroFragmentParser
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceParam
import org.cangnova.cangjie.cfir.resolve.providers.macro.MacroSurfaceToken
import org.cangnova.cangjie.cfir.resolve.providers.macro.TokenBackedMacroFragmentParser
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import org.cangnova.cangjie.config.CompilerConfiguration
import org.cangnova.cangjie.config.useLightTree
import org.cangnova.cangjie.lexer.CangJieLexer
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.parsing.CangJieLightParser
import org.cangnova.cangjie.parsing.CangJieParserDefinition
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.CjAnnotations
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.source.CjLightSourceElement
import org.cangnova.cangjie.source.CjPsiSourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.psi

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
        TokenBackedMacroFragmentParser(
            reparse = { text, input ->
                if (useLightTreeParser) {
                    reparseLightTreeMacroFragment(session, text, input)
                } else {
                    reparsePsiMacroFragment(project!!, session, text, input)
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
): Any? {
    val mode = input.mode
    val owner = input.node
    val surface = owner.surface
    val packageFqName = surface.scopeContext.packageFqName
    val sourcePsi = when (val source = surface.sourceRange?.source) {
        is CjPsiSourceElement -> source.psi
        is CjLightSourceElement -> source.unwrapToCjPsiSourceElement()?.psi
        else -> null
    }
    // 新 token 不继承宿主的 builtin-module；名称解析仍沿原始 PSI context 进行。
    val psiFactory = CjPsiFactory.forMacroExpansion(project, sourcePsi)
    val builder = PsiRawCfirBuilder(session)

    return when {
        mode == MacroFragmentParser.Mode.CUSTOM_ANNOTATION -> {
            val original = surface.replaceHandle.annotationCarrier?.owner as? CfirValueParameter
            val containingSymbol = original?.containingDeclarationSymbol ?: surface.replaceHandle.annotationCarrier?.owner?.symbol
                ?: return null
            val annotationText = text.withAbsoluteSourceStart(surface.sourceRange?.startOffset ?: 0)
            val annotationFile = psiFactory.createAnnotationOnlyFile(
                "$annotationText func __macro_annotation__() {}",
            )
            val annotation = PsiTreeUtil.findChildOfType(annotationFile, CjAnnotations::class.java)
                ?.entries
                ?.singleOrNull()
                ?: return null
            val annotationSourceOverride: CjSourceElement? = input.annotationSnapshot?.let { snapshot ->
                surface.annotationReparseSource(snapshot)
            }
            builder.buildAnnotationCallInPackage(
                annotation = annotation,
                containingSymbol = containingSymbol,
                packageFqName = packageFqName,
                sourceOverride = annotationSourceOverride,
                argumentListSourceOverride = input.annotationSnapshot?.originalAnnotation?.argumentList?.source,
            )
        }
        surface is MacroSurfaceParam -> {
            val parameter = psiFactory.createSingleParameter(text) ?: return null
            val original = surface.replaceHandle.carrier as? CfirValueParameter ?: return null
            builder.buildValueParameterInPackage(
                parameter = parameter,
                containingSymbol = original.containingDeclarationSymbol,
                packageFqName = packageFqName,
            )
        }
        mode == MacroFragmentParser.Mode.EXPRESSION -> {
            val expression = psiFactory.createExpressionIfPossible(text) ?: return null
            builder.buildExpressionInPackage(expression, packageFqName)
        }
        else -> {
            val fragment = psiFactory.createFile(text)
            builder.buildDeclarationFragmentInPackage(fragment, packageFqName)
        }
    }
}

/**
 * 使用 light tree parser 重新解析宏返回的源码片段。
 */
private fun reparseLightTreeMacroFragment(
    session: CfirSession,
    text: String,
    input: MacroFragmentInput,
): Any? {
    val mode = input.mode
    val owner = input.node
    val surface = owner.surface
    val packageFqName = surface.scopeContext.packageFqName
    return when {
        mode == MacroFragmentParser.Mode.CUSTOM_ANNOTATION -> {
            val ownerDeclaration = surface.replaceHandle.annotationCarrier?.owner ?: return null
            val containingSymbol = (ownerDeclaration as? CfirValueParameter)?.containingDeclarationSymbol
                ?: ownerDeclaration.symbol
            val parsed = parseLightTreeAnnotationFragment(
                session = session,
                text = text,
                sourceStartOffset = surface.sourceRange?.startOffset ?: 0,
            )
            val sourceOverride: CjSourceElement? = input.annotationSnapshot?.let { snapshot ->
                surface.annotationReparseSource(snapshot)
            }
            val argumentListSourceOverride = input.annotationSnapshot?.originalAnnotation?.argumentList?.source
            val annotation = parsed.tree.findFirst(CjNodeTypes.ANNOTATION) ?: return null
            parsed.builder.buildAnnotationCallInPackage(
                annotation = annotation,
                containingSymbol = containingSymbol,
                packageFqName = packageFqName,
                sourceOverride = sourceOverride,
                argumentListSourceOverride = argumentListSourceOverride,
            )
        }
        surface is MacroSurfaceParam -> {
            val original = surface.replaceHandle.carrier as? CfirValueParameter ?: return null
            val parsed = parseLightTreeFragment(session, "func __macro_fragment__($text) {}")
            val parameter = parsed.tree.findFirst(CjNodeTypes.VALUE_PARAMETER) ?: return null
            parsed.builder.buildValueParameterInPackage(
                parameter = parameter,
                containingSymbol = original.containingDeclarationSymbol,
                packageFqName = packageFqName,
            )
        }
        mode == MacroFragmentParser.Mode.EXPRESSION -> {
            val parsed = parseLightTreeFragment(
                session,
                "func __macro_fragment__(): Unit {\nlet __macro_fragment_value__ =\n$text\n}",
            )
            val variable = parsed.tree.findFirst(CjNodeTypes.VARIABLE) ?: return null
            val expression = parsed.tree.findFirstExpressionAfterEquals(variable) ?: return null
            parsed.builder.buildExpressionInPackage(expression, packageFqName)
        }
        else -> {
            val parsed = parseLightTreeFragment(session, text)
            parsed.builder.buildDeclarationFragmentInPackage(parsed.tree.root, packageFqName)
        }
    }
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

/**
 * light tree 宏片段解析结果。
 */
private data class ParsedLightTreeFragment(
    /**
     * 解析得到的 light tree。
     */
    val tree: FlyweightCapableTreeStructure<LighterASTNode>,
    /**
     * 基于该 light tree 构造 CFIR 的声明 builder。
     */
    val builder: LightTreeRawCfirDeclarationBuilder,
)

/**
 * 将文本解析为 light tree 片段并创建对应 raw CFIR builder。
 */
private fun parseLightTreeFragment(
    session: CfirSession,
    text: String,
): ParsedLightTreeFragment = createParsedLightTreeFragment(session, text) { builder ->
    // 对齐 ReplaceEachMacro 的新 token parser，不调用 SetModuleName。
    CangJieLightParser.parse(builder, languageModuleName = "")
}

/** 使用 annotation-only 语法解析 custom annotation 宏展开结果。 */
private fun parseLightTreeAnnotationFragment(
    session: CfirSession,
    text: String,
    sourceStartOffset: Int,
): ParsedLightTreeFragment {
    // annotation 在仓颉语法中必须附着于声明。这里与 PSI createAnnotations 使用同一建模：
    // 用语法载体声明形成标准 ANNOTATION 子树，最终只提取 annotation payload。
    val fragmentText = text.withAbsoluteSourceStart(sourceStartOffset) + " func __macro_annotation__() {}"
    return createParsedLightTreeFragment(session, fragmentText) { builder ->
        CangJieLightParser.parseAnnotationOnly(builder, languageModuleName = "")
    }
}

/**
 * 为临时重解析文本保留其在宿主文件中的绝对起点。
 *
 * PSI 与 LightTree 都从同一个带前导空格的文本开始构造 source；这样宏参数中
 * 重新出现的 annotation 不会把临时树的局部 offset 泄漏到宿主文件。
 */
private fun String.withAbsoluteSourceStart(sourceStartOffset: Int): String {
    require(sourceStartOffset >= 0) { "Annotation source start offset must be non-negative." }
    if (sourceStartOffset == 0) return this
    return buildString(sourceStartOffset + length) {
        repeat(sourceStartOffset) { append(' ') }
        append(this@withAbsoluteSourceStart)
    }
}

/** 统一创建宏片段 PsiBuilder、LightTree 与对应 raw CFIR builder。 */
private fun createParsedLightTreeFragment(
    session: CfirSession,
    text: String,
    parse: (PsiBuilder) -> FlyweightCapableTreeStructure<LighterASTNode>,
): ParsedLightTreeFragment {
    val parserDefinition = CangJieParserDefinition()
    val psiBuilder = PsiBuilderFactory.getInstance().createBuilder(
        parserDefinition,
        CangJieLexer(),
        text,
    )
    val lightTree = parse(psiBuilder)
    return ParsedLightTreeFragment(
        tree = lightTree,
        builder = LightTreeRawCfirDeclarationBuilder(
            session = session,
            baseScopeProvider = session.cangjieScopeProvider,
            tree = lightTree,
            source = text,
        ),
    )
}

/**
 * 使用 PSI factory 从文本中解析单个参数。
 */
private fun CjPsiFactory.createSingleParameter(text: String): CjParameter? {
    return runCatching {
        createParameterList("($text)").parameters.singleOrNull()
    }.getOrNull()
}

/**
 * 深度优先查找第一个匹配任意 token 类型的 light tree 节点。
 */
private fun FlyweightCapableTreeStructure<LighterASTNode>.findFirst(
    vararg tokenTypes: IElementType,
): LighterASTNode? {
    val accepted = tokenTypes.toSet()
    fun visit(node: LighterASTNode): LighterASTNode? {
        if (node.tokenType in accepted) return node
        val childrenRef = Ref<Array<LighterASTNode>>()
        val count = getChildren(node, childrenRef)
        val children = childrenRef.get() ?: LighterASTNode.EMPTY_ARRAY
        try {
            for (index in 0 until count) {
                visit(children[index])?.let { return it }
            }
        } finally {
            disposeChildren(children, count)
        }
        return null
    }
    return visit(root)
}

/**
 * 在变量节点中查找等号后的第一个表达式节点。
 */
private fun FlyweightCapableTreeStructure<LighterASTNode>.findFirstExpressionAfterEquals(
    node: LighterASTNode,
): LighterASTNode? {
    var afterEquals = false
    val childrenRef = Ref<Array<LighterASTNode>>()
    val count = getChildren(node, childrenRef)
    val children = childrenRef.get() ?: LighterASTNode.EMPTY_ARRAY
    try {
        for (index in 0 until count) {
            val child = children[index]
            if (child.tokenType == CjTokens.EQ) {
                afterEquals = true
            } else if (afterEquals) {
                if (LightTreeRawCfirExpressionBuilder.isExpressionToken(child.tokenType)) {
                    return child
                }
                findFirstExpressionAfterEquals(child)?.let { return it }
            }
        }
    } finally {
        disposeChildren(children, count)
    }
    return null
}

/**
 * 使用宏 payload tokenizer 对表面 token 文本重新分词。
 */
private fun List<MacroSurfaceToken>.reTokenizeMacroSurfaceTokens(): List<MacroSurfaceToken> {
    val payloadTokens = MacroPayloadTokenizer.tokenize(joinToString(separator = "") { it.text }, baseOffset = 0)
    return payloadTokens.map { token ->
        MacroSurfaceToken(
            text = token.text,
            startOffset = token.startOffset,
            endOffset = token.endOffset,
            kindName = token.kindName,
        )
    }
}
