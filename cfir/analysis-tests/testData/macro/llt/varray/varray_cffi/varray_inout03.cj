// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


var c_f1: CFunc<(CPointer<Int32>) -> Unit> = {
    a =>
}
var c_f2: CFunc<(VArray<Int32, $1>) -> Unit> = {
    a =>
}
var c_f3: CFunc<(CPointer<VArray<Int32, $1>>) -> Unit> = {
    a =>
}
var c_f4: CFunc<(VArray<VArray<Int32, $1>, $1>) -> Unit> = {
    a =>
}

class A {}

main() {
    unsafe {
        var a = VArray<Int32, $1>({i => Int32(i)})
        c_f2(<!INOUT_MISMATCH!>a<!>) // error
        c_f3(inout <!TYPE_MISMATCH!>a<!>) // error

        var a1 = VArray<Int32, $2>({i => Int32(i)})
        c_f2(inout <!TYPE_MISMATCH!>a1<!>) // error

        var b = VArray<VArray<Int32, $1>, $1>(repeat: [1])
        c_f1(inout <!TYPE_MISMATCH!>b<!>) // error
        c_f2(inout <!TYPE_MISMATCH!>b<!>) // error

        var b1 = VArray<VArray<Int32, $2>, $1>(repeat: [1, 2])
        c_f3(inout <!TYPE_MISMATCH!>b1<!>) // error
        c_f4(inout <!TYPE_MISMATCH!>b1<!>) // error

        var b2 = VArray<VArray<Int32, $1>, $2>(repeat: [1])
        c_f4(inout <!TYPE_MISMATCH!>b2<!>) // error
    }
    0
}
