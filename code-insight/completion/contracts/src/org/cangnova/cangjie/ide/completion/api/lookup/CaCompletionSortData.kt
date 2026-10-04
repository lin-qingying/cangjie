package org.cangnova.cangjie.ide.completion.api.lookup

/**
 * 候选来源。
 *
 * 来源描述「候选从哪一层作用域/哪一类索引来」，与 [org.cangnova.cangjie.ide.completion.api.CaCompletionItemKind]
 * 的「候选是什么形状」是两件事：一个 extend 成员既是来源 `EXTEND` 也是种类 `EXTEND_MEMBER`，
 * 而同一种类可以来自局部、成员或顶层三种来源，排序上必须区别对待。
 */
enum class CaCompletionOrigin {
    /** 局部声明、参数、类型参数。 */
    LOCAL_SCOPE,

    /** 隐式接收者成员。 */
    MEMBER,

    /** extend 提供的成员。 */
    EXTEND,

    /** 顶层声明。 */
    TOP_LEVEL,

    /** 包。 */
    PACKAGE,

    /** import 语句内部。 */
    IMPORT,

    /** 关键字。 */
    KEYWORD,

    /** 命名实参。 */
    NAMED_ARGUMENT,

    /** 枚举值或有参构造器。 */
    ENUM,

    /** 文档标签/引用。 */
    DOCUMENTATION,

    /** 声明生成建议。 */
    DECLARATION_SUGGESTION,
}

/**
 * 期望类型匹配结论。
 *
 * 「未知」与「不匹配」必须分开：未知时不得据此过滤候选，
 * 否则一个尚未推导完成的期望类型会把整张列表清空。
 */
enum class CaExpectedTypeMatch {
    /** 该位置没有确定的期望类型。 */
    UNKNOWN,

    /** 有确定期望类型，且候选与之相容。 */
    MATCH,

    /** 有确定期望类型，且候选与之不相容。 */
    MISMATCH,
}

/**
 * 分析期计算出的结构化排序数据。
 *
 * 排序在**分析期**算完并随快照冻结：界面期只做「结构化数据 → 平台优先级」的机械换算。
 * 这样做有两个原因：
 * 1. 期望类型匹配必须调用 Analysis，不能留在界面线程；
 * 2. 避免为每个维度注册一个平台 weigher（那会把语义判断推给无法访问 Analysis 的平台代码）。
 */
interface CaCompletionSortData {
    /** 候选来源。 */
    val origin: CaCompletionOrigin

    /**
     * 导入成本：数值越小越易用。
     *
     * 已可达取 [CaImportCost.DIRECT]，需要追加 import 取 [CaImportCost.ADD_IMPORT]，
     * 需先写限定名再缩短取 [CaImportCost.QUALIFY_THEN_SHORTEN]。
     */
    val importCost: Int

    /** 期望类型匹配结论；非 SMART 位置恒为 [CaExpectedTypeMatch.UNKNOWN]。 */
    val expectedTypeMatch: CaExpectedTypeMatch

    /**
     * 同位置内的出现次序权重。
     *
     * 用于在同一来源内部稳定排序（如按声明顺序），不参与跨来源比较。
     */
    val declarationOrder: Int
}

/**
 * 导入成本的取值常量。
 *
 * 数值刻意与 [CaCompletionSortData.importCost] 的语义一一对应，
 * 排序比较只允许引用本组常量，避免散落的魔数。
 */
object CaImportCost {
    /** 当前文件已可直接引用。 */
    const val DIRECT: Int = 0

    /** 追加一条 import 后可引用。 */
    const val ADD_IMPORT: Int = 10

    /** 必须写成限定名，再交给引用缩短。 */
    const val QUALIFY_THEN_SHORTEN: Int = 20
}

/**
 * 排序数据的标准实现。
 */
data class CaCompletionSortDataImpl(
    override val origin: CaCompletionOrigin,
    override val importCost: Int = CaImportCost.DIRECT,
    override val expectedTypeMatch: CaExpectedTypeMatch = CaExpectedTypeMatch.UNKNOWN,
    override val declarationOrder: Int = 0,
) : CaCompletionSortData {
    companion object {
        /** 已知来源的默认排序数据：不匹配未知导入成本、不确定期望类型。 */
        fun of(origin: CaCompletionOrigin, declarationOrder: Int = 0): CaCompletionSortDataImpl =
            CaCompletionSortDataImpl(origin = origin, declarationOrder = declarationOrder)
    }
}

/**
 * 取得替换了期望类型匹配结论的排序数据。
 *
 * 匹配结论必须在**分析内**写入，而排序数据是快照契约的一部分，
 * 因此它由排序数据自己提供复制入口，而不是让调用方把接口强转成实现类。
 */
fun CaCompletionSortData.withExpectedTypeMatch(match: CaExpectedTypeMatch): CaCompletionSortData =
    CaCompletionSortDataImpl(
        origin = origin,
        importCost = importCost,
        expectedTypeMatch = match,
        declarationOrder = declarationOrder,
    )

/**
 * 分析期权重：由结构化排序数据折算成一个可比较的整数。
 *
 * 权重是**分析期**产物，冻结在快照里；平台侧只做等比映射，不重新判断语义。
 * 折算顺序即优先级顺序：来源 > 导入成本 > 期望类型 > 声明次序。
 */
fun CaCompletionSortData.toWeight(): Int {
    val originScore = (CaCompletionOrigin.entries.size - origin.ordinal) * 1_000_000
    val importScore = (CaImportCost.QUALIFY_THEN_SHORTEN - importCost) * 1_000
    val typeScore = when (expectedTypeMatch) {
        CaExpectedTypeMatch.MATCH -> 100
        CaExpectedTypeMatch.UNKNOWN -> 0
        CaExpectedTypeMatch.MISMATCH -> -100
    }
    return originScore + importScore + typeScore + declarationOrder
}