package org.cangnova.cangjie.ide.completion.impl.cfir.sections

import org.cangnova.cangjie.analysis.api.CaExperimentalApi
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.components.CaTypeAccessKind
import org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaEnumConstructorSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaVariableSymbol
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.analysis.api.symbols.markers.CaNamedSymbol
import org.cangnova.cangjie.ide.completion.api.CompletionItemKind
import org.cangnova.cangjie.ide.completion.api.CompletionRequest
import org.cangnova.cangjie.ide.completion.api.CompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionOrigin
import org.cangnova.cangjie.ide.completion.impl.cfir.CfirCompletionSection
import org.cangnova.cangjie.ide.completion.impl.shared.context.positionContext
import org.cangnova.cangjie.ide.completion.impl.shared.context.CompletionPositionKind
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjMatchEntry
import org.cangnova.cangjie.psi.CjMatchExpression
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
        val prefix = request.prefix
        val file = position.containingCjFile ?: return emptyList()

        // `match` 的模式位置与成员访问位置的候选来源是同一条：都是某个枚举的构造器，
        // 区别只在「枚举从哪来」——成员访问从接收者，模式位置从 match 的**主语类型**。
        matchSubject(position)?.let { subject ->
            return analyze(file) { enumConstructorsOf(expressionTypeOf(subject), prefix, request.documentStamp) }
        }

        val context = request.positionContext
        if (context.kind != CompletionPositionKind.MEMBER_ACCESS) return emptyList()
        val receiver = context.receiverExpression as? CjElement ?: return emptyList()

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
     * 取出光标所在模式对应的 `match` 主语表达式。
     *
     * 仓颉的 `match` 按主语类型的枚举构造器分派，用户在 `case` 后面敲的几乎总是其中之一。
     * 只认**模式位置**：主语表达式自己所在的位置不是模式位置，那里该走普通表达式候选。
     */
    private fun matchSubject(position: CjElement): CjExpression? {
        val entry = PsiTreeUtil.getParentOfType(position, CjMatchEntry::class.java) ?: return null
        // 一个 case 子句里既有模式也有体（`case Dot => 1`）：只有模式那一段才是模式位置，
        // 体里该给的是普通表达式候选。按偏移划界，不靠「有没有解析出模式」——
        // 用户正打到一半时模式往往还没成型。
        val body = entry.expression ?: return null
        if (position.textRange.startOffset >= body.textRange.startOffset) return null
        val match = PsiTreeUtil.getParentOfType(entry, CjMatchExpression::class.java) ?: return null
        return match.subjectExpression
    }

    /**
     * 主语表达式的类型；取不到时返回 `null`。
     */
    private fun CaSession.expressionTypeOf(expression: CjExpression): CaType? =
        with(expression) { expressionType }

    /**
     * 枚举类型上的构造器候选。
     *
     * 不先判「这是不是一个枚举」：直接按 `CaEnumConstructorSymbol` 过滤，
     * 非枚举类型自然得到空集合。多一次枚举判定只多一处可能与实际符号族脱节的条件。
     */
    private fun CaSession.enumConstructorsOf(
        type: CaType?,
        prefix: String,
        documentStamp: Long,
    ): List<CompletionSnapshot> {
        if (type == null) return emptyList()
        return type.memberScopeQueries(CaTypeAccessKind.INSTANCE_THROUGH_RECEIVER)
            .mapNotNull { it.scope }
            .flatMap { it.callables { true } }
            .filterIsInstance<CaEnumConstructorSymbol>()
            .filter { (it as CaNamedSymbol).name?.asString()?.startsWith(prefix) == true }
            .map { constructor ->
                callableSnapshot(
                    constructor,
                    CompletionItemKind.ENUM_ENTRY,
                    CompletionOrigin.MEMBER,
                    documentStamp = documentStamp,
                )
            }
            .toList()
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
