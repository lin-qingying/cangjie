// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I {
    mut prop a: Int64
    prop b: Int64
}

class A {}

extend A <: I {
    <!PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_MUT!>prop <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>a<!>: Int64 {
        get() {
            0
        }
    }<!>
    <!PROPERTY_HAVE_SAME_DECLARATION_IN_INHERIT_IMMUT!>mut prop <!CANNOT_WEAKEN_ACCESS_PRIVILEGE!>b<!>: Int64 {
        get() {
            0
        }
        set(v) {
            ()
        }
    }<!>
}

main() {
    0
}
