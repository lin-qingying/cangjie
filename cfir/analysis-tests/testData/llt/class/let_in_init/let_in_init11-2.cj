// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    return 0
}

class B <: A {
    public init() {
        a = 0
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = 0
    }

    public func foo1() {
        a = 0
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>b<!> = 0
    }

    public func foo2() {
        this.a = 0
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>this.b<!> = 0
    }
}

open class A {
    public var a: Int32 = 1
    public let b: Int32 = 1
}
