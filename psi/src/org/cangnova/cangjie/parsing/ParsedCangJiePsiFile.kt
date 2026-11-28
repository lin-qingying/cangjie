package org.cangnova.cangjie.parsing

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.Key
import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.util.LocalTimeCounter
import org.cangnova.cangjie.messages.ParsingDiagnostic
import org.cangnova.cangjie.messages.CjParsingDiagnosticScope
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.parserLanguageModuleName
import java.util.Collections
import org.cangnova.cangjie.CjInMemoryTextSourceFile
import org.cangnova.cangjie.CjSourceFile
import org.cangnova.cangjie.source.CjSourceFileLinesMapping
import org.cangnova.cangjie.source.toSourceLinesMapping

/**
 * 一次目标编译语境的真实 PSI 产品。树、声明属性、诊断和后继 seed 都属于本次完整解析；
 * 源对象对应只用于从原 PSI 进入目标树，不允许据此复用原属性、符号或诊断。
 */
class ParsedCangJiePsiFile internal constructor(
    val originFile: CjFile,
    val file: CjFile,
    val inputSeed: CjParserSeed,
    private val entrySeed: CjParserSeed,
    val actualInputSeed: CjParserSeed,
    val diagnosticPublication: org.cangnova.cangjie.messages.CjParsingDiagnosticPublication,
    private val originText: String,
    private val originStamp: Long,
    private val originSourceView: CjPsiSourceView,
) {
    val diagnostics: List<ParsingDiagnostic> = diagnosticPublication.diagnostics
    val lexicalInput: CjLexicalSourceInput = checkNotNull(actualInputSeed.lexicalInput)
    /**
     * 编译产品读取本代冻结内容，但源文件身份属于真实来源。目标 PSI 无 physical VirtualFile，
     * 不能因它的路径缺失而丢失文件错误门控、后置 CFA 或序列化来源位置。
     */
    val sourceFile: CjSourceFile = CjInMemoryTextSourceFile(
        originFile.name, originFile.virtualFile?.path, lexicalInput.source.text, actualInputSeed.sourceKind,
    )
    /** 行列和源码内容同代冻结，保留原始 CR/LF 与 UTF-16 偏移，不依赖非物理 target 的 Document。 */
    val sourceFileLinesMapping: CjSourceFileLinesMapping = lexicalInput.source.text.toSourceLinesMapping()
    /** 目标解析的根输出独立保存；actualInputSeed 只保留实际入口与父区域。 */
    val diagnosticScope: CjParsingDiagnosticScope = checkNotNull(file.parsingDiagnosticScopeOrNull())
    private val originRoot: ASTNode = originFile.node
    private val targetRoot: ASTNode = file.node
    private val targetStamp: Long = file.modificationStamp
    private val targetRegion: CjmpParsedRoleRegion = checkNotNull(file.parsedRoleRegion)
    private val originFragmentInputs = (originFile as? CjCodeFragment)?.let(::CjCodeFragmentInputs)
    private val targetFragmentInputs = (file as? CjCodeFragment)?.let(::CjCodeFragmentInputs)
    private val correspondence: CjPsiGrammarCorrespondence = CjPsiGrammarCorrespondence(
        originFile, file, checkNotNull(actualInputSeed.sourceContext), lexicalInput, originSourceView,
    )

    init {
        check(targetRegion.entrySeed === entrySeed)
        requireConsistentInput()
    }

    /** 缺少唯一且完整的结构对应时返回缺失，不按名称、旧路径或偏移挑选另一个 owner。 */
    fun correspondingElementOrNull(element: PsiElement): PsiElement? = ReadAction.compute<PsiElement?, RuntimeException> {
        requireConsistentInput()
        if (element.containingFile !== originFile) null else correspondence.targetFor(element)
    }

    /** 验证、实际构建以及提交前复验必须共享一次平台读事务。 */
    fun <R> withTargetFile(action: (CjFile) -> R): R = ReadAction.compute<R, RuntimeException> {
        requireConsistentInput()
        val result = action(file)
        requireConsistentInput()
        result
    }

    /** 只供源码展示与编辑定位；编译器内部必须继续持有目标树中的真实对象。 */
    fun originalElementOrNull(element: PsiElement): PsiElement? = ReadAction.compute<PsiElement?, RuntimeException> {
        requireConsistentInput()
        if (element.containingFile !== file) null else correspondence.originFor(element)
    }

    /**
     * 在产品仍有效时一次性登记真实对象的导航对应。LL 修改服务在原树变化后使用已登记的
     * 原 owner 识别修改，不再调用失效产品；此表不授予旧属性、header 或 symbol 的保留资格。
     */
    fun captureOriginalElements(): Map<PsiElement, PsiElement> = ReadAction.compute<Map<PsiElement, PsiElement>, RuntimeException> {
        requireConsistentInput()
        correspondence.captureOriginalElements().also { requireConsistentInput() }
    }

    /** 原始 Document 或目标树发生修改后，本代产品整体失效，不重解析兜底或回贴属性。 */
    fun requireConsistentInput() {
        originSourceView.requireCurrent()
        check(originFile.isValid && file.isValid)
        check(originFile.node === originRoot && file.node === targetRoot) {
            "A PSI correspondence belongs to its actual source and target AST root objects."
        }
        check(originFile.modificationStamp == originStamp && file.modificationStamp == targetStamp) {
            "A PSI compilation product requires the same immutable source and target generations."
        }
        check(originFile.node.chars.toString() == originText && file.node.chars.toString() == originText)
        check(originFile.viewProvider.contents.toString() == originText && file.viewProvider.contents.toString() == originText) {
            "An uncommitted Document change invalidates the source compilation input generation."
        }
        check(file.getCopyableUserData(ROOT_PARSER_SEED_KEY) === actualInputSeed)
        check(file.parsingDiagnosticScopeOrNull() === diagnosticScope)
        check(file.parsedRoleRegion === targetRegion && targetRegion.entrySeed === entrySeed)
        check(targetRegion.lexicalInput === lexicalInput && file.cjmpParsingContext == entrySeed.cjmpContext)
        checkNotNull(actualInputSeed.sourceContext).requireInput(file.node.chars)
        originFragmentInputs?.requireUnchanged()
        targetFragmentInputs?.requireUnchanged()
    }
}

