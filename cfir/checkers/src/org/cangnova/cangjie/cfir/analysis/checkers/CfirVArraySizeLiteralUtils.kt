package org.cangnova.cangjie.cfir.analysis.checkers

import java.math.BigInteger
import org.cangnova.cangjie.cfir.resolve.constants.CfirIntConstantEvalUtils
import org.cangnova.cangjie.cfir.types.ConePrimitiveType
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.text

internal object CfirVArraySizeLiteralUtils {
    val targetType: ConePrimitiveType = ConePrimitiveType.INT64

    private val maxInt64 = BigInteger.valueOf(Long.MAX_VALUE)

    fun overflowingSizeLiteral(sizeLiteral: String): CfirIntConstantEvalUtils.ParsedIntLiteral? {
        val parsed = CfirIntConstantEvalUtils.parseIntLiteral(sizeLiteral.trim().removePrefix("$")) ?: return null
        return parsed.takeUnless { it.value >= BigInteger.ZERO && it.value <= maxInt64 }
    }

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
