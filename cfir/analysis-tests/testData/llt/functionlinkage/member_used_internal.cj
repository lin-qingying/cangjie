// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE and DEBUG_OPTION)EXEC-PIPE: %frontendCompiler %dump-ir-to-screen %compile_lib_opt --disable-reflection --apc=1 %f 2>&1 | compare %f
// (not CJNATIVE and DEBUG_OPTION) EXEC-PIPE: %frontendCompiler %dump-ir-to-screen %compile_lib_opt %f 2>&1 | compare %f
// (CJNATIVE) ASSERT: regex define internal i64 @.*pkg.*fooH
// (not CJNATIVE) ASSERT: regex define internal i64 @.*pkg.*fooE

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
