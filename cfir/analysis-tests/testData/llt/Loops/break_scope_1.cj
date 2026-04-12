// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %n.%suffix %run_args | compare %f

main() {
    var iter = 5
    while (iter > 0) {
        println("outer")
        do {
            println("inner")
        } while (break)
        iter--
    }
    0
}

// ASSERT: scan-1 outer
// ASSERT: scan-1 inner
