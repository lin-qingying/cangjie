// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %f %cmp_opt -o %output 2>&1 | compare %f

main() {
    let a = Some(15)
    if (let Some(<!VAR_IN_OR_CONDITION!>b<!>) <- a || true) {
        return 0
    }

    if (false || let Some(<!VAR_IN_OR_CONDITION!>b<!>) <- a) {
        return b - 1
    }

    return 2
}
