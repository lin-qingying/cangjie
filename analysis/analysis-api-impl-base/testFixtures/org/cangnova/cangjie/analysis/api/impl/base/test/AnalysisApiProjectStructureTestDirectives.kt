package org.cangnova.cangjie.analysis.api.impl.base.test

import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.RegisteredDirectives
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.directives.model.singleOrZeroValue
import org.cangnova.cangjie.test.directives.model.singleValue

/**
 * Analysis API 项目结构测试使用的模块形状。
 *
 * 这一层不直接暴露测试框架内部实现类名，而是约束公开 `CaModule` 家族在测试项目结构中的职责分工，
 * 避免测试数据被具体实现类名绑死。
 */
enum class ExpectedCaModuleShape {
    SourceModule,
    LibraryBinaryModule,
    LibrarySourceModule,
    BuiltinsModule,
    LibraryFallbackDependenciesModule,
    DanglingFileModule,
    NotUnderContentRootModule,
}

/**
 * Analysis API 项目结构 generated tests 的模块级断言指令。
 *
 * 与组件测试的文件级指令不同，这一层直接描述模块图约束：
 * 1. 主模块应映射成哪类 `CaModule`
 * 2. 是否暴露 binary artifact view
 * 3. 应注入哪些 auxiliary modules
 * 4. 主模块最终应看到哪些 direct regular / friend dependencies
 * 5. dangling / not-under-content-root 场景下的上下文模块绑定是否正确
 */
object AnalysisApiProjectStructureTestDirectives : SimpleDirectivesContainer() {
    /**
     * 记录主测试模块在 Analysis API 中应映射成的模块形状。
     *
     * 该字段是项目结构测试的核心断言，用于确认 source、library、builtins、dangling file 等
     * 不同输入模块被转换为正确的公开 `CaModule` 家族。
     */
    val EXPECTED_PRIMARY_MODULE_SHAPE by enumDirective<ExpectedCaModuleShape>(
        description = "主模块在 Analysis API 中应映射成的 `CaModule` 形状。",
        applicability = DirectiveApplicability.Module,
    )

    /**
     * 记录 binary artifact view 应映射成的模块形状。
     *
     * 该字段可省略；省略表示当前模块不应暴露 binary artifact 模块，存在时则用于校验
     * artifact 视图与主模块的结构区分。
     */
    val EXPECTED_BINARY_ARTIFACT_MODULE_SHAPE by enumDirective<ExpectedCaModuleShape>(
        description = "binary artifact view 应映射成的 `CaModule` 形状；省略表示当前场景不应暴露 binary view。",
        applicability = DirectiveApplicability.Module,
    )

    /**
     * 记录测试框架应为当前模块额外注入的 auxiliary module 形状。
     *
     * 用例通过该指令检查 builtins、fallback dependencies 等辅助模块是否以公开模块形态接入项目图。
     */
    val EXPECTED_AUXILIARY_MODULE_SHAPE by enumDirective<ExpectedCaModuleShape>(
        description = "测试框架应为当前模块额外注入的 auxiliary module 形状。",
        applicability = DirectiveApplicability.Module,
    )

    /**
     * 记录主模块 direct regular dependencies 中应出现的模块名。
     *
     * 该字段约束普通依赖边，防止项目结构转换时把 regular dependency 漏接或误归类为 friend dependency。
     */
    val EXPECTED_DIRECT_REGULAR_DEPENDENCY by stringDirective(
        description = "主模块 direct regular dependencies 中应出现的模块名。",
        applicability = DirectiveApplicability.Module,
    )

    /**
     * 记录主模块 direct friend dependencies 中应出现的模块名。
     *
     * 该字段用于断言 friend dependency 边，保证跨模块可见性相关测试建立在正确项目图上。
     */
    val EXPECTED_DIRECT_FRIEND_DEPENDENCY by stringDirective(
        description = "主模块 direct friend dependencies 中应出现的模块名。",
        applicability = DirectiveApplicability.Module,
    )

    /**
     * 记录 dangling file 模块应绑定到的上下文模块名。
     *
     * 该字段用于校验临时文件或悬挂文件分析时的 use-site module 选择是否稳定。
     */
    val EXPECTED_CONTEXT_MODULE by stringDirective(
        description = "dangling file 模块应绑定到的上下文模块名。",
        applicability = DirectiveApplicability.Module,
    )

    /**
     * 记录 not-under-content-root 模块应绑定到的原始模块名。
     *
     * 该字段覆盖不在内容根下文件的项目结构归属，确保 Analysis API 可以追溯其真实上下文。
     */
    val EXPECTED_ORIGINAL_MODULE by stringDirective(
        description = "not-under-content-root 模块应绑定到的原始模块名。",
        applicability = DirectiveApplicability.Module,
    )

    /**
     * 记录主模块是否应可直接作为 use-site module 解析。
     *
     * 该布尔指令用于区分真实可解析模块与只作为结构节点存在的辅助模块。
     */
    val EXPECTED_IS_RESOLVABLE by stringDirective(
        description = "主模块对 Analysis API 是否应可直接作为 use-site module 解析。",
        applicability = DirectiveApplicability.Module,
    )
}

/**
 * 读取主模块形状的期望值。
 *
 * 返回值用于项目结构测试的第一层断言，确认测试模块到公开 `CaModule` 的映射结果。
 */
val RegisteredDirectives.expectedPrimaryModuleShape: ExpectedCaModuleShape
    get() = singleValue(AnalysisApiProjectStructureTestDirectives.EXPECTED_PRIMARY_MODULE_SHAPE)

/**
 * 读取 binary artifact 模块形状的可选期望。
 *
 * 返回 `null` 表示当前模块不应提供 binary artifact view。
 */
val RegisteredDirectives.expectedBinaryArtifactModuleShape: ExpectedCaModuleShape?
    get() = singleOrZeroValue(AnalysisApiProjectStructureTestDirectives.EXPECTED_BINARY_ARTIFACT_MODULE_SHAPE)

/**
 * 读取 dangling file 上下文模块名的可选期望。
 *
 * 该值存在时，测试会校验悬挂文件模块绑定到指定上下文模块。
 */
val RegisteredDirectives.expectedContextModuleName: String?
    get() = singleOrZeroValue(AnalysisApiProjectStructureTestDirectives.EXPECTED_CONTEXT_MODULE)

/**
 * 读取 not-under-content-root 原始模块名的可选期望。
 *
 * 该值存在时，测试会校验非内容根文件模块能回溯到指定原始模块。
 */
val RegisteredDirectives.expectedOriginalModuleName: String?
    get() = singleOrZeroValue(AnalysisApiProjectStructureTestDirectives.EXPECTED_ORIGINAL_MODULE)

/**
 * 读取主模块是否应可解析的期望值。
 *
 * 访问器使用严格布尔解析，保证项目结构 testData 的可解析性约束没有宽松拼写。
 */
val RegisteredDirectives.expectedResolvable: Boolean
    get() = singleValue(AnalysisApiProjectStructureTestDirectives.EXPECTED_IS_RESOLVABLE).toBooleanStrict()
