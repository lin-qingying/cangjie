package org.cangnova.cangjie.codeinsight.hints

import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPsiElementPointer

/**
 * 共享 inlay hint 的协议级类别。
 *
 * 取值与 LSP `InlayHintKind` 一一对应；`OTHER` 留给协议没有对应类别的自定义提示。
 */
enum class CangJieInlayHintKind {
    /** 类型提示（变量类型、返回值类型）。 */
    TYPE,

    /** 形参名提示。 */
    PARAMETER_NAME,

    /** 无协议类别的自定义提示。 */
    OTHER,
}

/**
 * 提示文本的一个片段。
 *
 * [target] 只有 IDE 侧会填（用于点击跳转）；LSP 协议层没有跨请求稳定的 PSI 指针，
 * 转换时会整体丢弃该字段。
 */
data class CangJieInlayHintLabelPart(
    /** 已渲染的纯文本片段。 */
    val text: String,
    /** 片段对应的跳转目标；仅 IDE 宿主可消费。 */
    val target: SmartPsiElementPointer<PsiElement>? = null,
)

/**
 * 与宿主无关的 inlay hint。
 *
 * 生命周期硬约束：本类型**不得**持有 `CaSession` 或任何 lifetime owner。
 * 所有需要 session 的渲染都必须在 `analyze` 块内完成，出块后只剩纯文本与 PSI 层锚点。
 */
data class CangJieInlayHint(
    /** 提示插入位置，对应 Analysis 文本内的 offset。 */
    val offset: Int,
    /** 提示左侧是否需要宿主补空白。 */
    val paddingLeft: Boolean = false,
    /** 提示右侧是否需要宿主补空白。 */
    val paddingRight: Boolean = false,
    /** 协议级类别。 */
    val kind: CangJieInlayHintKind,
    /** 该提示属于哪一类语义项，宿主据此映射到自己的设置项。 */
    val target: CangJieInlayHintTarget,
    /** 已渲染的标签片段序列。 */
    val label: List<CangJieInlayHintLabelPart>,
    /** 已渲染的纯文本悬浮提示。 */
    val tooltip: String? = null,
) {
    /**
     * 把标签片段拼成纯文本。
     *
     * golden 契约比对与 LSP 单字符串标签都只需要这一段，避免两处各写一遍拼接。
     */
    val plainText: String
        get() = label.joinToString(separator = "") { it.text }
}

/**
 * 提示的语义种类。
 *
 * 只描述「这是哪一类提示」，**不含**任何宿主的设置项字符串：IDE 需要按项开关时自己映射
 * 到 `optionId`（见 `CangJieSharedInlayHintsProvider.optionIdOf`），LSP 侧直接忽略该字段。
 * 这样共享层不必知道 IntelliJ 的 bundle key，也不会把 IDE 概念泄漏到协议层。
 */
enum class CangJieInlayHintTarget {
    /** 局部 `let`/`var` 的类型。 */
    LOCAL_VARIABLE_TYPE,

    /** 顶层变量声明的类型。 */
    VARIABLE_TYPE,

    /** `prop` / 类成员字段的类型。 */
    PROPERTY_TYPE,

    /** 函数返回值类型。 */
    FUNCTION_RETURN_TYPE,

    /** 调用点实参的形参名。 */
    PARAMETER_NAME,

    /** 范围/切片端点的开闭性。 */
    RANGE_VALUE,

    /** lambda 函数体末表达式的类型（即隐式返回值类型）。 */
    LAMBDA_RETURN_TYPE,

    /** 多行链式调用中每个环节的结果类型。 */
    CALL_CHAIN_TYPE,
}

/**
 * 共享 inlay hint 的开关。
 *
 * LSP 侧固定全开；IDE 侧由宿主设置逐项映射。缺少开关的种类不得在收集器里硬编码产出。
 */
data class CangJieInlayHintOptions(
    /** 是否显示类成员属性/字段的类型。 */
    val showPropertyTypes: Boolean = true,
    /** 是否显示局部变量（含顶层变量声明）的类型。 */
    val showLocalVariableTypes: Boolean = true,
    /** 是否显示函数返回值类型。 */
    val showFunctionReturnTypes: Boolean = true,
    /** 是否显示调用实参的形参名。 */
    val showParameterNames: Boolean = true,
    /** 是否显示范围/切片端点的开闭性。 */
    val showRangeValues: Boolean = true,
    /** 是否显示 lambda 隐式返回值类型。 */
    val showLambdaReturns: Boolean = true,
    /** 是否显示多行链式调用各环节的结果类型。 */
    val showCallChainTypes: Boolean = true,
)
