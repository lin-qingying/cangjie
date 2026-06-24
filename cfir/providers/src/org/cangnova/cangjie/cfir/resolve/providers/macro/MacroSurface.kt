package org.cangnova.cangjie.cfir.resolve.providers.macro

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

/**
 * Macro construction-only surface 节点。
 *
 * Baseline 第 2 节硬性边界 #7：`MacroSurface*` **不实现** `CfirElement`，
 * **不**进入 generated `cfir-tree` visitor / checker。任何把 surface 放进
 * provider-visible final CFIR 的尝试都应当被
 * [recordExpandedRawFilesOnce] 拒绝。
 *
 * Baseline 第 7 节列出 surface 必须承载的字段集合：
 * - [surfaceId] / [qualifiedName] / [kind] / [hasParenthesis]
 * - [attrTokens] / [inputTokens]
 * - [sourceRange] / [scopeContext]
 * - [modifiers] / [carriedAnnotations]
 * - [capturedRawSyntax]
 * - [containerContext]（outer declaration / primary constructor / enum /
 *   block / comma-list context）
 * - [replaceHandle]
 *
 * Batch 4a 阶段：surface 模型立起来，字段为最小可用类型；
 * PSI / LightTree builder 双覆盖（baseline Batch 4 第二项）留到 4b。
 *
 * @property surfaceId construction 期唯一 surface id。
 * @property qualifiedName macro 调用限定名；解析失败或语法缺失时为 null。
 * @property kind macro 调用形式。
 * @property hasParenthesis 调用点是否显式带有参数括号。
 * @property attrTokens attr payload token 流。
 * @property inputTokens input payload token 流。
 * @property sourceRange surface 在宿主源码中的完整范围。
 * @property scopeContext surface 所在包、类、函数上下文。
 * @property modifiers surface 携带的 modifier 文本。
 * @property carriedAnnotations surface 携带或覆盖的 annotation 文本。
 * @property capturedRawSyntax raw builder 捕获的原始语法文本。
 * @property containerContext surface 所在语法容器上下文。
 * @property replaceHandle stable splice 必须使用的替换句柄。
 */
sealed class MacroSurface {
    /** construction 期唯一 surface id。 */
    abstract val surfaceId: Long
    /** macro 调用限定名；解析失败或语法缺失时为 null。 */
    abstract val qualifiedName: FqName?
    /** macro 调用形式。 */
    abstract val kind: Kind
    /** 调用点是否显式带有参数括号。 */
    abstract val hasParenthesis: Boolean
    /** attr payload token 流。 */
    abstract val attrTokens: List<MacroSurfaceToken>
    /** input payload token 流。 */
    abstract val inputTokens: List<MacroSurfaceToken>
    /** surface 在宿主源码中的完整范围。 */
    abstract val sourceRange: MacroSurfaceSourceRange?
    /** surface 所在包、类、函数上下文。 */
    abstract val scopeContext: MacroSurfaceScopeContext
    /** surface 携带的 modifier 文本。 */
    abstract val modifiers: List<String>
    /** surface 携带或覆盖的 annotation 文本。 */
    abstract val carriedAnnotations: List<String>
    /** raw builder 捕获的原始语法文本。 */
    abstract val capturedRawSyntax: String?
    /** surface 所在语法容器上下文。 */
    abstract val containerContext: MacroSurfaceContainerContext
    /** stable splice 必须使用的替换句柄。 */
    abstract val replaceHandle: CfirReplaceHandle

    /** Macro 调用形式：`@Name(...)` 为 [PLAIN]，`@!Name(...)` 为 [FORCED]。 */
    enum class Kind {
        /** 普通 `@Name(...)` 或 `@Name` 调用形式。 */
        PLAIN,
        /** 强制 `@!Name(...)` 调用形式。 */
        FORCED,
    }
}

/**
 * Macro surface 在源码中的范围（简化版，不依赖具体 token 系统）。
 *
 * @property source 源元素；无法从当前 raw builder 映射时为 null。
 * @property startOffset surface 起始偏移。
 * @property endOffset surface 结束偏移。
 */
data class MacroSurfaceSourceRange(
    /** 源元素；无法从当前 raw builder 映射时为 null。 */
    val source: CjSourceElement?,
    /** surface 起始偏移。 */
    val startOffset: Int,
    /** surface 结束偏移。 */
    val endOffset: Int,
)

