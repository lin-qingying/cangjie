package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.CjSourceElement

/** annotation 实参视图中的绑定状态。 */
public enum class CfirAnnotationArgumentStatus {
    /** 实参尚未经过参数绑定。 */
    UNRESOLVED,

    /** 实参已成功绑定到形参。 */
    RESOLVED,

    /** 实参没有可用的目标形参。 */
    UNMAPPED,

    /** 实参重复绑定了同一形参。 */
    DUPLICATE,

    /** 参数表达式或绑定过程产生错误。 */
    ERROR,
    /** 必需参数没有显式实参或默认值。 */
    MISSING,
}

/**
 * 一个 annotation 实参的只读语义投影视图。
 *
 * [CfirResolvedArgumentList.mapping] 仍是唯一绑定事实源；本视图只保存
 * source order、命名信息、状态和 source range，禁止复制另一套 mapping。
 */
public data class CfirAnnotationArgumentViewEntry(
    /** 实参在源码中的顺序。 */
    val sourceOrder: Int,
    /** 显式命名实参的名称；位置实参为空。 */
    val explicitName: Name?,
    /** 原始实参表达式。 */
    val argument: CfirExpression?,
    /** 唯一绑定到的形参；未映射/重复时为空。 */
    val resolvedParameter: CfirValueParameter?,
    /** 绑定结果状态。 */
    val status: CfirAnnotationArgumentStatus,
    /** 是否由形参默认值产生，而非源码显式实参。 */
    val isDefaultOrigin: Boolean,
    /** 常量求值结果；尚未求值或非字面量时为空。 */
    val constantExpression: CfirExpression?,
    /** 实参的完整 source range。 */
    val source: CjSourceElement?,
)

/**
 * annotation 实参的不可变视图。
 *
 * 该类型不实现或缓存参数绑定；调用者必须从当前 resolved argument list
 * 即时构造，因而不会产生第二个可变事实源。
 */
public data class CfirAnnotationArgumentView(
    /** 按 source order（默认值排在显式实参之后）排列的条目。 */
    val entries: List<CfirAnnotationArgumentViewEntry>,
) {
    public companion object {
        /** 没有可见实参时的稳定空视图。 */
        public val EMPTY: CfirAnnotationArgumentView = CfirAnnotationArgumentView(emptyList())
    }
}

/**
 * 从现有 mapping 构造 annotation 参数视图。
 *
 * [defaultParameters] 由 annotation semantic owner 提供；不传入时只投影
 * 显式实参。这样 view 不会自行查找 declaration 或重新解析名字。
 */
public fun CfirResolvedArgumentList.toAnnotationArgumentView(
    defaultParameters: List<CfirValueParameter> = emptyList(),
): CfirAnnotationArgumentView {
    val explicitArguments = originalArgumentList?.arguments ?: arguments
    val mappedArguments = mappingIncludingContextArguments
    val seenParameters = linkedSetOf<CfirValueParameter>()
    val entries = explicitArguments.mapIndexed { index, argument ->
        val mapped = mappedArguments[argument]
            ?: (argument as? CfirNamedArgumentExpression)?.let { mappedArguments[it.expression] }
        val explicitName = (argument as? CfirNamedArgumentExpression)?.argumentName
        val duplicate = mapped != null && !seenParameters.add(mapped)
        CfirAnnotationArgumentViewEntry(
            sourceOrder = index,
            explicitName = explicitName,
            argument = argument,
            resolvedParameter = mapped.takeUnless { duplicate },
            status = when {
                duplicate -> CfirAnnotationArgumentStatus.DUPLICATE
                mapped == null -> CfirAnnotationArgumentStatus.UNMAPPED
                argument.coneTypeOrNull?.isError == true -> CfirAnnotationArgumentStatus.ERROR
                else -> CfirAnnotationArgumentStatus.RESOLVED
            },
            isDefaultOrigin = false,
            constantExpression = ((argument as? CfirNamedArgumentExpression)?.expression ?: argument) as? CfirLiteralExpression,
            source = argument.source,
        )
    }.toMutableList()

    defaultParameters
        .asSequence()
        .filter { it !in seenParameters && it.defaultValue != null }
        .forEachIndexed { offset, parameter ->
            entries += CfirAnnotationArgumentViewEntry(
                sourceOrder = explicitArguments.size + offset,
                explicitName = parameter.name,
                argument = null,
                resolvedParameter = parameter,
                status = CfirAnnotationArgumentStatus.RESOLVED,
                isDefaultOrigin = true,
                constantExpression = parameter.defaultValue,
                source = parameter.source,
            )
        }

    return CfirAnnotationArgumentView(entries)
}
