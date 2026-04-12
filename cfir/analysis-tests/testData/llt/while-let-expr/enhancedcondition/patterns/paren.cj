// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %output
// RUN-EXEC: %run %run_opt %output %run_args | compare %f

func foo(a: ?Int64, b: ?Int64) {
    while (true && (let Some(_) <- a || let None <- b)) {
        return 8
    }
    -1
}

func Go(a: Int64) {
    a > 3
}

main() {
    println(foo(None, Some(3))) // -1
    println(foo(Some(8), None)) // 8
}

/* SCAN-IN
-1
8
 */
