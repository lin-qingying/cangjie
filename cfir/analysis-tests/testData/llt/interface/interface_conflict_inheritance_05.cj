// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

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
    <!sema_invalid_override_or_redefine_member_in_interface!>m<!>ut static prop a: Int64
    <!sema_invalid_override_or_redefine_member_in_interface!>s<!>tatic func f(): Unit
}

main() {}
