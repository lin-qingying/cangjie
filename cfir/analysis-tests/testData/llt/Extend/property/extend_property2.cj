<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class A {
    public func f(): Int64 {
        0
    }
}

extend A {
    prop <!sema_extend_member_cannot_shadow!>f<!>: String {
        get() {
            return "C"
        }
    }
}

class B {
    public var f: String = "2"
}

extend B {
    prop <!sema_extend_member_cannot_shadow!>f<!>: String {
        get() {
            return "C"
        }
    }
}

extend B {
    prop <!sema_extend_member_cannot_shadow!>f<!>: String {
        get() {
            return "C"
        }
    }
}
