package org.cangnova.cangjie.ide.core.overrideImplement

import com.intellij.codeInsight.generation.ClassMember
import com.intellij.codeInsight.generation.MemberChooserObject
import com.intellij.codeInsight.generation.MemberChooserObjectBase
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.components.containingDeclaration
import org.cangnova.cangjie.analysis.api.components.declaredMemberScope
import org.cangnova.cangjie.analysis.api.components.isVisibleInClass
import org.cangnova.cangjie.analysis.api.components.render
import org.cangnova.cangjie.analysis.api.components.scope
import org.cangnova.cangjie.analysis.api.renderer.declarations.impl.CaDeclarationRendererForSource
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaClassSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaNamedFunctionSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPropertySymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolModality
import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.psi.CjTypeStatement

internal abstract class GenerateMembersHandler(
    final override val toImplement: Boolean
) : AbstractGenerateMembersHandler<CangJieOverrideMemberChooserObject>() {

    override fun collectMembersToGenerate(typeStatement: CjTypeStatement): Collection<CangJieOverrideMemberChooserObject> {
        return analyze(typeStatement) {
            val classSymbol = typeStatement.symbol as? CaClassSymbol ?: return@analyze emptyList()
            collectMembersToGenerate(classSymbol, typeStatement.project)
        }
    }

    context(_: CaSession)
    protected abstract fun collectMembersToGenerate(
        classSymbol: CaClassSymbol,
        project: Project
    ): Collection<CangJieOverrideMemberChooserObject>

    override fun generateMembers(
        editor: Editor?,
        typeStatement: CjTypeStatement,
        selectedElements: Collection<CangJieOverrideMemberChooserObject>,
        copyDoc: Boolean
    ) {
        val project = typeStatement.project
        val generatedMembers = analyze(typeStatement) {
            selectedElements.mapNotNull { chooserObject ->
                chooserObject.symbolPointer.restoreSymbol(this)?.toGeneratedMember()
            }
        }
        if (generatedMembers.isEmpty()) return

        WriteCommandAction.runWriteCommandAction(
            project,
            CangJieOverrideImplementBundle.message("implement.members.handler.title"),
            null,
            {
                val psiFactory = CjPsiFactory(project)
                var lastInserted: PsiElement? = null
                generatedMembers.forEach { member ->
                    val declaration = member.toDeclaration(psiFactory)
                    lastInserted = typeStatement.addDeclaration(declaration)
                }

                CodeStyleManager.getInstance(project).reformat(typeStatement.containingFile)
                lastInserted?.textRange?.startOffset?.let { offset ->
                    editor?.caretModel?.moveToOffset(offset)
                }
            },
            typeStatement.containingFile
        )
    }

    context(_: CaSession)
    private fun CaCallableSymbol.toGeneratedMember(): GeneratedMember = when (this) {
        is CaNamedFunctionSymbol -> GeneratedMember.Function(buildFunctionText(this))
        is CaPropertySymbol -> GeneratedMember.Property(buildPropertyText(this))
        else -> error("Unsupported callable for override/implement generation: ${this::class.qualifiedName}")
    }

    context(_: CaSession)
    private fun buildFunctionText(symbol: CaNamedFunctionSymbol): String {
        val signature = normalizeOverrideSignature(
            symbol.render(CaDeclarationRendererForSource.WITH_SHORT_NAMES)
        )
        return buildString {
            append(signature)
            append(" {\n")
            append(UNIMPLEMENTED_BODY_TEXT)
            append("\n}")
        }
    }

    context(_: CaSession)
    private fun buildPropertyText(symbol: CaPropertySymbol): String {
        val signature = normalizeOverrideSignature(
            symbol.render(CaDeclarationRendererForSource.WITH_SHORT_NAMES)
        )
        return buildString {
            append(signature)
            append(" {\n")
            append("get() {\n")
            append(UNIMPLEMENTED_BODY_TEXT)
            append("\n}\n")
            if (!symbol.isLet || symbol.setter != null) {
                append("set(value) {\n")
                append(UNIMPLEMENTED_BODY_TEXT)
                append("\n}\n")
            }
            append("}")
        }
    }

    /**
     * 渲染出来的 fake/intersection override 签名可能仍带 `abstract` / `open`，
     * 这里统一规范成 IDE 生成所需的 concrete `override` 形态。
     */
    private fun normalizeOverrideSignature(rendered: String): String {
        var normalized = rendered
            .replace("abstract ", "")
            .replace("open ", "")

        if ("override " !in normalized) {
            normalized = when {
                "func " in normalized -> normalized.replaceFirst("func ", "override func ")
                "prop " in normalized -> normalized.replaceFirst("prop ", "override prop ")
                else -> normalized
            }
        }
        return normalized
    }

    private sealed interface GeneratedMember {
        fun toDeclaration(factory: CjPsiFactory): CjDeclaration

        data class Function(private val text: String) : GeneratedMember {
            override fun toDeclaration(factory: CjPsiFactory): CjDeclaration = factory.createFunction(text)
        }

        data class Property(private val text: String) : GeneratedMember {
            override fun toDeclaration(factory: CjPsiFactory): CjDeclaration = factory.createProperty(text)
        }
    }

    companion object {
        /**
         * 与 IDE 现有 `New CangJie Function Body.cj.ft` / `New CangJie Property Initializer.cj.ft`
         * 的默认占位语义保持一致。
         */
        private const val UNIMPLEMENTED_BODY_TEXT = "throw Exception(\"Not yet implemented\")"
    }
}

