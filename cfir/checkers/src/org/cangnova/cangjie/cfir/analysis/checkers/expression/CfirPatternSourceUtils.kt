package org.cangnova.cangjie.cfir.analysis.checkers.expression

import org.cangnova.cangjie.cfir.patterns.CfirOrPattern
import org.cangnova.cangjie.source.CjFakeSourceElementKind
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.CjSourceElementOffsetStrategy
import org.cangnova.cangjie.source.fakeElement

/** 完整 OR 模式范围，不包含 case/guard/分支体；种类和可达性诊断共用。 */
internal fun CfirOrPattern.patternRangeSource(): CjSourceElement? {
    val first = alternatives.firstOrNull()?.source ?: return null
    val last = alternatives.lastOrNull()?.source ?: return null
    if (first.startOffset >= last.endOffset) return first
    return first.fakeElement(
        CjFakeSourceElementKind.SyntheticCall,
        CjSourceElementOffsetStrategy.Custom.Initialized(first.startOffset, last.endOffset),
    )
}
