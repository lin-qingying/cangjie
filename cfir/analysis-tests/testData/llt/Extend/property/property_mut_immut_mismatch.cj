// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I {
    mut static prop p1: Int64
    mut prop p2: Int64
    static prop p3: Int64
    prop p4: Int64
    mut static prop p5: Int32
}

class A {}

extend A <: I {
    <!sema_property_have_same_declaration_in_inherit_mut!>s<!>tatic prop <!WEAK_VISIBILITY!>p1<!>: Int64 {
        get() {
            0
        }
    }
    <!sema_property_have_same_declaration_in_inherit_mut!>p<!>rop <!WEAK_VISIBILITY!>p2<!>: Int64 {
        get() {
            0
        }
    }
    <!sema_property_have_same_declaration_in_inherit_immut!>m<!>ut static prop <!WEAK_VISIBILITY!>p3<!>: Int64 {
        get() {
            0
        }
        set(v) {}
    }
    <!sema_property_have_same_declaration_in_inherit_immut!>m<!>ut prop <!WEAK_VISIBILITY!>p4<!>: Int64 {
        get() {
            0
        }
        set(v) {}
    }
    <!PROPERTY_OVERRIDE_IMPLEMENT_TYPE_DIFF!>s<!>tatic prop <!WEAK_VISIBILITY!>p5<!>: Int64 {
        get() {
            0
        }
    }
}
