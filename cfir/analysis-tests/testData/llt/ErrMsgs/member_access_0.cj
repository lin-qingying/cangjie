// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %compile_lib_opt %n.cj 2>&1 | compare %f

main() {
    let a = 1 <!sema_invalid_binary_expr!>+<!> true

    (false <!sema_invalid_binary_expr!>+<!> 2).f()
    a.f()
    a.x

    0
}

// No errors:

// Errors:
