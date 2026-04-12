// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %output
// RUN-EXEC-PIPE: %run %run_opt %output %run_args | compare %f

main() {
    let r: Any = "fff"
    let a = Some(r)
    if (let Some(b: String) <- a && b.size >= 1) {
        println(b)
    }
    while ((let Some(b: String) <- a && b.size >= 1) && b.size >= 2) {
        println(b)
        break
    }
}

/* SCAN
fff
fff
*/
