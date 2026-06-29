package org.cangnova.cangjie.analysis.test.framework

import org.cangnova.cangjie.analysis.test.framework.test.configurators.TestModuleKind
import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.directives.model.singleOrZeroValue
import org.cangnova.cangjie.test.model.TestModule

/**
 * Analysis API 测试框架级指令集合。
 *
 * 这些指令不描述某个组件的断言细节，而是声明：
 * 1. 测试模块应映射成哪一类 Analysis API 模块；
 * 2. 多模块、多文件场景下哪个模块或文件是 use-site 入口；
 * 3. 测试框架是否需要切换运行模式或依赖建模方式。
 */
object AnalysisApiTestDirectives : SimpleDirectivesContainer() {
    /**
     * 显式指定测试模块映射到的 Analysis API 模块种类。
     */
    val MODULE_KIND by enumDirective<TestModuleKind>(
        description = "显式指定测试模块映射到的 Analysis API 模块种类。",
        applicability = DirectiveApplicability.Module,
    )

    /**
     * 禁止当前测试在 dependent session 模式下生成或运行。
     */
    val DISABLE_DEPENDED_MODE by directive(
        description = "当前测试不应在 dependent session 模式下运行。",
        applicability = DirectiveApplicability.Any,
    )

    /**
     * 禁止当前测试在 standalone Analysis API 模式下生成或运行。
     */
    val IGNORE_STANDALONE by directive(
        description = "跳过 Standalone Analysis API 运行模式。",
        applicability = DirectiveApplicability.Any,
    )

    /**
     * 当测试模块包含多个源文件时，显式声明主文件。
     */
    val MAIN_FILE_NAME by stringDirective(
        description = "指定当前测试模块中的主文件名。",
        applicability = DirectiveApplicability.Module,
    )

    /**
     * 显式声明主模块，避免多模块测试依赖名称约定推导。
     */
    val MAIN_MODULE by directive(
        description = "标记当前模块是 Analysis API 测试入口模块。",
        applicability = DirectiveApplicability.Module,
    )

    /**
     * 指定当前模块作为 dangling/code-fragment 等场景的上下文模块。
     */
    val CONTEXT_MODULE by stringDirective(
        description = "指定当前模块对应的上下文模块名。",
        applicability = DirectiveApplicability.Module,
    )

    /**
     * 指定分析入口使用的 use-site 上下文模块名。
     */
    val ANALYSIS_CONTEXT_MODULE by stringDirective(
        description = "指定当前模块分析时采用的 use-site 上下文模块名。",
        applicability = DirectiveApplicability.Module,
    )

    /**
     * 要求库模块通过 fallback dependencies 建模默认可见依赖。
     */
    val FALLBACK_DEPENDENCIES by directive(
        description = "要求库模块使用 fallback dependencies 模块，而不是显式 regular/friend 依赖。",
        applicability = DirectiveApplicability.Module,
    )

    /**
     * 将库模块标记为 SDK 库，供测试 project structure 建模内建/标准库来源。
     */
    val SDK_LIBRARY by directive(
        description = "将当前库模块标记为宿主 SDK 库。",
        applicability = DirectiveApplicability.Module,
    )
}

/**
 * 当前测试模块显式声明的 Analysis API 模块种类。
 */
val TestModule.analysisApiModuleKind: TestModuleKind?
    get() = directives.singleOrZeroValue(AnalysisApiTestDirectives.MODULE_KIND)

/**
 * 当前测试模块显式声明的主文件名。
 */
val TestModule.analysisApiMainFileName: String?
    get() = directives.singleOrZeroValue(AnalysisApiTestDirectives.MAIN_FILE_NAME)

/**
 * 当前测试模块是否被标记为 Analysis API 主模块。
 */
val TestModule.isAnalysisApiMainModule: Boolean
    get() = AnalysisApiTestDirectives.MAIN_MODULE in directives

/**
 * 当前测试模块声明的结构上下文模块名。
 */
val TestModule.analysisApiContextModuleName: String?
    get() = directives.singleOrZeroValue(AnalysisApiTestDirectives.CONTEXT_MODULE)

/**
 * 当前测试模块声明的分析 use-site 上下文模块名。
 */
val TestModule.analysisApiUseSiteContextModuleName: String?
    get() = directives.singleOrZeroValue(AnalysisApiTestDirectives.ANALYSIS_CONTEXT_MODULE)

/**
 * 当前测试模块是否要求创建 fallback dependencies 模块。
 */
val TestModule.hasAnalysisApiFallbackDependencies: Boolean
    get() = AnalysisApiTestDirectives.FALLBACK_DEPENDENCIES in directives

/**
 * 当前测试模块是否代表宿主 SDK 库。
 */
val TestModule.isAnalysisApiSdkLibrary: Boolean
    get() = AnalysisApiTestDirectives.SDK_LIBRARY in directives
