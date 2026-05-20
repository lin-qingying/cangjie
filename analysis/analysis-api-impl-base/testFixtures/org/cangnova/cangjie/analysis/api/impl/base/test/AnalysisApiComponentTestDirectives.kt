package org.cangnova.cangjie.analysis.api.impl.base.test

import org.cangnova.cangjie.test.directives.model.DirectiveApplicability
import org.cangnova.cangjie.test.directives.model.SimpleDirectivesContainer
import org.cangnova.cangjie.test.directives.model.singleValue

/**
 * Analysis API 组件测试的文件级指令集合。
 *
 * 这里统一声明组件测试会消费的 testData 元信息，让抽象测试基类只负责：
 * 1. 定位目标 PSI 或公开语义入口；
 * 2. 读取期望结果；
 * 3. 对公开 Analysis API 行为做断言。
 *
 * 这样新增测试目录时，只需要补 testData 和抽象测试，不需要在具体 runner 中重复硬编码语义。
 */
object AnalysisApiComponentTestDirectives : SimpleDirectivesContainer() {
    val TARGET_CALL by stringDirective(
        description = "指定当前测试应定位的调用表达式文本。",
        applicability = DirectiveApplicability.File,
    )

    val TARGET_NAME by stringDirective(
        description = "指定当前测试应定位的 simple-name 文本。",
        applicability = DirectiveApplicability.File,
    )

    val TARGET_CLASS by stringDirective(
        description = "指定当前测试应定位的类声明名称。",
        applicability = DirectiveApplicability.File,
    )

    val TARGET_FUNCTION by stringDirective(
        description = "指定当前测试应定位的函数声明名称。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_CALLABLE_NAME by stringDirective(
        description = "调用解析或符号解析后应暴露的 callable 名称。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_EXPLICIT_RECEIVER_TYPE by stringDirective(
        description = "调用解析后显式接收者应渲染出的公开类型。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_ARGUMENT_TYPE by stringDirective(
        description = "调用解析后参数列表中每个参数的公开类型。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_EXPRESSION_TYPE by stringDirective(
        description = "表达式类型或类型指针恢复后应渲染出的公开类型。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_DECLARATION_RETURN_TYPE by stringDirective(
        description = "声明返回类型查询后应渲染出的公开类型。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_FILE_SYMBOL_NAME by stringDirective(
        description = "fileSymbol 应暴露的文件名。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_FILE_SYMBOL_PACKAGE by stringDirective(
        description = "fileSymbol 应暴露的包名。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_PACKAGE_SYMBOL_FQ_NAME by stringDirective(
        description = "包符号查询后应暴露的包全名。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_CLASS_ID by stringDirective(
        description = "class-like 符号应暴露的 ClassId 文本。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_CALLABLE_ID by stringDirective(
        description = "callable 符号应暴露的 CallableId 文本。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_RENDERED_CLASS_SYMBOL by stringDirective(
        description = "renderer.render(symbol) 应输出的 class-like 符号文本。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_RENDERED_CALLABLE_SYMBOL by stringDirective(
        description = "renderer.render(symbol) 应输出的 callable 符号文本。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_RENDERED_TYPE by stringDirective(
        description = "renderer.render(type) 或 CaType.render() 应输出的公开类型文本。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_DEFAULT_REGULAR_IMPORT by stringDirective(
        description = "默认 regular imports 应暴露的 import 文本。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_DEFAULT_LOW_PRIORITY_IMPORT by stringDirective(
        description = "默认 low-priority imports 应暴露的 import 文本。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_EXCLUDED_IMPORT by stringDirective(
        description = "默认 excluded imports 应暴露的包名。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_SUBSTITUTED_PARAMETER_TYPE by stringDirective(
        description = "签名替换后参数类型应渲染出的文本。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_SUBSTITUTED_RETURN_TYPE by stringDirective(
        description = "签名替换后返回类型应渲染出的文本。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_REFERENCE_SHORTENING_OPERATION by stringDirective(
        description = "引用缩短计划中的公开操作，格式为 expression|shortName|status|requiredImport。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_RETAINED_IMPORT by stringDirective(
        description = "导入优化后应保留的 import 文本。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_DUPLICATE_IMPORT by stringDirective(
        description = "导入优化后应识别为重复的 import 文本。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_UNUSED_IMPORT by stringDirective(
        description = "导入优化后应识别为未使用的 import 文本。",
        applicability = DirectiveApplicability.File,
    )

    val EXPECTED_MISSING_IMPORT by stringDirective(
        description = "导入优化后应建议补齐的 import 文本。",
        applicability = DirectiveApplicability.File,
    )

    val FILE_SCOPE_AVAILABLE_NAME by stringDirective(
        description = "文件作用域公开暴露的可查询名字。",
        applicability = DirectiveApplicability.File,
    )

    val FILE_SCOPE_CLASSIFIER by stringDirective(
        description = "文件作用域中应能按名查询到的 classifier。",
        applicability = DirectiveApplicability.File,
    )

    val FILE_SCOPE_CALLABLE by stringDirective(
        description = "文件作用域中应能按名查询到的 callable。",
        applicability = DirectiveApplicability.File,
    )

    val FILE_SCOPE_ABSENT_NAME by stringDirective(
        description = "文件作用域中不应出现的名字。",
        applicability = DirectiveApplicability.File,
    )

