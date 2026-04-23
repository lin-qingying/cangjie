// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

enum E1 {
    EE | ER(Int32)

    public prop a: Int64 {
        get() {
            0
        }
    }
    <!PROPERTY_MUST_HAVE_ACCESSORS!>public prop b: Int64 {
    }
    <!IMMUTABLE_PROPERTY_WITH_SETTER!>public prop c: Int64 {
        get() <!TYPE_MISMATCH!>{}<!>
        set(v) {}
    }
}

main(): Int64 {
    let a = EE
    let b = ER(3)
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>a.a<!> = 1
    <!CANNOT_ASSIGN_TO_IMMUTABLE!>b.b<!> = 1
    b.<!NOT_MEMBER_OF!>d<!>
    return 0
}
