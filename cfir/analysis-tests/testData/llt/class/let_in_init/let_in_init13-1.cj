// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class C <: A {
    public init() {
        a = 0
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>b = 0<!>
    }
    public init(c: Int32) {
        a = 0
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>b = 0<!>
    }
}

open class A {
    public mut prop a: Int64 {
        get() {
            return 0
        }
        set(v) {}
    }
    public prop b: Int64 {
        get() {
            return 0
        }
    }
    public init() {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!><!sema_illegal_member_used_in_open_constructor!>b<!> = 1<!>
    }
}

main(): Int64 {
    return 0
}
