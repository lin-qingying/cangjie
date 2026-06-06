// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE: %frontendCompiler %cmp_opt %typecheck %f 2>&1 | compare %f

package myenum

enum E {
    | A
    | B
}

main() {
    let x = A
    match (x) {
        case E.A => 0
        case <!sema_unreachable_pattern!>A<!> => 0
        case <!sema_unreachable_pattern!>E.A<!> => 0
        case B => 1
        case <!sema_unreachable_pattern!>E.B<!> => 1
        case <!sema_unreachable_pattern!>E.B<!> => 1
    }
}