private val PSI_COMPILATION_RESULT_KEY: Key<ParsedCangJiePsiFile> = Key.create("CANGJIE_PSI_COMPILATION_RESULT")

/** 目标树可以被本代 raw/lazy 构建再次读取；普通副本没有这份产品身份。 */
fun CjFile.compilationParsingResultOrNull(): ParsedCangJiePsiFile? = getUserData(PSI_COMPILATION_RESULT_KEY)

internal fun parsePsiCompilationFile(origin: CjFile, target: CjmpParsingContext.Compilation,
    settings: CjParserSettings?): ParsedCangJiePsiFile =
    ReadAction.compute<ParsedCangJiePsiFile, RuntimeException> {
    require(origin !is CjCodeFragment) { "A native code fragment requires its complete target entry seed and context." }
    val frozen = freezePsiInput(origin)
    val requested = frozen.seed.copy(
        cjmpContext = target, fileRole = target.initialFileRole, diagnosticScope = null,
        sourceContext = frozen.context, lexicalInput = frozen.input,
        settings = settings ?: frozen.seed.settings,
    )
    val file = PsiFileFactory.getInstance(origin.project).createFileFromText(
        origin.name, origin.viewProvider.fileType, frozen.text, LocalTimeCounter.currentTime(), false, false,
    ) as CjFile
    finishPsiCompilation(frozen, file, requested, requested)
}

internal fun parsePsiCompilationFragment(
    origin: CjCodeFragment,
    requested: CjParserSeed,
    targetContext: PsiElement?,
): ParsedCangJiePsiFile = ReadAction.compute<ParsedCangJiePsiFile, RuntimeException> {
    require(requested.cjmpContext is CjmpParsingContext.Compilation)
    val frozen = freezePsiInput(origin)
    require(requested.sourceKind == frozen.seed.sourceKind && requested.languageModuleName == frozen.seed.languageModuleName) {
        "A target code fragment must preserve its actual source kind and language-module input."
    }
    val entry = requested.copy(sourceContext = frozen.context, lexicalInput = frozen.input, diagnosticScope = null)
    val file = origin.createCompilationFragment(frozen.text, entry, targetContext)
    finishPsiCompilation(frozen, file, requested, entry)
}

private class FrozenPsiInput(
    val origin: CjFile,
    val text: String,
    val stamp: Long,
    val seed: CjParserSeed,
    val context: CjParserSourceContext,
    val input: CjLexicalSourceInput,
    val sourceView: CjPsiSourceView,
)

