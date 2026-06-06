<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface I {
    mut static prop p1: Int64
    mut prop p2: Int64
    static prop p3: Int64
    prop p4: Int64
    mut static prop p5: Int32
}

class A {}

extend A <: I {
    <!sema_property_have_same_declaration_in_inherit_mut!>s<!>tatic prop <!sema_weak_visibility!>p1<!>: Int64 {
        get() {
            0
        }
    }
    <!sema_property_have_same_declaration_in_inherit_mut!>p<!>rop <!sema_weak_visibility!>p2<!>: Int64 {
        get() {
            0
        }
    }
    <!sema_property_have_same_declaration_in_inherit_immut!>m<!>ut static prop <!sema_weak_visibility!>p3<!>: Int64 {
        get() {
            0
        }
        set(v) {}
    }
    <!sema_property_have_same_declaration_in_inherit_immut!>m<!>ut prop <!sema_weak_visibility!>p4<!>: Int64 {
        get() {
            0
        }
        set(v) {}
    }
    <!sema_property_override_implement_type_diff!>s<!>tatic prop <!sema_weak_visibility!>p5<!>: Int64 {
        get() {
            0
        }
    }
}
