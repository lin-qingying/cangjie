// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %f %cmp_opt 2>&1 | compare %f

main() {
    if (let <!parse_expected_pattern!>)<!> {

    }
    while (let  <!parse_expected_pattern!><-<!><!parse_expected_expression!>)<!> {

    }

    if (let a<!parse_expected_backarrow_in_let_cond!>)<!> {

    }
}
