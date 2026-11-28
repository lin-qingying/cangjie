package org.cangnova.cangjie.ide.core.overrideImplement

import com.intellij.codeInspection.options.OptMultiSelector
import com.intellij.modcommand.ActionContext
import com.intellij.modcommand.ModCommand
import com.intellij.modcommand.ModCommandAction
import com.intellij.modcommand.Presentation
import com.intellij.modcommand.PsiBasedModCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.cfir.diagnostics.CaCfirDiagnostic
import org.cangnova.cangjie.analysis.api.components.containingDeclaration
import org.cangnova.cangjie.analysis.api.components.render
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.renderer.declarations.impl.CaDeclarationRendererForSource
import org.cangnova.cangjie.codeinsight.api.applicators.fixes.CangJieQuickFixFactory
import org.cangnova.cangjie.codeinsight.api.members.CangJieGeneratableMember
import org.cangnova.cangjie.formatter.CangJieFormatter
import org.cangnova.cangjie.psi.CjFile
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
 * “抽象成员未实现”诊断对应的 ModCommand action。
 */
private class ImplementMembersQuickfix(
    typeStatement: CjTypeStatement,
    private val members: List<ImplementMembersOption>,
) : PsiBasedModCommandAction<CjTypeStatement>(typeStatement) {
    override fun getFamilyName(): String =
        CangJieOverrideImplementBundle.message("implement.members.handler.family")

    override fun getPresentation(context: ActionContext, element: CjTypeStatement): Presentation =
        Presentation.of(getFamilyName())

    override fun perform(context: ActionContext, element: CjTypeStatement): ModCommand =
        ModCommand.chooseMultipleMembers(
            CangJieOverrideImplementBundle.message("implement.members.handler.title"),
            members,
            members,
        ) { selectedOptions ->
            val selectedMembers = selectedOptions.map { (it as ImplementMembersOption).member }
            ModCommand.psiUpdate(element) { writableType, updater ->
                val lastInserted = insertRenderedMembers(
                    writableType,
                    selectedMembers.map { member -> member.kind to member.renderedText },
                )
                CangJieFormatter.reformatInPlace(writableType.containingFile as CjFile)
                lastInserted?.let(updater::moveCaretTo)
            }
        }
}

/**
 * ModCommand chooser 项：展示签名，同时携带已由 Analysis session 渲染的生成声明。
 */
private data class ImplementMembersOption(
    val displayText: String,
    val parentTypeText: String?,
    val member: CangJieGeneratableMember,
) : OptMultiSelector.OptElement {
    override fun getText(): String = displayText

    override fun getSecondaryText(): String? = parentTypeText
}

private data class ImplementMembersMember(
    override val name: String,
    override val renderedText: String,
    override val kind: CangJieGeneratableMember.Kind,
) : CangJieGeneratableMember

/**
 * “抽象成员未实现”诊断的 K2 quick-fix factory。
 *
 * 抽象成员实现族（`AbstractMemberNotImplemented` / `NeedMemberImplementation` /
 * `InterfaceMemberMustBeImplemented` / `SpecificMemberMustHaveImplementation`）
 * 的诊断 PSI 都落在类型声明上，共用同一套「收集未实现成员 → Implement Members」链路。
 */
object MemberNotImplementedQuickfixFactories {
    /**
     * 为 `AbstractMemberNotImplemented` 诊断创建“实现成员” quick-fix。
     */
    val abstractMemberNotImplemented: CangJieQuickFixFactory.ModCommandBased<CaCfirDiagnostic.AbstractMemberNotImplemented> =
        CangJieQuickFixFactory.ModCommandBased { diagnostic ->
            val typeStatement = diagnostic.psi as? CjTypeStatement ?: return@ModCommandBased emptyList()
            getUnimplementedMemberFixes(typeStatement)
        }

    /**
     * 为 `InterfaceMemberMustBeImplemented` 诊断创建“实现成员” quick-fix。
     *
     * 该诊断有两个上报场景：类实现接口缺成员（PSI 挂在类型声明上，可收集成员）
     * 与 extend 父接口义务（PSI 挂在 extend 上，符号模型不同不在此处理）。
     */
    val interfaceMemberMustBeImplemented: CangJieQuickFixFactory.ModCommandBased<CaCfirDiagnostic.InterfaceMemberMustBeImplemented> =
        CangJieQuickFixFactory.ModCommandBased { diagnostic ->
            fixesForAncestorTypeStatement(diagnostic.psi)
        }

    /**
     * 从诊断 PSI 向上找最近的类型声明并生成修复。
     */
    private fun CaSession.fixesForAncestorTypeStatement(psi: PsiElement): List<ModCommandAction> {
        val typeStatement = generateSequence(psi) { it.parent }
            .firstOrNull { it is CjTypeStatement } as? CjTypeStatement
            ?: return emptyList()
        return getUnimplementedMemberFixes(typeStatement)
    }

    /**
     * 根据诊断所在类型声明收集未实现成员并创建 ModCommand action。
     */
    private fun CaSession.getUnimplementedMemberFixes(
        typeStatement: CjTypeStatement,
    ): List<ModCommandAction> {
        val classSymbol = typeStatement.symbol as? CaClassSymbol ?: return emptyList()
        val unimplementedMembers = getUnimplementedMemberSymbols(classSymbol).map { symbol ->
            symbol.toImplementMembersOption()
        }
        if (unimplementedMembers.isEmpty()) return emptyList()
        return listOf(ImplementMembersQuickfix(typeStatement, unimplementedMembers))
    }

    @OptIn(CaExperimentalApi::class)
    context(session: CaSession)
    private fun CaCallableSymbol.toImplementMembersOption(): ImplementMembersOption {
        val (name, kind, renderedText) = when (this) {
            is CaNamedFunctionSymbol ->
                Triple(name.asString(), CangJieGeneratableMember.Kind.FUNCTION, buildOverrideFunctionText(this))
            is CaPropertySymbol ->
                Triple(name.asString(), CangJieGeneratableMember.Kind.PROPERTY, buildOverridePropertyText(this))
            else -> error("Unsupported callable for override/implement generation: " + this::class.qualifiedName)
        }
        val member = ImplementMembersMember(name, renderedText, kind)
        val parentName = (containingDeclaration as? CaClassSymbol)?.name?.asString()
        return ImplementMembersOption(
            displayText = render(CaDeclarationRendererForSource.WITH_SHORT_NAMES),
            parentTypeText = parentName,
            member = member,
        )
    }
}
