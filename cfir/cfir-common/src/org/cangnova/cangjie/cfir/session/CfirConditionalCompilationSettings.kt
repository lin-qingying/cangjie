package org.cangnova.cangjie.cfir.session

/**
 * `@When` 条件编译的显式 compiler-invocation 环境。
 *
 * 这些值必须由调用方从 compiler options 注入；CFIR 不得从 JVM 宿主系统、
 * `TargetPlatform` 或任意默认对象推导它们。该接口刻意不提供默认实现，
 * 以便在 source session 未配置条件环境时让配置错误可观察。
 */
public interface CfirConditionalCompilationSettings : CfirSessionComponent {
    /** 官方条件 `backend` 的规范值，例如 `cjnative`。 */
    public val backend: String

    /** 官方条件 `arch` 的 target triple 架构值。 */
    public val arch: String

    /** 官方条件 `os` 的 target triple 操作系统值。 */
    public val os: String

    /** 编译器版本，按官方三段版本语义比较。 */
    public val cjcVersion: String

    /** 是否启用 `debug` 条件。 */
    public val debug: Boolean

    /** 是否启用 `test` 条件。 */
    public val test: Boolean

    /** `--cfg` 注入的用户条件；保留字符串值，不接受宿主环境补值。 */
    public val userDefined: Map<String, String>
}

/**
 * 由 compiler invocation 显式构造的条件环境。
 *
 * 该类型是注入值的不可变载体，不是 session 默认配置；调用方必须明确提供所有
 * 内置字段后才能创建它。
 */
public data class ExplicitCfirConditionalCompilationSettings(
    override val backend: String,
    override val arch: String,
    override val os: String,
    override val cjcVersion: String,
    override val debug: Boolean,
    override val test: Boolean,
    override val userDefined: Map<String, String> = emptyMap(),
) : CfirConditionalCompilationSettings {
    init {
        require(backend.isNotBlank()) { "Conditional compilation backend must be explicitly configured." }
        require(arch.isNotBlank()) { "Conditional compilation arch must be explicitly configured." }
        require(os.isNotBlank()) { "Conditional compilation OS must be explicitly configured." }
        require(cjcVersion.isNotBlank()) { "Conditional compilation cjc version must be explicitly configured." }
    }
}

/** 当前 session 的显式 `@When` 环境；未注册时访问会抛出配置错误。 */
public val CfirSession.conditionalCompilationSettings: CfirConditionalCompilationSettings by
    CfirSession.sessionComponentAccessor()

/** 可选检查入口；不合成默认条件环境。 */
public val CfirSession.conditionalCompilationSettingsOrNull: CfirConditionalCompilationSettings?
    by CfirSession.nullableSessionComponentAccessor()
