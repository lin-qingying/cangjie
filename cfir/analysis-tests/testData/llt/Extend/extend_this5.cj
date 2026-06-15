<!MISSING_ENTRY!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// CHECK_PROGRAM_ENTRY
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


extend Int64 {
    public static func foo() {
        return <!STATIC_MEMBERS_CANNOT_CALL_MEMBERS!>t<!>his
    }
}

class A {
    public func f() {}
}

extend A {
    public static func foo() {
        return <!STATIC_MEMBERS_CANNOT_CALL_MEMBERS!>t<!>his.f()
    }
}

struct B {
    public let item: Int64 = 0
}

extend B {
    public static func foo() {
        return <!STATIC_MEMBERS_CANNOT_CALL_MEMBERS!>t<!>his.item
    }
}
