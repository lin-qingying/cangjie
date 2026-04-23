// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class A {
    public init() {}
    public init(a: Int64) {
        this()
        this.a = a
    }
    public func test() {
        var res = <!ILLEGAL_THIS_OR_SUPER_CALL!>this<!>(1)
    }
    public var a: Int64 = 1
    public var b = <!ILLEGAL_THIS_OR_SUPER_CALL!>this<!>()
}
