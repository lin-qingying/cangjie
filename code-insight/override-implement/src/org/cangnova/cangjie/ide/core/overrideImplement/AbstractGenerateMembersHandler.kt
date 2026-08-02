package org.cangnova.cangjie.ide.core.overrideImplement

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.generation.ClassMember
import com.intellij.codeInsight.generation.MemberChooserObject
import com.intellij.codeInsight.hint.HintManager
import com.intellij.ide.util.MemberChooser
import com.intellij.lang.LanguageCodeInsightActionHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.NlsContexts
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjTypeStatement
import org.cangnova.cangjie.psi.psiUtil.getNonStrictParentOfType

/**
 * 对齐 Kotlin `AbstractGenerateMembersHandler` 的 IDE 交互骨架。
 *
 * 该层只负责：
 * - 定位当前类型声明；
 * - 进度下收集候选成员；
 * - 弹 chooser / 自动全选；
 * - 提交文档并转交具体生成器。
 */
internal abstract class AbstractGenerateMembersHandler<T : ClassMember> : LanguageCodeInsightActionHandler {
    /**
     * 是否为“实现抽象成员”模式；为 true 时 chooser 默认全选候选成员。
     */
    abstract val toImplement: Boolean

    /**
     * 在同步进度窗口中读取可生成成员。
     */
    fun collectMembersToGenerateUnderProgress(typeStatement: CjTypeStatement): Collection<T> {
        return ProgressManager.getInstance().runProcessWithProgressSynchronously<Collection<T>, RuntimeException>(
            { runReadAction { collectMembersToGenerate(typeStatement) } },
            CangJieOverrideImplementBundle.message("dialog.progress.collect.members.to.generate"),
            true,
            typeStatement.project
        )
    }

    /**
     * 在读动作中收集当前类型可生成的成员。
     */
    @RequiresBackgroundThread(generateAssertion = false)
    protected abstract fun collectMembersToGenerate(typeStatement: CjTypeStatement): Collection<T>

    /**
     * 将用户选择的成员生成到目标类型声明中。
     */
    protected abstract fun generateMembers(
        editor: Editor?,
        typeStatement: CjTypeStatement,
        selectedElements: Collection<T>,
        copyDoc: Boolean
    )

    /**
     * 成员选择对话框标题。
     */
    @NlsContexts.DialogTitle
    protected abstract fun getChooserTitle(): String

    /**
     * 没有可生成成员时展示的提示文本。
     */
    @NlsContexts.HintText
    protected abstract fun getNoMembersFoundHint(): String

    /**
     * 判断目标类型声明是否允许当前 handler 执行。
     */
    protected open fun isValidForClass(typeStatement: CjTypeStatement): Boolean = true

    /**
     * 判断 chooser 节点是否应作为父类/接口分组节点显示。
     */
    protected open fun isClassNode(key: MemberChooserObject): Boolean = false

    /**
     * 根据当前编辑器光标定位需要生成成员的仓颉类型声明。
     */
    protected open fun resolveTargetTypeStatement(editor: Editor?, file: PsiFile): CjTypeStatement? {
        val cjFile = file as? CjFile ?: return null
        val offset = editor?.caretModel?.offset ?: return null
        return cjFile.findElementAt(offset)?.getNonStrictParentOfType<CjTypeStatement>()
    }

    /**
     * 弹出成员选择器并返回用户选择的成员集合。
     */
    private fun showMemberChooser(project: Project, members: Collection<T>): MemberChooser<T>? {
        @Suppress("UNCHECKED_CAST")
        val memberArray = members.toTypedArray<ClassMember>() as Array<T>
        val chooser = object : MemberChooser<T>(memberArray, false, true, project) {
            override fun isContainerNode(key: MemberChooserObject): Boolean {
                return super.isContainerNode(key) || isClassNode(key)
            }
        }
        chooser.title = getChooserTitle()
        if (toImplement) {
            chooser.selectElements(memberArray)
        }

        chooser.show()
        if (chooser.exitCode != DialogWrapper.OK_EXIT_CODE) return null
        return chooser
    }

    /**
     * IntelliJ action 可用性检查入口。
     */
    override fun isValidFor(editor: Editor, file: PsiFile): Boolean {
        val typeStatement = resolveTargetTypeStatement(editor, file) ?: return false
        return isValidForClass(typeStatement)
    }

    /**
     * IntelliJ action 执行入口。
     */
    override fun invoke(project: Project, editor: Editor, file: PsiFile) {
        invokeWithTarget(project, editor as Editor?, file, ApplicationManager.getApplication().isUnitTestMode)
    }

    /**
     * 对指定目标文件执行成员收集、选择和生成。
     */
    protected fun invokeWithTarget(project: Project, editor: Editor?, file: PsiFile, implementAll: Boolean) {
        val typeStatement = resolveTargetTypeStatement(editor, file) ?: return
        if (!isValidForClass(typeStatement)) return
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) return

        val members = collectMembersToGenerateUnderProgress(typeStatement)
        if (members.isEmpty() && !implementAll) {
            editor?.let { HintManager.getInstance().showErrorHint(it, getNoMembersFoundHint()) }
            return
        }

        val copyDoc: Boolean
        val selectedElements: Collection<T>
        if (implementAll || editor == null) {
            selectedElements = members
            copyDoc = false
        } else {
            val chooser = showMemberChooser(project, members) ?: return
            selectedElements = chooser.selectedElements ?: return
            copyDoc = chooser.isCopyJavadoc
        }

        if (selectedElements.isEmpty()) return
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        generateMembers(editor, typeStatement, selectedElements, copyDoc)
    }

    /**
     * 成员生成需要先完成 chooser 与读动作，因此不在 action 初始写动作中启动。
     */
    override fun startInWriteAction(): Boolean = false
}
