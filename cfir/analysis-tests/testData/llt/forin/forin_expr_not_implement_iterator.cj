// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f

internal import std.collection.*

main() {
    for (num in <!sema_expr_in_forin_must_has_iterator!>1<!>0) {
        println(num)
    }
    return 0
}
