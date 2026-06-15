// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I1 {
    mut static prop a: Int64 {
        get() {
            0
        }
        set(v) {}
    }
    static func f(): Unit {}
}

interface I2 <: I1 {
    <!INVALID_OVERRIDE_OR_REDEFINE_MEMBER_IN_INTERFACE!>m<!>ut static prop a: Int64
    <!INVALID_OVERRIDE_OR_REDEFINE_MEMBER_IN_INTERFACE!>s<!>tatic func f(): Unit
}

main() {}
