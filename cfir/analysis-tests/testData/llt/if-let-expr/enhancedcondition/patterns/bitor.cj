// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %f %cmp_opt -o %output -Woff all 2>&1 | compare %f

func foo(a: ?Int64, b: ?Int64) {
    if (let Some(_) <- a<!parse_expected_right_delimiter!> <!>| let None <- b) {
        1
    }
    if (let Some(_) <- b<!parse_expected_right_delimiter!> <!>| Go(8)) {
        2
    }
    if (let Some(_) | None <- a) {
        3
    } else {
        8
    }
}

func Go(a: Int64) {
    a > 3
}

main() {
    println(foo(Some(15), Some(3)))
    println(foo(Some(8), None))
}
