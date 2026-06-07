<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class A {
    public mut open prop i: Int64 {
        get() {
            return 0
        }
        set(v) {}
    }
}

interface B {
    mut prop i: Int32
}

class C <: A & B {
    <!sema_property_override_implement_type_diff!>p<!>ublic mut override prop i: Int64 {
        get() {
            return 1
        }
        set(v) {}
    }
}

open class A1 {
    public mut open prop i: String {
        get() {
            return "0"
        }
        set(v) {}
    }
}

interface B1 {
    mut prop i: Int64
}

class C1 <: A1 & B1 {
    <!sema_property_override_implement_type_diff!>p<!>ublic mut override prop i: Int64 {
        get() {
            return 1
        }
        set(v) {}
    }
}
