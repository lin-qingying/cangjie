package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.components.CaTypeAccessKind
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.api.CompletionRequest
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionOrigin
import org.cangnova.cangjie.ide.completion.impl.cfir.CfirCompletionSection
import org.cangnova.cangjie.ide.completion.impl.shared.context.CompletionPositionContexts
import org.cangnova.cangjie.ide.completion.impl.shared.context.CompletionPositionKind
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjReferenceExpression

/**
 * 枚举值候选区段。
 *
 * 仓颉允许**同名不同 payload 个数**的枚举构造器（官方 cjc 1.1.3 实测
 * `enum Shape { | Dot | Dot(Int64) }` 合法），因此去重必须按「名字 + payload 签名」
 * 而不是显示名——按显示名去重会把两个合法构造器合并成一个，用户无法选到无参那个。
 *
 * 候选通道与成员区段**同一条**（类型成员作用域），只是把 `CaEnumConstructorSymbol`
 * 单独取出并标为 `ENUM_ENTRY`：它们在语义上是枚举值而非普通成员，插入形态与排序
 * 权重都不同，混进 `MEMBER` 会让用户看不出这是枚举构造器。
 */
@OptIn(CaExperimentalApi::class)
class CfirEnumEntryCompletionSection : CfirCompletionSection(
    id = ID,
    executionPriority = PRIORITY,
) {
    /**
     * 计算当前位置的枚举值候选。
     */
    override fun compute(request: CompletionRequest): List<CompletionSnapshot> {
        val position = request.completionPosition
        val context = CompletionPositionContexts.classify(position)
        if (context.kind != CompletionPositionKind.MEMBER_ACCESS) return emptyList()
        val receiver = context.receiverExpression as? CjElement ?: return emptyList()
        val prefix = request.prefix
        val file = position.containingCjFile ?: return emptyList()

        return analyze(file) {
            val enumClass = resolveEnumClass(receiver) ?: return@analyze emptyList()
            val receiverType = enumClass.defaultType

            receiverType.memberScopeQueries(CaTypeAccessKind.INSTANCE_THROUGH_RECEIVER)
                .mapNotNull { it.scope }
                .flatMap { it.callables { true } }
                .filterIsInstance<CaEnumConstructorSymbol>()
                .filter { (it as CaNamedSymbol).name?.asString()?.startsWith(prefix) == true }
                .map { constructor ->
                    callableSnapshot(
                        constructor,
                        CompletionItemKind.ENUM_ENTRY,
                        CompletionOrigin.MEMBER,
                        documentStamp = request.documentStamp,
                    )
                }
        }
    }

    /**
     * 解析接收者指向的枚举类型；接收者不是分类器时返回 `null`。
     *
     * 与 `CfirMemberCompletionSection.resolveAccessTarget` 同形：只有解析结果**恰好**是
     * 唯一分类器、且没有任何变量绑定时才判为类型限定。
     */
    private fun CaSession.resolveEnumClass(receiver: CjElement): CaClassLikeSymbol? {
        val reference = receiver as? CjReferenceExpression ?: return null
        val symbols = with(this) { reference.resolveToSymbols() }
        return symbols.filterIsInstance<CaClassLikeSymbol>()
            .singleOrNull()
            ?.takeIf { symbols.none { it is CaVariableSymbol } }
    }

    companion object {
        /**
         * 区段标识。
         */
        const val ID: String = "cfir.enumEntry"

        /**
         * 执行优先级：与成员区段同批执行；枚举值本身也是成员，先执行不改变结果集，
         * 只决定同优先级下的展示顺序。
         */
        const val PRIORITY: Int = 12
    }
}
