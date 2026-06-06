// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt -c %n.cj 2>&1 | compare %f

main() {
    let a: Bool = true

    if (a) {
        true <!sema_invalid_binary_expr!>+<!> 0
    } else {
        false <!sema_invalid_binary_expr!>+<!> 1
    }

    let b: Bool = if (a) {
        true <!sema_invalid_binary_expr!>+<!> 0
    } else {
        false <!sema_invalid_binary_expr!>+<!> 1
    }

    if (a) {
        true <!sema_invalid_binary_expr!>+<!> 0
    }
    let c: Bool = if (a) {
        true <!sema_invalid_binary_expr!>+<!> 0
    }
    0
}
