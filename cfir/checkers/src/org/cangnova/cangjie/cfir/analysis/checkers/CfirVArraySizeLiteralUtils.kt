package org.cangnova.cangjie.cfir.analysis.checkers

import java.math.BigInteger
import org.cangnova.cangjie.cfir.resolve.constants.CfirIntConstantEvalUtils
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.text

/** VArray 长度字面量检查共享工具，集中处理 Int64 边界和诊断 source 定位。 */
internal object CfirVArraySizeLiteralUtils {
    /** VArray 长度字面量在语义检查中使用的目标整数类型。 */
    val targetType: ConePrimitiveType = ConePrimitiveType.INT64

    /** Int64 可表示的最大非负长度值。 */
    private val maxInt64 = BigInteger.valueOf(Long.MAX_VALUE)

    /** 解析 VArray 长度字面量，并在其超出 `[0, Int64.MAX_VALUE]` 时返回解析结果。 */
    fun overflowingSizeLiteral(sizeLiteral: String): CfirIntConstantEvalUtils.ParsedIntLiteral? {
        val parsed = CfirIntConstantEvalUtils.parseIntLiteral(sizeLiteral.trim().removePrefix("$")) ?: return null
        return parsed.takeUnless { it.value >= BigInteger.ZERO && it.value <= maxInt64 }
    }

    /** 将包含 `$` 前缀的 VArray 长度 source 收窄到实际数字部分，便于诊断精确标记。 */
    fun sizeLiteralDiagnosticSource(
        source: CjSourceElement?,
        sizeLiteral: String,
    ): AbstractCjSourceElement? {
        source ?: return null
        val sourceText = source.text?.toString() ?: return source
        val literalStartInSource = sourceText.indexOf(sizeLiteral)
        if (literalStartInSource < 0) return source

        val digitOffset = if (sizeLiteral.startsWith("$")) 1 else 0
        val startOffset = source.startOffset + literalStartInSource + digitOffset
        return CjOffsetsOnlySourceElement(
            startOffset = startOffset,
            endOffset = startOffset + sizeLiteral.removePrefix("$").length,
        )
    }
}
