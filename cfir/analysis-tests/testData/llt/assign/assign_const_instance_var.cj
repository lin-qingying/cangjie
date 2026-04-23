// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct A {
    public var a: Int32 = 1
    public let b: Int32 = 1
}

main(): Int64 {
    var x = A()
    x.a = 2
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>x.b<!> = 2

    let y = A()
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>y.a<!> = 2
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>y.b<!> = 2

    var m = B()
    m.a = 2
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>m.b<!> = 2

    let n = B()
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>n.a<!> = 2
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>n.b<!> = 2

    return 0
}

struct B {
    public var a: Int32 = 1
    public let b: Int32 = 1
}