/** 冻结本代真实来源视图；混合AST的旧完整parser输入只保留入口选项，不授予本代完整结果。 */
private fun freezePsiInput(origin: CjFile): FrozenPsiInput {
    require(!origin.isCompiled) { "Binary PSI belongs to its CJO declaration model, not a source compilation parser." }
    expandPsiGrammar(origin.node)
    val text = origin.node.chars.toString()
    check(origin.viewProvider.contents.toString() == text) { "A source compilation requires committed PSI input." }
    val stamp = origin.modificationStamp
    val seed = checkNotNull(origin.getCopyableUserData(ROOT_PARSER_SEED_KEY)) {
        "The actual source root must publish its parser input."
    }
    val view = origin.currentPsiSourceView()
    val context = view.sourceContext
    val input = view.lexicalInput
    context.requireInput(text)
    check(input.source === context.snapshot)
    view.requireCurrent()
    return FrozenPsiInput(origin, text, stamp, seed, context, input, view)
}

private fun finishPsiCompilation(
    frozen: FrozenPsiInput,
    file: CjFile,
    requested: CjParserSeed,
    entry: CjParserSeed,
): ParsedCangJiePsiFile {
    file.setOriginalFile(frozen.origin)
    file.parserLanguageModuleName = entry.languageModuleName
    file.parserSourceKind = entry.sourceKind
    file.cjmpParsingContext = entry.cjmpContext
    file.psiCompilationInput = CjPsiCompilationInput(entry)
    if (file is CjCodeFragment) file.forceResolveScope((frozen.origin as CjCodeFragment).getForcedResolveScope())
    expandPsiGrammar(file.node)
    val actual = checkNotNull(file.getCopyableUserData(ROOT_PARSER_SEED_KEY))
    check(actual.lexicalInput === frozen.input && actual.sourceContext == frozen.context)
    return ParsedCangJiePsiFile(frozen.origin, file, requested, entry, actual,
        file.collectParsingDiagnosticPublication(), frozen.text, frozen.stamp, frozen.sourceView).also {
        file.putUserData(PSI_COMPILATION_RESULT_KEY, it)
    }
}

/** 片段的非文本输入也具有代次；无平台事件的 imports/scope 更新不能绕过源验证。 */
private class CjCodeFragmentInputs(private val fragment: CjCodeFragment) {
    private val version = fragment.compilationInputVersion
    private val imports = fragment.importsToString()
    private val scope = fragment.getForcedResolveScope()
    private val context = fragment.parserCreationContext
    private val contextFile = context?.containingFile
    private val contextStamp = contextFile?.modificationStamp
    private val contextContents = contextFile?.viewProvider?.contents?.toString()
    private val contextText = context?.text
    private val contextSeed = (contextFile as? CjFile)?.getCopyableUserData(ROOT_PARSER_SEED_KEY)
    private val contextCompilation = (contextFile as? CjFile)?.cjmpParsingContext
    private val contextFragmentVersion = (contextFile as? CjCodeFragment)?.compilationInputVersion

    fun requireUnchanged() {
        check(fragment.compilationInputVersion == version && fragment.importsToString() == imports &&
            fragment.getForcedResolveScope() === scope && fragment.parserCreationContext === context) {
            "A code fragment must retain its imports, resolve scope and actual context generation."
        }
        check(context == null || context.isValid)
        check(context?.containingFile === contextFile && contextFile?.modificationStamp == contextStamp &&
            contextFile?.viewProvider?.contents?.toString() == contextContents && context?.text == contextText &&
            (contextFile as? CjFile)?.getCopyableUserData(ROOT_PARSER_SEED_KEY) === contextSeed &&
            (contextFile as? CjFile)?.cjmpParsingContext == contextCompilation &&
            (contextFile as? CjCodeFragment)?.compilationInputVersion == contextFragmentVersion) {
            "A code fragment compilation cannot outlive its actual host context generation."
        }
    }
}

/** 只走 SDK 真实子节点；不创建 wrapper、不拷贝旧叶或用另一 parser 的偏移补树。 */
internal fun expandPsiGrammar(node: ASTNode) {
    var child = node.firstChildNode
    while (child != null) {
        expandPsiGrammar(child)
        child = child.treeNext
    }
}
