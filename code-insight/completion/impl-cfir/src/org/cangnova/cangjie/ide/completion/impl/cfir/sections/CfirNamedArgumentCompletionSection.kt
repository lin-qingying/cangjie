package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.ide.completion.api.CompletionRequest
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.impl.cfir.CfirCompletionSection
import org.cangnova.cangjie.ide.completion.impl.shared.context.positionContext
import org.cangnova.cangjie.ide.completion.impl.shared.context.CompletionPositionKind
import org.cangnova.cangjie.ide.completion.impl.shared.namedArgumentSnapshot
import org.cangnova.cangjie.psi.CjCallExpression
import org.cangnova.cangjie.psi.CjReferenceExpression
import org.cangnova.cangjie.psi.CjValueArgument

/**
 * 仓颉命名实参候选区段。
 *
 * 仓颉的命名实参写作 `name: value`，**不是** Kotlin 的 `name = value`。前缀由
 * [namedArgumentSnapshot] 固定为 `"$name: "` 并从签名上禁止调用方传分隔符，
 * 这样「写成等号形式」只能来自把 Kotlin 语法照搬的实现。
 *
 * 候选来自**被调用方**的形参列表减去本调用里已用过的名字：仓颉不允许同一实参名
 * 重复出现，已用过的名字若再补出来，用户选中即编译失败。
 */
@OptIn(CaExperimentalApi::class)
class CfirNamedArgumentCompletionSection : CfirCompletionSection(
    id = ID,
    executionPriority = PRIORITY,
) {
    /**
     * 计算当前位置的命名实参候选。
     */
    override fun compute(request: CompletionRequest): List<CompletionSnapshot> {
        val position = request.completionPosition
        val context = request.positionContext
        if (context.kind != CompletionPositionKind.NAMED_ARGUMENT) {
            return emptyList()
        }

        val argument = PsiTreeUtil.getParentOfType(position, CjValueArgument::class.java)
            ?: return emptyList()
        val call = PsiTreeUtil.getParentOfType(argument, CjCallExpression::class.java)
            ?: return emptyList()
        val calleeReference = call.referenceExpression ?: return emptyList()
        val file = position.containingCjFile ?: return emptyList()
        val prefix = request.prefix

        return analyze(file) {
            val callee = resolveCallee(calleeReference)
                ?: return@analyze emptyList<CompletionSnapshot>()

            val used = call.valueArguments
                .filter { it !== argument }
                .mapNotNull { usedArgumentName(it) }
                .toSet()

            callee.namedParameterList()
                .mapNotNull { parameter ->
                    val name = parameter.name?.asString() ?: return@mapNotNull null
                    if (name in used || !name.startsWith(prefix)) return@mapNotNull null
                    namedArgumentSnapshot(
                        parameterName = name,
                        documentStamp = request.documentStamp,
                    ).build()
                }
        }
    }

    /**
     * 解析被调用方的 callable 符号。
     *
     * `resolveToSymbols` 是 `CaResolver` 的**成员扩展**，必须在会话接收者上调用；
     * 作为顶层扩展导入会解析不到。
     */
    private fun CaSession.resolveCallee(reference: CjReferenceExpression): CaCallableSymbol? =
        with(this) { reference.resolveToSymbols() }
            .filterIsInstance<CaCallableSymbol>()
            .firstOrNull()

    /**
     * 取一个已写出的实参所用的名字；位置实参（无名字）返回 `null`。
     */
    private fun usedArgumentName(argument: CjValueArgument): String? =
        argument.getArgumentName()?.asName?.asString()

    companion object {
        /**
         * 区段标识。
         */
        const val ID: String = "cfir.namedArgument"

        /**
         * 执行优先级：命名实参只在调用实参位置出现，与其他家族的候选集不重叠，
         * 排在成员与顶层候选之后不影响正确性。
         */
        const val PRIORITY: Int = 25
    }
}
