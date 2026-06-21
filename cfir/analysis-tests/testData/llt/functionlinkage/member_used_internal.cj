


// Test for 'foo' has internal linkage
package pkg

private func foo(rawData: Array<Byte>, formatBaseNum: UInt64, multmin: UInt64, maxAbsVal: UInt64): UInt64 {
    var num: UInt64 = 0
    var lineLength = 0
    var rawDataSlice = rawData
    if (formatBaseNum != 10) {
        rawDataSlice = rawData[2..]
    }
    for (i in 0..rawDataSlice.size) {
        var digit: UInt64 = 0
        if (rawDataSlice[i] == UInt8(UInt32(r'_'))) {
            lineLength++
            continue
        }
        if (digit >= formatBaseNum || lineLength >= rawDataSlice.size) {
            throw IllegalArgumentException("The part of value convert failed.")
        }
        if (num > multmin) {
            throw IllegalArgumentException("The part of value convert failed.")
        }
        num *= formatBaseNum
        if (maxAbsVal - num < digit) {
            throw IllegalArgumentException("The part of value convert failed.")
        }
        num += digit
    }
    return num
}

public class A {}

extend A {
    public func test(data: String): Unit {
        foo([], 0, 0, 0)
    }
}
