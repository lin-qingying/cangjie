package org.cangnova.cangjie.cfir.session

import org.cangnova.cangjie.source.CjSourceElement

/**
 * 条件编译（`@When`）中允许使用的比较运算符集合。
 *
 * 该枚举保持 source-neutral：它只描述官方语法层面允许的运算符词汇，
 * 不绑定到 raw CFIR 的任何表达式节点类型，从而可以被 evaluator、
 * 测试配置与诊断 owner 共同消费，而无需依赖前端解析器的具体实现。
 *
 * 语义上，每个成员对应官方 `@When` 条件中的一种比较形式：
 * - [EQ] / [NE]：等值与不等值比较，适用于几乎所有 builtin 条件；
 * - [LT] / [GT] / [LE] / [GE]：序数比较，主要用于版本类条件
 *   （如 `cjc_version`）的三段版本号比较。
 */
public enum class CfirConditionalCompilationOperator {
    /** 等值比较（`==`）。 */
    EQ,
    /** 不等值比较（`!=`）。 */
    NE,
    /** 小于比较（`<`）。 */
    LT,
    /** 大于比较（`>`）。 */
    GT,
    /** 小于等于比较（`<=`）。 */
    LE,
    /** 大于等于比较（`>=`）。 */
    GE,
}

/**
 * 官方 builtin 条件的语法契约快照。
 *
 * 每个实例描述一个 builtin 条件的完整语法边界：
 *
 * - [name]：条件名，与官方拼写严格一致（如 `backend`、`cjc_version`），
 *   evaluator 按此名分发到对应的取值逻辑；
 * - [operators]：该条件允许出现的比较运算符集合，超出集合的用法
 *   由 evaluator 归入 [CfirConditionalCompilationError.UNSUPPORTED_OPERATOR]；
 * - [supportedValues]：允许的取值白名单；为 `null` 时表示取值不受
 *   枚举约束（如版本字符串），为空集时表示该条件不接受值表达式
 *   （如 `debug`、`test` 这类纯开关条件）。
 */
public data class CfirConditionalCompilationBuiltinSpec(
    /** 条件的官方拼写名称，用作 evaluator 分发键与诊断中的条件名。 */
    public val name: String,
    /** 该条件语法上允许的比较运算符集合。 */
    public val operators: Set<CfirConditionalCompilationOperator>,
    /** 该条件允许的取值白名单；`null` 表示不限制取值。 */
    public val supportedValues: Set<String>?,
)

/**
 * 仓颉官方条件编译（`@When`）的唯一 builtin 条件目录。
 *
 * 这里集中声明官方支持的每一个 builtin 条件及其语法契约
 * （见 [CfirConditionalCompilationBuiltinSpec]），是整个条件编译子系统的
 * 单一事实来源：evaluator 据此分发取值与校验取值/运算符合法性，
 * 测试配置据此构造环境，诊断 owner 据此生成错误信息。
 *
 * 任何路径都不得重新硬编码 backend / arch / os 的允许值，
 * 否则会与官方语义产生漂移且难以发现。
 */
public object CfirConditionalCompilationCatalog {
    /** `backend` 条件：当前编译后端，官方目前仅允许 `cjnative`。 */
    public val backend: CfirConditionalCompilationBuiltinSpec = spec(
        name = "backend",
        operators = setOf(CfirConditionalCompilationOperator.EQ, CfirConditionalCompilationOperator.NE),
        supportedValues = setOf("cjnative"),
    )

    /** `arch` 条件：target triple 中的目标架构，仅支持等值/不等值比较。 */
    public val arch: CfirConditionalCompilationBuiltinSpec = spec(
        name = "arch",
        operators = setOf(CfirConditionalCompilationOperator.EQ, CfirConditionalCompilationOperator.NE),
        supportedValues = setOf("x86_64", "aarch64"),
    )

    /** `os` 条件：target triple 中的目标操作系统，仅支持等值/不等值比较。 */
    public val os: CfirConditionalCompilationBuiltinSpec = spec(
        name = "os",
        operators = setOf(CfirConditionalCompilationOperator.EQ, CfirConditionalCompilationOperator.NE),
        supportedValues = setOf("Windows", "Linux", "macOS", "HarmonyOS"),
    )

    /**
     * `cjc_version` 条件：编译器三段版本号。
     *
     * 取值不设白名单（[CfirConditionalCompilationBuiltinSpec.supportedValues] 为 `null`），
     * 但允许全部序数比较运算符，按官方三段版本语义逐段比较。
     */
    public val cjcVersion: CfirConditionalCompilationBuiltinSpec = spec(
        name = "cjc_version",
        operators = CfirConditionalCompilationOperator.entries.toSet(),
        supportedValues = null,
    )

