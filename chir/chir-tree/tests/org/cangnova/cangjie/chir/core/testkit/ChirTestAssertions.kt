package org.cangnova.cangjie.chir.core.testkit

import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.printer.ChirPrinter
import org.cangnova.cangjie.chir.core.serializer.ChirPackageCodec
import org.cangnova.cangjie.chir.core.serializer.ChirRoundTripAssert
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * CHIR 测试断言工具。
 */
object ChirTestAssertions {
    /**
     * 断言 CHIR 打印结果稳定。
     */
    fun assertPrinterStable(chirPackage: ChirPackage) {
        val first = ChirPrinter.print(chirPackage)
        val second = ChirPrinter.print(chirPackage)
        assertEquals(first, second, "printer output should be stable")
    }

    /**
     * 断言 CHIR 包可完成序列化往返且语义等价。
     */
    fun assertCodecRoundTrip(chirPackage: ChirPackage) {
        val restored = ChirPackageCodec.deserialize(ChirPackageCodec.serialize(chirPackage))
        ChirRoundTripAssert.assertSemanticallyEquivalent(chirPackage, restored)
    }
}
