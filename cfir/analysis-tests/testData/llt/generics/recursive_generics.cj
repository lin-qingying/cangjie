// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class A<<!GENERIC_PARAM_DIRECTLY_RECURSIVE!>K<!>, U> where U <: K, K <: U {
    func foo(x: Box<U>): Box<K> {
        x as Box<K> ?? throw Exception()
    }
}

func foo<<!GENERIC_PARAM_DIRECTLY_RECURSIVE!>K<!>, U>(x: Box<U>): Box<K> where U <: K, K <: U {
    x as Box<K> ?? throw Exception()
}

class B<<!GENERIC_PARAM_DIRECTLY_RECURSIVE!>K<!>, U, X, Y, Z> where K <: U, U <: X, X <: Y, Y <: Z, Z <: U {
    func foo(x: Box<Z>): Box<K> {
        x as Box<K> ?? throw Exception()
    }
}

main(): Int64 {
    0
}
