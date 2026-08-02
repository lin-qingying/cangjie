package org.cangnova.cangjie.ide.core.overrideImplement

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.cfir.diagnostics.CaCfirDiagnostic
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.codeinsight.api.applicators.fixes.CangJieQuickFixFactory
import org.cangnova.cangjie.psi.CjTypeStatement

/**
 * “实现成员”主处理器本体。
 *
 * 这里保持 K2 语义：handler 只负责收集和生成，不再直接承担 quick-fix 接口。
 */
internal open class ImplementMembersHandler : GenerateMembersHandler(true) {
    /**
     * 收集当前类型仍未实现的抽象成员。
     */
    context(session: CaSession)
    override fun collectMembersToGenerate(
        classSymbol: CaClassSymbol,
        project: Project
    ): Collection<CangJieOverrideMemberChooserObject> {
        return getUnimplementedMemberSymbols(classSymbol).map { symbol ->
            symbol.toChooserObject()
        }
    }

    /**
     * “实现成员” chooser 标题。
     */
    override fun getChooserTitle(): String = CangJieOverrideImplementBundle.message("implement.members.handler.title")

    /**
     * 没有可实现成员时的提示文本。
     */
    override fun getNoMembersFoundHint(): String =
        CangJieOverrideImplementBundle.message("implement.members.handler.no.members.hint")
}

/**
 * “抽象成员未实现”诊断对应的 intention action。
 */
internal class ImplementMembersQuickfix(
    /**
     * 诊断处已经收集好的待实现成员候选。
     */
    private val members: Collection<CangJieOverrideMemberChooserObject>,
) : ImplementMembersHandler(), IntentionAction {
    /**
     * intention 展示文本。
     */
    override fun getText(): String = familyName

    /**
     * intention family 名称。
     */
    override fun getFamilyName(): String = CangJieOverrideImplementBundle.message("implement.members.handler.family")

    /**
     * 诊断产生的 quick-fix 始终可用，实际候选已由 factory 过滤。
     */
    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean = true

    /**
     * 从已保存的 symbol pointer 恢复最新候选成员。
     */
    context(session: CaSession)
    override fun collectMembersToGenerate(
        classSymbol: CaClassSymbol,
        project: Project,
    ): Collection<CangJieOverrideMemberChooserObject> {
        return members.mapNotNull { chooserObject ->
            chooserObject.symbolPointer.restoreSymbol(session)?.toChooserObject()
        }
    }
}

/**
 * “抽象成员未实现”诊断的 K2 quick-fix factory。
 */
object MemberNotImplementedQuickfixFactories {
    /**
     * 为 `AbstractMemberNotImplemented` 诊断创建“实现成员” quick-fix。
     */
    val abstractMemberNotImplemented: CangJieQuickFixFactory.IntentionBased<CaCfirDiagnostic.AbstractMemberNotImplemented> =
        CangJieQuickFixFactory.IntentionBased { diagnostic ->
            val typeStatement = diagnostic.psi as? CjTypeStatement ?: return@IntentionBased emptyList()
            getUnimplementedMemberFixes(typeStatement)
        }

    /**
     * 根据诊断所在类型声明收集未实现成员并包装为 intention。
     */
    private fun CaSession.getUnimplementedMemberFixes(typeStatement: CjTypeStatement): List<IntentionAction> {
        val classSymbol = typeStatement.symbol as? CaClassSymbol ?: return emptyList()
        val unimplementedMembers = getUnimplementedMemberSymbols(classSymbol).map { symbol ->
            symbol.toChooserObject()
        }
        if (unimplementedMembers.isEmpty()) return emptyList()
        return listOf(ImplementMembersQuickfix(unimplementedMembers))
    }
}
