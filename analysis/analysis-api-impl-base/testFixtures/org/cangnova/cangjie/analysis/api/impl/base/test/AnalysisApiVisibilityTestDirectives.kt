package org.cangnova.cangjie.analysis.api.impl.base.test

import org.cangnova.cangjie.analysis.api.symbols.CaSymbolVisibility
import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.directives.model.singleValue
import kotlin.text.toBooleanStrict

/**
 * visibility 能力族专用指令。
 *
 * `TARGET_NAME` 继续复用公共 component 指令；
 * 这里补充“目标声明种类 + 期望 visibility + 当前 session 可见性”三类专属元信息，
 * 让 generated 测试可以稳定覆盖 source/local/extend/跨模块场景。
 */
object AnalysisApiVisibilityTestDirectives : SimpleDirectivesContainer() {
    /**
     * 指定 visibility 测试目标声明的种类。
     *
     * 测试框架根据该值区分顶层函数、binding pattern、extend 成员等声明形态，
     * 从而把可见性断言应用到正确的 symbol。
     */
    val VISIBILITY_TARGET_KIND by stringDirective(
        description = "visibility 测试目标声明种类，例如 TOP_LEVEL_FUNCTION / BINDING_PATTERN / EXTEND_MEMBER。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录目标声明在公开 symbol 上应暴露的可见性枚举。
     *
     * 该字段用于断言 `CaSymbolVisibility` 映射结果，保证源码修饰符和默认可见性不会在 Analysis API 层漂移。
     */
    val EXPECTED_SYMBOL_VISIBILITY by stringDirective(
        description = "目标声明公开 symbol 上暴露的 CaSymbolVisibility。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录目标声明的可见性是否由源码显式写出。
     *
     * 该布尔指令用于区分显式修饰符和默认可见性，覆盖 symbol status 中的来源信息。
     */
    val EXPECTED_VISIBILITY_EXPLICIT by stringDirective(
        description = "目标声明的可见性是否由源码显式写出，取值 true / false。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录目标声明在当前 use-site session 中是否可见。
     *
     * 该字段校验 visibility checker 的使用点判断，覆盖跨模块、friend 和局部声明等场景。
     */
    val EXPECTED_IS_VISIBLE by stringDirective(
        description = "目标声明在当前 use-site session 中是否可见，取值 true / false。",
        applicability = DirectiveApplicability.File,
    )
}

/**
 * 读取 visibility 测试目标声明的种类。
 *
 * 返回值驱动目标定位策略，确保后续可见性查询作用在 testData 指定的声明形态上。
 */
val RegisteredDirectives.visibilityTargetKind: String
    get() = singleValue(AnalysisApiVisibilityTestDirectives.VISIBILITY_TARGET_KIND)

/**
 * 读取公开 symbol 可见性枚举的期望值。
 *
 * 访问器集中执行字符串到 `CaSymbolVisibility` 的转换，让测试断言使用强类型枚举。
 */
val RegisteredDirectives.expectedSymbolVisibility: CaSymbolVisibility
    get() = CaSymbolVisibility.valueOf(singleValue(AnalysisApiVisibilityTestDirectives.EXPECTED_SYMBOL_VISIBILITY))

/**
 * 读取可见性是否显式声明的期望值。
 *
 * 该值使用严格布尔解析，确保 testData 中的非法布尔文本不会被隐式接受。
 */
val RegisteredDirectives.expectedVisibilityExplicit: Boolean
    get() = singleValue(AnalysisApiVisibilityTestDirectives.EXPECTED_VISIBILITY_EXPLICIT).toBooleanStrict()

/**
 * 读取当前 session 中目标声明是否可见的期望值。
 *
 * 返回值用于与公开 visibility checker 查询结果比较。
 */
val RegisteredDirectives.expectedVisibleInSession: Boolean
    get() = singleValue(AnalysisApiVisibilityTestDirectives.EXPECTED_IS_VISIBLE).toBooleanStrict()
