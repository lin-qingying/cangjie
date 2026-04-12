// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %output
// RUN-EXEC: %run %run_opt %output %run_args | compare %f

func foo(a: ?Int64, b: ?Int64) {
    while (let Some(a) <- a && a > 10 && let Some(b) <- b) {
        return a + b
    }
    -1
}

main() {
    println(foo(Some(18), Some(-11))) // 7
    println(foo(Some(3), Some(5))) // -1
    println(foo(None, None)) // -1
}

/* SCAN-IN
7
-1
-1
 */
