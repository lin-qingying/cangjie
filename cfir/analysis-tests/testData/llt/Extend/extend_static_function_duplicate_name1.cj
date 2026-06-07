// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class A {
    public var a = 0
    public static func foo1() {
        return 0
    }
}

extend A {
    public func <!INHERIT_MEMBER_KIND_INCONSISTENT, EXTEND_MEMBER_CANNOT_SHADOW!>foo1<!>() {
        return a
    }
}

extend A {
    public func <!INHERIT_MEMBER_KIND_INCONSISTENT!>foo1<!>(a: Int32) {
        return a
    }
}

main(): Int64 {
    return 0
}
