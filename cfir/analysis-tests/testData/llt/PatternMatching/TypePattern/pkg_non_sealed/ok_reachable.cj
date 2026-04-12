// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./pkg.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg.cj -o pkg.%middle
// EXEC: %compiler %cmp_opt pkg.%middle %f -o %output 2>&1 | compare %f
// EXEC: %run %run_opt %output %run_args
// ASSERT: regex-not warning

internal import pkg.*

class B <: A & I {}

func f(x: I) {
    match (x) {
        case _: A => 0
        case _: I => 1
    }
}

main() {
    f(B())
}
