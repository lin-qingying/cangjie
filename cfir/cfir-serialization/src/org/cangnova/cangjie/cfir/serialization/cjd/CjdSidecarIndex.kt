package org.cangnova.cangjie.cfir.serialization.cjd

import java.nio.file.Path

/** UTF-16 偏移，右端开区间，直接对应 PSI/LightTree 和原始文本。 */
data class CjdSourceRange(val startOffset: Int, val endOffset: Int)

/**
 * 原样注解，无求值或宏展开。rawText 是无损真相源（含 @!、方括号和字符串引号）。
 * 参数保留顺序、重复名称和未知表达式；特殊内置注解的非标准参数保存在 argumentText 中。
 */
data class CjdAnnotation(
    val name: String,
    val forcedCustom: Boolean,
    val rawText: String,
    val range: CjdSourceRange,
    val argumentText: String?,
    val arguments: List<CjdAnnotationArgument>,
)

/** 单个注解实参的语法快照：可选命名、表达式文本与解析出的表达式树。 */
data class CjdAnnotationArgument(
    val name: String?,
    val expressionText: String,
    val rawText: String,
    val range: CjdSourceRange,
    val expression: CjdAnnotationExpression = CjdAnnotationExpression("UNKNOWN", expressionText, range),
)

/** parser 确定的表达式树快照；保留 token 及子表达式，不进行求值或名字解析。 */
data class CjdAnnotationExpression(
    val syntaxKind: String,
    val rawText: String,
    val range: CjdSourceRange,
    val children: List<CjdAnnotationExpression> = emptyList(),
)

/** 已展开分组形式的导入路径；组织名前缀单独保留，不混入包名。 */
data class CjdAnnotationImport(
    val fqName: String,
    val isAllUnder: Boolean = false,
    val alias: String? = null,
    val organizationName: String? = null,
)

/** 同一 sidecar 的词法上下文；声明名用于拒绝隐式系统注解的同名遮蔽。 */
data class CjdAnnotationContext(
    val packageFqName: String = "",
    val organizationName: String? = null,
    val imports: List<CjdAnnotationImport> = emptyList(),
    val declaredNames: Set<String> = emptySet(),
)

/** 声明和参数的注解均按源码顺序保留；不包含访问器、类型参数或局部声明。 */
data class CjdDeclarationEntry(
    val key: DeclarationMatchKey,
    val annotations: List<CjdAnnotation>,
    val members: List<CjdDeclarationEntry>,
    val parameterAnnotations: List<List<CjdAnnotation>>,
    val range: CjdSourceRange,
)

/** sidecar 解析诊断分类；SYNTAX / MACRO_NOT_EXPANDED 等阻断类会令快照不可用。 */
enum class CjdDiagnosticKind { SYNTAX, UNSUPPORTED_DECLARATION, UNSUPPORTED_TYPE, MACRO_NOT_EXPANDED }

/** 单条 sidecar 解析诊断，定位信息使用 UTF-16 偏移区间。 */
data class CjdDiagnostic(val kind: CjdDiagnosticKind, val range: CjdSourceRange, val message: String)

/**
 * 单文件快照，不持有 PSI/LightTree，不注册任何声明。含语法错误时仅供诊断，不能合并。
 * sourceId 支持非物理 PSI；磁盘入口的 cjdPath 非空。快照失效由拥有者负责。
 */
interface CjdSidecarIndex {
    val sourceId: String
    val cjdPath: Path?
    val annotationContext: CjdAnnotationContext
    val declarations: List<CjdDeclarationEntry>
    val topLevel: Map<DeclarationMatchKey, List<CjdDeclarationEntry>>
    val diagnostics: List<CjdDiagnostic>
    val isUsable: Boolean get() = diagnostics.none { it.kind == CjdDiagnosticKind.SYNTAX || it.kind == CjdDiagnosticKind.UNSUPPORTED_DECLARATION || it.kind == CjdDiagnosticKind.MACRO_NOT_EXPANDED }

    /** 有向匹配，按源码次序返回所有候选；错误快照不返回匹配。 */
    fun findMatches(target: DeclarationMatchKey): List<CjdDeclarationEntry>
}

/** [CjdSidecarIndex] 的默认实现；按 kind+identifier 分桶加速有向匹配查询。 */
internal class CjdSidecarIndexImpl(
    override val sourceId: String,
    override val cjdPath: Path?,
    override val declarations: List<CjdDeclarationEntry>,
    override val diagnostics: List<CjdDiagnostic>,
    override val annotationContext: CjdAnnotationContext = CjdAnnotationContext(),
) : CjdSidecarIndex {
    override val topLevel: Map<DeclarationMatchKey, List<CjdDeclarationEntry>> = declarations.groupBy { it.key }
    private val buckets = declarations.groupBy { it.key.kind to it.key.identifier }
    override fun findMatches(target: DeclarationMatchKey): List<CjdDeclarationEntry> =
        if (!isUsable) emptyList() else buckets[target.kind to target.identifier].orEmpty().filter { it.key.matches(target) }
}
