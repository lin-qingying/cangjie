// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I {}

extend Int64 <: I {}

main() {
    var a = VArray<Int64, $3>(repeat: 0)
    var x: Int32 = 10
    // error
    <!BUILTIN_INDEX_IN_BOUND!>a<!>[3] == 0
    <!BUILTIN_INDEX_IN_BOUND!>a<!>[3] = 1
    <!BUILTIN_INDEX_IN_BOUND!>a<!>[-1] == 0
    <!BUILTIN_INDEX_IN_BOUND!>a<!>[-1] = 1

    let x1: Int64 = a[0] // ok
    let x2: I = a[0] // ok
    let x3: Int32 = <!TYPE_MISMATCH!>a[0]<!> // error
    a[0] = x1 // ok
    a[0] = <!ASSIGNMENT_TYPE_MISMATCH!>x2<!> // error
    a[0] = <!ASSIGNMENT_TYPE_MISMATCH!>x3<!> // error

    <!VARRAY_SUBSCRIPT_NUM!>a[1, 2]<!> // error
    a[<!TYPE_MISMATCH!>"1"<!>] // error

    a[1 + 2 * 2] // error
    0
}
