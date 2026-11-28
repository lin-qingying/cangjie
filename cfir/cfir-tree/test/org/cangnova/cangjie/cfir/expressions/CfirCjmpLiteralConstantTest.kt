package org.cangnova.cangjie.cfir.expressions

import org.cangnova.cangjie.cfir.CfirStringLiteralValueNormalizer
import org.cangnova.cangjie.cfir.expressions.builder.buildFunctionCall
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.references.builder.buildNamedReference
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.cangnova.cangjie.source.CjBinarySourceElement
import org.cangnova.cangjie.source.CjSourceLocation
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class CfirCjmpLiteralConstantTest {
    @Test
    fun literalProjectionExposesCjoComparableValues() {
        val boolean = buildLiteralExpression {
            kind = CfirLiteralKind.BOOLEAN
            value = true
        }
        val byte = buildLiteralExpression {
            kind = CfirLiteralKind.BYTE
            value = 97
        }
        val unit = buildLiteralExpression {
            kind = CfirLiteralKind.UNIT
            value = null
        }

        assertEquals(CfirCjmpLiteralConstant(CfirLiteralKind.BOOLEAN, "true"), boolean.cjmpAnnotationLiteralConstant())
        assertEquals(CfirCjmpLiteralConstant(CfirLiteralKind.BYTE, "97"), byte.cjmpAnnotationLiteralConstant())
        assertEquals(CfirCjmpLiteralConstant(CfirLiteralKind.UNIT, null), unit.cjmpAnnotationLiteralConstant())
    }

    @Test
    fun stringEscapeSpellingsNormalizeToTheSameAnnotationValue() {
        assertEquals(
            "a",
            CfirStringLiteralValueNormalizer.normalizeSourceLiteral("\"\\u{61}\""),
        )
        assertEquals(
            "$",
            CfirStringLiteralValueNormalizer.normalizeSourceLiteral("\"\\$\""),
        )
    }

    @Test
    fun runeNormalizationComparesUnicodeScalarValues() {
        assertEquals("a", CfirStringLiteralValueNormalizer.normalizeRuneSourceLiteral("r'a'"))
        assertEquals("a", CfirStringLiteralValueNormalizer.normalizeRuneSourceLiteral("r'\\u{61}'"))
        assertEquals("🍑", CfirStringLiteralValueNormalizer.normalizeRuneSourceLiteral("r'\\u{1F351}'"))
        assertEquals("$", CfirStringLiteralValueNormalizer.normalizeRuneSourceLiteral("r'\\$'"))
        assertNull(CfirStringLiteralValueNormalizer.normalizeRuneSourceLiteral("r'ab'"))
        assertNull(CfirStringLiteralValueNormalizer.normalizeRuneSourceLiteral("r'\\u{D800}'"))
        assertNull(CfirStringLiteralValueNormalizer.normalizeRuneSourceLiteral("'a'"))
    }

    @Test
    fun binaryStringProjectionRetainsCanonicalValuesAndSourceLocations() {
        // 二进制值已经解码；其中的引号和反斜杠是值本身，不能再次当作源码字面量解析。
        val canonicalValues = listOf("$", "\\n", "\\u{61}", "\"quoted\"", "\"$\"", "\n\t\\'$")
        for (kind in listOf(CfirLiteralKind.STRING, CfirLiteralKind.JSTRING)) {
            for ((index, canonicalValue) in canonicalValues.withIndex()) {
                assertBinaryLiteralProjection(kind, canonicalValue, line = 8 + index)
            }
        }
    }

    @Test
    fun binaryRuneProjectionRetainsDecodedScalarsAndSourceLocations() {
        // 转义 Rune 已是单个 Unicode 标量；反斜杠、引号、换行及美元符号不再进入语法解码。
        val canonicalValues = listOf("a", "$", "\n", "\t", "\r", "\\", "'", "🍑")
        for ((index, canonicalValue) in canonicalValues.withIndex()) {
            assertBinaryLiteralProjection(CfirLiteralKind.RUNE, canonicalValue, line = 16 + index)
        }
    }

    @Test
    fun binaryByteProjectionRetainsWireSpellingsAndSourceLocations() {
        // RuneByte 的 canonical 值保留 wire spelling，不能替换成 source 调试文本或数值码点。
        val wireSpellings = listOf("b'a'", "b'$'", "b'\\n'", "b'\\\\'", "b'\\''")
        for ((index, wireSpelling) in wireSpellings.withIndex()) {
            assertBinaryLiteralProjection(CfirLiteralKind.BYTE, wireSpelling, line = 24 + index)
        }
    }

    /** 使用正式反序列化器的占位文本与真实位置，检查比较投影既不重解码值也不移除 source。 */
    private fun assertBinaryLiteralProjection(kind: CfirLiteralKind, canonicalValue: String, line: Int) {
        val location = CjSourceLocation("D:/fixtures/common.cj", line, 17)
        val binarySource = object : CjBinarySourceElement(
            debugText = "CJO expression or pattern",
            binaryFilePath = location.filePath,
            stableIdentity = Any(),
            sourceLocation = location,
        ) {}
        val literal = buildLiteralExpression {
            source = binarySource
            this.kind = kind
            value = canonicalValue
        }

        assertSame(binarySource, literal.source)
        assertSame(location, binarySource.sourceLocation)
        assertEquals(
            CfirCjmpLiteralConstant(kind, canonicalValue),
            literal.cjmpAnnotationLiteralConstant(),
            "binary $kind at $location uses its canonical value verbatim",
        )
        assertSame(binarySource, literal.source, "annotation projection retains the binary diagnostic source")
        assertSame(location, binarySource.sourceLocation, "annotation projection retains the original location object")
        assertSame(canonicalValue, literal.value, "annotation projection does not decode or replace the canonical value")
    }

    @Test
    fun unaryMinusProjectionOnlyFoldsIntegerAndFloatLiterals() {
        fun unaryMinus(kind: CfirLiteralKind, value: Any) = buildFunctionCall {
            calleeReference = buildNamedReference { name = Name.identifier(OperatorNameConventions.UNARY_MINUS.asString()) }
            explicitReceiver = buildLiteralExpression {
                this.kind = kind
                this.value = value
            }
            origin = CfirFunctionCallOrigin.Operator
        }

        assertEquals(
            CfirCjmpLiteralConstant(CfirLiteralKind.INT, "-1"),
            unaryMinus(CfirLiteralKind.INT, 1).cjmpAnnotationLiteralConstant(),
        )
        assertEquals(
            CfirCjmpLiteralConstant(CfirLiteralKind.FLOAT, "-1.5"),
            unaryMinus(CfirLiteralKind.FLOAT, 1.5).cjmpAnnotationLiteralConstant(),
        )
        assertNull(
            unaryMinus(CfirLiteralKind.BYTE, 97).cjmpAnnotationLiteralConstant(),
            "the official parser keeps unary minus on a Byte literal as UnaryExpr",
        )
        val byteOperand = requireNotNull(unaryMinus(CfirLiteralKind.BYTE, 97).cjmpUnaryMinusOperand())
        assertEquals(
            CfirLiteralKind.BYTE,
            (byteOperand as CfirLiteralExpression).kind,
            "the serializer still receives the operand needed to preserve the UnaryExpr structure",
        )
    }
}
