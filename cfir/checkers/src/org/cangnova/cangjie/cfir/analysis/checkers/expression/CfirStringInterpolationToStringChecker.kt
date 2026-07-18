package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralExpression
import org.cangnova.cangjie.cfir.expressions.CfirStringInterpolation
import org.cangnova.cangjie.cfir.resolve.fullyExpandedType
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.constructType
import org.cangnova.cangjie.cfir.types.ConeErrorType
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.cfir.types.ConeTypeVariableType
import org.cangnova.cangjie.cfir.types.StdlibClassIds
import org.cangnova.cangjie.cfir.types.typeContext
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.text
import org.cangnova.cangjie.type.AbstractTypeChecker

/**
 * 检查字符串插值块的最终类型是否满足 `core.ToString` 契约。
 *
 * resolve 阶段负责把该契约写入推断约束；这里仅消费已经解析完成的 part 类型并报告
 * 确定的不兼容结果，避免诊断逻辑反向介入 PCLA 和调用完成过程。
 */
object CfirStringInterpolationToStringChecker : CfirStringInterpolationChecker() {
    /** 对每个非字面量插值块执行最终合规检查。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(expression: CfirStringInterpolation) {
        val toStringSymbol = context.session.symbolProvider.getClassLikeSymbolByClassId(StdlibClassIds.ToString)
            ?: return
        val toStringType = toStringSymbol.constructType()

        for (part in expression.parts) {
            if (part is CfirLiteralExpression) continue

            val partType = part.coneTypeOrNull?.fullyExpandedType(context.session) ?: continue
            if (partType is ConeErrorType || partType is ConeTypeVariableType || partType is ConePrimitiveType) continue
            if (AbstractTypeChecker.isSubtypeOf(context.session.typeContext, partType, toStringType) == true) continue

            val source = expression.interpolationMarkerSource(part) ?: continue
            reporter.reportOn(source, CfirErrors.INVALID_STRING_IMPLEMENTATION)
        }
    }
}

/**
 * 返回插值标记的统一源码范围。
 *
 * 长插值 `${expr}` 标记左花括号，短插值 `$name` 标记美元符号；实现只依赖 CFIR source
 * 的文件偏移与文本，因此 PSI 和 LightTree 两条 raw-builder 路径共享完全相同的定位逻辑。
 */
private fun CfirStringInterpolation.interpolationMarkerSource(part: CfirExpression): AbstractCjSourceElement? {
    val interpolationSource = source as? CjSourceElement ?: return null
    val partSource = part.source as? AbstractCjSourceElement ?: return null
    val interpolationText = interpolationSource.text?.toString() ?: return null
    val relativePartStart = (partSource.startOffset - interpolationSource.startOffset)
        .coerceIn(0, interpolationText.length)
    val dollarOffset = interpolationText.lastIndexOf('$', startIndex = (relativePartStart - 1).coerceAtLeast(0))
    if (dollarOffset < 0) return null

    val markerOffset = if (interpolationText.getOrNull(dollarOffset + 1) == '{') {
        dollarOffset + 1
    } else {
        dollarOffset
    }
    val absoluteMarkerOffset = interpolationSource.startOffset + markerOffset
    return CjOffsetsOnlySourceElement(absoluteMarkerOffset, absoluteMarkerOffset + 1)
}
