package org.cangnova.cangjie.ide.completion.api

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionIdentity
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionInsertion
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionSortData
import org.cangnova.cangjie.ide.completion.api.lookup.CompletionSymbolKind
import org.cangnova.cangjie.ide.completion.api.lookup.ImportResolution
import org.cangnova.cangjie.psi.CjElement

/**
 * 补全请求的来源类别。
 *
 * BASIC 与 SMART 共用候选生成，但期望类型过滤与构造方案只在 SMART 下启用；
 * 区分二者可以避免重复计算，也让「为什么某个候选缺席」可解释。
 */
enum class CompletionKind {
    /** 手动或自动触发的基础补全：给出全部适用候选并排序。 */
    BASIC,

    /** 智能补全：在基础集合之上按期望类型过滤，或给出可满足该类型的构造方案。 */
    SMART,
}

/**
 * 一次补全请求的稳定参数。
 *
 * 该对象是「分析期计算」与「界面期消费」之间唯一的边界：它只保存 PSI、
 * 文本区间与调用计数，不持有 Analysis 会话、符号或类型。
 */
interface CompletionRequest {
    /** 触发本次补全的原始 PSI 位置。 */
    val originalPosition: CjElement

    /** 经 dummy 修正后的实际分析位置；未修正时等于 [originalPosition]。 */
    val completionPosition: CjElement

    /**
     * 用于**位置形态分类**的原始元素；默认取 [completionPosition]。
     *
     * 之所以与 [completionPosition] 分开：有些位置不在仓颉 PSI 里，最典型的是文档注释——
     * 文档注释由 CDoc 词法产出 `CDocElement`，**不是 `CjElement`**，
     * 因此文档注释内最近的 `CjElement` 祖先是整个文件，按它分类只会得到 `EXPRESSION`。
     * 兜底取 `completionPosition` 会让 `DOCUMENTATION` 这个位置种类**永远到不了**，
     * 而按语言规则，注释内的候选与代码内的候选完全不同。
     *
     * [CangJieCompletionService] 的实现必须在构造请求时填入光标处的**原始叶子元素**
     * （平台侧就是 `CompletionParameters.position`），不能只填修正后的分析位置。
     */
    val positionElement: com.intellij.psi.PsiElement
        get() = completionPosition

    /** 补全前缀文本。 */
    val prefix: String

    /** 触发来源。 */
    val kind: CompletionKind

    /** 本位置上的触发次数；平台用它区分自动弹窗与显式重复调用。 */
    val invocationCount: Int

    /** 触发补全的原始偏移量。 */
    val offset: Int

    /** 需要被补全结果替换的文本区间；使用左闭右开区间。 */
    val replaceStartOffset: Int

    /** 替换区间的结束偏移量。 */
    val replaceEndOffset: Int

    /**
     * 触发补全时**原始文件**的文档修改戳。
     *
     * 分析在文件副本上进行，副本的修改戳与用户文档无关；插入发生在原始文档上，
     * 版本校验必须使用原始文档在请求时刻的戳。0 表示请求时未能捕获文档戳
     * （例如无文档的合成位置），此时仅当插入时刻的戳也为 0 才允许写入。
     */
    val documentStamp: Long
}

/**
 * 补全结果的稳定快照条目。
 *
 * 界面只允许消费本接口给出的不可变数据与必要指针：
 * **禁止**在分析结束后持有 Analysis 会话、符号、类型或惰性分析序列。
 * 需要语义复查时，用 [identity] 重新进入分析并恢复。
 *
 * 快照携带五组彼此独立的数据，对应补全链路的五个阶段：
 * - [identity]：候选「是谁」，用于恢复与去重；
 * - [typeText] / [tailText] / [kind]：候选「长什么样」，全部在分析期渲染完毕；
 * - [sort]：候选「排多前」，是结构化数据而非平台权重；
 * - [importPlan]：候选「怎么插进去」；
 * - [insertion]：插入时的括号、caret 与文档版本校验数据。
 */
interface CompletionSnapshot {
    /** 候选显示名。 */
    val lookupString: String

    /** 类型签名渲染文本；没有签名时为空串。 */
    val tailText: String

