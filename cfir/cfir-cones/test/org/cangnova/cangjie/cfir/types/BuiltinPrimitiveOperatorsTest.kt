package org.cangnova.cangjie.cfir.types

import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.name.OperatorNameConventions
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * 验证 primitive 内建运算符解析表的一元运算规则。
 */
class BuiltinPrimitiveOperatorsTest {

    /**
     * 全部具备内建候选的一元运算符名称。
     */
    private val unaryNames = listOf(
        OperatorNameConventions.NOT,
        OperatorNameConventions.UNARY_MINUS,
        OperatorNameConventions.UNARY_PLUS,
        OperatorNameConventions.INC,
        OperatorNameConventions.DEC,
    )

    /**
     * 验证 `Nothing` 操作数命中任一内建一元运算符，且结果类型仍为 `Nothing`。
     *
     * 对齐官方 `SynBuiltinUnaryExpr`：候选按 `IsSubtype(操作数, 候选 primitive)` 匹配，
     * `Nothing` 是任意类型的子类型，命中后 `ue.ty = ue.expr->ty`。
     */
    @Test
    fun `unary operators accept Nothing operand and keep Nothing result`() {
        for (name in unaryNames) {
            val match = BuiltinPrimitiveOperators.resolve(name, ConePrimitiveType.NOTHING, emptyList())
            assertNotNull(match, "$name should resolve on Nothing")
            assertEquals(PrimitiveTypeKind.NOTHING, match!!.signature.returnKind, "$name return kind")
            assertEquals(PrimitiveTypeKind.NOTHING, match.returnType.kind, "$name return type")
        }
    }

    /**
     * 验证不属于一元运算符的名称即便接收者是 `Nothing` 也不会凭空匹配。
     */
    @Test
    fun `Nothing does not resolve names without builtin unary candidates`() {
        val nonUnaryNames = listOf(
            OperatorNameConventions.PLUS,
            OperatorNameConventions.EQUALS,
            Name.identifier("*operator_unknown"),
        )
        for (name in nonUnaryNames) {
            assertNull(
                BuiltinPrimitiveOperators.resolve(name, ConePrimitiveType.NOTHING, emptyList()),
                "$name should not resolve as unary on Nothing",
            )
        }
    }

    /**
     * 验证 `Nothing` 不会因一元规则而对外暴露 primitive 成员签名。
     */
    @Test
    fun `Nothing exposes no primitive member signatures`() {
        assertTrue(BuiltinPrimitiveOperators.signaturesFor(PrimitiveTypeKind.NOTHING).isEmpty())
    }

    /**
     * 验证具体 primitive 接收者的一元运算结果类型不受 `Nothing` 规则影响。
     */
    @Test
    fun `unary operators on concrete primitives keep receiver kind`() {
        val boolNot = BuiltinPrimitiveOperators.resolve(
            OperatorNameConventions.NOT,
            ConePrimitiveType(PrimitiveTypeKind.BOOLEAN),
            emptyList(),
        )
        assertNotNull(boolNot)
        assertEquals(PrimitiveTypeKind.BOOLEAN, boolNot!!.signature.returnKind)

        val intNegate = BuiltinPrimitiveOperators.resolve(
            OperatorNameConventions.UNARY_MINUS,
            ConePrimitiveType(PrimitiveTypeKind.INT64),
            emptyList(),
        )
        assertNotNull(intNegate)
        assertEquals(PrimitiveTypeKind.INT64, intNegate!!.signature.returnKind)

        assertNull(
            BuiltinPrimitiveOperators.resolve(
                OperatorNameConventions.UNARY_MINUS,
                ConePrimitiveType(PrimitiveTypeKind.BOOLEAN),
                emptyList(),
            ),
            "unary minus should not resolve on Bool",
        )
    }
}