/**
 * 同时被实现成员 action 和 K2 quick-fix factory 复用的未实现成员收集逻辑。
 */
@OptIn(CaExperimentalApi::class)
context(session: CaSession)
internal fun getUnimplementedMemberSymbols(classSymbol: CaClassSymbol): List<CaCallableSymbol> {
    val result = linkedMapOf<String, CaCallableSymbol>()
    val visibleMembers = classSymbol.collectImplementationRelevantCallables()
        .filter { symbol -> symbol.isVisibleInClass(classSymbol) }
        .groupBy { symbol -> symbol.overrideSignatureKey() }

    for ((_, symbols) in visibleMembers) {
        val abstractSymbols = symbols.filter { symbol -> symbol.isAbstractLike() }
        if (abstractSymbols.isEmpty()) continue

        for (abstractSymbol in abstractSymbols) {
            val hasConcreteImplementation = symbols.any { candidate ->
                candidate !== abstractSymbol &&
                    !candidate.isAbstractLike() &&
                    candidate.canImplementAbstractMember(abstractSymbol)
            }
            if (!hasConcreteImplementation) {
                result.putIfAbsent(
                    abstractSymbol.render(CaDeclarationRendererForSource.WITH_SHORT_NAMES_RAW_SIGNATURES),
                    abstractSymbol,
                )
            }
        }
    }
    return result.values.toList()
}

/**
 * 收集实现成员时参与判定的 callable：
 * - 当前类型已经显式声明的成员，用于判断是否已经存在 concrete 实现；
 * - 源码直接父类型暴露的成员，用于定位仍需实现的抽象成员。
 *
 * 这里刻意从 `superTypes` 出发，而不是直接使用当前类型的 `memberScope`，
 * 以保持与 `CfirNotImplementedOverrideChecker` 的 declaration-site 语义一致，
 * 不让 extend 注入的接口义务污染类/struct 本体的实现候选。
 */
context(session: CaSession)
private fun CaClassSymbol.collectImplementationRelevantCallables(): List<CaCallableSymbol> {
    return buildList {
        addAll(declaredMemberScope.callables.toList())
        for (superType in superTypes) {
            addAll(superType.scope?.callables?.toList().orEmpty())
        }
    }
}

context(session: CaSession)
private fun CaCallableSymbol.overrideSignatureKey(): String =
    render(CaDeclarationRendererForSource.WITH_QUALIFIED_NAMES_RAW_SIGNATURES)

private fun CaCallableSymbol.isAbstractLike(): Boolean =
    modality == CaSymbolModality.ABSTRACT

private fun CaCallableSymbol.canImplementAbstractMember(abstractSymbol: CaCallableSymbol): Boolean {
    val actual = visibility.overrideRank() ?: return false
    val expected = abstractSymbol.visibility.overrideRank() ?: return false
    return actual >= expected
}

private fun CaSymbolVisibility.overrideRank(): Int? = when (this) {
    CaSymbolVisibility.PRIVATE_TO_THIS -> 0
    CaSymbolVisibility.PRIVATE -> 1
    CaSymbolVisibility.INTERNAL,
    CaSymbolVisibility.PROTECTED -> 2
    CaSymbolVisibility.PUBLIC -> 3
    CaSymbolVisibility.LOCAL,
    CaSymbolVisibility.UNKNOWN -> null
}

context(_: CaSession)
internal fun CaCallableSymbol.toChooserObject(): CangJieOverrideMemberChooserObject {
    val renderedText = render(CaDeclarationRendererForSource.WITH_SHORT_NAMES)
    val parentNodeText = (containingDeclaration as? CaClassSymbol)?.name?.asString()
    return CangJieOverrideMemberChooserObject(
        symbolPointer = createPointer(),
        presentableText = renderedText,
        parentNodeText = parentNodeText
    )
}

internal class CangJieOverrideMemberChooserObject(
    val symbolPointer: CaSymbolPointer<CaCallableSymbol>,
    presentableText: String,
    private val parentNodeText: String?
) : MemberChooserObjectBase(presentableText, null), ClassMember {
    override fun getParentNodeDelegate(): MemberChooserObject? {
        val text = parentNodeText ?: return null
        return MemberChooserObjectBase(text, null)
    }
}