/**
 * Macro surface 携带的单个 token 信息。
 *
 * Batch 6 起承载真实 lexer 输出：
 * - [text] —— token 原始文本
 * - [startOffset]/[endOffset] —— 相对于宿主文本（attr / input payload）的偏移
 * - [kindName] —— lexer token type 的字符串名（兼容 IntelliJ `IElementType.toString()`）
 *                Batch 6 阶段是 ID-only 字符串，Batch 8 fragment parser 会替换
 *                为 token kind 枚举 + delimiter / origin 信息。
 *
 * `useParentPos` 等 builtin macro 嵌套语义在 Batch 7 forest evaluator 内处理，
 * 不在 surface token 层维护。
 *
 * @property text token 原始文本。
 * @property startOffset token 起始偏移。
 * @property endOffset token 结束偏移。
 * @property kindName lexer token type 的字符串名。
 */
data class MacroSurfaceToken(
    /** token 原始文本。 */
    val text: String,
    /** token 起始偏移。 */
    val startOffset: Int,
    /** token 结束偏移。 */
    val endOffset: Int,
    /** lexer token type 的字符串名。 */
    val kindName: String? = null,
)

/**
 * Macro surface 的"作用域 / 容器 / 包"上下文。
 *
 * @property packageFqName surface 所在文件包名。
 * @property enclosingClassFqName 最近外层类、接口、结构或枚举的 FQN。
 * @property enclosingFunctionName 最近外层函数名。
 */
data class MacroSurfaceScopeContext(
    /** surface 所在文件包名。 */
    val packageFqName: FqName,
    /** 最近外层类、接口、结构或枚举的 FQN。 */
    val enclosingClassFqName: FqName?,
    /** 最近外层函数名。 */
    val enclosingFunctionName: Name?,
)

/**
 * Macro surface 的"语法容器"上下文（baseline 第 7 节）。
 *
 * @property outerDeclarationKind 最近外层声明或语句容器类型。
 * @property isInsidePrimaryConstructor 是否位于主构造函数参数或初始化上下文中。
 * @property isInsideEnumBody 是否位于枚举体内。
 * @property isInsideBlock 是否位于代码块内。
 * @property commaListPosition 位于逗号分隔列表中时的元素下标。
 */
data class MacroSurfaceContainerContext(
    /** 最近外层声明或语句容器类型。 */
    val outerDeclarationKind: OuterDeclarationKind,
    /** 是否位于主构造函数参数或初始化上下文中。 */
    val isInsidePrimaryConstructor: Boolean,
    /** 是否位于枚举体内。 */
    val isInsideEnumBody: Boolean,
    /** 是否位于代码块内。 */
    val isInsideBlock: Boolean,
    /** 位于逗号分隔列表中时的元素下标。 */
    val commaListPosition: Int? = null,
) {
    /** surface 最近外层声明或语句容器分类。 */
    enum class OuterDeclarationKind {
        /** 没有可用外层声明上下文。 */
        NONE,
        /** 顶层声明区域。 */
        TOP_LEVEL,
        /** class body。 */
        CLASS_BODY,
        /** interface body。 */
        INTERFACE_BODY,
        /** struct body。 */
        STRUCT_BODY,
        /** enum body。 */
        ENUM_BODY,
        /** extend body。 */
        EXTEND_BODY,
        /** function body。 */
        FUNCTION_BODY,
        /** property body。 */
        PROPERTY_BODY,
    }
}

/**
 * Surface 与最终 CFIR 节点之间的稳定 splice 句柄（baseline 第 8 节）。
 *
 * 禁止退化为 source offset fallback；任何 splice 必须通过 handle 完成。
 * Batch 4a 阶段是 token 占位实现，Batch 8 重写为真正的稳定句柄。
 *
 * @property handleId construction 期稳定替换句柄 id。
 * @property carrier raw builder 放入 CFIR 的 construction-only carrier。
 * @property annotationCarrier annotation surface 的唯一 splice key。
 */
data class CfirReplaceHandle(
    /** construction 期稳定替换句柄 id。 */
    val handleId: Long,
    /** raw builder 放入 CFIR 的 construction-only carrier，splice 只能按对象身份匹配，不能退回源码 offset。 */
    val carrier: Any? = null,
    /** annotation surface 的唯一 splice key；非 annotation surface 必须为 null。 */
    val annotationCarrier: CfirAnnotationReplaceCarrier? = null,
)

/**
 * Macro surface id 单调生成器。
 *
 * surface id 全局唯一即可（不要求跨 session 稳定），用以建立
 * [MacroSurface.surfaceId] -> [MacroSurface] 的反查；
 * 同 id 复用为 [CfirReplaceHandle.handleId]。
 */
object MacroSurfaceIdGenerator {
    /** 进程内单调递增计数器。 */
    private val counter = java.util.concurrent.atomic.AtomicLong(0)

