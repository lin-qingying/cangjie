// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %f %cmp_opt -o %output 2>&1 | compare %f

main() {
    let a = true
    let b = false
    let d = Some(3)
    if (a || b && let Some(<!VAR_IN_OR_CONDITION!>c<!>) <- d) {
        0
    } else {
        1
    }
    if ((let Some(<!VAR_IN_OR_CONDITION!>c<!>) <- d) && a || b) {
        0
    }
}
