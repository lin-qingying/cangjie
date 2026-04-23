// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct S {
    let a: VArray<VArray<Int64, $3>, $3> = VArray<VArray<Int64, $3>, $3>(repeat: [0, 1, 2])
    public prop b: VArray<Int64, $3> {
        get() {
            return [1, 2, 3]
        }
    }
}

func f(a: VArray<Int64, $3>) {
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>a[1]<!> = 2 // error
}

func f1(a: VArray<VArray<Int64, $3>, $3>) {
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>a[1][2]<!> = 2 // error
}

func f3(a: S) {
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>a.a[1][2]<!> = 2 //error
}

main() {
    var a = VArray<Int64, $3>(repeat: 0)
    a[1] = 2 // ok
    let b = VArray<Int64, $3>(repeat: 0)
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>b[1]<!> = 2 // error

    var arr = VArray<VArray<Int64, $3>, $2>(repeat: [1, 2, 3])
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>arr[1][2]<!> = 1 // error

    var varr = Array<VArray<Int64, $3>>(2, repeat: [1, 2, 3])
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>varr[1][2]<!> = 1 // error

    var s = S()
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>s.b[1]<!> = 1 // error
    0
}
