package org.cangnova.cangjie.ide.completion.api.lookup

import org.cangnova.cangjie.ide.completion.api.CompletionItemKind

/**
 * 候选来源。
 *
 * 来源描述「候选从哪一层作用域/哪一类索引来」，与 [org.cangnova.cangjie.ide.completion.api.CompletionItemKind]
 * 的「候选是什么形状」是两件事：一个 extend 成员既是来源 `EXTEND` 也是种类 `EXTEND_MEMBER`，
 * 而同一种类可以来自局部、成员或顶层三种来源，排序上必须区别对待。
 */
enum class CompletionOrigin {
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
enum class ExpectedTypeMatch {
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
interface CompletionSortData {
    /** 候选来源。 */
    val origin: CompletionOrigin

    /**
     * 导入成本：数值越小越易用。
     *
     * 已可达取 [ImportCost.DIRECT]，需要追加 import 取 [ImportCost.ADD_IMPORT]，
     * 需先写限定名再缩短取 [ImportCost.QUALIFY_THEN_SHORTEN]。
     */
    val importCost: Int

    /** 期望类型匹配结论；非 SMART 位置恒为 [ExpectedTypeMatch.UNKNOWN]。 */
    val expectedTypeMatch: ExpectedTypeMatch

    /**
     * 同位置内的出现次序权重。
     *
     * 用于在同一来源内部稳定排序（如按声明顺序），不参与跨来源比较。
     */
    val declarationOrder: Int

    /**
     * 候选类别。
     *
     * 类别参与排序的方式见 [categoryRank]：只有「兜底方案」与「附属条目」被压低，
     * 其余类别等值——位置分类已经把候选限制在同种类内。
     */
    val kind: CompletionItemKind

    /**
     * 该声明是否已被弃用。
     *
     * 弃用候选**降权但不剔除**：用户仍可能需要它（迁移期的代码就是靠它跑起来的），
     * 但把它排在同等可用的替代项之前会让用户持续选中不该再用的声明。
     * 剔除是另一回事，由诊断负责，不在补全里做。
     */
    val isDeprecated: Boolean
}

/**
 * 导入成本的取值常量。
 *
 * 数值刻意与 [CompletionSortData.importCost] 的语义一一对应，
 * 排序比较只允许引用本组常量，避免散落的魔数。
 */
object ImportCost {
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
data class CompletionSortDataImpl(
    override val origin: CompletionOrigin,
    override val importCost: Int = ImportCost.DIRECT,
    override val expectedTypeMatch: ExpectedTypeMatch = ExpectedTypeMatch.UNKNOWN,
    override val declarationOrder: Int = 0,
    override val isDeprecated: Boolean = false,
    override val kind: CompletionItemKind = CompletionItemKind.TOP_LEVEL,
) : CompletionSortData {
    companion object {
        /** 已知来源的默认排序数据：不匹配未知导入成本、不确定期望类型。 */
        fun of(origin: CompletionOrigin, declarationOrder: Int = 0): CompletionSortDataImpl =
            CompletionSortDataImpl(origin = origin, declarationOrder = declarationOrder)
    }
}

/**
 * 取得替换了期望类型匹配结论的排序数据。
 *
 * 匹配结论必须在**分析内**写入，而排序数据是快照契约的一部分，
 * 因此它由排序数据自己提供复制入口，而不是让调用方把接口强转成实现类。
 */
fun CompletionSortData.withExpectedTypeMatch(match: ExpectedTypeMatch): CompletionSortData =
    CompletionSortDataImpl(
        origin = origin,
        importCost = importCost,
        expectedTypeMatch = match,
        declarationOrder = declarationOrder,
        isDeprecated = isDeprecated,
        kind = kind,
    )

/**
 * 分析期权重：由结构化排序数据折算成一个可比较的整数。
 *
 * 权重是**分析期**产物，冻结在快照里；平台侧只做等比映射，不重新判断语义。
 * 折算顺序即优先级顺序：
 * **来源 > 导入成本 > 期望类型 > 类别 > 弃用 > 同级声明次序**。
 *
 * **每一维占一个互不重叠的位段**，即高位维度的最小步长严格大于低位各维的全幅跨度，
 * 因此低位维度无论取到多大都不会越过高位维度。这条约束必须逐维验算而不是凭感觉：
 * 同级声明次序原先是无界的，一个声明靠后的候选足以越过「是否需要导入」这一维。
 */
fun CompletionSortData.toWeight(): Int {
    val originScore = (CompletionOrigin.entries.size - origin.ordinal) * ORIGIN_BAND
    val importScore = (ImportCost.QUALIFY_THEN_SHORTEN - importCost) * IMPORT_BAND
    val typeScore = when (expectedTypeMatch) {
        ExpectedTypeMatch.MATCH -> TYPE_BAND
        ExpectedTypeMatch.UNKNOWN -> 0
        ExpectedTypeMatch.MISMATCH -> -TYPE_BAND
    }
    val categoryScore = categoryRank(kind) * CATEGORY_BAND
    val deprecationScore = if (isDeprecated) -DEPRECATION_BAND else 0
    return originScore + importScore + typeScore + categoryScore + deprecationScore +
        declarationOrder.coerceIn(0, DECLARATION_ORDER_BAND)
}

/**
 * 类别维度：只区分「附属条目」与真实候选。
 *
 * 不给出各类符号之间的次序：同一位置上的候选已经被位置分类限制在同种类里
 * （类型位置只出类型、成员位置只出成员），跨种类的相对次序没有可依据的规范，
 * 编一套出来只会让排序看起来有道理却无法解释。
 *
 * 只有文档标签/引用被压低：它是同一批补全里的附属条目，盖过声明本身没有道理。
 * **注意**：`CompletionItemKind` 目前没有「声明生成建议」这一项（它在
 * `CompletionIdentityKind` 里）；将来补全入口引入该类别时，它的权重必须落在
 * [CompletionItemKind.DOCUMENTATION] 这一档或更低——「要不要新建一个声明」
 * 是兜底方案，不是命中。
 */
fun categoryRank(kind: CompletionItemKind): Int = when (kind) {
    CompletionItemKind.DOCUMENTATION -> 0
    else -> 1
}

/** 来源维度步长；必须大于其余各维的全幅跨度。 */
private const val ORIGIN_BAND = 1_000_000

/** 导入成本维度步长；必须大于期望类型及其下各维的全幅跨度。 */
private const val IMPORT_BAND = 1_000

/** 期望类型匹配的加权量；必须大于类别、弃用与声明次序的全幅跨度（119）。 */
private const val TYPE_BAND = 200

/** 类别维度步长；必须大于弃用与声明次序的全幅跨度（19）。 */
private const val CATEGORY_BAND = 50

/** 弃用降权量；必须大于声明次序的上限。 */
private const val DEPRECATION_BAND = 10

/** 同级声明次序的上限。 */
private const val DECLARATION_ORDER_BAND = 9