    /** 生成下一个 construction 期唯一 surface id。 */
    fun next(): Long = counter.incrementAndGet()
}

/**
 * Macro surface declaration 形态：`@MyAnnot class Foo {}` / `@MyMacro func bar() {}` 等。
 */
data class MacroSurfaceDecl(
    override val surfaceId: Long,
    override val qualifiedName: FqName?,
    override val kind: MacroSurface.Kind,
    override val hasParenthesis: Boolean,
    override val attrTokens: List<MacroSurfaceToken>,
    override val inputTokens: List<MacroSurfaceToken>,
    override val sourceRange: MacroSurfaceSourceRange?,
    override val scopeContext: MacroSurfaceScopeContext,
    override val modifiers: List<String>,
    override val carriedAnnotations: List<String>,
    override val capturedRawSyntax: String?,
    override val containerContext: MacroSurfaceContainerContext,
    override val replaceHandle: CfirReplaceHandle,
) : MacroSurface()

/** Macro surface expression 形态：`@DebugLog(...)` 出现在 expression 位置。 */
data class MacroSurfaceExpr(
    override val surfaceId: Long,
    override val qualifiedName: FqName?,
    override val kind: MacroSurface.Kind,
    override val hasParenthesis: Boolean,
    override val attrTokens: List<MacroSurfaceToken>,
    override val inputTokens: List<MacroSurfaceToken>,
    override val sourceRange: MacroSurfaceSourceRange?,
    override val scopeContext: MacroSurfaceScopeContext,
    override val modifiers: List<String>,
    override val carriedAnnotations: List<String>,
    override val capturedRawSyntax: String?,
    override val containerContext: MacroSurfaceContainerContext,
    override val replaceHandle: CfirReplaceHandle,
) : MacroSurface()

/** Macro surface parameter 形态：`func f(@MyAttr x: Int) {}` 等。 */
data class MacroSurfaceParam(
    override val surfaceId: Long,
    override val qualifiedName: FqName?,
    override val kind: MacroSurface.Kind,
    override val hasParenthesis: Boolean,
    override val attrTokens: List<MacroSurfaceToken>,
    override val inputTokens: List<MacroSurfaceToken>,
    override val sourceRange: MacroSurfaceSourceRange?,
    override val scopeContext: MacroSurfaceScopeContext,
    override val modifiers: List<String>,
    override val carriedAnnotations: List<String>,
    override val capturedRawSyntax: String?,
    override val containerContext: MacroSurfaceContainerContext,
    override val replaceHandle: CfirReplaceHandle,
) : MacroSurface()

/** 通用 surface 节点（暂未确定 decl/expr/param 时使用）。 */
data class MacroSurfaceNode(
    override val surfaceId: Long,
    override val qualifiedName: FqName?,
    override val kind: MacroSurface.Kind,
    override val hasParenthesis: Boolean,
    override val attrTokens: List<MacroSurfaceToken>,
    override val inputTokens: List<MacroSurfaceToken>,
    override val sourceRange: MacroSurfaceSourceRange?,
    override val scopeContext: MacroSurfaceScopeContext,
    override val modifiers: List<String>,
    override val carriedAnnotations: List<String>,
    override val capturedRawSyntax: String?,
    override val containerContext: MacroSurfaceContainerContext,
    override val replaceHandle: CfirReplaceHandle,
) : MacroSurface()

/**
 * Builtin non-macro surface：`@IfAvailable` 等"看起来是宏但实际不送 executor"
 * 的语义级标记（baseline 第 8 节）。
 *
 * 必须在 stable splice **之前** desugar 为最终 CFIR；
 * 不允许残留在 successful final CFIR 中。
 */
sealed class BuiltinNonMacroSurface : MacroSurface()

/** `@IfAvailable(...)` 形态的 builtin non-macro。 */
data class IfAvailableSurface(
    override val surfaceId: Long,
    override val qualifiedName: FqName?,
    override val kind: MacroSurface.Kind,
    override val hasParenthesis: Boolean,
    override val attrTokens: List<MacroSurfaceToken>,
    override val inputTokens: List<MacroSurfaceToken>,
    override val sourceRange: MacroSurfaceSourceRange?,
    override val scopeContext: MacroSurfaceScopeContext,
    override val modifiers: List<String>,
    override val carriedAnnotations: List<String>,
    override val capturedRawSyntax: String?,
    override val containerContext: MacroSurfaceContainerContext,
    override val replaceHandle: CfirReplaceHandle,
    /** 条件分支主体 token，用于 desugar 前的简单评估。 */
    val branchTokens: List<MacroSurfaceToken>,
) : BuiltinNonMacroSurface()
