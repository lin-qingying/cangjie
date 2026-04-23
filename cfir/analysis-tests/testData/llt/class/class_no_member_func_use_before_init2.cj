// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A {
    public var a = <!USED_BEFORE_INITIALIZATION!>f<!>
    public var b: () -> Int64
    public var d = g
    public static var e = g
    public init(x: () -> Int64) {
        func g() {
            0
        }
        b = g
        a = x
    }
    public init() {
        b = g
        var h = f()
        a = f
        a = this.f
    }
    public func f(): Int64 {
        return 0
    }
    public static func g(): Int64 {
        return 1
    }
}
