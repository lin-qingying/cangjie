// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct B {
    public var a: Int32 = 1
    public let b: Int32 = 1
}

main(): Int64 {
    A().a++
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>A().b++<!>

    <!CANNOT_ASSIGN_TO_IMMUTABLE!>B().a++<!>
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>B().b++<!>

    return 0
}

class A {
    public var a: Int32 = 1
    public let b: Int32 = 1
}
