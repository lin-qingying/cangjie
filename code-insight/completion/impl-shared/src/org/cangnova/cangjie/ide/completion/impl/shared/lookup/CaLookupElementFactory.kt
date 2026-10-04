package org.cangnova.cangjie.ide.completion.impl.shared.lookup

import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.util.Key
import org.cangnova.cangjie.ide.completion.api.CaCompletionSnapshot
import org.cangnova.cangjie.ide.completion.api.lookup.CaCompletionInsertion
import org.cangnova.cangjie.ide.completion.api.lookup.CaCompletionOrigin
import org.cangnova.cangjie.ide.completion.api.lookup.CaExpectedTypeMatch
import org.cangnova.cangjie.ide.completion.impl.shared.insert.CaCompletionInsertHandlers

/**
 * 快照在 lookup 元素上的挂载键。
 *
 * 插入与语义复查都发生在分析会话**之后**，此时只能从 lookup 元素取回快照，
 * 凭 [CaCompletionSnapshot.identity] 重新进入分析。元素上不允许挂符号或类型。
 */
val CA_COMPLETION_SNAPSHOT: Key<CaCompletionSnapshot> = Key.create("CANGJIE_COMPLETION_SNAPSHOT")

/**
 * 平台 lookup 元素工厂。
 *
 * 这是「分析结果 → 界面元素」的唯一转换点，承载 B5 的硬约束：
 * **所有语义渲染都在分析期完成**，这里只搬运不可变字段。
 *
 * 明确不做的事：
 * - 不在 [create] 里访问 Analysis（调用点在分析之后，会触发跨会话断言）；
 * - 不在这里决定排序（见 [priorityOf]，排序是分析期结论的机械映射）；
 * - 不在这里解析导入冲突（冲突复验必须在写事务内做）。
 */
object CaLookupElementFactory {
    /**
     * 把稳定快照转换为平台 lookup 元素。
     */
    fun create(snapshot: CaCompletionSnapshot): LookupElement {
        var builder = LookupElementBuilder
            // 用身份去重键而不是快照实例当 lookupObject：平台按 lookupObject 判等，
            // 实例当键会让「同一候选被两个区段各产一次」的情况在弹窗里出现两行。
            .create(snapshot.identity.deduplicationKey, snapshot.lookupString)
            .withPresentableText(snapshot.presentableText)
            .withIcon(iconOf(snapshot.kind))
            .withInsertHandler(CaCompletionInsertHandlers.forSnapshot(snapshot))
        // with* 方法返回**新的** builder（本平台版本的 builder 不可变），返回值必须接住：
        // 把结果丢掉会让尾文本、类型文本静默丢失，弹窗里两个同名重载长得一模一样。
        // tailText 承载重载区分，必须显示。
        builder = builder.withTailText(snapshot.tailText, true)
        // typeText 为空时不要调用 withTypeText：平台会把「没设置」和「显式空串」区别对待，
        // 无条件传入空串会覆盖掉按 kind 推导的默认类型展示。
        if (snapshot.typeText.isNotEmpty()) {
            builder = builder.withTypeText(snapshot.typeText)
        }
        builder.putUserData(CA_COMPLETION_SNAPSHOT, snapshot)
        return PrioritizedLookupElement.withPriority(builder, priorityOf(snapshot))
    }

    /**
     * 批量转换，保持传入顺序。
     */
    fun createAll(snapshots: List<CaCompletionSnapshot>): List<LookupElement> = snapshots.map(::create)
}

/**
 * 快照到平台优先级的映射。
 *
 * 平台的 weigher 体系要求每个维度单独注册 `CompletionWeigher`，但那会把
 * 「期望类型是否匹配」「是否需要导入」这类**必须查 Analysis 的判断**
 * 推到拿不到 Analysis 的平台代码里。因此这里一次性把分析期冻结的权重
 * 折算成单个平台优先级：顺序与 [CaCompletionSnapshot.weight] 完全一致。
 *
 * 返回值越大越靠前，与平台 weigher 的约定相同。
 */
fun priorityOf(snapshot: CaCompletionSnapshot): Double = snapshot.weight.toDouble()

/**
 * 按分析期权重降序稳定排序。
 *
 * 稳定性显式用「先按权重降序、再按到达次序」实现：同一区段内的声明顺序是
 * 语义信息（源码自上而下），而 `sortedByDescending` 在 Kotlin 里是稳定排序，
 * 但把它与后续过滤/插入串起来时，依赖隐式稳定性会让两次相同请求给出不同列表，
 * 因此这里把次序显式写进比较器。
 */
fun List<CaCompletionSnapshot>.sortedByAnalysisWeight(): List<CaCompletionSnapshot> =
    withIndex()
        .sortedWith(
            compareByDescending<IndexedValue<CaCompletionSnapshot>> { it.value.weight }
                .thenBy { it.index },
        )
        .map { it.value }

/**
 * 候选来源到展示权重的相对次序。
 *
 * 单独抽成函数而不是散落在权重公式里：来源次序是**契约**
 * （design 要求保持 Kotlin 的执行/展示优先级），需要能被测试直接断言。
 */
fun originRank(origin: CaCompletionOrigin): Int = origin.ordinal

/**
 * 期望类型匹配是否构成「应当优先展示」的条件。
 *
 * 只有确定匹配才加权：把 UNKNOWN 当成匹配会让没有期望类型的位置凭空排序，
 * 把 MISMATCH 当成不匹配则会在期望类型推导不完整时误杀合法候选。
 */
fun isPromotedByExpectedType(match: CaExpectedTypeMatch): Boolean = match == CaExpectedTypeMatch.MATCH

/**
 * 插入数据是否要求在候选文本之后追加文本。
 *
 * 命名实参候选必须追加 `name: `，它不是「括号式插入」的特例。
 */
fun CaCompletionInsertion.requiresSuffixText(): Boolean = namedArgumentPrefix.isNotEmpty()