    val PACKAGE_SCOPE_CLASSIFIER by stringDirective(
        description = "包作用域中应能按名查询到的 classifier。",
        applicability = DirectiveApplicability.File,
    )

    val PACKAGE_SCOPE_CALLABLE by stringDirective(
        description = "包作用域中应能按名查询到的 callable。",
        applicability = DirectiveApplicability.File,
    )

    val PACKAGE_SCOPE_AVAILABLE_NAME by stringDirective(
        description = "包作用域中应可稳定枚举的名字。",
        applicability = DirectiveApplicability.File,
    )

    val DECLARED_MEMBER_SCOPE_AVAILABLE_NAME by stringDirective(
        description = "声明成员作用域中应可稳定枚举的名字。",
        applicability = DirectiveApplicability.File,
    )

    val DECLARED_MEMBER_SCOPE_CLASSIFIER by stringDirective(
        description = "声明成员作用域中应能按名查询到的 classifier。",
        applicability = DirectiveApplicability.File,
    )

    val DECLARED_MEMBER_SCOPE_CALLABLE by stringDirective(
        description = "声明成员作用域中应能按名查询到的 callable。",
        applicability = DirectiveApplicability.File,
    )

    val MEMBER_SCOPE_AVAILABLE_NAME by stringDirective(
        description = "use-site 成员作用域中应可稳定枚举的名字。",
        applicability = DirectiveApplicability.File,
    )

    val MEMBER_SCOPE_CLASSIFIER by stringDirective(
        description = "use-site 成员作用域中应能按名查询到的 classifier。",
        applicability = DirectiveApplicability.File,
    )

    val MEMBER_SCOPE_CALLABLE by stringDirective(
        description = "use-site 成员作用域中应能按名查询到的 callable。",
        applicability = DirectiveApplicability.File,
    )

    val TYPE_SCOPE_AVAILABLE_NAME by stringDirective(
        description = "类型作用域中应可稳定枚举的名字。",
        applicability = DirectiveApplicability.File,
    )

    val TYPE_SCOPE_CLASSIFIER by stringDirective(
        description = "类型作用域中应能按名查询到的 classifier。",
        applicability = DirectiveApplicability.File,
    )

    val TYPE_SCOPE_CALLABLE by stringDirective(
        description = "类型作用域中应能按名查询到的 callable。",
        applicability = DirectiveApplicability.File,
    )
}

val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.targetCallText: String
    get() = singleValue(AnalysisApiComponentTestDirectives.TARGET_CALL)

val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.targetNameText: String
    get() = singleValue(AnalysisApiComponentTestDirectives.TARGET_NAME)

val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.targetClassName: String
    get() = singleValue(AnalysisApiComponentTestDirectives.TARGET_CLASS)

val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.targetFunctionName: String
    get() = singleValue(AnalysisApiComponentTestDirectives.TARGET_FUNCTION)

val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.expectedCallableName: String
    get() = singleValue(AnalysisApiComponentTestDirectives.EXPECTED_CALLABLE_NAME)

val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.expectedExplicitReceiverType: String
    get() = singleValue(AnalysisApiComponentTestDirectives.EXPECTED_EXPLICIT_RECEIVER_TYPE)

val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.expectedExpressionType: String
    get() = singleValue(AnalysisApiComponentTestDirectives.EXPECTED_EXPRESSION_TYPE)

val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.expectedDeclarationReturnType: String
    get() = singleValue(AnalysisApiComponentTestDirectives.EXPECTED_DECLARATION_RETURN_TYPE)

val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.expectedFileSymbolName: String
    get() = singleValue(AnalysisApiComponentTestDirectives.EXPECTED_FILE_SYMBOL_NAME)

val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.expectedFileSymbolPackage: String
    get() = singleValue(AnalysisApiComponentTestDirectives.EXPECTED_FILE_SYMBOL_PACKAGE)

val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.expectedPackageSymbolFqName: String
    get() = singleValue(AnalysisApiComponentTestDirectives.EXPECTED_PACKAGE_SYMBOL_FQ_NAME)

val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.expectedClassId: String
    get() = singleValue(AnalysisApiComponentTestDirectives.EXPECTED_CLASS_ID)

val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.expectedCallableId: String
    get() = singleValue(AnalysisApiComponentTestDirectives.EXPECTED_CALLABLE_ID)

val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.expectedRenderedClassSymbol: String
    get() = this[AnalysisApiComponentTestDirectives.EXPECTED_RENDERED_CLASS_SYMBOL].joinToString(" ")

val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.expectedRenderedCallableSymbol: String
    get() = this[AnalysisApiComponentTestDirectives.EXPECTED_RENDERED_CALLABLE_SYMBOL].joinToString(" ")

val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.expectedRenderedType: String
    get() = singleValue(AnalysisApiComponentTestDirectives.EXPECTED_RENDERED_TYPE)

val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.expectedSubstitutedParameterType: String
    get() = singleValue(AnalysisApiComponentTestDirectives.EXPECTED_SUBSTITUTED_PARAMETER_TYPE)

val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.expectedSubstitutedReturnType: String
    get() = singleValue(AnalysisApiComponentTestDirectives.EXPECTED_SUBSTITUTED_RETURN_TYPE)
