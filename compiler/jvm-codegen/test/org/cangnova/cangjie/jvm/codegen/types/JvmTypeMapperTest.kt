package org.cangnova.cangjie.jvm.codegen.types

import org.cangnova.cangjie.chir.core.type.ChirPrimitiveType
import org.cangnova.cangjie.chir.core.type.ChirRawArrayType
import org.cangnova.cangjie.chir.core.type.ChirResolvedTypeRef
import org.cangnova.cangjie.jvm.codegen.diagnostics.JvmCodegenException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

/**
 * 验证 CHIR 数组的官方无符号维数映射为合法 JVM descriptor。
 */
class JvmTypeMapperTest {
    private val mapper = JvmTypeMapper()

    /**
     * JVM descriptor 接受的最大维数应保留为精确的数组类型。
     */
    @Test
    fun `maps maximum JVM array dimensions`() {
        val type = ChirResolvedTypeRef(
            ChirRawArrayType(
                elementType = ChirResolvedTypeRef(ChirPrimitiveType.INT32),
                dimensions = 255u,
            ),
        )

        assertEquals("[".repeat(255) + "I", mapper.mapValueType(type).descriptor)
    }

    /**
     * 超过 JVM classfile 数组维数上限时必须报告后端错误，不能窄化或生成非法 descriptor。
     */
    @Test
    fun `rejects raw array dimensions beyond JVM descriptor limit`() {
        val type = ChirResolvedTypeRef(
            ChirRawArrayType(
                elementType = ChirResolvedTypeRef(
                    ChirRawArrayType(
                        elementType = ChirResolvedTypeRef(ChirPrimitiveType.INT32),
                        dimensions = 2u,
                    ),
                ),
                dimensions = 254u,
            ),
        )

        assertThrows(JvmCodegenException::class.java) { mapper.mapValueType(type) }
    }

    /**
     * 官方 uint32 最大值不能在 JVM 映射期间溢出为负数或变成较小维数。
     */
    @Test
    fun `rejects unsigned dimensions without narrowing`() {
        val type = ChirResolvedTypeRef(
            ChirRawArrayType(
                elementType = ChirResolvedTypeRef(ChirPrimitiveType.INT32),
                dimensions = UInt.MAX_VALUE,
            ),
        )

        assertThrows(JvmCodegenException::class.java) { mapper.mapValueType(type) }
    }
}
