package org.cangnova.cangjie.chir.core.testkit

import org.cangnova.cangjie.chir.core.model.ChirPackage
import org.cangnova.cangjie.chir.core.printer.ChirPrinter
import org.cangnova.cangjie.chir.core.serializer.ChirPackageCodec
import org.cangnova.cangjie.chir.core.serializer.ChirRoundTripAssert
import org.junit.jupiter.api.Assertions.assertEquals

object ChirTestAssertions {
    fun assertPrinterStable(chirPackage: ChirPackage) {
        val first = ChirPrinter.print(chirPackage)
        val second = ChirPrinter.print(chirPackage)
        assertEquals(first, second, "printer output should be stable")
    }

    fun assertCodecRoundTrip(chirPackage: ChirPackage) {
        val restored = ChirPackageCodec.deserialize(ChirPackageCodec.serialize(chirPackage))
        ChirRoundTripAssert.assertSemanticallyEquivalent(chirPackage, restored)
    }
}
