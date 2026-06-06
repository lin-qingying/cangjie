// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE: %frontendCompiler %cmp_opt %typecheck %f 2>&1 | compare %f

interface I {}

struct A <: I {}

func f(<!chir_dce_unused_variable!>x<!>: I) {}

main() {
    match (f) {
        case <!sema_unreachable_pattern!>_: (A) -> Unit<!> => 0
        case _ => 1
    }
}
