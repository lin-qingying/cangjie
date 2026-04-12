// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix %run_args

main(): Int64 {
    var sum = 0
    for (i in 0..=0) {
        println(i)
        sum += 1
    }
    if (sum != 1) {
        return 1
    }
    return 0
}
