<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

extend A {
    <!sema_static_and_non_static_member_cannot_have_same_name!>p<!>ublic func x1() {}
    public static func <!sema_extend_member_cannot_shadow!>x2<!>() {}
    public func <!sema_extend_member_cannot_shadow!>y1<!>() {}
    <!sema_static_and_non_static_member_cannot_have_same_name!>p<!>ublic static func y2() {}
    <!sema_static_and_non_static_member_cannot_have_same_name!>p<!>ublic func <!sema_extend_member_cannot_shadow!>f1<!>() {}
    public static func <!sema_extend_member_cannot_shadow!>f2<!>() {}
    public func <!sema_extend_member_cannot_shadow!>g1<!>() {}
    <!sema_static_and_non_static_member_cannot_have_same_name!>p<!>ublic static func <!sema_extend_member_cannot_shadow!>g2<!>() {}
}

extend A {
    private func <!sema_extend_member_cannot_shadow!>a<!>() {}
    public func <!sema_extend_member_cannot_shadow!>b<!>() {}
    private func <!sema_extend_member_cannot_shadow!>c<!>() {}
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
