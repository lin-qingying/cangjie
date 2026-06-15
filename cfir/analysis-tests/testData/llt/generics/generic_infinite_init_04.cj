// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


// X -> X, Y -> E<X>
// X -> Y, Y -> E<X>  cause infinite substitution
// X -> E<X>, Y -> E<X>  cause infinite substitution

enum E<X> {
    e(X)
}

struct A<X, Y> {
    public operator func [](b: A<X, E<X>>): Bool {
        true
    }
    public operator func +(b: <!GENERIC_INFINITE_INSTANTIATION!>A<!><Y, E<X>>): Bool {
        true
    }
}

main(): Int64 {
    var a: A<Int32, Int32>
    return 0
}
