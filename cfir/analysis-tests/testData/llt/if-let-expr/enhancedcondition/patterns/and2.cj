// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %output
// RUN-EXEC: %run %run_opt %output %run_args | compare %f

func foo(a: ?Int64, b: ?Int64) {
    if (let Some(a) <- a && a > 10 && let Some(b) <- b) {
        a + b
    } else {
        -1
    }
}

main() {
    println(foo(Some(18), Some(-11)))
    println(foo(Some(3), Some(5)))
    println(foo(None, None))
}

/* SCAN-IN
7
-1
-1
 */
