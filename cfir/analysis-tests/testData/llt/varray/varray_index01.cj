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
    <!sema_builtin_index_in_bound!>a<!>[3] == 0
    <!sema_builtin_index_in_bound!>a<!>[3] = 1
    <!sema_builtin_index_in_bound!>a<!>[-1] == 0
    <!sema_builtin_index_in_bound!>a<!>[-1] = 1

    let x1: Int64 = a[0] // ok
    let x2: I = a[0] // ok
    let x3: Int32 = <!sema_mismatched_types!>a[0]<!> // error
    a[0] = x1 // ok
    a[0] = <!sema_mismatched_types!>x2<!> // error
    a[0] = <!sema_mismatched_types!>x3<!> // error

    <!sema_varray_subscript_num!>a[1, 2]<!> // error
    a[<!sema_mismatched_types!>"1"<!>] // error

    a[1 + 2 * 2] // error
    0
}
