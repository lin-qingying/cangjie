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
    context(session: CaSession)
    override fun collectMembersToGenerate(
        classSymbol: CaClassSymbol,
        project: Project
    ): Collection<CangJieOverrideMemberChooserObject> {
        return getUnimplementedMemberSymbols(classSymbol).map { symbol ->
            symbol.toChooserObject()
        }
    }

    override fun getChooserTitle(): String = CangJieOverrideImplementBundle.message("implement.members.handler.title")

    override fun getNoMembersFoundHint(): String =
        CangJieOverrideImplementBundle.message("implement.members.handler.no.members.hint")
}

internal class ImplementMembersQuickfix(
    private val members: Collection<CangJieOverrideMemberChooserObject>,
) : ImplementMembersHandler(), IntentionAction {
    override fun getText(): String = familyName

    override fun getFamilyName(): String = CangJieOverrideImplementBundle.message("implement.members.handler.family")

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean = true

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
    val abstractMemberNotImplemented: CangJieQuickFixFactory.IntentionBased<CaCfirDiagnostic.AbstractMemberNotImplemented> =
        CangJieQuickFixFactory.IntentionBased { diagnostic ->
            val typeStatement = diagnostic.psi as? CjTypeStatement ?: return@IntentionBased emptyList()
            getUnimplementedMemberFixes(typeStatement)
        }

    private fun CaSession.getUnimplementedMemberFixes(typeStatement: CjTypeStatement): List<IntentionAction> {
        val classSymbol = typeStatement.symbol as? CaClassSymbol ?: return emptyList()
        val unimplementedMembers = getUnimplementedMemberSymbols(classSymbol).map { symbol ->
            symbol.toChooserObject()
        }
        if (unimplementedMembers.isEmpty()) return emptyList()
        return listOf(ImplementMembersQuickfix(unimplementedMembers))
    }
}
