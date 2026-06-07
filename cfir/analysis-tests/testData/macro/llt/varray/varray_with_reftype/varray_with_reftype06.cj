// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


func foo1<T>(x: T): Unit {
    var s1: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>S<T><!>, $1> = [S(x)] // error
    var s2: VArray<<!VARRAY_ARG_TYPE_WITH_REFTYPE!>T<!>, $1> = [x] //error
}

struct S<T> {
    var a: T
    init(a1: T) {
        a = a1
    }
}

main(): Int64 {
    0
}
