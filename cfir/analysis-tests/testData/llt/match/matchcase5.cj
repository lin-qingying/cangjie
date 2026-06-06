<!sema_missing_entry!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func foo(): Int32 {
    var a: Int32 = 1
    var m: Int32 = 2
    var b: Int32 = <!sema_match_case_must_have_default!>m<!>atch {
        case m == 1 => 1
        case m == 2 => 2
        case m == 3 => 3
    }
    return b
}
