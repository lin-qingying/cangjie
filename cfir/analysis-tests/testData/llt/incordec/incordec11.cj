// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

main(): Int64 {
    var set1 = [1, 2, 3]
    set1[0] = set1[0] + 1
    set1[0] = set1[0] - 1
    if (set1[0] == 1) {
        return 0
    }
    return 1
}
