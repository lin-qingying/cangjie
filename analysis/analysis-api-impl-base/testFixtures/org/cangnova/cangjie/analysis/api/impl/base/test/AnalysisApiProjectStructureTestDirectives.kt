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
    ScriptModule,
    LibraryBinaryModule,
    LibrarySourceModule,
    BuiltinsModule,
    ScriptDependenciesModule,
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
    val EXPECTED_PRIMARY_MODULE_SHAPE by enumDirective<ExpectedCaModuleShape>(
        description = "主模块在 Analysis API 中应映射成的 `CaModule` 形状。",
        applicability = DirectiveApplicability.Module,
    )

    val EXPECTED_BINARY_ARTIFACT_MODULE_SHAPE by enumDirective<ExpectedCaModuleShape>(
        description = "binary artifact view 应映射成的 `CaModule` 形状；省略表示当前场景不应暴露 binary view。",
        applicability = DirectiveApplicability.Module,
    )

    val EXPECTED_AUXILIARY_MODULE_SHAPE by enumDirective<ExpectedCaModuleShape>(
        description = "测试框架应为当前模块额外注入的 auxiliary module 形状。",
        applicability = DirectiveApplicability.Module,
    )

    val EXPECTED_DIRECT_REGULAR_DEPENDENCY by stringDirective(
        description = "主模块 direct regular dependencies 中应出现的模块名。",
        applicability = DirectiveApplicability.Module,
    )

    val EXPECTED_DIRECT_FRIEND_DEPENDENCY by stringDirective(
        description = "主模块 direct friend dependencies 中应出现的模块名。",
        applicability = DirectiveApplicability.Module,
    )

    val EXPECTED_CONTEXT_MODULE by stringDirective(
        description = "dangling file 模块应绑定到的上下文模块名。",
        applicability = DirectiveApplicability.Module,
    )

    val EXPECTED_ORIGINAL_MODULE by stringDirective(
        description = "not-under-content-root 模块应绑定到的原始模块名。",
        applicability = DirectiveApplicability.Module,
    )

    val EXPECTED_IS_RESOLVABLE by stringDirective(
        description = "主模块对 Analysis API 是否应可直接作为 use-site module 解析。",
        applicability = DirectiveApplicability.Module,
    )
}

val RegisteredDirectives.expectedPrimaryModuleShape: ExpectedCaModuleShape
    get() = singleValue(AnalysisApiProjectStructureTestDirectives.EXPECTED_PRIMARY_MODULE_SHAPE)

val RegisteredDirectives.expectedBinaryArtifactModuleShape: ExpectedCaModuleShape?
    get() = singleOrZeroValue(AnalysisApiProjectStructureTestDirectives.EXPECTED_BINARY_ARTIFACT_MODULE_SHAPE)

val RegisteredDirectives.expectedContextModuleName: String?
    get() = singleOrZeroValue(AnalysisApiProjectStructureTestDirectives.EXPECTED_CONTEXT_MODULE)

val RegisteredDirectives.expectedOriginalModuleName: String?
    get() = singleOrZeroValue(AnalysisApiProjectStructureTestDirectives.EXPECTED_ORIGINAL_MODULE)

val RegisteredDirectives.expectedResolvable: Boolean
    get() = singleValue(AnalysisApiProjectStructureTestDirectives.EXPECTED_IS_RESOLVABLE).toBooleanStrict()
