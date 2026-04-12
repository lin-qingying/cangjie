// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %output | compare %f

main() {
    let num = 0

    let newNum = if (num == 0) {
        "let x= 1"
    } else {
        "\"仓颉\""
    }
    println(newNum)
    println(newNum is String)
}

/* SCAN
let x= 1
true
*/
