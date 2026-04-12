// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// ASSERT: scan mismatch 'inout' of function argument with type 'VArray'
// ASSERT: scan mismatched types

foreign func c_f1(a: CPointer<Int32>): Unit

foreign func c_f2(a: VArray<Int32, $1>): Unit

foreign func c_f3(a: CPointer<VArray<Int32, $1>>): Unit

foreign func c_f4(a: VArray<VArray<Int32, $1>, $1>): Unit

class A {}

main() {
    unsafe {
        var a = VArray<Int32, $1>({i => Int32(i)})
        c_f2(a) // error
        c_f3(inout a) // error

        var a1 = VArray<Int32, $2>({i => Int32(i)})
        c_f2(inout a1) // error

        var b = VArray<VArray<Int32, $1>, $1>(repeat: [1])
        c_f1(inout b) // error
        c_f2(inout b) // error

        var b1 = VArray<VArray<Int32, $2>, $1>(repeat: [1, 2])
        c_f3(inout b1) // error
        c_f4(inout b1) // error

        var b2 = VArray<VArray<Int32, $1>, $2>(repeat: [1])
        c_f4(inout b2) // error
    }
    0
}
