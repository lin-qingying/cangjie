<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


extend Int64 {
    public static func foo() {
        return <!sema_static_members_cannot_call_members!>t<!>his
    }
}

class A {
    public func f() {}
}

extend A {
    public static func foo() {
        return <!sema_static_members_cannot_call_members!>t<!>his.f()
    }
}

struct B {
    public let item: Int64 = 0
}

extend B {
    public static func foo() {
        return <!sema_static_members_cannot_call_members!>t<!>his.item
    }
}
