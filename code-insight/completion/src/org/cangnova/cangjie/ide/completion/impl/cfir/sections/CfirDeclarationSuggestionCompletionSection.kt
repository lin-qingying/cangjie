package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.codeinsight.api.members.memberGenerationRegistry
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.api.CompletionRequest
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionIdentityKind
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionOrigin
import org.cangnova.cangjie.ide.completion.impl.cfir.CfirCompletionSection
import org.cangnova.cangjie.ide.completion.impl.shared.completionSnapshot
import org.cangnova.cangjie.psi.CjBlockExpression
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjTypeStatement

/**
 * 声明生成建议：类体里「这个类型还缺哪些成员」的候选。
 *
 * 候选文本取自生成能力的**窄接口**（`CangJieMemberGenerationService`），
 * 补全因此不认识 chooser、UI 与写事务——那些留在 override-implement 模块内部。
 *
 * **未注册即无候选**：宿主没装配生成服务时不产出这个家族的候选，
 * 这是明确的降级，不是静默失败。
 */
@OptIn(CaExperimentalApi::class)
class CfirDeclarationSuggestionCompletionSection : CfirCompletionSection(
    id = ID,
    executionPriority = PRIORITY,
) {
    /**
     * 计算当前位置的声明生成建议。
     */
    override fun compute(request: CompletionRequest): List<CompletionSnapshot> {
        val position = request.completionPosition
        val typeStatement = declarationSiteOf(position) ?: return emptyList()
        // 未注册生成服务：这一族没有候选，不吞异常也不假装成功。
        val service = memberGenerationRegistry(position.project).service ?: return emptyList()
        val prefix = request.prefix

        return service.collectUnimplementedMembers(typeStatement)
            .filter { it.name.startsWith(prefix) }
            .map { member ->
                completionSnapshot(
                    lookupString = member.name,
                    kind = CompletionItemKind.DECLARATION_SUGGESTION,
                    origin = CompletionOrigin.DECLARATION_SUGGESTION,
                    identityKind = CompletionIdentityKind.DECLARATION_SUGGESTION,
                    // 尾文本给渲染后的声明：用户据此判断「生成出来是不是我要的」。
                    tailText = member.renderedText,
                    documentStamp = request.documentStamp,
                ).build()
            }
    }

    /**
     * 判断位置是否是**类体内的声明位置**；不是则返回 `null`。
     *
     * 判据刻意不用 `DECLARATION_NAME`：那要求用户已经开始写一个声明，
     * 而「我还缺哪些成员」恰恰是**还没开始写**的时候最需要。
     * 这里用「在类体内、且不在某个成员的方法体里」——方法体里该给的是普通表达式候选。
     */
    private fun declarationSiteOf(position: CjElement): CjTypeStatement? {
        if (PsiTreeUtil.getParentOfType(position, CjBlockExpression::class.java) != null) return null
        // `strict = false`：位置本身可能就是那个类型声明（光标停在类名上时），
        // 严格模式会把这种情况判成「不在任何类里」。
        return PsiTreeUtil.getParentOfType(position, CjTypeStatement::class.java, false)
    }

    companion object {
        /**
         * 区段标识。
         */
        const val ID: String = "cfir.declarationSuggestion"

        /**
         * 执行优先级：需要一次分析会话收集成员，排在作用域候选之后、索引候选之前。
         */
        const val PRIORITY: Int = 40
    }
}
