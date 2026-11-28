package org.cangnova.cangjie.parsing

import com.intellij.lang.ASTNode
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.util.Key
import org.cangnova.cangjie.psi.CjFile
import java.util.concurrent.atomic.AtomicReference

/**
 * 当前真实 PSI 代次的不可变来源视图。它统一诊断和源码绑定的坐标，也可作为下一次 parser 的输入，
 * 不授予历史声明属性、根 parser 结果或区域 COMPLETE 的语义有效性。
 */
interface CjPsiSourceView {
    val sourceContext: CjParserSourceContext
    val lexicalInput: CjLexicalSourceInput

    /** 当前 AST、文件代次或文本改变后，本视图整体失效。 */
    fun requireCurrent()
}

private class CjPsiSourceViewImpl(
    private val file: CjFile,
    private val root: ASTNode,
    private val stamp: Long,
    private val fileName: String,
    private val filePath: String?,
    override val sourceContext: CjParserSourceContext,
    override val lexicalInput: CjLexicalSourceInput,
) : CjPsiSourceView {
    fun belongsTo(node: ASTNode, modificationStamp: Long): Boolean =
        root === node && stamp == modificationStamp && fileName == file.name && filePath == file.virtualFile?.path

    override fun requireCurrent() {
        check(file.isValid && file.sourceViewState().get() === this && belongsTo(file.node, file.modificationStamp)) {
            "A PSI source view belongs to one actual AST and file generation."
        }
        sourceContext.requireInput(root.chars)
        check(lexicalInput.source === sourceContext.snapshot)
    }
}

/** 完整新根的真实输入可以初始化当前视图；局部 diff 必须先撤销这个完整根依据。 */
private class CjPsiSourceBasis(
    val context: CjParserSourceContext,
    val input: CjLexicalSourceInput,
    val fileName: String,
    val filePath: String?,
)

private val PSI_SOURCE_BASIS_KEY: Key<CjPsiSourceBasis> = Key.create("CANGJIE_PSI_SOURCE_BASIS")
private val PSI_SOURCE_VIEW_STATE_KEY: Key<AtomicReference<CjPsiSourceViewImpl?>> = Key.create("CANGJIE_PSI_SOURCE_VIEW_STATE")
internal val PUBLISHED_FILE_RESULT_KEY: Key<CjParsedFileResult> = Key.create("CANGJIE_PUBLISHED_FILE_RESULT")

/** 并发读事务共用一次实际 generation 的安装；原子状态不随 PSI copy 复制。 */
private fun CjFile.sourceViewState(): AtomicReference<CjPsiSourceViewImpl?> =
    putUserDataIfAbsent(PSI_SOURCE_VIEW_STATE_KEY, AtomicReference<CjPsiSourceViewImpl?>(null))

/** 首次建树或完整结果实际安装后初始化，不在候选 diff 中修改原文件。 */
internal fun CjFile.publishPsiSourceBasis(input: CjParserSeed) {
    putCopyableUserData(PSI_SOURCE_BASIS_KEY,
        CjPsiSourceBasis(checkNotNull(input.sourceContext), checkNotNull(input.lexicalInput), name, virtualFile?.path))
    sourceViewState().set(null)
}

/** 撤销当前代次的坐标视图，历史 ROOT 输入仍保留其真实 producer 来源。 */
internal fun CjFile.invalidatePsiSourceView() {
    putCopyableUserData(PSI_SOURCE_BASIS_KEY, null)
    sourceViewState().set(null)
}

/**
 * 在同一读事务内冻结实际 root、stamp 和全文。完整根保留原冻结输入；局部修改后的混合 AST
 * 建立本代坐标输入，不能把它伪装成旧 parser 的新结果或重写已有词元身份。
 */
fun CjFile.currentPsiSourceView(): CjPsiSourceView = ReadAction.compute<CjPsiSourceView, RuntimeException> {
    sourceViewInReadAction()
}

private fun CjFile.sourceViewInReadAction(): CjPsiSourceView {
    val root = node
    root.firstChildNode
    val stamp = modificationStamp
    val state = sourceViewState()
    while (true) {
        val cached = state.get()
        if (cached != null && cached.belongsTo(root, stamp)) {
            cached.requireCurrent()
            return cached
        }
        val fileName = name
        val filePath = virtualFile?.path
        val basis = getCopyableUserData(PSI_SOURCE_BASIS_KEY)
        val context: CjParserSourceContext
        val input: CjLexicalSourceInput
        if (basis != null && basis.fileName == fileName && basis.filePath == filePath) {
            context = basis.context
            input = basis.input
            context.requireInput(root.chars)
        } else {
            // 改名或复制到不同实际文件后撤销坐标 basis；历史 producer 输入和词法 origin 不变。
            if (basis != null) putCopyableUserData(PSI_SOURCE_BASIS_KEY, null)
            context = CjParserSourceContext(CjParsingSourceSnapshot(fileName, filePath, root.chars.toString()), 0)
            input = CjLexicalSourceInput(context.snapshot)
        }
        val created = CjPsiSourceViewImpl(this, root, stamp, fileName, filePath, context, input)
        if (state.compareAndSet(cached, created)) {
            created.requireCurrent()
            return created
        }
    }
}