    /** `debug` 条件：纯开关条件，不携带值表达式，也不允许任何比较运算符。 */
    public val debug: CfirConditionalCompilationBuiltinSpec = spec(
        name = "debug",
        operators = emptySet(),
        supportedValues = null,
    )

    /** `test` 条件：纯开关条件，不携带值表达式，也不允许任何比较运算符。 */
    public val test: CfirConditionalCompilationBuiltinSpec = spec(
        name = "test",
        operators = emptySet(),
        supportedValues = null,
    )

    /** 全部 builtin 条件的稳定列表，顺序与官方文档保持一致。 */
    public val builtins: List<CfirConditionalCompilationBuiltinSpec> = listOf(
        backend,
        arch,
        os,
        cjcVersion,
        debug,
        test,
    )

    /** 按条件名索引的查找表，供 [builtin] 做 O(1) 查询。 */
    private val byName: Map<String, CfirConditionalCompilationBuiltinSpec> = builtins.associateBy { it.name }

    /**
     * 按官方拼写名称查找 builtin 条件契约。
     *
     * @return 名称匹配的契约；未知条件名返回 `null`，
     *   调用方应将其归入 [CfirConditionalCompilationError.UNKNOWN_CONDITION]。
     */
    public fun builtin(name: String): CfirConditionalCompilationBuiltinSpec? = byName[name]

    /** 构造契约实例的内部便捷工厂，保证字段与构造参数一一对应。 */
    private fun spec(
        name: String,
        operators: Set<CfirConditionalCompilationOperator>,
        supportedValues: Set<String>?,
    ): CfirConditionalCompilationBuiltinSpec = CfirConditionalCompilationBuiltinSpec(
        name = name,
        operators = operators,
        supportedValues = supportedValues,
    )
}

/**
 * raw 解析阶段为特殊 builtin 语法保留的源码拼写。
 *
 * 在声明解析完成之前，`@When` 这类注解尚未绑定到真实的注解类，
 * raw 层只能按源码拼写识别它们；该对象集中维护这些拼写，
 * 避免各处散落魔法字符串。
 */
public object CfirRawBuiltinAnnotationNames {
    /** `@When` 条件编译注解在源码中的官方拼写。 */
    public const val WHEN: String = "When"
}

/**
 * 条件编译求值失败的分类。
 *
 * evaluator 在剪枝失败时不抛异常，而是把失败原因归入该枚举并发布
 * [CfirConditionalCompilationFailure]；后续 checker 负责把失败事实
 * 转换为结构化诊断，因此新增成员时必须同步补充对应的诊断映射。
 */
public enum class CfirConditionalCompilationError {
    /** `@When` 注解上没有任何条件表达式。 */
    NO_CONDITION,
    /** 条件表达式不符合官方语法允许的形态。 */
    INVALID_EXPRESSION,
    /** 条件名不在 [CfirConditionalCompilationCatalog.builtins] 之内。 */
    UNKNOWN_CONDITION,
    /** 条件取值不是合法字面量或不符合值语法。 */
    INVALID_VALUE,
    /** 条件取值语法合法，但不在该条件的取值白名单内。 */
    UNSUPPORTED_VALUE,
    /** 该条件不允许使用所出现的比较运算符。 */
    UNSUPPORTED_OPERATOR,
    /** 版本类条件的取值无法按三段版本语义解析。 */
    INVALID_VERSION,
}

/**
 * raw 剪枝阶段保留的条件编译失败事实。
 *
 * 该数据类只承载“失败发生了、在哪里、因为什么”，不负责渲染诊断文本；
 * 用结构化事实代替异常，是为了让同一份 raw 树可以被多个 checker
 * 消费并各自决定诊断策略。
 *
 * @property reason 失败的分类，决定最终诊断的 kind。
 * @property source 失败发生的源码位置；无法定位时为 `null`。
 * @property conditionName 涉及的条件名；与条件名无关的失败为 `null`。
 * @property rightValue 比较表达式右侧的原始取值文本。
 * @property operator 涉及的比较运算符；与运算符无关的失败为 `null`。
 */
public data class CfirConditionalCompilationFailure(
    /** 失败的分类，决定后续诊断的 kind 与消息模板。 */
    public val reason: CfirConditionalCompilationError,
    /** 失败发生的源码位置；无法定位时为 `null`。 */
    public val source: CjSourceElement?,
    /** 涉及的条件名；与条件名无关的失败为 `null`。 */
    public val conditionName: String? = null,
    /** 比较表达式右侧的原始取值文本。 */
    public val rightValue: String? = null,
    /** 涉及的比较运算符；与运算符无关的失败为 `null`。 */
    public val operator: CfirConditionalCompilationOperator? = null,
)
