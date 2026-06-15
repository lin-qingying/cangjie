<!MISSING_ENTRY!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// CHECK_PROGRAM_ENTRY
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


extend A {
    <!STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME!>p<!>ublic func x1() {}
    public static func <!EXTEND_MEMBER_CANNOT_SHADOW!>x2<!>() {}
    public func <!EXTEND_MEMBER_CANNOT_SHADOW!>y1<!>() {}
    <!STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME!>p<!>ublic static func y2() {}
    <!STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME!>p<!>ublic func <!EXTEND_MEMBER_CANNOT_SHADOW!>f1<!>() {}
    public static func <!EXTEND_MEMBER_CANNOT_SHADOW!>f2<!>() {}
    public func <!EXTEND_MEMBER_CANNOT_SHADOW!>g1<!>() {}
    <!STATIC_AND_NON_STATIC_MEMBER_CANNOT_HAVE_SAME_NAME!>p<!>ublic static func <!EXTEND_MEMBER_CANNOT_SHADOW!>g2<!>() {}
}

extend A {
    private func <!EXTEND_MEMBER_CANNOT_SHADOW!>a<!>() {}
    public func <!EXTEND_MEMBER_CANNOT_SHADOW!>b<!>() {}
    private func <!EXTEND_MEMBER_CANNOT_SHADOW!>c<!>() {}
    public func d() {}
    private func e() {}
}

class A {
    public static let x1: Int64 = 1
    public static let x2: Int64 = 1
    public let y1: Int64 = 2
    public let y2: Int64 = 2
    public static func f1(): Unit {}
    public static func f2(): Unit {}
    public func g1(): Unit {}
    public func g2(): Unit {}

    public func a() {}
    protected func b() {}
    protected func c() {}
    private func d() {}
    private func e() {}
}