    /** 渲染后的类型文本；无法渲染类型时为空串。 */
    val typeText: String

    /** 候选所属种类，用于图标与排序。 */
    val kind: CompletionItemKind

    /**
     * 候选所对应声明的具体形态，用于图标。
     *
     * 缺省按 [kind] 兜底：说不出具体形态时图标退回粗粒度映射（引入本字段之前的行为），
     * 而不是猜一个形态——把函数显示成类型比不显示形态更容易误导。
     */
    val symbolKind: CompletionSymbolKind
        get() = CompletionSymbolKind.fallbackFor(kind)

    /** 分析期权重；值越大越靠前。 */
    val weight: Int

    /** 候选稳定身份。 */
    val identity: CompletionIdentity

    /** 结构化排序数据。 */
    val sort: CompletionSortData

    /** 导入策略与冲突结论。 */
    val importPlan: ImportResolution

    /** 插入数据。 */
    val insertion: CompletionInsertion

    /** 弹窗中展示的主文本；与 [lookupString] 相同时无需单独设置。 */
    val presentableText: String
        get() = lookupString

    /** 插入后是否需要跟随 `(`。 */
    val insertParenthesis: Boolean
        get() = insertion.insertParenthesis

    /** 命名参数候选的冒号文本；非命名参数候选为空串。 */
    val namedArgumentPrefix: String
        get() = insertion.namedArgumentPrefix
}

/**
 * 候选类别。
 *
 * 类别只描述「候选是什么形状」，不描述它是否可见或是否已导入；
 * 可见性与导入需求必须由 Analysis 结论提供，不能从类别推断。
 */
enum class CompletionItemKind {
    /** 局部声明或参数。 */
    LOCAL,

    /**
     * 泛型类型参数。
     *
     * 与 [LOCAL] 分开：类型参数是分类器，插入形态与签名渲染都不同于局部值声明，
     * 归入 LOCAL 会让弹窗把它渲染成带括号的调用形态。
     */
    TYPE_PARAMETER,

    /** 成员。 */
    MEMBER,

    /** 静态成员。 */
    STATIC_MEMBER,

    /** extend 提供的成员。 */
    EXTEND_MEMBER,

    /** 顶层类型或函数等可导入声明。 */
    TOP_LEVEL,

    /** 包名。 */
    PACKAGE,

    /** import 语句中的成员。 */
    IMPORT_MEMBER,

    /** 关键字。 */
    KEYWORD,

    /** 命名实参。 */
    NAMED_ARGUMENT,

    /** 枚举值或有参构造器。 */
    ENUM_ENTRY,

    /** 文档标签或引用。 */
    DOCUMENTATION,

    /**
     * 声明生成建议：不是引用已有声明，而是「这里可以生成一个新成员」。
     *
     * 与其余类别并列而不是单独成族：它对用户就是一个候选，
     * 只是插入的是**新声明**而不是一次引用。
     */
    DECLARATION_SUGGESTION,
}

/**
 * 补全管线执行的稳定服务入口。
 *
 * 这是跨模块唯一的补全服务契约：宿主只依赖本接口消费补全，
 * 不接触内部贡献者列表、section 调度或 CFIR 实现细节。
 */
interface CangJieCompletionService {
    /**
     * 在给定位置执行补全并返回稳定快照序列。
     *
     * 实现必须保证：
     * 1. 返回结果不持有分析会话；
     * 2. 取消或失败时抛出异常而不是静默返回空列表；
     * 3. 同等输入的排序稳定。
     */
    fun complete(request: CompletionRequest): List<CompletionSnapshot>

    /**
     * 判断该位置是否允许发起补全；用于自动触发前的快速排除。
     *
     * 默认实现不做额外过滤：平台自身的 dummy 与字符过滤器已经处理大部分非法位置。
     */
    fun isAllowedAt(position: PsiElement): Boolean = true
}

/**
 * 仓颉原生补全的服务提供者。
 *
 * 平台侧注册实现并消费服务；未注册实现时补全不可用，
 * **不得**用通用词补全冒充仓颉语义补全。
 */
interface CangJieCompletionServiceProvider {
    /**
     * 取得当前可用的补全服务。
     */
    fun completionService(): CangJieCompletionService
}