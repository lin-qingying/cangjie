// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class A {
    public open prop f: Int32 {
        get() {
            return 0
        }
    }
    public init() {
    }
}

open class B <: A {
    public init() {
        <!sema_illegal_member_used_in_open_constructor!>f<!>
        var a = {=> <!sema_illegal_member_used_in_open_constructor!>f<!>}
        func f3(c!: Int32 = <!sema_illegal_member_used_in_open_constructor!>f<!>) {
            <!sema_illegal_member_used_in_open_constructor!>f<!>
        }
        a()
    }
    public open override prop f: Int32 {
        get() {
            return 2
        }
    }
}

class C <: B {
    public init() {
        super.f
        var a = {=> f}
        func f3(c!: Int32 = f) {
            this.f
        }
        B().f
        a()
    }
}

main(): Int64 {
    0
}
