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
 */
sealed class MacroSurface {
    abstract val surfaceId: Long
    abstract val qualifiedName: FqName?
    abstract val kind: Kind
    abstract val hasParenthesis: Boolean
    abstract val attrTokens: List<MacroSurfaceToken>
    abstract val inputTokens: List<MacroSurfaceToken>
    abstract val sourceRange: MacroSurfaceSourceRange?
    abstract val scopeContext: MacroSurfaceScopeContext
    abstract val modifiers: List<String>
    abstract val carriedAnnotations: List<String>
    abstract val capturedRawSyntax: String?
    abstract val containerContext: MacroSurfaceContainerContext
    abstract val replaceHandle: CfirReplaceHandle

    /** Macro 调用形式：`@Name(...)` 为 [PLAIN]，`@!Name(...)` 为 [FORCED]。 */
    enum class Kind { PLAIN, FORCED }
}

/**
 * Macro surface 在源码中的范围（简化版，不依赖具体 token 系统）。
 */
data class MacroSurfaceSourceRange(
    val source: CjSourceElement?,
    val startOffset: Int,
    val endOffset: Int,
)

/**
 * Macro surface 携带的单个 token 信息。
 *
 * Batch 6 真实 token capture 完成前是简化结构；
 * 后续会替换为 `TokenInfo`（含 kind / value / position / delimiter）。
 */
data class MacroSurfaceToken(
    val text: String,
    val startOffset: Int,
    val endOffset: Int,
)

/**
 * Macro surface 的"作用域 / 容器 / 包"上下文。
 */
data class MacroSurfaceScopeContext(
    val packageFqName: FqName,
    val enclosingClassFqName: FqName?,
    val enclosingFunctionName: Name?,
)

/**
 * Macro surface 的"语法容器"上下文（baseline 第 7 节）。
 */
data class MacroSurfaceContainerContext(
    val outerDeclarationKind: OuterDeclarationKind,
    val isInsidePrimaryConstructor: Boolean,
    val isInsideEnumBody: Boolean,
    val isInsideBlock: Boolean,
    val commaListPosition: Int? = null,
) {
    enum class OuterDeclarationKind {
        NONE,
        TOP_LEVEL,
        CLASS_BODY,
        INTERFACE_BODY,
        STRUCT_BODY,
        ENUM_BODY,
        EXTEND_BODY,
        FUNCTION_BODY,
        PROPERTY_BODY,
    }
}

/**
 * Surface 与最终 CFIR 节点之间的稳定 splice 句柄（baseline 第 8 节）。
 *
 * 禁止退化为 source offset fallback；任何 splice 必须通过 handle 完成。
 * Batch 4a 阶段是 token 占位实现，Batch 8 重写为真正的稳定句柄。
 */
data class CfirReplaceHandle(
    val handleId: Long,
)

/**
 * Macro surface id 单调生成器。
 *
 * surface id 全局唯一即可（不要求跨 session 稳定），用以建立
 * [MacroSurface.surfaceId] -> [MacroSurface] 的反查；
 * 同 id 复用为 [CfirReplaceHandle.handleId]。
 */
object MacroSurfaceIdGenerator {
    private val counter = java.util.concurrent.atomic.AtomicLong(0)

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
