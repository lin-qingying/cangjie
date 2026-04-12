// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %n.%suffix %run_args | compare %f

main() {
    for (i in 1..5) {
        println("outer")
        while (continue) {
            println("inner")
        }
    }
    0
}

// ASSERT: scan-4 outer
// ASSERT: scan-not inner
