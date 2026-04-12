// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

main(): Int64 {
    let a: Int64 = 10 << 1 << 1
    if (a != 40) {
        return 1
    }
    let b = -30 << 2
    if (b != -120) {
        return 1
    }
    let c = -30 >> 2
    if (c != -8) {
        return 1
    }
    return 0;
}
