// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// EXEC: %run %run_opt %output %run_args

var a = 1

main(): Int64 {
    <!chir_dce_unused_function!>func g() {
        var <!chir_dce_unused_variable!>x<!> = {=> <!sema_capture_has_shadow_variable!>a<!>}
    }<!>
    let <!chir_dce_unused_variable!>a<!> = true
    return 0
}
