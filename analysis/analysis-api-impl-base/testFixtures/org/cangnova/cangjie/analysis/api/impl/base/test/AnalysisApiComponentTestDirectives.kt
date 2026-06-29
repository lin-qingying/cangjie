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
    /**
     * 指定当前测试应定位的调用表达式文本。
     *
     * 该字段为 resolver、call model 和参数类型相关测试提供统一入口，测试基类会在主文件中定位
     * 对应调用并把它交给公开 Analysis API 查询。
     */
    val TARGET_CALL by stringDirective(
        description = "指定当前测试应定位的调用表达式文本。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 指定当前测试应定位的 simple-name 文本。
     *
     * 多个引用、symbol 和 usages 测试族复用该字段，确保按名称定位的测试都遵循同一个目标选择协议。
     */
    val TARGET_NAME by stringDirective(
        description = "指定当前测试应定位的 simple-name 文本。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 指定当前测试应定位的类声明名称。
     *
     * 该字段用于 class-like symbol、类型构造、继承关系和作用域测试，要求测试数据中的目标类名稳定唯一。
     */
    val TARGET_CLASS by stringDirective(
        description = "指定当前测试应定位的类声明名称。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 指定当前测试应定位的函数声明名称。
     *
     * 函数相关测试通过该字段恢复目标函数声明或 callable symbol，并断言其公开 Analysis API 视图。
     */
    val TARGET_FUNCTION by stringDirective(
        description = "指定当前测试应定位的函数声明名称。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录调用解析或符号解析后应暴露的 callable 名称。
     *
     * 该期望用于确认 resolver 返回的语义目标正确，而不是仅匹配调用表达式中的文本名称。
     */
    val EXPECTED_CALLABLE_NAME by stringDirective(
        description = "调用解析或符号解析后应暴露的 callable 名称。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录调用解析后显式接收者应渲染出的公开类型。
     *
     * 该期望用于覆盖成员调用、扩展成员和带接收者调用的接收者类型推断结果。
     */
    val EXPECTED_EXPLICIT_RECEIVER_TYPE by stringDirective(
        description = "调用解析后显式接收者应渲染出的公开类型。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录调用解析后参数列表中每个参数的公开类型。
     *
     * 该字段约束 call model 中 value argument 到形参类型的映射结果，便于发现参数类型补全或替换漂移。
     */
    val EXPECTED_ARGUMENT_TYPE by stringDirective(
        description = "调用解析后参数列表中每个参数的公开类型。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录表达式类型或类型指针恢复后应渲染出的公开类型。
     *
     * 多个表达式类型测试共用该字段，将语义查询结果固定为可比较的文本快照。
     */
    val EXPECTED_EXPRESSION_TYPE by stringDirective(
        description = "表达式类型或类型指针恢复后应渲染出的公开类型。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录声明返回类型查询后应渲染出的公开类型。
     *
     * 该期望用于函数、属性等声明的 return type 查询，避免测试直接依赖内部类型引用结构。
     */
    val EXPECTED_DECLARATION_RETURN_TYPE by stringDirective(
        description = "声明返回类型查询后应渲染出的公开类型。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录 fileSymbol 应暴露的文件名。
     *
     * 文件符号测试通过该字段确认主文件映射到正确的公开 file symbol。
     */
    val EXPECTED_FILE_SYMBOL_NAME by stringDirective(
        description = "fileSymbol 应暴露的文件名。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录 fileSymbol 应暴露的包名。
     *
     * 该字段与文件名期望配对，覆盖文件级 package 归属在公开符号模型中的呈现。
     */
    val EXPECTED_FILE_SYMBOL_PACKAGE by stringDirective(
        description = "fileSymbol 应暴露的包名。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录包符号查询后应暴露的包全名。
     *
     * package symbol 测试使用该字段断言包级符号身份，而不是只验证文件中的 package directive 文本。
     */
    val EXPECTED_PACKAGE_SYMBOL_FQ_NAME by stringDirective(
        description = "包符号查询后应暴露的包全名。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录 class-like 符号应暴露的 `ClassId` 文本。
     *
     * 该期望用于确认 class、interface、struct、enum 等类型声明在公开符号层拥有稳定身份。
     */
    val EXPECTED_CLASS_ID by stringDirective(
        description = "class-like 符号应暴露的 ClassId 文本。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录 callable 符号应暴露的 `CallableId` 文本。
     *
     * 该期望用于确认函数、属性和扩展 callable 在公开符号层拥有正确 owner 与名称。
     */
    val EXPECTED_CALLABLE_ID by stringDirective(
        description = "callable 符号应暴露的 CallableId 文本。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录 class-like symbol renderer 应输出的文本。
     *
     * 该字段覆盖声明 renderer 对类型声明符号的公开展示结果。
     */
    val EXPECTED_RENDERED_CLASS_SYMBOL by stringDirective(
        description = "renderer.render(symbol) 应输出的 class-like 符号文本。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录 callable symbol renderer 应输出的文本。
     *
     * 该字段覆盖声明 renderer 对函数、属性等 callable 符号的公开展示结果。
     */
    val EXPECTED_RENDERED_CALLABLE_SYMBOL by stringDirective(
        description = "renderer.render(symbol) 应输出的 callable 符号文本。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录 type renderer 或 `CaType.render()` 应输出的文本。
     *
     * 类型相关测试通过该字段统一比较公开类型渲染结果。
     */
    val EXPECTED_RENDERED_TYPE by stringDirective(
        description = "renderer.render(type) 或 CaType.render() 应输出的公开类型文本。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录默认 regular imports 应暴露的 import 文本。
     *
     * 默认导入测试通过该字段断言普通优先级导入集合是否稳定。
     */
    val EXPECTED_DEFAULT_REGULAR_IMPORT by stringDirective(
        description = "默认 regular imports 应暴露的 import 文本。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录默认 low-priority imports 应暴露的 import 文本。
     *
     * 该字段用于区分普通导入和低优先级导入，避免默认导入优先级被折叠。
     */
    val EXPECTED_DEFAULT_LOW_PRIORITY_IMPORT by stringDirective(
        description = "默认 low-priority imports 应暴露的 import 文本。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录默认 excluded imports 应暴露的包名。
     *
     * 测试通过该字段确认被排除的默认导入不会参与普通默认导入解析。
     */
    val EXPECTED_EXCLUDED_IMPORT by stringDirective(
        description = "默认 excluded imports 应暴露的包名。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录签名替换后参数类型应渲染出的文本。
     *
     * signature substitutor 测试用该字段断言类型参数替换后的 value parameter 类型。
     */
    val EXPECTED_SUBSTITUTED_PARAMETER_TYPE by stringDirective(
        description = "签名替换后参数类型应渲染出的文本。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录签名替换后返回类型应渲染出的文本。
     *
     * 该字段与参数类型期望配对，覆盖 callable signature 替换后的返回类型视图。
     */
    val EXPECTED_SUBSTITUTED_RETURN_TYPE by stringDirective(
        description = "签名替换后返回类型应渲染出的文本。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录引用缩短计划中的公开操作文本。
     *
     * 格式为 `expression|shortName|status|requiredImport`，用于稳定比较 shorten reference 计划。
     */
    val EXPECTED_REFERENCE_SHORTENING_OPERATION by stringDirective(
        description = "引用缩短计划中的公开操作，格式为 expression|shortName|status|requiredImport。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录导入优化后应保留的 import 文本。
     *
     * import optimization 测试通过该字段确认必要导入不会被误删。
     */
    val EXPECTED_RETAINED_IMPORT by stringDirective(
        description = "导入优化后应保留的 import 文本。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录导入优化后应识别为重复的 import 文本。
     *
     * 该字段用于断言重复导入检测结果。
     */
    val EXPECTED_DUPLICATE_IMPORT by stringDirective(
        description = "导入优化后应识别为重复的 import 文本。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录导入优化后应识别为未使用的 import 文本。
     *
     * 该字段用于断言未使用导入分析结果。
     */
    val EXPECTED_UNUSED_IMPORT by stringDirective(
        description = "导入优化后应识别为未使用的 import 文本。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录导入优化后应建议补齐的 import 文本。
     *
     * 该字段用于断言缺失导入补全计划。
     */
    val EXPECTED_MISSING_IMPORT by stringDirective(
        description = "导入优化后应建议补齐的 import 文本。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录文件作用域中应可稳定枚举的名字。
     *
     * file scope 测试用该字段校验公开作用域的名称集合。
     */
    val FILE_SCOPE_AVAILABLE_NAME by stringDirective(
        description = "文件作用域公开暴露的可查询名字。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录文件作用域中应能按名查询到的 classifier。
     *
     * 该字段覆盖 file scope 的 classifier 查找入口。
     */
    val FILE_SCOPE_CLASSIFIER by stringDirective(
        description = "文件作用域中应能按名查询到的 classifier。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录文件作用域中应能按名查询到的 callable。
     *
     * 该字段覆盖 file scope 的 callable 查找入口。
     */
    val FILE_SCOPE_CALLABLE by stringDirective(
        description = "文件作用域中应能按名查询到的 callable。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录文件作用域中不应出现的名字。
     *
     * 该字段用于负向断言，确认作用域不会暴露不属于当前文件可见集合的名称。
     */
    val FILE_SCOPE_ABSENT_NAME by stringDirective(
        description = "文件作用域中不应出现的名字。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录包作用域中应能按名查询到的 classifier。
     *
     * package scope 测试用该字段校验包级 classifier 查询。
     */
    val PACKAGE_SCOPE_CLASSIFIER by stringDirective(
        description = "包作用域中应能按名查询到的 classifier。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录包作用域中应能按名查询到的 callable。
     *
     * package scope 测试用该字段校验包级 callable 查询。
     */
    val PACKAGE_SCOPE_CALLABLE by stringDirective(
        description = "包作用域中应能按名查询到的 callable。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录包作用域中应可稳定枚举的名字。
     *
     * 该字段约束 package scope 的名称枚举结果。
     */
    val PACKAGE_SCOPE_AVAILABLE_NAME by stringDirective(
        description = "包作用域中应可稳定枚举的名字。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录声明成员作用域中应可稳定枚举的名字。
     *
     * declared member scope 测试通过该字段确认源码声明成员集合。
     */
    val DECLARED_MEMBER_SCOPE_AVAILABLE_NAME by stringDirective(
        description = "声明成员作用域中应可稳定枚举的名字。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录声明成员作用域中应能按名查询到的 classifier。
     *
     * 该字段覆盖声明成员作用域的 nested classifier 查询。
     */
    val DECLARED_MEMBER_SCOPE_CLASSIFIER by stringDirective(
        description = "声明成员作用域中应能按名查询到的 classifier。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录声明成员作用域中应能按名查询到的 callable。
     *
     * 该字段覆盖声明成员作用域的 callable 查询。
     */
    val DECLARED_MEMBER_SCOPE_CALLABLE by stringDirective(
        description = "声明成员作用域中应能按名查询到的 callable。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录 use-site 成员作用域中应可稳定枚举的名字。
     *
     * member scope 测试通过该字段确认继承、扩展和可见性合成后的名称集合。
     */
    val MEMBER_SCOPE_AVAILABLE_NAME by stringDirective(
        description = "use-site 成员作用域中应可稳定枚举的名字。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录 use-site 成员作用域中应能按名查询到的 classifier。
     *
     * 该字段覆盖 member scope 的 classifier 查询结果。
     */
    val MEMBER_SCOPE_CLASSIFIER by stringDirective(
        description = "use-site 成员作用域中应能按名查询到的 classifier。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录 use-site 成员作用域中应能按名查询到的 callable。
     *
     * 该字段覆盖 member scope 的 callable 查询结果。
     */
    val MEMBER_SCOPE_CALLABLE by stringDirective(
        description = "use-site 成员作用域中应能按名查询到的 callable。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录类型作用域中应可稳定枚举的名字。
     *
     * type scope 测试用该字段确认通过类型视角暴露的成员名称集合。
     */
    val TYPE_SCOPE_AVAILABLE_NAME by stringDirective(
        description = "类型作用域中应可稳定枚举的名字。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录类型作用域中应能按名查询到的 classifier。
     *
     * 该字段覆盖 type scope 的 classifier 查询入口。
     */
    val TYPE_SCOPE_CLASSIFIER by stringDirective(
        description = "类型作用域中应能按名查询到的 classifier。",
        applicability = DirectiveApplicability.File,
    )

    /**
     * 记录类型作用域中应能按名查询到的 callable。
     *
     * 该字段覆盖 type scope 的 callable 查询入口。
     */
    val TYPE_SCOPE_CALLABLE by stringDirective(
        description = "类型作用域中应能按名查询到的 callable。",
        applicability = DirectiveApplicability.File,
    )
}

/**
 * 读取当前测试要定位的调用表达式文本。
 *
 * 访问器封装 `TARGET_CALL` 的单值读取规则，供调用解析相关测试直接获取稳定目标。
 */
val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.targetCallText: String
    get() = singleValue(AnalysisApiComponentTestDirectives.TARGET_CALL)

/**
 * 读取当前测试要定位的 simple-name 文本。
 *
 * 多个测试族通过该访问器共享同一目标名称来源，避免重复解析公共 directive。
 */
val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.targetNameText: String
    get() = singleValue(AnalysisApiComponentTestDirectives.TARGET_NAME)

/**
 * 读取当前测试要定位的类声明名称。
 *
 * 返回值用于恢复 class-like PSI 或 symbol，是类相关组件测试的公共入口。
 */
val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.targetClassName: String
    get() = singleValue(AnalysisApiComponentTestDirectives.TARGET_CLASS)

/**
 * 读取当前测试要定位的函数声明名称。
 *
 * 函数、callable symbol 和返回类型测试通过该值锁定目标函数。
 */
val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.targetFunctionName: String
    get() = singleValue(AnalysisApiComponentTestDirectives.TARGET_FUNCTION)

/**
 * 读取调用或符号解析后期望得到的 callable 名称。
 *
 * 该值用于和公开 Analysis API 返回的 callable symbol 名称比较。
 */
val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.expectedCallableName: String
    get() = singleValue(AnalysisApiComponentTestDirectives.EXPECTED_CALLABLE_NAME)

/**
 * 读取显式接收者类型的期望渲染文本。
 *
 * 返回值用于断言调用模型中的 explicit receiver type。
 */
val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.expectedExplicitReceiverType: String
    get() = singleValue(AnalysisApiComponentTestDirectives.EXPECTED_EXPLICIT_RECEIVER_TYPE)

/**
 * 读取表达式类型的期望渲染文本。
 *
 * 该值覆盖表达式类型查询和类型指针恢复后的公开类型比较。
 */
val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.expectedExpressionType: String
    get() = singleValue(AnalysisApiComponentTestDirectives.EXPECTED_EXPRESSION_TYPE)

/**
 * 读取声明返回类型的期望渲染文本。
 *
 * 测试基类使用该值断言函数或属性声明的公开 return type。
 */
val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.expectedDeclarationReturnType: String
    get() = singleValue(AnalysisApiComponentTestDirectives.EXPECTED_DECLARATION_RETURN_TYPE)

/**
 * 读取 file symbol 名称的期望值。
 *
 * 返回值用于确认公开文件符号对应的源文件身份。
 */
val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.expectedFileSymbolName: String
    get() = singleValue(AnalysisApiComponentTestDirectives.EXPECTED_FILE_SYMBOL_NAME)

/**
 * 读取 file symbol 包名的期望值。
 *
 * 返回值用于确认文件符号暴露的 package 归属。
 */
val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.expectedFileSymbolPackage: String
    get() = singleValue(AnalysisApiComponentTestDirectives.EXPECTED_FILE_SYMBOL_PACKAGE)

/**
 * 读取 package symbol 全名的期望值。
 *
 * 该值用于比较包符号查询结果的公开 FqName。
 */
val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.expectedPackageSymbolFqName: String
    get() = singleValue(AnalysisApiComponentTestDirectives.EXPECTED_PACKAGE_SYMBOL_FQ_NAME)

/**
 * 读取 class-like symbol 的 `ClassId` 期望文本。
 *
 * 返回值用于断言公开符号身份是否与 testData 中声明的类型一致。
 */
val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.expectedClassId: String
    get() = singleValue(AnalysisApiComponentTestDirectives.EXPECTED_CLASS_ID)

/**
 * 读取 callable symbol 的 `CallableId` 期望文本。
 *
 * 返回值用于断言 callable 的 owner、名称和符号身份。
 */
val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.expectedCallableId: String
    get() = singleValue(AnalysisApiComponentTestDirectives.EXPECTED_CALLABLE_ID)

/**
 * 读取 class-like symbol renderer 的期望输出。
 *
 * 指令 token 会以空格还原，支持较长声明渲染文本在 testData 中分段记录。
 */
val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.expectedRenderedClassSymbol: String
    get() = this[AnalysisApiComponentTestDirectives.EXPECTED_RENDERED_CLASS_SYMBOL].joinToString(" ")

/**
 * 读取 callable symbol renderer 的期望输出。
 *
 * 指令 token 会以空格还原，供 renderer 组件测试直接比较完整文本。
 */
val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.expectedRenderedCallableSymbol: String
    get() = this[AnalysisApiComponentTestDirectives.EXPECTED_RENDERED_CALLABLE_SYMBOL].joinToString(" ")

/**
 * 读取 type renderer 的期望输出。
 *
 * 返回值用于统一比较公开类型渲染结果。
 */
val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.expectedRenderedType: String
    get() = singleValue(AnalysisApiComponentTestDirectives.EXPECTED_RENDERED_TYPE)

/**
 * 读取签名替换后参数类型的期望渲染文本。
 *
 * 该值用于 signature substitutor 测试的参数类型断言。
 */
val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.expectedSubstitutedParameterType: String
    get() = singleValue(AnalysisApiComponentTestDirectives.EXPECTED_SUBSTITUTED_PARAMETER_TYPE)

/**
 * 读取签名替换后返回类型的期望渲染文本。
 *
 * 该值用于 signature substitutor 测试的返回类型断言。
 */
val org.cangnova.cangjie.test.directives.model.RegisteredDirectives.expectedSubstitutedReturnType: String
    get() = singleValue(AnalysisApiComponentTestDirectives.EXPECTED_SUBSTITUTED_RETURN_TYPE)
