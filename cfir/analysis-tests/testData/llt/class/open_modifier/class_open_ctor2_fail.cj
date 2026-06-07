// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I {
    func coo() {}
}

open class A <: I {
    protected func foo() {}
}

extend A {
    func goo() {}
}

open class B <: A {
    public init() {
        { // lambda
            =>
            <!sema_illegal_member_used_in_open_constructor!>f<!>oo()
            <!sema_illegal_member_used_in_open_constructor!>g<!>oo()
            let f1 = <!sema_illegal_member_used_in_open_constructor!>f<!>oo
            <!sema_illegal_member_used_in_open_constructor!>g<!>oo
            this.<!sema_illegal_member_used_in_open_constructor!>c<!>oo
        }
    }
}

main(): Int64 {
    0
}
