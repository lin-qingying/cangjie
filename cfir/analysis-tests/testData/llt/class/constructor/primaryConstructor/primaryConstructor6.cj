// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class Parent {
    public Parent(c!: Int64 = 1, var a!: Int32 = 1) {}
}

class children <: Parent {
    public children(c!: Int32 = <!ILLEGAL_THIS_OR_SUPER_CALL!>this<!>.a) {}
    public init(c: Int64, b!: Int32 = <!ILLEGAL_THIS_OR_SUPER_CALL!>super<!>.a) {}
    public func f(d!: Int32 = this.a) {}
    public func f1(d!: Int32 = super.a) {}
}

struct A {
    public var a: Int64 = 1
    public init(e!: Int64 = <!ILLEGAL_THIS_OR_SUPER_CALL!>super<!>.a) {}
    public init(b!: String = "a", c!: Int64 = <!ILLEGAL_THIS_OR_SUPER_CALL!>this<!>.a) {}
}

main(): Int64 {
    return 0
}
