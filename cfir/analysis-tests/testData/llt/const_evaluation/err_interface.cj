// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I {
    const func f(): Int64
    static const func f2(): Int64
}

const func g<T>(i: T) where T <: I {
    i.f() + T.f2()
}

class A {}

extend A <: I {
    public func <!INHERIT_MEMBER_KIND_INCONSISTENT!>f<!>() {
        0
    }
    public static func <!INHERIT_MEMBER_KIND_INCONSISTENT!>f2<!>() {
        1
    }
    public func f(a: Int64) {
        0
    }
}

main() {
    const a = <!EXPECT_CONST!>A<!>()
    g(a)
}
