// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct B {
    public var a: Int32 = 1
    public let b: Int32 = 1
}

func m() {
    return B()
}

func k() {
    return A()
}

main(): Int64 {
    k().a = 2
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>k().b<!> = 2
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>m().a<!> = 2
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>m().b<!> = 2

    return 0
}

class A {
    public var a: Int32 = 1
    public let b: Int32 = 1
}
