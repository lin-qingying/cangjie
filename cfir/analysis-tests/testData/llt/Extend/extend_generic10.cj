// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A<T> {}

interface Foo {
    func foo(): Unit {}
}

extend<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>> A<T> <: Foo where T <: Int64 {}

func boo(x: Foo) {}

main(): Int64 {
    let a = A<Int32>()
    boo(<!TYPE_MISMATCH!>a<!>)
    a.<!NOT_MEMBER_OF!>foo<!>()
    return 0
}
